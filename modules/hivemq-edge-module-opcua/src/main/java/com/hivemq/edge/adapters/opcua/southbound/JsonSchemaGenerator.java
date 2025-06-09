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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.core.typetree.DataType;
import org.eclipse.milo.opcua.sdk.core.typetree.DataTypeTree;
import org.eclipse.milo.opcua.stack.core.OpcUaDataType;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.EnumDefinition;
import org.eclipse.milo.opcua.stack.core.types.structured.StructureDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.hivemq.edge.adapters.opcua.Constants.OBJECT_DATA_TYPE;
import static com.hivemq.edge.adapters.opcua.Constants.TYPE;
import static com.hivemq.edge.adapters.opcua.southbound.BuiltinJsonSchema.MAPPER;
import static com.hivemq.edge.adapters.opcua.southbound.BuiltinJsonSchema.SCHEMA_URI;
import static com.hivemq.edge.adapters.opcua.southbound.BuiltinJsonSchema.createJsonSchemaForArrayType;
import static com.hivemq.edge.adapters.opcua.southbound.BuiltinJsonSchema.createJsonSchemaForBuiltInType;
import static com.hivemq.edge.adapters.opcua.southbound.BuiltinJsonSchema.populatePropertiesForArray;
import static com.hivemq.edge.adapters.opcua.southbound.BuiltinJsonSchema.populatePropertiesForBuiltinType;

public class JsonSchemaGenerator {

    private final @NotNull OpcUaClient client;
    private final @NotNull DataTypeTree tree;

    public JsonSchemaGenerator(final @NotNull OpcUaClient client) {
        this.client = client;
        try {
            this.tree = client.getDataTypeTree();
        } catch (final UaException e) {
            throw new RuntimeException(e);
        }
    }

    public @NotNull CompletableFuture<Optional<JsonNode>> createMqttPayloadJsonSchema(final @NotNull OpcuaTag tag) {
        final String nodeId = tag.getDefinition().getNode();
        final var jsonSchemaGenerator = new JsonSchemaGenerator(client);
        final var parsed = NodeId.parse(nodeId);
        return jsonSchemaGenerator.collectTypeInfo(parsed).thenApply(info -> {
            if (info.arrayDimensions() != null && info.arrayDimensions().length > 0) {
                return createJsonSchemaForArrayType(info.dataType(), info.arrayDimensions);
            } else if (info.nestedFields() == null || info.nestedFields().isEmpty()) {
                return createJsonSchemaForBuiltInType(info.dataType());
            } else {
                return jsonSchemaGenerator.jsonSchemaFromNodeId(info);
            }
        }).thenApply(Optional::of);
    }

    private @NotNull CompletableFuture<FieldInformation> collectTypeInfo(final @NotNull NodeId destinationNodeId) {
        final CompletableFuture<UaVariableNode> variableNodeFuture =
                client.getAddressSpace().getVariableNodeAsync(destinationNodeId);

        return variableNodeFuture.thenApply(uaVariableNode -> {
            final NodeId dataTypeNodeId = uaVariableNode.getDataType();
            final DataType dataType = tree.getDataType(dataTypeNodeId);
            final UInteger[] dimensions = uaVariableNode.getArrayDimensions();

            if (dataType == null) {
                throw new RuntimeException("Unable to find the data type for the given node id '" +
                        destinationNodeId +
                        "'.");
            }
            final OpcUaDataType builtinType = tree.getBuiltinType(dataType.getNodeId());

            if (builtinType != OpcUaDataType.ExtensionObject) {
                return new FieldInformation(null,
                        //No name since this is the root
                        dataType.getNodeId().expanded(client.getNamespaceTable()).getNamespaceUri(),
                        builtinType,
                        null,
                        false,
                        dimensions,
                        true,
                        List.of());
            } else {
                if (dataType.getBinaryEncodingId() == null) {
                    throw new RuntimeException("No encoding was present for the complex data type: '" +
                            dataType +
                            "'.");
                }
                return processExtensionObject(dataType, true, null);
            }
        }).exceptionally(throwable -> {
            throw new RuntimeException("Problem accessing node", throwable);
        });
    }

