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
package com.hivemq.configuration.reader;

import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.entity.adapter.TagEntity;
import jakarta.xml.bind.ValidationEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ProtocolAdapterExtractor
        implements ReloadableExtractor<List<@NotNull ProtocolAdapterEntity>, List<@NotNull ProtocolAdapterEntity>> {

    private final @NotNull Set<String> tagNames;
    private final @NotNull ConfigFileReaderWriter configFileReaderWriter;
    private volatile @NotNull List<ProtocolAdapterEntity> allConfigs;
    private volatile @Nullable Consumer<List<@NotNull ProtocolAdapterEntity>> consumer;

    public ProtocolAdapterExtractor(final @NotNull ConfigFileReaderWriter configFileReaderWriter) {
        this.configFileReaderWriter = configFileReaderWriter;
        this.tagNames = new CopyOnWriteArraySet<>();
        this.allConfigs = List.of();
        this.consumer = cfg -> log.debug("No consumer registered yet");
    }

    public @NotNull List<ProtocolAdapterEntity> getAllConfigs() {
        return allConfigs;
    }

    public @NotNull Optional<ProtocolAdapterEntity> getAdapterByAdapterId(final @NotNull String adapterId) {
        return allConfigs.stream().filter(adapter -> adapter.getAdapterId().equals(adapterId)).findFirst();
    }

    @Override
    public synchronized Configurator.@NotNull ConfigResult updateConfig(final @NotNull HiveMQConfigEntity config) {
        final List<ValidationEvent> validationEvents = new ArrayList<>();
        final var newConfigs = List.copyOf(config.getProtocolAdapterConfig());
        newConfigs.forEach(entity -> entity.validate(validationEvents));
        final List<ValidationEvent> errorEvents = validationEvents.stream()
                .filter(event -> event.getSeverity() == ValidationEvent.FATAL_ERROR ||
                        event.getSeverity() == ValidationEvent.ERROR)
                .toList();
        if (!errorEvents.isEmpty()) {
            errorEvents.forEach(event -> log.error("Protocol adapter config error: {}", event.getMessage()));
            return Configurator.ConfigResult.ERROR;
        }
        return updateTagNames(newConfigs).map(duplicates -> Configurator.ConfigResult.ERROR).orElseGet(() -> {
            allConfigs = newConfigs;
            //We don'T write here because this method is triggered as the result of a write
            notifyConsumer();
            return Configurator.ConfigResult.SUCCESS;
        });
    }

    public synchronized @NotNull Configurator.ConfigResult updateAllAdapters(final @NotNull List<ProtocolAdapterEntity> adapterConfigs) {
        final var newConfigs = List.copyOf(adapterConfigs);
        return updateTagNames(newConfigs).map(duplicates -> Configurator.ConfigResult.ERROR).orElseGet(() -> {
            replaceConfigsAndTriggerWrite(newConfigs);
            return Configurator.ConfigResult.SUCCESS;
        });
    }

    private void replaceConfigsAndTriggerWrite(final @NotNull List<@NotNull ProtocolAdapterEntity> newConfigs) {
        allConfigs = newConfigs;
        notifyConsumer();
        configFileReaderWriter.writeConfigWithSync();
    }

    public synchronized boolean addAdapter(final @NotNull ProtocolAdapterEntity protocolAdapterConfig) {
        final var allConfigsTemp = List.copyOf(allConfigs);
        if (allConfigsTemp.stream().anyMatch(cfg -> protocolAdapterConfig.getAdapterId().equals(cfg.getAdapterId()))) {
            throw new IllegalArgumentException("adapter already exists by id '" +
                    protocolAdapterConfig.getProtocolId() +
                    "'");
        }
        return addTagNamesIfNoDuplicates(protocolAdapterConfig.getTags()).map(dupes -> {
            log.error("Found duplicated tag names: {}", dupes);
            return false;
        }).orElseGet(() -> {
            final var newConfigs = new ImmutableList.Builder<@NotNull ProtocolAdapterEntity>().addAll(allConfigsTemp)
                    .add(protocolAdapterConfig)
                    .build();
            replaceConfigsAndTriggerWrite(newConfigs);
            return true;
        });
    }

    public synchronized boolean updateAdapter(
            final @NotNull ProtocolAdapterEntity protocolAdapterConfig) {
        return updateAdapter(protocolAdapterConfig, true);
    }

    /**
     * Update adapter config, optionally triggering refresh.
     * When triggerRefresh=false, this is used for hot-reload operations where the adapter
     * has already been updated in-place and we only need to persist the config change.
     */
    public synchronized boolean updateAdapter(
            final @NotNull ProtocolAdapterEntity protocolAdapterConfig,
            final boolean triggerRefresh) {
        final var duplicateTags = new HashSet<String>();
        final var updated = new AtomicBoolean(false);
        final var newConfigs = allConfigs.stream().map(oldInstance -> {
            if (oldInstance.getAdapterId().equals(protocolAdapterConfig.getAdapterId())) {
                return replaceTagNamesIfNoDuplicates(oldInstance.getTags(),
                        protocolAdapterConfig.getTags()).map(dupes -> {
                    duplicateTags.addAll(dupes);
                    return oldInstance;
                }).orElseGet(() -> {
                    updated.set(true);
                    return protocolAdapterConfig;
                });
            } else {
                return oldInstance;
            }
        }).toList();
        if (updated.get()) {
            if (!duplicateTags.isEmpty()) {
                log.error("Found duplicated tag names: {}", duplicateTags);
                return false;
            } else {
                if (triggerRefresh) {
                    replaceConfigsAndTriggerWrite(newConfigs);
                } else {
                    // Hot-reload: update config without triggering refresh (adapter already updated)
                    replaceConfigsWithoutNotify(newConfigs);
                }
                return true;
            }
        }
        return false;
    }

    private void replaceConfigsWithoutNotify(final @NotNull List<@NotNull ProtocolAdapterEntity> newConfigs) {
        allConfigs = newConfigs;
        configFileReaderWriter.writeConfigWithSync();
    }

    public synchronized boolean deleteAdapter(final @NotNull String adapterId) {
        final var newConfigs = new ArrayList<>(allConfigs);
        final var deleted =
                allConfigs.stream().filter(config -> config.getAdapterId().equals(adapterId)).findFirst().map(found -> {
                    newConfigs.remove(found);
                    found.getTags().stream().map(TagEntity::getName).toList().forEach(tagNames::remove);
                    return true;
                }).orElse(false);

        if (deleted) {
            replaceConfigsAndTriggerWrite(List.copyOf(newConfigs));
            return true;
        }
        return false;
    }

    private void notifyConsumer() {
        final var consumer = this.consumer;
        if (consumer != null) {
            consumer.accept(allConfigs);
        }
    }

    private @NotNull Optional<Set<String>> updateTagNames(final @NotNull List<ProtocolAdapterEntity> entities) {
        final var newTagNames = new HashSet<String>();
        final var duplicates = new HashSet<String>();
        entities.stream().flatMap(cfg -> cfg.getTags().stream()).forEach(tag -> {
            if (newTagNames.contains(tag.getName())) {
                duplicates.add(tag.getName());
            } else {
                newTagNames.add(tag.getName());
            }
        });

        if (!duplicates.isEmpty()) {
            log.error("Duplicate tags detected while updating: {}", duplicates);
            return Optional.of(duplicates);
        }
        tagNames.clear();
        tagNames.addAll(newTagNames);
        return Optional.empty();
    }

    private @NotNull Optional<Set<String>> addTagNamesIfNoDuplicates(final @NotNull List<TagEntity> newTags) {
        final var newTagNames = new HashSet<String>();
        final var duplicates = new HashSet<String>();
        newTags.forEach(tag -> {
            if (tagNames.contains(tag.getName())) {
                duplicates.add(tag.getName());
            } else {
                newTagNames.add(tag.getName());
            }
        });

        if (!duplicates.isEmpty()) {
            log.error("Duplicate tags detected while adding: {}", duplicates);
            return Optional.of(duplicates);
        }
        tagNames.addAll(newTagNames);
        return Optional.empty();
    }

    private @NotNull Optional<Set<String>> replaceTagNamesIfNoDuplicates(
            final @NotNull List<TagEntity> oldTags,
            final @NotNull List<TagEntity> newTags) {
        final var newTagNames = new HashSet<String>();
        final var duplicates = new HashSet<String>();

        final var currentTagNames = new HashSet<>(tagNames);

        oldTags.stream().map(TagEntity::getName).toList().forEach(currentTagNames::remove);

        newTags.forEach(tag -> {
            if (currentTagNames.contains(tag.getName())) {
                duplicates.add(tag.getName());
            } else {
                newTagNames.add(tag.getName());
            }
        });
        if (!duplicates.isEmpty()) {
            log.error("Duplicate tags detected while replacing: {}", duplicates);
            return Optional.of(duplicates);
        }

        tagNames.addAll(newTagNames);
        return Optional.empty();
    }

    @Override
    public synchronized void registerConsumer(final @Nullable Consumer<List<@NotNull ProtocolAdapterEntity>> consumer) {
        this.consumer = consumer;
        notifyConsumer();
    }

    @Override
    public boolean needsRestartWithConfig(final @NotNull HiveMQConfigEntity config) {
        return false;
    }

    @Override
    public synchronized void sync(final @NotNull HiveMQConfigEntity config) {
        config.getProtocolAdapterConfig().clear();
        config.getProtocolAdapterConfig().addAll(allConfigs);
    }
}
