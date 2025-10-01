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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.model.*;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingInput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.redis.config.RedisAdapterConfig;
import com.hivemq.edge.adapters.redis.config.RedisAdapterTag;
import com.hivemq.edge.adapters.redis.config.RedisAdapterTagDefinition;
import com.hivemq.edge.adapters.redis.helpers.RedisHelpers;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.List;
import java.util.Objects;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.STATELESS;


public class RedisPollingProtocolAdapter implements BatchPollingProtocolAdapter {
    private static final @NotNull Logger log = LoggerFactory.getLogger(RedisPollingProtocolAdapter.class);
    private static final @NotNull ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final @NotNull RedisAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull RedisHelpers redisHelpers;
    private final @NotNull List<Tag> tags;
    private JedisPool jedisPool;
    private final @NotNull String adapterId;
    private final @NotNull AdapterFactories adapterFactories;

    public RedisPollingProtocolAdapter(final @NotNull ProtocolAdapterInformation adapterInformation,
                                       final @NotNull ProtocolAdapterInput<RedisAdapterConfig> input) {
        this.tags = input.getTags();
        this.redisHelpers = new RedisHelpers();
        this.adapterId = input.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.adapterFactories = input.adapterFactories();
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
                output.failStart(new Throwable("Error connecting Redis server, please check the configuration"),
                        "Error connecting Redis server, please check the configuration");
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
    public void poll(final @NotNull BatchPollingInput pollingInput, final @NotNull BatchPollingOutput pollingOutput) {
        log.debug("Handling tags for Redis protocol adapter");
        tags.forEach(tag -> loadRedisData(pollingOutput, (RedisAdapterTag) tag));
        log.debug("Finished getting tags");
        protocolAdapterState.setConnectionStatus(STATELESS);
        pollingOutput.finish();
        log.debug("Finished polling");
    }

    private void loadRedisData(final @NotNull BatchPollingOutput output, final @NotNull RedisAdapterTag tag) {
        /* Check if the pool is closed, if not, create a new one */
        if(jedisPool.isClosed()){
            jedisPool = redisHelpers.initJedisPool(adapterConfig);
        }

        final RedisAdapterTagDefinition definition = tag.getDefinition();

        log.debug("Tag : {}", tag.getName());

        /* Connect to Redis and get the key */
        try {
            /* Switch method based on a type of key to retrieve*/
            final ObjectNode node = OBJECT_MAPPER.createObjectNode();
            final DataPointFactory dataPointFactory = adapterFactories.dataPointFactory();
            switch (Objects.requireNonNull(Objects.requireNonNull(definition.getType()).toString())) {
                case "HASH":
                    log.debug("Handling Hash");
                    log.debug("Hash Key : {}", definition.getKey());
                    log.debug("Field : {}", definition.getField());
                    try (final Jedis jedis = jedisPool.getResource();){
                        final String result;
                        if (definition.getAll()){
                            result = jedis.hgetAll(definition.getKey()).toString();
                        } else {
                            result = jedis.hget(definition.getKey(), definition.getField());
                        }
                        log.debug("Hash Result : {}",result);
                        node.put("value", result);
                        output.addDataPoint(dataPointFactory.create(tag.getName(), node));
                    }
                    break;
                case "LIST":
                    log.debug("Handling List like queue");
                    log.debug("List Key : {}", definition.getKey());
                    try (final Jedis jedis = jedisPool.getResource();){
                        final String result = jedis.rpop(definition.getKey());
                        log.debug("List Result : {}",result);
                        node.put("value", result);
                        output.addDataPoint(dataPointFactory.create(tag.getName(), node));
                    }
                    break;
                default:
                    log.debug("Handling Default (String)");
                    log.debug("String Key : {}", definition.getKey());
                    try (final Jedis jedis = jedisPool.getResource();){
                        final String result = jedis.get(definition.getKey());
                        node.put("value", result);
                        log.debug(node.toString());
                        log.debug("Adding datapoint");
                        log.debug(tag.getName());
                        output.addDataPoint(dataPointFactory.create(tag.getName(), node));
                    }
                    break;
            }
        } catch (final Exception e) {
            output.fail(e, e.getMessage());
        }
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
