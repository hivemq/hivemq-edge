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

import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.polling.PollingInput;
import com.hivemq.adapter.sdk.api.polling.PollingOutput;
import com.hivemq.adapter.sdk.api.polling.PollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.STATELESS;

public class SimulationProtocolAdapter implements PollingProtocolAdapter<SimulationPollingContext> {

    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull SimulationAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull TimeWaiter timeWaiter;
    private static final @NotNull Random RANDOM = new Random();

    public SimulationProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<SimulationAdapterConfig> protocolAdapterInput,
            final @NotNull TimeWaiter timeWaiter) {
        this.adapterInformation = adapterInformation;
        this.adapterConfig = protocolAdapterInput.getConfig();
        this.protocolAdapterState = protocolAdapterInput.getProtocolAdapterState();
        this.timeWaiter = timeWaiter;
        this.protocolAdapterState.setConnectionStatus(STATELESS);
    }

    @Override
    public @NotNull String getId() {
        return adapterConfig.getId();
    }

    @Override
    public void start(
            final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
        output.startedSuccessfully();
    }

    @Override
    public void stop(@NotNull final ProtocolAdapterStopInput input, @NotNull final ProtocolAdapterStopOutput output) {
        output.stoppedSuccessfully();
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    @Override
    public void poll(
            final @NotNull PollingInput<SimulationPollingContext> pollingInput,
            final @NotNull PollingOutput pollingOutput) {

        final int minDelay = adapterConfig.getMinDelay();
        final int maxDelay = adapterConfig.getMaxDelay();

        new Thread(() -> {
            if (minDelay > maxDelay) {
                pollingOutput.fail(String.format(
                        "The configured min '%d' delay was bigger than the max delay '%d'. Simulator Adapter will not publish a value.",
                        minDelay,
                        maxDelay));
            } else if (minDelay == maxDelay && maxDelay > 0) {
                try {
                    timeWaiter.sleep(minDelay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    pollingOutput.fail("Thread was interrupted");
                    return;
                }
            } else if (maxDelay > 0) {
                final int sleepMS = minDelay + RANDOM.nextInt(maxDelay - minDelay);
                try {
                    timeWaiter.sleep(sleepMS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    pollingOutput.fail("Thread was interrupted");
                    return;
                }
            }

            pollingOutput.addDataPoint("sample",
                    ThreadLocalRandom.current()
                            .nextDouble(Math.min(adapterConfig.getMinValue(), adapterConfig.getMaxValue()),
                                    Math.max(adapterConfig.getMinValue() + 1, adapterConfig.getMaxValue())));
            pollingOutput.finish();
        }).start();
    }

    @Override
    public @NotNull List<SimulationPollingContext> getPollingContexts() {
        return adapterConfig.getPollingContexts();
    }

    @Override
    public int getPollingIntervalMillis() {
        return adapterConfig.getPollingIntervalMillis();
    }

    @Override
    public int getMaxPollingErrorsBeforeRemoval() {
        return adapterConfig.getMaxPollingErrorsBeforeRemoval();
    }
}
