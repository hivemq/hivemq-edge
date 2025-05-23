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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.core.types.codec.DynamicStructCodec;
import org.eclipse.milo.opcua.sdk.core.typetree.DataType;
import org.eclipse.milo.opcua.sdk.core.typetree.DataTypeTree;
import org.eclipse.milo.opcua.stack.core.OpcUaDataType;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.StructureDefinition;
import org.eclipse.milo.opcua.stack.core.types.structured.StructureField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class JsonSchemaGenerator {

    private static final Logger log = LoggerFactory.getLogger(JsonSchemaGenerator.class);

    private final @NotNull OpcUaClient client;
    private final @NotNull DataTypeTree tree;
    private final @NotNull BuiltinJsonSchema builtinJsonSchema;
    private final @NotNull ObjectMapper objectMapper;

    public record Result(Optional<JsonNode> schema, String errorMessage) {};

    public static CompletableFuture<Result> createMqttPayloadJsonSchema(
            final @NotNull OpcUaClient client, final @NotNull OpcuaTag tag) {
        final String nodeId = tag.getDefinition().getNode();
        try {
            final var jsonSchemaGenerator = new JsonSchemaGenerator(client, new ObjectMapper());
            return jsonSchemaGenerator.createJsonSchema(NodeId.parse(nodeId));
        } catch (UaException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public JsonSchemaGenerator(final @NotNull OpcUaClient client, final @NotNull ObjectMapper objectMapper)
            throws UaException {
        this.client = client;
        this.tree = client.getDataTypeTree();
        this.objectMapper = objectMapper;
        this.builtinJsonSchema = new BuiltinJsonSchema();
    }

    private CompletableFuture<Result> createJsonSchema(final @NotNull NodeId destinationNodeId) {
        final CompletableFuture<UaVariableNode> variableNodeFuture =
                client.getAddressSpace().getVariableNodeAsync(destinationNodeId);
        return variableNodeFuture
                .thenCompose(uaVariableNode -> {
                    final NodeId dataTypeNodeId = uaVariableNode.getDataType();
                    final DataType dataType = tree.getDataType(dataTypeNodeId);
                    final UInteger[] dimensions = uaVariableNode.getArrayDimensions();
                    if (dataType == null) {
                        return CompletableFuture.completedFuture(new Result(Optional.empty(), "Unable to find the data type for the given node id '" + destinationNodeId + "'."));
                    }
                    final OpcUaDataType builtinType = tree.getBuiltinType(dataType.getNodeId());
                    if (builtinType != OpcUaDataType.ExtensionObject) {
                        if(dimensions != null && dimensions.length > 0) {
                            return CompletableFuture.completedFuture(new Result(Optional.of(builtinJsonSchema.getJsonSchema(builtinType, dimensions)), null));
                        } else {
                            return CompletableFuture.completedFuture(new Result(Optional.of(builtinJsonSchema.getJsonSchema(builtinType)), null));
                        }
                    } else {
                        final NodeId binaryEncodingId = dataType.getBinaryEncodingId();
                        if (binaryEncodingId == null) {
                            return CompletableFuture.completedFuture(new Result(Optional.empty(), "No encoding was present for the complex data type: '" + dataType + "'."));
                        }
                        return CompletableFuture.completedFuture(new Result(Optional.of(jsonSchemaFromNodeId(binaryEncodingId)), null));
                    }
                }).exceptionally(throwable -> {
                    log.error("Problem accessing node", throwable);
                    return new Result(Optional.empty(), "No node was found for the given node id '" + destinationNodeId + "'");
                });
    }

    private void addNestedStructureInformation(
            final @NotNull ObjectNode propertiesNode,
            final @NotNull FieldInformation fieldType) {
        final OpcUaDataType builtinDataType = fieldType.dataType;

        final ObjectNode nestedPropertiesNode = objectMapper.createObjectNode();
        propertiesNode.set(fieldType.name(), nestedPropertiesNode);

        if (builtinDataType != OpcUaDataType.ExtensionObject) {
            BuiltinJsonSchema.populatePropertiesForBuiltinType(nestedPropertiesNode, builtinDataType, objectMapper);
        } else if(fieldType.arrayDimensions() != null && fieldType.arrayDimensions().length > 0) {
            BuiltinJsonSchema.populatePropertiesForArray(nestedPropertiesNode, builtinDataType, objectMapper, fieldType.arrayDimensions());
        } else {
            nestedPropertiesNode.set("type", new TextNode("object"));
            final ObjectNode innerProperties = objectMapper.createObjectNode();
            nestedPropertiesNode.set("properties", innerProperties);

            client.getStaticDataTypeManager().getTypeDictionary(fieldType.namespaceUri());
            final String namespaceURI = fieldType.namespaceUri();

            final ExpandedNodeId expandedNodeId = ExpandedNodeId.of(namespaceURI, fieldType.name());

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

            final ArrayNode requiredAttributesArray = objectMapper.createArrayNode();
            for (final FieldInformation entry : fieldType.nestedFields()) {
                if(entry.required()) {
                    requiredAttributesArray.add(entry.name());
                }
                addNestedStructureInformation(propertiesNode, entry);
            }
            nestedPropertiesNode.set("required", requiredAttributesArray);
        }
    }

    private @NotNull JsonNode jsonSchemaFromNodeId(final @Nullable NodeId binaryEncodingId) {
        if (binaryEncodingId == null) {
            throw new RuntimeException("Binary encoding id was null for nested struct.");
        }

        final ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.set("$schema", new TextNode("https://json-schema.org/draft/2020-12/schema"));
        rootNode.set("title", new TextNode("CustomStruct: " + binaryEncodingId.toParseableString()));
        rootNode.set("type", new TextNode("object"));

        final ObjectNode valueNode = objectMapper.createObjectNode();
        rootNode.set("value", valueNode);
        valueNode.set("type", new TextNode("object"));

        final ObjectNode propertiesNode = objectMapper.createObjectNode();
        valueNode.set("properties", propertiesNode);


        final ArrayNode requiredAttributesArray = objectMapper.createArrayNode();
        generateFieldInformationForEncoding(client, binaryEncodingId).forEach(fieldInformation -> {
            if(fieldInformation.required()) {
                //TODO in the old version we added ALL fields to the list of required ones,
                // this is correct but may break compatibility
                requiredAttributesArray.add(fieldInformation.name());
            }
            addNestedStructureInformation(propertiesNode, fieldInformation);
        });

        valueNode.set("required", requiredAttributesArray);

        final ArrayNode requiredProperties = objectMapper.createArrayNode();
        requiredProperties.add("value");
        rootNode.set("required", requiredProperties);
        return rootNode;
    }

    public record FieldInformation(
            String name,
            String namespaceUri,
            OpcUaDataType dataType,
            UInteger[] arrayDimensions,
            boolean required,
            List<FieldInformation> nestedFields) {}

    public static List<FieldInformation> generateFieldInformationForEncoding(final @NotNull OpcUaClient client, final @NotNull NodeId binaryEncodingId) {
        try {
            final var definition = extractStructureDefinition(client, binaryEncodingId);
            final List<FieldInformation> ret = new ArrayList<>();
            for (final StructureField field : definition.getFields()) {

                final var dataType = OpcUaDataType.fromNodeId(field.getDataType());

                if(OpcUaDataType.ExtensionObject.equals(dataType)) {
                    final var dataTypeId = field.getTypeId().toNodeId(client.getNamespaceTable());
                    final DataType nextType = client.getDataTypeTree().getDataType(dataTypeId.get());
                    final var subFields = generateFieldInformationForEncoding(client, nextType.getBinaryEncodingId());
                    ret.add(new FieldInformation(
                            field.getName(),
                            dataType.getNodeId().expanded(client.getNamespaceTable()).getNamespaceUri(),
                            dataType,
                            field.getArrayDimensions(),
                            !field.getIsOptional(),
                            subFields));
                } else {
                    ret.add(new FieldInformation(
                            field.getName(),
                            dataType.getNodeId().expanded(client.getNamespaceTable()).getNamespaceUri(),
                            dataType,
                            field.getArrayDimensions(),
                            !field.getIsOptional(),
                            null));
                }
            }
            return ret;
        } catch (UaException e) {
            throw new RuntimeException(e);
        }
    }

    public static StructureDefinition extractStructureDefinition(final @NotNull OpcUaClient client, final @NotNull NodeId binaryEncodingId) {
        try {
            final DynamicStructCodec dataTypeCodec =
                    (DynamicStructCodec)client.getDynamicEncodingContext().getDataTypeManager().getCodec(binaryEncodingId);

            final Field f = DynamicStructCodec.class.getDeclaredField("definition"); //NoSuchFieldException
            f.setAccessible(true);
            return (StructureDefinition) f.get(dataTypeCodec);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
