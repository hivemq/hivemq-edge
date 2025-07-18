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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.adapter.sdk.api.tag.TagDefinition;
import com.hivemq.configuration.entity.adapter.NorthboundMappingEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.entity.adapter.TagEntity;
import com.hivemq.persistence.domain.DomainTag;
import com.hivemq.persistence.mappings.NorthboundMapping;
import com.hivemq.persistence.mappings.SouthboundMapping;
import org.jetbrains.annotations.NotNull;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
public class ProtocolAdapterConfigConverter {

    private final @NotNull ProtocolAdapterFactoryManager protocolAdapterFactoryManager;
    private final @NotNull ObjectMapper mapper;


    @Inject
    public ProtocolAdapterConfigConverter(
            final @NotNull ProtocolAdapterFactoryManager protocolAdapterFactoryManager,
            final @NotNull ObjectMapper mapper) {
        this.protocolAdapterFactoryManager = protocolAdapterFactoryManager;
        this.mapper = mapper;
    }

    public @NotNull ProtocolAdapterConfig fromEntity(
            final @NotNull ProtocolAdapterEntity protocolAdapterEntity) {
        final Map<String, Object> adapterConfigMap = protocolAdapterEntity.getConfig();
        final List<TagEntity> tagEntities = protocolAdapterEntity.getTags();
        final List<Map<String, Object>> tagMaps =
                tagEntities.stream().map(TagEntity::toMap).collect(Collectors.toList());

        // we can assume that writing is enabled as the config for writing should always include the config for reading as well.
        final ProtocolAdapterFactory<?> protocolAdapterFactory =
                getProtocolAdapterFactory(protocolAdapterEntity.getProtocolId());
        final ProtocolSpecificAdapterConfig protocolSpecificAdapterConfig =
                protocolAdapterFactory.convertConfigObject(mapper, adapterConfigMap, true);
        final List<? extends Tag> tags = protocolAdapterFactory.convertTagDefinitionObjects(mapper, tagMaps);
        final List<NorthboundMapping> northboundMappingList = protocolAdapterEntity.getNorthboundMappingEntities()
                .stream()
                .map(NorthboundMappingEntity::toPersistence)
                .collect(Collectors.toList());
        final List<SouthboundMapping> southboundMappingList = protocolAdapterEntity.getSouthboundMappingEntities()
                .stream()
                .map(southbound -> southbound.toPersistence(mapper))
                .collect(Collectors.toList());

        return new ProtocolAdapterConfig(protocolAdapterEntity.getAdapterId(),
                protocolAdapterEntity.getProtocolId(),
                protocolAdapterEntity.getConfigVersion(),
                protocolSpecificAdapterConfig,
                southboundMappingList,
                northboundMappingList,
                tags);
    }

    public @NotNull ProtocolAdapterFactory<?> getProtocolAdapterFactory(
            final @NotNull String protocolId) {
        final @NotNull Optional<ProtocolAdapterFactory<?>> factoryOptional =
                protocolAdapterFactoryManager.get(protocolId);
        if (factoryOptional.isEmpty()) {
            throw new IllegalArgumentException("No Factory was found for adapter with protocol id '" +
                    protocolId +
                    "'. ");
        }
        return factoryOptional.get();
    }

    public @NotNull ProtocolAdapterEntity toEntity(
            final @NotNull ProtocolAdapterConfig config) {
        return ProtocolAdapterEntity.from(config, mapper);
    }

    public @NotNull ProtocolSpecificAdapterConfig convertAdapterConfig(
            final @NotNull String protocolId, final @NotNull Map<String, Object> config, final boolean writingEnabled) {
        return getProtocolAdapterFactory(protocolId).convertConfigObject(mapper, config, writingEnabled);
    }

    public @NotNull List<? extends Tag> mapsToTags(
            final @NotNull String protocolId, final @NotNull List<Map<String, Object>> domainTags) {
        return getProtocolAdapterFactory(protocolId).convertTagDefinitionObjects(mapper, domainTags);
    }

    public @NotNull <T extends Tag> T domaintTagToTag(
            final @NotNull String protocolId, final @NotNull DomainTag domainTag) {
        return (T)getProtocolAdapterFactory(protocolId).convertTagDefinitionObject(mapper, domainTag.toTagMap());
    }

    public @NotNull JsonNode convertTagDefinitionToJsonNode(final @NotNull TagDefinition tagDefinition) {
        return mapper.valueToTree(tagDefinition);
    }
}
