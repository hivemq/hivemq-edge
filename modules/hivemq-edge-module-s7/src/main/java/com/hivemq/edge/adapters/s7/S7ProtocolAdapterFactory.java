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
package com.hivemq.edge.adapters.s7;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.edge.adapters.s7.config.S7AdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author HiveMQ Adapter Generator
 */
public class S7ProtocolAdapterFactory implements ProtocolAdapterFactory<S7AdapterConfig> {

    private static final @NotNull Logger log = LoggerFactory.getLogger(S7ProtocolAdapterFactory.class);

    final boolean writingEnabled;

    public S7ProtocolAdapterFactory(final boolean writingEnabled) {
        this.writingEnabled = writingEnabled;
    }

    @Override
    public @NotNull ProtocolAdapterInformation getInformation() {
        return S7ProtocolAdapterInformation.INSTANCE;
    }

    @Override
    public @NotNull ProtocolAdapter createAdapter(
            @NotNull final ProtocolAdapterInformation adapterInformation,
            @NotNull final ProtocolAdapterInput<S7AdapterConfig> input) {
        return new S7ProtocolAdapter(adapterInformation, input);
    }


    @Override
    public @NotNull Class<S7AdapterConfig> getConfigClass() {
        return S7AdapterConfig.class;
    }


    @Override
    public @NotNull ProtocolAdapterConfig convertConfigObject(
            final @NotNull ObjectMapper objectMapper, final @NotNull Map<String, Object> config) {
        return ProtocolAdapterFactory.super.convertConfigObject(objectMapper, config);
    }

}
