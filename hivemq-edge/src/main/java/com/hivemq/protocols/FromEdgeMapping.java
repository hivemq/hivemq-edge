package com.hivemq.protocols;

import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.configuration.entity.adapter.FromEdgeMappingEntity;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class FromEdgeMapping implements PollingContext {

    private final @NotNull String topic;
    private final @NotNull String tagName;
    private final @NotNull MessageHandlingOptions messageHandlingOptions;
    private final boolean includeTagNames;
    private final boolean includeTimestamp;
    private final @NotNull List<MqttUserProperty> userProperties;
    private final int maxQoS;

    public FromEdgeMapping(
            @NotNull final String tagName,
            @NotNull final String topic,
            final int maxQoS,
            final @NotNull MessageHandlingOptions messageHandlingOptions,
            final boolean includeTagNames,
            final boolean includeTimestamp,
            final @NotNull List<MqttUserProperty> userProperties) {
        this.tagName = tagName;
        this.topic = topic;
        this.messageHandlingOptions = messageHandlingOptions;
        this.includeTagNames = includeTagNames;
        this.includeTimestamp = includeTimestamp;
        this.userProperties = userProperties;
        this.maxQoS = maxQoS;

    }

    @Override
    public @NotNull String getMqttTopic() {
        return topic;
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    @Override
    public int getMqttQos() {
        return 0;
    }

    public int getMaxQoS() {
        return maxQoS;
    }

    public @NotNull MessageHandlingOptions getMessageHandlingOptions() {
        return messageHandlingOptions;
    }

    public @NotNull Boolean getIncludeTimestamp() {
        return includeTimestamp;
    }

    public @NotNull Boolean getIncludeTagNames() {
        return includeTagNames;
    }

    public @NotNull List<MqttUserProperty> getUserProperties() {
        return userProperties;
    }

    public static @NotNull FromEdgeMapping fromEntity(final @NotNull FromEdgeMappingEntity entity) {
        final List<MqttUserProperty> mqttUserProperties = entity.getUserProperties()
                .stream()
                .map(mqttUserPropertyEntity -> new MqttUserProperty(mqttUserPropertyEntity.getName(),
                        mqttUserPropertyEntity.getValue()))
                .collect(Collectors.toList());

        return new FromEdgeMapping(entity.getTagName(),
                entity.getTopic(),
                entity.getMaxQoS(),
                entity.getMessageHandlingOptions(),
                entity.isIncludeTagNames(),
                entity.isIncludeTimestamp(),
                mqttUserProperties);
    }


}
