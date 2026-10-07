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
package com.hivemq.edge.adapters.opcua.method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.adapter.sdk.api.schema.SchemaJsonRepresentation;
import java.util.List;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.OpcUaDataType;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/// How an argument's OPC UA value rank reaches the generated schema.
///
/// An argument carries a type *and* a rank, and both decide its shape: rank -1 is a scalar, a positive rank is
/// that many array dimensions (OPC 10000-3 section 5.6.2). An earlier version of the union kept only the type, so
/// an `Int32` argument with rank 1 was described as a plain integer and Edge rejected every correct array request
/// with `array found, integer expected` before it could reach the device. These pin the rank's path through.
class MethodSchemasArrayTest {

    private static final @NotNull NodeId NODE = new NodeId(2, "Boiler1.M");

    private static @NotNull Argument argument(
            final @NotNull String name,
            final @NotNull NodeId dataType,
            final int valueRank,
            final @Nullable UInteger @Nullable [] dimensions) {

        return new Argument(name, dataType, valueRank, dimensions, LocalizedText.NULL_VALUE);
    }

    private static @NotNull MethodSignature method(final @NotNull String name, final @NotNull Argument... inputs) {
        return new MethodSignature(name, NODE, List.of(inputs), List.of());
    }

    private static @NotNull JsonNode requestProperties(final @NotNull MethodSignature... methods) {
        return SchemaJsonRepresentation.INSTANCE
                .toJsonSchemaDocument(MethodSchemas.requestSchema(List.of(methods)))
                .get("properties");
    }

    @Test
    void scalarRank_staysAScalar() {
        final JsonNode prop = requestProperties(method("Square", argument("input1", NodeIds.Int32, -1, null)))
                .get("input1");

        assertThat(prop.get("type").textValue()).isEqualTo("integer");
    }

    @Test
    void rankOne_becomesAnArrayOfTheElementType() {
        // The case that was broken. AddArray on the IoTech Lua simulator declares exactly this.
        final JsonNode prop = requestProperties(
                        method("AddArray", argument("Array input1", NodeIds.Int32, 1, new UInteger[] {uint(5)})))
                .get("Array input1");

        assertThat(prop.get("type").textValue()).isEqualTo("array");
        assertThat(prop.get("items").get("type").textValue()).isEqualTo("integer");
    }

    @Test
    void aDeclaredDimensionLengthBecomesAnExactSize() {
        // ArrayDimensions of 5 means exactly five elements, not "up to five". The SDK renders the bound as
        // minContains/maxContains, which is the vocabulary the value-tag array path already uses.
        final JsonNode prop = requestProperties(
                        method("AddArray", argument("Array input1", NodeIds.Int32, 1, new UInteger[] {uint(5)})))
                .get("Array input1");

        assertThat(prop.get("minContains").intValue()).isEqualTo(5);
        assertThat(prop.get("maxContains").intValue()).isEqualTo(5);
    }

    @Test
    void aZeroDimension_constrainsNothing() {
        // Zero means "any length" in OPC UA. Stating a size of zero would admit only the empty array.
        final JsonNode prop = requestProperties(
                        method("Take", argument("values", NodeIds.Int32, 1, new UInteger[] {uint(0)})))
                .get("values");

        assertThat(prop.get("type").textValue()).isEqualTo("array");
        assertThat(prop.has("minContains")).isFalse();
        assertThat(prop.has("maxContains")).isFalse();
    }

    @Test
    void absentDimensions_stillGiveAnArrayOfTheRightDepth() {
        // The rank decides the nesting; ArrayDimensions only supplies lengths. A server may omit them.
        final JsonNode prop = requestProperties(method("Take", argument("values", NodeIds.String, 1, null)))
                .get("values");

        assertThat(prop.get("type").textValue()).isEqualTo("array");
        assertThat(prop.get("items").get("type").textValue()).isEqualTo("string");
    }

    @Test
    void rankTwo_nestsTwice() {
        final JsonNode prop = requestProperties(
                        method("Grid", argument("cells", NodeIds.Double, 2, new UInteger[] {uint(2), uint(3)})))
                .get("cells");

        assertThat(prop.get("type").textValue()).isEqualTo("array");
        assertThat(prop.get("items").get("type").textValue()).isEqualTo("array");
        assertThat(prop.get("items").get("items").get("type").textValue()).isEqualTo("number");
    }

