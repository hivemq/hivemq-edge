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
import com.hivemq.edge.adapters.opcua.config.OpcUaAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.legacy.LegacyOpcUaAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttMapping;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OpcUaProtocolAdapterFactory implements ProtocolAdapterFactory<OpcUaAdapterConfig>, LegacyConfigConversion {

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
            final @NotNull ProtocolAdapterInput<OpcUaAdapterConfig> input) {
        return new OpcUaProtocolAdapter(adapterInformation, input);
    }

    @NotNull
    public ConfigTagsTuple tryConvertLegacyConfig(
            final @NotNull ObjectMapper objectMapper, final @NotNull Map<String, Object> config) {
        final LegacyOpcUaAdapterConfig legacyOpcUaAdapterConfig =
                objectMapper.convertValue(config, LegacyOpcUaAdapterConfig.class);


        final List<OpcUaToMqttMapping> opcuaToMqttMappings = new ArrayList<>();
        final List<OpcuaTag> tags = new ArrayList<>();
        for (final LegacyOpcUaAdapterConfig.Subscription subscription : legacyOpcUaAdapterConfig.getSubscriptions()) {
            // create tag first
            final String newTagName = legacyOpcUaAdapterConfig.getId() + "-" + UUID.randomUUID();
            tags.add(new OpcuaTag(newTagName, "not set", new OpcuaTagDefinition(subscription.getNode())));
            opcuaToMqttMappings.add(new OpcUaToMqttMapping(newTagName,
                    subscription.getMqttTopic(),
                    subscription.getPublishingInterval(),
                    subscription.getServerQueueSize(),
                    subscription.getQos(),
                    subscription.getMessageExpiryInterval() != null ?
                            subscription.getMessageExpiryInterval().longValue() :
                            null));
        }
        final OpcUaToMqttConfig opcuaToMqttConfig = new OpcUaToMqttConfig(opcuaToMqttMappings);

        return new ConfigTagsTuple(new OpcUaAdapterConfig(legacyOpcUaAdapterConfig.getId(),
                legacyOpcUaAdapterConfig.getUri(),
                legacyOpcUaAdapterConfig.getOverrideUri(),
                legacyOpcUaAdapterConfig.getAuth(),
                legacyOpcUaAdapterConfig.getTls(),
                opcuaToMqttConfig,
                legacyOpcUaAdapterConfig.getSecurity()),
                tags);
    }
}
