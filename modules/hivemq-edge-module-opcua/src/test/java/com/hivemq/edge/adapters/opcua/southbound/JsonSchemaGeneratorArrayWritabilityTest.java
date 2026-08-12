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
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationOutput;
import java.util.List;
import org.eclipse.milo.opcua.stack.core.OpcUaDataType;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Writability of array <em>elements</em>.
 * <p>
 * An array-valued node is written by supplying its elements. Elements that render {@code readOnly} therefore
 * describe a shape nothing can be written to — the degenerate "array of read-only items admits only
 * {@code []}" case the SDK Javadoc warns about. Before this was fixed the elements fell through to
 * {@code SchemaBuilder}'s {@code writable = false} default, so <em>every</em> writable array node on every
 * OPC-UA server advertised read-only elements; observed live against a Prosys server on
 * {@code AccessLevelCurrentReadWrite}, whose southbound document carried {@code "readOnly": true} on its items.
 * <p>
 * The rule under test: <b>when a tag is writable, so are the elements of any array carrying its data</b> — at
 * every nesting depth.
 */
class JsonSchemaGeneratorArrayWritabilityTest {

    private static final UInteger @NotNull [] ONE_DIMENSION = {UInteger.valueOf(0)};
    private static final UInteger @NotNull [] THREE_DIMENSIONS = {
        UInteger.valueOf(0), UInteger.valueOf(0), UInteger.valueOf(0)
    };

    private static @NotNull JsonSchemaGenerator.FieldInformation arrayField(
            final UInteger @NotNull [] dimensions, final boolean readable, final boolean writable) {
        return new JsonSchemaGenerator.FieldInformation(
                "value", null, OpcUaDataType.Int32, null, false, dimensions, false, readable, writable, List.of());
    }

    private static @NotNull JsonNode render(final @NotNull JsonSchemaGenerator.FieldInformation info) {
        final TagSchemaCreationOutput.DataPointSchema dps = JsonSchemaGenerator.buildSchema(info);
        return SchemaJsonRepresentation.INSTANCE.toJsonSchemaDocument(dps.valueSchema());
    }

    @Test
    void test_writableArray_hasWritableItems() {
        final JsonNode schema = render(arrayField(ONE_DIMENSION, true, true));

        assertThat(schema.get("type").asText()).isEqualTo("array");
        assertThat(schema.has("readOnly")).as("the array itself is writable").isFalse();
        assertThat(schema.get("items").has("readOnly"))
                .as("...and so are its elements — otherwise only [] could ever be written")
                .isFalse();
    }

    @Test
    void test_readOnlyArray_keepsReadOnlyItems() {
        final JsonNode schema = render(arrayField(ONE_DIMENSION, true, false));

        assertThat(schema.get("readOnly").asBoolean()).isTrue();
        assertThat(schema.get("items").get("readOnly").asBoolean())
                .as("a read-only node's elements stay read-only — the fix must not make everything writable")
                .isTrue();
    }

    @Test
    void test_writeOnlyArray_propagatesWriteOnlyToItems() {
        final JsonNode schema = render(arrayField(ONE_DIMENSION, false, true));

        assertThat(schema.get("writeOnly").asBoolean()).isTrue();
        assertThat(schema.get("items").get("writeOnly").asBoolean()).isTrue();
    }

    @Test
    void test_multiDimensionalWritableArray_isWritableAllTheWayDown() {
        final JsonNode schema = render(arrayField(THREE_DIMENSIONS, true, true));

        JsonNode level = schema;
        for (int depth = 0; depth < 3; depth++) {
            assertThat(level.get("type").asText())
                    .as("depth %s is an array", depth)
                    .isEqualTo("array");
            assertThat(level.has("readOnly"))
                    .as("depth %s must be writable", depth)
                    .isFalse();
            level = level.get("items");
        }
        assertThat(level.get("type").asText()).isEqualTo("integer");
        assertThat(level.has("readOnly"))
                .as("the innermost scalar element is the thing actually written")
                .isFalse();
    }

    @Test
    void test_multiDimensionalReadOnlyArray_isReadOnlyAllTheWayDown() {
        final JsonNode schema = render(arrayField(THREE_DIMENSIONS, true, false));

        JsonNode level = schema;
        for (int depth = 0; depth < 3; depth++) {
            assertThat(level.get("readOnly").asBoolean())
                    .as("depth %s must stay read-only", depth)
                    .isTrue();
            level = level.get("items");
        }
        assertThat(level.get("readOnly").asBoolean()).isTrue();
    }

    /** The access-flag combinations an OPC-UA AccessLevel can produce, end to end. */
    @ParameterizedTest(name = "readable={0} writable={1}")
    @CsvSource({"true,true", "true,false", "false,true", "false,false"})
    void test_itemsAlwaysMatchTheNodesOwnAccessFlags(final boolean readable, final boolean writable) {
        final JsonNode schema = render(arrayField(ONE_DIMENSION, readable, writable));
        final JsonNode items = schema.get("items");

        assertThat(items.has("readOnly"))
                .as("items readOnly must mirror the node")
                .isEqualTo(schema.has("readOnly"));
        assertThat(items.has("writeOnly"))
                .as("items writeOnly must mirror the node")
                .isEqualTo(schema.has("writeOnly"));
    }

    @Test
    void test_scalarNode_isUnaffected() {
        // Guards the blast radius: only the array path changed.
        final JsonNode schema = render(new JsonSchemaGenerator.FieldInformation(
                "value", null, OpcUaDataType.Int32, null, false, null, false, true, true, List.of()));

        assertThat(schema.get("type").asText()).isEqualTo("integer");
        assertThat(schema.has("readOnly")).isFalse();
        assertThat(schema.has("items")).isFalse();
    }
}
