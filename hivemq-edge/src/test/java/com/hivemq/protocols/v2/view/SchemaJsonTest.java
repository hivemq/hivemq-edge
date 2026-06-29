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
package com.hivemq.protocols.v2.view;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.schema.AnySchema;
import com.hivemq.adapter.sdk.api.schema.ArraySchema;
import com.hivemq.adapter.sdk.api.schema.ObjectSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.schema.SchemaJsonRepresentation;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * S22 precursor: the v2 API serves the reused v1 {@link Schema} through the reused
 * {@link SchemaJsonRepresentation} projection — no v2-side copy exists (see {@code ReuseNoForkTest}).
 * This locks the projection contract the v2 schema endpoints rely on: every reused {@link ScalarType} maps to
 * the expected JSON-Schema {@code type}/{@code format}, and the constraints are projected.
 */
class SchemaJsonTest {

    private static @NotNull ObjectNode toJsonSchema(final @NotNull Schema schema) {
        return SchemaJsonRepresentation.INSTANCE.toJsonSchema(schema);
    }

    private static @NotNull ScalarSchema scalar(
            final @NotNull ScalarType type, final @Nullable Number minimum, final @Nullable Number maximum) {
        return new ScalarSchema(type, minimum, maximum, null, null, false, true, false);
    }

    /**
     * Compiles WITHOUT a {@code default} branch — a new {@link ScalarType} constant breaks this test at
     * compile time until its projection is locked in here.
     */
    private static @NotNull String expectedJsonType(final @NotNull ScalarType type) {
        return switch (type) {
            case BOOLEAN -> "boolean";
            case LONG, ULONG -> "integer";
            case DOUBLE -> "number";
            case STRING, BINARY, INSTANT, LOCAL_DATE, LOCAL_TIME, LOCAL_DATE_TIME, DURATION -> "string";
        };
    }

    private static @Nullable String expectedJsonFormat(final @NotNull ScalarType type) {
        return switch (type) {
            case BOOLEAN, LONG, ULONG, DOUBLE, STRING, BINARY -> null;
            case INSTANT -> "date-time";
            case LOCAL_DATE -> "date";
            case LOCAL_TIME -> "local-time";
            case LOCAL_DATE_TIME -> "local-date-time";
            case DURATION -> "duration";
        };
    }

    @Test
    void everyScalarType_projectsTheExpectedJsonTypeAndFormat() {
        for (final ScalarType scalarType : ScalarType.values()) {
            final ObjectNode jsonSchema = toJsonSchema(scalar(scalarType, null, null));

            assertThat(jsonSchema.get("type").asText())
                    .as("JSON-Schema type for %s", scalarType)
                    .isEqualTo(expectedJsonType(scalarType));

            final String expectedFormat = expectedJsonFormat(scalarType);
            if (expectedFormat == null) {
                assertThat(jsonSchema.has("format"))
                        .as("no format for %s", scalarType)
                        .isFalse();
            } else {
                assertThat(jsonSchema.get("format").asText())
                        .as("JSON-Schema format for %s", scalarType)
                        .isEqualTo(expectedFormat);
            }
        }
    }

    @Test
    void binary_projectsBase64ContentEncoding() {
        final ObjectNode jsonSchema = toJsonSchema(scalar(ScalarType.BINARY, null, null));
        assertThat(jsonSchema.get("contentEncoding").asText()).isEqualTo("base64");
    }

    @Test
    void scalarRangeConstraints_areProjected() {
        final ObjectNode integerSchema = toJsonSchema(scalar(ScalarType.LONG, 0L, 100L));
        assertThat(integerSchema.get("minimum").asLong()).isEqualTo(0L);
        assertThat(integerSchema.get("maximum").asLong()).isEqualTo(100L);

        final ObjectNode numberSchema = toJsonSchema(scalar(ScalarType.DOUBLE, -1.5d, 1.5d));
        assertThat(numberSchema.get("minimum").asDouble()).isEqualTo(-1.5d);
        assertThat(numberSchema.get("maximum").asDouble()).isEqualTo(1.5d);
    }

    @Test
    void objectConstraints_areProjected() {
        final Schema objectSchema = new ObjectSchema(
                Map.of("temperature", scalar(ScalarType.DOUBLE, null, null)),
                List.of("temperature"),
                false,
                null,
                null,
                false,
                true,
                false);

        final ObjectNode jsonSchema = toJsonSchema(objectSchema);

        assertThat(jsonSchema.get("type").asText()).isEqualTo("object");
        assertThat(jsonSchema.get("properties").get("temperature").get("type").asText())
                .isEqualTo("number");
        assertThat(jsonSchema.get("required")).hasSize(1);
        assertThat(jsonSchema.get("required").get(0).asText()).isEqualTo("temperature");
        assertThat(jsonSchema.get("additionalProperties").asBoolean()).isFalse();
    }

    @Test
    void arrayConstraints_areProjected() {
        final Schema arraySchema =
                new ArraySchema(scalar(ScalarType.LONG, null, null), 1, 8, null, null, false, true, false);

        final ObjectNode jsonSchema = toJsonSchema(arraySchema);

        assertThat(jsonSchema.get("type").asText()).isEqualTo("array");
        assertThat(jsonSchema.get("items").get("type").asText()).isEqualTo("integer");
        assertThat(jsonSchema.get("minContains").asInt()).isEqualTo(1);
        assertThat(jsonSchema.get("maxContains").asInt()).isEqualTo(8);
    }

    @Test
    void anySchema_projectsNoTypeRestriction() {
        final ObjectNode jsonSchema = toJsonSchema(new AnySchema(null, null, false, true, false));
        assertThat(jsonSchema.has("type")).isFalse();
    }
}
