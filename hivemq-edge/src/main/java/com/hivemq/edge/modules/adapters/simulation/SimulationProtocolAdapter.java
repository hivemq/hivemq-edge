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

import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
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
import com.hivemq.edge.modules.adapters.simulation.config.SimulationSpecificAdapterConfig;
import com.hivemq.edge.modules.adapters.simulation.tag.RandomValueConfig;
import com.hivemq.edge.modules.adapters.simulation.tag.SimulationTag;
import com.hivemq.edge.modules.adapters.simulation.tag.SimulationTagDefinition;
import com.hivemq.edge.modules.adapters.simulation.tag.SimulationValueType;
import com.hivemq.edge.modules.adapters.simulation.tag.StaticValueConfig;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.jetbrains.annotations.NotNull;

public class SimulationProtocolAdapter implements BatchPollingProtocolAdapter {

    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull SimulationSpecificAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull TimeWaiter timeWaiter;
    private static final @NotNull Random RANDOM = new Random();
    private final @NotNull String adapterId;
    private final @NotNull List<SimulationTag> tags;

    public SimulationProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<SimulationSpecificAdapterConfig> protocolAdapterInput,
            final @NotNull TimeWaiter timeWaiter) {
        this.adapterId = protocolAdapterInput.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.adapterConfig = protocolAdapterInput.getConfig();
        this.protocolAdapterState = protocolAdapterInput.getProtocolAdapterState();
        this.timeWaiter = timeWaiter;
        this.protocolAdapterState.setConnectionStatus(STATELESS);
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
        output.startedSuccessfully();
    }

    @Override
    public void stop(final @NotNull ProtocolAdapterStopInput input, final @NotNull ProtocolAdapterStopOutput output) {
        output.stoppedSuccessfully();
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    @Override
    public void poll(final @NotNull BatchPollingInput pollingInput, final @NotNull BatchPollingOutput pollingOutput) {
        new Thread(() -> {
                    for (final SimulationTag tag : tags) {
                        final int minDelay = adapterConfig.getMinDelay();
                        final int maxDelay = adapterConfig.getMaxDelay();
                        if (minDelay > maxDelay) {
                            pollingOutput.fail(String.format(
                                    "The configured min '%d' delay was bigger than the max delay '%d'. Simulator Adapter will not publish a value.",
                                    minDelay, maxDelay));
                        } else if (minDelay == maxDelay && maxDelay > 0) {
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
                        pollingOutput.addDataPoint(tag.getName(), generateValue(tag.getDefinition()));
                    }
                    pollingOutput.finish();
                })
                .start();
    }

    private @NotNull Object generateValue(final @NotNull SimulationTagDefinition def) {
        return switch (def.getType()) {
            case LEGACY_RANDOM_DOUBLE -> ThreadLocalRandom.current()
                    .nextDouble(
                            Math.min(adapterConfig.getMinValue(), adapterConfig.getMaxValue()),
                            Math.max(adapterConfig.getMinValue() + 1, adapterConfig.getMaxValue()));
            case RANDOM_NUMBER -> {
                final RandomValueConfig rv = Objects.requireNonNull(def.getRandomValue());
                yield switch (rv.getValueType()) {
                    case INT -> ThreadLocalRandom.current().nextInt((int) rv.getMinValue(), (int) rv.getMaxValue());
                    case LONG -> ThreadLocalRandom.current()
                            .nextLong((long) rv.getMinValue(), (long) rv.getMaxValue());
                    case DOUBLE -> ThreadLocalRandom.current().nextDouble(rv.getMinValue(), rv.getMaxValue());
                    case STRING -> throw new IllegalStateException(
                            "randomValue with STRING valueType is rejected at construction");
                };
            }
            case STATIC_VALUE -> Objects.requireNonNull(def.getStaticValue()).getParsedValue();
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
            case LEGACY_RANDOM_DOUBLE -> builder.scalar(ScalarType.DOUBLE)
                    .minimum((double) Math.min(adapterConfig.getMinValue(), adapterConfig.getMaxValue()))
                    .maximum((double) Math.max(adapterConfig.getMinValue() + 1, adapterConfig.getMaxValue()))
                    .readable();
            case RANDOM_NUMBER -> {
                final RandomValueConfig rv = Objects.requireNonNull(def.getRandomValue());
                applyNumericScalar(builder, rv.getValueType(), rv.getMinValue(), rv.getMaxValue());
            }
            case STATIC_VALUE -> {
                final StaticValueConfig sv = Objects.requireNonNull(def.getStaticValue());
                applyScalarForValueType(builder, sv.getValueType()).readable();
            }
        }
        return builder.build();
    }

    private static @NotNull SchemaBuilder applyNumericScalar(
            final @NotNull SchemaBuilder builder,
            final @NotNull SimulationValueType valueType,
            final double min,
            final double max) {
        switch (valueType) {
            case INT -> builder.scalar(ScalarType.LONG)
                    .minimum((long) min)
                    .maximum((long) max)
                    .readable();
            case LONG -> builder.scalar(ScalarType.LONG)
                    .minimum((long) min)
                    .maximum((long) max)
                    .readable();
            case DOUBLE -> builder.scalar(ScalarType.DOUBLE)
                    .minimum(min)
                    .maximum(max)
                    .readable();
            case STRING -> throw new IllegalStateException("RANDOM_NUMBER cannot have STRING valueType");
        }
        return builder;
    }

    private static @NotNull SchemaBuilder applyScalarForValueType(
            final @NotNull SchemaBuilder builder, final @NotNull SimulationValueType valueType) {
        return switch (valueType) {
            case INT -> builder.scalar(ScalarType.LONG)
                    .minimum((long) Integer.MIN_VALUE)
                    .maximum((long) Integer.MAX_VALUE);
            case LONG -> builder.scalar(ScalarType.LONG);
            case DOUBLE -> builder.scalar(ScalarType.DOUBLE);
            case STRING -> builder.scalar(ScalarType.STRING);
        };
    }
}
