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
        return MethodSignature.ofObject("AddArray", NODE, NODE, List.of(inputs), List.of());
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
    void aMatrixWithRowsOfUnequalLength_isRefusedNamingTheRow() throws Exception {
        // The decisive one, and the reason rectangularity is checked while length is not. Matrix takes each
        // dimension's length from the first row and flattens against it, and its own consistency check is an
        // assert, disabled at runtime. So a short row made it read past that row's end: [[1],[2,3]] reached the
        // device as [[1],[2]] -- the 2 relocated out of row 0 into row 1, which is worse than a dropped element
        // because nothing about the result looks truncated.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("grid", NodeIds.Int32, 2, new UInteger[] {uint(2), uint(2)})),
                        json("{\"grid\": [[1], [2, 3]]}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("grid")
                .hasMessageContaining("row 1");

        // The other direction threw ArrayIndexOutOfBoundsException from inside Matrix. That descends from
        // IndexOutOfBoundsException, not IllegalArgumentException, so it escaped the conversion's catch as itself
        // and the caller got no response at all rather than a refusal.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("grid", NodeIds.Int32, 2, new UInteger[] {uint(2), uint(2)})),
                        json("{\"grid\": [[1, 2], [3]]}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("row 1");
    }

    @Test
    void aMatrixWhoseFirstRowIsEmpty_isRefused() throws Exception {
        // Taking the length from row 0 makes an empty first row discard everything: [[],[1]] reached the device as
        // [[],[]], a matrix with no values at all.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("grid", NodeIds.Int32, 2, new UInteger[] {uint(2), uint(1)})),
                        json("{\"grid\": [[], [1]]}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("row 1");
    }

    @Test
    void aRectangularMatrixOfTheWrongSize_isStillAccepted() throws Exception {
        // Rectangularity is checked; length is not. A 2x3 supplied for an argument declaring 2x2 is well-formed and
        // reaches the device unchanged, which is where a length disagreement is answered. Without this the fix
        // would have quietly become a length check as well.
        final Variant[] arguments = MethodArguments.forCall(
                signature(argument("grid", NodeIds.Int32, 2, new UInteger[] {uint(2), uint(2)})),
                json("{\"grid\": [[1, 2, 3], [4, 5, 6]]}"));

        final Matrix matrix = (Matrix) arguments[0].value();
        assertThat(matrix.getDimensions()).containsExactly(2, 3);
    }

    @Test
    void aRaggedInnerDimension_isRefused() throws Exception {
        // Checked at every level, not only the outermost: the two outer rows agree in length, and the raggedness
        // sits one dimension in.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("cube", NodeIds.Int32, 3, new UInteger[] {uint(2), uint(2), uint(2)})),
                        json("{\"cube\": [[[1, 2], [3, 4]], [[5, 6], [7]]]}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("row 1");
    }

    @Test
    void aDisagreementBetweenBranches_isRefused() throws Exception {
        // Sam Cao's case, 2026-10-12, and the one a sibling-only comparison cannot see. The two inner lists `[1]`
        // and `[2,3]` have *different parents*, so nothing ever compared them: each was the first row of its own
        // branch and agreed with itself. Matrix then took the inner length as 1 and flattened against it, so this
        // reached the device as elements [1, 2] -- the `3` silently dropped.
        //
        // The lengths are now kept per depth rather than per parent, so the second list is measured against the
        // first whichever branch it sits in.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("cube", NodeIds.Int32, 3, new UInteger[] {uint(2), uint(1), uint(1)})),
                        json("{\"cube\": [[[1]], [[2, 3]]]}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("depth 3");
    }

    @Test
    void anAgreementAcrossBranches_isStillAccepted() throws Exception {
        // The boundary of the test above. Same two branches, same depth, lengths that agree -- so keeping the
        // lengths per depth must not refuse a value that is simply rectangular.
        final Variant[] arguments = MethodArguments.forCall(
                signature(argument("cube", NodeIds.Int32, 3, new UInteger[] {uint(2), uint(1), uint(2)})),
                json("{\"cube\": [[[1, 2]], [[3, 4]]]}"));

        final Matrix matrix = (Matrix) arguments[0].value();
        assertThat(matrix.getDimensions()).containsExactly(2, 1, 2);
    }

    @Test
    void everyDimensionBeingEmptyInEveryBranch_isAccepted() throws Exception {
        // Why the "not yet known" marker cannot be 0. An empty list has length 0, which is a real length a later
        // list must match -- so a slot holding 0 has to mean "0 was seen", not "nothing was seen yet". With 0 as
        // the marker this rectangular 2x0 value would reset its slot on every branch and never compare anything.
        final Variant[] arguments = MethodArguments.forCall(
                signature(argument("grid", NodeIds.Int32, 2, new UInteger[] {uint(2), uint(0)})),
                json("{\"grid\": [[], []]}"));

        final Matrix matrix = (Matrix) arguments[0].value();
        assertThat(matrix.getDimensions()).containsExactly(2, 0);
    }

    @Test
    void rankZero_takesTheDepthFromTheRequest() throws Exception {
        // The decisive test. OPC 10000-3 table 28: rank 0 (OneOrMoreDimensions) declares an array of one
        // dimension or more, and never a scalar -- the server has simply declined to fix the depth. Treating it
        // as a scalar, which is what `rank < 1` did, refused every legal value: an array is the only thing such
        // an argument accepts, so the method could not be invoked at all.
        //
        // The depth therefore comes from the request, which is sound because nothing earlier constrains it --
        // the generated schema says `any` for this rank, having no way to state "an array of unknown depth".
        final Variant[] oneDimension = MethodArguments.forCall(
                signature(argument("values", NodeIds.Int32, 0, null)), json("{\"values\": [1, 2, 3]}"));

        assertThat(oneDimension[0].value()).isInstanceOf(Integer[].class);
        assertThat((Integer[]) oneDimension[0].value()).containsExactly(1, 2, 3);

        final Variant[] twoDimensions = MethodArguments.forCall(
                signature(argument("values", NodeIds.Int32, 0, null)), json("{\"values\": [[1, 2], [3, 4]]}"));

        assertThat(twoDimensions[0].value()).isInstanceOf(Matrix.class);
        assertThat(((Matrix) twoDimensions[0].value()).getDimensions()).containsExactly(2, 2);
    }

    @Test
    void rankZero_refusesASingleValueNamingWhyItCannotBeOne() throws Exception {
        // The one thing rank 0 excludes. Checked here rather than left to the device so the caller is told what
        // the declaration says, which a server's bare Bad_InvalidArgument would not convey.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("values", NodeIds.Int32, 0, null)), json("{\"values\": 7}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("values")
                .hasMessageContaining("one or more dimensions");
    }

    @Test
    void rankAny_acceptsEitherAScalarOrAnArray() throws Exception {
        // Rank -2 (Any): a scalar or an array of any number of dimensions. Both halves are legal, and only the
        // scalar half used to work.
        final Variant[] scalar =
                MethodArguments.forCall(signature(argument("v", NodeIds.Int32, -2, null)), json("{\"v\": 7}"));
        assertThat(scalar[0].value()).isEqualTo(7);

        final Variant[] flat =
                MethodArguments.forCall(signature(argument("v", NodeIds.Int32, -2, null)), json("{\"v\": [1, 2]}"));
        assertThat(flat[0].value()).isInstanceOf(Integer[].class);

        final Variant[] nested = MethodArguments.forCall(
                signature(argument("v", NodeIds.Int32, -2, null)), json("{\"v\": [[1, 2], [3, 4]]}"));
        assertThat(nested[0].value()).isInstanceOf(Matrix.class);
    }

    @Test
    void rankScalarOrOneDimension_acceptsBothAndRefusesDeeper() throws Exception {
        // Rank -3 (ScalarOrOneDimension) permits exactly two shapes, so unlike rank -2 there is something to
        // refuse -- and refusing it here names the declaration rather than leaving the device to object.
        final Variant[] scalar =
                MethodArguments.forCall(signature(argument("v", NodeIds.Int32, -3, null)), json("{\"v\": 7}"));
        assertThat(scalar[0].value()).isEqualTo(7);

        final Variant[] flat =
                MethodArguments.forCall(signature(argument("v", NodeIds.Int32, -3, null)), json("{\"v\": [1, 2]}"));
        assertThat(flat[0].value()).isInstanceOf(Integer[].class);

        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("v", NodeIds.Int32, -3, null)), json("{\"v\": [[1, 2]]}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("one-dimensional");
    }

    @Test
    void rankScalar_stillRefusesAnArray() throws Exception {
        // The counterpart: rank -1 is the one negative rank that genuinely means scalar, and inferring a depth
        // from the request must not start reshaping it.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("v", NodeIds.Int32, -1, null)), json("{\"v\": [1, 2]}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("v");
    }

    @Test
    void aDeclaredRank_stillDecidesTheDepthRatherThanTheRequest() throws Exception {
        // And a positive rank keeps deciding: a flat list for a two-dimensional argument is a caller's mistake,
        // not something to reshape into one row.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("grid", NodeIds.Int32, 2, null)), json("{\"grid\": [1, 2, 3]}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("expected an array");
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
