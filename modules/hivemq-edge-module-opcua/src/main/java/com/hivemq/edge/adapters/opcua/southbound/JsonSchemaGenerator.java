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

public class JsonSchemaGenerator {

    private final @NotNull OpcUaClient client;
    private final @NotNull DataTypeTree tree;
    private final @NotNull BuiltinJsonSchema builtinJsonSchema;
    private final @NotNull ObjectMapper objectMapper;

    public record FieldInformation(
            String name,
            String namespaceUri,
            OpcUaDataType dataType,
            DataType customDataType,
            boolean isEnum,
            UInteger[] arrayDimensions,
            boolean required,
            List<FieldInformation> nestedFields) {}

    public JsonSchemaGenerator(final @NotNull OpcUaClient client, final @NotNull ObjectMapper objectMapper) {
        this.client = client;
        try {
            this.tree = client.getDataTypeTree();
        } catch (final UaException e) {
            throw new RuntimeException(e);
        }
        this.objectMapper = objectMapper;
        this.builtinJsonSchema = new BuiltinJsonSchema();
    }

    public CompletableFuture<Optional<JsonNode>> createMqttPayloadJsonSchema(final @NotNull OpcuaTag tag) {
        final String nodeId = tag.definition().node();
        final var jsonSchemaGenerator = new JsonSchemaGenerator(client, new ObjectMapper());
        final var parsed = NodeId.parse(nodeId);
        return jsonSchemaGenerator
                .collectTypeInfo(parsed)
                .thenApply(info -> {
                    if(info.arrayDimensions() != null && info.arrayDimensions().length > 0) {
                        return builtinJsonSchema.createJsonSchemaForArrayType(info.dataType(), info.arrayDimensions);
                    }
                    else if(info.nestedFields() == null || info.nestedFields().isEmpty()) {
                        return builtinJsonSchema.createJsonSchemaForBuiltInType(info.dataType());
                    } else {
                        return jsonSchemaGenerator.jsonSchemaFromNodeId(info);
                    }
                })
                .thenApply(Optional::of);
    }

    private CompletableFuture<FieldInformation> collectTypeInfo(final @NotNull NodeId destinationNodeId) {
        final CompletableFuture<UaVariableNode> variableNodeFuture =
                client.getAddressSpace().getVariableNodeAsync(destinationNodeId);

        return variableNodeFuture
                .thenApply(uaVariableNode -> {
                    final NodeId dataTypeNodeId = uaVariableNode.getDataType();
                    final DataType dataType = tree.getDataType(dataTypeNodeId);
                    final UInteger[] dimensions = uaVariableNode.getArrayDimensions();

                    if (dataType == null) {
                        throw new RuntimeException("Unable to find the data type for the given node id '" + destinationNodeId + "'.");
                    }
                    final OpcUaDataType builtinType = tree.getBuiltinType(dataType.getNodeId());

                    if (builtinType != OpcUaDataType.ExtensionObject) {
                        return new FieldInformation(
                                null, //No name since this is the root
                                dataType.getNodeId().expanded(client.getNamespaceTable()).getNamespaceUri(),
                                builtinType,
                                null,
                                false,
                                dimensions,
                                true,
                                List.of());
                    } else {
                        if (dataType.getBinaryEncodingId() == null) {
                            throw new RuntimeException("No encoding was present for the complex data type: '" + dataType + "'.");
                        }
                        return processExtensionObject(dataType,true, null);
                    }
                }).exceptionally(throwable -> {
                    throw new RuntimeException("Problem accessing node", throwable);
                });
    }

