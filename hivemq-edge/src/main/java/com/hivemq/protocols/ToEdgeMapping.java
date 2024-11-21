package com.hivemq.protocols;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.configuration.entity.adapter.ToEdgeMappingEntity;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.fieldmapping.FieldMappings;

import java.util.List;

public class ToEdgeMapping implements PollingContext {

    private final @NotNull String topicFilter;
    private final @NotNull String tagName;
    private final @NotNull FieldMappings fieldMappings;


    public ToEdgeMapping(
            final @NotNull String tagName,
            final @NotNull String topicFilter,
            final @NotNull FieldMappings fieldMappings) {
        this.tagName = tagName;
        this.topicFilter = topicFilter;
        this.fieldMappings = fieldMappings;

    }

    @Override
    public @NotNull String getMqttTopic() {
        return topicFilter;
    }

    public @NotNull String getTagName() {
        return tagName;
    }


    public @NotNull FieldMappings getFieldMappings() {
        return fieldMappings;
    }


    public int getMaxQoS() {
        return maxQoS;
    }

    public static @NotNull ToEdgeMapping fromEntity(
            final @NotNull ToEdgeMappingEntity toEdgeMappingEntity, final @NotNull ObjectMapper objectMapper) {
        return new ToEdgeMapping(toEdgeMappingEntity.getTagName(),
                toEdgeMappingEntity.getTopicFilter(),
                FieldMappings.fromEntity(toEdgeMappingEntity.getFieldMappingsEntity(), objectMapper));
    }
}
