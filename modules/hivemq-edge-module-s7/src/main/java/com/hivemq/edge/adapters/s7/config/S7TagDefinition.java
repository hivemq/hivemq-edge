package com.hivemq.edge.adapters.s7.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.tag.TagDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class S7TagDefinition implements TagDefinition {

    @JsonProperty(value = "address", required = true)
    @ModuleConfigField(title = "Tag Address",
                       description = "The well formed address of the tag to read",
                       required = true)
    private final @NotNull String address;

    @JsonProperty(value = "dataType", required = true)
    @ModuleConfigField(title = "Data Type", description = "The expected data type of the tag", enumDisplayValues = {
            "Boolean",
            "Byte",
            "Int16",
            "UInt16",
            "Int32",
            "UInt32",
            "Int64",
            "Real (float 32)",
            "LReal (double 64)",
            "String",
            "Date (DateStamp)",
            "Time Of Day (TimeStamp)",
            "Date Time (DateTimeStamp)",
            "Timing (Duration ms)"}, required = true)
    private final @NotNull S7DataType dataType;

    public S7TagDefinition(@JsonProperty(value = "address", required = true) @NotNull final String address,@JsonProperty(value = "dataType", required = true) @NotNull final S7DataType dataType) {
        this.address = address;
        this.dataType = dataType;
    }

    public @NotNull String getAddress() {
        return address;
    }

    public @NotNull S7DataType getDataType() {
        return dataType;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final S7TagDefinition that = (S7TagDefinition) o;
        return Objects.equals(address, that.address) && dataType == that.dataType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, dataType);
    }

    @Override
    public String toString() {
        return "S7TagDefinition{" + "address='" + address + '\'' + ", dataType=" + dataType + '}';
    }
}
