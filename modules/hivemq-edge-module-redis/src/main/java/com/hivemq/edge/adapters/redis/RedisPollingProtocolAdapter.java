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

import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.model.*;
import com.hivemq.adapter.sdk.api.polling.PollingInput;
import com.hivemq.adapter.sdk.api.polling.PollingOutput;
import com.hivemq.adapter.sdk.api.polling.PollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.redis.config.RedisAdapterConfig;
import com.hivemq.edge.adapters.redis.config.RedisAdapterTagDefinition;
import com.hivemq.edge.adapters.redis.helpers.RedisHelpers;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.json.Path2;

import java.util.List;
import java.util.Objects;


public class RedisPollingProtocolAdapter implements PollingProtocolAdapter{
    private static final @NotNull Logger log = LoggerFactory.getLogger(RedisPollingProtocolAdapter.class);
    private final @NotNull RedisAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull RedisHelpers redisHelpers;
    private final @NotNull List<Tag> tags;
    private JedisPool jedisPool;
    private final @NotNull String adapterId;

    public RedisPollingProtocolAdapter(final @NotNull ProtocolAdapterInformation adapterInformation, final @NotNull ProtocolAdapterInput<RedisAdapterConfig> input) {
        this.tags = input.getTags();
        this.redisHelpers = new RedisHelpers();
        this.adapterId = input.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
    }

    @Override
    public @NotNull String getId() {
        return adapterId;
    }

    @Override
    public void start(final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
        /* Test connection to the database when starting the adapter. */
        try {
            // Start Initialization
            jedisPool = redisHelpers.initJedisPool(adapterConfig);

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
    public void poll(final @NotNull PollingInput pollingInput, final @NotNull PollingOutput pollingOutput) {
        final PollingContext pollingContext = pollingInput.getPollingContext();

        /* Handle tags */
        try {
            tags.stream()
                    .filter(tag -> tag.getName().equals(pollingContext.getTagName()))
                    .findFirst()
                    .ifPresentOrElse(
                            def -> {
                                RedisAdapterTagDefinition definition = (RedisAdapterTagDefinition) def.getDefinition();

                                /* Connect to Redis and get the key */
                                try {
                                    if(jedisPool.isClosed()){
                                        jedisPool = redisHelpers.initJedisPool(adapterConfig);
                                    }

                                    /* Switch method based on type of key to retrieve*/
                                    switch (Objects.requireNonNull(Objects.requireNonNull(definition.getType()).toString())) {
                                        case "HASH":
                                            log.debug("Handling Hash");
                                            log.debug("Hash Key : {}", definition.getKey());
                                            log.debug("Field : {}", definition.getField());
                                            try (Jedis jedis = jedisPool.getResource();){
                                                String result;

                                                if (definition.getAll()){
                                                    result = jedis.hgetAll(definition.getKey()).toString();
                                                } else {
                                                    result = jedis.hget(definition.getKey(), definition.getField());
                                                }
                                                log.debug("Hash Result : {}",result);
                                                pollingOutput.addDataPoint("hashValue", result);
                                            }
                                            break;
                                        case "LIST":
                                            log.debug("Handling List like queue");
                                            log.debug("List Key : {}", definition.getKey());
                                            try (Jedis jedis = jedisPool.getResource();){
                                                String result = jedis.rpop(definition.getKey());
                                                log.debug("List Result : {}",result);
                                                pollingOutput.addDataPoint("listValue", result);
                                            }
                                            break;
                                        default:
                                            log.debug("Handling Default (String)");
                                            log.debug("String Key : {}", definition.getKey());
                                            try (Jedis jedis = jedisPool.getResource();){
                                                String result = jedis.get(definition.getKey());
                                                log.debug("String Result : {}",result);
                                                pollingOutput.addDataPoint("stringValue", result);
                                            }
                                            break;
                                    }
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            },
                            () -> pollingOutput.fail("Polling for Redis protocol adapter failed because the used tag '" +
                                    pollingInput.getPollingContext().getTagName() +
                                    "' was not found. For the polling to work the tag must be created via REST API or the UI.")
                    );
        } catch (Exception e) {
            log.debug(e.getMessage());
            throw new RuntimeException(e);
        }

        pollingOutput.finish();
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
