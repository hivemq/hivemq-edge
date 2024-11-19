package com.hivemq.configuration.entity.adapter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.protocols.FromEdgeMapping;

import javax.xml.bind.annotation.XmlElement;

public class FromEdgeMappingEntity {

    @XmlElement(name = "topic", required = true)
    private final @NotNull String topic;

    @XmlElement(name = "tag-name", required = true)
    private final @NotNull String tagName;

    // no-arg constructor for JaxB
    public FromEdgeMappingEntity() {
        topic = "";
        tagName = "";
    }

    public FromEdgeMappingEntity(@NotNull final String tagName, @NotNull final String topic) {
        this.tagName = tagName;
        this.topic = topic;
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public @NotNull String getTopic() {
        return topic;
    }

    public static @NotNull FromEdgeMappingEntity from(final @NotNull FromEdgeMapping fromEdgeMapping) {
        return new FromEdgeMappingEntity(fromEdgeMapping.getTagName(), fromEdgeMapping.getTopic());
    }
}
