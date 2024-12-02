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
package com.hivemq.edge.adapters.opcua.client;

import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterPublishService;
import com.hivemq.edge.adapters.opcua.OpcUaException;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.serialization.SerializationContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class OpcUaSubscriptionConsumer {
    private static final Logger log = LoggerFactory.getLogger(OpcUaSubscriptionConsumer.class);

    private final @NotNull PollingContext northboundMapping;
    private final @NotNull OpcUaToMqttConfig opcUaToMqttConfig;
    private final @NotNull ReadValueId readValueId;
    private final @NotNull ProtocolAdapterPublishService adapterPublishService;
    private final @NotNull EventService eventService;
    private final @NotNull SerializationContext serializationContext;
    private final @NotNull Optional<EndpointDescription> endpointDescription;
    private final @NotNull ProtocolAdapterMetricsService metricsHelper;
    private final @NotNull String adapterId;
    private final @NotNull String protocolAdapterId;
    private final @NotNull UaSubscription uaSubscription;

    public OpcUaSubscriptionConsumer(
            final @NotNull OpcUaToMqttConfig opcUaToMqttConfig,
            final @NotNull PollingContext northboundMapping,
            final @NotNull UaSubscription uaSubscription,
            final @NotNull ReadValueId readValueId,
            final @NotNull ProtocolAdapterPublishService adapterPublishService,
            final @NotNull EventService eventService,
            final @NotNull Optional<EndpointDescription> endpointDescription,
            final @NotNull SerializationContext serializationContext,
            final @NotNull ProtocolAdapterMetricsService metricsHelper,
            final @NotNull String adapterId,
            final @NotNull String protocolAdapterId) {
        this.northboundMapping = northboundMapping;
        this.opcUaToMqttConfig = opcUaToMqttConfig;
        this.readValueId = readValueId;
        this.adapterPublishService = adapterPublishService;
        this.eventService = eventService;
        this.serializationContext = serializationContext;
        this.endpointDescription = endpointDescription;
        this.metricsHelper = metricsHelper;
        this.adapterId = adapterId;
        this.protocolAdapterId = protocolAdapterId;
        this.uaSubscription = uaSubscription;
    }

    public CompletableFuture<SubscriptionResult> start() {
        // create a new client handle, these have to be unique for each handle.
        final UInteger clientHandle = uaSubscription.nextClientHandle();

        final MonitoringParameters parameters = new MonitoringParameters(clientHandle,
                (double) opcUaToMqttConfig.getPublishingInterval(),
                null,
                uint(opcUaToMqttConfig.getServerQueueSize()),
                true);

        final MonitoredItemCreateRequest request =
                new MonitoredItemCreateRequest(readValueId, MonitoringMode.Reporting, parameters);

        final UaSubscription.ItemCreationCallback onItemCreated =
                (item, id) -> item.setValueConsumer(new OpcUaDataValueConsumer(northboundMapping,
                        opcUaToMqttConfig,
                        adapterPublishService,
                        serializationContext,
                        endpointDescription,
                        readValueId.getNodeId(),
                        metricsHelper,
                        adapterId,
                        eventService,
                        protocolAdapterId));

        return uaSubscription
                .createMonitoredItems(TimestampsToReturn.Both, List.of(request), onItemCreated)
                .thenApply(items -> {
                    for (final UaMonitoredItem item : items) {
                        if (item.getStatusCode().isGood()) {
                            if (log.isDebugEnabled()) {
                                log.debug("OPC UA subscription created for nodeId={}",
                                        item.getReadValueId().getNodeId());
                            }
                        } else {
                            final String descriptions = StatusCodes
                                    .lookup(item.getStatusCode().getValue())
                                    .map(descriptionArray -> String.join(",", descriptionArray))
                                    .orElse("no further description");

                            log.warn("OPC UA subscription failed for nodeId '{}': {} (status={})",
                                    item.getReadValueId().getNodeId(),
                                    descriptions,
                                    item.getStatusCode());

                            throw new OpcUaException("OPC UA subscription failed for nodeId `" +
                                    item.getReadValueId().getNodeId() +
                                    "`: " + descriptions +" (status '" +
                                    item.getStatusCode() +
                                    "')");
                        }
                    }
                    return new SubscriptionResult(northboundMapping, uaSubscription, this);
                });
    }

    public static class SubscriptionResult {
        public final @NotNull PollingContext subscription;
        public final @NotNull UaSubscription uaSubscription;
        public final @NotNull OpcUaSubscriptionConsumer consumer;

        public SubscriptionResult(
                final @NotNull PollingContext subscription,
                final @NotNull UaSubscription uaSubscription,
                final @NotNull OpcUaSubscriptionConsumer consumer) {
            this.subscription = subscription;
            this.uaSubscription = uaSubscription;
            this.consumer = consumer;
        }
    }
}
