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
package com.hivemq.protocols;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import com.hivemq.adapter.sdk.api.config.legacy.ConfigTagsTuple;
import com.hivemq.adapter.sdk.api.config.legacy.LegacyConfigConversion;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class ProtocolAdapterConfigPersistence {

    private static final Logger log = LoggerFactory.getLogger(ProtocolAdapterConfigPersistence.class);

    private @NotNull final ProtocolAdapterConfig adapterConfig;
    private @NotNull final List<? extends Tag> tags;

    public ProtocolAdapterConfigPersistence(
            @NotNull final ProtocolAdapterConfig adapterConfig,
            @NotNull final List<? extends Tag> tags) {
        Objects.requireNonNull(adapterConfig);
        Objects.requireNonNull(tags);
        this.adapterConfig = adapterConfig;
        this.tags = tags;
    }

    public static ProtocolAdapterConfigPersistence fromAdapterConfigMap(@NotNull final Map<String, Object> adapterConfig,
                                                                        final boolean writingEnabled,
                                                                        @NotNull final ObjectMapper mapper,
                                                                        @NotNull final ProtocolAdapterFactory protocolAdapterFactory) {
        Map<String, Object> adapterConfigMap = (Map<String, Object>)adapterConfig.get("config");
        List<Map<String, Object>> tagMaps = Objects.requireNonNullElse((List<Map<String, Object>>)adapterConfig.get("tags"), List.of());

        if(adapterConfigMap != null) {
            final ProtocolAdapterConfig protocolAdapterConfig = protocolAdapterFactory.convertConfigObject(
                    mapper,
                    adapterConfigMap,
                    writingEnabled);
            final List<? extends Tag> tags = protocolAdapterFactory.convertTagDefinitionObjects(mapper, tagMaps);
            return new ProtocolAdapterConfigPersistence(protocolAdapterConfig,tags);
        } else if(protocolAdapterFactory instanceof LegacyConfigConversion) {
            log.warn(
                    "Trying to load {} as legacy configuration. Support for the legacy configuration will be removed in the beginning of 2025.",
                    protocolAdapterFactory.getInformation().getDisplayName());

            final ConfigTagsTuple configTagsTuple = ((LegacyConfigConversion) protocolAdapterFactory)
                            .tryConvertLegacyConfig(mapper, adapterConfig);
            return new ProtocolAdapterConfigPersistence(
                    configTagsTuple.getConfig(),
                    configTagsTuple.getTags());
        } else {
            log.error(
                    "No <config>-tag in configuration file for {}",
                    protocolAdapterFactory.getInformation().getDisplayName());
            throw new IllegalArgumentException("No <config>-tag in configuration file for " + protocolAdapterFactory.getInformation().getDisplayName());
        }

    }

    public @NotNull ProtocolAdapterConfig getAdapterConfig() {
        return adapterConfig;
    }

    public @NotNull List<? extends Tag> getTags() {
        return tags;
    }

    public Optional<Set<String>> missingTags() {
        final Set<String> names = new HashSet<>(adapterConfig.calculateAllUsedTags());
        tags.forEach(tag -> names.remove(tag.getName()));
        if(names.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(names);
        }
    }
}
