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
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.edge.adapters.modbus.config.legacy.LegacyModbusAdapterConfig;
import com.hivemq.edge.adapters.modbus.config.ModbusAdapterConfig;
import com.hivemq.edge.adapters.modbus.config.ModbusToMqttConfig;
import com.hivemq.edge.adapters.modbus.config.ModbusToMqttMapping;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ModbusProtocolAdapterFactory implements ProtocolAdapterFactory<ModbusAdapterConfig> {

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
            final @NotNull ObjectMapper objectMapper, final @NotNull Map<String, Object> config) {
        if(config.get("modbusToMqtt") != null || config.get("mqttToModbus") != null) {
            return ProtocolAdapterFactory.super.convertConfigObject(objectMapper, config);
        } else  {
            return tryConvertLegacyConfig(objectMapper, config);
        }
    }

    private static @NotNull ModbusAdapterConfig tryConvertLegacyConfig(final @NotNull ObjectMapper objectMapper, final @NotNull Map<String, Object> config) {
        final LegacyModbusAdapterConfig legacyModbusAdapterConfig =
                objectMapper.convertValue(config, LegacyModbusAdapterConfig.class);

        final List<ModbusToMqttMapping> modbusToMqttMappings = legacyModbusAdapterConfig.getSubscriptions()
                .stream()
                .map(context -> new ModbusToMqttMapping(context.getMqttTopic(),
                        context.getMqttQos(),
                        context.getMessageHandlingOptions(),
                        context.getIncludeTimestamp(),
                        context.getIncludeTagNames(),
                        context.getLegacyProperties()
                                .stream()
                                .map(legacyUserProperty -> new MqttUserProperty(legacyUserProperty.getName(),
                                        legacyUserProperty.getValue()))
                                .collect(Collectors.toList()),
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
