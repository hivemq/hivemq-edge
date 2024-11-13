package com.hivemq.edge.adapters.file.payload;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.edge.adapters.file.tag.FileTag;
import org.jetbrains.annotations.NotNull;

public class FileDataPoint implements DataPoint {

    private final @NotNull Object tagValue;
    private final @NotNull FileTag tag;

    public FileDataPoint(final @NotNull FileTag tag, final @NotNull Object tagValue) {
        this.tag = tag;
        this.tagValue = tagValue;
    }

    @Override
    public @NotNull Object getTagValue() {
        return tagValue;
    }

    @Override
    public @NotNull String getTagName() {
        return tag.getName();
    }

    public @NotNull FileTag getTag() {
        return tag;
    }
}
