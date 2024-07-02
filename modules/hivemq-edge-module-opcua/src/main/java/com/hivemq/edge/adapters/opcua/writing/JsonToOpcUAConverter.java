package com.hivemq.edge.adapters.opcua.writing;

import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.jetbrains.annotations.NotNull;

public class JsonToOpcUAConverter {


    public static @NotNull Object convertToOpcUAValue(final @NotNull Object value, final @NotNull OpcUaValueType type) {

        // primitive data types are already converted by Jackson. We can return them here. However we miss a check as a result.
        // TODO likely we want to have a case for every primitive object as well and validate that the claimed value is the actual value
        if (type.isJacksonDefaultPrimitive()) {
            return value;
        }

        switch (type) {
            case UINTEGER:
                if (value instanceof String) {
                    return UInteger.valueOf((String) value);
                } else if (value instanceof Integer) {
                    return UInteger.valueOf((Integer) value);
                } else if (value instanceof Long) {
                    return UInteger.valueOf((Long) value);
                } else {
                    throw createException(value, type.name());
                }
            case BYTE:
                break;
            case UBYTE:
                break;
            case Short:
                break;
            case USHORT:
                break;
            case LONG:
                break;
            case ULONG:
                break;
            case FLOAT:
                break;
            case DOUBLE:
                break;
            case STRING:
                break;
        }


        throw new IllegalArgumentException("Unknown type");
    }


    private static @NotNull IllegalArgumentException createException(
            Object value,
            final @NotNull String intendedClass) {
        throw new IllegalArgumentException("Can not convert '" +
                value +
                "' of class '" +
                value.getClass().getSimpleName() +
                "' to " +
                intendedClass +
                ".");

    }

}
