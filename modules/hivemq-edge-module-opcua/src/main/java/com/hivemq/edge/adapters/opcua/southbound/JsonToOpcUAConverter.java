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
package com.hivemq.edge.adapters.opcua.southbound;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.core.types.DynamicStructType;
import org.eclipse.milo.opcua.sdk.core.types.codec.DynamicStructCodec;
import org.eclipse.milo.opcua.sdk.core.typetree.DataType;
import org.eclipse.milo.opcua.sdk.core.typetree.DataTypeTree;
import org.eclipse.milo.opcua.stack.core.OpcUaDataType;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.UaRuntimeException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.eclipse.milo.opcua.stack.core.OpcUaDataType.Guid;
import static org.eclipse.milo.opcua.stack.core.OpcUaDataType.Int16;
import static org.eclipse.milo.opcua.stack.core.OpcUaDataType.Int32;
import static org.eclipse.milo.opcua.stack.core.OpcUaDataType.Int64;
import static org.eclipse.milo.opcua.stack.core.OpcUaDataType.SByte;
import static org.eclipse.milo.opcua.stack.core.OpcUaDataType.UInt16;
import static org.eclipse.milo.opcua.stack.core.OpcUaDataType.UInt32;
import static org.eclipse.milo.opcua.stack.core.OpcUaDataType.UInt64;

public class JsonToOpcUAConverter {

    private static final @NotNull Logger log = LoggerFactory.getLogger("com.hivemq.edge.write.JsonToOpcUAConverter");

    private final @NotNull OpcUaClient client;
    private final @NotNull DataTypeTree tree;
    private final @NotNull JsonSchemaGenerator jsonSchemaGenerator;

