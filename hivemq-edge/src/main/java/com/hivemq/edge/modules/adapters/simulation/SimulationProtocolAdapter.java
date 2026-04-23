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

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.STATELESS;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.datapoint.DataPointBuilder;
import com.hivemq.adapter.sdk.api.datapoint.DataPointListBuilder;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingInput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.schema.SchemaBuilder;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationInput;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationOutput;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.writing.WritingInput;
import com.hivemq.adapter.sdk.api.writing.WritingOutput;
import com.hivemq.adapter.sdk.api.writing.WritingPayload;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.edge.modules.adapters.simulation.config.SimulationSpecificAdapterConfig;
import com.hivemq.edge.modules.adapters.simulation.tag.RandomValueConfig;
import com.hivemq.edge.modules.adapters.simulation.tag.SimulationTag;
import com.hivemq.edge.modules.adapters.simulation.tag.SimulationTagDefinition;
import com.hivemq.edge.modules.adapters.simulation.tag.SimulationValueType;
import com.hivemq.edge.modules.adapters.simulation.tag.StaticValueConfig;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.jetbrains.annotations.NotNull;

public class SimulationProtocolAdapter implements BatchPollingProtocolAdapter, WritingProtocolAdapter {

    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull SimulationSpecificAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull TimeWaiter timeWaiter;
    private static final @NotNull Random RANDOM = new Random();
    private final @NotNull String adapterId;
    private final @NotNull List<SimulationTag> tags;
    /**
     * Holds the live value for every STATIC_VALUE tag. Seeded from each tag's configured
     * {@link StaticValueConfig#getParsedValue()} at {@link #start(ProtocolAdapterStartInput, ProtocolAdapterStartOutput)}
     * and updated via {@link #write(WritingInput, WritingOutput)}. The configured value on the
     * tag itself is intentionally never mutated.
     */
    private final @NotNull ConcurrentHashMap<String, Object> currentStaticValues = new ConcurrentHashMap<>();

