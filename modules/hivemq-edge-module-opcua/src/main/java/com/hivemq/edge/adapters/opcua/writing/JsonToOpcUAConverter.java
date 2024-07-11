package com.hivemq.edge.adapters.opcua.writing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.milo.opcua.binaryschema.AbstractCodec;
import org.eclipse.milo.opcua.binaryschema.GenericStructCodec;
import org.eclipse.milo.opcua.binaryschema.Struct;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.OpcUaDefaultBinaryEncoding;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExtensionObject;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.opcfoundation.opcua.binaryschema.FieldType;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonToOpcUAConverter {

    private static final @NotNull ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static @NotNull Object convertToOpcUAValue(
            final @NotNull Object value,
            final @NotNull OpcUaValueType type,
            final @NotNull OpcUaClient client,
            final @Nullable NodeId dataTypeNodeId) {

        // primitive data types are already converted by Jackson. We can return them here. However we miss a check as a result.
        // TODO likely we want to have a case for every primitive object as well and validate that the claimed value is the actual value
        if (type.isJacksonDefaultPrimitive()) {
            return value;
        }

        switch (type) {
            case INTEGER:
                break;
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
            case BOOLEAN:
                break;
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
            case CUSTOM_STRUCT:

                LinkedHashMap rootNode = (LinkedHashMap) value;
                try {
                    final GenericStructCodec dataTypeCodec =
                            (GenericStructCodec) client.getDynamicSerializationContext()
                                    .getDataTypeManager()
                                    .getCodec(dataTypeNodeId);

                    // there is no other way than reflection to access the field "field" in order to have information on the
                    // fields of the struct
                    Field f = AbstractCodec.class.getDeclaredField("fields"); //NoSuchFieldException
                    f.setAccessible(true);
                    Map<String, FieldType> fields = (Map<String, FieldType>) f.get(dataTypeCodec);
                    final Struct.Builder builder = Struct.builder("ScanSettings");
                    for (final Map.Entry<String, FieldType> entry : fields.entrySet()) {
                        String key = entry.getKey();
                        final FieldType fieldType = entry.getValue();
                        final Object jsonNode = rootNode.get(key);
                        System.err.println(fieldType.getTypeName().toString());
                        switch (fieldType.getTypeName().toString()) {
                            case ("{http://opcfoundation.org/BinarySchema/}UInt32"):
                                builder.addMember(key, UInteger.valueOf(jsonNode.toString()));
                                break;
                            case ("{http://opcfoundation.org/BinarySchema/}String"):
                                builder.addMember(key, jsonNode);
                                break;
                            case ("{http://opcfoundation.org/BinarySchema/}Boolean"):
                                builder.addMember(key, Boolean.parseBoolean(jsonNode.toString()));
                                break;
                            case ("{urn:hivemq:test:testns}CustomEnumType"):
                                builder.addMember(key, Integer.valueOf(jsonNode.toString()));
                                break;
                        }
                    }


                    return ExtensionObject.encode(client.getDynamicSerializationContext(),
                            builder.build(),
                            dataTypeNodeId,
                            OpcUaDefaultBinaryEncoding.getInstance());
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }

                // TODO assert cast
        }


        throw new IllegalArgumentException("Unknown type");
    }


    private static @NotNull IllegalArgumentException createException(
            Object value, final @NotNull String intendedClass) {
        throw new IllegalArgumentException("Can not convert '" +
                value +
                "' of class '" +
                value.getClass().getSimpleName() +
                "' to " +
                intendedClass +
                ".");

    }

}
