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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.milo.opcua.stack.core.OpcUaDataType;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class BuiltinJsonSchemaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void whenAddReadOnlyMetadataProperties_thenAllMetadataPropertiesAreAdded() {
        final ObjectNode propertiesNode = MAPPER.createObjectNode();

        BuiltinJsonSchema.addReadOnlyMetadataProperties(propertiesNode, MAPPER);

        assertThat(propertiesNode.has("sourceTime")).isTrue();
        assertThat(propertiesNode.has("serverTime")).isTrue();
        assertThat(propertiesNode.has("sourcePicoseconds")).isTrue();
        assertThat(propertiesNode.has("serverPicoseconds")).isTrue();
        assertThat(propertiesNode.has("statusCode")).isTrue();
    }

    @Test
    void whenAddReadOnlyMetadataProperties_thenSourceTimestampIsReadOnly() {
        final ObjectNode propertiesNode = MAPPER.createObjectNode();

        BuiltinJsonSchema.addReadOnlyMetadataProperties(propertiesNode, MAPPER);

        final JsonNode sourceTimestamp = propertiesNode.get("sourceTime");
        assertThat(sourceTimestamp.get("type").asText()).isEqualTo("string");
        assertThat(sourceTimestamp.get("format").asText()).isEqualTo("date-time");
        assertThat(sourceTimestamp.get("readOnly").asBoolean()).isTrue();
    }

    @Test
    void whenAddReadOnlyMetadataProperties_thenServerTimestampIsReadOnly() {
        final ObjectNode propertiesNode = MAPPER.createObjectNode();

        BuiltinJsonSchema.addReadOnlyMetadataProperties(propertiesNode, MAPPER);

        final JsonNode serverTimestamp = propertiesNode.get("serverTime");
        assertThat(serverTimestamp.get("type").asText()).isEqualTo("string");
        assertThat(serverTimestamp.get("format").asText()).isEqualTo("date-time");
        assertThat(serverTimestamp.get("readOnly").asBoolean()).isTrue();
    }

    @Test
    void whenAddReadOnlyMetadataProperties_thenSourcePicosecondsIsReadOnlyWithValidRange() {
        final ObjectNode propertiesNode = MAPPER.createObjectNode();

        BuiltinJsonSchema.addReadOnlyMetadataProperties(propertiesNode, MAPPER);

        final JsonNode sourcePicoseconds = propertiesNode.get("sourcePicoseconds");
        assertThat(sourcePicoseconds.get("type").asText()).isEqualTo("integer");
        assertThat(sourcePicoseconds.get("minimum").asInt()).isEqualTo(0);
        assertThat(sourcePicoseconds.get("maximum").asInt()).isEqualTo(65535);
        assertThat(sourcePicoseconds.get("readOnly").asBoolean()).isTrue();
    }

    @Test
    void whenAddReadOnlyMetadataProperties_thenServerPicosecondsIsReadOnlyWithValidRange() {
        final ObjectNode propertiesNode = MAPPER.createObjectNode();

        BuiltinJsonSchema.addReadOnlyMetadataProperties(propertiesNode, MAPPER);

        final JsonNode serverPicoseconds = propertiesNode.get("serverPicoseconds");
        assertThat(serverPicoseconds.get("type").asText()).isEqualTo("integer");
        assertThat(serverPicoseconds.get("minimum").asInt()).isEqualTo(0);
        assertThat(serverPicoseconds.get("maximum").asInt()).isEqualTo(65535);
        assertThat(serverPicoseconds.get("readOnly").asBoolean()).isTrue();
    }

    @Test
    void whenAddReadOnlyMetadataProperties_thenStatusCodeIsReadOnlyObject() {
        final ObjectNode propertiesNode = MAPPER.createObjectNode();

        BuiltinJsonSchema.addReadOnlyMetadataProperties(propertiesNode, MAPPER);

        final JsonNode statusCode = propertiesNode.get("statusCode");
        assertThat(statusCode.get("type").asText()).isEqualTo("object");
        assertThat(statusCode.get("readOnly").asBoolean()).isTrue();
        assertThat(statusCode.has("properties")).isTrue();

        final JsonNode statusCodeProps = statusCode.get("properties");
        assertThat(statusCodeProps.has("code")).isTrue();
        assertThat(statusCodeProps.get("code").get("type").asText()).isEqualTo("integer");
        assertThat(statusCodeProps.has("symbol")).isTrue();
        assertThat(statusCodeProps.get("symbol").get("type").asText()).isEqualTo("string");
    }

    @ParameterizedTest
    @EnumSource(value = OpcUaDataType.class, names = {
            "Boolean",
            "Byte",
            "SByte",
            "Int16",
            "UInt16",
            "Int32",
            "UInt32",
            "Int64",
            "UInt64",
            "Float",
            "Double",
            "String",
            "DateTime"})
    void whenCreateJsonSchemaForBuiltInTypeWithMetadata_thenSchemaContainsMetadataProperties(
            final OpcUaDataType dataType) {
        final JsonNode schema = BuiltinJsonSchema.createJsonSchemaForBuiltInType(dataType, true);

        assertThat(schema).isNotNull();
        final JsonNode properties = schema.get("properties");
        assertThat(properties).isNotNull();
        assertThat(properties.has("value")).isTrue();
        assertThat(properties.has("sourceTime")).isTrue();
        assertThat(properties.has("serverTime")).isTrue();
        assertThat(properties.has("sourcePicoseconds")).isTrue();
        assertThat(properties.has("serverPicoseconds")).isTrue();
        assertThat(properties.has("statusCode")).isTrue();

        // Verify metadata properties are readOnly
        assertThat(properties.get("sourceTime").get("readOnly").asBoolean()).isTrue();
        assertThat(properties.get("serverTime").get("readOnly").asBoolean()).isTrue();
        assertThat(properties.get("sourcePicoseconds").get("readOnly").asBoolean()).isTrue();
        assertThat(properties.get("serverPicoseconds").get("readOnly").asBoolean()).isTrue();
        assertThat(properties.get("statusCode").get("readOnly").asBoolean()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = OpcUaDataType.class, names = {
            "Boolean",
            "Byte",
            "SByte",
            "Int16",
            "UInt16",
            "Int32",
            "UInt32",
            "Int64",
            "UInt64",
            "Float",
            "Double",
            "String",
            "DateTime"})
    void whenCreateJsonSchemaForBuiltInTypeWithoutMetadata_thenSchemaDoesNotContainMetadataProperties(
            final OpcUaDataType dataType) {
        final JsonNode schema = BuiltinJsonSchema.createJsonSchemaForBuiltInType(dataType, false);

        assertThat(schema).isNotNull();
        final JsonNode properties = schema.get("properties");
        assertThat(properties).isNotNull();
        assertThat(properties.has("value")).isTrue();
        assertThat(properties.has("sourceTime")).isFalse();
        assertThat(properties.has("serverTime")).isFalse();
        assertThat(properties.has("sourcePicoseconds")).isFalse();
        assertThat(properties.has("serverPicoseconds")).isFalse();
        assertThat(properties.has("statusCode")).isFalse();
    }

    @Test
    void whenCreateJsonSchemaForArrayTypeWithMetadata_thenSchemaContainsMetadataProperties() {
        final UInteger[] dimensions = new UInteger[]{UInteger.valueOf(10)};

        final JsonNode schema = BuiltinJsonSchema.createJsonSchemaForArrayType(OpcUaDataType.Int32, dimensions, true);

        assertThat(schema).isNotNull();
        final JsonNode properties = schema.get("properties");
        assertThat(properties).isNotNull();
        assertThat(properties.has("value")).isTrue();
        assertThat(properties.has("sourceTime")).isTrue();
        assertThat(properties.has("serverTime")).isTrue();
        assertThat(properties.has("sourcePicoseconds")).isTrue();
        assertThat(properties.has("serverPicoseconds")).isTrue();
        assertThat(properties.has("statusCode")).isTrue();

        // Verify metadata properties are readOnly
        assertThat(properties.get("sourceTime").get("readOnly").asBoolean()).isTrue();
    }

    @Test
    void whenCreateJsonSchemaForArrayTypeWithoutMetadata_thenSchemaDoesNotContainMetadataProperties() {
        final UInteger[] dimensions = new UInteger[]{UInteger.valueOf(10)};

        final JsonNode schema = BuiltinJsonSchema.createJsonSchemaForArrayType(OpcUaDataType.Int32, dimensions, false);

        assertThat(schema).isNotNull();
        final JsonNode properties = schema.get("properties");
        assertThat(properties).isNotNull();
        assertThat(properties.has("value")).isTrue();
        assertThat(properties.has("sourceTime")).isFalse();
        assertThat(properties.has("serverTime")).isFalse();
    }

    @Test
    void whenCreateJsonSchemaForBuiltInTypeWithMetadata_thenValueIsStillRequired() {
        final JsonNode schema = BuiltinJsonSchema.createJsonSchemaForBuiltInType(OpcUaDataType.Int32, true);

        assertThat(schema).isNotNull();
        final JsonNode required = schema.get("required");
        assertThat(required).isNotNull();
        assertThat(required.isArray()).isTrue();
        assertThat(required.size()).isEqualTo(1);
        assertThat(required.get(0).asText()).isEqualTo("value");
    }
}
