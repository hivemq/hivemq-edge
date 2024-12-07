/*
 * Copyright 2024-present HiveMQ GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.hivemq.edge.adapters.redis;

import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.model.*;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.adapters.redis.config.RedisAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"FieldCanBeLocal", "unused", "EmptyTryBlock"})
public class RedisSubscribingProtocolAdapter implements ProtocolAdapter {
    private static final @NotNull Logger log = LoggerFactory.getLogger(RedisSubscribingProtocolAdapter.class);

    private final @NotNull RedisAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull AdapterFactories adapterFactories;

    public RedisSubscribingProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation, final @NotNull ProtocolAdapterInput<RedisAdapterConfig> input) {
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.adapterFactories = input.adapterFactories();
    }
    @Override
    public @NotNull String getId() {
        return adapterConfig.getId();
    }

    @Override
    public void start(@NotNull ProtocolAdapterStartInput input, @NotNull ProtocolAdapterStartOutput output) {
        try {
            // connect and subscribe your client here
            output.startedSuccessfully();
        } catch (final Exception e) {
            // error handling like logging and signaling edge that the start failed
            output.failStart(e, null);
        }
    }

    @Override
    public void stop(@NotNull ProtocolAdapterStopInput protocolAdapterStopInput, @NotNull ProtocolAdapterStopOutput protocolAdapterStopOutput) {
        try {
            // gracefully disconnect and cleanup resources here
        } catch (final Exception e) {
            protocolAdapterStopOutput.failStop(e, null);
        }
        protocolAdapterStopOutput.stoppedSuccessfully();
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }
}