    public JsonToOpcUAConverter(final @NotNull OpcUaClient client) {
        this.client = client;
        try {
            this.tree = client.getDataTypeTree();
        } catch (final UaException e) {
            throw new RuntimeException(e);
        }
        this.jsonSchemaGenerator = new JsonSchemaGenerator(client, new ObjectMapper());
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

            final var dataType = tree.getDataType(dataTypeNodeId);
            if (dataType == null) {
                log.warn("No data type was found in the DataTypeTree for dataType with nodeId '{}'.", dataTypeNodeId);
                throw new RuntimeException("No data type was found in the DataTypeTree for node id '" +
                        dataTypeNodeId +
                        "'");
            }
            log.debug("DataType NodeId '{}' represents data type '{}'.", dataTypeNodeId, dataType.getBrowseName().getName());

            final var builtinType = tree.getBuiltinType(dataType.getNodeId());
            log.debug(
                    "Destination Node '{}' has DataType NodeId '{}' representing DataType '{}' with builtin type '{}'. The Json '{}' is parsed to this.",
                    destinationNodeId,
                    dataTypeNodeId,
                    dataType,
                    builtinType,
                    rootNode);
            if (builtinType != OpcUaDataType.ExtensionObject) {
                if(rootNode.isArray()) {
                    return generateArrayFromArrayNode((ArrayNode) rootNode, builtinType);
                } else {
                    return parsetoOpcUAObject(builtinType, rootNode);
                }
            }

            final var field = jsonSchemaGenerator.processExtensionObject(dataType, true, null);

            final var datatTypesToRegister = new ArrayList<DataType>();
            collectCustomDatatypes(field, datatTypesToRegister);
            datatTypesToRegister.forEach(dataTypeToRegister -> {
                try {
                    client.getStaticDataTypeManager()
                        .registerType(
                                dataTypeToRegister.getNodeId(),
                                new DynamicStructCodec(dataTypeToRegister, client.getDataTypeTree()),
                                dataTypeToRegister.getBinaryEncodingId(),
                                null,
                                null
                        );
                } catch (final UaException e) {
                    throw new RuntimeException(e);
                }
            });

            final LinkedHashMap<String, Object> dataTypeMap = new LinkedHashMap<>();
            field.nestedFields()
                    .forEach(nestedField -> {
                        final var key = nestedField.name();
                        final var jsonNode = rootNode.get(key);
                        dataTypeMap.put(nestedField.name(), parseToOpcUACompatibleObject(jsonNode, nestedField));
                    });

            return ExtensionObject.encode(client.getDynamicEncodingContext(), new DynamicStructType(dataType, dataTypeMap));

        } catch (final UaException e) {
            throw new RuntimeException(e);
        }
    }

    private void collectCustomDatatypes(final @NotNull JsonSchemaGenerator.FieldInformation fieldInformation, final @NotNull List<DataType> result) {
        if(fieldInformation.customDataType() != null) {
            result.add(fieldInformation.customDataType());
        }
        if(fieldInformation.nestedFields() != null) {
            for (final JsonSchemaGenerator.FieldInformation nestedField : fieldInformation.nestedFields()) {
                collectCustomDatatypes(nestedField, result);
            }
        }
    }


    private @NotNull Object parseToOpcUACompatibleObject(
            final @NotNull JsonNode jsonNode, final @NotNull JsonSchemaGenerator.FieldInformation fieldType) {
        final OpcUaDataType builtinDataType = fieldType.dataType();

        if (builtinDataType == OpcUaDataType.ExtensionObject || fieldType.customDataType() != null) {
            final String namespaceURI = fieldType.namespaceUri();
            final ExpandedNodeId expandedNodeId = ExpandedNodeId.of(namespaceURI, fieldType.customDataType().getBrowseName().getName());

            final Optional<NodeId> optionalDataTypeId = expandedNodeId.toNodeId(client.getNamespaceTable());
            if (optionalDataTypeId.isEmpty()) {
                log.warn("Expanded node id '{}}' could not be parsed to node id.", expandedNodeId);
                throw new RuntimeException("Expanded node id '" + expandedNodeId + "' could not be parsed to node id.");
            }

            final NodeId dataTypeId = optionalDataTypeId.get();
            final DataType dataType = tree.getDataType(dataTypeId);

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
            return extractExtensionObject(jsonNode, fieldType);
        }

        return parsetoOpcUAObject(builtinDataType, jsonNode);
    }

    private @NotNull Object parsetoOpcUAObject(
            final @NotNull OpcUaDataType builtinDataType, final @NotNull JsonNode jsonNode) {

        return switch (builtinDataType) {
            case Boolean -> extractBoolean(jsonNode);
            case Byte -> extractUnsignedByte(jsonNode);
            case SByte -> extractSByte(jsonNode);
            case UInt16 -> extractUShort(jsonNode);
            case UInt32 -> extractUInteger(jsonNode);
            case Int16 -> extractSignedShort(jsonNode);
            case Int32 -> extractSignedInteger(jsonNode);
            case Int64 -> extractLong(jsonNode);
            case UInt64 -> extractUnsignedLong(jsonNode);
            case Float -> extractFloat(jsonNode);
            case Double -> extractDouble(jsonNode);
            case String -> extractString(jsonNode);
            case DateTime -> extractDateTime(jsonNode);
            case Guid -> extractGuid(jsonNode);
            case ByteString -> extractByteString(jsonNode);
            case XmlElement -> extractXmlElement(jsonNode);
            case NodeId -> extractNodeId(jsonNode);
            case ExpandedNodeId -> extractExpandedNodeId(jsonNode);
            case StatusCode -> extractStatusCode(jsonNode);
            case QualifiedName -> extractQualifiedName(jsonNode);
            case LocalizedText -> extractLocalizedText(jsonNode);
            case DataValue -> throw new NotImplementedException(); // TODO DataValue is too complex for now
            case Variant -> throw new NotImplementedException(); // TODO Variant is too complex for now
            case DiagnosticInfo -> {
                log.error("DiagnosticInfo is not supported for writing to OPCUA. This is a readonly type.");
                throw new NotImplementedException();
            }
            default -> throw createException(jsonNode, builtinDataType.name());
        };
    }

    private @NotNull DynamicStructType extractExtensionObject(
            final @NotNull JsonNode jsonNode, final @NotNull JsonSchemaGenerator.FieldInformation fieldInformation) {

        final var fields = new LinkedHashMap<String, Object>();
        fieldInformation.nestedFields().forEach(field -> {
            final String key = field.name();
            final JsonNode nestedObjectNode = jsonNode.get(key);
            if (nestedObjectNode == null) {
                throw new RuntimeException("No nested json was found for key '" + key + "'.");
            }
            final Object parsed = parseToOpcUACompatibleObject(nestedObjectNode, field);
            fields.put(key, parsed);
        });

        return new DynamicStructType(fieldInformation.customDataType(), fields);
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
        throw createException(jsonNode, OpcUaDataType.StatusCode.name());
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
        throw createException(jsonNode, OpcUaDataType.ExpandedNodeId.name());
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
        throw createException(jsonNode, OpcUaDataType.NodeId.name());
    }

    static XmlElement extractXmlElement(final JsonNode jsonNode) {
        if (jsonNode.isTextual()) {
            return XmlElement.of(jsonNode.asText());
        }
        throw createException(jsonNode, OpcUaDataType.XmlElement.name());
    }

    static ByteString extractByteString(final JsonNode jsonNode) {
        if (jsonNode.isTextual()) {
            return ByteString.of(Base64.getDecoder().decode(jsonNode.asText()));
        }
        throw createException(jsonNode, OpcUaDataType.ByteString.name());
    }

    private static UUID extractGuid(final JsonNode jsonNode) {
        if (jsonNode.isTextual()) {
            return UUID.fromString(jsonNode.asText());
        }
        throw createException(jsonNode, Guid.name());
    }

    private static DateTime extractDateTime(final JsonNode jsonNode) {
        if (jsonNode.isTextual()) {
            return new DateTime(Date.from(Instant.parse(jsonNode.asText())));
        }
        throw createException(jsonNode, OpcUaDataType.DateTime.name());
    }

    static @NotNull String extractString(final JsonNode jsonNode) {
        if (jsonNode.isTextual()) {
            return jsonNode.asText();
        }
        throw createException(jsonNode, OpcUaDataType.String.name());
    }


    static double extractDouble(final JsonNode jsonNode) {
        if (jsonNode.isDouble()) {
            return jsonNode.asDouble();
        }

        if (jsonNode.isInt()) {
            final double parsedDouble = jsonNode.intValue();
            final int parsedIntegerBack = (int) parsedDouble;
            if (parsedIntegerBack != jsonNode.intValue()) {
                throw new IllegalArgumentException(String.format(
                        "An integer was supplied for a double node that is not representable without rounding. Input Integer: '%d', Output Double: '%f'. To avoid inaccuracies the publish will not be consumed.",
                        jsonNode.intValue(),
                        parsedDouble));
            }
            return parsedDouble;
        }

        throw createException(jsonNode, OpcUaDataType.Double.name());
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

        throw createException(jsonNode, OpcUaDataType.Float.name());
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
                throw createOverflowException(value, OpcUaDataType.Byte.name());
            } else if (value < UByte.MIN_VALUE) {
                throw createUnderflowException(value, OpcUaDataType.Byte.name());
            }
            return UByte.valueOf((byte) value);
        }
        throw createException(jsonNode, OpcUaDataType.Byte.name());
    }

    @NotNull
    static UInteger extractUInteger(final JsonNode jsonNode) {
        if (jsonNode.isInt() || jsonNode.isLong()) {
            final long value = jsonNode.longValue();
            if (value > UInteger.MAX_VALUE) {
                throw createOverflowException(value, OpcUaDataType.UInt32.name());
            } else if (value < UInteger.MIN_VALUE) {
                throw createUnderflowException(value, OpcUaDataType.UInt32.name());
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
            throw createException(jsonNode, OpcUaDataType.Boolean.name());
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

    private Object[] generateArrayFromArrayNode(final @NotNull ArrayNode arrayNode, final @NotNull OpcUaDataType type) {
        final Object[] ret = (Object[])Array.newInstance(type.getBackingClass(), arrayNode.size());

        for (int i = 0; i < arrayNode.size(); i++) {
            final JsonNode arrayEntry = arrayNode.get(i);
            if (arrayEntry.isArray()) {
                ret[i] = generateArrayFromArrayNode((ArrayNode) arrayEntry, type);
            } else {
                ret[i] = parsetoOpcUAObject(type, arrayEntry);
            }
        }
        return ret;
    }
}
