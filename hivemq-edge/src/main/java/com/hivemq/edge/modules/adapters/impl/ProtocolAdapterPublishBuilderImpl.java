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
package com.hivemq.edge.modules.adapters.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterPublishBuilder;
import com.hivemq.adapter.sdk.api.ProtocolPublishResult;
import com.hivemq.api.mqtt.PublishReturnCode;
import com.hivemq.mqtt.handler.publish.PublishingResult;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProtocolAdapterPublishBuilderImpl implements ProtocolAdapterPublishBuilder {

    private final @NotNull String hivemqId;
    private final @NotNull SendCallback sendCallback;

    private final PUBLISHFactory.Mqtt5Builder builder = new PUBLISHFactory.Mqtt5Builder();
    private final ImmutableList.Builder<MqttUserProperty> userProperties = ImmutableList.builder();
    private final ImmutableMap.Builder<String, String> dynamicContext = ImmutableMap.builder();
    private @Nullable ProtocolAdapter adapter;

    public ProtocolAdapterPublishBuilderImpl(final @NotNull String hivemqId, final @NotNull SendCallback sendCallback) {
        this.hivemqId = hivemqId;
        this.sendCallback = sendCallback;
    }

    @Override
    public @NotNull ProtocolAdapterPublishBuilder withTopic(final @NotNull String mqttTopic) {
        builder.withTopic(mqttTopic);
        return this;
    }

    @Override
    public @NotNull ProtocolAdapterPublishBuilder withPayload(final @NotNull byte[] payload) {
        builder.withPayload(payload);
        return this;
    }

    @Override
    public @NotNull ProtocolAdapterPublishBuilder withQoS(final int qos) {
        final QoS qoS = requireNonNull(QoS.valueOf(qos));
        builder.withQoS(qoS);
        builder.withOnwardQos(qoS);
        return this;
    }

    @Override
    public @NotNull ProtocolAdapterPublishBuilder withMessageExpiryInterval(final long messageExpiryInterval) {
        builder.withMessageExpiryInterval(messageExpiryInterval);
        return this;
    }

    @Override
    public @NotNull ProtocolAdapterPublishBuilder withUserProperty(
            final @NotNull String name, final @NotNull String value) {
        userProperties.add(new MqttUserProperty(name, value));
        return this;
    }

    /**
     * Sets the MQTT 5 {@code Correlation Data} property.
     * <p>
     * <b>Not on {@link ProtocolAdapterPublishBuilder}, deliberately.</b> That interface is in the adapter SDK,
     * which stays byte-identical to master this release, and an adapter has no business setting this anyway:
     * correlating a response with its request is Edge's concern, decided from the mapping and the request. So
     * this is declared on the implementation and the one Edge-internal caller
     * ({@code NorthboundTagConsumer}) holds that type rather than the interface.
     * <p>
     * The property is additive and costs an MQTT 3 subscriber nothing: the version-5 publish encoder writes it,
     * the version-3 encoder has no field for it and never reads it, so one message serves both from the same
     * object. The token also stays in the payload, which is where an MQTT 3 caller reads it.
     */
    public @NotNull ProtocolAdapterPublishBuilderImpl withCorrelationData(final byte @NotNull [] correlationData) {
        builder.withCorrelationData(correlationData);
        return this;
    }

    @Override
    public @NotNull ProtocolAdapterPublishBuilder withRetain(final boolean retained) {
        builder.withRetain(retained);
        return this;
    }

    @Override
    public @NotNull ProtocolAdapterPublishBuilder withContextInformation(
            final @NotNull String key, final @NotNull String value) {
        dynamicContext.put(key, value);
        return this;
    }

    @Override
    public @NotNull CompletableFuture<ProtocolPublishResult> send() {
        final PUBLISH publish = builder.withHivemqId(hivemqId)
                .withUserProperties(Mqtt5UserProperties.of(userProperties.build()))
                .build();
        final CompletableFuture<PublishingResult> publishSend =
                sendCallback.onPublishSend(publish, requireNonNull(adapter), dynamicContext.buildKeepingLast());
        return publishSend.thenApply(publishingResult -> {
            final PublishReturnCode publishReturnCode = publishingResult.getPublishReturnCode();
            return switch (publishReturnCode) {
                case DELIVERED -> ProtocolPublishResult.DELIVERED;
                case NO_MATCHING_SUBSCRIBERS -> ProtocolPublishResult.NO_MATCHING_SUBSCRIBERS;
                case FAILED -> ProtocolPublishResult.FAILED;
            };
        });
    }

    @Override
    public @NotNull ProtocolAdapterPublishBuilder withAdapter(final @NotNull ProtocolAdapter adapter) {
        this.adapter = adapter;
        return this;
    }

    public interface SendCallback {
        @NotNull
        CompletableFuture<PublishingResult> onPublishSend(
                final @NotNull PUBLISH publish,
                final @NotNull ProtocolAdapter protocolAdapter,
                final @NotNull ImmutableMap<String, String> dynamicContext);
    }
}
