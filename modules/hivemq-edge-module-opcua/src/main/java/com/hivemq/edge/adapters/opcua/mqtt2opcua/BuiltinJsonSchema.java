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
import org.eclipse.milo.opcua.stack.core.BuiltinDataType;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.ULong;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class BuiltinJsonSchema {

    private static final @NotNull ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final @NotNull Logger log = LoggerFactory.getLogger("com.hivemq.edge.write.BuiltinJsonSchema");
    private static final @NotNull String MINIMUM_KEY_WORD = "minimum";
    private static final @NotNull String MAXIMUM_KEY_WORD = "maximum";
    public static final @NotNull String INTEGER_DATA_TYPE = "integer";

    private final @NotNull HashMap<BuiltinDataType, JsonNode> classToJsonSchema = new HashMap<>();

    public BuiltinJsonSchema() {
        try {
            classToJsonSchema.put(BuiltinDataType.Boolean,
                    createJsonSchemaForBuiltinType("Boolean JsonSchema", BuiltinDataType.Boolean));
            final JsonNode byteJsonSchema = createJsonSchemaForBuiltinType(" Byte JsonSchema", BuiltinDataType.String);
            classToJsonSchema.put(BuiltinDataType.SByte, byteJsonSchema);
            classToJsonSchema.put(BuiltinDataType.Byte, byteJsonSchema);

            classToJsonSchema.put(BuiltinDataType.UInt64,
                    createJsonSchemaForBuiltinType("UInt64 JsonSchema", BuiltinDataType.UInt64));
            classToJsonSchema.put(BuiltinDataType.UInt32,
                    createJsonSchemaForBuiltinType("UInt32 JsonSchema", BuiltinDataType.UInt32));
            classToJsonSchema.put(BuiltinDataType.UInt16,
                    createJsonSchemaForBuiltinType("UInt16 JsonSchema", BuiltinDataType.UInt16));

            classToJsonSchema.put(BuiltinDataType.Int64,
                    createJsonSchemaForBuiltinType("Int64 JsonSchema", BuiltinDataType.Int64));
            classToJsonSchema.put(BuiltinDataType.Int32,
                    createJsonSchemaForBuiltinType("Int32 JsonSchema", BuiltinDataType.Int32));
            classToJsonSchema.put(BuiltinDataType.Int16,
                    createJsonSchemaForBuiltinType("Int16 JsonSchema", BuiltinDataType.Int16));

            classToJsonSchema.put(BuiltinDataType.Float,
                    createJsonSchemaForBuiltinType("Float JsonSchema", BuiltinDataType.Float));
            classToJsonSchema.put(BuiltinDataType.Double,
                    createJsonSchemaForBuiltinType("Double JsonSchema", BuiltinDataType.Double));
            classToJsonSchema.put(BuiltinDataType.String,
                    createJsonSchemaForBuiltinType("String JsonSchema", BuiltinDataType.String));

            classToJsonSchema.put(BuiltinDataType.DateTime,
                    createJsonSchemaForBuiltinType("DateTime JsonSchema", BuiltinDataType.DateTime));

            classToJsonSchema.put(BuiltinDataType.Guid,
                    createJsonSchemaForBuiltinType("Guid JsonSchema", BuiltinDataType.String));
            classToJsonSchema.put(BuiltinDataType.ByteString,
                    createJsonSchemaForBuiltinType("ByteString JsonSchema", BuiltinDataType.String));
            classToJsonSchema.put(BuiltinDataType.XmlElement,
                    createJsonSchemaForBuiltinType("XmlElement JsonSchema", BuiltinDataType.String));

            classToJsonSchema.put(BuiltinDataType.QualifiedName,
                    createJsonSchemaForBuiltinType("QualifiedName JsonSchema", BuiltinDataType.QualifiedName));

            classToJsonSchema.put(BuiltinDataType.NodeId,
                    createJsonSchemaForBuiltinType("XmlElement NodeId", BuiltinDataType.String));
            classToJsonSchema.put(BuiltinDataType.ExpandedNodeId,
                    createJsonSchemaForBuiltinType("ExpandedNodeId JsonSchema", BuiltinDataType.String));

            classToJsonSchema.put(BuiltinDataType.StatusCode,
                    createJsonSchemaForBuiltinType("StatusCode JsonSchema", BuiltinDataType.StatusCode));
            classToJsonSchema.put(BuiltinDataType.LocalizedText,
                    createJsonSchemaForBuiltinType("LocalizedText JsonSchema", BuiltinDataType.String));
        } catch (final Exception jsonSchemaGenerationException) {
            log.error("Exception while initializing the JsonSchema for the builtin types:",
                    jsonSchemaGenerationException);
            throw new RuntimeException(jsonSchemaGenerationException);
        }
    }

    public @NotNull JsonNode getJsonSchema(final @NotNull BuiltinDataType builtinDataType) {
        return classToJsonSchema.get(builtinDataType);
    }

    private @NotNull JsonNode createJsonSchemaForBuiltinType(
            final @NotNull String title,
            final @NotNull BuiltinDataType builtinDataType) {
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

    public static void populatePropertiesForBuiltinType(
            final @NotNull ObjectNode nestedPropertiesNode,
            final @NotNull BuiltinDataType builtinDataType,
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
            case DateTime:
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
                nestedPropertiesNode.set(MINIMUM_KEY_WORD, new FloatNode(Float.MIN_VALUE));
                nestedPropertiesNode.set(MAXIMUM_KEY_WORD, new FloatNode(Float.MAX_VALUE));
                return;
            case Double:
                nestedPropertiesNode.set("type", new TextNode("number"));
                nestedPropertiesNode.set(MINIMUM_KEY_WORD, new DoubleNode(Double.MIN_VALUE));
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
