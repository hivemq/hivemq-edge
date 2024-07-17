package com.hivemq.edge.adapters.opcua.writing;

import com.google.common.io.BaseEncoding;
import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.milo.opcua.binaryschema.AbstractCodec;
import org.eclipse.milo.opcua.binaryschema.GenericEnumCodec;
import org.eclipse.milo.opcua.binaryschema.GenericStructCodec;
import org.eclipse.milo.opcua.binaryschema.Struct;
import org.eclipse.milo.opcua.sdk.client.DataTypeTreeBuilder;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.core.DataTypeTree;
import org.eclipse.milo.opcua.stack.core.BuiltinDataType;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.UaRuntimeException;
import org.eclipse.milo.opcua.stack.core.serialization.codecs.DataTypeCodec;
import org.eclipse.milo.opcua.stack.core.types.OpcUaDefaultBinaryEncoding;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExtensionObject;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.XmlElement;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.ULong;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.opcfoundation.opcua.binaryschema.FieldType;

import java.lang.reflect.Field;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"rawtypes", "unchecked"})
public class JsonToOpcUAConverter {


    private final @NotNull OpcUaClient client;
    private final @NotNull DataTypeTree tree;

    public JsonToOpcUAConverter(final @NotNull OpcUaClient client) throws UaException {
        this.client = client;
        this.tree = DataTypeTreeBuilder.build(client);
    }

    public static JsonToOpcUAConverter getInstance(final @NotNull OpcUaClient client) throws UaException {
        return new JsonToOpcUAConverter(client);
    }


