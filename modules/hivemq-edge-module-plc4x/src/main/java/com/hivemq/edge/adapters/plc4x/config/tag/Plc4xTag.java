package com.hivemq.edge.adapters.plc4x.config.tag;

import com.hivemq.adapter.sdk.api.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Plc4xTag implements Tag<Plc4xTagDefinition> {

    private final @NotNull String tagName;
    private final @NotNull Plc4xTagDefinition plc4XTagDefinition;

    public Plc4xTag(final @NotNull String tagName, final @NotNull Plc4xTagDefinition plc4XTagDefinition) {
        this.tagName = tagName;
        this.plc4XTagDefinition = plc4XTagDefinition;
    }

    @Override
    public @NotNull Plc4xTagDefinition getDefinition() {
        return plc4XTagDefinition;
    }

    @Override
    public @NotNull String getName() {
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
        return tagName.equals(httpTag.tagName) && plc4XTagDefinition.equals(httpTag.plc4XTagDefinition);
    }

    @Override
    public int hashCode() {
        int result = tagName.hashCode();
        result = 31 * result + plc4XTagDefinition.hashCode();
        return result;
    }
}
