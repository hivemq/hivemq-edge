/*
 * Copyright 2023-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.edge.adapters.opcua;

import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.edge.adapters.opcua.client.OpcUaSubscriptionConsumer;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.UaServiceFaultException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class OpcUaSubscriptionLifecycle implements OpcUaSubscription.SubscriptionListener {

    private static final Logger log = LoggerFactory.getLogger(OpcUaSubscriptionLifecycle.class);

    private final @NotNull Map<UInteger, OpcUaSubscriptionConsumer.SubscriptionResult> subscriptionMap =
            new ConcurrentHashMap<>();
    private final @NotNull OpcUaClient opcUaClient;
    private final @NotNull String adapterId;
    private final @NotNull String protocolAdapterId;
    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull EventService eventService;
    private final @NotNull ProtocolAdapterTagStreamingService protocolAdapterTagStreamingService;
    private final @NotNull OpcUaToMqttConfig opcUaToMqttConfig;
    private final @NotNull DataPointFactory dataPointFactory;


    public OpcUaSubscriptionLifecycle(
            final @NotNull OpcUaClient opcUaClient,
            final @NotNull String adapterId,
            final @NotNull String protocolAdapterId,
            final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService,
            final @NotNull EventService eventService,
            final @NotNull ProtocolAdapterTagStreamingService protocolAdapterTagStreamingService,
            final @NotNull OpcUaToMqttConfig opcUaToMqttConfig,
            final @NotNull DataPointFactory dataPointFactory) {
        this.opcUaClient = opcUaClient;
        this.adapterId = adapterId;
        this.protocolAdapterId = protocolAdapterId;
        this.protocolAdapterMetricsService = protocolAdapterMetricsService;
        this.eventService = eventService;
        this.protocolAdapterTagStreamingService = protocolAdapterTagStreamingService;
        this.opcUaToMqttConfig = opcUaToMqttConfig;
        this.dataPointFactory = dataPointFactory;
    }

    @Override
    public void onKeepAliveReceived(final @NotNull OpcUaSubscription subscription) {
        OpcUaSubscription.SubscriptionListener.super.onKeepAliveReceived(subscription);
        protocolAdapterMetricsService.increment("subscription.keepalive.count");
    }

    @Override
    public void onTransferFailed(final OpcUaSubscription subscription, final StatusCode statusCode) {
        protocolAdapterMetricsService.increment("subscription.transfer.failed.count");

        //clear subscription from the map since it is dead
        final OpcUaSubscriptionConsumer.SubscriptionResult subscriptionResult =
                subscriptionMap.remove(subscription.getSubscriptionId());

        if (subscriptionResult != null) {
            subscribe(subscriptionResult.opcuaTag()).exceptionally(ex -> {
                if (ex instanceof UaServiceFaultException) {
                    final UaServiceFaultException cause = (UaServiceFaultException) ex.getCause();
                    if (cause.getStatusCode().getValue() == StatusCodes.Bad_SubscriptionIdInvalid) {
                        log.warn("Resubscribing to OPC UA after transfer failure: {}", statusCode, ex);
                        try {
                            subscribe(subscriptionResult.opcuaTag()).exceptionally(t -> {
                                log.error("Problem resucbscribing after subscription {} failed with {}",
                                        subscription.getSubscriptionId(),
                                        statusCode,
                                        t);
                                return null;
                            }).get();
                        } catch (InterruptedException | ExecutionException e) {
                            log.error("Problem resucbscribing after subscription {} failed with {}",
                                    subscription.getSubscriptionId(),
                                    statusCode,
                                    e);
                        }
                    } else {
                        log.error("Not able to recreate OPC UA subscription after transfer failure", ex);
                    }
                } else {
                    log.error("Not able to recreate OPC UA subscription after transfer failure", ex);
                }
                return null;
            });
        } else {
            log.error("Received Transfer Failure {} for a non existent subscription: {}", subscription, statusCode);
        }

    }


    public @NotNull CompletableFuture<Object> subscribe(final @NotNull OpcuaTag opcuaTag) {
        final String nodeId = opcuaTag.getDefinition().getNode();
        log.info("Subscribing to OPC UA node {}", nodeId);
        final ReadValueId readValueId =
                new ReadValueId(NodeId.parse(nodeId), AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE);

        final var subscription = new OpcUaSubscription(opcUaClient);
        subscription.setPublishingInterval((double)opcUaToMqttConfig.getPublishingInterval());
        return subscription
                .createAsync()
                .thenCompose(v -> {
                    try {
                        return new OpcUaSubscriptionConsumer(
                                subscription,
                                opcUaToMqttConfig,
                                readValueId,
                                opcUaClient.getDynamicEncodingContext(),
                                protocolAdapterTagStreamingService,
                                eventService,
                                adapterId,
                                protocolAdapterId,
                                opcuaTag,
                                dataPointFactory)
                                .start();
                    } catch (UaException e) {
                        return CompletableFuture.failedFuture(e);
                    }
                })
                .thenCompose(result -> {
                    subscription
                            .getSubscriptionId()
                            .ifPresentOrElse(
                                    id -> subscriptionMap.put(id, result),
                                    () -> log.error("Subscription ID is not present for tag {}", opcuaTag)
                            );
                    return null;
                }).toCompletableFuture();
    }

    public @NotNull CompletableFuture<Void> subscribeAll(final @NotNull List<OpcuaTag> tags) {

        final CompletableFuture<Void> resultFuture = new CompletableFuture<>();

        final CompletableFuture[] subscriptionFutures =
                tags.stream().map(this::subscribe).toArray(CompletableFuture[]::new);


        CompletableFuture.allOf(subscriptionFutures).thenApply(unused -> {
            resultFuture.complete(null);
            return null;
        }).exceptionally(throwable -> {
            resultFuture.completeExceptionally(throwable);
            return null;
        });
        return resultFuture;
    }


    @NotNull
    public CompletableFuture<Void> stop() {
        return CompletableFuture.completedFuture(null);
    }

}
