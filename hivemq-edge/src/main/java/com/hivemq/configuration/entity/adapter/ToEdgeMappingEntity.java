package com.hivemq.configuration.entity.adapter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.protocols.ToEdgeMapping;

import javax.xml.bind.annotation.XmlElement;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class ToEdgeMappingEntity {

    @XmlElement(name = "topic-filter", required = true)
    private final @NotNull String topicFilter;

    @XmlElement(name = "tag-name", required = true)
    private final @NotNull String tagName;

    @XmlElement(name = "max-qos", required = true)
    private final int maxQoS;

    @XmlElement(name = "fieldMappings", required = true)
    private final @NotNull FieldMappingsEntity fieldMappingsEntity;

    // no-arg constructor for JaxB
    public ToEdgeMappingEntity() {
        topicFilter = "";
        tagName = "";
        maxQoS = 2;
        fieldMappingsEntity = null;
    }

    public ToEdgeMappingEntity(
            final @NotNull String tagName,
            final @NotNull String topicFilter,
            final int maxQoS,
            final @NotNull FieldMappingsEntity fieldMappingsEntity) {
        this.tagName = tagName;
        this.topicFilter = topicFilter;
        this.maxQoS = maxQoS;
        this.fieldMappingsEntity = fieldMappingsEntity;
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public @NotNull String getTopicFilter() {
        return topicFilter;
    }

    public int getMaxQoS() {
        return maxQoS;
    }

    public @NotNull FieldMappingsEntity getFieldMappingsEntity() {
        return fieldMappingsEntity;
    }

    public static @NotNull ToEdgeMappingEntity from(final @NotNull ToEdgeMapping toEdgeMapping) {
        return new ToEdgeMappingEntity(toEdgeMapping.getTagName(),
                toEdgeMapping.getTopicFilter(),
                toEdgeMapping.getMaxQoS(),
                FieldMappingsEntity.from(toEdgeMapping.getFieldMappings()));
    }
}
