package com.hivemq.edge.adapters.modbus.writing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ShortNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.jetbrains.annotations.NotNull;

public class ModbusJsonSchemaGenerator {

    //TODO should be handed over by Edge
    private static final @NotNull ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String MINIMUM_KEY_WORD = "minimum";
    private static final String MAXIMUM_KEY_WORD = "maximum";
    public static final String INTEGER_DATA_TYPE = "integer";

    public static JsonNode createJsonSchema(ConvertibleDataType convertibleDataType) {

        final ObjectNode rootNode = OBJECT_MAPPER.createObjectNode();
        final ObjectNode propertiesNode = OBJECT_MAPPER.createObjectNode();
        final ObjectNode valueNode = OBJECT_MAPPER.createObjectNode();
        rootNode.set("$schema", new TextNode("https://json-schema.org/draft/2019-09/schema"));
        rootNode.set("title", new TextNode(convertibleDataType.name() + "-schema"));
        rootNode.set("type", new TextNode("object"));
        rootNode.set("properties", propertiesNode);
        propertiesNode.set("value", valueNode);


        switch (convertibleDataType) {
            case BYTE:
                valueNode.set("type", new TextNode("string"));
                break;
            case INTEGER:
                valueNode.set("type", new TextNode(INTEGER_DATA_TYPE));
                valueNode.set(MINIMUM_KEY_WORD, new LongNode(Integer.MIN_VALUE));
                valueNode.set(MAXIMUM_KEY_WORD, new LongNode(Integer.MAX_VALUE));
                break;
            case SHORT:
                valueNode.set("type", new TextNode(INTEGER_DATA_TYPE));
                valueNode.set(MINIMUM_KEY_WORD, new ShortNode(Short.MIN_VALUE));
                valueNode.set(MAXIMUM_KEY_WORD, new ShortNode(Short.MAX_VALUE));
                break;
            case LONG:
                valueNode.set("type", new TextNode(INTEGER_DATA_TYPE));
                valueNode.set(MINIMUM_KEY_WORD, new LongNode(Short.MIN_VALUE));
                valueNode.set(MAXIMUM_KEY_WORD, new LongNode(Short.MAX_VALUE));
                break;
            case UTF8_STRING:
                valueNode.set("type", new TextNode("string"));
                break;
            case ASCII_STRING:
                valueNode.set("type", new TextNode("string"));
                break;
        }


        final ArrayNode requiredAttributes = OBJECT_MAPPER.createArrayNode();
        requiredAttributes.add("value");
        rootNode.set("required", requiredAttributes);

        return rootNode;
    }
}
