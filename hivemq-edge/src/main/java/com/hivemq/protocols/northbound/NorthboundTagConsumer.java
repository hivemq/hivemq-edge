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

import static com.hivemq.protocols.MethodRequestFields.CONTEXT;
import static com.hivemq.protocols.MethodRequestFields.CORRELATION_DATA;
import static com.hivemq.protocols.MethodRequestFields.ERROR_CODE;
import static com.hivemq.protocols.MethodRequestFields.INTERNAL_RESPONSE_PARAMETERS;
import static com.hivemq.protocols.MethodRequestFields.METHOD_NAME;
import static com.hivemq.protocols.MethodRequestFields.RESPONSE_TOPIC;
import static com.hivemq.protocols.MethodRequestFields.STATUS_INFO;
import static com.hivemq.protocols.MethodRequestFields.VALUE;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.hivemq.adapter.sdk.api.ProtocolPublishResult;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Payload;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.datapoint.DataPointWithMetadata;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterPublishBuilderImpl;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterPublishServiceImpl;
import com.hivemq.edge.modules.api.events.model.EventImpl;
import com.hivemq.persistence.mappings.NorthboundMapping;
import com.hivemq.protocols.ProtocolAdapterWrapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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

        // PHASE 1 -- VALIDATE
        // Consumes the datapoint from phase 0 and the mapping this consumer was built with. Produces
        // nothing: a message is only worth assembling once the mapping can address it.
        Preconditions.checkNotNull(dataPoint);
        Preconditions.checkNotNull(northboundMapping);
        Preconditions.checkNotNull(northboundMapping.getMqttTopic());
        Preconditions.checkArgument(
                northboundMapping.getMqttQos() <= 2 && northboundMapping.getMqttQos() >= 0,
                "QoS needs to be a valid QoS value (0,1,2)");

        try {

            // PHASE 2 -- DISASSEMBLE THE DATAPOINT
            // Consumes the datapoint from phase 0. Produces dpValue, dpTagName, dpTimestamp, dpHasMetadata,
            // dpMetadata and dpContext. This is the only phase that knows DataPointWithMetadata exists: it
            // resolves the kind once, so no later phase asks again. A plain datapoint has no time, metadata
            // or context of its own, so the time becomes now and the other two stay null.
            //
            // dpHasMetadata is separate from dpMetadata because the two mean different things: a datapoint
            // that carries a metadata aspect but no metadata in it publishes `metadata: null`, while a plain
            // datapoint publishes no such field at all.
            final JsonNode dpValue;
            final String dpTagName = dataPoint.getTagName();
            final long dpTimestamp;
            final boolean dpHasMetadata;
            final JsonNode dpMetadata;
            final JsonNode dpContext;
            if (dataPoint instanceof final DataPointWithMetadata dpMeta) {
                dpValue = dpMeta.getTagValue();
                dpTimestamp = dpMeta.getTimestamp();
                dpHasMetadata = true;
                dpMetadata = dpMeta.getMetadata()
                        .map(metadata -> objectMapper.convertValue(metadata, JsonNode.class))
                        .orElse(null);
                dpContext = dpMeta.getContext().orElse(null);
            } else {
                if (dataPoint.treatTagValueAsJson()) {
                    dpValue = objectMapper.readTree((String) dataPoint.getTagValue());
                    if (dpValue == null) {
                        throw new RuntimeException("No value entry in JSON message");
                    }
                } else {
                    dpValue = objectMapper.convertValue(dataPoint.getTagValue(), JsonNode.class);
                }
                dpTimestamp = System.currentTimeMillis();
                dpHasMetadata = false;
                dpMetadata = null;
                dpContext = null;
            }

            // PHASE 3 -- DISASSEMBLE THE VALUE
            // Consumes dpValue alone. Produces value and the five parameters of a method response:
            // methodName, errorCode, statusInfo, responseTopic and correlationData. The other four aspects
            // bypass this phase untouched, which is the statement that handling a method response cannot
            // affect the envelope.
            //
            // A scalar or an array is the value itself. An object may carry the response parameters, which
            // are taken out of it. This phase only separates -- it reads each parameter as the adapter wrote
            // it, correlationData with its prefix still on, and leaves every translation to the consumer.
            //
            // The five are a closed set. A parameter Edge does not know is ignored rather than forwarded:
            // what a response publishes is Edge's contract with the subscriber, not the adapter's.
            final JsonNode value;
            String methodName = null;
            String errorCode = null;
            JsonNode statusInfo = null;
            String responseTopic = null;
            String correlationData = null;
            if (dpValue instanceof final ObjectNode valueWithResponseParameters
                    && valueWithResponseParameters.get(INTERNAL_RESPONSE_PARAMETERS)
                            instanceof final ObjectNode responseParameters) {

                final ObjectNode outputs = JsonNodeFactory.instance.objectNode();
                for (final var p : valueWithResponseParameters.properties()) {
                    if (!p.getKey().equals(INTERNAL_RESPONSE_PARAMETERS)) {
                        outputs.set(p.getKey(), p.getValue());
                    }
                }
                methodName = textOrNull(responseParameters.get(METHOD_NAME));
                errorCode = textOrNull(responseParameters.get(ERROR_CODE));

                // Null on failure, the empty object on a success that returns nothing. Deciding this on
                // `errorCode` rather than on emptiness is what keeps the two apart: a method declaring no
                // outputs succeeds with nothing in `value`, and reporting that as null would say the call
                // did not run.
                //
                // A failure that nevertheless carried outputs keeps them, which the documented rule does not
                // allow for. The device said something, and publishing null would delete it to satisfy a
                // shape -- so the rule yields here rather than the information.
                value = errorCode != null && outputs.isEmpty() ? JsonNodeFactory.instance.nullNode() : outputs;
                statusInfo = responseParameters.get(STATUS_INFO);
                correlationData = textOrNull(responseParameters.get(CORRELATION_DATA));
                // A blank reply address counts as none, so the mapping's own topic stands.
                final String address = textOrNull(responseParameters.get(RESPONSE_TOPIC));
                responseTopic = address != null && !address.isBlank() ? address : null;
            } else {
                value = dpValue;
            }

            // PHASE 4 -- ASSEMBLE THE MESSAGE
            // Consumes the aspects from phase 2 and value plus four of the five parameters from phase 3 --
            // responseTopic is not published, because a response must not carry a routing instruction for
            // itself. Produces jsonToSend and topic.
            //
            // The two streams rejoin here, so this is where the collision rule lives: the aspects are
            // written after the parameters, so an aspect always wins a name clash -- a parameter that
            // silently replaced `value` would be far harder to diagnose than one dropped.
            final ObjectNode node = JsonNodeFactory.instance.objectNode();
            if (methodName != null) {
                node.put(METHOD_NAME, methodName);
            }
            if (errorCode != null) {
                node.put(ERROR_CODE, errorCode);
            }
            if (statusInfo != null) {
                node.set(STATUS_INFO, statusInfo);
            }
            if (correlationData != null) {
                // `raw:` goes, `base64:` stays. The caller typed the raw value and must get back exactly
                // that; the base64 one is bytes a JSON string cannot hold, and the prefix tells a subscriber
                // reading the payload to decode rather than compare.
                node.put(
                        CORRELATION_DATA,
                        correlationData.startsWith(RAW_PREFIX)
                                ? correlationData.substring(RAW_PREFIX.length())
                                : correlationData);
            }
            node.set(VALUE, value);
            if (northboundMapping.getIncludeTimestamp()) {
                node.set("timestamp", JsonNodeFactory.instance.numberNode(dpTimestamp));
            }
            if (northboundMapping.getIncludeMetadata() && dpHasMetadata) {
                node.set("metadata", dpMetadata != null ? dpMetadata : JsonNodeFactory.instance.nullNode());
            }
            if (northboundMapping.getIncludeTagNames()) {
                node.set("tagName", JsonNodeFactory.instance.textNode(dpTagName));
            }
            if (!northboundMapping.getUserProperties().isEmpty()) {
                node.set(
                        "mqttUserProperties",
                        objectMapper.convertValue(northboundMapping.getUserProperties(), JsonNode.class));
            }
            if (dpContext != null) {
                node.set(CONTEXT, dpContext);
            }
            final byte[] jsonToSend = objectMapper.writeValueAsBytes(node);
            final String topic = responseTopic != null ? responseTopic : northboundMapping.getMqttTopic();

            // PHASE 5 -- PUBLISH
            // Consumes jsonToSend and topic from phase 4 and correlationData from phase 3. Produces nothing:
            // it sends, counts the outcome, and fires an event on the first sample.
            //
            // The implementation type, not the SDK interface, because the correlation data goes into the
            // MQTT 5 property and that method is deliberately not on the interface -- see
            // ProtocolAdapterPublishBuilderImpl.withCorrelationData. Edge already casts the same way in
            // ModuleServicesPerModuleImpl.
            final ProtocolAdapterPublishBuilderImpl publishBuilder =
                    (ProtocolAdapterPublishBuilderImpl) protocolAdapterPublishService.createPublish();
            publishBuilder
                    .withTopic(topic)
                    .withQoS(northboundMapping.getMqttQos())
                    .withPayload(jsonToSend)
                    .withAdapter(protocolAdapter.getAdapter());
            if (correlationData != null) {
                // The property is binary, so both prefixes come off and `base64:` is decoded. This is what
                // makes the round trip byte-identical for a caller who set the property on the request.
                publishBuilder.withCorrelationData(correlationDataAsBytes(correlationData));
            }
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

    /// The encoding prefixes a correlation token carries while it travels through Edge.
    ///
    /// Edge's own bookkeeping, not part of what a caller sent: `raw:` marks text the caller typed into
    /// the request payload, `base64:` marks bytes they set as the MQTT 5 `Correlation Data` property,
    /// which is binary and cannot cross as text unencoded. Phase 3 of [#accept] reverses both for the
    /// MQTT 5 property; only `base64:` survives into the payload.
    private static final @NotNull String RAW_PREFIX = "raw:";

    private static final @NotNull String BASE64_PREFIX = "base64:";

    /// The text of a response parameter, or null when the response did not carry it or carried a non-text.
    private static @Nullable String textOrNull(final @Nullable JsonNode parameter) {
        return parameter != null && parameter.isTextual() ? parameter.textValue() : null;
    }

    /// The correlation data as the bytes the MQTT 5 property takes, with Edge's prefix removed.
    ///
    /// `base64:` marks bytes a caller set as the property on the request, which only survive the response
    /// parameters as text; decoding them is what makes the round trip byte-identical. Text that claims to be
    /// base64 and is not passes through as it stands -- nothing Edge writes does that, and the alternative
    /// to passing it through is dropping the correlation entirely.
    private static byte @NotNull [] correlationDataAsBytes(final @NotNull String correlationData) {
        if (correlationData.startsWith(BASE64_PREFIX)) {
            final String encoded = correlationData.substring(BASE64_PREFIX.length());
            try {
                return Base64.getDecoder().decode(encoded);
            } catch (final IllegalArgumentException e) {
                return encoded.getBytes(StandardCharsets.UTF_8);
            }
        }
        if (correlationData.startsWith(RAW_PREFIX)) {
            return correlationData.substring(RAW_PREFIX.length()).getBytes(StandardCharsets.UTF_8);
        }
        return correlationData.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public @Nullable String getScope() {
        return protocolAdapter.getId();
    }
}
