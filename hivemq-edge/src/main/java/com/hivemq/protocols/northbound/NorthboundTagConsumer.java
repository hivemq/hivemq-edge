/*
 * Copyright 2019-present HiveMQ GmbH
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
package com.hivemq.protocols.northbound;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.hivemq.adapter.sdk.api.ProtocolAdapterPublishBuilder;
import com.hivemq.adapter.sdk.api.ProtocolPublishResult;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Payload;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.datapoint.DataPointWithMetadata;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterPublishServiceImpl;
import com.hivemq.edge.modules.api.events.model.EventImpl;
import com.hivemq.persistence.mappings.NorthboundMapping;
import com.hivemq.protocols.ProtocolAdapterWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class NorthboundTagConsumer implements SingleTagConsumer {

    private static final Logger log = LoggerFactory.getLogger(NorthboundTagConsumer.class);

    private final @NotNull NorthboundMapping northboundMapping;
    private final @NotNull ProtocolAdapterWrapper protocolAdapter;
    private final @NotNull ObjectMapper objectMapper;
    private final @NotNull ProtocolAdapterPublishServiceImpl protocolAdapterPublishService;
    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull EventService eventService;
    private final @NotNull AtomicInteger publishCount = new AtomicInteger(0);

    public NorthboundTagConsumer(
            final @NotNull NorthboundMapping northboundMapping,
            final @NotNull ProtocolAdapterWrapper protocolAdapter,
            final @NotNull ObjectMapper objectMapper,
            final @NotNull ProtocolAdapterPublishServiceImpl protocolAdapterPublishService,
            final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService,
            final @NotNull EventService eventService) {
        this.northboundMapping = northboundMapping;
        this.protocolAdapter = protocolAdapter;
        this.objectMapper = objectMapper;
        this.protocolAdapterPublishService = protocolAdapterPublishService;
        this.protocolAdapterMetricsService = protocolAdapterMetricsService;
        this.eventService = eventService;
    }

    @Override
    public void accept(final @NotNull DataPoint dataPoint) {
        Preconditions.checkNotNull(dataPoint);
        Preconditions.checkNotNull(northboundMapping);
        Preconditions.checkNotNull(northboundMapping.getMqttTopic());

        Preconditions.checkArgument(
                northboundMapping.getMqttQos() <= 2 && northboundMapping.getMqttQos() >= 0,
                "QoS needs to be a valid QoS value (0,1,2)");
        try {
            final byte[] jsonToSend = convertDataPointToJson(dataPoint, objectMapper);
            final ProtocolAdapterPublishBuilder publishBuilder = protocolAdapterPublishService
                    .createPublish()
                    .withTopic(northboundMapping.getMqttTopic())
                    .withQoS(northboundMapping.getMqttQos())
                    .withPayload(jsonToSend)
                    .withAdapter(protocolAdapter.getAdapter());
            final CompletableFuture<ProtocolPublishResult> publishFuture = publishBuilder.send();
            publishFuture
                    .thenAccept(publishReturnCode -> {
                        protocolAdapterMetricsService.incrementReadPublishSuccess();
                        if (publishCount.incrementAndGet() == 1) {
                            eventService
                                    .createAdapterEvent(
                                            protocolAdapter.getId(),
                                            protocolAdapter
                                                    .getAdapterInformation()
                                                    .getProtocolId())
                                    .withSeverity(EventImpl.SEVERITY.INFO)
                                    .withTimestamp(System.currentTimeMillis())
                                    .withMessage(String.format(
                                            "Adapter '%s' took first sample to be published to '%s'",
                                            protocolAdapter.getId(), northboundMapping.getMqttTopic()))
                                    .withPayload(
                                            Payload.ContentType.JSON, new String(jsonToSend, StandardCharsets.UTF_8))
                                    .fire();
                        }
                    })
                    .exceptionally(throwable -> {
                        protocolAdapterMetricsService.incrementReadPublishFailure();
                        log.warn("Error publishing adapter payload", throwable);
                        return null;
                    });
        } catch (final Exception e) {
            log.warn("Exception during polling of data for adapters '{}':", protocolAdapter.getId(), e);
        }
    }

    @Override
    public @NotNull String getTagName() {
        return northboundMapping.getTagName();
    }

    private byte @NotNull [] convertDataPointToJson(
            final @NotNull DataPoint dataPoint, final @NotNull ObjectMapper objectMapper)
            throws JsonProcessingException {
        final ObjectNode node = JsonNodeFactory.instance.objectNode();
        if (dataPoint instanceof final DataPointWithMetadata dpMeta) {
            node.set("value", dpMeta.getTagValue());
            if(northboundMapping.getIncludeTimestamp()) {
                node.set("timestamp", JsonNodeFactory.instance.numberNode(dpMeta.getTimestamp()));
            }
            if (northboundMapping.getIncludeMetadata()) {
                dpMeta.getMetadata()
                        .ifPresentOrElse(
                                metadata -> node.set("metadata", objectMapper.convertValue(metadata, JsonNode.class)),
                                () -> node.set("metadata", JsonNodeFactory.instance.nullNode()));
            }
        } else {
            if(northboundMapping.getIncludeTimestamp()) {
                node.set("timestamp", JsonNodeFactory.instance.numberNode(System.currentTimeMillis()));
            }
            if (dataPoint.treatTagValueAsJson()) {
                final JsonNode jsonValue = objectMapper.readTree((String) dataPoint.getTagValue());
                if (jsonValue != null) {
                    node.set("value", jsonValue);
                } else {
                    throw new RuntimeException("No value entry in JSON message");
                }
            } else {
                node.set("value", objectMapper.convertValue(dataPoint.getTagValue(), JsonNode.class));
            }
        }
        if(northboundMapping.getIncludeTagNames()) {
            node.set("tagName", JsonNodeFactory.instance.textNode(dataPoint.getTagName()));
        }
        return objectMapper.writeValueAsBytes(node);
    }

    @Override
    public @Nullable String getScope() {
        return protocolAdapter.getId();
    }
}
