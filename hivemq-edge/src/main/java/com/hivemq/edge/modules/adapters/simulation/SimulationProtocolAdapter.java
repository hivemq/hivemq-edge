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

import com.hivemq.edge.modules.adapters.PollingPerSubscriptionProtocolAdapter;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSample;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSampleImpl;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterInput;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterStartInput;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterStartOutput;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterState;
import com.hivemq.edge.modules.config.AdapterSubscription;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import static com.hivemq.edge.modules.api.adapters.ProtocolAdapterState.ConnectionStatus.STATELESS;
import static com.hivemq.edge.modules.api.adapters.ProtocolAdapterState.RuntimeStatus.STARTED;
import static com.hivemq.edge.modules.api.adapters.ProtocolAdapterState.RuntimeStatus.STOPPED;

public class SimulationProtocolAdapter implements PollingPerSubscriptionProtocolAdapter {

    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull SimulationAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterState protocolAdapterState;

    public SimulationProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<SimulationAdapterConfig> protocolAdapterInput) {
        this.adapterInformation = adapterInformation;
        this.adapterConfig = protocolAdapterInput.getConfig();
        this.protocolAdapterState = protocolAdapterInput.getProtocolAdapterState();
        this.protocolAdapterState.setConnectionStatus(STATELESS);
    }

    @Override
    public @NotNull String getId() {
        return adapterConfig.getId();
    }

    @Override
    public @NotNull CompletableFuture<ProtocolAdapterStartOutput> start(
            final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
        protocolAdapterState.setRuntimeStatus(STARTED);
        return CompletableFuture.completedFuture(output);
    }

    @Override
    public @NotNull CompletableFuture<Void> stop() {
        protocolAdapterState.setRuntimeStatus(STOPPED);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    @Override
    public @NotNull CompletableFuture<? extends ProtocolAdapterDataSample> poll(final @NotNull AdapterSubscription adapterSubscription) {
        ProtocolAdapterDataSample dataSample = new ProtocolAdapterDataSampleImpl(adapterSubscription);

        dataSample.addDataPoint("sample",
                ThreadLocalRandom.current()
                        .nextDouble(Math.min(adapterConfig.getMinValue(), adapterConfig.getMaxValue()),
                                Math.max(adapterConfig.getMinValue() + 1, adapterConfig.getMaxValue())));
        return CompletableFuture.completedFuture(dataSample);
    }

    @Override
    public @NotNull List<? extends AdapterSubscription> getSubscriptions() {
        return adapterConfig.getSubscriptions();
    }
}
