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
        return MethodSignature.ofObject(name, NODE, NODE, List.of(inputs), List.of());
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
    void noLengthIsStated_whateverTheDeviceDeclared() {
        // This used to emit minContains and maxContains, both set to the declared length, and a test pinned
        // that. Three things were wrong with it:
        //
        //   - The keywords do nothing. They count elements matching a `contains` keyword, and none is emitted,
        //     so a schema saying minContains 5 accepted [], [1] and [1,2,3,4,5,6] alike -- measured against the
        //     validator Edge uses.
        //   - Equal minimum and maximum misstates the declaration. OPC 10000-3 section 5.6.2: ArrayDimensions
        //     "specifies the maximum supported length of each dimension", so 5 does not mean exactly five.
        //   - A union over several methods had no length to agree on, so the first browsed supplied the number
        //     and reversing the browse order changed the schema.
        //
        // Saying nothing needs maxItems, which the shared SDK's array builder cannot emit, and the SDK is
        // unchanged this release. So the length rests with the device -- where it already rested, since the
        // keywords never constrained anything.
        final JsonNode declared = requestProperties(
                        method("AddArray", argument("Array input1", NodeIds.Int32, 1, new UInteger[] {uint(5)})))
                .get("Array input1");

        assertThat(declared.get("type").textValue()).isEqualTo("array");
        assertThat(declared.has("minContains"))
                .withFailMessage("a length was stated: %s", declared)
                .isFalse();
        assertThat(declared.has("maxContains")).isFalse();

        // Zero already meant "any length" and emitted nothing; it still does.
        final JsonNode unknown = requestProperties(
                        method("Take", argument("values", NodeIds.Int32, 1, new UInteger[] {uint(0)})))
                .get("values");

        assertThat(unknown.get("type").textValue()).isEqualTo("array");
        assertThat(unknown.has("minContains")).isFalse();
    }

    @Test
    void twoMethodsDeclaringDifferentLengths_giveTheSameSchemaInEitherBrowseOrder() {
        // What the length removal fixes beyond the inert keywords. With a length emitted, the union took it
        // from whichever method the browse reported first, so an identical object produced a different schema
        // depending on the order the server happened to answer in.
        final MethodSignature three = method("A", argument("v", NodeIds.Int32, 1, new UInteger[] {uint(3)}));
        final MethodSignature five = method("B", argument("v", NodeIds.Int32, 1, new UInteger[] {uint(5)}));

        final JsonNode oneOrder = requestProperties(three, five);
        final JsonNode otherOrder = requestProperties(five, three);

        assertThat(oneOrder).isEqualTo(otherOrder);
        // And both still describe an array of the element type, rather than giving up on the argument.
        assertThat(oneOrder.get("v").get("type").textValue()).isEqualTo("array");
    }

    @Test
    void absentDimensions_stillGiveAnArrayOfTheRightDepth() {
        // The rank decides the nesting; ArrayDimensions only supplies lengths. A server may omit them.
        final JsonNode prop = requestProperties(method("Take", argument("values", NodeIds.String, 1, null)))
                .get("values");

        assertThat(prop.get("type").textValue()).isEqualTo("array");
        // A String has a null value of its own, so the elements are nullable (EDG-1138); the array is not.
        assertThat(prop.get("items").get("type").toString()).isEqualTo("[\"string\",\"null\"]");
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
        final MethodSignature grid = MethodSignature.ofObject(
                "Grid", NODE, NODE, List.of(), List.of(argument("out", NodeIds.Int32, 1, new UInteger[] {uint(5)})));

        final JsonNode schema =
                SchemaJsonRepresentation.INSTANCE.toJsonSchemaDocument(MethodSchemas.responseSchema(List.of(grid)));
        // The response root is nullable, so the properties sit in the object branch of the anyOf.
        final JsonNode prop = schema.get("anyOf").get(0).get("properties").get("out");

        // An output is nullable -- it is published as null when this client cannot decode it -- and a nullable
        // array renders as an `anyOf` of the array and `null`, so the array itself is the first branch. A
        // nullable *scalar* instead states a two-entry `type`; the renderer differs by kind.
        final JsonNode array = prop.get("anyOf").get(0);
        assertThat(prop.get("anyOf").get(1).get("type").textValue()).isEqualTo("null");
        assertThat(array.get("type").textValue()).isEqualTo("array");
        assertThat(array.get("items").get("type").textValue()).isEqualTo("integer");
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
        final MethodSignature withOutput = MethodSignature.ofObject(
                "Square", NODE, NODE, List.of(), List.of(argument("output1", NodeIds.Int32, -1, null)));

        final JsonNode schema = SchemaJsonRepresentation.INSTANCE.toJsonSchemaDocument(
                MethodSchemas.responseSchema(List.of(withOutput)));
        final JsonNode prop = schema.get("anyOf").get(0).get("properties").get("output1");

        assertThat(prop.get("readOnly").booleanValue()).isTrue();
    }
}
