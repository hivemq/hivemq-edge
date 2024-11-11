package com.hivemq.edge.adapters.opcua.config.tag;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.tag.TagDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OpcuaTagDefinition implements TagDefinition {

    @JsonProperty(value = "node", required = true)
    @ModuleConfigField(title = "Destination Node ID",
                       description = "identifier of the node on the OPC UA server. Example: \"ns=3;s=85/0:Temperature\"",
                       required = true)
    private final @NotNull String node;

    @JsonCreator
    public OpcuaTagDefinition(@JsonProperty(value = "node", required = true) final @NotNull String node) {
        this.node = node;
    }

    public @NotNull String getNode() {
        return node;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final OpcuaTagDefinition that = (OpcuaTagDefinition) o;
        return node.equals(that.node);
    }

    @Override
    public int hashCode() {
        return node.hashCode();
    }
}
