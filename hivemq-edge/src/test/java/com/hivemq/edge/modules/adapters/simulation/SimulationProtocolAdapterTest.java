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
package com.hivemq.edge.modules.adapters.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.writing.WritingContext;
import com.hivemq.adapter.sdk.api.writing.WritingInput;
import com.hivemq.adapter.sdk.api.writing.WritingOutput;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSampleImpl;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import com.hivemq.edge.modules.adapters.impl.polling.PollingOutputImpl;
import com.hivemq.edge.modules.adapters.impl.polling.batch.BatchPollingInputImpl;
import com.hivemq.edge.modules.adapters.simulation.config.SimulationSpecificAdapterConfig;
import com.hivemq.edge.modules.adapters.simulation.tag.RandomValueConfig;
import com.hivemq.edge.modules.adapters.simulation.tag.SimulationTag;
import com.hivemq.edge.modules.adapters.simulation.tag.SimulationTagDefinition;
import com.hivemq.edge.modules.adapters.simulation.tag.SimulationValueType;
import com.hivemq.edge.modules.adapters.simulation.tag.StaticValueConfig;
import com.hivemq.protocols.tag.TagSchemaCreationInputImpl;
import com.hivemq.protocols.tag.TagSchemaCreationOutputImpl;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;

@SuppressWarnings({"rawtypes", "unchecked"})
class SimulationProtocolAdapterTest {

    private final @NotNull ProtocolAdapterInput input = mock();
    private final @NotNull SimulationSpecificAdapterConfig protocolAdapterConfig = mock();
    private @NotNull SimulationProtocolAdapter simulationProtocolAdapter;
    private final @NotNull BatchPollingInputImpl pollingInput = new BatchPollingInputImpl();
    private final @NotNull ProtocolAdapterDataSampleImpl dataSample = new ProtocolAdapterDataSampleImpl("adapter1");
    private final @NotNull PollingOutputImpl pollingOutput = new PollingOutputImpl(dataSample, "adapter1");
    private final @NotNull TimeWaiter timeWaiter = mock();

    @BeforeEach
    void setUp() {
        when(input.getProtocolAdapterState())
                .thenReturn(new ProtocolAdapterStateImpl(mock(), "simulation", "test-simulator"));
        when(input.getConfig()).thenReturn(protocolAdapterConfig);
        when(input.getTags())
                .thenReturn(List.of(new SimulationTag("tag1", "description", new SimulationTagDefinition())));
        simulationProtocolAdapter =
                new SimulationProtocolAdapter(SimulationProtocolAdapterInformation.INSTANCE, input, timeWaiter);
    }

    private void setAdapterTags(final @NotNull List<SimulationTag> tags) {
        when(input.getTags()).thenReturn((List) tags);
        simulationProtocolAdapter =
                new SimulationProtocolAdapter(SimulationProtocolAdapterInformation.INSTANCE, input, timeWaiter);
    }

    @Test
    void test_poll_whenMinDelayIsBiggerThanMax_thenExecutionException() throws InterruptedException {
        when(protocolAdapterConfig.getMinDelay()).thenReturn(2);
        when(protocolAdapterConfig.getMaxDelay()).thenReturn(1);

        simulationProtocolAdapter.poll(pollingInput, pollingOutput);

        assertThatThrownBy(() -> pollingOutput.getOutputFuture().get()).isInstanceOf(ExecutionException.class);
        verify(timeWaiter, never()).sleep(anyInt());
    }

    @Test
    @Timeout(2)
    void test_poll_whenMinAndMaxIsTheSame_thenThreadWaitsExactlyTimeAmount()
            throws InterruptedException, ExecutionException {
        when(protocolAdapterConfig.getMinDelay()).thenReturn(1);
        when(protocolAdapterConfig.getMaxDelay()).thenReturn(1);

        simulationProtocolAdapter.poll(pollingInput, pollingOutput);
        pollingOutput.getOutputFuture().get();

        verify(timeWaiter, times(1)).sleep(eq(1));
    }

    @Test
    @Timeout(2)
    void test_poll_whenMaxBiggerMin_thenThreadWaitsBetweem() throws InterruptedException, ExecutionException {
        when(protocolAdapterConfig.getMinDelay()).thenReturn(1);
        when(protocolAdapterConfig.getMaxDelay()).thenReturn(3);

        simulationProtocolAdapter.poll(pollingInput, pollingOutput);
        pollingOutput.getOutputFuture().get();

        final ArgumentCaptor<Integer> argumentCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(timeWaiter, times(1)).sleep(argumentCaptor.capture());
        final Integer sleepTimeMillis = argumentCaptor.getValue();
        assertTrue(sleepTimeMillis >= 1);
        assertTrue(sleepTimeMillis <= 3);
    }

