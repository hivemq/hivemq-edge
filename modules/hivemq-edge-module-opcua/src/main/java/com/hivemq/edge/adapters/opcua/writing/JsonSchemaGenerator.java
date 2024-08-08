package com.hivemq.edge.adapters.opcua.writing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.eclipse.milo.opcua.binaryschema.AbstractCodec;
import org.eclipse.milo.opcua.binaryschema.GenericEnumCodec;
import org.eclipse.milo.opcua.binaryschema.GenericStructCodec;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

public class JsonSchemaGenerator {

    private static final @NotNull Logger log = LoggerFactory.getLogger("com.hivemq.edge.write.JsonSchemaGenerator");


    private final @NotNull OpcUaClient client;
    private final @NotNull DataTypeTree tree;
    private final @NotNull BuiltinJsonSchema builtinJsonSchema;
    private final @NotNull ObjectMapper objectMapper;

    public JsonSchemaGenerator(final @NotNull OpcUaClient client, final @NotNull ObjectMapper objectMapper)
            throws UaException {
        this.client = client;
        this.tree = DataTypeTreeBuilder.build(client);
        this.objectMapper = objectMapper;
        this.builtinJsonSchema = BuiltinJsonSchema.getInstance();
    }

    public static JsonSchemaGenerator getInstance(
            final @NotNull OpcUaClient client, final @NotNull ObjectMapper objectMapper) throws UaException {
        return new JsonSchemaGenerator(client, objectMapper);
    }

    public @Nullable JsonNode getJsonSchema(final @NotNull NodeId destinationNodeId)
            throws JsonSchemaGenerationException {
        try {
            final NodeId dataTypeNodeId = getDataTypeNodeId(destinationNodeId);
            if (dataTypeNodeId == null) {
                throw new JsonSchemaGenerationException("No dataType-nodeId was found for the destination nodeId " +
                        destinationNodeId +
                        "'");
            }
            final DataTypeTree.DataType dataType = tree.getDataType(dataTypeNodeId);
            if (dataType == null) {
                throw new JsonSchemaGenerationException("No data type was found in the DataTypeTree for node id '" +
                        dataTypeNodeId +
                        "'");
            }
            final BuiltinDataType builtinType = tree.getBuiltinType(dataType.getNodeId());
            if (builtinType != BuiltinDataType.ExtensionObject) {
                return builtinJsonSchema.getJsonSchema(builtinType);
            } else {
                final NodeId binaryEncodingId = dataType.getBinaryEncodingId();
                if (binaryEncodingId == null) {
                    throw new JsonSchemaGenerationException("No encoding was present for data type: '" +
                            dataType.toString() +
                            "'");
                }
                return createJsonSchema(binaryEncodingId);
            }


        } catch (UaException e) {
            throw new JsonSchemaGenerationException("Unknown type");
        }


    }


    private @Nullable NodeId getDataTypeNodeId(final @NotNull NodeId variableNodeId) throws UaException {
        final UaVariableNode variableNode = client.getAddressSpace().getVariableNode(variableNodeId);
        return variableNode.getDataType();
    }


    @Nullable
    JsonNode createJsonSchema(final @Nullable NodeId binaryEncodingId) throws JsonSchemaGenerationException {
        if (binaryEncodingId == null) {
            throw new JsonSchemaGenerationException("Binary encoding id was null for nested struct. ");
        }

        final ObjectNode rootNode = objectMapper.createObjectNode();

        rootNode.set("§id", new TextNode("CustomStruct: " + binaryEncodingId.toParseableString()));
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

    public void addNestedStructureInformation(
            @NotNull final ObjectNode propertiesNode, @NotNull final FieldType fieldType)
            throws JsonSchemaGenerationException {
        final BuiltinDataType builtinDataType = mapToBuildInDataType(fieldType);

        ObjectNode nestedPropertiesNode = objectMapper.createObjectNode();
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

                throw new JsonSchemaGenerationException("Expanded node id '" +
                        expandedNodeId +
                        "' could not be parsed to node id.");
            }

            final NodeId dataTypeId = optionalDataTypeId.get();
            final DataTypeTree.DataType dataType = tree.getDataType(dataTypeId);
            if (dataType == null) {
                throw new JsonSchemaGenerationException("No data type was found in the DataTypeTree for node id '" +
                        dataTypeId +
                        "'");
            }

            final NodeId binaryEncodingId = dataType.getBinaryEncodingId();
            if (binaryEncodingId == null) {
                throw new JsonSchemaGenerationException("Binary encoding id was null for nested struct.");
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


    private @NotNull BuiltinDataType mapToBuildInDataType(final @NotNull FieldType fieldType)
            throws JsonSchemaGenerationException {
        final String namespaceURI = fieldType.getTypeName().getNamespaceURI();
        boolean isStandard = namespaceURI.startsWith("http://opcfoundation.org/");
        if (isStandard) {
            String localPart = fieldType.getTypeName().getLocalPart();
            try {
                return BuiltinDataType.valueOf(localPart);
            } catch (IllegalArgumentException e) {
                throw new JsonSchemaGenerationException(e.getMessage());
            }
        } else {
            final DataTypeCodec dataTypeCodec =
                    client.getDynamicDataTypeManager().getCodec(namespaceURI, fieldType.getTypeName().getLocalPart());
            if (dataTypeCodec instanceof GenericEnumCodec) {
                // enum are encoded as integers
                return BuiltinDataType.Int32;
            } else if (dataTypeCodec instanceof GenericStructCodec) {
                return BuiltinDataType.ExtensionObject;
            }
        }
        //TODO is this actually reachable?
        throw new JsonSchemaGenerationException("TODO");
    }

    private @NotNull Map<String, FieldType> getStructureInformation(final @NotNull NodeId binaryEncodingId)
            throws JsonSchemaGenerationException {
        try {
            final DataTypeCodec dataTypeCodec =
                    client.getDynamicSerializationContext().getDataTypeManager().getCodec(binaryEncodingId);
            final Field f = AbstractCodec.class.getDeclaredField("fields"); //NoSuchFieldException
            f.setAccessible(true);
            return (Map<String, FieldType>) f.get(dataTypeCodec);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            if (e.getMessage() != null) {
                throw new JsonSchemaGenerationException("Unable to find information on fields in the codec: " +
                        e.getMessage());
            } else {
                throw new JsonSchemaGenerationException("Unable to find information on fields in the codec.");
            }
        }
    }
}
