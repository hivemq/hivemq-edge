package com.hivemq.configuration.entity.adapter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.xml.bind.annotation.XmlElement;

public class ToEdgeMappingEntity {

    @XmlElement(name ="topic-filter")
    private final @NotNull String topicFilter;

    @XmlElement(name ="tag-name")
    private final @NotNull String tagName;

    // no-arg constructor for JaxB
    public ToEdgeMappingEntity() {
        topicFilter = "";
        tagName = "";
    }

    public ToEdgeMappingEntity(@NotNull final String tagName, @NotNull final String topicFilter) {
        this.tagName = tagName;
        this.topicFilter = topicFilter;
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public @NotNull String getTopicFilter() {
        return topicFilter;
    }
}
