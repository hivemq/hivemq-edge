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
package com.hivemq.edge.adapters.etherip_cip_odva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.schema.ObjectSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationInput;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationOutput;
import com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType;
import com.hivemq.edge.adapters.etherip_cip_odva.config.EipSpecificAdapterConfig;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTag;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTagDefinition;
import com.hivemq.edge.adapters.etherip_cip_odva.tag.TagSchemaMapper;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class EthernetIPCipOdvaPollingProtocolAdapterCreateTagSchemaTest {

    private static final String TAG_NAME = "tag1";
    private static final String ADDRESS = "@4/100/1";

    // ── Exhaustive scalar-type coverage ─────────────────────────────────────

    @ParameterizedTest
    @EnumSource(
            value = CipDataType.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = {"COMPOSITE"})
    void test_buildScalarSchema_everyScalarTypeProducesScalarSchema(final CipDataType type) {
        final Schema schema = TagSchemaMapper.buildScalarSchema(type, true, true);
        assertThat(schema).as("Schema for %s", type).isInstanceOf(ScalarSchema.class);
    }

    // ── Spot checks: scalar type mapping ────────────────────────────────────

    @Test
    void test_boolMapsToBoolean() {
        assertType(CipDataType.BOOL, ScalarType.BOOLEAN);
    }

    @Test
    void test_signedIntsMapToLongWithBounds() {
        assertType(CipDataType.SINT, ScalarType.LONG, -128L, 127L);
        assertType(CipDataType.INT, ScalarType.LONG, -32_768L, 32_767L);
        assertType(CipDataType.DINT, ScalarType.LONG, -2_147_483_648L, 2_147_483_647L);
        assertType(CipDataType.LINT, ScalarType.LONG, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    @Test
    void test_unsignedIntsMapToULongWithBounds() {
        assertType(CipDataType.USINT, ScalarType.ULONG, 0L, 255L);
        assertType(CipDataType.UINT, ScalarType.ULONG, 0L, 65_535L);
        assertType(CipDataType.UDINT, ScalarType.ULONG, 0L, 4_294_967_295L);
    }

    @Test
    void test_floatsMapToDouble() {
        assertType(CipDataType.REAL, ScalarType.DOUBLE, -3.4028235e38d, 3.4028235e38d);
        assertType(CipDataType.LREAL, ScalarType.DOUBLE, -1.7976931348623157e308d, 1.7976931348623157e308d);
    }

    @Test
    void test_stringsMapToString() {
        assertType(CipDataType.SSTRING, ScalarType.STRING);
        assertType(CipDataType.STRING, ScalarType.STRING);
    }

    @Test
    void test_scalarSchemaReflectsReadWriteFlags() {
        final ScalarSchema scalar = (ScalarSchema) TagSchemaMapper.buildScalarSchema(CipDataType.INT, true, true);
        assertThat(scalar.readable()).isTrue();
        assertThat(scalar.writable()).isTrue();
    }

    @Test
    void test_scalarSchemaReadOnlyHasWritableFalse() {
        final ScalarSchema scalar = (ScalarSchema) TagSchemaMapper.buildScalarSchema(CipDataType.INT, true, false);
        assertThat(scalar.readable()).isTrue();
        assertThat(scalar.writable()).isFalse();
    }

    @Test
    void test_scalarSchemaWriteOnlyHasReadableFalse() {
        final ScalarSchema scalar = (ScalarSchema) TagSchemaMapper.buildScalarSchema(CipDataType.INT, false, true);
        assertThat(scalar.readable()).isFalse();
        assertThat(scalar.writable()).isTrue();
    }

    @Test
    void test_buildScalarSchema_forCompositeType_throws() {
        assertThatThrownBy(() -> TagSchemaMapper.buildScalarSchema(CipDataType.COMPOSITE, true, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── createTagSchema control-flow ────────────────────────────────────────

    @Test
    void test_createTagSchema_scalar_happyPath() {
        final var adapter = newAdapter(List.of(scalarTag(TAG_NAME, ADDRESS, CipDataType.INT)));
        final var output = new CapturingOutput();

        adapter.createTagSchema(input(TAG_NAME), output);

        assertThat(output.failed).isFalse();
        assertThat(output.finishedSchema).isNotNull();
        final Schema valueSchema = output.finishedSchema.valueSchema();
        assertThat(valueSchema).isInstanceOf(ScalarSchema.class);
        assertThat(((ScalarSchema) valueSchema).type()).isEqualTo(ScalarType.LONG);
    }

    @Test
    void test_createTagSchema_unknownTag_fails() {
        final var adapter = newAdapter(List.of(scalarTag(TAG_NAME, ADDRESS, CipDataType.INT)));
        final var output = new CapturingOutput();

        adapter.createTagSchema(input("someOtherTag"), output);

        assertThat(output.failed).isTrue();
        assertThat(output.failMessage).contains("Unable to find tag definition");
    }

    @Test
    void test_createTagSchema_composite_happyPath_producesObjectSchema() {
        final var adapter = newAdapter(List.of(
                scalarTag("speed", ADDRESS, CipDataType.INT),
                scalarTag("direction", ADDRESS, CipDataType.BOOL),
                scalarTag("label", ADDRESS, CipDataType.STRING),
                compositeTag("Motor", ADDRESS)));
        final var output = new CapturingOutput();

        adapter.createTagSchema(input("Motor"), output);

        assertThat(output.failed).isFalse();
        assertThat(output.finishedSchema).isNotNull();
        final Schema valueSchema = output.finishedSchema.valueSchema();
        assertThat(valueSchema).isInstanceOf(ObjectSchema.class);
        final ObjectSchema obj = (ObjectSchema) valueSchema;
        assertThat(obj.readable()).isTrue();
        assertThat(obj.writable()).isFalse();
        assertThat(obj.properties()).containsOnlyKeys("speed", "direction", "label");
        assertThat(((ScalarSchema) obj.properties().get("speed")).type()).isEqualTo(ScalarType.LONG);
        assertThat(((ScalarSchema) obj.properties().get("direction")).type()).isEqualTo(ScalarType.BOOLEAN);
        assertThat(((ScalarSchema) obj.properties().get("label")).type()).isEqualTo(ScalarType.STRING);
    }

    @Test
    void test_createTagSchema_compositePropertiesAreReadOnly() {
        final var adapter =
                newAdapter(List.of(scalarTag("speed", ADDRESS, CipDataType.INT), compositeTag("Motor", ADDRESS)));
        final var output = new CapturingOutput();

        adapter.createTagSchema(input("Motor"), output);

        assertThat(output.failed).isFalse();
        final ObjectSchema obj = (ObjectSchema) output.finishedSchema.valueSchema();
        final ScalarSchema speed = (ScalarSchema) obj.properties().get("speed");
        assertThat(speed.readable()).isTrue();
        assertThat(speed.writable()).isFalse();
    }

    @Test
    void test_createTagSchema_compositeWithNoSiblings_fails() {
        final var adapter = newAdapter(List.of(compositeTag("Motor", ADDRESS)));
        final var output = new CapturingOutput();

        adapter.createTagSchema(input("Motor"), output);

        assertThat(output.failed).isTrue();
        assertThat(output.failMessage).contains("Motor");
        assertThat(output.failMessage).contains(ADDRESS);
        assertThat(output.failMessage).contains("no scalar siblings");
    }

    @Test
    void test_createTagSchema_compositeOnlyGroupsSiblingsAtSameAddress() {
        final var adapter = newAdapter(List.of(
                scalarTag("sameAddress", ADDRESS, CipDataType.INT),
                scalarTag("otherAddress", "@4/100/2", CipDataType.INT),
                compositeTag("Motor", ADDRESS)));
        final var output = new CapturingOutput();

        adapter.createTagSchema(input("Motor"), output);

        final ObjectSchema obj = (ObjectSchema) output.finishedSchema.valueSchema();
        assertThat(obj.properties()).containsOnlyKeys("sameAddress");
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static void assertType(final CipDataType dt, final ScalarType expected) {
        final ScalarSchema scalar = (ScalarSchema) TagSchemaMapper.buildScalarSchema(dt, true, true);
        assertThat(scalar.type()).as("ScalarType for %s", dt).isEqualTo(expected);
    }

    private static void assertType(
            final CipDataType dt, final ScalarType expected, final Number expectedMin, final Number expectedMax) {
        final ScalarSchema scalar = (ScalarSchema) TagSchemaMapper.buildScalarSchema(dt, true, true);
        assertThat(scalar.type()).as("ScalarType for %s", dt).isEqualTo(expected);
        assertThat(scalar.minimum()).as("minimum for %s", dt).isEqualTo(expectedMin);
        assertThat(scalar.maximum()).as("maximum for %s", dt).isEqualTo(expectedMax);
    }

    private static @NotNull CipTag scalarTag(final String name, final String address, final CipDataType type) {
        return new CipTag(name, null, new CipTagDefinition(address, 1, type, 0.0, 0, 0, null));
    }

    private static @NotNull CipTag compositeTag(final String name, final String address) {
        return new CipTag(name, null, new CipTagDefinition(address, 1, CipDataType.COMPOSITE, 0.0, 0, 0, null));
    }

    private static @NotNull TagSchemaCreationInput input(final String tagName) {
        return () -> tagName;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static @NotNull EthernetIPCipOdvaPollingProtocolAdapter newAdapter(final List<CipTag> tags) {
        final ProtocolAdapterInput<EipSpecificAdapterConfig> input = mock(ProtocolAdapterInput.class);
        when(input.getAdapterId()).thenReturn("test-adapter");
        when(input.getTags()).thenReturn((List) tags);
        return new EthernetIPCipOdvaPollingProtocolAdapter(EthernetIPCipOdvaProtocolAdapterInformation.INSTANCE, input);
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
