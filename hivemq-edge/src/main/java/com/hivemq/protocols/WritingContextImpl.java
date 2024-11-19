package com.hivemq.protocols;

import com.hivemq.persistence.fieldmapping.FieldMappings;
import org.jetbrains.annotations.NotNull;

public class WritingContextImpl implements InternalWritingContext {

    private final @NotNull String tagName;
    private final @NotNull String topicFilter;
    private final int mqttMaxQoS;
    private final @com.hivemq.extension.sdk.api.annotations.NotNull FieldMappings fieldMappings;

    public WritingContextImpl(
            final @NotNull String tagName,
            final @NotNull String topicFilter,
            final int mqttMaxQoS,
            final @NotNull FieldMappings fieldMappings) {
        this.tagName = tagName;
        this.topicFilter = topicFilter;
        this.mqttMaxQoS = mqttMaxQoS;
        this.fieldMappings = fieldMappings;
    }

    @Override
    public @NotNull String getTagName() {
        return tagName;
    }

    @Override
    public @NotNull String getMqttTopicFilter() {
        return topicFilter;
    }

    @Override
    public int getMqttMaxQos() {
        return mqttMaxQoS;
    }

    @Override
    public @NotNull FieldMappings getFieldMappings() {
        return fieldMappings;
    }
}
