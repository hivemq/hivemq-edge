package com.hivemq.edge.adapters.modbus.writing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.writing.WritePayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ModbusWritePayload implements WritePayload {
    @JsonProperty("value")
    @ModuleConfigField(title = "Value", description = "The value that should be written", required = true)
    private @NotNull Object value;


    public ModbusWritePayload(
            final @NotNull @JsonProperty("value") Object value) {
        this.value = value;
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

        ModbusWritePayload payload = (ModbusWritePayload) o;
        return value.equals(payload.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
