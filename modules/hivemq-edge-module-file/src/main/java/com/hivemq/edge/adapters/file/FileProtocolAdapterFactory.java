/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.edge.adapters.file.config.FileAdapterConfig;
import com.hivemq.edge.adapters.file.config.FileToMqttConfig;
import com.hivemq.edge.adapters.file.config.FileToMqttMapping;
import com.hivemq.edge.adapters.file.config.legacy.LegacyFileAdapterConfig;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FileProtocolAdapterFactory implements ProtocolAdapterFactory<FileAdapterConfig> {

    @Override
    public @NotNull ProtocolAdapterInformation getInformation() {
        return FileProtocolAdapterInformation.INSTANCE;
    }

    @Override
    public @NotNull ProtocolAdapter createAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            @NotNull final ProtocolAdapterInput<FileAdapterConfig> input) {
        return new FilePollingProtocolAdapter(adapterInformation, input);
    }

    @Override
    public @NotNull FileAdapterConfig convertConfigObject(
            final @NotNull ObjectMapper objectMapper, final @NotNull Map<String, Object> config) {
        if (config.get("FileToMqtt") != null || config.get("mqttToFile") != null) {
            return ProtocolAdapterFactory.super.convertConfigObject(objectMapper, config);
        } else {
            return tryConvertLegacyConfig(objectMapper, config);
        }
    }

    @Override
    public @NotNull Class<FileAdapterConfig> getConfigClass() {
        return FileAdapterConfig.class;
    }

    private static @NotNull FileAdapterConfig tryConvertLegacyConfig(
            final @NotNull ObjectMapper objectMapper,
            final @NotNull Map<String, Object> config) {
        final LegacyFileAdapterConfig legacyFileAdapterConfig =
                objectMapper.convertValue(config, LegacyFileAdapterConfig.class);

        final List<FileToMqttMapping> FileToMqttMappings = legacyFileAdapterConfig.getPollingContexts()
                .stream()
                .map(context -> new FileToMqttMapping(context.getDestinationMqttTopic(),
                        context.getQos(),
                        context.getMessageHandlingOptions(),
                        context.getIncludeTimestamp(),
                        context.getIncludeTagNames(),
                        context.getUserProperties(),
                        context.getFilePath(),
                        context.getContentType()))
                .collect(Collectors.toList());

        final FileToMqttConfig FileToMqttConfig =
                new FileToMqttConfig(legacyFileAdapterConfig.getPollingIntervalMillis(),
                        legacyFileAdapterConfig.getMaxPollingErrorsBeforeRemoval(),
                        FileToMqttMappings);

        return new FileAdapterConfig(legacyFileAdapterConfig.getId(), FileToMqttConfig);
    }

}
