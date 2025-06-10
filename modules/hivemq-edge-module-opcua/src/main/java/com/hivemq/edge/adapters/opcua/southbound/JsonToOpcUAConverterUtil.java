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
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.milo.opcua.sdk.core.typetree.DataType;
import org.eclipse.milo.opcua.stack.core.OpcUaDataType;
import org.eclipse.milo.opcua.stack.core.UaRuntimeException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
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
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.eclipse.milo.opcua.stack.core.OpcUaDataType.Guid;
import static org.eclipse.milo.opcua.stack.core.OpcUaDataType.Int16;
import static org.eclipse.milo.opcua.stack.core.OpcUaDataType.Int32;
import static org.eclipse.milo.opcua.stack.core.OpcUaDataType.Int64;
import static org.eclipse.milo.opcua.stack.core.OpcUaDataType.SByte;
import static org.eclipse.milo.opcua.stack.core.OpcUaDataType.UInt16;
import static org.eclipse.milo.opcua.stack.core.OpcUaDataType.UInt32;
import static org.eclipse.milo.opcua.stack.core.OpcUaDataType.UInt64;

final class JsonToOpcUAConverterUtil {

    private static final @NotNull Logger log = LoggerFactory.getLogger(JsonToOpcUAConverterUtil.class);

    private JsonToOpcUAConverterUtil() {
        throw new UnsupportedOperationException();
    }

    static @NotNull LocalizedText extractLocalizedText(final @NotNull JsonNode jsonNode) {
        if (jsonNode.isTextual()) {
            return LocalizedText.english(jsonNode.asText());
        }
        throw createException(jsonNode, "LocalizedText");
    }

    static @NotNull QualifiedName extractQualifiedName(final @NotNull JsonNode jsonNode) {
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

    static @NotNull StatusCode extractStatusCode(final @NotNull JsonNode jsonNode) {
        if (jsonNode.isInt()) {
            return new StatusCode(jsonNode.asInt());
        }
        throw createException(jsonNode, OpcUaDataType.StatusCode.name());
    }

    static @NotNull ExpandedNodeId extractExpandedNodeId(final @NotNull JsonNode jsonNode) {
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

    static @NotNull NodeId extractNodeId(final @NotNull JsonNode jsonNode) {
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

    static @NotNull XmlElement extractXmlElement(final @NotNull JsonNode jsonNode) {
        if (jsonNode.isTextual()) {
            return XmlElement.of(jsonNode.asText());
        }
        throw createException(jsonNode, OpcUaDataType.XmlElement.name());
    }

    static @NotNull ByteString extractByteString(final @NotNull JsonNode jsonNode) {
        if (jsonNode.isTextual()) {
            return ByteString.of(Base64.getDecoder().decode(jsonNode.asText()));
        }
        throw createException(jsonNode, OpcUaDataType.ByteString.name());
    }

    static @NotNull UUID extractGuid(final @NotNull JsonNode jsonNode) {
        if (jsonNode.isTextual()) {
            return UUID.fromString(jsonNode.asText());
        }
        throw createException(jsonNode, Guid.name());
    }

    static @NotNull DateTime extractDateTime(final @NotNull JsonNode jsonNode) {
        if (jsonNode.isTextual()) {
            return new DateTime(Date.from(Instant.parse(jsonNode.asText())));
        }
        throw createException(jsonNode, OpcUaDataType.DateTime.name());
    }

    static @NotNull String extractString(final @NotNull JsonNode jsonNode) {
        if (jsonNode.isTextual()) {
            return jsonNode.asText();
        }
        throw createException(jsonNode, OpcUaDataType.String.name());
    }

    static double extractDouble(final @NotNull JsonNode jsonNode) {
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

    @VisibleForTesting
    static float extractFloat(final @NotNull JsonNode jsonNode) {
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

    static @NotNull ULong extractUnsignedLong(final @NotNull JsonNode jsonNode) {
        if (jsonNode.isLong()) {
            return ULong.valueOf(jsonNode.asLong());
        }
        throw createException(jsonNode, UInt64.name());
    }

    static long extractLong(final @NotNull JsonNode jsonNode) {
        if (jsonNode.isLong()) {
            return jsonNode.longValue();
        }
        throw createException(jsonNode, Int64.name());
    }

    @VisibleForTesting
    static int extractSignedInteger(final @NotNull JsonNode jsonNode) {
        if (jsonNode.isLong() && !jsonNode.canConvertToInt()) {
            throw createOverflowException(jsonNode, Int32.name());
        }
        if (jsonNode.isInt()) {
            return jsonNode.intValue();
        }
        throw createException(jsonNode, Int32.name());
    }

    static short extractSignedShort(final @NotNull JsonNode jsonNode) {
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

    @VisibleForTesting
    static byte extractSByte(final @NotNull JsonNode jsonNode) {
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

    @VisibleForTesting
    static @NotNull UByte extractUnsignedByte(final @NotNull JsonNode jsonNode) {
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


    @VisibleForTesting
    static @NotNull UInteger extractUInteger(final @NotNull JsonNode jsonNode) {
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

    static @NotNull UShort extractUShort(final @NotNull JsonNode jsonNode) {
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

    static boolean extractBoolean(final @NotNull JsonNode jsonNode) {
        if (jsonNode.isBoolean()) {
            return jsonNode.asBoolean();
        }
        throw createException(jsonNode, OpcUaDataType.Boolean.name());
    }

    static @NotNull IllegalArgumentException createException(
            final @NotNull Object value,
            final @NotNull String intendedClass) {
        log.warn("Can not convert '{}' of class '{}' to '{}'.", value, value.getClass().getSimpleName(), intendedClass);
        throw new IllegalArgumentException("Can not convert '" +
                value +
                "' of class '" +
                value.getClass().getSimpleName() +
                "' to " +
                intendedClass +
                '.');
    }

    static @NotNull IllegalArgumentException createOverflowException(
            final @NotNull Object value,
            final @NotNull String intendedClass) {
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
            final @NotNull Object value,
            final @NotNull String intendedClass) {
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

    static void collectCustomDatatypes(
            final @NotNull JsonSchemaGenerator.FieldInformation fieldInformation,
            final @NotNull List<DataType> result) {
        if (fieldInformation.customDataType() != null) {
            result.add(fieldInformation.customDataType());
        }
        if (fieldInformation.nestedFields() != null) {
            for (final JsonSchemaGenerator.FieldInformation nestedField : fieldInformation.nestedFields()) {
                collectCustomDatatypes(nestedField, result);
            }
        }
    }

    static @NotNull Object parsetoOpcUAObject(
            final @NotNull OpcUaDataType builtinDataType,
            final @NotNull JsonNode jsonNode) {

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

    static @NotNull Object @NotNull [] generateArrayFromArrayNode(
            final @NotNull ArrayNode arrayNode,
            final @NotNull OpcUaDataType type) {
        final Object[] ret = (Object[]) Array.newInstance(type.getBackingClass(), arrayNode.size());
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
