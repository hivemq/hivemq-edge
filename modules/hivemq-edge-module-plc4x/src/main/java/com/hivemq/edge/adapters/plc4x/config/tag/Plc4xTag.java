package com.hivemq.edge.adapters.plc4x.config.tag;

import com.hivemq.adapter.sdk.api.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Plc4xTag implements Tag<Plc4xTagAddress> {

    private final @NotNull String tagName;
    private final @NotNull Plc4xTagAddress plc4xTagAddress;

    public Plc4xTag(final @NotNull String tagName, final @NotNull Plc4xTagAddress plc4xTagAddress) {
        this.tagName = tagName;
        this.plc4xTagAddress = plc4xTagAddress;
    }

    @Override
    public @NotNull Plc4xTagAddress getTagAddress() {
        return plc4xTagAddress;
    }

    @Override
    public @NotNull String getTagName() {
        return tagName;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Plc4xTag httpTag = (Plc4xTag) o;
        return tagName.equals(httpTag.tagName) && plc4xTagAddress.equals(httpTag.plc4xTagAddress);
    }

    @Override
    public int hashCode() {
        int result = tagName.hashCode();
        result = 31 * result + plc4xTagAddress.hashCode();
        return result;
    }
}
