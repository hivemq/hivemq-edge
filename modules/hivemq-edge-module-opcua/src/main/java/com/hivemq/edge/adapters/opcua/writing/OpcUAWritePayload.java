package com.hivemq.edge.adapters.opcua.writing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.writing.WritePayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OpcUAWritePayload implements WritePayload {

    @JsonProperty("node")
    @ModuleConfigField(title = "Source Node ID",
                       description = "identifier of the node on the OPC-UA server. Example: \"ns=3;s=85/0:Temperature\"",
                       required = true)
    private @NotNull String node = "";

    @JsonProperty("value")
    @ModuleConfigField(title = "Value", description = "The value that should be written", required = true)
    private @NotNull Object value;

    @JsonProperty("type")
    @ModuleConfigField(title = "Type", description = "The type of the value that should be written", required = true)
    private @NotNull OpcUaValueType type;

    public OpcUAWritePayload(
            final @NotNull @JsonProperty("node") String node,
            final @NotNull @JsonProperty("value") Object value,
            final @NotNull @JsonProperty("type") OpcUaValueType type) {
        this.node = node;
        this.value = value;
        this.type = type;
    }

    public @NotNull String getNode() {
        return node;
    }

    public @NotNull OpcUaValueType getType() {
        return type;
    }

    public @NotNull Object getValue() {
        return value;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        OpcUAWritePayload payload = (OpcUAWritePayload) o;
        return node.equals(payload.node) && value.equals(payload.value) && type == payload.type;
    }

    @Override
    public int hashCode() {
        int result = node.hashCode();
        result = 31 * result + value.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }
}
