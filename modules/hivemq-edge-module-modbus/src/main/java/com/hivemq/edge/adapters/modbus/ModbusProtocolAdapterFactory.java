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
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.config.legacy.ConfigTagsTuple;
import com.hivemq.adapter.sdk.api.config.legacy.LegacyConfigConversion;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.edge.adapters.modbus.config.ModbusAdu;
import com.hivemq.edge.adapters.modbus.config.ModbusDataType;
import com.hivemq.edge.adapters.modbus.config.ModbusSpecificAdapterConfig;
import com.hivemq.edge.adapters.modbus.config.ModbusToMqttConfig;
import com.hivemq.edge.adapters.modbus.config.ModbusToMqttMapping;
import com.hivemq.edge.adapters.modbus.config.legacy.LegacyModbusPollingContext;
import com.hivemq.edge.adapters.modbus.config.legacy.LegacyModbusSpecificAdapterConfig;
import com.hivemq.edge.adapters.modbus.config.tag.ModbusTag;
import com.hivemq.edge.adapters.modbus.config.tag.ModbusTagDefinition;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ModbusProtocolAdapterFactory
        implements ProtocolAdapterFactory<ModbusSpecificAdapterConfig>, LegacyConfigConversion {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ModbusProtocolAdapterFactory.class);

    final boolean writingEnabled;

    public ModbusProtocolAdapterFactory(@NotNull final ProtocolAdapterFactoryInput input) {
        this.writingEnabled = input.isWritingEnabled();
    }

    @Override
    public @NotNull ProtocolAdapterInformation getInformation() {
        return ModbusProtocolAdapterInformation.INSTANCE;
    }

    @Override
    public @NotNull ProtocolAdapter createAdapter(
            @NotNull final ProtocolAdapterInformation adapterInformation,
            @NotNull final ProtocolAdapterInput<ModbusSpecificAdapterConfig> input) {
        return new ModbusProtocolAdapter(adapterInformation, input);
    }

    @NotNull
    public ConfigTagsTuple tryConvertLegacyConfig(
            final @NotNull ObjectMapper objectMapper, final @NotNull Map<String, Object> config) {
        final LegacyModbusSpecificAdapterConfig legacyModbusAdapterConfig =
                objectMapper.convertValue(config, LegacyModbusSpecificAdapterConfig.class);


        final List<PollingContext> modbusToMqttMappings = new ArrayList<>();
        final List<ModbusTag> modbusTags = new ArrayList<>();
        for (final LegacyModbusPollingContext context : legacyModbusAdapterConfig.getSubscriptions()) {
            // create tag first
            final String newTagName = legacyModbusAdapterConfig.getId() + "-" + UUID.randomUUID();
            modbusTags.add(new ModbusTag(newTagName,
                    "not set",
                    new ModbusTagDefinition(context.getAddressRange().startIdx,
                            ModbusAdu.HOLDING_REGISTERS,
                            0,
                            false,
                            ModbusDataType.INT_32)));
            final ModbusToMqttMapping modbusToMqttMapping = new ModbusToMqttMapping(context.getMqttTopic(),
                    context.getMqttQos(),
                    newTagName,
                    context.getMessageHandlingOptions(),
                    context.getIncludeTimestamp(),
                    context.getIncludeTagNames(),
                    context.getLegacyProperties());
            modbusToMqttMappings.add(modbusToMqttMapping);
        }

        final ModbusToMqttConfig modbusToMqttConfig =
                new ModbusToMqttConfig(legacyModbusAdapterConfig.getPollingIntervalMillis(),
                        legacyModbusAdapterConfig.getMaxPollingErrorsBeforeRemoval(),
                        legacyModbusAdapterConfig.getPublishChangedDataOnly());


        return new ConfigTagsTuple(legacyModbusAdapterConfig.getId(),
                new ModbusSpecificAdapterConfig(legacyModbusAdapterConfig.getPort(),
                        legacyModbusAdapterConfig.getHost(),
                        legacyModbusAdapterConfig.getTimeout(),
                        modbusToMqttConfig),
                modbusTags,
                modbusToMqttMappings);
    }
}
