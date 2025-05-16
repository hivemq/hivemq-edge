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
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ShortNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.eclipse.milo.opcua.stack.core.OpcUaDataType;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.ULong;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;

public class BuiltinJsonSchema {

    private static final @NotNull ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final @NotNull Logger log = LoggerFactory.getLogger("com.hivemq.edge.write.BuiltinJsonSchema");
    private static final @NotNull String MINIMUM_KEY_WORD = "minimum";
    private static final @NotNull String MAXIMUM_KEY_WORD = "maximum";
    public static final @NotNull String INTEGER_DATA_TYPE = "integer";
    public static final @NotNull String ARRAY_DATA_TYPE = "array";
    public static final @NotNull String ARRAY_ITEMS = "items";
    public static final @NotNull String ARRAY_MAX_TIMES = "maxItems";

    private final @NotNull HashMap<OpcUaDataType, JsonNode> classToJsonSchema = new HashMap<>();

    public BuiltinJsonSchema() {
        try {
            classToJsonSchema.put(OpcUaDataType.Boolean,
                    createJsonSchemaForBuiltinType("Boolean JsonSchema", OpcUaDataType.Boolean));

            classToJsonSchema.put(OpcUaDataType.SByte,
                    createJsonSchemaForBuiltinType("SByte JsonSchema", OpcUaDataType.SByte));
            classToJsonSchema.put(OpcUaDataType.Byte,
                    createJsonSchemaForBuiltinType("Byte JsonSchema", OpcUaDataType.Byte));

            classToJsonSchema.put(OpcUaDataType.UInt64,
                    createJsonSchemaForBuiltinType("UInt64 JsonSchema", OpcUaDataType.UInt64));
            classToJsonSchema.put(OpcUaDataType.UInt32,
                    createJsonSchemaForBuiltinType("UInt32 JsonSchema", OpcUaDataType.UInt32));
            classToJsonSchema.put(OpcUaDataType.UInt16,
                    createJsonSchemaForBuiltinType("UInt16 JsonSchema", OpcUaDataType.UInt16));

            classToJsonSchema.put(OpcUaDataType.Int64,
                    createJsonSchemaForBuiltinType("Int64 JsonSchema", OpcUaDataType.Int64));
            classToJsonSchema.put(OpcUaDataType.Int32,
                    createJsonSchemaForBuiltinType("Int32 JsonSchema", OpcUaDataType.Int32));
            classToJsonSchema.put(OpcUaDataType.Int16,
                    createJsonSchemaForBuiltinType("Int16 JsonSchema", OpcUaDataType.Int16));

            classToJsonSchema.put(OpcUaDataType.Float,
                    createJsonSchemaForBuiltinType("Float JsonSchema", OpcUaDataType.Float));
            classToJsonSchema.put(OpcUaDataType.Double,
                    createJsonSchemaForBuiltinType("Double JsonSchema", OpcUaDataType.Double));
            classToJsonSchema.put(OpcUaDataType.String,
                    createJsonSchemaForBuiltinType("String JsonSchema", OpcUaDataType.String));

            classToJsonSchema.put(OpcUaDataType.DateTime,
                    createJsonSchemaForBuiltinType("DateTime JsonSchema", OpcUaDataType.DateTime));

            classToJsonSchema.put(OpcUaDataType.Guid,
                    createJsonSchemaForBuiltinType("Guid JsonSchema", OpcUaDataType.String));
            classToJsonSchema.put(OpcUaDataType.ByteString,
                    createJsonSchemaForBuiltinType("ByteString JsonSchema", OpcUaDataType.String));
            classToJsonSchema.put(OpcUaDataType.XmlElement,
                    createJsonSchemaForBuiltinType("XmlElement JsonSchema", OpcUaDataType.String));

            classToJsonSchema.put(OpcUaDataType.QualifiedName,
                    createJsonSchemaForBuiltinType("QualifiedName JsonSchema", OpcUaDataType.QualifiedName));

            classToJsonSchema.put(OpcUaDataType.NodeId,
                    createJsonSchemaForBuiltinType("XmlElement NodeId", OpcUaDataType.String));
            classToJsonSchema.put(OpcUaDataType.ExpandedNodeId,
                    createJsonSchemaForBuiltinType("ExpandedNodeId JsonSchema", OpcUaDataType.String));

            classToJsonSchema.put(OpcUaDataType.StatusCode,
                    createJsonSchemaForBuiltinType("StatusCode JsonSchema", OpcUaDataType.StatusCode));
            classToJsonSchema.put(OpcUaDataType.LocalizedText,
                    createJsonSchemaForBuiltinType("LocalizedText JsonSchema", OpcUaDataType.String));
        } catch (final Exception jsonSchemaGenerationException) {
            log.error("Exception while initializing the JsonSchema for the builtin types:",
                    jsonSchemaGenerationException);
            throw new RuntimeException(jsonSchemaGenerationException);
        }
    }

