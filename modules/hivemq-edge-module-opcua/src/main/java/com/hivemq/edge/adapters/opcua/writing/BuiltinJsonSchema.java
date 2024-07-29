package com.hivemq.edge.adapters.opcua.writing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.eclipse.milo.opcua.stack.core.BuiltinDataType;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.ULong;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class BuiltinJsonSchema {

    private static final @NotNull ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final @NotNull HashMap<BuiltinDataType, JsonNode> classToJsonSchema = new HashMap<>();


    private BuiltinJsonSchema() {
        try {
            classToJsonSchema.put(BuiltinDataType.Boolean,
                    createJsonSchemaForBuiltinType("Boolean", " Boolean JsonSchema", BuiltinDataType.Boolean));
            final JsonNode byteJsonSchema =
                    createJsonSchemaForBuiltinType("Byte", " Byte JsonSchema", BuiltinDataType.String);
            classToJsonSchema.put(BuiltinDataType.SByte, byteJsonSchema);
            classToJsonSchema.put(BuiltinDataType.Byte, byteJsonSchema);


            classToJsonSchema.put(BuiltinDataType.UInt64,
                    createJsonSchemaForBuiltinType("Integer", "UInt64 JsonSchema", BuiltinDataType.UInt64));
            classToJsonSchema.put(BuiltinDataType.UInt32,
                    createJsonSchemaForBuiltinType("Integer", "UInt32 JsonSchema", BuiltinDataType.UInt32));
            classToJsonSchema.put(BuiltinDataType.UInt16,
                    createJsonSchemaForBuiltinType("Integer", "UInt16 JsonSchema", BuiltinDataType.UInt16));


            classToJsonSchema.put(BuiltinDataType.Int64,
                    createJsonSchemaForBuiltinType("Integer", "Int64 JsonSchema", BuiltinDataType.Int64));
            classToJsonSchema.put(BuiltinDataType.Int32,
                    createJsonSchemaForBuiltinType("Integer", "Int32 JsonSchema", BuiltinDataType.Int32));
            classToJsonSchema.put(BuiltinDataType.Int16,
                    createJsonSchemaForBuiltinType("Integer", "Int16 JsonSchema", BuiltinDataType.Int16));

            classToJsonSchema.put(BuiltinDataType.Float,
                    createJsonSchemaForBuiltinType("Float", " Float JsonSchema", BuiltinDataType.Float));
            classToJsonSchema.put(BuiltinDataType.Double,
                    createJsonSchemaForBuiltinType("Double", " Double JsonSchema", BuiltinDataType.Double));
            classToJsonSchema.put(BuiltinDataType.String,
                    createJsonSchemaForBuiltinType("String", " String JsonSchema", BuiltinDataType.String));

            classToJsonSchema.put(BuiltinDataType.DateTime,
                    createJsonSchemaForBuiltinType("DateTime", "DateTime JsonSchema", BuiltinDataType.DateTime));

            classToJsonSchema.put(BuiltinDataType.Guid,
                    createJsonSchemaForBuiltinType("Guid", " Guid JsonSchema", BuiltinDataType.String));
            classToJsonSchema.put(BuiltinDataType.ByteString,
                    createJsonSchemaForBuiltinType("ByteString", " ByteString JsonSchema", BuiltinDataType.String));
            classToJsonSchema.put(BuiltinDataType.XmlElement,
                    createJsonSchemaForBuiltinType("XmlElement", " XmlElement JsonSchema", BuiltinDataType.String));

            classToJsonSchema.put(BuiltinDataType.QualifiedName,
                    createJsonSchemaForBuiltinType("QualifiedName",
                            "QualifiedName JsonSchema",
                            BuiltinDataType.QualifiedName));

            classToJsonSchema.put(BuiltinDataType.NodeId,
                    createJsonSchemaForBuiltinType("NodeId", " XmlElement NodeId", BuiltinDataType.String));
            classToJsonSchema.put(BuiltinDataType.ExpandedNodeId,
                    createJsonSchemaForBuiltinType("ExpandedNodeId",
                            " ExpandedNodeId JsonSchema",
                            BuiltinDataType.String));

            classToJsonSchema.put(BuiltinDataType.StatusCode, createJsonSchemaForBuiltinType("StatusCode",
                            " StatusCode JsonSchema",
                            BuiltinDataType.StatusCode));
            classToJsonSchema.put(BuiltinDataType.LocalizedText,
                    createJsonSchemaForBuiltinType("LocalizedText",
                            " LocalizedText JsonSchema",
                            BuiltinDataType.String));
        } catch (JsonSchemaGenerationException jsonSchemaGenerationException) {
            throw new RuntimeException(jsonSchemaGenerationException);
        }
    }

    public static BuiltinJsonSchema getInstance() throws UaException {
        return new BuiltinJsonSchema();
    }

    public @NotNull JsonNode getJsonSchema(final @NotNull BuiltinDataType builtinDataType) {
        return classToJsonSchema.get(builtinDataType);
    }


    private @NotNull JsonNode createJsonSchemaForBuiltinType(
            final @NotNull String id, final @NotNull String title, final @NotNull BuiltinDataType builtinDataType)
            throws JsonSchemaGenerationException {
        final ObjectNode rootNode = OBJECT_MAPPER.createObjectNode();
        final ObjectNode propertiesNode = OBJECT_MAPPER.createObjectNode();
        final ObjectNode valueNode = OBJECT_MAPPER.createObjectNode();
        rootNode.set("§id", new TextNode(id));
        rootNode.set("$schema", new TextNode("https://json-schema.org/draft/2020-12/schema"));
        rootNode.set("title", new TextNode(title));
        rootNode.set("type", new TextNode("object"));
        rootNode.set("properties", propertiesNode);
        propertiesNode.set("value", valueNode);
        populatePropertiesForBuiltinType(valueNode, builtinDataType, OBJECT_MAPPER);

        final ArrayNode requiredAttributes = OBJECT_MAPPER.createArrayNode();
        requiredAttributes.add("value");
        rootNode.set("required", requiredAttributes);

        return rootNode;
    }

    public static void populatePropertiesForBuiltinType(
            final @NotNull ObjectNode nestedPropertiesNode,
            final @NotNull BuiltinDataType builtinDataType,
            final @NotNull ObjectMapper objectMapper) throws JsonSchemaGenerationException {
        switch (builtinDataType) {
            case Boolean:
                nestedPropertiesNode.set("type", new TextNode("boolean"));
                return;
            case SByte:
            case Byte:
                nestedPropertiesNode.set("type", new TextNode("string"));
                return;
            case Int16:
                nestedPropertiesNode.set("type", new TextNode("integer"));
                return;
            case UInt16:
                nestedPropertiesNode.set("type", new TextNode("integer"));
                nestedPropertiesNode.set("min", new IntNode(UShort.MIN.intValue()));
                nestedPropertiesNode.set("max", new LongNode(UShort.MAX.intValue()));
                return;
            case StatusCode:
            case Int32:
                nestedPropertiesNode.set("type", new TextNode("integer"));
                nestedPropertiesNode.set("min", new LongNode(Integer.MIN_VALUE));
                nestedPropertiesNode.set("max", new LongNode(Integer.MAX_VALUE));
                return;
            case UInt32:
                nestedPropertiesNode.set("type", new TextNode("integer"));
                nestedPropertiesNode.set("min", new LongNode(UInteger.MIN_VALUE));
                nestedPropertiesNode.set("max", new LongNode(UInteger.MAX_VALUE));
                return;
            case DateTime:
            case Int64:
                nestedPropertiesNode.set("type", new TextNode("integer"));
                nestedPropertiesNode.set("min", new LongNode(Long.MIN_VALUE));
                nestedPropertiesNode.set("max", new LongNode(Long.MAX_VALUE));
                return;
            case UInt64:
                nestedPropertiesNode.set("type", new TextNode("integer"));
                nestedPropertiesNode.set("min", new BigIntegerNode(ULong.MIN_VALUE));
                nestedPropertiesNode.set("max", new BigIntegerNode(ULong.MAX_VALUE));
                return;
            case Float:
                nestedPropertiesNode.set("type", new TextNode("number"));
                nestedPropertiesNode.set("min", new FloatNode(Float.MIN_VALUE));
                nestedPropertiesNode.set("max", new FloatNode(Float.MAX_VALUE));
                return;
            case Double:
                nestedPropertiesNode.set("type", new TextNode("number"));
                nestedPropertiesNode.set("min", new DoubleNode(Double.MIN_VALUE));
                nestedPropertiesNode.set("max", new DoubleNode(Double.MAX_VALUE));
                return;
            case String:
            case Guid:
            case ByteString:
            case XmlElement:
            case NodeId:
            case ExpandedNodeId:
            case LocalizedText:
                nestedPropertiesNode.set("type", new TextNode("string"));
                return;
            case QualifiedName:
                nestedPropertiesNode.set("type", new TextNode("object"));

                final ObjectNode innerProperties = objectMapper.createObjectNode();

                final ObjectNode namespaceIndexNode = objectMapper.createObjectNode();
                namespaceIndexNode.set("type", new TextNode("integer"));
                innerProperties.set("namespaceIndex", namespaceIndexNode);

                final ObjectNode nameNode = objectMapper.createObjectNode();
                nameNode.set("type", new TextNode("string"));
                innerProperties.set("name", nameNode);

                nestedPropertiesNode.set("properties", innerProperties);
                final ArrayNode requiredAttributes = OBJECT_MAPPER.createArrayNode();
                requiredAttributes.add("name");
                requiredAttributes.add("namespaceIndex");
                nestedPropertiesNode.set("required", requiredAttributes);
                return;
            case ExtensionObject:
            case DataValue:
            case Variant:
            case DiagnosticInfo:
                throw new JsonSchemaGenerationException("NOT AVAILABLE");
        }
    }

}
