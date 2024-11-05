package com.hivemq.edge.adapters.modbus.config.tag;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.tag.TagDefinition;
import com.hivemq.edge.adapters.modbus.config.AddressRange;
import com.hivemq.edge.adapters.modbus.config.ModbusAdapterConfig;
import com.hivemq.edge.adapters.modbus.config.ModbusAdu;
import com.hivemq.edge.adapters.modbus.config.ModbusDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static java.util.Objects.requireNonNullElse;

public class ModbusTagDefinition implements TagDefinition {

    @JsonProperty(value = "startIdx", required = true)
    @ModuleConfigField(title = "Start Index",
                       description = "The Starting Index (Incl.) of the Address Range",
                       numberMin = 0,
                       numberMax = ModbusAdapterConfig.PORT_MAX,
                       required = true)
    public final int startIdx;

    @JsonProperty(value = "readType", required = true)
    @ModuleConfigField(title = "The way the register range should be read",
                       description = "Type of read to performe on the registers",
                       required = true)
    public final @NotNull ModbusAdu readType;

    @JsonProperty(value = "unitId", required = true)
    @ModuleConfigField(title = "The id of the unit to access",
                       description = "Id of the unit to access on the modbus",
                       required = true)
    public final int unitId;

    @JsonProperty(value = "flipRegisters", defaultValue = "false")
    @ModuleConfigField(title = "Indicates if registers should be evaluated in reverse order",
                       description = "Registers and their contents are normally written/read as big endian, some implementations decided to write the content as big endian but to order the actual registers as little endian.",
                       defaultValue = "false")
    public final boolean flipRegisters;

    @JsonProperty("dataType")
    @ModuleConfigField(title = "Data Type",
                       description = "Define how the read registers are interpreted",
                       defaultValue = "INT_16")
    private final @NotNull ModbusDataType dataType;

    @JsonCreator
    public ModbusTagDefinition(
            @JsonProperty(value = "startIdx", required = true) final int startIdx,
            @JsonProperty(value = "readType", required = true) final @NotNull ModbusAdu readType,
            @JsonProperty(value = "unitId", required = true) final int unitId,
            @JsonProperty(value = "flipRegisters", defaultValue = "false") final boolean flipRegisters,
            @JsonProperty(value = "dataType") final @Nullable ModbusDataType dataType) {
        this.startIdx = startIdx;
        this.readType = readType;
        this.unitId = unitId;
        this.flipRegisters = flipRegisters;
        this.dataType = requireNonNullElse(dataType, ModbusDataType.INT_16);

    }

    public @NotNull ModbusDataType getDataType() {
        return dataType;
    }

    public int getStartIdx() {
        return startIdx;
    }

    public @NotNull ModbusAdu getReadType() {
        return readType;
    }

    public int getUnitId() {
        return unitId;
    }

    public boolean isFlipRegisters() {
        return flipRegisters;
    }

    @Override
    public String toString() {
        return "ModbusTagDefinition{" +
                "startIdx=" +
                startIdx +
                ", readType=" +
                readType +
                ", unitId=" +
                unitId +
                ", flipRegisters=" +
                flipRegisters +
                ", dataType=" +
                dataType +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ModbusTagDefinition that = (ModbusTagDefinition) o;
        return startIdx == that.startIdx &&
                unitId == that.unitId &&
                flipRegisters == that.flipRegisters &&
                readType == that.readType &&
                dataType == that.dataType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(startIdx, readType, unitId, flipRegisters, dataType);
    }
}
