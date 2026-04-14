/*
 * Copyright 2019-present HiveMQ GmbH
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
package com.hivemq.schema;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.schema.AnySchema;
import com.hivemq.adapter.sdk.api.schema.ArraySchema;
import com.hivemq.adapter.sdk.api.schema.ObjectSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SchemaJsonRepresentationTest {

    private final com.hivemq.adapter.sdk.api.schema.SchemaJsonRepresentation repr =
            com.hivemq.adapter.sdk.api.schema.SchemaJsonRepresentation.INSTANCE;

    // ── Scalar → JSON ────────────────────────────────────────────────────────

    @Test
    void test_toJson_scalarLong() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .scalar(ScalarType.LONG)
                .build();
        final ObjectNode json = repr.toJsonSchema(schema);

        assertThat(json.get("type").asText()).isEqualTo("integer");
        assertThat(json.get("readOnly").asBoolean()).isTrue();
    }

    @Test
    void test_toJson_scalarBoolean() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .scalar(ScalarType.BOOLEAN)
                .build();
        final ObjectNode json = repr.toJsonSchema(schema);

        assertThat(json.get("type").asText()).isEqualTo("boolean");
    }

    @Test
    void test_toJson_scalarDouble() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .scalar(ScalarType.DOUBLE)
                .build();
        final ObjectNode json = repr.toJsonSchema(schema);

        assertThat(json.get("type").asText()).isEqualTo("number");
    }

    @Test
    void test_toJson_scalarString() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .scalar(ScalarType.STRING)
                .build();
        final ObjectNode json = repr.toJsonSchema(schema);

        assertThat(json.get("type").asText()).isEqualTo("string");
    }

    @Test
    void test_toJson_scalarUlong() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .scalar(ScalarType.ULONG)
                .build();
        final ObjectNode json = repr.toJsonSchema(schema);

        assertThat(json.get("type").asText()).isEqualTo("integer");
    }

    @Test
    void test_toJson_scalarBinary() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .scalar(ScalarType.BINARY)
                .build();
        final ObjectNode json = repr.toJsonSchema(schema);

        assertThat(json.get("type").asText()).isEqualTo("string");
        assertThat(json.get("contentEncoding").asText()).isEqualTo("base64");
    }

    // ── Nullable scalar → JSON ───────────────────────────────────────────────

    @Test
    void test_toJson_nullableScalar() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .scalar(ScalarType.LONG)
                .nullable()
                .build();
        final ObjectNode json = repr.toJsonSchema(schema);

        assertThat(json.get("type").isArray()).isTrue();
        assertThat(json.get("type")).hasSize(2);
        assertThat(json.get("type").get(0).asText()).isEqualTo("integer");
        assertThat(json.get("type").get(1).asText()).isEqualTo("null");
    }

    // ── Annotations → JSON ───────────────────────────────────────────────────

    @Test
    void test_toJson_scalarWithAnnotations() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .scalar(ScalarType.LONG)
                .nullable()
                .title("Motor Speed")
                .description("RPM of the main shaft")
                .readable(true)
                .writable(false)
                .build();
        final ObjectNode json = repr.toJsonSchema(schema);

        assertThat(json.get("title").asText()).isEqualTo("Motor Speed");
        assertThat(json.get("description").asText()).isEqualTo("RPM of the main shaft");
        assertThat(json.get("readOnly").asBoolean()).isTrue();
    }

    @Test
    void test_toJson_writableDoesNotEmitReadOnly() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .scalar(ScalarType.LONG)
                .writable(true)
                .build();
        final ObjectNode json = repr.toJsonSchema(schema);

        assertThat(json.has("readOnly")).isFalse();
    }

    @Test
    void test_toJson_notReadableEmitsWriteOnly() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .scalar(ScalarType.LONG)
                .readable(false)
                .writable(true)
                .build();
        final ObjectNode json = repr.toJsonSchema(schema);

        assertThat(json.get("writeOnly").asBoolean()).isTrue();
    }

    // ── Any → JSON ───────────────────────────────────────────────────────────

    @Test
    void test_toJson_any() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .any()
                .build();
        final ObjectNode json = repr.toJsonSchema(schema);

        assertThat(json.has("type")).isFalse();
        assertThat(json.get("readOnly").asBoolean()).isTrue();
    }

    // ── Object → JSON ────────────────────────────────────────────────────────

    @Test
    void test_toJson_object() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .startObject()
                .property("rpm")
                .required()
                .scalar(ScalarType.LONG)
                .title("Motor Speed")
                .description("RPM")
                .readable(true)
                .writable(false)
                .property("bearing")
                .startObject()
                .property("temperature")
                .required()
                .scalar(ScalarType.DOUBLE)
                .property("vibration")
                .required()
                .scalar(ScalarType.DOUBLE)
                .additionalProperties(false)
                .endObject()
                .endObject()
                .build();

        final ObjectNode json = repr.toJsonSchema(schema);

        assertThat(json.get("type").asText()).isEqualTo("object");
        assertThat(json.get("required")).hasSize(1);
        assertThat(json.get("required").get(0).asText()).isEqualTo("rpm");

        final var rpmNode = json.get("properties").get("rpm");
        assertThat(rpmNode.get("type").asText()).isEqualTo("integer");
        assertThat(rpmNode.get("title").asText()).isEqualTo("Motor Speed");

        final var bearingNode = json.get("properties").get("bearing");
        assertThat(bearingNode.get("type").asText()).isEqualTo("object");
        assertThat(bearingNode.get("required")).hasSize(2);
        assertThat(bearingNode.get("additionalProperties").asBoolean()).isFalse();
    }

    @Test
    void test_toJson_objectNoRequired_omitsRequiredField() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .startObject()
                .property("x")
                .scalar(ScalarType.LONG)
                .endObject()
                .build();

        final ObjectNode json = repr.toJsonSchema(schema);
        assertThat(json.has("required")).isFalse();
    }

    @Test
    void test_toJson_nullableObject_usesAnyOf() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .startObject()
                .property("x")
                .scalar(ScalarType.LONG)
                .endObject()
                .nullable()
                .build();

        final ObjectNode json = repr.toJsonSchema(schema);

        assertThat(json.has("anyOf")).isTrue();
        assertThat(json.get("anyOf")).hasSize(2);
        assertThat(json.get("anyOf").get(0).get("type").asText()).isEqualTo("object");
        assertThat(json.get("anyOf").get(1).get("type").asText()).isEqualTo("null");
    }

    // ── Array → JSON ─────────────────────────────────────────────────────────

    @Test
    void test_toJson_array() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .startArray()
                .scalar(ScalarType.LONG)
                .minContains(1)
                .maxContains(10)
                .endArray()
                .build();

        final ObjectNode json = repr.toJsonSchema(schema);

        assertThat(json.get("type").asText()).isEqualTo("array");
        assertThat(json.get("items").get("type").asText()).isEqualTo("integer");
        assertThat(json.get("minContains").asInt()).isEqualTo(1);
        assertThat(json.get("maxContains").asInt()).isEqualTo(10);
    }

    @Test
    void test_toJson_nullableArray_usesAnyOf() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .startArray()
                .scalar(ScalarType.LONG)
                .endArray()
                .nullable()
                .build();

        final ObjectNode json = repr.toJsonSchema(schema);
        assertThat(json.has("anyOf")).isTrue();
        assertThat(json.get("anyOf")).hasSize(2);
    }

    // ── JSON → Schema (reverse) ──────────────────────────────────────────────

    @Test
    void test_fromJson_scalarInteger() {
        final Schema schema = repr.fromJsonSchemaString("""
                {"type":"integer"}""");

        assertThat(schema).isInstanceOf(ScalarSchema.class);
        assertThat(((ScalarSchema) schema).type()).isEqualTo(ScalarType.LONG);
        assertThat(schema.nullable()).isFalse();
    }

    @Test
    void test_fromJson_scalarNullable() {
        final Schema schema = repr.fromJsonSchemaString("""
                {"type":["integer","null"]}""");

        assertThat(schema).isInstanceOf(ScalarSchema.class);
        assertThat(((ScalarSchema) schema).type()).isEqualTo(ScalarType.LONG);
        assertThat(schema.nullable()).isTrue();
    }

    @Test
    void test_fromJson_scalarBoolean() {
        final Schema schema = repr.fromJsonSchemaString("""
                {"type":"boolean"}""");
        assertThat(((ScalarSchema) schema).type()).isEqualTo(ScalarType.BOOLEAN);
    }

    @Test
    void test_fromJson_scalarNumber() {
        final Schema schema = repr.fromJsonSchemaString("""
                {"type":"number"}""");
        assertThat(((ScalarSchema) schema).type()).isEqualTo(ScalarType.DOUBLE);
    }

    @Test
    void test_fromJson_scalarString() {
        final Schema schema = repr.fromJsonSchemaString("""
                {"type":"string"}""");
        assertThat(((ScalarSchema) schema).type()).isEqualTo(ScalarType.STRING);
    }

    @Test
    void test_fromJson_object() {
        final Schema schema = repr.fromJsonSchemaString("""
                {
                  "type": "object",
                  "properties": {
                    "rpm": {"type": "integer", "title": "Motor Speed"},
                    "label": {"type": "string"}
                  },
                  "required": ["rpm"]
                }""");

        assertThat(schema).isInstanceOf(ObjectSchema.class);
        final var o = (ObjectSchema) schema;
        assertThat(o.properties()).hasSize(2);
        assertThat(o.required()).containsExactly("rpm");
        assertThat(((ScalarSchema) o.properties().get("rpm")).type()).isEqualTo(ScalarType.LONG);
        assertThat(o.properties().get("rpm").title()).isEqualTo("Motor Speed");
    }

    @Test
    void test_fromJson_objectWithAdditionalPropertiesFalse() {
        final Schema schema = repr.fromJsonSchemaString("""
                {"type":"object","properties":{"x":{"type":"integer"}},"additionalProperties":false}""");

        assertThat(((ObjectSchema) schema).additionalProperties()).isFalse();
    }

    @Test
    void test_fromJson_array() {
        final Schema schema = repr.fromJsonSchemaString("""
                {"type":"array","items":{"type":"number"},"minContains":1,"maxContains":5}""");

        assertThat(schema).isInstanceOf(ArraySchema.class);
        final var a = (ArraySchema) schema;
        assertThat(((ScalarSchema) a.items()).type()).isEqualTo(ScalarType.DOUBLE);
        assertThat(a.minContains()).isEqualTo(1);
        assertThat(a.maxContains()).isEqualTo(5);
    }

    @Test
    void test_fromJson_emptySchema() {
        final Schema schema = repr.fromJsonSchemaString("{}");

        assertThat(schema).isInstanceOf(AnySchema.class);
    }

    @Test
    void test_fromJson_readOnlyAnnotation() {
        final Schema schema = repr.fromJsonSchemaString("""
                {"type":"integer","readOnly":true}""");

        assertThat(schema.writable()).isFalse();
        assertThat(schema.readable()).isTrue();
    }

    @Test
    void test_fromJson_writeOnlyAnnotation() {
        final Schema schema = repr.fromJsonSchemaString("""
                {"type":"integer","writeOnly":true}""");

        assertThat(schema.readable()).isFalse();
        assertThat(schema.writable()).isTrue();
    }

    @Test
    void test_fromJson_invalidJson_throwsException() {
        assertThatThrownBy(() -> repr.fromJsonSchemaString("{invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid JSON impl");
    }

    @Test
    void test_fromJson_unknownType_throwsException() {
        assertThatThrownBy(() -> repr.fromJsonSchemaString("""
                {"type":"foo"}"""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown JSON Schema type");
    }

    // ── Round-trip: Schema → JSON → Schema ───────────────────────────────────

    @Test
    void test_roundTrip_scalarLong() {
        final Schema original = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .scalar(ScalarType.LONG)
                .title("RPM")
                .description("Rotational speed")
                .build();

        final String json = repr.toJsonSchemaString(original);
        final Schema recovered = repr.fromJsonSchemaString(json);

        assertThat(recovered).isInstanceOf(ScalarSchema.class);
        assertThat(((ScalarSchema) recovered).type()).isEqualTo(ScalarType.LONG);
        assertThat(recovered.title()).isEqualTo("RPM");
        assertThat(recovered.description()).isEqualTo("Rotational speed");
    }

    @Test
    void test_roundTrip_nullableScalar() {
        final Schema original = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .scalar(ScalarType.DOUBLE)
                .nullable()
                .build();

        final String json = repr.toJsonSchemaString(original);
        final Schema recovered = repr.fromJsonSchemaString(json);

        assertThat(((ScalarSchema) recovered).type()).isEqualTo(ScalarType.DOUBLE);
        assertThat(recovered.nullable()).isTrue();
    }

    @Test
    void test_roundTrip_object() {
        final Schema original = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .startObject()
                .property("x")
                .required()
                .scalar(ScalarType.LONG)
                .property("y")
                .scalar(ScalarType.STRING)
                .additionalProperties(false)
                .endObject()
                .title("Point")
                .build();

        final String json = repr.toJsonSchemaString(original);
        final Schema recovered = repr.fromJsonSchemaString(json);

        assertThat(recovered).isInstanceOf(ObjectSchema.class);
        final var o = (ObjectSchema) recovered;
        assertThat(o.properties()).hasSize(2);
        assertThat(o.required()).containsExactly("x");
        assertThat(o.additionalProperties()).isFalse();
        assertThat(o.title()).isEqualTo("Point");
    }

    @Test
    void test_roundTrip_array() {
        final Schema original = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .startArray()
                .scalar(ScalarType.LONG)
                .minContains(0)
                .maxContains(100)
                .endArray()
                .build();

        final String json = repr.toJsonSchemaString(original);
        final Schema recovered = repr.fromJsonSchemaString(json);

        assertThat(recovered).isInstanceOf(ArraySchema.class);
        final var a = (ArraySchema) recovered;
        assertThat(((ScalarSchema) a.items()).type()).isEqualTo(ScalarType.LONG);
        assertThat(a.minContains()).isEqualTo(0);
        assertThat(a.maxContains()).isEqualTo(100);
    }

    // ── Known round-trip losses ──────────────────────────────────────────────

    @Test
    void test_roundTripLoss_ulongBecomesLong() {
        final Schema original = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .scalar(ScalarType.ULONG)
                .build();
        final String json = repr.toJsonSchemaString(original);
        final Schema recovered = repr.fromJsonSchemaString(json);

        // ULONG → "integer" → LONG
        assertThat(((ScalarSchema) recovered).type()).isEqualTo(ScalarType.LONG);
    }

    @Test
    void test_roundTripLoss_binaryBecomesString() {
        final Schema original = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .scalar(ScalarType.BINARY)
                .build();
        final String json = repr.toJsonSchemaString(original);
        final Schema recovered = repr.fromJsonSchemaString(json);

        // BINARY → "string"/"base64" → STRING (contentEncoding not inspected on reverse)
        assertThat(((ScalarSchema) recovered).type()).isEqualTo(ScalarType.STRING);
    }

    // ── Minimum / Maximum ────────────────────────────────────────────────────

    @Test
    void test_toJson_scalarWithMinMax() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .scalar(ScalarType.LONG)
                .minimum(-128)
                .maximum(127)
                .build();
        final ObjectNode json = repr.toJsonSchema(schema);

        assertThat(json.get("minimum").asLong()).isEqualTo(-128);
        assertThat(json.get("maximum").asLong()).isEqualTo(127);
    }

    @Test
    void test_toJson_scalarWithoutMinMax_omitsFields() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .scalar(ScalarType.LONG)
                .build();
        final ObjectNode json = repr.toJsonSchema(schema);

        assertThat(json.has("minimum")).isFalse();
        assertThat(json.has("maximum")).isFalse();
    }

    @Test
    void test_fromJson_scalarWithMinMax() {
        final Schema schema = repr.fromJsonSchemaString("""
                {"type":"integer","minimum":-32768,"maximum":32767}""");

        final var s = (ScalarSchema) schema;
        assertThat(s.minimum().longValue()).isEqualTo(-32768);
        assertThat(s.maximum().longValue()).isEqualTo(32767);
    }

    @Test
    void test_fromJson_scalarWithoutMinMax() {
        final Schema schema = repr.fromJsonSchemaString("""
                {"type":"integer"}""");

        final var s = (ScalarSchema) schema;
        assertThat(s.minimum()).isNull();
        assertThat(s.maximum()).isNull();
    }

    @Test
    void test_roundTrip_scalarWithMinMax() {
        final Schema original = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .scalar(ScalarType.LONG)
                .minimum(0)
                .maximum(65535)
                .title("UInt16")
                .build();

        final String json = repr.toJsonSchemaString(original);
        final Schema recovered = repr.fromJsonSchemaString(json);

        final var s = (ScalarSchema) recovered;
        assertThat(s.type()).isEqualTo(ScalarType.LONG);
        assertThat(s.minimum().longValue()).isEqualTo(0);
        assertThat(s.maximum().longValue()).isEqualTo(65535);
        assertThat(s.title()).isEqualTo("UInt16");
    }

    @Test
    void test_roundTrip_scalarDoubleWithMinMax() {
        final Schema original = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .scalar(ScalarType.DOUBLE)
                .minimum(-1.5)
                .maximum(99.9)
                .build();

        final String json = repr.toJsonSchemaString(original);
        final Schema recovered = repr.fromJsonSchemaString(json);

        final var s = (ScalarSchema) recovered;
        assertThat(s.minimum().doubleValue()).isEqualTo(-1.5);
        assertThat(s.maximum().doubleValue()).isEqualTo(99.9);
    }

    // ── Temporal scalars → JSON ──────────────────────────────────────────────

    @Test
    void test_toJson_scalarInstant() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .scalar(ScalarType.INSTANT)
                .build();
        final ObjectNode json = repr.toJsonSchema(schema);

        assertThat(json.get("type").asText()).isEqualTo("string");
        assertThat(json.get("format").asText()).isEqualTo("date-time");
    }

    @Test
    void test_toJson_scalarLocalDate() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .scalar(ScalarType.LOCAL_DATE)
                .build();
        final ObjectNode json = repr.toJsonSchema(schema);

        assertThat(json.get("type").asText()).isEqualTo("string");
        assertThat(json.get("format").asText()).isEqualTo("date");
    }

    @Test
    void test_toJson_scalarLocalTime() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .scalar(ScalarType.LOCAL_TIME)
                .build();
        final ObjectNode json = repr.toJsonSchema(schema);

        assertThat(json.get("type").asText()).isEqualTo("string");
        assertThat(json.get("format").asText()).isEqualTo("local-time");
    }

    @Test
    void test_toJson_scalarLocalDateTime() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .scalar(ScalarType.LOCAL_DATE_TIME)
                .build();
        final ObjectNode json = repr.toJsonSchema(schema);

        assertThat(json.get("type").asText()).isEqualTo("string");
        assertThat(json.get("format").asText()).isEqualTo("local-date-time");
    }

    @Test
    void test_toJson_scalarDuration() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .scalar(ScalarType.DURATION)
                .build();
        final ObjectNode json = repr.toJsonSchema(schema);

        assertThat(json.get("type").asText()).isEqualTo("string");
        assertThat(json.get("format").asText()).isEqualTo("duration");
    }

    @Test
    void test_toJson_nullableTemporalPreservesFormat() {
        final Schema schema = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .scalar(ScalarType.LOCAL_DATE_TIME)
                .nullable()
                .build();
        final ObjectNode json = repr.toJsonSchema(schema);

        final var typeNode = json.get("type");
        assertThat(typeNode.isArray()).isTrue();
        assertThat(typeNode.get(0).asText()).isEqualTo("string");
        assertThat(typeNode.get(1).asText()).isEqualTo("null");
        assertThat(json.get("format").asText()).isEqualTo("local-date-time");
    }

    // ── Temporal round-trip ──────────────────────────────────────────────────

    @Test
    void test_roundTrip_scalarInstant() {
        assertTemporalRoundTrip(ScalarType.INSTANT);
    }

    @Test
    void test_roundTrip_scalarLocalDate() {
        assertTemporalRoundTrip(ScalarType.LOCAL_DATE);
    }

    @Test
    void test_roundTrip_scalarLocalTime() {
        assertTemporalRoundTrip(ScalarType.LOCAL_TIME);
    }

    @Test
    void test_roundTrip_scalarLocalDateTime() {
        assertTemporalRoundTrip(ScalarType.LOCAL_DATE_TIME);
    }

    @Test
    void test_roundTrip_scalarDuration() {
        assertTemporalRoundTrip(ScalarType.DURATION);
    }

    @Test
    void test_fromJson_unknownFormatStaysString() {
        final Schema recovered = repr.fromJsonSchemaString(
                "{\"type\":\"string\",\"format\":\"email\"}");
        assertThat(((ScalarSchema) recovered).type()).isEqualTo(ScalarType.STRING);
    }

    private void assertTemporalRoundTrip(final ScalarType type) {
        final Schema original = new com.hivemq.adapter.sdk.api.schema.SchemaBuilder()
                .scalar(type)
                .build();

        final String json = repr.toJsonSchemaString(original);
        final Schema recovered = repr.fromJsonSchemaString(json);

        assertThat(recovered).isInstanceOf(ScalarSchema.class);
        assertThat(((ScalarSchema) recovered).type()).isEqualTo(type);
    }
}
