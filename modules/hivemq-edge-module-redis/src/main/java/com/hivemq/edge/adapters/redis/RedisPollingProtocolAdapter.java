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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.model.*;
import com.hivemq.adapter.sdk.api.polling.PollingInput;
import com.hivemq.adapter.sdk.api.polling.PollingOutput;
import com.hivemq.adapter.sdk.api.polling.PollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.adapters.redis.config.RedisAdapterConfig;
import com.hivemq.edge.adapters.redis.config.RedisPollingContext;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.*;

import java.util.List;

public class RedisPollingProtocolAdapter implements PollingProtocolAdapter<RedisPollingContext> {
    private final @NotNull RedisAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull List<RedisPollingContext> pollingContext;
    private JedisPool jedisPool = new JedisPool();
    private String password;

    public RedisPollingProtocolAdapter(final @NotNull ProtocolAdapterInformation adapterInformation, final @NotNull ProtocolAdapterInput<RedisAdapterConfig> input) {
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.pollingContext = adapterConfig.getPollingContexts();
    }

    @Override
    public @NotNull String getId() {
        return adapterConfig.getId();
    }

    @Override
    public void start(final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
        /* Test connection to the database when starting the adapter. */
        try {
            // Start Initialization
            initJedisPool();

            if(!jedisPool.isClosed()){
                output.startedSuccessfully();
                protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
            } else {
                output.failStart(new Throwable("Error connecting Redis server, please check the configuration"), "Error connecting Redis server, please check the configuration");
                protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
            }
        } catch (final Exception e) {
            output.failStart(e, null);
            protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
        }
    }

    private JedisPoolConfig buildPoolConfig() {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        // Change settings here if needed.
        return poolConfig;
    }

    @Override
    public void stop(final @NotNull ProtocolAdapterStopInput protocolAdapterStopInput, final @NotNull ProtocolAdapterStopOutput protocolAdapterStopOutput) {
        jedisPool.close();
        protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
        protocolAdapterStopOutput.stoppedSuccessfully();
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    @Override
    public void poll(final @NotNull PollingInput<RedisPollingContext> pollingInput, final @NotNull PollingOutput pollingOutput) {
        /* Connect to Redis and get the key */
        try {
            if(jedisPool.isClosed()){
               initJedisPool();
            }
            /* Get resource from the Pool */
            try (Jedis jedis = jedisPool.getResource();){
                String result = jedis.get(pollingInput.getPollingContext().getKey());
                /* Publish datapoint */
                pollingOutput.addDataPoint("keyValue", result);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        pollingOutput.finish();
    }

    @Override
    public @NotNull List<RedisPollingContext> getPollingContexts() {
        return pollingContext;
    }

    @Override
    public int getPollingIntervalMillis() {
        return adapterConfig.getPollingIntervalMillis();
    }

    @Override
    public int getMaxPollingErrorsBeforeRemoval() {
        return adapterConfig.getMaxPollingErrorsBeforeRemoval();
    }

    // Initialize Jedis Pool
    public void initJedisPool() throws Exception {
        JedisPoolConfig jedisPoolConfig = buildPoolConfig();
        if(!adapterConfig.getPassword().isEmpty()) {
            jedisPool = new JedisPool(jedisPoolConfig,adapterConfig.getServer(),adapterConfig.getPort(),60,adapterConfig.getPassword());
        } else {
            jedisPool = new JedisPool(jedisPoolConfig, adapterConfig.getServer(), adapterConfig.getPort(), 60);
        }
    }
}