    public @NotNull JsonNode getJsonSchema(final @NotNull OpcUaDataType builtinDataType) {
        return classToJsonSchema.get(builtinDataType);
    }

    private @NotNull JsonNode createJsonSchemaForBuiltinType(
            final @NotNull String title, final @NotNull OpcUaDataType builtinDataType) {
        final ObjectNode rootNode = OBJECT_MAPPER.createObjectNode();
        final ObjectNode propertiesNode = OBJECT_MAPPER.createObjectNode();
        final ObjectNode valueNode = OBJECT_MAPPER.createObjectNode();
        rootNode.set("$schema", new TextNode("https://json-schema.org/draft/2019-09/schema"));
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

    public @NotNull JsonNode getJsonSchema(final @NotNull OpcUaDataType builtinDataType,
                                           final @NotNull UInteger[] dimensions) {

        final ObjectNode rootNode = OBJECT_MAPPER.createObjectNode();
        final ObjectNode propertiesNode = OBJECT_MAPPER.createObjectNode();
        final ObjectNode valueNode = OBJECT_MAPPER.createObjectNode();
        rootNode.set("$schema", new TextNode("https://json-schema.org/draft/2019-09/schema"));
        rootNode.set("title", new TextNode("Array of " + builtinDataType.name() + " JsonSchema"));
        rootNode.set("type", new TextNode("object"));
        rootNode.set("properties", propertiesNode);
        propertiesNode.set("value", valueNode);
        populatePropertiesForArray(valueNode, builtinDataType, OBJECT_MAPPER, dimensions);

        final ArrayNode requiredAttributes = OBJECT_MAPPER.createArrayNode();
        requiredAttributes.add("value");
        rootNode.set("required", requiredAttributes);
        return rootNode;
    }

    public static void populatePropertiesForArray(final @NotNull ObjectNode propertiesNode,
                                                  final @NotNull OpcUaDataType builtinDataType,
                                                  final @NotNull ObjectMapper objectMapper,
                                                  final @NotNull UInteger[] dimensions) {
            if(dimensions.length == 0) {
                throw new IllegalArgumentException("Array of " + builtinDataType.name() + " dimensions must not be empty");
            }
            final long maxSize = dimensions[0].longValue();

            propertiesNode.set("type", new TextNode(ARRAY_DATA_TYPE));

            //0 for a dimension means unlimited
            if(maxSize > 0) {
                propertiesNode.set("maxItems", new LongNode(maxSize));
                propertiesNode.set("minItems", new LongNode(maxSize));
            }
            final ObjectNode itemsNode = objectMapper.createObjectNode();
            propertiesNode.set("items", itemsNode);

            if (dimensions.length == 1) {
                //last element, we can now set the array type
                populatePropertiesForBuiltinType(itemsNode, builtinDataType, objectMapper);
            } else {
                //nesting deeper
                populatePropertiesForArray(itemsNode, builtinDataType, objectMapper, Arrays.copyOfRange(dimensions, 1, dimensions.length));
            }
    }

    public static void populatePropertiesForBuiltinType(
            final @NotNull ObjectNode nestedPropertiesNode,
            final @NotNull OpcUaDataType builtinDataType,
            final @NotNull ObjectMapper objectMapper) {
        switch (builtinDataType) {
            case Boolean:
                nestedPropertiesNode.set("type", new TextNode("boolean"));
                return;
            case SByte:
                nestedPropertiesNode.set("type", new TextNode(INTEGER_DATA_TYPE));
                nestedPropertiesNode.set(MINIMUM_KEY_WORD, new ShortNode(Byte.MIN_VALUE));
                nestedPropertiesNode.set(MAXIMUM_KEY_WORD, new ShortNode(Byte.MAX_VALUE));
                return;
            case Byte:
                nestedPropertiesNode.set("type", new TextNode(INTEGER_DATA_TYPE));
                nestedPropertiesNode.set(MINIMUM_KEY_WORD, new ShortNode(UByte.MIN_VALUE));
                nestedPropertiesNode.set(MAXIMUM_KEY_WORD, new ShortNode(UByte.MAX_VALUE));
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
            case DateTime:
                nestedPropertiesNode.set("type", new TextNode("string"));
                nestedPropertiesNode.set("format", new TextNode("date-time"));
                return;
            case Int16:
                nestedPropertiesNode.set("type", new TextNode(INTEGER_DATA_TYPE));
                nestedPropertiesNode.set(MINIMUM_KEY_WORD, new ShortNode(Short.MIN_VALUE));
                nestedPropertiesNode.set(MAXIMUM_KEY_WORD, new ShortNode(Short.MAX_VALUE));
                return;
            case UInt16:
                nestedPropertiesNode.set("type", new TextNode(INTEGER_DATA_TYPE));
                nestedPropertiesNode.set(MINIMUM_KEY_WORD, new IntNode(UShort.MIN.intValue()));
                nestedPropertiesNode.set(MAXIMUM_KEY_WORD, new LongNode(UShort.MAX.intValue()));
                return;
            case StatusCode:
            case Int32:
                nestedPropertiesNode.set("type", new TextNode(INTEGER_DATA_TYPE));
                nestedPropertiesNode.set(MINIMUM_KEY_WORD, new LongNode(Integer.MIN_VALUE));
                nestedPropertiesNode.set(MAXIMUM_KEY_WORD, new LongNode(Integer.MAX_VALUE));
                return;
            case UInt32:
                nestedPropertiesNode.set("type", new TextNode(INTEGER_DATA_TYPE));
                nestedPropertiesNode.set(MINIMUM_KEY_WORD, new LongNode(UInteger.MIN_VALUE));
                nestedPropertiesNode.set(MAXIMUM_KEY_WORD, new LongNode(UInteger.MAX_VALUE));
                return;
            case Int64:
                nestedPropertiesNode.set("type", new TextNode(INTEGER_DATA_TYPE));
                nestedPropertiesNode.set(MINIMUM_KEY_WORD, new LongNode(Long.MIN_VALUE));
                nestedPropertiesNode.set(MAXIMUM_KEY_WORD, new LongNode(Long.MAX_VALUE));
                return;
            case UInt64:
                nestedPropertiesNode.set("type", new TextNode(INTEGER_DATA_TYPE));
                nestedPropertiesNode.set(MINIMUM_KEY_WORD, new BigIntegerNode(ULong.MIN_VALUE));
                nestedPropertiesNode.set(MAXIMUM_KEY_WORD, new BigIntegerNode(ULong.MAX_VALUE));
                return;
            case Float:
                nestedPropertiesNode.set("type", new TextNode("number"));
                nestedPropertiesNode.set(MINIMUM_KEY_WORD, new FloatNode(-Float.MAX_VALUE));
                nestedPropertiesNode.set(MAXIMUM_KEY_WORD, new FloatNode(Float.MAX_VALUE));
                return;
            case Double:
                nestedPropertiesNode.set("type", new TextNode("number"));
                nestedPropertiesNode.set(MINIMUM_KEY_WORD, new DoubleNode(-Double.MAX_VALUE));
                nestedPropertiesNode.set(MAXIMUM_KEY_WORD, new DoubleNode(Double.MAX_VALUE));
                return;
            case QualifiedName:
                nestedPropertiesNode.set("type", new TextNode("object"));

                final ObjectNode innerProperties = objectMapper.createObjectNode();

                final ObjectNode namespaceIndexNode = objectMapper.createObjectNode();
                namespaceIndexNode.set("type", new TextNode(INTEGER_DATA_TYPE));
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
                throw new RuntimeException("unsupported builtin data type '" + builtinDataType.name() + "'");
        }
    }
}
