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
package com.hivemq.extensions.services.builder;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.general.UserProperty;
import com.hivemq.extension.sdk.api.packets.publish.PayloadFormatIndicator;
import com.hivemq.extension.sdk.api.packets.publish.PublishPacket;
import com.hivemq.extension.sdk.api.services.builder.PublishBuilder;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;
import com.hivemq.extension.sdk.api.services.publish.Publish;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.extensions.packets.publish.PublishPacketImpl;
import com.hivemq.extensions.services.publish.PublishImpl;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.util.Topics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jakarta.inject.Inject;
import java.nio.ByteBuffer;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.mqtt.message.publish.PUBLISH.MESSAGE_EXPIRY_INTERVAL_NOT_SET;

/**
 * @author Lukas Brandl
 */
public class PublishBuilderImpl implements PublishBuilder {

    @NotNull
    private Qos qos = Qos.AT_MOST_ONCE;

    private boolean retain = false;

    @Nullable
    private String topic;

    @Nullable
    private PayloadFormatIndicator payloadFormatIndicator;

    @NotNull
    private Long messageExpiryInterval = PUBLISH.MESSAGE_EXPIRY_INTERVAL_NOT_SET;

    @Nullable
    private String responseTopic;

    @Nullable
    private ByteBuffer correlationData;

    @Nullable
    private String contentType;

    @Nullable
    private ByteBuffer payload;

    @NotNull
    private final ImmutableList.Builder<MqttUserProperty> userPropertyBuilder = ImmutableList.builder();

    @NotNull
    private final MqttConfigurationService mqttConfigurationService;

    @NotNull
    private final SecurityConfigurationService securityConfigurationService;

    @Inject
    public PublishBuilderImpl(final @NotNull ConfigurationService configurationService) {
        this.mqttConfigurationService = configurationService.mqttConfiguration();
        this.securityConfigurationService = configurationService.securityConfiguration();
    }

    @NotNull
    @Override
    public PublishBuilder fromPublish(final @NotNull PublishPacket publish) {

        Preconditions.checkNotNull(publish, "publish must not be null");

        if (!(publish instanceof PublishPacketImpl)) {
            throw new DoNotImplementException(PublishPacket.class.getSimpleName());
        }

        return fromComplete(publish.getQos(),
                publish.getRetain(),
                publish.getTopic(),
                publish.getPayloadFormatIndicator(),
                publish.getMessageExpiryInterval(),
                publish.getResponseTopic(),
                publish.getCorrelationData(),
                publish.getContentType(),
                publish.getPayload(),
                publish.getUserProperties());
    }

    @NotNull
    @Override
    public PublishBuilder fromPublish(final @NotNull Publish publish) {

        Preconditions.checkNotNull(publish, "publish must not be null");

        if (!(publish instanceof PublishImpl)) {
            throw new DoNotImplementException(Publish.class.getSimpleName());
        }

        return fromComplete(publish.getQos(),
                publish.getRetain(),
                publish.getTopic(),
                publish.getPayloadFormatIndicator(),
                publish.getMessageExpiryInterval(),
                publish.getResponseTopic(),
                publish.getCorrelationData(),
                publish.getContentType(),
                publish.getPayload(),
                publish.getUserProperties());
    }

    public @NotNull PublishBuilder fromPublish(@NotNull final PUBLISH publish) {
        Preconditions.checkNotNull(publish, "publish must not be null");
        this.qos = publish.getQoS().toQos();
        this.retain(publish.isRetain());
        if (publish.getPayloadFormatIndicator() != null) {
            this.payloadFormatIndicator = publish.getPayloadFormatIndicator().toPayloadFormatIndicator();
        }
        this.messageExpiryInterval(publish.getMessageExpiryInterval());
        this.responseTopic(publish.getResponseTopic());
        if (publish.getCorrelationData() != null) {
            this.correlationData(ByteBuffer.wrap(publish.getCorrelationData()));
        }

        this.contentType(publish.getContentType());
        if (publish.getPayload() != null) {
            this.payload = ByteBuffer.wrap(publish.getPayload());

        }
        for (final UserProperty userProperty : publish.getUserProperties().asList()) {
            this.userProperty(userProperty.getName(), userProperty.getValue());
        }
        return this;
    }


