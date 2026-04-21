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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
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
        assertThat((Integer) value).isBetween(5, 14);
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
                new SimulationTagDefinition(
                        null, new StaticValueConfig(SimulationValueType.LONG, "1234567890123")))));

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
    void test_createTagSchema_legacy_producesDoubleScalar() throws Exception {
        when(protocolAdapterConfig.getMinValue()).thenReturn(0);
        when(protocolAdapterConfig.getMaxValue()).thenReturn(1000);

        final ObjectNode schema = resolveSchema("tag1");
        assertValueScalarType(schema, "DOUBLE");
    }

    @Test
    @Timeout(2)
    void test_createTagSchema_randomNumberInt_producesLongScalar() throws Exception {
        setAdapterTags(List.of(new SimulationTag(
                "random-int",
                "desc",
                new SimulationTagDefinition(new RandomValueConfig(SimulationValueType.INT, 0.0, 100.0), null))));

        final ObjectNode schema = resolveSchema("random-int");
        assertValueScalarType(schema, "LONG");
    }

    @Test
    @Timeout(2)
    void test_createTagSchema_staticValueString_producesStringScalar() throws Exception {
        setAdapterTags(List.of(new SimulationTag(
                "static-string",
                "desc",
                new SimulationTagDefinition(null, new StaticValueConfig(SimulationValueType.STRING, "hello")))));

        final ObjectNode schema = resolveSchema("static-string");
        assertValueScalarType(schema, "STRING");
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

    private @NotNull Object firstValue() {
        final Map<String, List<DataPoint>> dataPoints = dataSample.getDataPoints();
        assertThat(dataPoints).isNotEmpty();
        final List<DataPoint> points = dataPoints.values().iterator().next();
        assertThat(points).isNotEmpty();
        return points.get(0).getTagValue();
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
}
