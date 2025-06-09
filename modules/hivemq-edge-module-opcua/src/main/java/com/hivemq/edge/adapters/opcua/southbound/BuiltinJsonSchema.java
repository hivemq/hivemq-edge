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
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ShortNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.hivemq.edge.adapters.opcua.Constants;
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
import java.util.Map;

import static com.hivemq.edge.adapters.opcua.Constants.ARRAY_ITEMS;
import static com.hivemq.edge.adapters.opcua.Constants.ARRAY_MAX_TIMES;
import static com.hivemq.edge.adapters.opcua.Constants.ARRAY_MIN_TIMES;
import static com.hivemq.edge.adapters.opcua.Constants.BOOLEAN_DATA_TYPE;
import static com.hivemq.edge.adapters.opcua.Constants.DATETIME_DATA_TYPE;
import static com.hivemq.edge.adapters.opcua.Constants.NUMBER_DATA_TYPE;
import static com.hivemq.edge.adapters.opcua.Constants.OBJECT_DATA_TYPE;
import static com.hivemq.edge.adapters.opcua.Constants.STRING_DATA_TYPE;
import static com.hivemq.edge.adapters.opcua.Constants.TYPE;

public class BuiltinJsonSchema {
    static final @NotNull String SCHEMA_URI = "https://json-schema.org/draft/2019-09/schema";
    static final @NotNull ObjectMapper MAPPER = new ObjectMapper();
    private static final @NotNull Logger log = LoggerFactory.getLogger("com.hivemq.edge.write.BuiltinJsonSchema");
    private static final @NotNull Map<OpcUaDataType, JsonNode> BUILT_IN_TYPES;

    static {
        try {
            final Map<OpcUaDataType, JsonNode> types = new HashMap<>();

            types.put(OpcUaDataType.Boolean,
                    createJsonSchemaForBuiltinType("Boolean JsonSchema", OpcUaDataType.Boolean));

            types.put(OpcUaDataType.SByte, createJsonSchemaForBuiltinType("SByte JsonSchema", OpcUaDataType.SByte));
            types.put(OpcUaDataType.Byte, createJsonSchemaForBuiltinType("Byte JsonSchema", OpcUaDataType.Byte));

            types.put(OpcUaDataType.UInt64, createJsonSchemaForBuiltinType("UInt64 JsonSchema", OpcUaDataType.UInt64));
            types.put(OpcUaDataType.UInt32, createJsonSchemaForBuiltinType("UInt32 JsonSchema", OpcUaDataType.UInt32));
            types.put(OpcUaDataType.UInt16, createJsonSchemaForBuiltinType("UInt16 JsonSchema", OpcUaDataType.UInt16));

            types.put(OpcUaDataType.Int64, createJsonSchemaForBuiltinType("Int64 JsonSchema", OpcUaDataType.Int64));
            types.put(OpcUaDataType.Int32, createJsonSchemaForBuiltinType("Int32 JsonSchema", OpcUaDataType.Int32));
            types.put(OpcUaDataType.Int16, createJsonSchemaForBuiltinType("Int16 JsonSchema", OpcUaDataType.Int16));

            types.put(OpcUaDataType.Float, createJsonSchemaForBuiltinType("Float JsonSchema", OpcUaDataType.Float));
            types.put(OpcUaDataType.Double, createJsonSchemaForBuiltinType("Double JsonSchema", OpcUaDataType.Double));
            types.put(OpcUaDataType.String, createJsonSchemaForBuiltinType("String JsonSchema", OpcUaDataType.String));

            types.put(OpcUaDataType.DateTime,
                    createJsonSchemaForBuiltinType("DateTime JsonSchema", OpcUaDataType.DateTime));

            types.put(OpcUaDataType.Guid, createJsonSchemaForBuiltinType("Guid JsonSchema", OpcUaDataType.String));
            types.put(OpcUaDataType.ByteString,
                    createJsonSchemaForBuiltinType("ByteString JsonSchema", OpcUaDataType.String));
            types.put(OpcUaDataType.XmlElement,
                    createJsonSchemaForBuiltinType("XmlElement JsonSchema", OpcUaDataType.String));

            types.put(OpcUaDataType.QualifiedName,
                    createJsonSchemaForBuiltinType("QualifiedName JsonSchema", OpcUaDataType.QualifiedName));

            types.put(OpcUaDataType.NodeId, createJsonSchemaForBuiltinType("XmlElement NodeId", OpcUaDataType.String));
            types.put(OpcUaDataType.ExpandedNodeId,
                    createJsonSchemaForBuiltinType("ExpandedNodeId JsonSchema", OpcUaDataType.String));

            types.put(OpcUaDataType.StatusCode,
                    createJsonSchemaForBuiltinType("StatusCode JsonSchema", OpcUaDataType.StatusCode));
            types.put(OpcUaDataType.LocalizedText,
                    createJsonSchemaForBuiltinType("LocalizedText JsonSchema", OpcUaDataType.String));

            BUILT_IN_TYPES = Map.copyOf(types);
        } catch (final Exception e) {
            log.error("Exception while initializing the JsonSchema for the builtin types:", e);
            throw new RuntimeException(e);
        }
    }

