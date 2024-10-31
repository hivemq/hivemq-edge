package com.hivemq.edge.adapters.modbus.config.tag;

import com.hivemq.adapter.sdk.api.tag.Tag;
import org.jetbrains.annotations.NotNull;

public class ModbusTag implements Tag<ModbusTagDefinition> {

    private final @NotNull String tagName;
    private final @NotNull ModbusTagDefinition modbusTagDefinition;

    public ModbusTag(final @NotNull String tagName, final @NotNull ModbusTagDefinition modbusTagDefinition) {
        this.tagName = tagName;
        this.modbusTagDefinition = modbusTagDefinition;
    }


    @Override
    public @NotNull ModbusTagDefinition getDefinition() {
        return modbusTagDefinition;
    }

    @Override
    public @NotNull String getName() {
        return tagName;
    }

    @Override
    public boolean equals(@NotNull final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ModbusTag modbusTag = (ModbusTag) o;
        return tagName.equals(modbusTag.tagName) && modbusTagDefinition.equals(modbusTag.modbusTagDefinition);
    }

    @Override
    public int hashCode() {
        int result = tagName.hashCode();
        result = 31 * result + modbusTagDefinition.hashCode();
        return result;
    }
}
