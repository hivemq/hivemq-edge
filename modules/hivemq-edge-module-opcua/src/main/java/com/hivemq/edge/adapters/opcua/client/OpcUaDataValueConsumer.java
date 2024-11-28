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

import com.hivemq.adapter.sdk.api.ProtocolAdapterPublishBuilder;
import com.hivemq.adapter.sdk.api.ProtocolPublishResult;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.events.model.Payload;
import com.hivemq.adapter.sdk.api.mappings.fromedge.FromEdgeMapping;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterPublishService;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttMapping;
import com.hivemq.edge.adapters.opcua.opcua2mqtt.OpcUaJsonPayloadConverter;
import com.hivemq.edge.adapters.opcua.util.Bytes;
import org.eclipse.milo.opcua.stack.core.serialization.SerializationContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class OpcUaDataValueConsumer implements Consumer<DataValue> {
    private static final Logger log = LoggerFactory.getLogger(OpcUaDataValueConsumer.class);

    public static final byte[] EMTPY_BYTES = new byte[]{};

    private final @NotNull FromEdgeMapping mapping;
    final @NotNull OpcUaToMqttConfig opcUaToMqttConfig;
    private final @NotNull ProtocolAdapterPublishService adapterPublishService;
    private final @NotNull SerializationContext serializationContext;
    private final @NotNull Optional<EndpointDescription> endpoint;
    private final @NotNull NodeId nodeId;
    private final @NotNull ProtocolAdapterMetricsService metricsHelper;
    private final @NotNull EventService eventService;
    private final @NotNull String protocolAdapterId;
    private final @NotNull String adapterId;
    private final @NotNull AtomicBoolean firstMessageReceived = new AtomicBoolean(false);

    public OpcUaDataValueConsumer(
            final @NotNull FromEdgeMapping mapping,
            final @NotNull OpcUaToMqttConfig opcUaToMqttConfig,
            final @NotNull ProtocolAdapterPublishService adapterPublishService,
            final @NotNull SerializationContext serializationContext,
            final @NotNull Optional<EndpointDescription> endpoint,
            final @NotNull NodeId nodeId,
            final @NotNull ProtocolAdapterMetricsService metricsHelper,
            final @NotNull String adapterId,
            final @NotNull EventService eventService,
            final @NotNull String protocolAdapterId) {
        this.mapping = mapping;
        this.opcUaToMqttConfig = opcUaToMqttConfig;
        this.adapterPublishService = adapterPublishService;
        this.nodeId = nodeId;
        this.adapterId = adapterId;
        this.metricsHelper = metricsHelper;
        this.eventService = eventService;
        this.protocolAdapterId = protocolAdapterId;
        this.serializationContext = serializationContext;
        this.endpoint = endpoint;
    }

    @Override
    public void accept(final @NotNull DataValue dataValue) {
        try {

            final byte[] convertedPayload = convertPayload(dataValue, serializationContext);
            final ProtocolAdapterPublishBuilder publishBuilder = adapterPublishService.createPublish()
                    .withTopic(mapping.getMqttTopic())
                    .withPayload(convertedPayload).withQoS(mapping.getMqttQos())
                    .withContextInformation("opcua-node-id", nodeId.toParseableString());

            publishBuilder.withMessageExpiryInterval(mapping.getMessageExpiryInterval());

            try {
                endpoint.ifPresent(ep -> {
                    publishBuilder.withContextInformation("opcua-server-endpoint-url", ep.getEndpointUrl());
                    publishBuilder.withContextInformation("opcua-server-application-uri",
                    ep.getServer().getApplicationUri());
                });
            } catch (final Exception e) {
                //ignore, but log
                log.debug("Not able to get dynamic context infos for OPC UA message for adapter {}", adapterId);
            }


            if (firstMessageReceived.compareAndSet(false, true)) {
                eventService.createAdapterEvent(adapterId, protocolAdapterId)
                        .withSeverity(Event.SEVERITY.INFO)
                        .withMessage(String.format("Adapter '%s' took first sample to be published to '%s'",
                                adapterId,
                                mapping.getMqttTopic()))
                        .withPayload(Payload.ContentType.JSON, new String(convertedPayload, StandardCharsets.UTF_8))
                        .fire();

            }
            final CompletableFuture<ProtocolPublishResult> publishFuture = publishBuilder.send();

            publishFuture.thenAccept(publishReturnCode -> {
                metricsHelper.incrementReadPublishSuccess();
            }).exceptionally(throwable -> {
                log.error("Error on publishing from OPC UA subscription for adapter {}", adapterId, throwable);
                metricsHelper.incrementReadPublishFailure();
                return null;
            });

        } catch (Exception e) {
            log.error("Error on creating MQTT publish from OPC UA subscription for adapter {}", adapterId, e);
        }
    }

    private static byte @NotNull [] convertPayload(
            final @NotNull DataValue dataValue,
            final @NotNull SerializationContext serializationContext) {
        //null value, emtpy buffer
        if (dataValue.getValue().getValue() == null) {
            return EMTPY_BYTES;
        }
        return Bytes.fromReadOnlyBuffer(OpcUaJsonPayloadConverter.convertPayload(serializationContext, dataValue));
    }
}
