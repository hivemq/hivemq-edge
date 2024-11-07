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
import org.jetbrains.annotations.Nullable;
import org.opcfoundation.opcua.binaryschema.FieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.hivemq.edge.adapters.opcua.mqtt2opcua.BuiltInDataTypeConverter.convertFieldTypeToBuiltInDataType;

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
            final @NotNull JsonNode rootNode,
            final @NotNull NodeId destinationNodeId) {
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
                return parsetoOpcUAObject(builtinType, rootNode, null);
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

    private @Nullable Object parseToOpcUACompatibleObject(
            final @NotNull JsonNode jsonNode,
            final @NotNull FieldType fieldType) {
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
            return parsetoOpcUAObject(builtinDataType, jsonNode, binaryEncodingId);
        }
        return parsetoOpcUAObject(builtinDataType, jsonNode, null);
    }

    private @Nullable Object parsetoOpcUAObject(
            final @NotNull BuiltinDataType builtinDataType,
            final @NotNull JsonNode jsonNode,
            final @Nullable NodeId binaryEncodingId) {
        switch (builtinDataType) {
            case Boolean:
                if (jsonNode.isBoolean()) {
                    return jsonNode.asBoolean();
                } else {
                    throw createException(jsonNode, builtinDataType.name());
                }
            case Byte:
                if (jsonNode.isTextual()) {
                    final byte[] decoded = Base64.getDecoder().decode(jsonNode.asText());
                    if (decoded.length > 1) {
                        throw new RuntimeException(
                                "Single byte was expected to be encoded, but multiple bytes were encoded in '" +
                                        jsonNode.asText() +
                                        "'.");
                    } else if (decoded.length == 0) {
                        throw new RuntimeException(
                                "Single byte was expected to be encoded, but no bytes were encoded in '" +
                                        jsonNode.asText() +
                                        "'.");
                    } else {
                        return UByte.valueOf(decoded[0]);
                    }
                }
                throw createException(jsonNode, builtinDataType.name());
            case SByte:
                if (jsonNode.isTextual()) {
                    final byte[] decoded = Base64.getDecoder().decode(jsonNode.asText());
                    if (decoded.length > 1) {
                        throw new RuntimeException(
                                "Single byte was expected to be encoded, but multiple bytes were encoded in '" +
                                        jsonNode.asText() +
                                        "'.");
                    } else if (decoded.length == 0) {
                        throw new RuntimeException(
                                "Single byte was expected to be encoded, but no bytes were encoded in '" +
                                        jsonNode.asText() +
                                        "'.");
                    } else {
                        return decoded[0];
                    }
                }
                throw createException(jsonNode, builtinDataType.name());
            case UInt16:
                if (jsonNode.isShort()) {
                    return UShort.valueOf(jsonNode.intValue());
                }
                throw createException(jsonNode, builtinDataType.name());
            case UInt32:
                if (jsonNode.isInt()) {
                    return UInteger.valueOf(jsonNode.intValue());
                }
                throw createException(jsonNode, builtinDataType.name());
            case Int16:
                if (jsonNode.isShort()) {
                    return jsonNode.shortValue();
                }
                throw createException(jsonNode, builtinDataType.name());
            case Int32:
                if (jsonNode.isInt()) {
                    return jsonNode.intValue();
                }
                throw createException(jsonNode, builtinDataType.name());
            case Int64:
                if (jsonNode.isLong()) {
                    return jsonNode.longValue();
                }
                throw createException(jsonNode, builtinDataType.name());
            case UInt64:
                if (jsonNode.isLong()) {
                    return ULong.valueOf(jsonNode.asLong());
                }
                throw createException(jsonNode, builtinDataType.name());
            case Float:
                if (jsonNode.isFloat()) {
                    return jsonNode.floatValue();
                }
                throw createException(jsonNode, builtinDataType.name());
            case Double:
                if (jsonNode.isDouble()) {
                    return jsonNode.asDouble();
                }
                throw createException(jsonNode, builtinDataType.name());
            case String:
                if (jsonNode.isTextual()) {
                    return jsonNode.asText();
                }
                throw createException(jsonNode, builtinDataType.name());
            case DateTime:
                if (jsonNode.isLong()) {
                    return new DateTime(jsonNode.asLong());
                }
                throw createException(jsonNode, builtinDataType.name());
            case Guid:
                if (jsonNode.isTextual()) {
                    return UUID.fromString(jsonNode.asText());
                }
                throw createException(jsonNode, builtinDataType.name());
            case ByteString:
                if (jsonNode.isTextual()) {
                    return ByteString.of(BaseEncoding.base64().decode(jsonNode.asText()));
                }
                throw createException(jsonNode, builtinDataType.name());
            case XmlElement:
                if (jsonNode.isTextual()) {
                    return XmlElement.of(jsonNode.asText());
                }
                throw createException(jsonNode, builtinDataType.name());
            case NodeId:
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
                throw createException(jsonNode, builtinDataType.name());
            case ExpandedNodeId:
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
                throw createException(jsonNode, builtinDataType.name());
            case StatusCode:
                if (jsonNode.isInt()) {
                    return new StatusCode(jsonNode.asInt());
                }
                throw createException(jsonNode, builtinDataType.name());
            case QualifiedName:
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
            case LocalizedText:
                if (jsonNode.isTextual()) {
                    return LocalizedText.english(jsonNode.asText());
                }
                throw createException(jsonNode, "LocalizedText");
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
            case ExtensionObject:
                if (binaryEncodingId == null) {
                    throw new RuntimeException("Binary encoding id was null for nested struct. ");
                }

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
        throw createException(jsonNode, builtinDataType.name());
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

    private static @NotNull IllegalArgumentException createException(
            final Object value,
            final @NotNull String intendedClass) {
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
}
