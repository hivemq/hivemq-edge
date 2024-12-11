/*
 * Copyright 2023-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.edge.adapters.opcua.mqtt2opcua;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.io.BaseEncoding;
import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.milo.opcua.binaryschema.AbstractCodec;
import org.eclipse.milo.opcua.binaryschema.Struct;
import org.eclipse.milo.opcua.sdk.client.DataTypeTreeBuilder;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
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
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.ULong;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.jetbrains.annotations.NotNull;
import org.opcfoundation.opcua.binaryschema.FieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.hivemq.edge.adapters.opcua.mqtt2opcua.BuiltInDataTypeConverter.convertFieldTypeToBuiltInDataType;
import static org.eclipse.milo.opcua.stack.core.BuiltinDataType.Guid;
import static org.eclipse.milo.opcua.stack.core.BuiltinDataType.Int16;
import static org.eclipse.milo.opcua.stack.core.BuiltinDataType.Int32;
import static org.eclipse.milo.opcua.stack.core.BuiltinDataType.Int64;
import static org.eclipse.milo.opcua.stack.core.BuiltinDataType.SByte;
import static org.eclipse.milo.opcua.stack.core.BuiltinDataType.UInt16;
import static org.eclipse.milo.opcua.stack.core.BuiltinDataType.UInt32;
import static org.eclipse.milo.opcua.stack.core.BuiltinDataType.UInt64;

@SuppressWarnings({"rawtypes", "unchecked"})
public class JsonToOpcUAConverter {

    private static final @NotNull Logger log = LoggerFactory.getLogger("com.hivemq.edge.write.JsonToOpcUAConverter");

    private final @NotNull OpcUaClient client;
    private final @NotNull DataTypeTree tree;

    public JsonToOpcUAConverter(final @NotNull OpcUaClient client) throws UaException {
        this.client = client;
        this.tree = DataTypeTreeBuilder.build(client);
    }

    public @NotNull Object convertToOpcUAValue(
            final @NotNull JsonNode rootNode, final @NotNull NodeId destinationNodeId) {
        log.debug("Convert json '{}' to opcua compatible object for destination nodeId '{}'.",
                rootNode,
                destinationNodeId);
        try {
            final NodeId dataTypeNodeId = client.getAddressSpace().getVariableNode(destinationNodeId).getDataType();
            if (dataTypeNodeId == null) {
                log.warn("No dataType-nodeId was found for the destination nodeId '{}'.", destinationNodeId);
                throw new RuntimeException("No dataType-nodeId was found for the destination nodeId " +
                        destinationNodeId +
                        "'");
            }
            log.debug("Destination NodeId '{}' has DataType NodeId '{}'.", destinationNodeId, dataTypeNodeId);

            final DataTypeTree.DataType dataType = tree.getDataType(dataTypeNodeId);
            if (dataType == null) {
                log.warn("No data type was found in the DataTypeTree for dataType with nodeId '{}'.", dataTypeNodeId);
                throw new RuntimeException("No data type was found in the DataTypeTree for node id '" +
                        dataTypeNodeId +
                        "'");
            }
            log.debug("DataType NodeId '{}' represents data type '{}'.", dataTypeNodeId, dataType);

            final BuiltinDataType builtinType = tree.getBuiltinType(dataType.getNodeId());
            log.debug(
                    "Destination Node '{}' has DataType NodeId '{}' representing DataType '{}' with builtin type '{}'. The Json '{}' is parsed to this.",
                    destinationNodeId,
                    dataTypeNodeId,
                    dataType,
                    builtinType,
                    rootNode);

            if (builtinType != BuiltinDataType.ExtensionObject) {
                return parsetoOpcUAObject(builtinType, rootNode);
            }

            final NodeId binaryEncodingId = dataType.getBinaryEncodingId();
            if (binaryEncodingId == null) {
                log.warn("No encoding was present for data type: '{}'.", dataType);
                throw new RuntimeException("No encoding was present for data type: '" + dataType + "'");
            }
            log.debug("DataType '{}' has binary encoding id '{}'.", dataType, binaryEncodingId);

            final Map<String, FieldType> fields = getStructureInformation(binaryEncodingId);
            log.debug("Found fields '{}' for binary encoding id '{}'.", fields, binaryEncodingId);
            final Struct.Builder builder = Struct.builder("CustomStruct"); // apparently the name is not important

            for (final Map.Entry<String, FieldType> entry : fields.entrySet()) {
                final String key = entry.getKey();
                final FieldType fieldType = entry.getValue();
                final JsonNode jsonNode = rootNode.get(key);
                if (jsonNode == null) {
                    log.warn("Expected field '{}' to be present in the json '{}', but field was not present.",
                            key,
                            rootNode);
                    throw new RuntimeException("Expected field '" +
                            key +
                            "' to be present in the json, but field was not present.");
                }
                log.debug("Parsing '{}' for field type '{}'", jsonNode, fieldType);
                final Object parsed = parseToOpcUACompatibleObject(jsonNode, fieldType);
                builder.addMember(key, parsed);
            }
            return ExtensionObject.encode(client.getDynamicSerializationContext(),
                    builder.build(),
                    binaryEncodingId,
                    OpcUaDefaultBinaryEncoding.getInstance());
        } catch (final UaException e) {
            throw new RuntimeException(e);
        }
    }

    private @NotNull Object parseToOpcUACompatibleObject(
            final @NotNull JsonNode jsonNode, final @NotNull FieldType fieldType) {
        final BuiltinDataType builtinDataType = convertFieldTypeToBuiltInDataType(fieldType, client);

        client.getStaticDataTypeManager().getDataTypeDictionary(fieldType.getTypeName().getNamespaceURI());

        if (builtinDataType == BuiltinDataType.ExtensionObject) {
            final String namespaceURI = fieldType.getTypeName().getNamespaceURI();
            final ExpandedNodeId expandedNodeId = new ExpandedNodeId.Builder().setNamespaceUri(namespaceURI)
                    .setIdentifier(fieldType.getTypeName().getLocalPart())
                    .build();

            final Optional<NodeId> optionalDataTypeId = expandedNodeId.toNodeId(client.getNamespaceTable());
            if (optionalDataTypeId.isEmpty()) {
                log.warn("Expanded node id '{}}' could not be parsed to node id.", expandedNodeId);
                throw new RuntimeException("Expanded node id '" + expandedNodeId + "' could not be parsed to node id.");
            }

            final NodeId dataTypeId = optionalDataTypeId.get();
            final DataTypeTree.DataType dataType = tree.getDataType(dataTypeId);

            if (dataType == null) {
                log.warn("No data type was found in the DataTypeTree for dataType with nodeId '{}'.", dataTypeId);
                throw new RuntimeException("No data type was found in the DataTypeTree for node id '" +
                        dataTypeId +
                        "'");
            }

            final NodeId binaryEncodingId = dataType.getBinaryEncodingId();
            log.debug(
                    "FieldType '{}' with ExpandedNodeId '{}' resolved to DataType NodeId '{}' representing DataType '{}', which has BinaryEncodingId '{}' and represents the builtin type '{}'. The Json '{}' is parsed to this.",
                    fieldType,
                    expandedNodeId,
                    dataTypeId,
                    dataType,
                    binaryEncodingId,
                    builtinDataType,
                    jsonNode);

            if (binaryEncodingId == null) {
                throw new IllegalStateException("Binary encoding id was null for nested struct.");
            }
            return extractExtensionObject(jsonNode, binaryEncodingId);
        }
        return parsetoOpcUAObject(builtinDataType, jsonNode);
    }

    private @NotNull Object parsetoOpcUAObject(
            final @NotNull BuiltinDataType builtinDataType, final @NotNull JsonNode jsonNode) {
        switch (builtinDataType) {
            case Boolean:
                return extractBoolean(jsonNode);
            case Byte:
                return extractUnsignedByte(jsonNode);
            case SByte:
                return extractSByte(jsonNode);
            case UInt16:
                return extractUShort(jsonNode);
            case UInt32:
                return extractUInteger(jsonNode);
            case Int16:
                return extractSignedShort(jsonNode);
            case Int32:
                return extractSignedInteger(jsonNode);
            case Int64:
                return extractLong(jsonNode);
            case UInt64:
                return extractUnsignedLong(jsonNode);
            case Float:
                return extractFloat(jsonNode);
            case Double:
                return extractDouble(jsonNode);
            case String:
                return extractString(jsonNode);
            case DateTime:
                return extractDateTime(jsonNode);
            case Guid:
                return extractGuid(jsonNode);
            case ByteString:
                return extractByteString(jsonNode);
            case XmlElement:
                return extractXmlElement(jsonNode);
            case NodeId:
                return extractNodeId(jsonNode);
            case ExpandedNodeId:
                return extractExpandedNodeId(jsonNode);
            case StatusCode:
                return extractStatusCode(jsonNode);
            case QualifiedName:
                return extractQualifiedName(jsonNode);
            case LocalizedText:
                return extractLocalizedText(jsonNode);
            case DataValue:
                // DataValue is too complex for now
                // TODO implement
                throw new NotImplementedException();
            case Variant:
                // TODO implement
                // Variant is too complex for now
                throw new NotImplementedException();
            case DiagnosticInfo:
                // TODO implement
                // DiagnosticInfo is too complex for now
                throw new NotImplementedException();
        }
        throw createException(jsonNode, builtinDataType.name());
    }

    private @NotNull Struct extractExtensionObject(
            final @NotNull JsonNode jsonNode, final @NotNull NodeId binaryEncodingId) {


        final Map<String, FieldType> fields = getStructureInformation(binaryEncodingId);
        final Struct.Builder builder = Struct.builder("CustomStruct"); // apparently the name is not important

        for (final Map.Entry<String, FieldType> entry : fields.entrySet()) {
            final String key = entry.getKey();
            final FieldType fieldType = entry.getValue();
            final JsonNode nestedObjectNode = jsonNode.get(key);
            if (nestedObjectNode == null) {
                throw new RuntimeException("No nested json was found for key '" + key + "'.");
            }
            final Object parsed = parseToOpcUACompatibleObject(nestedObjectNode, fieldType);
            builder.addMember(key, parsed);
        }
        return builder.build();
    }

    private static LocalizedText extractLocalizedText(final JsonNode jsonNode) {
        if (jsonNode.isTextual()) {
            return LocalizedText.english(jsonNode.asText());
        }
        throw createException(jsonNode, "LocalizedText");
    }

    private static QualifiedName extractQualifiedName(final JsonNode jsonNode) {
        // qualified name needs two fields, so it needs to be own object in the JSON
        if (!jsonNode.has("namespaceIndex")) {
            throw new RuntimeException("Field 'namespaceIndex' was not found.");
        }
        final JsonNode namespaceIndexNode = jsonNode.get("namespaceIndex");
        if (!namespaceIndexNode.isInt()) {
            throw createException(namespaceIndexNode, "Integer");
        }

        if (!jsonNode.has("name")) {
            throw new RuntimeException("Field 'name' was not found.");
        }
        final JsonNode nameNode = jsonNode.get("name");
        if (!nameNode.isTextual()) {
            throw createException(nameNode, "String");
        }
        return new QualifiedName(namespaceIndexNode.asInt(), nameNode.asText());
    }

    static StatusCode extractStatusCode(final JsonNode jsonNode) {
        if (jsonNode.isInt()) {
            return new StatusCode(jsonNode.asInt());
        }
        throw createException(jsonNode, BuiltinDataType.StatusCode.name());
    }

    static ExpandedNodeId extractExpandedNodeId(final JsonNode jsonNode) {
        if (jsonNode.isTextual()) {
            try {
                return ExpandedNodeId.parse(jsonNode.asText());
            } catch (final UaRuntimeException e) {
                throw new RuntimeException("ExpandedNodeId could not be parsed from '" +
                        jsonNode.asText() +
                        "': " +
                        e.getMessage());
            }
        }
        throw createException(jsonNode, BuiltinDataType.ExpandedNodeId.name());
    }

    static NodeId extractNodeId(final JsonNode jsonNode) {
        if (jsonNode.isTextual()) {
            try {
                return NodeId.parse(jsonNode.asText());
            } catch (final UaRuntimeException e) {
                throw new RuntimeException("NodeId could not be parsed from '" +
                        jsonNode.asText() +
                        "': " +
                        e.getMessage());
            }
        }
        throw createException(jsonNode, BuiltinDataType.NodeId.name());
    }

    static XmlElement extractXmlElement(final JsonNode jsonNode) {
        if (jsonNode.isTextual()) {
            return XmlElement.of(jsonNode.asText());
        }
        throw createException(jsonNode, BuiltinDataType.XmlElement.name());
    }

    static ByteString extractByteString(final JsonNode jsonNode) {
        if (jsonNode.isTextual()) {
            return ByteString.of(BaseEncoding.base64().decode(jsonNode.asText()));
        }
        throw createException(jsonNode, BuiltinDataType.ByteString.name());
    }

    private static UUID extractGuid(final JsonNode jsonNode) {
        if (jsonNode.isTextual()) {
            return UUID.fromString(jsonNode.asText());
        }
        throw createException(jsonNode, Guid.name());
    }

    private static DateTime extractDateTime(final JsonNode jsonNode) {
        if (jsonNode.isLong()) {
            return new DateTime(jsonNode.asLong());
        }
        throw createException(jsonNode, BuiltinDataType.DateTime.name());
    }

    static @NotNull String extractString(final JsonNode jsonNode) {
        if (jsonNode.isTextual()) {
            return jsonNode.asText();
        }
        throw createException(jsonNode, BuiltinDataType.String.name());
    }

    static double extractDouble(final JsonNode jsonNode) {
        if (jsonNode.isDouble()) {
            return jsonNode.asDouble();
        }
        throw createException(jsonNode, BuiltinDataType.Double.name());
    }

    static float extractFloat(final JsonNode jsonNode) {
        if (jsonNode.isDouble()) {
            return jsonNode.floatValue();
        }

        if (jsonNode.isFloat()) {
            return jsonNode.floatValue();
        }

        if (jsonNode.isInt()) {
            final float parsedFloat = jsonNode.intValue();
            final int parsedIntegerBack = (int) parsedFloat;
            if (parsedIntegerBack != jsonNode.intValue()) {
                throw new IllegalArgumentException(String.format(
                        "An integer was supplied for a float node that is not representable without rounding. Input Integer: '%d', Output Float: '%f'. To avoid inaccuracies the publish will not be consumed.",
                        jsonNode.intValue(),
                        parsedFloat));
            }
            return parsedFloat;
        }

        throw createException(jsonNode, BuiltinDataType.Float.name());
    }

    static ULong extractUnsignedLong(final JsonNode jsonNode) {
        if (jsonNode.isLong()) {
            return ULong.valueOf(jsonNode.asLong());
        }
        throw createException(jsonNode, UInt64.name());
    }

    static long extractLong(final JsonNode jsonNode) {
        if (jsonNode.isLong()) {
            return jsonNode.longValue();
        }
        throw createException(jsonNode, Int64.name());
    }

    static int extractSignedInteger(final JsonNode jsonNode) {
        if (jsonNode.isLong() && !jsonNode.canConvertToInt()) {
            throw createOverflowException(jsonNode, Int32.name());
        }

        if (jsonNode.isInt()) {
            return jsonNode.intValue();
        }
        throw createException(jsonNode, Int32.name());
    }

    static short extractSignedShort(final JsonNode jsonNode) {
        if (jsonNode.isInt()) {
            final int value = jsonNode.intValue();
            if (value > Short.MAX_VALUE) {
                throw createOverflowException(value, Int16.name());
            } else if (value < Short.MIN_VALUE) {
                throw createUnderflowException(value, Int16.name());
            }
            return jsonNode.shortValue();
        }
        throw createException(jsonNode, Int16.name());
    }

    static byte extractSByte(final JsonNode jsonNode) {
        if (jsonNode.isInt()) {
            final int value = jsonNode.intValue();
            if (value > Byte.MAX_VALUE) {
                throw createOverflowException(value, SByte.name());
            } else if (value < Byte.MIN_VALUE) {
                throw createUnderflowException(value, SByte.name());
            }
            return (byte) value;
        }
        throw createException(jsonNode, SByte.name());
    }

    static @NotNull UByte extractUnsignedByte(final JsonNode jsonNode) {
        if (jsonNode.isInt()) {
            final int value = jsonNode.intValue();
            if (value > UByte.MAX_VALUE) {
                throw createOverflowException(value, BuiltinDataType.Byte.name());
            } else if (value < UByte.MIN_VALUE) {
                throw createUnderflowException(value, BuiltinDataType.Byte.name());
            }
            return UByte.valueOf((byte) value);
        }
        throw createException(jsonNode, BuiltinDataType.Byte.name());
    }

    @NotNull
    static UInteger extractUInteger(final JsonNode jsonNode) {
        if (jsonNode.isInt() || jsonNode.isLong()) {
            final long value = jsonNode.longValue();
            if (value > UInteger.MAX_VALUE) {
                throw createOverflowException(value, BuiltinDataType.UInt32.name());
            } else if (value < UInteger.MIN_VALUE) {
                throw createUnderflowException(value, BuiltinDataType.UInt32.name());
            }
            return UInteger.valueOf(jsonNode.intValue());
        }
        throw createException(jsonNode, UInt32.name());
    }

    static UShort extractUShort(final JsonNode jsonNode) {
        if (jsonNode.isInt()) {
            final int value = jsonNode.intValue();
            if (value > UShort.MAX_VALUE) {
                throw createOverflowException(value, UInt16.name());
            } else if (value < UShort.MIN_VALUE) {
                throw createUnderflowException(value, UInt16.name());
            }
            return UShort.valueOf(value);
        }
        throw createException(jsonNode, UInt16.name());
    }

    static boolean extractBoolean(final JsonNode jsonNode) {
        if (jsonNode.isBoolean()) {
            return jsonNode.asBoolean();
        } else {
            throw createException(jsonNode, BuiltinDataType.Boolean.name());
        }
    }


    private @NotNull Map<String, FieldType> getStructureInformation(final @NotNull NodeId binaryEncodingId) {
        try {
            final DataTypeCodec dataTypeCodec =
                    client.getDynamicSerializationContext().getDataTypeManager().getCodec(binaryEncodingId);
            final Field f = AbstractCodec.class.getDeclaredField("fields"); //NoSuchFieldException
            f.setAccessible(true);
            return (Map<String, FieldType>) f.get(dataTypeCodec);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Unable to find information on fields in the codec", e);
        }
    }

    static @NotNull IllegalArgumentException createException(
            final @NotNull Object value, final @NotNull String intendedClass) {
        log.warn("Can not convert '{}' of class '{}' to '{}'..",
                value,
                value.getClass().getSimpleName(),
                intendedClass);
        throw new IllegalArgumentException("Can not convert '" +
                value +
                "' of class '" +
                value.getClass().getSimpleName() +
                "' to " +
                intendedClass +
                ".");
    }

    static @NotNull IllegalArgumentException createOverflowException(
            final @NotNull Object value, final @NotNull String intendedClass) {
        log.warn("Conversion error: The value  '{}' of type '{}' cannot be converted to '{}' due to overflow.",
                value,
                value.getClass().getSimpleName(),
                intendedClass);
        throw new IllegalArgumentException("Conversion error: The value '" +
                value +
                "' of type '" +
                value.getClass().getSimpleName() +
                "' cannot be concerted to " +
                intendedClass +
                "due to overflow.");
    }

    static @NotNull IllegalArgumentException createUnderflowException(
            final @NotNull Object value, final @NotNull String intendedClass) {
        log.warn("Conversion error: The value  '{}' of type '{}' cannot be converted to '{}' due to underflow.",
                value,
                value.getClass().getSimpleName(),
                intendedClass);
        throw new IllegalArgumentException("Conversion error: The value '" +
                value +
                "' of type '" +
                value.getClass().getSimpleName() +
                "' cannot be concerted to " +
                intendedClass +
                "due to underflow.");
    }
}