    public SimulationProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<SimulationSpecificAdapterConfig> protocolAdapterInput,
            final @NotNull TimeWaiter timeWaiter) {
        this.adapterId = protocolAdapterInput.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.adapterConfig = protocolAdapterInput.getConfig();
        this.protocolAdapterState = protocolAdapterInput.getProtocolAdapterState();
        protocolAdapterState.setConnectionStatus(STATELESS);
        this.timeWaiter = timeWaiter;
        this.tags = protocolAdapterInput.getTags().stream()
                .map(tag -> (SimulationTag) tag)
                .toList();
    }

    @Override
    public @NotNull String getId() {
        return adapterId;
    }

    @Override
    public void start(
            final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
        for (final SimulationTag tag : tags) {
            final StaticValueConfig sv = tag.getDefinition().getStaticValue();
            if (sv != null) {
                currentStaticValues.put(tag.getName(), sv.getParsedValue());
            }
        }
        output.startedSuccessfully();
    }

    @Override
    public void stop(final @NotNull ProtocolAdapterStopInput input, final @NotNull ProtocolAdapterStopOutput output) {
        currentStaticValues.clear();
        output.stoppedSuccessfully();
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    @Override
    public void poll(final @NotNull BatchPollingInput pollingInput, final @NotNull BatchPollingOutput pollingOutput) {
        final DataPointListBuilder publisher = pollingOutput.dataPointListPublisher();
        for (final SimulationTag tag : tags) {
            final int minDelay = adapterConfig.getMinDelay();
            final int maxDelay = adapterConfig.getMaxDelay();
            if (minDelay > maxDelay) {
                pollingOutput.fail(String.format(
                        "The configured min '%d' delay was bigger than the max delay '%d'. Simulator Adapter will not publish a value.",
                        minDelay, maxDelay));
                return;
            }
            if (minDelay == maxDelay && maxDelay > 0) {
                try {
                    timeWaiter.sleep(minDelay);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    pollingOutput.fail("Thread was interrupted");
                    return;
                }
            } else if (maxDelay > 0) {
                final int sleepMS = minDelay + RANDOM.nextInt(maxDelay - minDelay);
                try {
                    timeWaiter.sleep(sleepMS);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    pollingOutput.fail("Thread was interrupted");
                    return;
                }
            }
            final DataPointBuilder<DataPointListBuilder> b = publisher.addDataPoint(tag);
            writeValue(b, tag);
            b.endDataPoint();
        }
        publisher.publish();
    }

    private void writeValue(final @NotNull DataPointBuilder<?> b, final @NotNull SimulationTag tag) {
        final SimulationTagDefinition def = tag.getDefinition();
        switch (def.getType()) {
            case LEGACY_RANDOM_DOUBLE ->
                b.value(ThreadLocalRandom.current()
                        .nextDouble(
                                Math.min(adapterConfig.getMinValue(), adapterConfig.getMaxValue()),
                                Math.max(adapterConfig.getMinValue() + 1, adapterConfig.getMaxValue())));
            case RANDOM_NUMBER -> {
                final RandomValueConfig rv = Objects.requireNonNull(def.getRandomValue());
                switch (rv.getValueType()) {
                    case INT ->
                        b.value(ThreadLocalRandom.current()
                                .nextInt((int) rv.getMinValue(), (int) rv.getMaxValue() + 1));
                    case LONG ->
                        b.value(ThreadLocalRandom.current()
                                .nextLong((long) rv.getMinValue(), (long) rv.getMaxValue() + 1));
                    case DOUBLE ->
                        b.value(ThreadLocalRandom.current().nextDouble(rv.getMinValue(), rv.getMaxValue()));
                    case STRING ->
                        throw new IllegalStateException(
                                "randomValue with STRING valueType is rejected at construction");
                }
            }
            case STATIC_VALUE -> {
                final StaticValueConfig sv = Objects.requireNonNull(def.getStaticValue());
                // Emit the current runtime value (seeded at start() from sv.getParsedValue(),
                // updated via write()). Fall back to the configured value if the adapter has not
                // been started yet.
                final Object current = currentStaticValues.getOrDefault(tag.getName(), sv.getParsedValue());
                switch (current) {
                    case final Integer i -> b.value(i);
                    case final Long l -> b.value(l);
                    case final Double d -> b.value(d);
                    case final String s -> b.value(s);
                    default ->
                        throw new IllegalStateException("Unexpected current static value type: " + current.getClass());
                }
            }
        }
    }

    @Override
    public void write(final @NotNull WritingInput input, final @NotNull WritingOutput output) {
        final String tagName = input.getWritingContext().getTagName();
        final SimulationTag tag = tags.stream()
                .filter(t -> tagName.equals(t.getName()))
                .findFirst()
                .orElse(null);
        if (tag == null) {
            output.fail("Tag '" + tagName + "' not found.");
            return;
        }
        final StaticValueConfig sv = tag.getDefinition().getStaticValue();
        if (sv == null) {
            output.fail("Tag '" + tagName + "' is not writable (only static-value tags accept writes).");
            return;
        }
        final JsonNode payloadValue = ((SimulationWritingPayload) input.getWritingPayload()).value();
        final Object parsed;
        try {
            parsed = coerce(payloadValue, sv.getValueType());
        } catch (final IllegalArgumentException e) {
            output.fail(e, "Unable to coerce payload for tag '" + tagName + "'");
            return;
        }
        currentStaticValues.put(tagName, parsed);
        output.finish();
    }

    @Override
    public @NotNull Class<? extends WritingPayload> getMqttPayloadClass() {
        return SimulationWritingPayload.class;
    }

    private static @NotNull Object coerce(final @NotNull JsonNode node, final @NotNull SimulationValueType valueType) {
        return switch (valueType) {
            case INT -> {
                if (!node.canConvertToInt()) {
                    throw new IllegalArgumentException("value '" + node + "' is not a valid INT");
                }
                yield node.intValue();
            }
            case LONG -> {
                if (!node.canConvertToLong()) {
                    throw new IllegalArgumentException("value '" + node + "' is not a valid LONG");
                }
                yield node.longValue();
            }
            case DOUBLE -> {
                if (!node.isNumber()) {
                    throw new IllegalArgumentException("value '" + node + "' is not a valid DOUBLE");
                }
                yield node.doubleValue();
            }
            case STRING -> {
                if (!node.isTextual()) {
                    throw new IllegalArgumentException("value '" + node + "' is not a valid STRING");
                }
                yield node.textValue();
            }
        };
    }

    @Override
    public int getPollingIntervalMillis() {
        return adapterConfig.getSimulationToMqttConfig().getPollingIntervalMillis();
    }

    @Override
    public int getMaxPollingErrorsBeforeRemoval() {
        return adapterConfig.getSimulationToMqttConfig().getMaxPollingErrorsBeforeRemoval();
    }

    @Override
    public void createTagSchema(
            final @NotNull TagSchemaCreationInput input, final @NotNull TagSchemaCreationOutput output) {
        tags.stream()
                .filter(tag -> input.getTagName().equals(tag.getName()))
                .findFirst()
                .map(tag -> buildSchemaForTag(tag.getDefinition()))
                .ifPresentOrElse(
                        schema -> output.finish(new TagSchemaCreationOutput.DataPointSchema(schema, null, null)),
                        () -> output.fail("Unable to find tag definition for tag " + input.getTagName()
                                + ", cannot create schema"));
    }

    private @NotNull Schema buildSchemaForTag(final @NotNull SimulationTagDefinition def) {
        final var builder = new SchemaBuilder();
        switch (def.getType()) {
            case LEGACY_RANDOM_DOUBLE ->
                builder.scalar(ScalarType.DOUBLE)
                        .minimum((double) Math.min(adapterConfig.getMinValue(), adapterConfig.getMaxValue()))
                        .maximum((double) Math.max(adapterConfig.getMinValue() + 1, adapterConfig.getMaxValue()))
                        .readable();
            case RANDOM_NUMBER -> {
                final RandomValueConfig rv = Objects.requireNonNull(def.getRandomValue());
                applyNumericScalar(builder, rv.getValueType(), rv.getMinValue(), rv.getMaxValue());
            }
            case STATIC_VALUE -> {
                final StaticValueConfig sv = Objects.requireNonNull(def.getStaticValue());
                // Static tags are the only kind that accept southbound writes — downstream consumers
                // can update the emitted value. Random/legacy tags are read-only by nature.
                applyScalarForValueType(builder, sv.getValueType()).readable().writable();
            }
        }
        return builder.build();
    }

    private static void applyNumericScalar(
            final @NotNull SchemaBuilder builder,
            final @NotNull SimulationValueType valueType,
            final double min,
            final double max) {
        switch (valueType) {
            case INT ->
                builder.scalar(ScalarType.LONG)
                        .minimum((int) min)
                        .maximum((int) max)
                        .readable();
            case LONG ->
                builder.scalar(ScalarType.LONG)
                        .minimum((long) min)
                        .maximum((long) max)
                        .readable();
            case DOUBLE ->
                builder.scalar(ScalarType.DOUBLE).minimum(min).maximum(max).readable();
            case STRING -> throw new IllegalStateException("RANDOM_NUMBER cannot have STRING valueType");
        }
    }

    private static @NotNull SchemaBuilder applyScalarForValueType(
            final @NotNull SchemaBuilder builder, final @NotNull SimulationValueType valueType) {
        return switch (valueType) {
            case INT ->
                builder.scalar(ScalarType.LONG).minimum(Integer.MIN_VALUE).maximum(Integer.MAX_VALUE);
            case LONG -> builder.scalar(ScalarType.LONG);
            case DOUBLE -> builder.scalar(ScalarType.DOUBLE);
            case STRING -> builder.scalar(ScalarType.STRING);
        };
    }
}
