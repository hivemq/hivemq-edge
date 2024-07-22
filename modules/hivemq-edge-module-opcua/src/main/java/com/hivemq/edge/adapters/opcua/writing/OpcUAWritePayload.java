package com.hivemq.edge.adapters.opcua.writing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.writing.WritePayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OpcUAWritePayload implements WritePayload {

    @JsonProperty("value")
    @ModuleConfigField(title = "Value", description = "The value that should be written", required = true)
    private @NotNull JsonNode value;

    public OpcUAWritePayload(
            final @NotNull @JsonProperty("value") JsonNode value) {
        this.value = value;
    }

    public @NotNull JsonNode getValue() {
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
        return value.equals(payload.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
