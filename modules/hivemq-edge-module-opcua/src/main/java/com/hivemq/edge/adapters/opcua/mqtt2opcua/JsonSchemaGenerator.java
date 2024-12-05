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
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationOutput;
import org.eclipse.milo.opcua.binaryschema.AbstractCodec;
import org.eclipse.milo.opcua.sdk.client.DataTypeTreeBuilder;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.core.DataTypeTree;
import org.eclipse.milo.opcua.stack.core.BuiltinDataType;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.serialization.codecs.DataTypeCodec;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.opcfoundation.opcua.binaryschema.FieldType;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.hivemq.edge.adapters.opcua.mqtt2opcua.BuiltInDataTypeConverter.convertFieldTypeToBuiltInDataType;

public class JsonSchemaGenerator {

    private final @NotNull OpcUaClient client;
    private final @NotNull DataTypeTree tree;
    private final @NotNull BuiltinJsonSchema builtinJsonSchema;
    private final @NotNull ObjectMapper objectMapper;

    public JsonSchemaGenerator(final @NotNull OpcUaClient client, final @NotNull ObjectMapper objectMapper)
            throws UaException {
        this.client = client;
        this.tree = DataTypeTreeBuilder.build(client);
        this.objectMapper = objectMapper;
        this.builtinJsonSchema = new BuiltinJsonSchema();
    }

    public void createJsonSchema(
            final @NotNull NodeId destinationNodeId, final @NotNull TagSchemaCreationOutput output) {
        final CompletableFuture<UaVariableNode> variableNodeFuture =
                client.getAddressSpace().getVariableNodeAsync(destinationNodeId);
        variableNodeFuture.whenComplete((uaVariableNode, throwable) -> {
            if (throwable != null) {
                // no node was found for the given nodeId
                output.tagNotFound("No node was found for the given node id '" + destinationNodeId + "'");
                return;
            }
            final NodeId dataTypeNodeId = uaVariableNode.getDataType();
            final DataTypeTree.DataType dataType = tree.getDataType(dataTypeNodeId);
            if (dataType == null) {
                output.fail("Unable to find the data type for the given node id '" + destinationNodeId + "'.");
                return;
            }
            final BuiltinDataType builtinType = tree.getBuiltinType(dataType.getNodeId());
            if (builtinType != BuiltinDataType.ExtensionObject) {
                output.finish(builtinJsonSchema.getJsonSchema(builtinType));
            } else {
                final NodeId binaryEncodingId = dataType.getBinaryEncodingId();
                if (binaryEncodingId == null) {
                    output.fail("No encoding was present for the complex data type: '" + dataType + "'.");
                }
                output.finish(jsonSchemaFromNodeId(binaryEncodingId));
            }
        });
    }

    public void addNestedStructureInformation(
            final @NotNull ObjectNode propertiesNode, final @NotNull FieldType fieldType) {
        final BuiltinDataType builtinDataType = convertFieldTypeToBuiltInDataType(fieldType, client);

        final ObjectNode nestedPropertiesNode = objectMapper.createObjectNode();
        propertiesNode.set(fieldType.getName(), nestedPropertiesNode);

        if (builtinDataType != BuiltinDataType.ExtensionObject) {
            BuiltinJsonSchema.populatePropertiesForBuiltinType(nestedPropertiesNode, builtinDataType, objectMapper);
        } else {
            nestedPropertiesNode.set("type", new TextNode("object"));
            final ObjectNode innerProperties = objectMapper.createObjectNode();
            nestedPropertiesNode.set("properties", innerProperties);

            client.getStaticDataTypeManager().getDataTypeDictionary(fieldType.getTypeName().getNamespaceURI());
            final String namespaceURI = fieldType.getTypeName().getNamespaceURI();
            final ExpandedNodeId expandedNodeId = new ExpandedNodeId.Builder().setNamespaceUri(namespaceURI)
                    .setIdentifier(fieldType.getTypeName().getLocalPart())
                    .build();
            final Optional<NodeId> optionalDataTypeId = expandedNodeId.toNodeId(client.getNamespaceTable());
            if (optionalDataTypeId.isEmpty()) {

                throw new RuntimeException("Expanded node id '" + expandedNodeId + "' could not be parsed to node id.");
            }

            final NodeId dataTypeId = optionalDataTypeId.get();
            final DataTypeTree.DataType dataType = tree.getDataType(dataTypeId);
            if (dataType == null) {
                throw new RuntimeException("No data type was found in the DataTypeTree for node id '" +
                        dataTypeId +
                        "'");
            }

            final NodeId binaryEncodingId = dataType.getBinaryEncodingId();
            if (binaryEncodingId == null) {
                throw new RuntimeException("Binary encoding id was null for nested struct.");
            }

            final Map<String, FieldType> embeddedFields = getStructureInformation(binaryEncodingId);
            final ArrayNode requiredAttributesArray = objectMapper.createArrayNode();
            for (final Map.Entry<String, FieldType> entry : embeddedFields.entrySet()) {
                requiredAttributesArray.add(entry.getValue().getName());
                addNestedStructureInformation(innerProperties, entry.getValue());
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

        final Map<String, FieldType> fields = getStructureInformation(binaryEncodingId);
        final ArrayNode requiredAttributesArray = objectMapper.createArrayNode();

        for (final Map.Entry<String, FieldType> entry : fields.entrySet()) {
            requiredAttributesArray.add(entry.getValue().getName());
            final FieldType fieldType = entry.getValue();
            addNestedStructureInformation(propertiesNode, fieldType);
        }
        valueNode.set("required", requiredAttributesArray);

        final ArrayNode requiredProperties = objectMapper.createArrayNode();
        requiredProperties.add("value");
        rootNode.set("required", requiredProperties);
        return rootNode;
    }

    private @NotNull Map<String, FieldType> getStructureInformation(final @NotNull NodeId binaryEncodingId) {
        try {
            final DataTypeCodec dataTypeCodec =
                    client.getDynamicSerializationContext().getDataTypeManager().getCodec(binaryEncodingId);
            final Field f = AbstractCodec.class.getDeclaredField("fields"); //NoSuchFieldException
            f.setAccessible(true);
            return (Map<String, FieldType>) f.get(dataTypeCodec);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            if (e.getMessage() != null) {
                throw new RuntimeException("Unable to find information on fields in the codec: " + e.getMessage());
            } else {
                throw new RuntimeException("Unable to find information on fields in the codec.");
            }
        }
    }
}
