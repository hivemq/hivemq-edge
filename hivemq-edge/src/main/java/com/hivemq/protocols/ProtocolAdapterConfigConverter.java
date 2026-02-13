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
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.adapter.sdk.api.tag.TagDefinition;
import com.hivemq.configuration.entity.adapter.NorthboundMappingEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.entity.adapter.TagEntity;
import com.hivemq.persistence.domain.DomainTag;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

@Singleton
public class ProtocolAdapterConfigConverter {

    private final @NotNull ProtocolAdapterFactoryManager factoryManager;
    private final @NotNull ObjectMapper mapper;

    @Inject
    public ProtocolAdapterConfigConverter(
            final @NotNull ProtocolAdapterFactoryManager factoryManager, final @NotNull ObjectMapper mapper) {
        this.factoryManager = factoryManager;
        this.mapper = mapper;
    }

    public @NotNull ProtocolAdapterConfig fromEntity(final @NotNull ProtocolAdapterEntity entity) {
        // we can assume that writing is enabled as the config for writing
        // should always include the config for reading as well.
        final ProtocolAdapterFactory<?> factory = getProtocolAdapterFactory(entity.getProtocolId());
        return new ProtocolAdapterConfig(
                entity.getAdapterId(),
                entity.getProtocolId(),
                entity.getConfigVersion(),
                factory.convertConfigObject(mapper, entity.getConfig(), true),
                entity.getSouthboundMappings().stream()
                        .map(southbound -> southbound.toPersistence(mapper))
                        .toList(),
                entity.getNorthboundMappings().stream()
                        .map(NorthboundMappingEntity::toPersistence)
                        .toList(),
                factory.convertTagDefinitionObjects(
                        mapper, entity.getTags().stream().map(TagEntity::toMap).toList()));
    }

    private @NotNull ProtocolAdapterFactory<?> getProtocolAdapterFactory(final @NotNull String protocolId) {
        return factoryManager
                .get(protocolId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No Factory was found for adapter with protocol id '" + protocolId + "'"));
    }

    public @NotNull <T extends Tag> T domainTagToTag(
            final @NotNull String protocolId, final @NotNull DomainTag domainTag) {
        //noinspection unchecked
        return (T) getProtocolAdapterFactory(protocolId).convertTagDefinitionObject(mapper, domainTag.toTagMap());
    }

    public @NotNull JsonNode convertTagDefinitionToJsonNode(final @NotNull TagDefinition tagDefinition) {
        return mapper.valueToTree(tagDefinition);
    }
}
