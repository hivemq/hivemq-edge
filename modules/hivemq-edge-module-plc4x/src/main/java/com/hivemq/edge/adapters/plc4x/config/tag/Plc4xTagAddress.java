package com.hivemq.edge.adapters.plc4x.config.tag;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Plc4xTagAddress {

    @JsonProperty(value = "tagAddress", required = true)
    @ModuleConfigField(title = "Tag Address",
                       description = "The well formed address of the tag to read",
                       required = true)
    private final @NotNull String tagAddress;

    @JsonCreator
    public Plc4xTagAddress(@JsonProperty("tagAddress") final @NotNull String tagAddress) {
        this.tagAddress = tagAddress;
    }

    public @NotNull String getTagAddress() {
        return tagAddress;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Plc4xTagAddress that = (Plc4xTagAddress) o;
        return tagAddress.equals(that.tagAddress);
    }

    @Override
    public int hashCode() {
        return tagAddress.hashCode();
    }
}
