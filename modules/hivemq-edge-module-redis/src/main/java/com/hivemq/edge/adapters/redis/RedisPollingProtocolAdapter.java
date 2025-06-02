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
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.json.Path2;

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
    private UnifiedJedis unifiedJedis;
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
        try {
            unifiedJedis = redisHelpers.initUnifiedJedis(adapterConfig);
            output.startedSuccessfully();
            protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);

        } catch (final Exception e) {
            output.failStart(new Throwable("Error connecting Redis server, please check the configuration"),
                e.getMessage());
            protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
        }
    }

    @Override
    public void stop(final @NotNull ProtocolAdapterStopInput protocolAdapterStopInput, final @NotNull ProtocolAdapterStopOutput protocolAdapterStopOutput) {
        unifiedJedis.close();
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
        final RedisAdapterTagDefinition definition = tag.getDefinition();
        log.debug("Tag : {}", tag.getName());

        try {
            final ObjectNode node = OBJECT_MAPPER.createObjectNode();
            final DataPointFactory dataPointFactory = adapterFactories.dataPointFactory();

            /* Switch method based on a type of key to retrieve*/
            switch (Objects.requireNonNull(Objects.requireNonNull(definition.getType()).toString())) {
                case "HASH":
                    log.debug("Handling Hash");
                    log.debug("Hash Key : {}", definition.getKey());
                    log.debug("Field : {}", definition.getField());

                    try {
                        final String result;
                        if (definition.getAll()){
                            result = unifiedJedis.hgetAll(definition.getKey()).toString();
                        } else {
                            result = unifiedJedis.hget(definition.getKey(), definition.getField());
                        }
                        node.put("value", result);
                        log.debug("Adding HASH datapoint : {} with value : {}", tag.getName(), node);
                        output.addDataPoint(dataPointFactory.create(tag.getName(), node));
                    } catch (final Exception e) {
                        log.error("Error getting HASH data from Redis", e);
                        output.fail(e, e.getMessage());
                    }
                    break;
                case "LIST":
                    log.debug("Handling List like queue");
                    log.debug("List Key : {}", definition.getKey());
                    try {
                        final String result = unifiedJedis.rpop(definition.getKey());
                        node.put("value", result);
                        log.debug("Adding LIST datapoint : {} with value : {}", tag.getName(), node);
                        output.addDataPoint(dataPointFactory.create(tag.getName(), node));
                    } catch (final Exception e) {
                        log.error("Error getting LIST data from Redis", e);
                        output.fail(e, e.getMessage());
                    }
                    break;

                case "JSON":
                    log.debug("Handling JSON Key");
                    log.debug("Json Key : {}", definition.getKey());
                    try {
                        final Object result = unifiedJedis.jsonGet(definition.getKey(), new Path2("$"));
                        node.put("value", result.toString());
                        log.debug("Adding JSON datapoint : {} with value : {}", tag.getName(), node);
                        output.addDataPoint(dataPointFactory.create(tag.getName(), node));
                    } catch (final Exception e) {
                        log.error("Error getting JSON data from Redis", e);
                        output.fail(e, e.getMessage());
                    }
                    break;
                default:
                    log.debug("Handling Default (String)");
                    log.debug("String Key : {}", definition.getKey());
                    try {
                        final String result = unifiedJedis.get(definition.getKey());
                        node.put("value", result);
                        log.debug("Adding STRING datapoint : {} with value : {}", tag.getName(), node);
                        output.addDataPoint(dataPointFactory.create(tag.getName(), node));
                    } catch (final Exception e) {
                        log.error("Error getting STRING data from Redis", e);
                        output.fail(e, e.getMessage());
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
