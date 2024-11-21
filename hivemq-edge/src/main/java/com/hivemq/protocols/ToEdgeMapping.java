package com.hivemq.protocols;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.configuration.entity.adapter.ToEdgeMappingEntity;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.fieldmapping.FieldMappings;

public class ToEdgeMapping {

    private final @NotNull String topicFilter;
    private final @NotNull String tagName;
    private final int maxQoS;
    private final @NotNull FieldMappings fieldMappings;


    public ToEdgeMapping(
            final @NotNull String tagName,
            final @NotNull String topicFilter,
            final int maxQoS,
            final @NotNull FieldMappings fieldMappings) {
        this.tagName = tagName;
        this.topicFilter = topicFilter;
        this.maxQoS = maxQoS;
        this.fieldMappings = fieldMappings;

    }

    public @NotNull String getTopicFilter() {
        return topicFilter;
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public @NotNull FieldMappings getFieldMappings() {
        return fieldMappings;
    }

    public static @NotNull ToEdgeMapping fromEntity(
            final @NotNull ToEdgeMappingEntity toEdgeMappingEntity, final @NotNull ObjectMapper objectMapper) {
        return new ToEdgeMapping(toEdgeMappingEntity.getTagName(),
                toEdgeMappingEntity.getTopicFilter(),
                toEdgeMappingEntity.getMaxQos(),
                FieldMappings.fromEntity(toEdgeMappingEntity.getFieldMappingsEntity(), objectMapper));
    }

    public int getMaxQoS() {
        return maxQoS;
    }
}
