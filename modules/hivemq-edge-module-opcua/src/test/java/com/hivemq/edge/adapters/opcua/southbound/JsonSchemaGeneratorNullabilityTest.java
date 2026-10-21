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
package com.hivemq.edge.adapters.opcua.southbound;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.adapter.sdk.api.schema.SchemaJsonRepresentation;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.milo.opcua.stack.core.OpcUaDataType;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/// A value is nullable in the schema exactly where its OPC UA type has a null value (EDG-1138), so the schema and
/// the converter agree on which `null` is a value: the schema lets it through, and the converter sends the type's
/// null value.
class JsonSchemaGeneratorNullabilityTest {

    private static @NotNull JsonNode render(final @NotNull OpcUaDataType type, final UInteger @Nullable [] dimensions) {
        return SchemaJsonRepresentation.INSTANCE.toJsonSchemaDocument(
                JsonSchemaGenerator.buildSchema(new JsonSchemaGenerator.FieldInformation(
                                "value", null, type, null, false, dimensions, false, true, true, List.of()))
                        .valueSchema());
    }

    private static @NotNull List<String> types(final @NotNull JsonNode schema) {
        final List<String> types = new ArrayList<>();
        final JsonNode type = schema.get("type");
        if (type == null) {
            return types;
        }
        if (type.isArray()) {
            type.forEach(t -> types.add(t.asText()));
        } else {
            types.add(type.asText());
        }
        return types;
    }

    @ParameterizedTest
    @EnumSource(
            value = OpcUaDataType.class,
            names = {"String", "ByteString", "XmlElement", "ExpandedNodeId"})
    void aScalarWhoseTypeHasANullValue_isNullable(final @NotNull OpcUaDataType type) {
        assertThat(types(render(type, null))).contains("null");
    }

    @ParameterizedTest
    @EnumSource(
            value = OpcUaDataType.class,
            names = {"Boolean", "Int32", "UInt64", "Double", "DateTime", "Guid", "StatusCode"})
    void aScalarWhoseTypeHasNoNullValue_isNotNullable(final @NotNull OpcUaDataType type) {
        assertThat(types(render(type, null))).doesNotContain("null");
    }

    @Test
    void aQualifiedName_isANullableObject() {
        final JsonNode schema = render(OpcUaDataType.QualifiedName, null);

        assertThat(schema.has("anyOf"))
                .as("a nullable object renders as anyOf [object, null]")
                .isTrue();
    }

    @Test
    void anArrayOfStrings_isNullable_andSoAreItsElements() {
        final JsonNode schema = render(OpcUaDataType.String, new UInteger[] {UInteger.valueOf(0)});

        final JsonNode array = theNonNullBranch(schema);
        assertThat(types(array)).containsExactly("array");
        assertThat(types(array.get("items"))).contains("string", "null");
    }

    @Test
    void anArrayOfIntegers_isNullable_butItsElementsAreNot() {
        // The null array is a value of every array, whatever its element type.
        final JsonNode schema = render(OpcUaDataType.Int32, new UInteger[] {UInteger.valueOf(0)});

        final JsonNode array = theNonNullBranch(schema);
        assertThat(types(array.get("items"))).doesNotContain("null");
    }

    @Test
    void theRowsOfATwoDimensionalArray_areNotNullable() {
        // A row is not a value of its own, and the converter refuses a null row.
        final JsonNode schema = render(OpcUaDataType.Int32, new UInteger[] {UInteger.valueOf(0), UInteger.valueOf(0)});

        final JsonNode rows = theNonNullBranch(schema).get("items");
        assertThat(types(rows)).containsExactly("array");
    }

    /// The schema of a nullable array or object, `anyOf [schema, {"type": "null"}]`, without its null branch.
    private static @NotNull JsonNode theNonNullBranch(final @NotNull JsonNode schema) {
        final JsonNode anyOf = schema.get("anyOf");
        assertThat(anyOf).as("a nullable array renders as anyOf [array, null]").isNotNull();
        assertThat(anyOf.size()).isEqualTo(2);
        assertThat(types(anyOf.get(1))).containsExactly("null");
        return anyOf.get(0);
    }
}
