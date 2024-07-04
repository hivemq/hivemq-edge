package com.hivemq.edge.adapters.opcua.writing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.writing.WritePayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OpcUAWritePayload implements WritePayload {
    @JsonProperty("value")
    @ModuleConfigField(title = "Value", description = "The value that should be written", required = true)
    private @NotNull Object value;

    @JsonProperty("type")
    @ModuleConfigField(title = "Type", description = "The type of the value that should be written", required = true)
    private @NotNull OpcUaValueType type;

    public OpcUAWritePayload(
            final @NotNull @JsonProperty("value") Object value,
            final @NotNull @JsonProperty("type") OpcUaValueType type) {
        this.value = value;
        this.type = type;
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
        return value.equals(payload.value) && type == payload.type;
    }

    @Override
    public int hashCode() {
        int result = value.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }
}
