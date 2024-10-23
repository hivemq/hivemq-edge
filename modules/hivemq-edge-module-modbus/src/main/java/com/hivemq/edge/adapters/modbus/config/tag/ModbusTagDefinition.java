package com.hivemq.edge.adapters.modbus.config.tag;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.edge.adapters.modbus.config.AddressRange;
import com.hivemq.edge.adapters.modbus.config.ModbusDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNullElse;

public class ModbusTagDefinition {

    @JsonProperty(value = "addressRange", required = true)
    @ModuleConfigField(title = "Address Range",
                       description = "Define the start and end index values for your memory addresses",
                       required = true)
    private final @NotNull AddressRange addressRange;

    @JsonProperty("dataType")
    @ModuleConfigField(title = "Data Type",
                       description = "Define how the read registers are interpreted",
                       defaultValue = "INT_16")
    private final @NotNull ModbusDataType dataType;

    @JsonCreator
    public ModbusTagDefinition(
            @JsonProperty(value = "addressRange", required = true) final @NotNull AddressRange addressRange,
            @JsonProperty(value = "dataType") final @Nullable ModbusDataType dataType) {
        this.addressRange = addressRange;
        this.dataType = requireNonNullElse(dataType, ModbusDataType.INT_16);

    }

    public @NotNull AddressRange getAddressRange() {
        return addressRange;
    }

    public @NotNull ModbusDataType getDataType() {
        return dataType;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ModbusTagDefinition that = (ModbusTagDefinition) o;
        return addressRange.equals(that.addressRange) && dataType == that.dataType;
    }

    @Override
    public int hashCode() {
        int result = addressRange.hashCode();
        result = 31 * result + dataType.hashCode();
        return result;
    }
}
