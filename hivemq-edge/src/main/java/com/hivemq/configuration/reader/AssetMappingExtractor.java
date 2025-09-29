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
import com.hivemq.combining.model.DataCombiner;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.combining.DataCombinerEntity;
import com.hivemq.configuration.entity.combining.DataCombiningEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AssetMappingExtractor
        implements ReloadableExtractor<List<@NotNull DataCombinerEntity>, List<@NotNull DataCombiner>> {

    private final @NotNull ConfigFileReaderWriter configFileReaderWriter;
    private volatile @NotNull List<DataCombinerEntity> config = List.of();
    private volatile @Nullable Consumer<List<@NotNull DataCombiner>> consumer =
            cfg -> log.debug("No consumer registered for Asset Mapping yet");

    public AssetMappingExtractor(final @NotNull ConfigFileReaderWriter configFileReaderWriter) {
        this.configFileReaderWriter = configFileReaderWriter;
    }

    @Override
    public boolean needsRestartWithConfig(final @NotNull HiveMQConfigEntity config) {
        return false;
    }

    @Override
    public @NotNull Configurator.ConfigResult updateConfig(final HiveMQConfigEntity config) {
        this.config = List.copyOf(config.getAssetMapperEntities());
        notifyConsumer();
        return Configurator.ConfigResult.SUCCESS;
    }

    public synchronized boolean updateDataCombiner(final @NotNull DataCombiner dataCombiner) {
        final var updated = new AtomicBoolean(false);
        final var newConfigs = config.stream().map(oldInstance -> {
            if (oldInstance.getId().equals(dataCombiner.id())) {
                updated.set(true);
                return dataCombiner.toPersistence();
            } else {
                return oldInstance;
            }
        }).toList();
        if (updated.get()) {
            replaceConfigsAndTriggerWrite(newConfigs);
            return true;
        }
        return false;
    }

    public synchronized boolean addDataCombiner(final @NotNull DataCombiner dataCombiner) {
        return getCombinerById(dataCombiner.id()).map(found -> {
            log.warn("Tried adding a data combiner with the same id {}", dataCombiner.id());
            return false;
        }).orElseGet(() -> {
            final var newConfigs = new ImmutableList.Builder<DataCombinerEntity>().addAll(config)
                    .add(dataCombiner.toPersistence())
                    .build();

            replaceConfigsAndTriggerWrite(newConfigs);
            return true;
        });
    }

    public Optional<DataCombiner> getCombinerById(final @NotNull UUID id) {
        return config.stream()
                .filter(oldInstance -> oldInstance.getId().equals(id))
                .findFirst()
                .map(DataCombiner::fromPersistence);
    }

    public @NotNull List<DataCombiner> getAllCombiners() {
        return config.stream().map(DataCombiner::fromPersistence).toList();
    }

    public synchronized boolean deleteDataCombiner(final @NotNull UUID dataCombinerId) {
        final var removed = new AtomicBoolean(false);
        final var newConfigs = config.stream().filter(combiner -> {
            if (combiner.getId().equals(dataCombinerId)) {
                removed.set(true);
                return false;
            }
            return true;
        }).toList();
        replaceConfigsAndTriggerWrite(newConfigs);
        return removed.get();
    }

    private void replaceConfigsAndTriggerWrite(final @NotNull List<@NotNull DataCombinerEntity> newConfigs) {
        config = newConfigs;
        notifyConsumer();
        configFileReaderWriter.writeConfigWithSync();
    }

    private void notifyConsumer() {
        final var consumer = this.consumer;
        if (consumer != null) {
            consumer.accept(config.stream().map(DataCombiner::fromPersistence).toList());
        }
    }

    @Override
    public void registerConsumer(final @NotNull Consumer<List<@NotNull DataCombiner>> consumer) {
        this.consumer = consumer;
        notifyConsumer();
    }

    @Override
    public void sync(final @NotNull HiveMQConfigEntity config) {
        config.getAssetMapperEntities().clear();
        config.getAssetMapperEntities().addAll(this.config);
    }

    public @NotNull Set<String> getMappingIdSet() {
        return config.stream()
                .flatMap(dataCombinerEntity -> dataCombinerEntity.getDataCombiningEntities()
                        .stream()
                        .map(DataCombiningEntity::getId))
                .map(UUID::toString)
                .collect(Collectors.toSet());
    }
}
