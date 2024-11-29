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

import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.persistence.mappings.NorthboundMapping;
import com.hivemq.persistence.mappings.SouthboundMapping;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ProtocolAdapterConfig {

    private final @NotNull ProtocolSpecificAdapterConfig adapterConfig;
    private final @NotNull List<? extends Tag> tags;
    private final @NotNull String adapterId;
    private final @NotNull String protocolId;
    private final @NotNull List<SouthboundMapping> southboundMappings;
    private final @NotNull List<NorthboundMapping> northboundMappings;

    public ProtocolAdapterConfig(
            final @NotNull String adapterId,
            final @NotNull String protocolId,
            final @NotNull ProtocolSpecificAdapterConfig protocolSpecificConfig,
            final @NotNull List<SouthboundMapping> southboundMappings,
            final @NotNull List<NorthboundMapping> northboundMappings,
            final @NotNull List<? extends Tag> tags) {
        this.adapterId = adapterId;
        this.protocolId = protocolId;
        this.southboundMappings = southboundMappings;
        this.northboundMappings = northboundMappings;
        this.adapterConfig = protocolSpecificConfig;
        this.tags = tags;
    }

    public @NotNull Optional<Set<String>> missingTags() {
        if (protocolId.equals("simulation")) {
            return Optional.empty();
        }

        final Set<String> names = new HashSet<>();
        southboundMappings.forEach(mapping -> names.add(mapping.getTagName()));
        northboundMappings.forEach(mapping -> names.add(mapping.getTagName()));

        this.tags.forEach(tag -> names.remove(tag.getName()));
        if (names.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(names);
        }
    }

    public @NotNull String getAdapterId() {
        return adapterId;
    }

    public @NotNull String getProtocolId() {
        return protocolId;
    }

    public @NotNull ProtocolSpecificAdapterConfig getAdapterConfig() {
        return adapterConfig;
    }

    public @NotNull List<? extends Tag> getTags() {
        return tags;
    }

    public @NotNull List<NorthboundMapping> getFromEdgeMappings() {
        return northboundMappings;
    }

    public @NotNull List<SouthboundMapping> getToEdgeMappings() {
        return southboundMappings;
    }
}
