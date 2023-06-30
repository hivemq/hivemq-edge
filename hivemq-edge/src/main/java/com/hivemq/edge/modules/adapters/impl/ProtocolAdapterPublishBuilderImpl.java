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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapter;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPublishBuilder;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.handler.publish.PublishReturnCode;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public class ProtocolAdapterPublishBuilderImpl implements ProtocolAdapterPublishBuilder {

    private final @NotNull String hivemqId;
    private final @NotNull SendCallback sendCallback;

    private final PUBLISHFactory.Mqtt5Builder builder = new PUBLISHFactory.Mqtt5Builder();
    private final ImmutableList.Builder<MqttUserProperty> userProperties = ImmutableList.builder();
    private final ImmutableMap.Builder<String, String> dynamicContext = ImmutableMap.builder();
    private @Nullable ProtocolAdapter adapter;

    public ProtocolAdapterPublishBuilderImpl(
            final @NotNull String hivemqId, final @NotNull SendCallback sendCallback) {
        this.hivemqId = hivemqId;
        this.sendCallback = sendCallback;
    }

    @Override
    public @NotNull ProtocolAdapterPublishBuilder withTopic(@NotNull final String mqttTopic) {
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
    public @NotNull CompletableFuture<PublishReturnCode> send() {

        final PUBLISH publish = builder.withHivemqId(hivemqId)
                .withUserProperties(Mqtt5UserProperties.of(userProperties.build()))
                .build();

        return sendCallback.onPublishSend(publish, Objects.requireNonNull(adapter), dynamicContext.buildKeepingLast());
    }

    public @NotNull ProtocolAdapterPublishBuilder withAdapter(final @NotNull ProtocolAdapter adapter) {
        this.adapter = adapter;
        return this;
    }

    public interface SendCallback {
        @NotNull CompletableFuture<PublishReturnCode> onPublishSend(
                final @NotNull PUBLISH publish,
                final @NotNull ProtocolAdapter protocolAdapter,
                final @NotNull ImmutableMap<String, String> dynamicContext);
    }
}
