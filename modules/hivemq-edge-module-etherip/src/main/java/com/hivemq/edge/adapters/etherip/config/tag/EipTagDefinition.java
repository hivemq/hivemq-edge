package com.hivemq.edge.adapters.etherip.config.tag;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.tag.TagDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EipTagDefinition implements TagDefinition {

    @JsonProperty(value = "address", required = true)
    @ModuleConfigField(title = "address",
                       description = "Address of the tag on the device",
                       required = true)
    private final @NotNull String address;

    @JsonCreator
    public EipTagDefinition(@JsonProperty(value = "address", required = true) final @NotNull String address) {
        this.address = address;
    }

    public @NotNull String getAddress() {
        return address;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final EipTagDefinition that = (EipTagDefinition) o;
        return address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }
}
