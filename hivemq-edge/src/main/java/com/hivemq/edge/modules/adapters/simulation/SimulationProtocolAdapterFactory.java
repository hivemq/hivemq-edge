/*
 * Copyright 2019-present HiveMQ GmbH
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
package com.hivemq.edge.modules.adapters.simulation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.edge.modules.adapters.simulation.config.SimulationAdapterConfig;
import com.hivemq.edge.modules.adapters.simulation.config.SimulationToMqttConfig;
import com.hivemq.edge.modules.adapters.simulation.config.SimulationToMqttMapping;
import com.hivemq.edge.modules.adapters.simulation.config.legacy.LegacySimulationAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SimulationProtocolAdapterFactory implements ProtocolAdapterFactory<SimulationAdapterConfig> {

    private static final @NotNull Logger log = LoggerFactory.getLogger(SimulationProtocolAdapterFactory.class);

    final boolean writingEnabled;

    public SimulationProtocolAdapterFactory(
            final @NotNull ProtocolAdapterFactoryInput protocolAdapterFactoryInput) {
        this.writingEnabled = protocolAdapterFactoryInput.isWritingEnabled();
    }

    @Override
    public @NotNull ProtocolAdapterInformation getInformation() {
        return SimulationProtocolAdapterInformation.INSTANCE;
    }

    @Override
    public @NotNull ProtocolAdapter createAdapter(
            @NotNull final ProtocolAdapterInformation adapterInformation,
            @NotNull final ProtocolAdapterInput<SimulationAdapterConfig> input) {
        return new SimulationProtocolAdapter(adapterInformation, input, TimeWaiter.INSTANCE);
    }

    @Override
    public @NotNull ProtocolAdapterConfig convertConfigObject(
            final @NotNull ObjectMapper objectMapper, final @NotNull Map<String, Object> config, final boolean writingEnabled) {
        try {
            return ProtocolAdapterFactory.super.convertConfigObject(objectMapper, config, writingEnabled);
        } catch (final Exception currentConfigFailedException) {
            try {
                log.warn(
                        "Could not load '{}' configuration, trying to load legacy configuration. Because: '{}'. Support for the legacy configuration will be removed in the beginning of 2025.",
                        SimulationProtocolAdapterInformation.INSTANCE.getDisplayName(),
                        currentConfigFailedException.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("Original Exception:", currentConfigFailedException);
                }
                return tryConvertLegacyConfig(objectMapper, config);
            } catch (final Exception legacyConfigFailedException) {
                log.warn("Could not load legacy '{}' configuration. Because: '{}'",
                        SimulationProtocolAdapterInformation.INSTANCE.getDisplayName(),
                        legacyConfigFailedException.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("Original Exception:", legacyConfigFailedException);
                }
                //we rethrow the exception from the current config conversation, to have a correct rest response.
                throw currentConfigFailedException;
            }
        }
    }

    private static @NotNull SimulationAdapterConfig tryConvertLegacyConfig(
            final @NotNull ObjectMapper objectMapper, final @NotNull Map<String, Object> config) {
        final LegacySimulationAdapterConfig legacySimulationAdapterConfig =
                objectMapper.convertValue(config, LegacySimulationAdapterConfig.class);

        final List<SimulationToMqttMapping> simulationToMqttMappings =
                legacySimulationAdapterConfig.getPollingContexts()
                        .stream()
                        .map(context -> new SimulationToMqttMapping(context.getMqttTopic(),
                                context.getMqttQos(),
                                context.getMessageHandlingOptions(),
                                context.getIncludeTimestamp(),
                                context.getIncludeTagNames(),
                                context.getUserProperties()))
                        .collect(Collectors.toList());

        final SimulationToMqttConfig simulationToMqttConfig = new SimulationToMqttConfig(simulationToMqttMappings,
                legacySimulationAdapterConfig.getPollingIntervalMillis(),
                legacySimulationAdapterConfig.getMaxPollingErrorsBeforeRemoval());


        return new SimulationAdapterConfig(simulationToMqttConfig,
                legacySimulationAdapterConfig.getId(),
                legacySimulationAdapterConfig.getMinValue(),
                legacySimulationAdapterConfig.getMaxValue(),
                legacySimulationAdapterConfig.getMinDelay(),
                legacySimulationAdapterConfig.getMaxDelay());
    }

}