    public FieldInformation processExtensionObject(final @NotNull DataType dataType, final boolean required, final @Nullable String name) {
        try {

            final var dataTypeDefiniton = dataType.getDataTypeDefinition();

            if(dataTypeDefiniton instanceof final StructureDefinition structureDefiniton) {
                if(structureDefiniton.getFields() != null) {
                    final var properties = Arrays.stream(structureDefiniton.getFields()).map(field -> {
                        final String localPart;
                        final DataType extractedDataType;
                        try {
                            extractedDataType= client.getDataTypeTree().getDataType(field.getDataType());
                            if(extractedDataType == null) {
                                throw new RuntimeException("Unsupported type definition: " + dataTypeDefiniton);
                            }
                            localPart = extractedDataType.getBrowseName().name();
                        } catch (final UaException e) {
                            throw new RuntimeException(e);
                        }
                        final var fieldName = field.getName();
                        final var isRequired = !field.getIsOptional();
                        final var namespaceUri = field.getDataType().expanded(client.getNamespaceTable()).getNamespaceUri();
                        final boolean isStandard = namespaceUri != null && namespaceUri.startsWith("http://opcfoundation.org/");
                        final var opcUaType = isStandard ? OpcUaDataType.valueOf(localPart) : null;

                        if (isStandard) {
                            return new FieldInformation(
                                    fieldName,
                                    namespaceUri,
                                    opcUaType,
                                    null,
                                    false,
                                    null,
                                    isRequired,
                                    List.of());
                        } else {
                            return processExtensionObject(
                                    extractedDataType,
                                    isRequired,
                                    fieldName);
                        }
                    }).toList();

                    return new FieldInformation(
                            name,
                            client.getNamespaceTable().get(dataType.getBrowseName().getNamespaceIndex()),
                            null,
                            dataType,
                            false,
                            null,
                            required,
                            properties);
                } else {
                    return new FieldInformation(
                            name,
                            client.getNamespaceTable().get(dataType.getBrowseName().getNamespaceIndex()),
                            null,
                            dataType,
                            false,
                            null,
                            required,
                            List.of());
                }

            } else if (dataTypeDefiniton instanceof EnumDefinition) {
                throw new RuntimeException("Enums not implemented yet");
            } else {
                throw new RuntimeException("Unsupported type definition: " + dataTypeDefiniton);
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void addNestedStructureInformation(
            final @NotNull ObjectNode propertiesNode,
            final @NotNull FieldInformation fieldType) {
        final OpcUaDataType builtinDataType = fieldType.dataType;

        final ObjectNode nestedPropertiesNode = objectMapper.createObjectNode();
        propertiesNode.set(fieldType.name(), nestedPropertiesNode);

        if (builtinDataType != OpcUaDataType.ExtensionObject && fieldType.customDataType() == null) {
            BuiltinJsonSchema.populatePropertiesForBuiltinType(nestedPropertiesNode, builtinDataType, objectMapper);
        } else if(fieldType.arrayDimensions() != null && fieldType.arrayDimensions().length > 0) {
            BuiltinJsonSchema.populatePropertiesForArray(nestedPropertiesNode, builtinDataType, objectMapper, fieldType.arrayDimensions());
        } else {
            nestedPropertiesNode.set("type", new TextNode("object"));
            final ObjectNode innerProperties = objectMapper.createObjectNode();
            nestedPropertiesNode.set("properties", innerProperties);

            if(fieldType.namespaceUri() != null) {
                verifyDataTypeforField(fieldType);
            }

            final ArrayNode requiredAttributesArray = objectMapper.createArrayNode();
            for (final FieldInformation entry : fieldType.nestedFields()) {
                if(entry.required()) {
                    requiredAttributesArray.add(entry.name());
                }
                addNestedStructureInformation(innerProperties, entry);
            }
            nestedPropertiesNode.set("required", requiredAttributesArray);
        }
    }

    private void verifyDataTypeforField(final @NotNull FieldInformation fieldType) {
        client.getStaticDataTypeManager().getTypeDictionary(fieldType.namespaceUri());

        final ExpandedNodeId expandedNodeId = fieldType.customDataType() == null ? fieldType.dataType().getNodeId().expanded() : fieldType.customDataType().getNodeId().expanded();
        final Optional<NodeId> optionalDataTypeId = expandedNodeId.toNodeId(client.getNamespaceTable());
        if (optionalDataTypeId.isEmpty()) {
            throw new RuntimeException("Expanded node id '" + expandedNodeId + "' could not be parsed to node id.");
        }

        final NodeId dataTypeId = optionalDataTypeId.get();
        final DataType dataType = tree.getDataType(dataTypeId);
        if (dataType == null) {
            throw new RuntimeException("No data type was found in the DataTypeTree for node id '" +
                    dataTypeId +
                    "'");
        }
        final NodeId binaryEncodingId = dataType.getBinaryEncodingId();
        if (binaryEncodingId == null) {
            throw new RuntimeException("Binary encoding id was null for nested struct.");
        }
    }

    private @NotNull JsonNode jsonSchemaFromNodeId(final @NotNull FieldInformation fieldInformation) {

        final ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.set("$schema", new TextNode("https://json-schema.org/draft/2020-12/schema"));
        rootNode.set("title", new TextNode("CustomStruct: " + (fieldInformation.dataType() != null ? fieldInformation.dataType().getNodeId().toParseableString() : fieldInformation.customDataType().getNodeId().toParseableString())));
        rootNode.set("type", new TextNode("object"));

        final ObjectNode valueNode = objectMapper.createObjectNode();
        rootNode.set("value", valueNode);
        valueNode.set("type", new TextNode("object"));

        final ObjectNode propertiesNode = objectMapper.createObjectNode();
        valueNode.set("properties", propertiesNode);


        final ArrayNode requiredAttributesArray = objectMapper.createArrayNode();
        fieldInformation.nestedFields()
                .forEach(fieldInfo -> {
                    if(fieldInfo.required()) {
                        requiredAttributesArray.add(fieldInfo.name());
                    }
                    addNestedStructureInformation(propertiesNode, fieldInfo);
                });

        valueNode.set("required", requiredAttributesArray);

        final ArrayNode requiredProperties = objectMapper.createArrayNode();
        requiredProperties.add("value");
        rootNode.set("required", requiredProperties);
        return rootNode;
    }

}
