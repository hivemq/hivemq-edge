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
import com.hivemq.edge.adapters.redis.helpers.RedisHelpers;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

@SuppressWarnings({"FieldCanBeLocal", "unused", "EmptyTryBlock"})
public class RedisSubscribingProtocolAdapter implements ProtocolAdapter {
    private static final @NotNull Logger log = LoggerFactory.getLogger(RedisSubscribingProtocolAdapter.class);

    private final @NotNull RedisAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull AdapterFactories adapterFactories;
    private final @NotNull String adapterId;
    private final @NotNull RedisHelpers redisHelpers;
    private JedisPool jedisPool;

    public RedisSubscribingProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation, final @NotNull ProtocolAdapterInput<RedisAdapterConfig> input, @NotNull RedisHelpers redisHelpers) {
        this.adapterId = input.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.adapterFactories = input.adapterFactories();
        this.redisHelpers = new RedisHelpers();
    }

    @Override
    public @NotNull String getId() {
        return adapterId;
    }

    @Override
    public void start(@NotNull ProtocolAdapterStartInput input, @NotNull ProtocolAdapterStartOutput output) {
        /* Test connection to the database when starting the adapter. */
        try {
            // Start Initialization
            jedisPool = redisHelpers.initJedisPool(adapterConfig);

            if(!jedisPool.isClosed()){
                output.startedSuccessfully();
                protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
                jedisPool.close();
            } else {
                output.failStart(new Throwable("Error connecting Redis server, please check the configuration"), "Error connecting Redis server, please check the configuration");
                protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
            }
        } catch (final Exception e) {
            jedisPool.close();
            output.failStart(e, null);
            protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
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
