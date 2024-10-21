package com.hivemq.edge.adapters.modbus.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum ModbusDataType {
    BOOL("BOOL", 1),
    INT_16("INT_16", 1),
    UINT_16("UINT_16", 1),
    INT_32("INT_32", 2),
    UINT_32("UINT_32", 2),
    INT_64("INT_64", 4),
    FLOAT_32("FLOAT_32", 2),
    FLOAT_64("FLOAT_64", 4),
    UTF_8("UTF_8", 4);

    private static final Map<String, ModbusDataType> BY_LABEL;

    static {
        final Map<String, ModbusDataType> temp = new HashMap<>();
        for (ModbusDataType e : values()) {
            temp.put(e.label, e);
        }
        BY_LABEL = Collections.unmodifiableMap(temp);
    }

    public final String label;
    public final int nrOfRegistersToRead;

    ModbusDataType(String label, int nrOfRegistersToRead) {
        this.label = label;
        this.nrOfRegistersToRead = nrOfRegistersToRead;
    }


    public static ModbusDataType valueOfLabel(String label) {
        return BY_LABEL.get(label);
    }

    @Override
    public String toString() {
        return this.label;
    }
}
