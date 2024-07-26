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
                    createSimpleJsonSchema("Boolean", " Boolean JsonSchema", "Boolean"));
            final JsonNode byteJsonSchema = createSimpleJsonSchema("Byte", " Byte JsonSchema", "String");
            classToJsonSchema.put(BuiltinDataType.SByte, byteJsonSchema);
            classToJsonSchema.put(BuiltinDataType.Byte, byteJsonSchema);


            classToJsonSchema.put(BuiltinDataType.UInt64,
                    createSimpleJsonSchemaForIntegers("Integer", "UInt64 JsonSchema", BuiltinDataType.UInt64));
            classToJsonSchema.put(BuiltinDataType.UInt32,
                    createSimpleJsonSchemaForIntegers("Integer", "UInt32 JsonSchema", BuiltinDataType.UInt32));
            classToJsonSchema.put(BuiltinDataType.UInt16,
                    createSimpleJsonSchemaForIntegers("Integer", "UInt16 JsonSchema", BuiltinDataType.UInt16));


            classToJsonSchema.put(BuiltinDataType.Int64,
                    createSimpleJsonSchemaForIntegers("Integer", "Int64 JsonSchema", BuiltinDataType.Int64));
            classToJsonSchema.put(BuiltinDataType.Int32,
                    createSimpleJsonSchemaForIntegers("Integer", "Int32 JsonSchema", BuiltinDataType.Int32));
            classToJsonSchema.put(BuiltinDataType.Int16,
                    createSimpleJsonSchemaForIntegers("Integer", "Int16 JsonSchema", BuiltinDataType.Int16));

            classToJsonSchema.put(BuiltinDataType.Float,
                    createSimpleJsonSchemaForIntegers("Float", " Float JsonSchema", BuiltinDataType.Float));
            classToJsonSchema.put(BuiltinDataType.Double,
                    createSimpleJsonSchemaForIntegers("Double", " Double JsonSchema", BuiltinDataType.Double));
            classToJsonSchema.put(BuiltinDataType.String,
                    createSimpleJsonSchema("String", " String JsonSchema", "string"));

            classToJsonSchema.put(BuiltinDataType.DateTime,
                    createSimpleJsonSchemaForIntegers("DateTime", "DateTime JsonSchema", BuiltinDataType.DateTime));

            classToJsonSchema.put(BuiltinDataType.Guid, createSimpleJsonSchema("Guid", " Guid JsonSchema", "string"));
            classToJsonSchema.put(BuiltinDataType.ByteString,
                    createSimpleJsonSchema("ByteString", " ByteString JsonSchema", "string"));
            classToJsonSchema.put(BuiltinDataType.XmlElement,
                    createSimpleJsonSchema("XmlElement", " XmlElement JsonSchema", "string"));

            classToJsonSchema.put(BuiltinDataType.QualifiedName, createSimpleJsonSchemaForQualifiedName());

            classToJsonSchema.put(BuiltinDataType.NodeId,
                    createSimpleJsonSchema("NodeId", " XmlElement NodeId", "string"));
            classToJsonSchema.put(BuiltinDataType.ExpandedNodeId,
                    createSimpleJsonSchema("ExpandedNodeId", " ExpandedNodeId JsonSchema", "string"));

            classToJsonSchema.put(BuiltinDataType.StatusCode,
                    createSimpleJsonSchemaForIntegers("StatusCode",
                            " StatusCode JsonSchema",
                            BuiltinDataType.StatusCode));
            classToJsonSchema.put(BuiltinDataType.LocalizedText,
                    createSimpleJsonSchema("LocalizedText", " LocalizedText JsonSchema", "string"));
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


    private @NotNull JsonNode createSimpleJsonSchemaForIntegers(
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


        return rootNode;
    }


    private @NotNull JsonNode createSimpleJsonSchema(
            final @NotNull String id, final @NotNull String title, final @NotNull String dataType) {
        final ObjectNode rootNode = OBJECT_MAPPER.createObjectNode();
        rootNode.set("§id", new TextNode(id));
        rootNode.set("$schema", new TextNode("https://json-schema.org/draft/2020-12/schema"));
        rootNode.set("title", new TextNode(title));
        rootNode.set("type", new TextNode("object"));

        final ObjectNode valueNode = OBJECT_MAPPER.createObjectNode();
        valueNode.set("type", new TextNode(dataType));

        final ObjectNode propertiesNode = OBJECT_MAPPER.createObjectNode();
        propertiesNode.set("value", valueNode);
        rootNode.set("properties", propertiesNode);

        final ArrayNode requiredProperties = OBJECT_MAPPER.createArrayNode();
        requiredProperties.add("value");
        rootNode.set("required", requiredProperties);

        return rootNode;
    }


    private @NotNull JsonNode createSimpleJsonSchemaForQualifiedName() {
        final ObjectNode rootNode = OBJECT_MAPPER.createObjectNode();
        rootNode.set("§id", new TextNode("QualifiedName"));
        rootNode.set("$schema", new TextNode("https://json-schema.org/draft/2020-12/schema"));
        rootNode.set("title", new TextNode("QualifiedName JsonSchema"));
        rootNode.set("type", new TextNode("object"));

        final ObjectNode valueNode = OBJECT_MAPPER.createObjectNode();
        final ObjectNode innerProperties = OBJECT_MAPPER.createObjectNode();

        final ObjectNode namespaceIndexNode = OBJECT_MAPPER.createObjectNode();
        namespaceIndexNode.set("type", new TextNode("Integer"));

        final ObjectNode nameNode = OBJECT_MAPPER.createObjectNode();
        nameNode.set("type", new TextNode("String"));

        valueNode.set("type", new TextNode("object"));
        valueNode.set("properties", innerProperties);

        final ObjectNode propertiesNode = OBJECT_MAPPER.createObjectNode();
        propertiesNode.set("value", valueNode);
        rootNode.set("properties", propertiesNode);

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
                nestedPropertiesNode.set("properties", innerProperties);

                final ObjectNode namespaceIndexNode = objectMapper.createObjectNode();
                namespaceIndexNode.set("type", new TextNode("Integer"));
                innerProperties.set("namespaceIndex", namespaceIndexNode);

                final ObjectNode nameNode = objectMapper.createObjectNode();
                nameNode.set("type", new TextNode("String"));
                innerProperties.set("name", nameNode);
                return;
            case ExtensionObject:
            case DataValue:
            case Variant:
            case DiagnosticInfo:
                throw new JsonSchemaGenerationException("NOT AVAILABLE");
        }
    }

}