    @NotNull
    private PublishBuilder fromComplete(
            final @NotNull Qos qos,
            final boolean retain,
            final @NotNull String topic,
            final @NotNull Optional<PayloadFormatIndicator> payloadFormatIndicator,
            final @NotNull Optional<Long> messageExpiryInterval,
            final @NotNull Optional<String> responseTopic,
            final @NotNull Optional<ByteBuffer> correlationData,
            final @NotNull Optional<String> contentType,
            final @NotNull Optional<ByteBuffer> payload,
            final @NotNull UserProperties userProperties) {
        this.qos(qos);
        this.retain(retain);
        this.topic(topic);
        this.payloadFormatIndicator(payloadFormatIndicator.orElse(null));
        this.messageExpiryInterval(messageExpiryInterval.orElse(PUBLISH.MESSAGE_EXPIRY_INTERVAL_NOT_SET));
        this.responseTopic(responseTopic.orElse(null));
        this.correlationData(correlationData.orElse(null));
        this.contentType(contentType.orElse(null));
        this.payload = payload.orElse(null);
        for (final UserProperty userProperty : userProperties.asList()) {
            this.userProperty(userProperty.getName(), userProperty.getValue());
        }
        return this;
    }

    @NotNull
    @Override
    public PublishBuilder qos(final @NotNull Qos qos) {
        PluginBuilderUtil.checkQos(qos, mqttConfigurationService.maximumQos().getQosNumber());
        this.qos = qos;
        return this;
    }

    @NotNull
    @Override
    public PublishBuilder retain(final boolean retain) {
        if (!mqttConfigurationService.retainedMessagesEnabled() && retain) {
            throw new IllegalArgumentException("Retained messages are disabled");
        }
        this.retain = retain;
        return this;
    }

    @NotNull
    @Override
    public PublishBuilder topic(final @NotNull String topic) {
        checkNotNull(topic, "Topic must not be null");

        if (!Topics.isValidTopicToPublish(topic)) {
            throw new IllegalArgumentException("The topic (" + topic + ") is invalid for PUBLISH messages ");
        }

        if (!PluginBuilderUtil.isValidUtf8String(topic, securityConfigurationService.validateUTF8())) {
            throw new IllegalArgumentException("The topic (" + topic + ") is UTF-8 malformed");
        }

        this.topic = topic;
        return this;
    }

    @NotNull
    @Override
    public PublishBuilder payloadFormatIndicator(final @Nullable PayloadFormatIndicator payloadFormatIndicator) {
        this.payloadFormatIndicator = payloadFormatIndicator;
        return this;
    }

    @NotNull
    @Override
    public PublishBuilder messageExpiryInterval(final long messageExpiryInterval) {
        PluginBuilderUtil.checkMessageExpiryInterval(messageExpiryInterval,
                mqttConfigurationService.maxMessageExpiryInterval());
        this.messageExpiryInterval = messageExpiryInterval;
        return this;
    }

    @NotNull
    @Override
    public PublishBuilder responseTopic(final @Nullable String responseTopic) {
        PluginBuilderUtil.checkResponseTopic(responseTopic, securityConfigurationService.validateUTF8());
        this.responseTopic = responseTopic;
        return this;
    }

    @NotNull
    @Override
    public PublishBuilder correlationData(final @Nullable ByteBuffer correlationData) {
        this.correlationData = correlationData;
        return this;
    }

    @NotNull
    @Override
    public PublishBuilder contentType(final @Nullable String contentType) {
        PluginBuilderUtil.checkContentType(contentType, securityConfigurationService.validateUTF8());
        this.contentType = contentType;
        return this;
    }

    @NotNull
    @Override
    public PublishBuilder payload(final @NotNull ByteBuffer payload) {
        checkNotNull(payload, "Payload must not be null");
        this.payload = payload;
        return this;
    }

    @NotNull
    @Override
    public PublishBuilder userProperty(final @NotNull String name, final @NotNull String value) {
        PluginBuilderUtil.checkUserProperty(name, value, securityConfigurationService.validateUTF8());
        this.userPropertyBuilder.add(new MqttUserProperty(name, value));
        return this;
    }

    @NotNull
    @Override
    public Publish build() {

        checkNotNull(topic, "Topic must never be null");
        checkNotNull(payload, "Payload must never be null");

        if (messageExpiryInterval == MESSAGE_EXPIRY_INTERVAL_NOT_SET) {
            messageExpiryInterval = mqttConfigurationService.maxMessageExpiryInterval();
        }

        return new PublishImpl(qos,
                retain,
                topic,
                payloadFormatIndicator,
                messageExpiryInterval,
                responseTopic,
                correlationData,
                contentType,
                payload,
                UserPropertiesImpl.of(userPropertyBuilder.build()));
    }
}
