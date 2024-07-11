package com.hivemq.edge.adapters.opcua.writing;

public enum OpcUaValueType {

    INTEGER(true),
    UINTEGER(false),

    BOOLEAN(true),
    BYTE(false),
    UBYTE(false),

    Short(false),
    USHORT(false),

    // LONG is kind of primitive to Jackson, but if the long fits into a Integer, it will create a Integer. Not sure if OpcUA would have a problem with that.
    LONG(false),
    ULONG(false),

    // default for floating point numbers in jackson is double, not float.
    FLOAT(false),
    DOUBLE(true),

    STRING(true),
    CUSTOM_STRUCT(false);

    private final boolean jacksonDefaultPrimitive;

    OpcUaValueType(final boolean jacksonDefaultPrimitive) {
        this.jacksonDefaultPrimitive = jacksonDefaultPrimitive;
    }

    public boolean isJacksonDefaultPrimitive() {
        return jacksonDefaultPrimitive;
    }
}
