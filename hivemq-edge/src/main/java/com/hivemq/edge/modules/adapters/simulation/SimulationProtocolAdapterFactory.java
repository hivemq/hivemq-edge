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
import com.hivemq.edge.modules.adapters.ProtocolAdapter;
import com.hivemq.edge.modules.adapters.ProtocolAdapterInformation;
import com.hivemq.edge.modules.adapters.config.ProtocolAdapterConfig;
import com.hivemq.edge.modules.adapters.factories.ProtocolAdapterFactory;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterInput;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.Map;

public class SimulationProtocolAdapterFactory implements ProtocolAdapterFactory<SimulationAdapterConfig> {

    @Override
    public @NotNull ProtocolAdapterInformation getInformation() {
        return SimulationProtocolAdapterInformation.INSTANCE;
    }

    @Override
    public @NotNull ProtocolAdapter createAdapter(@NotNull final ProtocolAdapterInformation adapterInformation, @NotNull final ProtocolAdapterInput<SimulationAdapterConfig> input) {
        return new SimulationProtocolAdapter(adapterInformation, input);
    }

    @Override
    public @NotNull SimulationAdapterConfig convertConfigObject(final @NotNull ObjectMapper objectMapper, final @NotNull Map<@NotNull String, Object> config) {
        return SimulationConfigConverter.convertConfig(objectMapper, config);
    }

    @Override
    public Map<String, Object> unconvertConfigObject(final @NotNull ObjectMapper objectMapper, final @NotNull ProtocolAdapterConfig config) {
        return SimulationConfigConverter.unconvertConfig(objectMapper, config);
    }

    @Override
    public @NotNull Class<SimulationAdapterConfig> getConfigClass() {
        return SimulationAdapterConfig.class;
    }

}
