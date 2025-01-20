package com.hivemq.protocols;

import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.persistence.mappings.NorthboundMapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

class PollingContextWrapper implements PollingContext {
    private final @NotNull String topic;
    private final @NotNull String tagName;
    private final @NotNull MessageHandlingOptions messageHandlingOptions;
    private final boolean includeTagNames;
    private final boolean includeTimestamp;
    private final @NotNull List<MqttUserProperty> userProperties;
    private final int maxQoS;
    private final long messageExpiryInterval;

    public PollingContextWrapper(
            final String topic,
            final String tagName,
            final MessageHandlingOptions messageHandlingOptions,
            final boolean includeTagNames,
            final boolean includeTimestamp,
            final List<MqttUserProperty> userProperties,
            final int maxQoS,
            final long messageExpiryInterval) {
        this.topic = topic;
        this.tagName = tagName;
        this.messageHandlingOptions = messageHandlingOptions;
        this.includeTagNames = includeTagNames;
        this.includeTimestamp = includeTimestamp;
        this.userProperties = userProperties;
        this.maxQoS = maxQoS;
        this.messageExpiryInterval = messageExpiryInterval;
    }

    @Override
    public @NotNull String getMqttTopic() {
        return topic;
    }

    @Override
    public @NotNull String getTagName() {
        return tagName;
    }

    @Override
    public int getMqttQos() {
        return maxQoS;
    }

    @Override
    public @NotNull MessageHandlingOptions getMessageHandlingOptions() {
        return messageHandlingOptions;
    }

    @Override
    public @NotNull Boolean getIncludeTimestamp() {
        return includeTimestamp;
    }

    @Override
    public @NotNull Boolean getIncludeTagNames() {
        return includeTagNames;
    }

    @Override
    public @NotNull List<MqttUserProperty> getUserProperties() {
        return userProperties;
    }

    @Override
    public @Nullable Long getMessageExpiryInterval() {
        return messageExpiryInterval;
    }

    public static @NotNull PollingContextWrapper from(final NorthboundMapping northboundMapping) {
        return new PollingContextWrapper(northboundMapping.getMqttTopic(),
                northboundMapping.getTagName(),
                northboundMapping.getMessageHandlingOptions(),
                northboundMapping.getIncludeTagNames(),
                northboundMapping.getIncludeTimestamp(),
                List.copyOf(northboundMapping.getUserProperties()),
                northboundMapping.getMqttQos(),
                northboundMapping.getMessageExpiryInterval());
    }
}
