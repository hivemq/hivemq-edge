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
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ProtocolAdapterConfigPersistence {

    private @NotNull final ProtocolAdapterConfig adapterConfig;
    private @NotNull final List<? extends Tag> tags;

    public ProtocolAdapterConfigPersistence(
            @NotNull final ProtocolAdapterConfig adapterConfig,
            @NotNull final List<? extends Tag> tags) {
        this.adapterConfig = adapterConfig;
        this.tags = tags;
    }

    public static ProtocolAdapterConfigPersistence fromAdapterConfigMap(@NotNull final Map<String, Object> adapterConfig,
                                                                        final boolean writingEnabled,
                                                                        @NotNull final ObjectMapper mapper,
                                                                        @NotNull final ProtocolAdapterFactory protocolAdapterFactory) {
        return fromMaps((Map<String, Object>)adapterConfig.get("config"), (List<Map<String, Object>>)adapterConfig.get("tags"), writingEnabled, mapper, protocolAdapterFactory);
    }

    public static ProtocolAdapterConfigPersistence fromMaps(@NotNull final Map<String, Object> adapterConfig,
                                                            @NotNull final List<Map<String, Object>> tagMaps,
                                                            final boolean writingEnabled,
                                                            @NotNull final ObjectMapper mapper,
                                                            @NotNull final ProtocolAdapterFactory protocolAdapterFactory) {

        return new ProtocolAdapterConfigPersistence(
                protocolAdapterFactory.convertConfigObject(mapper, (Map<String, Object>)adapterConfig.get("config"), writingEnabled),
                protocolAdapterFactory.convertTagDefinitionObjects(mapper, (List<Map<String, Object>>)adapterConfig.get("tags"))
        );
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
