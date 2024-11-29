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
package com.hivemq.edge.adapters.opcua;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.config.legacy.ConfigTagsTuple;
import com.hivemq.adapter.sdk.api.config.legacy.LegacyConfigConversion;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.legacy.LegacyOpcUaAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttMapping;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class OpcUaProtocolAdapterFactory
        implements ProtocolAdapterFactory<OpcUaSpecificAdapterConfig>, LegacyConfigConversion {

    private static final @NotNull Logger log = LoggerFactory.getLogger(OpcUaProtocolAdapterFactory.class);

    final boolean writingEnabled;

    public OpcUaProtocolAdapterFactory(@NotNull final ProtocolAdapterFactoryInput input) {
        this.writingEnabled = input.isWritingEnabled();
    }

    @Override
    public @NotNull ProtocolAdapterInformation getInformation() {
        return OpcUaProtocolAdapterInformation.INSTANCE;
    }

    @Override
    public @NotNull ProtocolAdapter createAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<OpcUaSpecificAdapterConfig> input) {
        return new OpcUaProtocolAdapter(adapterInformation, input);
    }

    @NotNull
    public ConfigTagsTuple tryConvertLegacyConfig(
            final @NotNull ObjectMapper objectMapper, final @NotNull Map<String, Object> config) {
        final LegacyOpcUaAdapterConfig legacyOpcUaAdapterConfig =
                objectMapper.convertValue(config, LegacyOpcUaAdapterConfig.class);


        final List<OpcUaToMqttMapping> opcuaToMqttMappings = new ArrayList<>();
        final List<OpcuaTag> tags = new ArrayList<>();

        final Set<Integer> publishingIntervals = new HashSet<>();
        final Set<Integer> serverQueueSizes = new HashSet<>();

        for (final LegacyOpcUaAdapterConfig.Subscription subscription : legacyOpcUaAdapterConfig.getSubscriptions()) {
            final String newTagName = legacyOpcUaAdapterConfig.getId() + "-" + UUID.randomUUID();
            tags.add(new OpcuaTag(newTagName, "not set", new OpcuaTagDefinition(subscription.getNode())));

            publishingIntervals.add(subscription.getPublishingInterval());
            serverQueueSizes.add(subscription.getServerQueueSize());

            opcuaToMqttMappings.add(new OpcUaToMqttMapping(
                    subscription.getMqttTopic(),
                    subscription.getQos(),
                    subscription.getMessageExpiryInterval() != null ? subscription.getMessageExpiryInterval().longValue() : 4294967295L,
                    newTagName));
        }

        final Optional<Integer> publishingInterval = publishingIntervals.stream().max(Integer::compareTo);
        final Optional<Integer> serverQueueSize = serverQueueSizes.stream().min(Integer::compareTo);

        if(publishingIntervals.size() > 1 || serverQueueSizes.size() > 1) {
            log.warn("There are multiple values for publishingInterval and serverQueueSize set, picking publishingInterval={} and serverQueueSize={}", publishingIntervals, serverQueueSize);
        }

        final OpcUaToMqttConfig opcuaToMqttConfig = new OpcUaToMqttConfig(
                publishingInterval.orElse(null),
                serverQueueSize.orElse(null));

        return new ConfigTagsTuple(legacyOpcUaAdapterConfig.getId(), new OpcUaSpecificAdapterConfig(
                legacyOpcUaAdapterConfig.getUri(),
                legacyOpcUaAdapterConfig.getOverrideUri(),
                legacyOpcUaAdapterConfig.getAuth(),
                legacyOpcUaAdapterConfig.getTls(),
                opcuaToMqttConfig,
                legacyOpcUaAdapterConfig.getSecurity()),
                tags,
                opcuaToMqttMappings);
    }
}