    @Test
    @Timeout(2)
    void test_poll_whenMaxAndMinAreZero_thenDoNotSleep() throws InterruptedException, ExecutionException {
        when(protocolAdapterConfig.getMinDelay()).thenReturn(0);
        when(protocolAdapterConfig.getMaxDelay()).thenReturn(0);

        simulationProtocolAdapter.poll(pollingInput, pollingOutput);
        pollingOutput.getOutputFuture().get();

        verify(timeWaiter, never()).sleep(anyInt());
    }

    @Test
    @Timeout(2)
    void test_poll_legacyDefaultDefinition_emitsDouble() throws ExecutionException, InterruptedException {
        when(protocolAdapterConfig.getMinValue()).thenReturn(0);
        when(protocolAdapterConfig.getMaxValue()).thenReturn(1000);

        simulationProtocolAdapter.poll(pollingInput, pollingOutput);
        pollingOutput.getOutputFuture().get();

        final Object value = firstValue();
        assertThat(value).isInstanceOf(Double.class);
        assertThat((Double) value).isBetween(0.0d, 1000.0d);
    }

    @Test
    @Timeout(2)
    void test_poll_randomNumberInt_emitsIntegerWithinRange() throws ExecutionException, InterruptedException {
        setAdapterTags(List.of(new SimulationTag(
                "random-int",
                "desc",
                new SimulationTagDefinition(new RandomValueConfig(SimulationValueType.INT, 5.0, 15.0), null))));

        simulationProtocolAdapter.poll(pollingInput, pollingOutput);
        pollingOutput.getOutputFuture().get();

        final Object value = firstValue();
        assertThat(value).isInstanceOf(Integer.class);
        assertThat((Integer) value).isBetween(5, 15);
    }

    @Test
    @Timeout(2)
    void test_poll_randomNumberLong_emitsLongWithinRange() throws ExecutionException, InterruptedException {
        setAdapterTags(List.of(new SimulationTag(
                "random-long",
                "desc",
                new SimulationTagDefinition(new RandomValueConfig(SimulationValueType.LONG, 100.0, 1000.0), null))));

        simulationProtocolAdapter.poll(pollingInput, pollingOutput);
        pollingOutput.getOutputFuture().get();

        final Object value = firstValue();
        assertThat(value).isInstanceOf(Long.class);
        assertThat((Long) value).isBetween(100L, 999L);
    }

    @Test
    @Timeout(2)
    void test_poll_randomNumberDouble_emitsDoubleWithinRange() throws ExecutionException, InterruptedException {
        setAdapterTags(List.of(new SimulationTag(
                "random-double",
                "desc",
                new SimulationTagDefinition(new RandomValueConfig(SimulationValueType.DOUBLE, 0.25, 0.75), null))));

        simulationProtocolAdapter.poll(pollingInput, pollingOutput);
        pollingOutput.getOutputFuture().get();

        final Object value = firstValue();
        assertThat(value).isInstanceOf(Double.class);
        assertThat((Double) value).isBetween(0.25d, 0.75d);
    }

    @Test
    @Timeout(2)
    void test_poll_staticValueInt_emitsParsedInteger() throws ExecutionException, InterruptedException {
        setAdapterTags(List.of(new SimulationTag(
                "static-int",
                "desc",
                new SimulationTagDefinition(null, new StaticValueConfig(SimulationValueType.INT, "42")))));

        simulationProtocolAdapter.poll(pollingInput, pollingOutput);
        pollingOutput.getOutputFuture().get();

        assertThat(firstValue()).isEqualTo(42);
    }

    @Test
    @Timeout(2)
    void test_poll_staticValueLong_emitsParsedLong() throws ExecutionException, InterruptedException {
        setAdapterTags(List.of(new SimulationTag(
                "static-long",
                "desc",
                new SimulationTagDefinition(null, new StaticValueConfig(SimulationValueType.LONG, "1234567890123")))));

        simulationProtocolAdapter.poll(pollingInput, pollingOutput);
        pollingOutput.getOutputFuture().get();

        assertThat(firstValue()).isEqualTo(1234567890123L);
    }

