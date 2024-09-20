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
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.edge.adapters.opcua.config.OpcUaAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.OpcUaToMqttMapping;
import com.hivemq.edge.adapters.opcua.config.legacy.LegacyOpcUaAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OpcUaProtocolAdapterFactory implements ProtocolAdapterFactory<OpcUaAdapterConfig> {

    private static final @NotNull Logger log = LoggerFactory.getLogger(OpcUaProtocolAdapterFactory.class);

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

    @Override
    public @NotNull Class<OpcUaAdapterConfig> getConfigClass() {
        return OpcUaAdapterConfig.class;
    }

    @Override
    public @NotNull OpcUaAdapterConfig convertConfigObject(
            final @NotNull ObjectMapper objectMapper,
            final @NotNull Map<String, Object> config) {
        try {
            return ProtocolAdapterFactory.super.convertConfigObject(objectMapper, config);
        } catch (final Exception currentConfigFailedException) {
            try {
                log.warn("Could not load '{}' configuration, trying to load legacy configuration. Because: '{}'. Support for the legacy configuration will be removed in the beginning of 2025.",
                        OpcUaProtocolAdapterInformation.INSTANCE.getDisplayName(),
                        currentConfigFailedException.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("Original Exception:", currentConfigFailedException);
                }
                return tryConvertLegacyConfig(objectMapper, config);
            } catch (final Exception legacyConfigFailedException) {
                log.warn("Could not load legacy '{}' configuration. Because: '{}'",
                        OpcUaProtocolAdapterInformation.INSTANCE.getDisplayName(),
                        legacyConfigFailedException.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("Original Exception:", legacyConfigFailedException);
                }
                //we rethrow the exception from the current config conversation, to have a correct rest response.
                throw currentConfigFailedException;
            }
        }
    }

    private static @NotNull OpcUaAdapterConfig tryConvertLegacyConfig(
            final @NotNull ObjectMapper objectMapper, final @NotNull Map<String, Object> config) {
        final LegacyOpcUaAdapterConfig legacyOpcUaAdapterConfig =
                objectMapper.convertValue(config, LegacyOpcUaAdapterConfig.class);

        final List<OpcUaToMqttMapping> opcuaToMqttMappings = legacyOpcUaAdapterConfig.getSubscriptions()
                .stream()
                .map(context -> new OpcUaToMqttMapping(context.getNode(),
                        context.getMqttTopic(),
                        context.getPublishingInterval(),
                        context.getServerQueueSize(),
                        context.getQos(),
                        context.getMessageExpiryInterval() != null ?
                                context.getMessageExpiryInterval().longValue() :
                                null))
                .collect(Collectors.toList());

        final OpcUaToMqttConfig opcuaToMqttConfig = new OpcUaToMqttConfig(opcuaToMqttMappings);

        return new OpcUaAdapterConfig(legacyOpcUaAdapterConfig.getId(),
                legacyOpcUaAdapterConfig.getUri(),
                legacyOpcUaAdapterConfig.getOverrideUri(),
                legacyOpcUaAdapterConfig.getAuth(),
                legacyOpcUaAdapterConfig.getTls(),
                opcuaToMqttConfig,
                legacyOpcUaAdapterConfig.getSecurity());
    }
}
