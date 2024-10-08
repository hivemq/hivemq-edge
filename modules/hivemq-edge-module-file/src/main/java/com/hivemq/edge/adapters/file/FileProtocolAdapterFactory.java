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
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.edge.adapters.file.config.FileAdapterConfig;
import com.hivemq.edge.adapters.file.config.FileToMqttConfig;
import com.hivemq.edge.adapters.file.config.FileToMqttMapping;
import com.hivemq.edge.adapters.file.config.legacy.LegacyFileAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FileProtocolAdapterFactory implements ProtocolAdapterFactory<FileAdapterConfig> {

    private static final @NotNull Logger log = LoggerFactory.getLogger(FileProtocolAdapterFactory.class);

    final boolean writingEnabled;

    public FileProtocolAdapterFactory(final boolean writingEnabled) {
        this.writingEnabled = writingEnabled;
    }

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
    public @NotNull ProtocolAdapterConfig convertConfigObject(
            final @NotNull ObjectMapper objectMapper,
            final @NotNull Map<String, Object> config) {
        try {
            return ProtocolAdapterFactory.super.convertConfigObject(objectMapper, config);
        } catch (final Exception currentConfigFailedException) {
            try {
                log.warn("Could not load '{}' configuration, trying to load legacy configuration. Because: '{}'. Support for the legacy configuration will be removed in the beginning of 2025.",
                        FileProtocolAdapterInformation.INSTANCE.getDisplayName(),
                        currentConfigFailedException.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("Original Exception:", currentConfigFailedException);
                }
                return tryConvertLegacyConfig(objectMapper, config);
            } catch (final Exception legacyConfigFailedException) {
                log.warn("Could not load legacy '{}' configuration. Because: '{}'",
                        FileProtocolAdapterInformation.INSTANCE.getDisplayName(),
                        legacyConfigFailedException.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("Original Exception:", legacyConfigFailedException);
                }
                //we rethrow the exception from the current config conversation, to have a correct rest response.
                throw currentConfigFailedException;
            }
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

        final List<FileToMqttMapping> fileToMqttMappings = legacyFileAdapterConfig.getPollingContexts()
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

        final FileToMqttConfig fileToMqttConfig =
                new FileToMqttConfig(legacyFileAdapterConfig.getPollingIntervalMillis(),
                        legacyFileAdapterConfig.getMaxPollingErrorsBeforeRemoval(),
                        fileToMqttMappings);

        return new FileAdapterConfig(legacyFileAdapterConfig.getId(), fileToMqttConfig);
    }

}