    @Test
    @Timeout(2)
    void test_poll_staticValueDouble_emitsParsedDouble() throws ExecutionException, InterruptedException {
        setAdapterTags(List.of(new SimulationTag(
                "static-double",
                "desc",
                new SimulationTagDefinition(null, new StaticValueConfig(SimulationValueType.DOUBLE, "3.14")))));

        simulationProtocolAdapter.poll(pollingInput, pollingOutput);
        pollingOutput.getOutputFuture().get();

        assertThat(firstValue()).isEqualTo(3.14d);
    }

    @Test
    @Timeout(2)
    void test_poll_staticValueString_emitsParsedString() throws ExecutionException, InterruptedException {
        setAdapterTags(List.of(new SimulationTag(
                "static-string",
                "desc",
                new SimulationTagDefinition(null, new StaticValueConfig(SimulationValueType.STRING, "hello")))));

        simulationProtocolAdapter.poll(pollingInput, pollingOutput);
        pollingOutput.getOutputFuture().get();

        assertThat(firstValue()).isEqualTo("hello");
    }

    @Test
    @Timeout(2)
    void test_createTagSchema_legacy_producesDoubleScalarReadOnly() throws Exception {
        when(protocolAdapterConfig.getMinValue()).thenReturn(0);
        when(protocolAdapterConfig.getMaxValue()).thenReturn(1000);

        final ObjectNode schema = resolveSchema("tag1");
        assertValueScalarType(schema, "DOUBLE");
        assertValueWritable(schema, false);
    }

    @Test
    @Timeout(2)
    void test_createTagSchema_randomNumberInt_producesLongScalarReadOnly() throws Exception {
        setAdapterTags(List.of(new SimulationTag(
                "random-int",
                "desc",
                new SimulationTagDefinition(new RandomValueConfig(SimulationValueType.INT, 0.0, 100.0), null))));

        final ObjectNode schema = resolveSchema("random-int");
        assertValueScalarType(schema, "LONG");
        assertValueWritable(schema, false);
    }

    @Test
    @Timeout(2)
    void test_createTagSchema_staticValueString_producesStringScalarWritable() throws Exception {
        setAdapterTags(List.of(new SimulationTag(
                "static-string",
                "desc",
                new SimulationTagDefinition(null, new StaticValueConfig(SimulationValueType.STRING, "hello")))));

        final ObjectNode schema = resolveSchema("static-string");
        assertValueScalarType(schema, "STRING");
        assertValueWritable(schema, true);
    }

    @Test
    @Timeout(2)
    void test_start_seedsCurrentStaticValueFromConfig() throws Exception {
        final SimulationTag tag = staticTag("static-int", SimulationValueType.INT, "42");
        setAdapterTags(List.of(tag));
        startAdapter();

        assertThat(pollFreshAndGetFirstValue()).isEqualTo(42);
    }

    @Test
    @Timeout(2)
    void test_write_staticInt_updatesValueOnNextPoll() throws Exception {
        final SimulationTag tag = staticTag("static-int", SimulationValueType.INT, "10");
        setAdapterTags(List.of(tag));
        startAdapter();

        final WritingOutput wo = write("static-int", IntNode.valueOf(99));
        verify(wo).finish();

        assertThat(pollFreshAndGetFirstValue()).isEqualTo(99);
    }

    @Test
    @Timeout(2)
    void test_write_staticLong_updatesValueOnNextPoll() throws Exception {
        final SimulationTag tag = staticTag("static-long", SimulationValueType.LONG, "10");
        setAdapterTags(List.of(tag));
        startAdapter();

        final WritingOutput wo = write("static-long", LongNode.valueOf(1_234_567_890_123L));
        verify(wo).finish();

        assertThat(pollFreshAndGetFirstValue()).isEqualTo(1_234_567_890_123L);
    }

    @Test
    @Timeout(2)
    void test_write_staticDouble_updatesValueOnNextPoll() throws Exception {
        final SimulationTag tag = staticTag("static-double", SimulationValueType.DOUBLE, "1.1");
        setAdapterTags(List.of(tag));
        startAdapter();

        final WritingOutput wo = write("static-double", DoubleNode.valueOf(3.14d));
        verify(wo).finish();

        assertThat(pollFreshAndGetFirstValue()).isEqualTo(3.14d);
    }

