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
package com.hivemq.edge.adapters.plc4x.types.siemens;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapter;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterFactory;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterInput;
import com.hivemq.edge.modules.config.CustomConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.Map;

/**
 * @author HiveMQ Adapter Generator
 */
public class S7ProtocolAdapterFactory implements ProtocolAdapterFactory<S7AdapterConfig> {

    @Override
    public @NotNull ProtocolAdapterInformation getInformation() {
        return S7ProtocolAdapterInformation.INSTANCE;
    }

    @Override
    public @NotNull ProtocolAdapter createAdapter(@NotNull final ProtocolAdapterInformation adapterInformation, @NotNull final ProtocolAdapterInput<S7AdapterConfig> input) {
        return new S7ProtocolAdapter(adapterInformation, input.getConfig(), input.getMetricRegistry());
    }

    @Override
    public @NotNull S7AdapterConfig convertConfigObject(final @NotNull ObjectMapper objectMapper, final @NotNull Map<@NotNull String, Object> config) {
        return S7ConfigConverter.convertConfig(objectMapper, config);
    }

    @Override
    public Map<String, Object> unconvertConfigObject(final @NotNull ObjectMapper objectMapper, final CustomConfig config) {
        return S7ConfigConverter.unconvertConfig(objectMapper, config);
    }

    @Override
    public @NotNull Class<S7AdapterConfig> getConfigClass() {
        return S7AdapterConfig.class;
    }

}
