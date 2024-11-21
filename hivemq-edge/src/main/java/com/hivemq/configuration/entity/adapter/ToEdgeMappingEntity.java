package com.hivemq.configuration.entity.adapter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.protocols.ToEdgeMapping;

import javax.xml.bind.annotation.XmlElement;

@SuppressWarnings("unused")
public class ToEdgeMappingEntity {

    @XmlElement(name = "topic-filter", required = true)
    private final @NotNull String topicFilter;

    @XmlElement(name = "tag-name", required = true)
    private final @NotNull String tagName;

    @XmlElement(name = "max-qos", required = true)
    private final int qos;

    @XmlElement(name = "fieldMappings", required = true)
    private final @NotNull FieldMappingsEntity fieldMappingsEntity;

    // no-arg constructor for JaxB
    public ToEdgeMappingEntity() {
        topicFilter = "";
        tagName = "";
        qos = 1;
        fieldMappingsEntity = null;
    }

    public ToEdgeMappingEntity(
            final @NotNull String tagName,
            final @NotNull String topicFilter,
            final int maxQoS,
            final @NotNull FieldMappingsEntity fieldMappingsEntity) {
        this.tagName = tagName;
        this.topicFilter = topicFilter;
        this.fieldMappingsEntity = fieldMappingsEntity;
        this.qos = maxQoS;
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public @NotNull String getTopicFilter() {
        return topicFilter;
    }

    public @NotNull FieldMappingsEntity getFieldMappingsEntity() {
        return fieldMappingsEntity;
    }

    public int getMaxQos() {
        return qos;
    }

    public static @NotNull ToEdgeMappingEntity from(final @NotNull ToEdgeMapping toEdgeMapping) {
        return new ToEdgeMappingEntity(toEdgeMapping.getTagName(),
                toEdgeMapping.getTopicFilter(),
                toEdgeMapping.getMaxQoS(),
                FieldMappingsEntity.from(toEdgeMapping.getFieldMappings()));
    }
}