    static void populatePropertiesForArray(
            final @NotNull ObjectNode propertiesNode,
            final @NotNull OpcUaDataType builtinDataType,
            final @NotNull ObjectMapper objectMapper,
            final @NotNull UInteger @NotNull [] dimensions) {
        if (dimensions.length == 0) {
            throw new IllegalArgumentException("Array of " + builtinDataType.name() + " dimensions must not be empty");
        }
        final long maxSize = dimensions[0].longValue();

        propertiesNode.set(TYPE, new TextNode(Constants.ARRAY_DATA_TYPE));

        //0 for a dimension means unlimited
        if (maxSize > 0) {
            propertiesNode.set(ARRAY_MAX_TIMES, new LongNode(maxSize));
            propertiesNode.set(ARRAY_MIN_TIMES, new LongNode(maxSize));
        }
        final ObjectNode itemsNode = objectMapper.createObjectNode();
        propertiesNode.set(ARRAY_ITEMS, itemsNode);

        if (dimensions.length == 1) {
            //last element, we can now set the array type
            populatePropertiesForBuiltinType(itemsNode, builtinDataType, objectMapper);
        } else {
            //nesting deeper
            populatePropertiesForArray(itemsNode,
                    builtinDataType,
                    objectMapper,
                    Arrays.copyOfRange(dimensions, 1, dimensions.length));
        }
    }

