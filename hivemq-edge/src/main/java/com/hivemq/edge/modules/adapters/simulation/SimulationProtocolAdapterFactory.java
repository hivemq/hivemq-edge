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

import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.edge.modules.adapters.simulation.config.SimulationAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;

public class SimulationProtocolAdapterFactory implements ProtocolAdapterFactory<SimulationAdapterConfig> {

    @Override
    public @NotNull ProtocolAdapterInformation getInformation() {
        return SimulationProtocolAdapterInformation.INSTANCE;
    }

    @Override
    public @NotNull ProtocolAdapter createAdapter(@NotNull final ProtocolAdapterInformation adapterInformation, @NotNull final ProtocolAdapterInput<SimulationAdapterConfig> input) {
        return new SimulationProtocolAdapter(adapterInformation, input, TimeWaiter.INSTANCE);
    }

    @Override
    public @NotNull Class<SimulationAdapterConfig> getConfigClass() {
        return SimulationAdapterConfig.class;
    }

}
