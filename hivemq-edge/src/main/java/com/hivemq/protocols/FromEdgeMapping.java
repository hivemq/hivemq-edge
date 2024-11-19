package com.hivemq.protocols;

import com.hivemq.configuration.entity.adapter.FromEdgeMappingEntity;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.xml.bind.annotation.XmlElement;

public class FromEdgeMapping {

    private final @NotNull String topic;
    private final @NotNull String tagName;

    public FromEdgeMapping(@NotNull final String tagName, @NotNull final String topic) {
        this.tagName = tagName;
        this.topic = topic;
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public @NotNull String getTopic() {
        return topic;
    }



    public static @NotNull FromEdgeMapping fromEntity(final @NotNull FromEdgeMappingEntity entity){
        return new FromEdgeMapping(entity.getTagName(), entity.getTopic());
    }



}