    public @Nullable Object convertToOpcUAValue(
            final @NotNull Object value, final @NotNull NodeId destinationNodeId) {
        try {
            final NodeId dataTypeNodeId = getDataTypeNodeId(destinationNodeId);
            final DataTypeTree.DataType dataType = tree.getDataType(dataTypeNodeId);
            if (dataType == null) {
                // TODO we were not able to find the information on the encoding
                throw new IllegalArgumentException();
            }
            final BuiltinDataType builtinType = tree.getBuiltinType(dataType.getNodeId());
            if (builtinType != BuiltinDataType.ExtensionObject) {
                return parsetoOpcUAObject(builtinType, value, null);
            }


            final NodeId binaryEncodingId = dataType.getBinaryEncodingId();
            if (binaryEncodingId == null) {
                // TODO potentially there could be other encodings be present
                throw new IllegalArgumentException();
            }


            // TODO needs a check
            final LinkedHashMap rootNode = (LinkedHashMap) value;
            final Map<String, FieldType> fields = getStructureInformation(binaryEncodingId);
            final Struct.Builder builder = Struct.builder("CustomStruct"); // apparently the name is not important

            for (final Map.Entry<String, FieldType> entry : fields.entrySet()) {
                final String key = entry.getKey();
                final FieldType fieldType = entry.getValue();
                final Object jsonNode = rootNode.get(key);
                if (jsonNode == null) {
                    //TODO handle missing field in json
                    throw new IllegalArgumentException();
                }

                final Object parsed = parseToOpcUACompatibleObject(jsonNode, fieldType);
                builder.addMember(key, parsed);
            }
            return ExtensionObject.encode(client.getDynamicSerializationContext(),
                    builder.build(),
                    binaryEncodingId,
                    OpcUaDefaultBinaryEncoding.getInstance());


        } catch (UaException e) {
            throw new IllegalArgumentException("Unknown type");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    private @Nullable NodeId getDataTypeNodeId(final @NotNull NodeId variableNodeId) throws UaException {
        final UaVariableNode variableNode = client.getAddressSpace().getVariableNode(variableNodeId);
        return variableNode.getDataType();
    }

    private @NotNull BuiltinDataType mapToBuildInDataType(final @NotNull FieldType fieldType) {
        final String namespaceURI = fieldType.getTypeName().getNamespaceURI();
        boolean isStandard = namespaceURI.startsWith("http://opcfoundation.org/");
        if (isStandard) {
            switch (fieldType.getTypeName().getLocalPart()) {
                // TODO get all the cases
                case ("UInt32"):
                    return BuiltinDataType.UInt32;
                case ("String"):
                    return BuiltinDataType.String;
                case ("Boolean"):
                    return BuiltinDataType.Boolean;
                case ("Structure"):
                    return BuiltinDataType.ExtensionObject;
            }
        } else {
            final DataTypeCodec dataTypeCodec =
                    client.getDynamicDataTypeManager().getCodec(namespaceURI, fieldType.getTypeName().getLocalPart());
            if (dataTypeCodec instanceof GenericEnumCodec) {
                return BuiltinDataType.Int32;
            } else if (dataTypeCodec instanceof GenericStructCodec) {
                return BuiltinDataType.ExtensionObject;
            }
        }
        throw new IllegalArgumentException();
    }


    private @Nullable Object parseToOpcUACompatibleObject(
            final @NotNull Object jsonNode, final @NotNull FieldType fieldType) {
        final BuiltinDataType builtinDataType = mapToBuildInDataType(fieldType);

        client.getStaticDataTypeManager().getDataTypeDictionary(fieldType.getTypeName().getNamespaceURI());

        if (builtinDataType == BuiltinDataType.ExtensionObject) {
            final String namespaceURI = fieldType.getTypeName().getNamespaceURI();
            final ExpandedNodeId expandedNodeId = new ExpandedNodeId.Builder().setNamespaceUri(namespaceURI)
                    .setIdentifier("DataType." + fieldType.getTypeName().getLocalPart())
                    .build();


            final Optional<NodeId> optionalDataTypeId = expandedNodeId.toNodeId(client.getNamespaceTable());
            if (optionalDataTypeId.isEmpty()) {
                // TODO
                throw new IllegalArgumentException();
            }

            final NodeId dataTypeId = optionalDataTypeId.get();
            final DataTypeTree.DataType dataType = tree.getDataType(dataTypeId);

            if (dataType == null) {
                throw new IllegalArgumentException();
            }

            final NodeId binaryEncodingId = dataType.getBinaryEncodingId();
            return parsetoOpcUAObject(builtinDataType, jsonNode, binaryEncodingId);
        }
        return parsetoOpcUAObject(builtinDataType, jsonNode, null);
    }

    @Nullable
    Object parsetoOpcUAObject(
            final @NotNull BuiltinDataType builtinDataType,
            final @NotNull Object value, final @Nullable NodeId binaryEncodingId) {
        switch (builtinDataType) {
            case Boolean:
                if (value instanceof String) {
                    return Boolean.valueOf((String) value);
                } else if (value instanceof Boolean) {
                    return value;
                } else {
                    throw createException(value, builtinDataType.name());
                }
            case Byte:
            case SByte:
                if (value instanceof String) {
                    return Byte.valueOf((String) value);
                } else if (value instanceof Byte) {
                    return value;
                } else if (value instanceof Integer) {
                    return value;
                } else {
                    throw createException(value, builtinDataType.name());
                }
            case UInt16:
            case UInt32:
                if (value instanceof String) {
                    return UInteger.valueOf((String) value);
                } else if (value instanceof Integer) {
                    return UInteger.valueOf((Integer) value);
                } else if (value instanceof Long) {
                    return UInteger.valueOf((Long) value);
                } else {
                    throw createException(value, builtinDataType.name());
                }
            case Int16:
                if (value instanceof String) {
                    return Short.valueOf((String) value);
                } else if (value instanceof Short) {
                    return value;
                } else if (value instanceof Integer) {
                    // todo if it is a long in the json, it is a problem as it does not fit in Integer
                    return value;
                } else {
                    throw createException(value, builtinDataType.name());
                }
            case Int32:
                if (value instanceof String) {
                    return Integer.valueOf((String) value);
                } else if (value instanceof Integer) {
                    return value;
                } else if (value instanceof Long) {
                    // todo if it is a long in the json, it is a problem as it does not fit in Integer
                    return value;
                } else {
                    throw createException(value, builtinDataType.name());
                }
            case Int64:
                if (value instanceof String) {
                    return Long.valueOf((String) value);
                } else if (value instanceof Integer) {
                    return value;
                } else if (value instanceof Long) {
                    return value;
                } else {
                    throw createException(value, builtinDataType.name());
                }
            case UInt64:
                if (value instanceof String) {
                    return ULong.valueOf((String) value);
                } else if (value instanceof Integer) {
                    return ULong.valueOf((Integer) value);
                } else if (value instanceof Long) {
                    return ULong.valueOf((Long) value);
                } else {
                    throw createException(value, builtinDataType.name());
                }
            case Float:
                if (value instanceof String) {
                    return Float.valueOf((String) value);
                } else if (value instanceof Integer) {
                    return Float.valueOf((Integer) value);
                } else if (value instanceof Long) {
                    return Float.valueOf((Long) value);
                } else if (value instanceof Float) {
                    return value;
                } else if (value instanceof Double) {
                    return value;
                } else {
                    throw createException(value, builtinDataType.name());
                }
            case Double:
                if (value instanceof String) {
                    return Double.valueOf((String) value);
                } else if (value instanceof Integer) {
                    return Double.valueOf((Integer) value);
                } else if (value instanceof Long) {
                    return Double.valueOf((Long) value);
                } else if (value instanceof Float) {
                    return value;
                } else if (value instanceof Double) {
                    return value;
                } else {
                    throw createException(value, builtinDataType.name());
                }
            case String:
                if (value instanceof String) {
                    return value;
                } else {
                    throw createException(value, "String");
                }
            case DateTime:
                if (value instanceof String) {
                    return DateTimeFormatter.ISO_INSTANT.parse((String) value);
                } else if (value instanceof Long) {
                    return new DateTime((Long) value);
                } else if (value instanceof Integer) {
                    return new DateTime((Integer) value);
                } else {
                    throw createException(value, builtinDataType.name());
                }
            case Guid:
                if (value instanceof String) {
                    return UUID.fromString((String) value);
                } else {
                    throw createException(value, builtinDataType.name());
                }
            case ByteString:
                if (value instanceof String) {
                    return ByteString.of(BaseEncoding.base64().decode((String) value));
                } else {
                    throw createException(value, builtinDataType.name());
                }
            case XmlElement:
                if (value instanceof String) {
                    return XmlElement.of((String) value);
                } else {
                    throw createException(value, builtinDataType.name());
                }
            case NodeId:
                if (value instanceof String) {
                    try {
                        return NodeId.parse((String) value);
                    } catch (UaRuntimeException e) {
                        //TODO handle
                    }
                } else {
                    throw createException(value, builtinDataType.name());
                }
            case ExpandedNodeId:
                if (value instanceof String) {
                    try {
                        return ExpandedNodeId.parse((String) value);
                    } catch (UaRuntimeException e) {
                        //TODO handle
                    }
                } else {
                    throw createException(value, builtinDataType.name());
                }
            case StatusCode:
                if (value instanceof Integer) {
                    return new StatusCode((Integer) value);
                } else {
                    throw createException(value, builtinDataType.name());
                }
            case QualifiedName:
                // qualified name needs two fields, so it needs to be own object in the JSON
                if (value instanceof LinkedHashMap) {
                    final LinkedHashMap rootNode = (LinkedHashMap) value;
                    final Object namespaceIndex = rootNode.get("namespaceIndex");
                    final Object name = rootNode.get("");
                    if (namespaceIndex instanceof Integer) {
                        if (name instanceof String) {
                            return new QualifiedName((Integer) namespaceIndex, (String) name);
                        } else {
                            throw createException(namespaceIndex, "String");
                        }
                    } else {
                        throw createException(namespaceIndex, "Integer");
                    }
                } else {
                    // TODO value is not an json object (LinkedHashMap)
                    throw createException(value, "LinkedHashMap");
                }
            case LocalizedText:
                if (value instanceof String) {
                    return LocalizedText.english((String) value);
                } else {
                    throw createException(value, "LocalizedText");
                }
            case DataValue:
                // DataValue is too complex for now
                throw new NotImplementedException();
            case Variant:
                // Variant is too complex for now
                throw new NotImplementedException();
            case DiagnosticInfo:
                // DiagnosticInfo is too complex for now
                throw new NotImplementedException();
            case ExtensionObject:
                try {
                    if (binaryEncodingId == null) {
                        throw new UnsupportedOperationException(
                                "Structs embedded in other structs are currently not supported.");
                    }

                    final LinkedHashMap rootNode = (LinkedHashMap) value;
                    final Map<String, FieldType> fields = getStructureInformation(binaryEncodingId);
                    final Struct.Builder builder =
                            Struct.builder("CustomStruct"); // apparently the name is not important

                    for (final Map.Entry<String, FieldType> entry : fields.entrySet()) {
                        final String key = entry.getKey();
                        final FieldType fieldType = entry.getValue();
                        final Object jsonNode = rootNode.get(key);
                        if (jsonNode == null) {
                            //TODO handle missing field in json
                            throw new IllegalArgumentException();
                        }

                        final Object parsed = parseToOpcUACompatibleObject(jsonNode, fieldType);
                        builder.addMember(key, parsed);
                    }
                    return builder.build();
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }

        }
        return null;
    }


    private @NotNull Map<String, FieldType> getStructureInformation(final @NotNull NodeId binaryEncodingId)
            throws NoSuchFieldException, IllegalAccessException {
        final DataTypeCodec dataTypeCodec =
                client.getDynamicSerializationContext().getDataTypeManager().getCodec(binaryEncodingId);
        final Field f = AbstractCodec.class.getDeclaredField("fields"); //NoSuchFieldException
        f.setAccessible(true);
        return (Map<String, FieldType>) f.get(dataTypeCodec);
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
