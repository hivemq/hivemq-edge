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
package com.hivemq.edge.adapters.modbus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.extension.sdk.api.adapters.ProtocolAdapter;
import com.hivemq.extension.sdk.api.adapters.ProtocolAdapterInformation;
import com.hivemq.extension.sdk.api.adapters.config.ProtocolAdapterConfig;
import com.hivemq.extension.sdk.api.adapters.factories.ProtocolAdapterFactory;
import com.hivemq.extension.sdk.api.adapters.model.ProtocolAdapterInput;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.Map;

public class ModbusProtocolAdapterFactory implements ProtocolAdapterFactory<ModbusAdapterConfig> {

    @Override
    public @NotNull ProtocolAdapterInformation getInformation() {
        return ModbusProtocolAdapterInformation.INSTANCE;
    }

    @Override
    public @NotNull ProtocolAdapter createAdapter(
            @NotNull final ProtocolAdapterInformation adapterInformation,
            @NotNull final ProtocolAdapterInput<ModbusAdapterConfig> input) {
        return new ModbusProtocolAdapter(adapterInformation,
                input.getConfig(),
                input);
    }

    @Override
    public @NotNull ModbusAdapterConfig convertConfigObject(
            final @NotNull ObjectMapper objectMapper,
            final @NotNull Map<@NotNull String, Object> config) {
        return ModbusConfigConverter.convertConfig(objectMapper, config);
    }

    @Override
    public @NotNull Map<String, Object> unconvertConfigObject(
            final @NotNull ObjectMapper objectMapper,
            final @NotNull ProtocolAdapterConfig config) {
        return ModbusConfigConverter.unconvertConfig(objectMapper, config);
    }

    @Override
    public @NotNull Class<ModbusAdapterConfig> getConfigClass() {
        return ModbusAdapterConfig.class;
    }

}
