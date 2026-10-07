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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.Matrix;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/// Encoding an array-valued argument.
///
/// The encoder decides what it is writing from the Java array's component type, so an array argument has to be
/// built as `Integer[]` rather than `Object[]`: a correctly-shaped `Object[]` is not an Int32 array and the server
/// would refuse it. These pin the component type, the nesting depth and the shape refusals.
class MethodArgumentsArrayTest {

    private static final @NotNull ObjectMapper MAPPER = new ObjectMapper();
    private static final @NotNull NodeId NODE = new NodeId(2, "Boiler1.AddArray");

    private static @NotNull Argument argument(
            final @NotNull String name,
            final @NotNull NodeId dataType,
            final int valueRank,
            final @Nullable UInteger @Nullable [] dimensions) {

        return new Argument(name, dataType, valueRank, dimensions, LocalizedText.NULL_VALUE);
    }

    private static @NotNull MethodSignature signature(final @NotNull Argument... inputs) {
        return new MethodSignature("AddArray", NODE, List.of(inputs), List.of());
    }

    private static @NotNull JsonNode json(final @NotNull String raw) throws Exception {
        return MAPPER.readTree(raw);
    }

    @Test
    void anArrayArgument_becomesATypedJavaArray() throws Exception {
        // The decisive test. An Object[] holding Integers has the right contents and the wrong type, and the
        // encoder would write it as something other than an Int32 array.
        final Variant[] arguments = MethodArguments.forCall(
                signature(argument("values", NodeIds.Int32, 1, new UInteger[] {uint(5)})),
                json("{\"values\": [1, 2, 3, 4, 5]}"));

        assertThat(arguments).hasSize(1);
        assertThat(arguments[0].value()).isInstanceOf(Integer[].class);
        assertThat((Integer[]) arguments[0].value()).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    void anEmptyArray_isAllowed() throws Exception {
        // A zero-length array is a value, not an omission. Length is the device's business, not the encoder's.
        final Variant[] arguments = MethodArguments.forCall(
                signature(argument("values", NodeIds.Int32, 1, new UInteger[] {uint(0)})), json("{\"values\": []}"));

        assertThat((Integer[]) arguments[0].value()).isEmpty();
    }

    @Test
    void elementTypesAreCheckedPerElement() throws Exception {
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("values", NodeIds.Int32, 1, null)), json("{\"values\": [1, \"two\"]}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("values")
                .hasMessageContaining("Int32[]");
    }

    @Test
    void elementRangesAreCheckedPerElement() throws Exception {
        // Int32's range matters per element for the same reason it matters for a scalar: a narrowing cast wraps
        // silently, so 2147483648 would arrive at the device as -2147483648.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("values", NodeIds.Int32, 1, null)), json("{\"values\": [1, 2147483648]}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("values");
    }

    @Test
    void aScalarSuppliedForAnArrayArgument_isRefused() throws Exception {
        // Not coerced into a one-element array: the request disagrees with the declaration, and guessing which
        // side is right would send the device something it did not ask for.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("values", NodeIds.Int32, 1, null)), json("{\"values\": 7}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("expected an array");
    }

    @Test
    void anArraySuppliedForAScalarArgument_isRefused() throws Exception {
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("value", NodeIds.Int32, -1, null)), json("{\"value\": [1, 2]}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("value");
    }

    @Test
    void twoDimensions_areWrappedInAMatrix() throws Exception {
        // Milo refuses a nested Java array inside a Variant -- "Multi-dimensional arrays must be wrapped in
        // Matrix" -- so anything past one dimension has to go in as a Matrix. Its nested form is the array built
        // from the request, and its dimensions come from that array's shape.
        final Variant[] arguments = MethodArguments.forCall(
                signature(argument("grid", NodeIds.Int32, 2, new UInteger[] {uint(2), uint(3)})),
                json("{\"grid\": [[1, 2, 3], [4, 5, 6]]}"));

        assertThat(arguments[0].value()).isInstanceOf(Matrix.class);
        final Matrix matrix = (Matrix) arguments[0].value();
        assertThat(matrix.getDimensions()).containsExactly(2, 3);

        final Integer[][] grid = (Integer[][]) matrix.nestedArrayValue();
        assertThat(grid[0]).containsExactly(1, 2, 3);
        assertThat(grid[1]).containsExactly(4, 5, 6);
    }

    @Test
    void oneDimension_isNotWrapped() throws Exception {
        // Only multi-dimensional values need the wrapper; a plain array is a Variant value in its own right, and
        // wrapping it would change what the device receives.
        final Variant[] arguments = MethodArguments.forCall(
                signature(argument("values", NodeIds.Int32, 1, null)), json("{\"values\": [1, 2]}"));

        assertThat(arguments[0].value()).isInstanceOf(Integer[].class);
    }

    @Test
    void aFlatArrayForATwoDimensionalArgument_isRefused() throws Exception {
        // The declared rank decides the depth, not how deeply the JSON happens to nest. Encoding a flat list as a
        // two-dimensional array is not possible, and silently treating it as one row would be worse.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("grid", NodeIds.Int32, 2, new UInteger[] {uint(2), uint(3)})),
                        json("{\"grid\": [1, 2, 3]}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("expected an array");
    }

    @Test
    void aStringArray_keepsItsComponentType() throws Exception {
        final Variant[] arguments = MethodArguments.forCall(
                signature(argument("names", NodeIds.String, 1, null)), json("{\"names\": [\"a\", \"b\"]}"));

        assertThat(arguments[0].value()).isInstanceOf(String[].class);
        assertThat((String[]) arguments[0].value()).containsExactly("a", "b");
    }

    @Test
    void aMixedSignatureKeepsPositions() throws Exception {
        // AddArray's real shape on the IoTech Lua simulator: an array then a scalar. Positions are what the Call
        // service goes by, so an array argument must not disturb them.
        final Variant[] arguments = MethodArguments.forCall(
                signature(
                        argument("Array input1", NodeIds.Int32, 1, new UInteger[] {uint(5)}),
                        argument("Delta input2", NodeIds.Int32, -1, null)),
                json("{\"Delta input2\": 10, \"Array input1\": [1, 2, 3, 4, 5]}"));

        assertThat(arguments).hasSize(2);
        assertThat((Integer[]) arguments[0].value()).containsExactly(1, 2, 3, 4, 5);
        assertThat(arguments[1].value()).isEqualTo(10);
    }
}
