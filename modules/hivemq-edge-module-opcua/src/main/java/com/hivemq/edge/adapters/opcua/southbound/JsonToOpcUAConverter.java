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
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.core.types.DynamicStructType;
import org.eclipse.milo.opcua.sdk.core.types.codec.DynamicStructCodec;
import org.eclipse.milo.opcua.sdk.core.typetree.DataType;
import org.eclipse.milo.opcua.sdk.core.typetree.DataTypeTree;
import org.eclipse.milo.opcua.stack.core.OpcUaDataType;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExtensionObject;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Optional;

import static com.hivemq.edge.adapters.opcua.southbound.JsonToOpcUAConverterUtil.collectCustomDatatypes;
import static com.hivemq.edge.adapters.opcua.southbound.JsonToOpcUAConverterUtil.generateArrayFromArrayNode;
import static com.hivemq.edge.adapters.opcua.southbound.JsonToOpcUAConverterUtil.parsetoOpcUAObject;

public class JsonToOpcUAConverter {

    private static final @NotNull Logger log = LoggerFactory.getLogger(JsonToOpcUAConverter.class);

    private final @NotNull OpcUaClient client;
    private final @NotNull DataTypeTree tree;
    private final @NotNull JsonSchemaGenerator jsonSchemaGenerator;

    public JsonToOpcUAConverter(final @NotNull OpcUaClient client) {
        try {
            this.tree = client.getDataTypeTree();
        } catch (final UaException e) {
            throw new RuntimeException(e);
        }
        this.client = client;
        this.jsonSchemaGenerator = new JsonSchemaGenerator(client);
    }

    public @NotNull Object convertToOpcUAValue(
            final @NotNull JsonNode rootNode,
            final @NotNull NodeId destinationNodeId) {
        if (log.isDebugEnabled()) {
            log.debug("Convert json '{}' to opcua compatible object for destination nodeId '{}'.",
                    rootNode,
                    destinationNodeId);
        }
        try {
            final NodeId dataTypeNodeId = client.getAddressSpace().getVariableNode(destinationNodeId).getDataType();
            if (dataTypeNodeId == null) {
                log.warn("No dataType-nodeId was found for the destination nodeId '{}'.", destinationNodeId);
                throw new RuntimeException("No dataType-nodeId was found for the destination nodeId " +
                        destinationNodeId +
                        "'");
            }
            if (log.isDebugEnabled()) {
                log.debug("Destination NodeId '{}' has DataType NodeId '{}'.", destinationNodeId, dataTypeNodeId);
            }

            final var dataType = tree.getDataType(dataTypeNodeId);
            if (dataType == null) {
                log.warn("No data type was found in the DataTypeTree for dataType with nodeId '{}'.", dataTypeNodeId);
                throw new RuntimeException("No data type was found in the DataTypeTree for node id '" +
                        dataTypeNodeId +
                        "'");
            }
            if (log.isDebugEnabled()) {
                log.debug("DataType NodeId '{}' represents data type '{}'.",
                        dataTypeNodeId,
                        dataType.getBrowseName().getName());
            }

            final var builtinType = tree.getBuiltinType(dataType.getNodeId());

            if (log.isDebugEnabled()) {
                log.debug(
                        "Destination Node '{}' has DataType NodeId '{}' representing DataType '{}' with builtin type '{}'. The Json '{}' is parsed to this.",
                        destinationNodeId,
                        dataTypeNodeId,
                        dataType,
                        builtinType,
                        rootNode);
            }

            if (builtinType != OpcUaDataType.ExtensionObject) {
                if (rootNode.isArray()) {
                    return generateArrayFromArrayNode((ArrayNode) rootNode, builtinType);
                } else {
                    return parsetoOpcUAObject(builtinType, rootNode);
                }
            }

            final var field = jsonSchemaGenerator.processExtensionObject(dataType, true, null);

            final var dataTypesToRegister = new ArrayList<DataType>();
            collectCustomDatatypes(field, dataTypesToRegister);
            dataTypesToRegister.forEach(dataTypeToRegister -> {
                try {
                    client.getStaticDataTypeManager()
                            .registerType(dataTypeToRegister.getNodeId(),
                                    new DynamicStructCodec(dataTypeToRegister, client.getDataTypeTree()),
                                    dataTypeToRegister.getBinaryEncodingId(),
                                    null,
                                    null);
                } catch (final UaException e) {
                    throw new RuntimeException(e);
                }
            });

            final LinkedHashMap<String, Object> dataTypeMap = new LinkedHashMap<>();
            field.nestedFields().forEach(nestedField -> {
                final var key = nestedField.name();
                final var jsonNode = rootNode.get(key);
                dataTypeMap.put(key, parseToOpcUACompatibleObject(jsonNode, nestedField));
            });

            return ExtensionObject.encode(client.getDynamicEncodingContext(),
                    new DynamicStructType(dataType, dataTypeMap));
        } catch (final UaException e) {
            throw new RuntimeException(e);
        }
    }

    private @NotNull Object parseToOpcUACompatibleObject(
            final @NotNull JsonNode jsonNode,
            final @NotNull JsonSchemaGenerator.FieldInformation fieldType) {
        final OpcUaDataType builtinDataType = fieldType.dataType();

        if (builtinDataType == OpcUaDataType.ExtensionObject || fieldType.customDataType() != null) {
            final String namespaceURI = fieldType.namespaceUri();
            final ExpandedNodeId expandedNodeId =
                    ExpandedNodeId.of(namespaceURI, fieldType.customDataType().getBrowseName().getName());

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

    private @NotNull DynamicStructType extractExtensionObject(
            final @NotNull JsonNode jsonNode,
            final @NotNull JsonSchemaGenerator.FieldInformation fieldInformation) {

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
}