    @Test
    void anArrayAndAScalarOfTheSameType_disagree() {
        // The decisive one for the union. Without the rank in the key these two collapsed to "Int32" and the
        // schema described the array argument as a scalar -- stating something false about one of the methods.
        final var union = MethodSchemas.unionOfInputs(List.of(
                method("Scalar", argument("data", NodeIds.Int32, -1, null)),
                method("Array", argument("data", NodeIds.Int32, 1, new UInteger[] {uint(3)}))));

        assertThat(union.get("data")).isEqualTo(MethodSchemas.ArgumentShape.UNSTATEABLE);
        assertThat(requestProperties(
                                method("Scalar", argument("data", NodeIds.Int32, -1, null)),
                                method("Array", argument("data", NodeIds.Int32, 1, new UInteger[] {uint(3)})))
                        .get("data")
                        .has("type"))
                .isFalse();
    }

    @Test
    void arraysOfDifferingLengths_stillAgree() {
        // Lengths are not part of the identity: both methods agree the argument is a one-dimensional Int32 array,
        // which is worth stating. Only the shared part is kept, so no length is asserted.
        final var union = MethodSchemas.unionOfInputs(List.of(
                method("Five", argument("data", NodeIds.Int32, 1, new UInteger[] {uint(5)})),
                method("Three", argument("data", NodeIds.Int32, 1, new UInteger[] {uint(3)}))));

        assertThat(union.get("data").isArray()).isTrue();
        assertThat(union.get("data").type()).isEqualTo(OpcUaDataType.Int32);
    }

    @Test
    void arraysOfDifferingDepth_disagree() {
        final var union = MethodSchemas.unionOfInputs(List.of(
                method("Flat", argument("data", NodeIds.Int32, 1, new UInteger[] {uint(3)})),
                method("Nested", argument("data", NodeIds.Int32, 2, new UInteger[] {uint(3), uint(3)}))));

        assertThat(union.get("data")).isEqualTo(MethodSchemas.ArgumentShape.UNSTATEABLE);
    }

    @Test
    void anArrayOfACustomStructure_keepsItsArrayness() {
        // The element type cannot be stated, but the array-ness can. Losing both would describe a shape a correct
        // request fails to match; losing only the element type leaves a schema that is incomplete, not wrong.
        final JsonNode prop = requestProperties(
                        method("Load", argument("recipes", new NodeId(3, 4001), 1, new UInteger[] {uint(0)})))
                .get("recipes");

        assertThat(prop.get("type").textValue()).isEqualTo("array");
    }

    @Test
    void outputArrays_areDescribedTheSameWay() {
        final MethodSignature grid = new MethodSignature(
                "Grid", NODE, List.of(), List.of(argument("out", NodeIds.Int32, 1, new UInteger[] {uint(5)})));

        final JsonNode schema =
                SchemaJsonRepresentation.INSTANCE.toJsonSchemaDocument(MethodSchemas.responseSchema(List.of(grid)));
        // The response root is nullable, so the properties sit in the object branch of the anyOf.
        final JsonNode prop = schema.get("anyOf").get(0).get("properties").get("out");

        assertThat(prop.get("type").textValue()).isEqualTo("array");
        assertThat(prop.get("items").get("type").textValue()).isEqualTo("integer");
    }

    @Test
    void requestArguments_areWritable() {
        // A request's arguments are exactly what a caller supplies, so marking them read-only states the opposite of
        // the truth. The builder defaults every node to not-writable, which renders readOnly: true, so the request
        // path has to say otherwise -- without it every argument of every method was advertised as read-only, and
        // the mapping editor treats a read-only field as somewhere a field mapping may not write.
        final JsonNode prop = requestProperties(method("Square", argument("input1", NodeIds.Int32, -1, null)))
                .get("input1");

        assertThat(prop.has("readOnly")).isFalse();
    }

    @Test
    void requestArrayArguments_areWritableAtEveryDepth() {
        // An array is written by supplying its elements, so read-only elements describe a shape nothing can be
        // written to -- the degenerate "array of read-only items admits only []" case.
        final JsonNode prop = requestProperties(
                        method("AddArray", argument("points", NodeIds.Int32, 1, new UInteger[] {uint(5)})))
                .get("points");

        assertThat(prop.has("readOnly")).isFalse();
        assertThat(prop.get("items").has("readOnly")).isFalse();
    }

    @Test
    void responseOutputs_stayReadOnly() {
        // The counterpart, and the reason this is a flag rather than a blanket change: a response's outputs are what
        // the device produced. Nobody writes them, and claiming they are writable would be the inverse mistake.
        final MethodSignature withOutput =
                new MethodSignature("Square", NODE, List.of(), List.of(argument("output1", NodeIds.Int32, -1, null)));

        final JsonNode schema = SchemaJsonRepresentation.INSTANCE.toJsonSchemaDocument(
                MethodSchemas.responseSchema(List.of(withOutput)));
        final JsonNode prop = schema.get("anyOf").get(0).get("properties").get("output1");

        assertThat(prop.get("readOnly").booleanValue()).isTrue();
    }
}
