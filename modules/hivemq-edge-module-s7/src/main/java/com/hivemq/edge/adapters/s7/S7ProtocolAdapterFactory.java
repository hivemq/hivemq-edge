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
import com.hivemq.edge.adapters.plc4x.config.Plc4xToMqttMapping;
import com.hivemq.edge.adapters.plc4x.types.siemens.config.S7AdapterConfig;
import com.hivemq.edge.adapters.plc4x.types.siemens.config.S7ToMqttConfig;
import com.hivemq.edge.adapters.plc4x.types.siemens.config.legacy.LegacyS7AdapterConfig;
import com.hivemq.edge.adapters.s7.config.S7AdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        try {
            return ProtocolAdapterFactory.super.convertConfigObject(objectMapper, config);
        } catch (final Exception currentConfigFailedException) {
            try {
                log.warn("Could not load '{}' configuration, trying to load legacy configuration. Because: '{}'. Support for the legacy configuration will be removed in the beginning of 2025.",
                        S7ProtocolAdapterInformation.INSTANCE.getDisplayName(),
                        currentConfigFailedException.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("Original Exception:", currentConfigFailedException);
                }
                return tryConvertLegacyConfig(objectMapper, config);
            } catch (final Exception legacyConfigFailedException) {
                log.warn("Could not load legacy '{}' configuration. Because: '{}'",
                        S7ProtocolAdapterInformation.INSTANCE.getDisplayName(),
                        legacyConfigFailedException.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("Original Exception:", legacyConfigFailedException);
                }
                //we rethrow the exception from the current config conversation, to have a correct rest response.
                throw currentConfigFailedException;
            }
        }
    }

    private static @NotNull S7AdapterConfig tryConvertLegacyConfig(
            final @NotNull ObjectMapper objectMapper,
            final @NotNull Map<String, Object> config) {
        final LegacyS7AdapterConfig legacyS7AdapterConfig =
                objectMapper.convertValue(config, LegacyS7AdapterConfig.class);

        final List<Plc4xToMqttMapping> plc4xToMqttMappings = legacyS7AdapterConfig.getSubscriptions()
                .stream()
                .map(subscription -> new Plc4xToMqttMapping(subscription.getMqttTopic(),
                        subscription.getMqttQos(),
                        subscription.getMessageHandlingOptions(),
                        subscription.getIncludeTimestamp(),
                        subscription.getIncludeTagNames(),
                        subscription.getTagName(),
                        subscription.getTagAddress(),
                        subscription.getDataType(),
                        subscription.getUserProperties()))
                .collect(Collectors.toList());

        final S7ToMqttConfig s7ToMqttConfig = new S7ToMqttConfig(legacyS7AdapterConfig.getPollingIntervalMillis(),
                legacyS7AdapterConfig.getMaxPollingErrorsBeforeRemoval(),
                legacyS7AdapterConfig.getPublishChangedDataOnly(),
                plc4xToMqttMappings);

        return new S7AdapterConfig(legacyS7AdapterConfig.getId(),
                legacyS7AdapterConfig.getPort(),
                legacyS7AdapterConfig.getHost(),
                legacyS7AdapterConfig.getControllerType(),
                legacyS7AdapterConfig.getRemoteRack(),
                legacyS7AdapterConfig.getRemoteRack2(),
                legacyS7AdapterConfig.getRemoteSlot(),
                legacyS7AdapterConfig.getRemoteSlot2(),
                legacyS7AdapterConfig.getRemoteTsap(),
                s7ToMqttConfig);
    }
}
