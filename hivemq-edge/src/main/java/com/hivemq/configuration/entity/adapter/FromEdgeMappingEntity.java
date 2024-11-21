package com.hivemq.configuration.entity.adapter;

import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.protocols.FromEdgeMapping;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.ArrayList;
import java.util.List;

public class FromEdgeMappingEntity {

    @XmlElement(name = "topic", required = true)
    private final @NotNull String topic;

    @XmlElement(name = "tag-name", required = true)
    private final @NotNull String tagName;

    @XmlElement(name = "max-qos", required = true)
    private final int maxQoS;

    @XmlElement(name = "messageHandlingOptions", required = true)
    private final @NotNull MessageHandlingOptions messageHandlingOptions;

    @XmlElement(name = "includeTagNames", required = true)
    private final boolean includeTagNames;

    @XmlElement(name = "includeTimestamp", required = true)
    private final boolean includeTimestamp;

    @XmlElementWrapper(name = "includeTagNames", required = true)
    @XmlElement(name = "mqttUserProperties")
    private final @NotNull List<MqttUserProperty> userProperties;

    // no-arg constructor for JaxB
    public FromEdgeMappingEntity() {
        topic = "";
        tagName = "";
        messageHandlingOptions = MessageHandlingOptions.MQTTMessagePerSubscription;
        includeTagNames = true;
        includeTimestamp = true;
        maxQoS = 2;
        userProperties = new ArrayList<>();
    }

    public FromEdgeMappingEntity(
            @NotNull final String tagName,
            @NotNull final String topic,
            final int maxQoS,
            final @NotNull MessageHandlingOptions messageHandlingOptions,
            final boolean includeTagNames,
            final boolean includeTimestamp,
            final @NotNull List<MqttUserProperty> userProperties) {
        this.tagName = tagName;
        this.topic = topic;
        this.maxQoS = maxQoS;
        this.messageHandlingOptions = messageHandlingOptions;
        this.includeTagNames = includeTagNames;
        this.includeTimestamp = includeTimestamp;
        this.userProperties = userProperties;
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public @NotNull String getTopic() {
        return topic;
    }

    public @NotNull MessageHandlingOptions getMessageHandlingOptions() {
        return messageHandlingOptions;
    }

    public boolean isIncludeTagNames() {
        return includeTagNames;
    }

    public boolean isIncludeTimestamp() {
        return includeTimestamp;
    }

    public @NotNull List<MqttUserProperty> getUserProperties() {
        return userProperties;
    }

    public int getMaxQoS() {
        return maxQoS;
    }

    public static @NotNull FromEdgeMappingEntity from(final @NotNull FromEdgeMapping fromEdgeMapping) {
        return new FromEdgeMappingEntity(fromEdgeMapping.getTagName(),
                fromEdgeMapping.getTopic(),
                fromEdgeMapping.getMaxQoS(),
                fromEdgeMapping.getMessageHandlingOptions(),
                fromEdgeMapping.getIncludeTagNames(),
                fromEdgeMapping.getIncludeTimestamp(),
                fromEdgeMapping.getUserProperties());
    }
}
