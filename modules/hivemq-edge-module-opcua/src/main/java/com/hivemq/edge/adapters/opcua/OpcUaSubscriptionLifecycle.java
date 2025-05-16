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
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.opcua2mqtt.OpcUaJsonPayloadConverter;
import com.hivemq.edge.adapters.opcua.util.Bytes;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.UaServiceFaultException;
import org.eclipse.milo.opcua.stack.core.encoding.EncodingContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class OpcUaSubscriptionLifecycle implements OpcUaSubscription.SubscriptionListener {

    private static final Logger log = LoggerFactory.getLogger(OpcUaSubscriptionLifecycle.class);

    public static final byte[] EMTPY_BYTES = new byte[]{};

    private final @NotNull OpcUaClient opcUaClient;
    private final @NotNull String adapterId;
    private final @NotNull String protocolAdapterId;
    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull EventService eventService;
    private final @NotNull ProtocolAdapterTagStreamingService protocolAdapterTagStreamingService;
    private final @NotNull OpcUaToMqttConfig opcUaToMqttConfig;
    private final @NotNull DataPointFactory dataPointFactory;

    private final @NotNull Map<NodeId, OpcuaTag> nodeIdToTag = new ConcurrentHashMap<>();
    private final @NotNull Map<OpcuaTag, Boolean> tagToFirstSeen = new ConcurrentHashMap<>();

    private final @NotNull List<OpcuaTag> tags;

    private volatile @NotNull OpcUaSubscription subscription;

    public OpcUaSubscriptionLifecycle(
            final @NotNull OpcUaClient opcUaClient,
            final @NotNull String adapterId,
            final @NotNull String protocolAdapterId,
            final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService,
            final @NotNull EventService eventService,
            final @NotNull ProtocolAdapterTagStreamingService protocolAdapterTagStreamingService,
            final @NotNull OpcUaToMqttConfig opcUaToMqttConfig,
            final @NotNull DataPointFactory dataPointFactory,
            final @NotNull List<OpcuaTag> tags) {
        this.opcUaClient = opcUaClient;
        this.adapterId = adapterId;
        this.protocolAdapterId = protocolAdapterId;
        this.protocolAdapterMetricsService = protocolAdapterMetricsService;
        this.eventService = eventService;
        this.protocolAdapterTagStreamingService = protocolAdapterTagStreamingService;
        this.opcUaToMqttConfig = opcUaToMqttConfig;
        this.dataPointFactory = dataPointFactory;
        this.tags = tags;
    }

    @Override
    public void onKeepAliveReceived(final @NotNull OpcUaSubscription subscription) {
        OpcUaSubscription.SubscriptionListener.super.onKeepAliveReceived(subscription);
        protocolAdapterMetricsService.increment("subscription.keepalive.count");
    }

    @Override
    public synchronized void onTransferFailed(final OpcUaSubscription deadSubscription, final StatusCode statusCode) {
        protocolAdapterMetricsService.increment("subscription.transfer.failed.count");

        if(subscription != null) {
            subscription = null;
        }
        start().exceptionally(ex -> {
                    if (ex instanceof UaServiceFaultException) {
                        final UaServiceFaultException cause = (UaServiceFaultException) ex.getCause();
                        if (cause.getStatusCode().getValue() == StatusCodes.Bad_SubscriptionIdInvalid) {
                            log.warn("Failed resubscribing to OPC UA after transfer failure: {}", statusCode, ex);
                            //TODO there was another subscribe attempt in here, do we need that????
    //                        try {
    //                            subscribe(subscriptionResult.opcuaTag()).exceptionally(t -> {
    //                                log.error("Problem resucbscribing after subscription {} failed with {}",
    //                                        subscription.getSubscriptionId(),
    //                                        statusCode,
    //                                        t);
    //                                return null;
    //                            }).get();
    //                        } catch (InterruptedException | ExecutionException e) {
    //                            log.error("Problem resucbscribing after subscription {} failed with {}",
    //                                    subscription.getSubscriptionId(),
    //                                    statusCode,
    //                                    e);
    //                        }
                        } else {
                            log.error("Not able to recreate OPC UA subscription after transfer failure", ex);
                        }
                    } else {
                        log.error("Not able to recreate OPC UA subscription after transfer failure", ex);
                    }
                    return null;
                });

    }

    @Override
    public void onDataReceived(
            final OpcUaSubscription subscription,
            final List<OpcUaMonitoredItem> items,
            final List<DataValue> values) {

        for (int i = 0; i < items.size(); i++) {
            final var tag = nodeIdToTag.get(items.get(i).getReadValueId().getNodeId());
            if (null == tagToFirstSeen.putIfAbsent(tag, true)) {
                eventService.createAdapterEvent(adapterId, protocolAdapterId)
                        .withSeverity(Event.SEVERITY.INFO)
                        .withMessage(String.format("Adapter '%s' took first sample for tag '%s'",
                                adapterId,
                                tag.getName()))
                        .fire();

            }
            final var value = values.get(i);
            try {
                final var convertedPayload = new String(convertPayload(value, opcUaClient.getDynamicEncodingContext()));
                protocolAdapterTagStreamingService.feed(tag.getName(), List.of(dataPointFactory.createJsonDataPoint(tag.getName(), convertedPayload)));
            } catch (UaException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public @NotNull CompletableFuture<Object> start() {

        subscription = new OpcUaSubscription(opcUaClient);
        subscription.setPublishingInterval((double)opcUaToMqttConfig.getPublishingInterval());
        subscription.setSubscriptionListener(this);

        tags.forEach(opcuaTag -> {
            final String nodeId = opcuaTag.getDefinition().getNode();
            log.info("Subscribing to OPC UA node {}", nodeId);
            final ReadValueId readValueId =
                    new ReadValueId(NodeId.parse(nodeId), AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE);
            var monitoredItem = OpcUaMonitoredItem
                    .newDataItem(readValueId.getNodeId(), MonitoringMode.Reporting);
            monitoredItem.setQueueSize(uint(opcUaToMqttConfig.getServerQueueSize()));
            monitoredItem.setSamplingInterval(opcUaToMqttConfig.getPublishingInterval());
            subscription.addMonitoredItem(monitoredItem);
        });

        return subscription
                .createAsync()
                .thenCompose(v -> {
                    final var failures = subscription
                            .createMonitoredItems()
                            .stream()
                            .map(item -> {
                                if (item.isGood()) {
                                    if (log.isDebugEnabled()) {
                                        log.debug("OPC UA subscription created for nodeId={}",
                                                item.monitoredItem().getReadValueId().getNodeId());
                                    }
                                    return Optional.empty();
                                } else {
                                    final String descriptions = StatusCodes
                                            .lookup(item.serviceResult().getValue())
                                            .map(descriptionArray -> String.join(",", descriptionArray))
                                            .orElse("no further description");

                                    log.warn("OPC UA subscription failed for nodeId '{}': {} (status={})",
                                            item.monitoredItem().getReadValueId().getNodeId(),
                                            descriptions,
                                            item.serviceResult());

                                    return Optional.of("OPC UA subscription failed for nodeId `" +
                                            item.monitoredItem().getReadValueId().getNodeId() +
                                            "`: " + descriptions +" (status '" +
                                            item.serviceResult() +
                                            "')");
                                }
                            })
                            .filter(Optional::isPresent)
                            .toList();
                    if (!failures.isEmpty()) {
                        //TODO needs a nicer error message
                        return CompletableFuture.failedFuture(new OpcUaException(failures.toString()));
                    }
                    return CompletableFuture.completedFuture(null);
                }).toCompletableFuture();
    }

    @NotNull
    public CompletableFuture<Void> stop() {
        return CompletableFuture.completedFuture(null);
    }

    private static byte @NotNull [] convertPayload(
            final @NotNull DataValue dataValue,
            final @NotNull EncodingContext serializationContext) {
        //null value, emtpy buffer
        if (dataValue.getValue().getValue() == null) {
            return EMTPY_BYTES;
        }
        return Bytes.fromReadOnlyBuffer(OpcUaJsonPayloadConverter.convertPayload(serializationContext,
                dataValue));
    }

}
