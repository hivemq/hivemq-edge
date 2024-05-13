package com.hivemq.edge.modules.adapters.data;

import com.hivemq.extension.sdk.api.adapters.data.DataPoint;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.Objects;

public class DataPointImpl implements DataPoint {
    private final @NotNull Object tagValue;
    private final @NotNull String tagName;

    public DataPointImpl(final @NotNull String tagName, final @NotNull Object tagValue) {
        this.tagName = tagName;
        this.tagValue = tagValue;
    }

    @Override
    public @NotNull Object getTagValue() {
        return tagValue;
    }

    @Override
    public @NotNull String getTagName() {
        return tagName;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
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