    public @NotNull FieldInformation processExtensionObject(
            final @NotNull DataType dataType,
            final boolean required,
            final @Nullable String name) {
        try {

            final var dataTypeDefinition = dataType.getDataTypeDefinition();
            if (dataTypeDefinition instanceof final StructureDefinition structureDefinition) {
                if (structureDefinition.getFields() != null) {
                    final var properties = Arrays.stream(structureDefinition.getFields()).map(field -> {
                        final String localPart;
                        final DataType extractedDataType;
                        try {
                            extractedDataType = client.getDataTypeTree().getDataType(field.getDataType());
                            if (extractedDataType == null) {
                                throw new RuntimeException("Unsupported type definition: " + dataTypeDefinition);
                            }
                            localPart = extractedDataType.getBrowseName().name();
                        } catch (final UaException e) {
                            throw new RuntimeException(e);
                        }
                        final var fieldName = field.getName();
                        final var isRequired = !field.getIsOptional();
                        final var namespaceUri =
                                field.getDataType().expanded(client.getNamespaceTable()).getNamespaceUri();
                        final boolean isStandard =
                                namespaceUri != null && namespaceUri.startsWith("http://opcfoundation.org/");
                        final var opcUaType = isStandard ? OpcUaDataType.valueOf(localPart) : null;

                        if (isStandard) {
                            return new FieldInformation(fieldName,
                                    namespaceUri,
                                    opcUaType,
                                    null,
                                    false,
                                    null,
                                    isRequired,
                                    List.of());
                        } else {
                            return processExtensionObject(extractedDataType, isRequired, fieldName);
                        }
                    }).toList();

                    return new FieldInformation(name,
                            client.getNamespaceTable().get(dataType.getBrowseName().getNamespaceIndex()),
                            null,
                            dataType,
                            false,
                            null,
                            required,
                            properties);
                } else {
                    return new FieldInformation(name,
                            client.getNamespaceTable().get(dataType.getBrowseName().getNamespaceIndex()),
                            null,
                            dataType,
                            false,
                            null,
                            required,
                            List.of());
                }

            } else if (dataTypeDefinition instanceof EnumDefinition) {
                throw new RuntimeException("Enums not implemented yet");
            } else {
                throw new RuntimeException("Unsupported type definition: " + dataTypeDefinition);
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void addNestedStructureInformation(
            final @NotNull ObjectNode propertiesNode,
            final @NotNull FieldInformation fieldType) {
        final OpcUaDataType builtinDataType = fieldType.dataType;

        final ObjectNode nestedPropertiesNode = MAPPER.createObjectNode();
        propertiesNode.set(fieldType.name(), nestedPropertiesNode);

        if (builtinDataType != OpcUaDataType.ExtensionObject && fieldType.customDataType() == null) {
            populatePropertiesForBuiltinType(nestedPropertiesNode, builtinDataType, MAPPER);
        } else if (fieldType.arrayDimensions() != null && fieldType.arrayDimensions().length > 0) {
            populatePropertiesForArray(nestedPropertiesNode, builtinDataType, MAPPER, fieldType.arrayDimensions());
        } else {
            nestedPropertiesNode.set(TYPE, new TextNode(OBJECT_DATA_TYPE));
            final ObjectNode innerProperties = MAPPER.createObjectNode();
            nestedPropertiesNode.set("properties", innerProperties);

            if (fieldType.namespaceUri() != null) {
                verifyDataTypeForField(fieldType);
            }

            final ArrayNode requiredAttributesArray = MAPPER.createArrayNode();
            for (final FieldInformation entry : fieldType.nestedFields()) {
                if (entry.required()) {
                    requiredAttributesArray.add(entry.name());
                }
                addNestedStructureInformation(innerProperties, entry);
            }
            nestedPropertiesNode.set("required", requiredAttributesArray);
        }
    }

    private void verifyDataTypeForField(final @NotNull FieldInformation fieldType) {
        client.getStaticDataTypeManager().getTypeDictionary(fieldType.namespaceUri());

        final ExpandedNodeId expandedNodeId = fieldType.customDataType() == null ?
                fieldType.dataType().getNodeId().expanded() :
                fieldType.customDataType().getNodeId().expanded();
        final Optional<NodeId> optionalDataTypeId = expandedNodeId.toNodeId(client.getNamespaceTable());
        if (optionalDataTypeId.isEmpty()) {
            throw new RuntimeException("Expanded node id '" + expandedNodeId + "' could not be parsed to node id.");
        }

        final NodeId dataTypeId = optionalDataTypeId.get();
        final DataType dataType = tree.getDataType(dataTypeId);
        if (dataType == null) {
            throw new RuntimeException("No data type was found in the DataTypeTree for node id '" + dataTypeId + "'");
        }
        final NodeId binaryEncodingId = dataType.getBinaryEncodingId();
        if (binaryEncodingId == null) {
            throw new RuntimeException("Binary encoding id was null for nested struct.");
        }
    }

    private @NotNull JsonNode jsonSchemaFromNodeId(final @NotNull FieldInformation fieldInformation) {
        final ObjectNode rootNode = MAPPER.createObjectNode();
        rootNode.set("$schema", new TextNode(SCHEMA_URI));
        rootNode.set("title",
                new TextNode("CustomStruct: " +
                        (fieldInformation.dataType() != null ?
                                fieldInformation.dataType().getNodeId().toParseableString() :
                                fieldInformation.customDataType().getNodeId().toParseableString())));
        rootNode.set(TYPE, new TextNode(OBJECT_DATA_TYPE));

        final ObjectNode valueNode = MAPPER.createObjectNode();
        rootNode.set("value", valueNode);
        valueNode.set(TYPE, new TextNode(OBJECT_DATA_TYPE));

        final ObjectNode propertiesNode = MAPPER.createObjectNode();
        valueNode.set("properties", propertiesNode);


        final ArrayNode requiredAttributesArray = MAPPER.createArrayNode();
        fieldInformation.nestedFields().forEach(fieldInfo -> {
            if (fieldInfo.required()) {
                requiredAttributesArray.add(fieldInfo.name());
            }
            addNestedStructureInformation(propertiesNode, fieldInfo);
        });

        valueNode.set("required", requiredAttributesArray);

        final ArrayNode requiredProperties = MAPPER.createArrayNode();
        requiredProperties.add("value");
        rootNode.set("required", requiredProperties);
        return rootNode;
    }

    public record FieldInformation(String name, String namespaceUri, OpcUaDataType dataType, DataType customDataType,
                                   boolean isEnum, UInteger[] arrayDimensions, boolean required,
                                   List<FieldInformation> nestedFields) {
    }
}
