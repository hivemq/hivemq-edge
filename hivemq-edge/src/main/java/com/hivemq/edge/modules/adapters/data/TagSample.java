package com.hivemq.edge.modules.adapters.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * @author Simon L Johnson
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TagSample {

    private String tagName;
    private Object tagValue;

    public TagSample(final @NotNull String tagName, final @NotNull Object tagValue) {
        this.tagName = tagName;
        this.tagValue = tagValue;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(final String tagName) {
        this.tagName = tagName;
    }

    public Object getTagValue() {
        return tagValue;
    }

    public void setTagValue(final Object tagValue) {
        this.tagValue = tagValue;
    }
}