    @Test
    @Timeout(2)
    void test_write_staticString_updatesValueOnNextPoll() throws Exception {
        final SimulationTag tag = staticTag("static-str", SimulationValueType.STRING, "before");
        setAdapterTags(List.of(tag));
        startAdapter();

        final WritingOutput wo = write("static-str", TextNode.valueOf("after"));
        verify(wo).finish();

        assertThat(pollFreshAndGetFirstValue()).isEqualTo("after");
    }

    @Test
    @Timeout(2)
    void test_write_doesNotMutateOriginalStaticValueConfig() throws Exception {
        final SimulationTag tag = staticTag("static-int", SimulationValueType.INT, "10");
        setAdapterTags(List.of(tag));
        startAdapter();

        write("static-int", IntNode.valueOf(99));

        assertThat(tag.getDefinition().getStaticValue().getParsedValue()).isEqualTo(10);
        assertThat(tag.getDefinition().getStaticValue().getValue()).isEqualTo("10");
    }

    @Test
    @Timeout(2)
    void test_write_randomTag_fails() {
        final SimulationTag tag = new SimulationTag(
                "random-int",
                "desc",
                new SimulationTagDefinition(new RandomValueConfig(SimulationValueType.INT, 0.0, 10.0), null));
        setAdapterTags(List.of(tag));
        startAdapter();

        final WritingOutput wo = write("random-int", IntNode.valueOf(42));

        final ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
        verify(wo).fail(msg.capture());
        assertThat(msg.getValue()).contains("not writable");
    }

    @Test
    @Timeout(2)
    void test_write_legacyTag_fails() {
        final SimulationTag tag = new SimulationTag("legacy", "desc", new SimulationTagDefinition());
        setAdapterTags(List.of(tag));
        startAdapter();

        final WritingOutput wo = write("legacy", DoubleNode.valueOf(1.0));

        final ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
        verify(wo).fail(msg.capture());
        assertThat(msg.getValue()).contains("not writable");
    }

    @Test
    @Timeout(2)
    void test_write_unknownTag_fails() {
        setAdapterTags(List.of(staticTag("static-int", SimulationValueType.INT, "1")));
        startAdapter();

        final WritingOutput wo = write("does-not-exist", IntNode.valueOf(0));

        final ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
        verify(wo).fail(msg.capture());
        assertThat(msg.getValue()).contains("does-not-exist");
    }

    @Test
    @Timeout(2)
    void test_write_unparseablePayload_fails() {
        setAdapterTags(List.of(staticTag("static-int", SimulationValueType.INT, "1")));
        startAdapter();

        final WritingOutput wo = write("static-int", TextNode.valueOf("not-a-number"));

        verify(wo).fail(any(Throwable.class), anyString());
    }

    @Test
    @Timeout(2)
    void test_stop_clearsCurrentStaticValuesAndRestartReverts() throws Exception {
        final SimulationTag tag = staticTag("static-int", SimulationValueType.INT, "7");
        setAdapterTags(List.of(tag));
        startAdapter();
        write("static-int", IntNode.valueOf(99));
        assertThat(pollFreshAndGetFirstValue()).isEqualTo(99);

        stopAdapter();
        startAdapter();

        assertThat(pollFreshAndGetFirstValue()).isEqualTo(7);
    }

    @Test
    void test_getMqttPayloadClass_returnsSimulationWritingPayload() {
        assertThat(simulationProtocolAdapter.getMqttPayloadClass()).isEqualTo(SimulationWritingPayload.class);
    }

    @Test
    @Timeout(2)
    void test_createTagSchema_unknownTag_fails() {
        final TagSchemaCreationOutputImpl output = new TagSchemaCreationOutputImpl();
        simulationProtocolAdapter.createTagSchema(new TagSchemaCreationInputImpl("does-not-exist"), output);

        assertThatThrownBy(() -> output.getFuture().get()).isInstanceOf(ExecutionException.class);
        assertThat(output.getStatus()).isEqualTo(TagSchemaCreationOutputImpl.Status.UNSPECIFIED_FAILURE);
        assertThat(output.getMessage()).contains("does-not-exist");
    }

    private static @NotNull SimulationTag staticTag(
            final @NotNull String name, final @NotNull SimulationValueType valueType, final @NotNull String value) {
        return new SimulationTag(
                name, "desc", new SimulationTagDefinition(null, new StaticValueConfig(valueType, value)));
    }

