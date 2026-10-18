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
package com.hivemq.edge.adapters.opcua.northbound;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.datapoint.DataPointWithMetadata;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import org.eclipse.milo.opcua.stack.core.encoding.DefaultEncodingContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Matrix;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.junit.jupiter.api.Test;

/// Publishing a multi-dimensional value.
///
/// Milo delivers one as a `Matrix` rather than as a nested Java array, and no branch of this converter named that
/// type -- so such a value fell through to the catch-all at the end of each dispatch and was published as Java
/// debug text: `"Matrix{dataType=Int32, dataTypeId=i=6, dimensions=[2, 2], flatArray=[1, 2, 3, 4]}"`, a string
/// where the schema promises an array of arrays.
///
/// Unwrapping it exposed a second fault one level down. The dispatch that writes a value **into** an array
/// appended an inner array's elements to the array being written rather than starting a nested one, so a 2x2
/// matrix would have come out as `[1,2,3,4]`. That branch was unreachable while nothing ever handed the converter
/// a nested array, which is why it went unnoticed; the two fixes have to land together.
class OpcUaToJsonConverterMatrixTest {

    /// Runs one value through the converter and returns the emitted `value` node.
    private static JsonNode convert(final Object value) {
        final DataValue dataValue = new DataValue(new Variant(value), StatusCode.GOOD, null);
        final var builder = (DataPointWithMetadata.DataPointBuilderImpl<Void>) DataPointWithMetadata.<Void>builder(
                new OpcuaTag("test-tag", "", new OpcuaTagDefinition("ns=2;i=1001")), b -> null);

        OpcUaToJsonConverter.convertPayload(() -> DefaultEncodingContext.INSTANCE, dataValue, builder);

        return builder.build("test-adapter").getTagValue();
    }

    @Test
    void aMatrix_isPublishedAsAnArrayOfArrays() {
        final JsonNode value = convert(new Matrix(new Integer[][] {{1, 2}, {3, 4}}));

        assertThat(value.isArray()).isTrue();
        assertThat(value).hasSize(2);
        assertThat(value.get(0).isArray()).isTrue();
        assertThat(value.get(0).get(0).intValue()).isEqualTo(1);
        assertThat(value.get(0).get(1).intValue()).isEqualTo(2);
        assertThat(value.get(1).get(0).intValue()).isEqualTo(3);
        assertThat(value.get(1).get(1).intValue()).isEqualTo(4);
    }

    @Test
    void aMatrixIsNotFlattened() {
        // Stated separately because flattening is the failure that looks most like success: the values are all
        // present and in order, so only the shape reveals it -- and a consumer reading rows gets one long row.
        final JsonNode value = convert(new Matrix(new Integer[][] {{1, 2}, {3, 4}}));

        // Two elements rather than four, and each one an array rather than a number. Asserting only the absence
        // of numbers would pass for the text form too, which is a different failure.
        assertThat(value.isArray()).isTrue();
        assertThat(value).hasSize(2);
        assertThat(value).allSatisfy(element -> assertThat(element.isArray()).isTrue());
    }

    @Test
    void aMatrixIsNotPublishedAsText() {
        // What it did before: Matrix.toString(), which even contains the values -- so a consumer looking for
        // numbers found text that happened to have numbers in it.
        final JsonNode value = convert(new Matrix(new Integer[][] {{1, 2}, {3, 4}}));

        assertThat(value.isTextual()).isFalse();
    }

    @Test
    void aThreeDimensionalMatrix_keepsEveryLevel() {
        final JsonNode value = convert(new Matrix(new Integer[][][] {{{1, 2}, {3, 4}}, {{5, 6}, {7, 8}}}));

        assertThat(value.get(0).get(1).get(0).intValue()).isEqualTo(3);
        assertThat(value.get(1).get(1).get(1).intValue()).isEqualTo(8);
    }

    @Test
    void aOneDimensionalArray_isUnchanged() {
        // The counterpart: nesting an inner array must not start wrapping a plain array's elements.
        final JsonNode value = convert(new Integer[] {1, 2, 3});

        assertThat(value.get(0).intValue()).isEqualTo(1);
        assertThat(value.get(2).intValue()).isEqualTo(3);
    }
}
