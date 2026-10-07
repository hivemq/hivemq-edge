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
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            // Resolved before the payload is sent and after it is built, because the datapoint is the source of
            // both: the payload omits the response address and the topic consumes it.
            final String topic = topicFor(dataPoint);
            final ProtocolAdapterPublishBuilder publishBuilder = protocolAdapterPublishService
                    .createPublish()
                    .withTopic(topic)
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
                                            protocolAdapter.getId(), topic))
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

    /**
     * The {@code context} entry naming where a response should go, read rather than published.
     */
    private static final @NotNull String RESPONSE_TOPIC = "responseTopic";

    private static final @NotNull String CORRELATION_DATA = "correlationData";

    /**
     * The {@code context} entries a method response carries, each published by name in
     * {@link #publishResponseFields}. {@code methodName} is on every response; the other two only on a failure.
     */
    private static final @NotNull String METHOD_NAME = "methodName";

    private static final @NotNull String ERROR_CODE = "errorCode";

    private static final @NotNull String STATUS_INFO = "statusInfo";

    /**
     * The topic to publish a datapoint on: the address it carries, and the mapping's own topic when it carries
     * none.
     * <p>
     * There is deliberately no northbound setting gating this, and that is a decision rather than an omission. The
     * concern a gate would answer — a caller redirecting an adapter's output by sending a field — is already
     * answered on the southbound side, where the field mapping is a whitelist: with its response-topic flag off,
     * no instruction names {@code responseTopic}, the generated transform drops it, and the adapter never receives
     * it. So a datapoint carries a response address only when the southbound mapping asked for one <em>and</em>
     * the request supplied it. A second switch here would not add protection; it would only add a pair that can be
     * configured on one side and silently do nothing.
     */
    private @NotNull String topicFor(final @NotNull DataPoint dataPoint) {
        return responseTopicOf(dataPoint).orElseGet(northboundMapping::getMqttTopic);
    }

    /**
     * The response address a datapoint carries, if it carries a usable one.
     * <p>
     * Blank is treated as absent rather than as an error: a caller that sends an empty string has named no topic,
     * and publishing to {@code ""} would fail far from the cause.
     */
    private static @NotNull Optional<String> responseTopicOf(final @NotNull DataPoint dataPoint) {
        if (!(dataPoint instanceof final DataPointWithMetadata dpMeta)) {
            return Optional.empty();
        }
        return dpMeta.getContext()
                .map(context -> context.get(RESPONSE_TOPIC))
                .filter(JsonNode::isTextual)
                .map(JsonNode::textValue)
                .filter(topic -> !topic.isBlank());
    }

    private byte @NotNull [] convertDataPointToJson(
            final @NotNull DataPoint dataPoint, final @NotNull ObjectMapper objectMapper)
            throws JsonProcessingException {
        final ObjectNode node = JsonNodeFactory.instance.objectNode();
        if (dataPoint instanceof final DataPointWithMetadata dpMeta) {
            node.set("value", dpMeta.getTagValue());
            if (northboundMapping.getIncludeTimestamp()) {
                node.set("timestamp", JsonNodeFactory.instance.numberNode(dpMeta.getTimestamp()));
            }
            if (northboundMapping.getIncludeMetadata()) {
                dpMeta.getMetadata()
                        .ifPresentOrElse(
                                metadata -> node.set("metadata", objectMapper.convertValue(metadata, JsonNode.class)),
                                () -> node.set("metadata", JsonNodeFactory.instance.nullNode()));
            }
        } else {
            if (northboundMapping.getIncludeTimestamp()) {
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
        if (northboundMapping.getIncludeTagNames()) {
            node.set("tagName", JsonNodeFactory.instance.textNode(dataPoint.getTagName()));
        }
        if (!northboundMapping.getUserProperties().isEmpty()) {
            node.set(
                    "mqttUserProperties",
                    objectMapper.convertValue(northboundMapping.getUserProperties(), JsonNode.class));
        }
        if (dataPoint instanceof final DataPointWithMetadata dpMeta) {
            dpMeta.getContext().ifPresent(context -> publishResponseFields(node, context));
        }
        return objectMapper.writeValueAsBytes(node);
    }

    /**
     * Publishes the four method-response fields a datapoint carries in its {@code context} child, each beside
     * {@code value} rather than nested under a {@code context} key — so the message reads
     * {@code {"methodName": "Start", "value": {...}}}.
     * <p>
     * <b>Each field is named here, one statement apiece.</b> This deliberately does not copy whatever
     * {@code context} happens to hold. {@code context} is the <em>protocol's</em> namespace for what describes a
     * tag or device and does not change — units, a node id — so Edge does not own it and must not republish it
     * wholesale. A method response's fields are per-message and belong in {@code metadata} by that model; they
     * ride here only because carrying them properly needs new datapoint aspects, which is an SDK change this
     * release does not make. Edge-2 gives them named aspects.
     * <p>
     * An earlier version copied every entry. That published {@code {"key": "value"}} — a placeholder the polling
     * datapoint builder had written into {@code context} since 2026-03 and that nine releases shipped unseen,
     * because nothing published {@code context} — on every message of all eight polling adapters, and
     * {@code SimpleHttpToMqttIT} caught it on a strict comparison. Naming the fields is what makes that class of
     * surprise impossible: a field reaches the wire only if this method says so.
     * <p>
     * <b>{@code responseTopic} is read, never published.</b> It says <em>where</em> the answer goes rather than
     * anything about the answer, and {@link #responseTopicOf} consumes it to choose the topic; publishing it too
     * would put a routing instruction inside the routed message.
     * <p>
     * <b>{@code correlationData} is published only when the mapping asks.</b> The southbound flag decides whether
     * the token is read at all, this one whether it goes on the wire: a deployment may correlate inside Edge and
     * still keep the token off its MQTT messages.
     */
    private void publishResponseFields(final @NotNull ObjectNode node, final @NotNull JsonNode context) {
        if (!context.isObject()) {
            return;
        }
        copy(node, context, METHOD_NAME);
        if (northboundMapping.isWriteCorrelationData()) {
            copy(node, context, CORRELATION_DATA);
        }
        copy(node, context, ERROR_CODE);
        copy(node, context, STATUS_INFO);
    }

    /**
     * Copies one named field, when the datapoint carries it and the message has no such field yet.
     * <p>
     * The present-check is what makes a field optional: {@code errorCode} and {@code statusInfo} appear only on a
     * failure, {@code correlationData} only when the caller sent a token. The not-already-set check keeps a
     * context entry from overwriting an aspect written above — it cannot happen for these four names today, and a
     * silent replacement of {@code value} would be far harder to diagnose than one that declines.
     */
    private static void copy(
            final @NotNull ObjectNode node, final @NotNull JsonNode context, final @NotNull String field) {
        final JsonNode value = context.get(field);
        if (value != null && !node.has(field)) {
            node.set(field, value);
        }
    }

    @Override
    public @Nullable String getScope() {
        return protocolAdapter.getId();
    }
}
