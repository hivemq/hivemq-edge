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
package com.hivemq.edge.adapters.modbus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.edge.adapters.modbus.config.ModbusAdapterConfig;
import com.hivemq.edge.adapters.modbus.config.ModbusToMqttConfig;
import com.hivemq.edge.adapters.modbus.config.ModbusToMqttMapping;
import com.hivemq.edge.adapters.modbus.config.legacy.LegacyModbusAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ModbusProtocolAdapterFactory implements ProtocolAdapterFactory<ModbusAdapterConfig> {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ModbusProtocolAdapterFactory.class);

    @Override
    public @NotNull ProtocolAdapterInformation getInformation() {
        return ModbusProtocolAdapterInformation.INSTANCE;
    }

    @Override
    public @NotNull ProtocolAdapter createAdapter(
            @NotNull final ProtocolAdapterInformation adapterInformation,
            @NotNull final ProtocolAdapterInput<ModbusAdapterConfig> input) {
        return new ModbusProtocolAdapter(adapterInformation, input.getConfig(), input);
    }

    @Override
    public @NotNull Class<ModbusAdapterConfig> getConfigClass() {
        return ModbusAdapterConfig.class;
    }

    @Override
    public @NotNull ModbusAdapterConfig convertConfigObject(
            final @NotNull ObjectMapper objectMapper,
            final @NotNull Map<String, Object> config) {
        try {
            return ProtocolAdapterFactory.super.convertConfigObject(objectMapper, config);
        } catch (final Exception currentConfigFailedException) {
            try {
                log.warn("Could not load '{}' configuration, trying to load legacy configuration. Because: '{}'",
                        ModbusProtocolAdapterInformation.INSTANCE.getDisplayName(),
                        currentConfigFailedException.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("Original Exception:", currentConfigFailedException);
                }
                return tryConvertLegacyConfig(objectMapper, config);
            } catch (final Exception legacyConfigFailedException) {
                log.warn("Could not load legacy '{}' configuration. Because: '{}'",
                        ModbusProtocolAdapterInformation.INSTANCE.getDisplayName(),
                        legacyConfigFailedException.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("Original Exception:", legacyConfigFailedException);
                }
                //we rethrow the exception from the current config conversation, to have a correct rest response.
                throw currentConfigFailedException;
            }
        }
    }

    private static @NotNull ModbusAdapterConfig tryConvertLegacyConfig(
            final @NotNull ObjectMapper objectMapper,
            final @NotNull Map<String, Object> config) {
        final LegacyModbusAdapterConfig legacyModbusAdapterConfig =
                objectMapper.convertValue(config, LegacyModbusAdapterConfig.class);

        final List<ModbusToMqttMapping> modbusToMqttMappings = legacyModbusAdapterConfig.getSubscriptions()
                .stream()
                .map(context -> new ModbusToMqttMapping(context.getMqttTopic(),
                        context.getMqttQos(),
                        context.getMessageHandlingOptions(),
                        context.getIncludeTimestamp(),
                        context.getIncludeTagNames(),
                        context.getLegacyProperties(),
                        context.getAddressRange()))
                .collect(Collectors.toList());

        final ModbusToMqttConfig modbusToMqttConfig =
                new ModbusToMqttConfig(legacyModbusAdapterConfig.getPollingIntervalMillis(),
                        legacyModbusAdapterConfig.getMaxPollingErrorsBeforeRemoval(),
                        legacyModbusAdapterConfig.getPublishChangedDataOnly(),
                        modbusToMqttMappings);


        return new ModbusAdapterConfig(legacyModbusAdapterConfig.getId(),
                legacyModbusAdapterConfig.getPort(),
                legacyModbusAdapterConfig.getHost(),
                legacyModbusAdapterConfig.getTimeout(),
                modbusToMqttConfig);
    }
}
