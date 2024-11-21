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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ProtocolAdapterConfig {

    private static final Logger log = LoggerFactory.getLogger(ProtocolAdapterConfig.class);

    private final @NotNull ProtocolSpecificAdapterConfig adapterConfig;
    private final @NotNull List<? extends Tag> tags;
    private final @NotNull String adapterId;
    private final @NotNull String protocolId;
    private final @NotNull List<ToEdgeMapping> toEdgeMappings;
    private final @NotNull List<FromEdgeMapping> fromEdgeMappings;

    public ProtocolAdapterConfig(
            final @NotNull String adapterId,
            final @NotNull String protocolId,
            final @NotNull ProtocolSpecificAdapterConfig protocolSpecificConfig,
            final @NotNull List<ToEdgeMapping> toEdgeMappings,
            final @NotNull List<FromEdgeMapping> fromEdgeMappings,
            final @NotNull List<? extends Tag> tags) {
        this.adapterId = adapterId;
        this.protocolId = protocolId;
        this.toEdgeMappings = toEdgeMappings;
        this.fromEdgeMappings = fromEdgeMappings;
        this.adapterConfig = protocolSpecificConfig;
        this.tags = tags;
    }

    public @NotNull Optional<Set<String>> missingTags() {
        final Set<String> names = this.tags.stream().map(Tag::getName).collect(Collectors.toSet());
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

    public @NotNull List<FromEdgeMapping> getFromEdgeMappings() {
        return fromEdgeMappings;
    }

    public @NotNull List<ToEdgeMapping> getToEdgeMappings() {
        return toEdgeMappings;
    }
}
