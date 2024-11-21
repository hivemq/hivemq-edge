package com.hivemq.configuration.entity.adapter;

import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.protocols.ToEdgeMapping;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class ToEdgeMappingEntity {

    @XmlElement(name = "topic-filter", required = true)
    private final @NotNull String topicFilter;

    @XmlElement(name = "tag-name", required = true)
    private final @NotNull String tagName;

    @XmlElement(name = "fieldMappings", required = true)
    private final @NotNull FieldMappingsEntity fieldMappingsEntity;

    // no-arg constructor for JaxB
    public ToEdgeMappingEntity() {
        topicFilter = "";
        tagName = "";

        fieldMappingsEntity = null;
    }

    public ToEdgeMappingEntity(
            final @NotNull String tagName,
            final @NotNull String topicFilter,
            final @NotNull FieldMappingsEntity fieldMappingsEntity) {
        this.tagName = tagName;
        this.topicFilter = topicFilter;
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
                toEdgeMapping.getMqttTopic(),

                FieldMappingsEntity.from(toEdgeMapping.getFieldMappings()));
    }
}
