package com.hivemq.edge.modules.adapters.data;

import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.Objects;

public class DataPointImpl implements DataPoint {
    private final Object tagValue;
    private final String tagName;

    public DataPointImpl(final @NotNull String tagName, final @NotNull Object tagValue) {
        this.tagName = tagName;
        this.tagValue = tagValue;
    }

    @Override
    public Object getTagValue() {
        return tagValue;
    }

    @Override
    public String getTagName() {
        return tagName;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DataPointImpl dataPoint = (DataPointImpl) o;
        return Objects.equals(tagValue, dataPoint.tagValue) && Objects.equals(tagName, dataPoint.tagName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tagValue, tagName);
    }
}
