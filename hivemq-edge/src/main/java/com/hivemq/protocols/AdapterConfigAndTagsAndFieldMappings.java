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
import com.hivemq.configuration.entity.adapter.FieldMappingsEntity;
import com.hivemq.persistence.fieldmapping.FieldMappings;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hivemq.protocols.ConfigPersistence.TAG_MAPPING_KEY;

public class AdapterConfigAndTagsAndFieldMappings {

    private static final Logger log = LoggerFactory.getLogger(AdapterConfigAndTagsAndFieldMappings.class);

    private final @NotNull ProtocolAdapterConfig adapterConfig;
    private final @NotNull List<? extends Tag> tags;
    private final @NotNull List<FieldMappings> fieldMappings;

    public AdapterConfigAndTagsAndFieldMappings(
            final @NotNull ProtocolAdapterConfig adapterConfig,
            final @NotNull List<? extends Tag> tags,
            final @NotNull List<FieldMappings> fieldMappings) {
        Objects.requireNonNull(adapterConfig);
        Objects.requireNonNull(tags);
        this.adapterConfig = adapterConfig;
        this.tags = tags;
        this.fieldMappings = fieldMappings;
    }

    public @NotNull ProtocolAdapterConfig getAdapterConfig() {
        return adapterConfig;
    }

    public @NotNull List<? extends Tag> getTags() {
        return tags;
    }

    public @NotNull Optional<Set<String>> missingTags() {
        final Set<String> names = new HashSet<>(adapterConfig.calculateAllUsedTags());
        tags.forEach(tag -> names.remove(tag.getName()));
        if (names.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(names);
        }
    }

    public static AdapterConfigAndTagsAndFieldMappings fromAdapterConfigMap(
            final @NotNull Map<String, Object> adapterConfig,
            final boolean writingEnabled,
            final @NotNull ObjectMapper mapper,
            final @NotNull ProtocolAdapterFactory protocolAdapterFactory) {
        final Map<String, Object> adapterConfigMap = (Map<String, Object>) adapterConfig.get("config");
        final List<Map<String, Object>> tagMaps =
                Objects.requireNonNullElse((List<Map<String, Object>>) adapterConfig.get("tags"), List.of());
        final List<Map<String, Object>> fieldMappingsMaps =
                Objects.requireNonNullElse((List<Map<String, Object>>) adapterConfig.get(TAG_MAPPING_KEY), List.of());


        if (adapterConfigMap != null) {
            final ProtocolAdapterConfig protocolAdapterConfig =
                    protocolAdapterFactory.convertConfigObject(mapper, adapterConfigMap, writingEnabled);
            final List<? extends Tag> tags = protocolAdapterFactory.convertTagDefinitionObjects(mapper, tagMaps);
            final List<FieldMappings> fieldMappings = fieldMappingsMaps.stream()
                    .map(tagMap -> mapper.convertValue(tagMap, FieldMappingsEntity.class))
                    .map(entity -> FieldMappings.fromEntity(entity, mapper))
                    .collect(Collectors.toList());
            return new AdapterConfigAndTagsAndFieldMappings(protocolAdapterConfig, tags, fieldMappings);
        } else if (protocolAdapterFactory instanceof LegacyConfigConversion) {
            log.warn(
                    "Trying to load {} as legacy configuration. Support for the legacy configuration will be removed in the beginning of 2025.",
                    protocolAdapterFactory.getInformation().getDisplayName());

            final ConfigTagsTuple configTagsTuple =
                    ((LegacyConfigConversion) protocolAdapterFactory).tryConvertLegacyConfig(mapper, adapterConfig);
            // currently legacy configs wont have a fieldmappings
            return new AdapterConfigAndTagsAndFieldMappings(configTagsTuple.getConfig(),
                    configTagsTuple.getTags(),
                    List.of());
        } else {
            log.error("No <config>-tag in configuration file for {}",
                    protocolAdapterFactory.getInformation().getDisplayName());
            throw new IllegalArgumentException("No <config>-tag in configuration file for " +
                    protocolAdapterFactory.getInformation().getDisplayName());
        }

    }

    public @NotNull List<FieldMappings> getFieldMappings() {
        return fieldMappings;
    }
}