    static void populatePropertiesForBuiltinType(
            final @NotNull ObjectNode nestedPropertiesNode,
            final @NotNull OpcUaDataType builtinDataType,
            final @NotNull ObjectMapper objectMapper) {
        switch (builtinDataType) {
            case Boolean:
                nestedPropertiesNode.set(TYPE, new TextNode(BOOLEAN_DATA_TYPE));
                return;
            case SByte:
                nestedPropertiesNode.set(TYPE, new TextNode(Constants.INTEGER_DATA_TYPE));
                nestedPropertiesNode.set(Constants.MINIMUM_KEY_WORD, new ShortNode(Byte.MIN_VALUE));
                nestedPropertiesNode.set(Constants.MAXIMUM_KEY_WORD, new ShortNode(Byte.MAX_VALUE));
                return;
            case Byte:
                nestedPropertiesNode.set(TYPE, new TextNode(Constants.INTEGER_DATA_TYPE));
                nestedPropertiesNode.set(Constants.MINIMUM_KEY_WORD, new ShortNode(UByte.MIN_VALUE));
                nestedPropertiesNode.set(Constants.MAXIMUM_KEY_WORD, new ShortNode(UByte.MAX_VALUE));
                return;
            case String:
            case Guid:
            case ByteString:
            case XmlElement:
            case NodeId:
            case ExpandedNodeId:
            case LocalizedText:
                nestedPropertiesNode.set(TYPE, new TextNode(STRING_DATA_TYPE));
                return;
            case DateTime:
                nestedPropertiesNode.set(TYPE, new TextNode(STRING_DATA_TYPE));
                nestedPropertiesNode.set("format", new TextNode(DATETIME_DATA_TYPE));
                return;
            case Int16:
                nestedPropertiesNode.set(TYPE, new TextNode(Constants.INTEGER_DATA_TYPE));
                nestedPropertiesNode.set(Constants.MINIMUM_KEY_WORD, new ShortNode(Short.MIN_VALUE));
                nestedPropertiesNode.set(Constants.MAXIMUM_KEY_WORD, new ShortNode(Short.MAX_VALUE));
                return;
            case UInt16:
                nestedPropertiesNode.set(TYPE, new TextNode(Constants.INTEGER_DATA_TYPE));
                nestedPropertiesNode.set(Constants.MINIMUM_KEY_WORD, new IntNode(UShort.MIN.intValue()));
                nestedPropertiesNode.set(Constants.MAXIMUM_KEY_WORD, new LongNode(UShort.MAX.intValue()));
                return;
            case StatusCode:
            case Int32:
                nestedPropertiesNode.set(TYPE, new TextNode(Constants.INTEGER_DATA_TYPE));
                nestedPropertiesNode.set(Constants.MINIMUM_KEY_WORD, new LongNode(Integer.MIN_VALUE));
                nestedPropertiesNode.set(Constants.MAXIMUM_KEY_WORD, new LongNode(Integer.MAX_VALUE));
                return;
            case UInt32:
                nestedPropertiesNode.set(TYPE, new TextNode(Constants.INTEGER_DATA_TYPE));
                nestedPropertiesNode.set(Constants.MINIMUM_KEY_WORD, new LongNode(UInteger.MIN_VALUE));
                nestedPropertiesNode.set(Constants.MAXIMUM_KEY_WORD, new LongNode(UInteger.MAX_VALUE));
                return;
            case Int64:
                nestedPropertiesNode.set(TYPE, new TextNode(Constants.INTEGER_DATA_TYPE));
                nestedPropertiesNode.set(Constants.MINIMUM_KEY_WORD, new LongNode(Long.MIN_VALUE));
                nestedPropertiesNode.set(Constants.MAXIMUM_KEY_WORD, new LongNode(Long.MAX_VALUE));
                return;
            case UInt64:
                nestedPropertiesNode.set(TYPE, new TextNode(Constants.INTEGER_DATA_TYPE));
                nestedPropertiesNode.set(Constants.MINIMUM_KEY_WORD, new BigIntegerNode(ULong.MIN_VALUE));
                nestedPropertiesNode.set(Constants.MAXIMUM_KEY_WORD, new BigIntegerNode(ULong.MAX_VALUE));
                return;
            case Float:
                nestedPropertiesNode.set(TYPE, new TextNode(NUMBER_DATA_TYPE));
                nestedPropertiesNode.set(Constants.MINIMUM_KEY_WORD, new FloatNode(-Float.MAX_VALUE));
                nestedPropertiesNode.set(Constants.MAXIMUM_KEY_WORD, new FloatNode(Float.MAX_VALUE));
                return;
            case Double:
                nestedPropertiesNode.set(TYPE, new TextNode(NUMBER_DATA_TYPE));
                nestedPropertiesNode.set(Constants.MINIMUM_KEY_WORD, new DoubleNode(-Double.MAX_VALUE));
                nestedPropertiesNode.set(Constants.MAXIMUM_KEY_WORD, new DoubleNode(Double.MAX_VALUE));
                return;
            case QualifiedName:
                nestedPropertiesNode.set(TYPE, new TextNode(OBJECT_DATA_TYPE));

                final ObjectNode innerProperties = objectMapper.createObjectNode();

                final ObjectNode namespaceIndexNode = objectMapper.createObjectNode();
                namespaceIndexNode.set(TYPE, new TextNode(Constants.INTEGER_DATA_TYPE));
                innerProperties.set("namespaceIndex", namespaceIndexNode);

                final ObjectNode nameNode = objectMapper.createObjectNode();
                nameNode.set(TYPE, new TextNode(STRING_DATA_TYPE));
                innerProperties.set("name", nameNode);

                nestedPropertiesNode.set("properties", innerProperties);
                final ArrayNode requiredAttributes = MAPPER.createArrayNode();
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

    private static @NotNull JsonNode createJsonSchemaForBuiltinType(
            final @NotNull String title,
            final @NotNull OpcUaDataType builtinDataType) {
        final ObjectNode rootNode = MAPPER.createObjectNode();
        final ObjectNode propertiesNode = MAPPER.createObjectNode();
        final ObjectNode valueNode = MAPPER.createObjectNode();
        rootNode.set("$schema", new TextNode(SCHEMA_URI));
        rootNode.set("title", new TextNode(title));
        rootNode.set(TYPE, new TextNode(OBJECT_DATA_TYPE));
        rootNode.set("properties", propertiesNode);
        propertiesNode.set("value", valueNode);

        populatePropertiesForBuiltinType(valueNode, builtinDataType, MAPPER);

        final ArrayNode requiredAttributes = MAPPER.createArrayNode();
        requiredAttributes.add("value");
        rootNode.set("required", requiredAttributes);
        return rootNode;
    }

    static @NotNull JsonNode createJsonSchemaForArrayType(
            final @NotNull OpcUaDataType builtinDataType,
            final @NotNull UInteger @NotNull [] dimensions) {
        final ObjectNode rootNode = MAPPER.createObjectNode();
        final ObjectNode propertiesNode = MAPPER.createObjectNode();
        final ObjectNode valueNode = MAPPER.createObjectNode();
        rootNode.set("$schema", new TextNode(SCHEMA_URI));
        rootNode.set("title", new TextNode("Array of " + builtinDataType.name() + " JsonSchema"));
        rootNode.set(TYPE, new TextNode(OBJECT_DATA_TYPE));
        rootNode.set("properties", propertiesNode);
        propertiesNode.set("value", valueNode);

        populatePropertiesForArray(valueNode, builtinDataType, MAPPER, dimensions);

        final ArrayNode requiredAttributes = MAPPER.createArrayNode();
        requiredAttributes.add("value");
        rootNode.set("required", requiredAttributes);
        return rootNode;
    }

    static @NotNull JsonNode createJsonSchemaForBuiltInType(final @NotNull OpcUaDataType builtinDataType) {
        return BUILT_IN_TYPES.get(builtinDataType);
    }
}