    private void startAdapter() {
        simulationProtocolAdapter.start(mock(), mock());
    }

    private void stopAdapter() {
        simulationProtocolAdapter.stop(mock(), mock());
    }

    private @NotNull WritingOutput write(
            final @NotNull String tagName, final @NotNull com.fasterxml.jackson.databind.JsonNode value) {
        final WritingContext ctx = mock();
        when(ctx.getTagName()).thenReturn(tagName);
        final WritingInput wi = mock();
        when(wi.getWritingContext()).thenReturn(ctx);
        when(wi.getWritingPayload()).thenReturn(new SimulationWritingPayload(value));
        final WritingOutput wo = mock();
        simulationProtocolAdapter.write(wi, wo);
        return wo;
    }

    private @NotNull Object pollFreshAndGetFirstValue() throws ExecutionException, InterruptedException {
        final ProtocolAdapterDataSampleImpl sample = new ProtocolAdapterDataSampleImpl("adapter1");
        final PollingOutputImpl out = new PollingOutputImpl(sample, "adapter1");
        simulationProtocolAdapter.poll(pollingInput, out);
        out.getOutputFuture().get();
        final Map<String, List<DataPoint>> dps = sample.getDataPoints();
        assertThat(dps).isNotEmpty();
        final List<DataPoint> points = dps.values().iterator().next();
        assertThat(points).isNotEmpty();
        final Object raw = points.get(0).getTagValue();
        if (raw instanceof com.fasterxml.jackson.databind.node.IntNode n) return n.intValue();
        if (raw instanceof com.fasterxml.jackson.databind.node.LongNode n) return n.longValue();
        if (raw instanceof com.fasterxml.jackson.databind.node.DoubleNode n) return n.doubleValue();
        if (raw instanceof com.fasterxml.jackson.databind.node.TextNode n) return n.textValue();
        return raw;
    }

    private @NotNull Object firstValue() {
        final Map<String, List<DataPoint>> dataPoints = dataSample.getDataPoints();
        assertThat(dataPoints).isNotEmpty();
        final List<DataPoint> points = dataPoints.values().iterator().next();
        assertThat(points).isNotEmpty();
        // The new DataPointListBuilder wraps values in Jackson JsonNodes (IntNode, LongNode, etc.).
        // Unwrap here so per-test assertions can stay focused on the emitted Java type.
        final Object raw = points.get(0).getTagValue();
        if (raw instanceof com.fasterxml.jackson.databind.node.IntNode n) return n.intValue();
        if (raw instanceof com.fasterxml.jackson.databind.node.LongNode n) return n.longValue();
        if (raw instanceof com.fasterxml.jackson.databind.node.DoubleNode n) return n.doubleValue();
        if (raw instanceof com.fasterxml.jackson.databind.node.TextNode n) return n.textValue();
        return raw;
    }

    private @NotNull ObjectNode resolveSchema(final @NotNull String tagName) throws Exception {
        final TagSchemaCreationOutputImpl output = new TagSchemaCreationOutputImpl();
        simulationProtocolAdapter.createTagSchema(new TagSchemaCreationInputImpl(tagName), output);
        return output.getFuture().get();
    }

    private static void assertValueScalarType(final @NotNull ObjectNode schema, final @NotNull String scalarType) {
        final var valueNode = schema.path("properties").path("value");
        assertThat(valueNode.isMissingNode()).as("value property present").isFalse();
        final var jsonType = valueNode.path("type").asText();
        switch (scalarType) {
            case "DOUBLE" -> assertThat(jsonType).isEqualTo("number");
            case "LONG" -> assertThat(jsonType).isEqualTo("integer");
            case "STRING" -> assertThat(jsonType).isEqualTo("string");
            default -> throw new IllegalArgumentException("unsupported scalarType " + scalarType);
        }
    }

    private static void assertValueWritable(final @NotNull ObjectNode schema, final boolean expected) {
        // SchemaJsonRepresentation emits writable=false as readOnly=true in the JSON Schema;
        // writable=true omits readOnly entirely (or sets it to false).
        final var valueNode = schema.path("properties").path("value");
        assertThat(valueNode.isMissingNode()).as("value property present").isFalse();
        final boolean readOnly = valueNode.path("readOnly").asBoolean(false);
        assertThat(readOnly).as("readOnly flag on value schema").isEqualTo(!expected);
    }
}
