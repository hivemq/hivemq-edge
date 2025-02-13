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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.polling.PollingInput;
import com.hivemq.adapter.sdk.api.polling.PollingOutput;
import com.hivemq.adapter.sdk.api.polling.PollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationInput;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationOutput;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.modules.adapters.simulation.config.SimulationSpecificAdapterConfig;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.STATELESS;

public class SimulationProtocolAdapter implements PollingProtocolAdapter {

    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull SimulationSpecificAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull TimeWaiter timeWaiter;
    private static final @NotNull Random RANDOM = new Random();
    private final @NotNull String adapterId;
    private final @NotNull ObjectMapper objectMapper = new ObjectMapper();

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
    public void poll(
            final @NotNull PollingInput pollingInput, final @NotNull PollingOutput pollingOutput) {

            // TODO
            new Thread(() -> {
                for (final PollingContext pollingContext : pollingInput.getPollingContexts()) {
                    final int minDelay = adapterConfig.getMinDelay();
                    final int maxDelay = adapterConfig.getMaxDelay();

                    if (minDelay > maxDelay) {
                        pollingOutput.fail(String.format(
                                "The configured min '%d' delay was bigger than the max delay '%d'. Simulator Adapter will not publish a value.",
                                minDelay,
                                maxDelay));
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

                    pollingOutput.addDataPoint(pollingContext.getTagName(),
                            ThreadLocalRandom.current()
                                    .nextDouble(Math.min(adapterConfig.getMinValue(), adapterConfig.getMaxValue()),
                                            Math.max(adapterConfig.getMinValue() + 1, adapterConfig.getMaxValue())));
                }
                pollingOutput.finish();
            }).start();

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
        output.finish(objectMapper.createObjectNode());
    }
}
