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
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.edge.adapters.opcua.OpcUaException;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.opcua2mqtt.OpcUaJsonPayloadConverter;
import com.hivemq.edge.adapters.opcua.util.Bytes;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.serialization.SerializationContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class OpcUaSubscriptionConsumer {
    private static final Logger log = LoggerFactory.getLogger(OpcUaSubscriptionConsumer.class);

    public static final byte[] EMTPY_BYTES = new byte[]{};

    private final @NotNull ReadValueId readValueId;
    private final @NotNull ProtocolAdapterTagStreamingService protocolAdapterTagStreamingService;
    private final @NotNull EventService eventService;
    private final @NotNull String adapterId;
    private final @NotNull String protocolAdapterId;
    private final @NotNull UaSubscription uaSubscription;
    private final @NotNull OpcuaTag tag;
    private final @NotNull DataPointFactory dataPointFactory;
    private final @NotNull OpcUaToMqttConfig opcUaToMqttConfig;
    private final @NotNull SerializationContext serializationContext;

    public OpcUaSubscriptionConsumer(
            final @NotNull UaSubscription uaSubscription,
            final @NotNull OpcUaToMqttConfig opcUaToMqttConfig,
            final @NotNull ReadValueId readValueId,
            final @NotNull SerializationContext serializationContext,
            final @NotNull ProtocolAdapterTagStreamingService protocolAdapterTagStreamingService,
            final @NotNull EventService eventService,
            final @NotNull String adapterId,
            final @NotNull String protocolAdapterId,
            final @NotNull OpcuaTag tag,
            final @NotNull DataPointFactory dataPointFactory) {
        this.readValueId = readValueId;
        this.protocolAdapterTagStreamingService = protocolAdapterTagStreamingService;
        this.eventService = eventService;
        this.adapterId = adapterId;
        this.protocolAdapterId = protocolAdapterId;
        this.uaSubscription = uaSubscription;
        this.tag = tag;
        this.dataPointFactory = dataPointFactory;
        this.opcUaToMqttConfig = opcUaToMqttConfig;
        this.serializationContext = serializationContext;
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
                (item, id) -> {
                    final AtomicBoolean firstMessageReceived = new AtomicBoolean(false);
                    item.setValueConsumer(value -> {
                        if (firstMessageReceived.compareAndSet(false, true)) {
                            eventService.createAdapterEvent(adapterId, protocolAdapterId)
                                    .withSeverity(Event.SEVERITY.INFO)
                                    .withMessage(String.format("Adapter '%s' took first sample for tag '%s'",
                                            adapterId,
                                            tag.getName()))
                                    .fire();

                        }
                        final var convertedPayload = new String(convertPayload(value, serializationContext));
                        protocolAdapterTagStreamingService.feed(tag.getName(), List.of(dataPointFactory.createJsonDataPoint(tag.getName(), convertedPayload)));
                    });
        };

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
                    return new SubscriptionResult(tag, uaSubscription, this);
                });
    }

    public record SubscriptionResult (
            @NotNull OpcuaTag opcuaTag,
            @NotNull UaSubscription uaSubscription,
            @NotNull OpcUaSubscriptionConsumer consumer) {}

    private static byte @NotNull [] convertPayload(
            final @NotNull DataValue dataValue,
            final @NotNull SerializationContext serializationContext) {
        //null value, emtpy buffer
        if (dataValue.getValue().getValue() == null) {
            return EMTPY_BYTES;
        }
        return Bytes.fromReadOnlyBuffer(OpcUaJsonPayloadConverter.convertPayload(serializationContext,
                dataValue));
    }
}
