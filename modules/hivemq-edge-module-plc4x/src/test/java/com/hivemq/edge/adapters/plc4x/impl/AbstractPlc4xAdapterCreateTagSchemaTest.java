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
package com.hivemq.edge.adapters.plc4x.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.schema.ScalarSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.schema.SchemaBuilder;
import com.hivemq.adapter.sdk.api.schema.SchemaJsonRepresentation;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationInput;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationOutput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.edge.adapters.plc4x.config.Plc4xDataType;
import com.hivemq.edge.adapters.plc4x.config.Plc4xDataType.DATA_TYPE;
import com.hivemq.edge.adapters.plc4x.config.tag.Plc4xTag;
import com.hivemq.edge.adapters.plc4x.config.tag.Plc4xTagDefinition;
import com.hivemq.edge.adapters.plc4x.types.siemens.S7ProtocolAdapter;
import com.hivemq.edge.adapters.plc4x.types.siemens.S7ProtocolAdapterInformation;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class AbstractPlc4xAdapterCreateTagSchemaTest {

    private static final String TAG_NAME = "tag1";

    // ── Exhaustive coverage of the data-type → ScalarType mapping ──────────────

    @ParameterizedTest
    @EnumSource(
            value = DATA_TYPE.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = {"NULL"})
    void test_applyDataTypeToBuilder_everyNonNullTypeProducesAScalarSchema(final DATA_TYPE type) {
        final var builder = new SchemaBuilder();
        AbstractPlc4xAdapter.applyDataTypeToBuilder(builder, type);
        final Schema schema = builder.build();

        assertThat(schema).as("Schema for %s", type).isInstanceOfAny(ScalarSchema.class);
    }

    // ── Spot-check mapping correctness for every category ─────────────────────

    @Test
    void test_boolMapsToBoolean() {
        assertType(DATA_TYPE.BOOL, ScalarType.BOOLEAN);
    }

    @Test
    void test_signedIntsMapToLongWithBounds() {
        assertType(DATA_TYPE.SINT, ScalarType.LONG, -128L, 127L);
        assertType(DATA_TYPE.INT, ScalarType.LONG, -32_768L, 32_767L);
        assertType(DATA_TYPE.DINT, ScalarType.LONG, -2_147_483_648L, 2_147_483_647L);
        assertType(DATA_TYPE.LINT, ScalarType.LONG, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    @Test
    void test_unsignedIntsMapToULong() {
        assertType(DATA_TYPE.USINT, ScalarType.ULONG, 0L, 255L);
        assertType(DATA_TYPE.UINT, ScalarType.ULONG, 0L, 65_535L);
        assertType(DATA_TYPE.UDINT, ScalarType.ULONG, 0L, 4_294_967_295L);
    }

    @Test
    void test_floatsMapToDouble() {
        assertType(DATA_TYPE.REAL, ScalarType.DOUBLE, -3.4028235e38d, 3.4028235e38d);
        assertType(DATA_TYPE.LREAL, ScalarType.DOUBLE, -1.7976931348623157e308d, 1.7976931348623157e308d);
    }

    @Test
    void test_stringsMapToString() {
        assertType(DATA_TYPE.CHAR, ScalarType.STRING);
        assertType(DATA_TYPE.WCHAR, ScalarType.STRING);
        assertType(DATA_TYPE.STRING, ScalarType.STRING);
        assertType(DATA_TYPE.WSTRING, ScalarType.STRING);
    }

    @Test
    void test_durationsMapToDuration() {
        assertType(DATA_TYPE.TIME, ScalarType.DURATION);
        assertType(DATA_TYPE.LTIME, ScalarType.DURATION);
    }

    @Test
    void test_datesMapToLocalDate() {
        assertType(DATA_TYPE.DATE, ScalarType.LOCAL_DATE);
        assertType(DATA_TYPE.LDATE, ScalarType.LOCAL_DATE);
    }

    @Test
    void test_timesOfDayMapToLocalTime() {
        assertType(DATA_TYPE.TIME_OF_DAY, ScalarType.LOCAL_TIME);
        assertType(DATA_TYPE.LTIME_OF_DAY, ScalarType.LOCAL_TIME);
    }

    @Test
    void test_dateTimesMapToLocalDateTime() {
        assertType(DATA_TYPE.DATE_AND_TIME, ScalarType.LOCAL_DATE_TIME);
        assertType(DATA_TYPE.LDATE_AND_TIME, ScalarType.LOCAL_DATE_TIME);
        assertType(DATA_TYPE.DATE_AND_LTIME, ScalarType.LOCAL_DATE_TIME);
    }

    @Test
    void test_rawByteArrayMapsToBinary() {
        assertType(DATA_TYPE.RAW_BYTE_ARRAY, ScalarType.BINARY);
    }

    // ── createTagSchema control-flow ──────────────────────────────────────────

    @Test
    void test_createTagSchema_happyPath_producesScalarSchema() {
        final var adapter = newAdapter(List.of(tag(TAG_NAME, DATA_TYPE.INT)));
        final var output = new CapturingOutput();

        adapter.createTagSchema(input(TAG_NAME), output);

        assertThat(output.failed).isFalse();
        assertThat(output.finishedSchema).isNotNull();
        final Schema valueSchema = output.finishedSchema.valueSchema();
        assertThat(valueSchema).isInstanceOf(ScalarSchema.class);
        assertThat(((ScalarSchema) valueSchema).type()).isEqualTo(ScalarType.LONG);
    }

    @Test
    void test_createTagSchema_nullDataType_fails() {
        final var adapter = newAdapter(List.of(tag(TAG_NAME, DATA_TYPE.NULL)));
        final var output = new CapturingOutput();

        adapter.createTagSchema(input(TAG_NAME), output);

        assertThat(output.failed).isTrue();
        assertThat(output.failMessage).contains("NULL data type");
    }

    @Test
    void test_createTagSchema_unknownTag_fails() {
        final var adapter = newAdapter(List.of(tag(TAG_NAME, DATA_TYPE.INT)));
        final var output = new CapturingOutput();

        adapter.createTagSchema(input("someOtherTag"), output);

        assertThat(output.failed).isTrue();
        assertThat(output.failMessage).contains("Unable to find tag definition");
    }

    @Test
    void test_createTagSchema_jsonHasExpectedFormat_forTemporal() {
        final var adapter = newAdapter(List.of(tag(TAG_NAME, DATA_TYPE.DATE_AND_TIME)));
        final var output = new CapturingOutput();

        adapter.createTagSchema(input(TAG_NAME), output);

        assertThat(output.failed).isFalse();
        final var json = SchemaJsonRepresentation.INSTANCE.toJsonSchema(output.finishedSchema.valueSchema());
        assertThat(json.get("type").asText()).isEqualTo("string");
        assertThat(json.get("format").asText()).isEqualTo("local-date-time");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static void assertType(final DATA_TYPE dt, final ScalarType expected) {
        final var builder = new SchemaBuilder();
        AbstractPlc4xAdapter.applyDataTypeToBuilder(builder, dt);
        final var scalar = (ScalarSchema) builder.build();
        assertThat(scalar.type()).as("ScalarType for %s", dt).isEqualTo(expected);
    }

    private static void assertType(
            final DATA_TYPE dt, final ScalarType expected, final Number expectedMin, final Number expectedMax) {
        final var builder = new SchemaBuilder();
        AbstractPlc4xAdapter.applyDataTypeToBuilder(builder, dt);
        final var scalar = (ScalarSchema) builder.build();
        assertThat(scalar.type()).as("ScalarType for %s", dt).isEqualTo(expected);
        assertThat(scalar.minimum()).as("minimum for %s", dt).isEqualTo(expectedMin);
        assertThat(scalar.maximum()).as("maximum for %s", dt).isEqualTo(expectedMax);
    }

    private static @NotNull Plc4xTag tag(final String name, final Plc4xDataType.DATA_TYPE type) {
        return new Plc4xTag(name, null, new Plc4xTagDefinition("addr", type));
    }

    private static @NotNull TagSchemaCreationInput input(final String tagName) {
        return () -> tagName;
    }

    @SuppressWarnings("unchecked")
    private static @NotNull S7ProtocolAdapter newAdapter(final List<Plc4xTag> tags) {
        final ProtocolAdapterInput<com.hivemq.edge.adapters.plc4x.types.siemens.config.S7SpecificAdapterConfig> input =
                mock(ProtocolAdapterInput.class);
        when(input.getTags()).thenReturn((List) tags);
        when(input.moduleServices()).thenReturn(mock(ModuleServices.class));
        return new S7ProtocolAdapter(S7ProtocolAdapterInformation.INSTANCE, input);
    }

    private static final class CapturingOutput implements TagSchemaCreationOutput {
        @Nullable
        DataPointSchema finishedSchema;

        boolean failed;

        @Nullable
        String failMessage;

        @Override
        public void finish(final @NotNull DataPointSchema schema) {
            this.finishedSchema = schema;
        }

        @Override
        public void finish(final @NotNull JsonNode schema) {}

        @Override
        public void notSupported() {}

        @Override
        public void adapterNotStarted() {}

        @Override
        public void fail(final @NotNull Throwable t, final @Nullable String errorMessage) {
            this.failed = true;
            this.failMessage = errorMessage;
        }

        @Override
        public void fail(final @NotNull String errorMessage) {
            this.failed = true;
            this.failMessage = errorMessage;
        }

        @Override
        public void tagNotFound(final @NotNull String errorMessage) {
            this.failed = true;
            this.failMessage = errorMessage;
        }
    }
}
