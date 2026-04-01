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
package com.hivemq.edge.knappogue;

import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.adapter.MqttUserPropertyEntity;
import com.hivemq.configuration.entity.adapter.NorthboundMappingEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.entity.adapter.TagEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.edge.compiler.lib.model.CompiledAdapterConfig;
import com.hivemq.edge.compiler.lib.model.CompiledConfig;
import com.hivemq.edge.compiler.lib.model.CompiledNorthboundMapping;
import com.hivemq.edge.compiler.lib.model.CompiledTag;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Translates a {@link CompiledConfig} (from the edge-compiler) into Edge's internal entity model and applies it via
 * the hot-reload path.
 *
 * <p>Only the adapter list is replaced. All other Edge config (listeners, MQTT settings, bridges, UNS, etc.) is left
 * intact — partial-replace semantics.
 */
public class CompiledConfigApplier {

    private static final @NotNull Logger log = LoggerFactory.getLogger(CompiledConfigApplier.class);

    private final @NotNull ConfigFileReaderWriter configFileReaderWriter;

    public CompiledConfigApplier(final @NotNull ConfigFileReaderWriter configFileReaderWriter) {
        this.configFileReaderWriter = configFileReaderWriter;
    }

    /**
     * Applies the compiled config to the running Edge instance.
     *
     * @return true if the config was applied successfully
     */
    public boolean apply(final @NotNull CompiledConfig compiledConfig) {
        if (!CompiledConfig.NOTICE.equals(compiledConfig.notice())) {
            log.error(
                    "Compiled config has unexpected _notice field: '{}'. Refusing to apply.", compiledConfig.notice());
            return false;
        }

        final List<ProtocolAdapterEntity> translatedAdapters = compiledConfig.protocolAdapters().stream()
                .map(this::translateAdapter)
                .toList();

        if (!compiledConfig.dataCombiners().isEmpty()) {
            log.warn(
                    "Compiled config contains {} data combiner(s) — data combiner translation is not yet implemented and will be skipped.",
                    compiledConfig.dataCombiners().size());
        }

        final HiveMQConfigEntity currentEntity = configFileReaderWriter.getCurrentConfigEntity();
        currentEntity.getProtocolAdapterConfig().clear();
        currentEntity.getProtocolAdapterConfig().addAll(translatedAdapters);

        final boolean success = configFileReaderWriter.applyCompiledConfig(currentEntity);
        if (success) {
            log.info(
                    "Applied compiled config: {} adapter(s) from edge version '{}'.",
                    translatedAdapters.size(),
                    compiledConfig.edgeVersion());
            configFileReaderWriter.writeConfigWithSync();
        } else {
            log.error("Failed to apply compiled config — see above for details.");
        }
        return success;
    }

    private @NotNull ProtocolAdapterEntity translateAdapter(final @NotNull CompiledAdapterConfig src) {
        final List<TagEntity> tags = src.tags().stream().map(this::translateTag).toList();
        final List<NorthboundMappingEntity> northbound = src.northboundMappings().stream()
                .map(this::translateNorthboundMapping)
                .toList();

        return new ProtocolAdapterEntity(
                src.adapterId(),
                src.protocolId(),
                1, // configVersion default — compiler omits this field
                src.config(),
                northbound,
                List.of(), // southboundMappings — empty for POC
                tags);
    }

    private @NotNull TagEntity translateTag(final @NotNull CompiledTag src) {
        return new TagEntity(src.name(), src.description(), src.definition());
    }

    private @NotNull NorthboundMappingEntity translateNorthboundMapping(final @NotNull CompiledNorthboundMapping src) {
        final List<MqttUserPropertyEntity> userProperties = src.userProperties().stream()
                .map(p -> new MqttUserPropertyEntity(p.name(), p.value()))
                .toList();
        return new NorthboundMappingEntity(
                src.tagName(),
                src.topic(),
                src.maxQos(),
                null, // messageHandlingOptions — ignored by NorthboundMappingEntity
                src.includeTagNames(),
                src.includeTimestamp(),
                src.includeMetadata(),
                userProperties,
                src.messageExpiryInterval());
    }
}
