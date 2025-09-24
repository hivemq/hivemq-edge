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

package com.hivemq.pulse.messaging;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Sets;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.combining.model.DataCombiner;
import com.hivemq.combining.model.DataCombining;
import com.hivemq.combining.model.EntityType;
import com.hivemq.combining.runtime.DataCombiningRuntime;
import com.hivemq.combining.runtime.DataCombiningRuntimeFactory;
import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.entity.pulse.PulseAssetEntity;
import com.hivemq.configuration.entity.pulse.PulseAssetMappingStatus;
import com.hivemq.configuration.entity.pulse.PulseEntity;
import com.hivemq.configuration.reader.AssetMappingExtractor;
import com.hivemq.configuration.reader.PulseExtractor;
import com.hivemq.pulse.utils.PulseAgentAssetUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.hivemq.metrics.HiveMQMetrics.ASSET_MAPPERS_COUNT_CURRENT;

@Singleton
public final class AssetMapperManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AssetMapperManager.class);

    private final @NotNull AssetMappingExtractor assetMappingExtractor;
    private final @NotNull DataCombiningRuntimeFactory dataCombiningRuntimeFactory;
    private final @NotNull EventService eventService;
    private final @NotNull Map<UUID, AssetMapperTask> idToAssetMapperTaskMap;
    private final @NotNull PulseExtractor pulseExtractor;

    @Inject
    public AssetMapperManager(
            final @NotNull EventService eventService,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull DataCombiningRuntimeFactory dataCombiningRuntimeFactory,
            final @NotNull ShutdownHooks shutdownHooks,
            final @NotNull AssetMappingExtractor assetMappingExtractor,
            final @NotNull PulseExtractor pulseExtractor) {
        idToAssetMapperTaskMap = new ConcurrentHashMap<>();
        this.assetMappingExtractor = assetMappingExtractor;
        this.dataCombiningRuntimeFactory = dataCombiningRuntimeFactory;
        this.eventService = eventService;
        this.pulseExtractor = pulseExtractor;
        metricRegistry.registerGauge(ASSET_MAPPERS_COUNT_CURRENT.name(), idToAssetMapperTaskMap::size);
        shutdownHooks.add(new HiveMQShutdownHook() {
            @Override
            public @NotNull String name() {
                return "Pulse Asset Mapper Manager Shutdown";
            }

            @Override
            public void run() {
                try {
                    stopAll().get();
                } catch (final InterruptedException e) {
                    LOGGER.warn("Shutdown of AssetMapperManager was interrupted.");
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                } catch (final ExecutionException e) {
                    LOGGER.warn("Exception was thrown during the shutdown of AssetMapperManager:", e);
                }
            }
        });
    }

    public void start() {
        LOGGER.debug("Starting Pulse Asset Mapper Manager");
        assetMappingExtractor.registerConsumer(this::refresh);
    }

    public void refresh(final @NotNull List<DataCombiner> assetMappers) {
        LOGGER.info("Refreshing Pulse Asset Mappers");
        final PulseEntity pulseEntity = pulseExtractor.getPulseEntity();
        synchronized (pulseEntity.getLock()) {
            final Set<UUID> assetMapperIdSet = idToAssetMapperTaskMap.keySet();
            LOGGER.info("AssetMappers: {}", assetMapperIdSet);
            // Let's filter out non-streaming asset mappers.
            final Map<String, PulseAssetEntity> assetEntityMap = PulseAgentAssetUtils.toAssetEntityMap(pulseEntity);
            final @NotNull Map<UUID, DataCombiner> newAssetMapperMap = assetMappers.stream()
                    .filter(dataCombiner -> IntStream.range(0,
                                    Math.min(dataCombiner.entityReferences().size(), dataCombiner.dataCombinings().size()))
                            .noneMatch(i -> {
                                if (dataCombiner.entityReferences().get(i).type() == EntityType.PULSE_AGENT) {
                                    final DataCombining dataCombining = dataCombiner.dataCombinings().get(i);
                                    final PulseAssetEntity asset = assetEntityMap.get(dataCombining.id().toString());
                                    if (asset == null) {
                                        LOGGER.warn("Pulse asset '{}' is not found.", dataCombining.id());
                                        return true;
                                    }
                                    return asset.getMapping().getId() == null ||
                                            asset.getMapping().getStatus() != PulseAssetMappingStatus.STREAMING;
                                }
                                return false;
                            }))
                    .collect(Collectors.toMap(DataCombiner::id, Function.identity()));
            // Let's determine what to be created, updated, deleted.
            final Set<UUID> newAssetMapperIdSet = newAssetMapperMap.keySet();
            final Set<UUID> toBeDeletedAssetMapperIdSet = Sets.difference(assetMapperIdSet, newAssetMapperIdSet);
            final Set<UUID> toBeCreatedAssetMapperIdSet = Sets.difference(newAssetMapperIdSet, assetMapperIdSet);
            final Set<UUID> toBeUpdatedAssetMapperIdSet = Sets.intersection(newAssetMapperIdSet, assetMapperIdSet);
            final List<UUID> failedDataCombiners = new ArrayList<>();
            // Delete
            toBeDeletedAssetMapperIdSet.forEach(uuid -> {
                try {
                    LOGGER.debug("Deleting asset mapper '{}'", uuid);
                    final AssetMapperTask assetMapperTask = idToAssetMapperTaskMap.get(uuid);
                    internalDeleteDataCombiner(uuid);
                    eventService.createCombinerEvent(assetMapperTask.dataCombiner().id())
                            .withSeverity(Event.SEVERITY.INFO)
                            .withMessage(String.format("Asset mapper '%s' was permanently deleted.",
                                    assetMapperTask.getName()))
                            .fire();
                } catch (final Exception e) {
                    failedDataCombiners.add(uuid);
                    LOGGER.error("Failed deleting asset mapper {}", uuid, e);
                }
            });
            // Create
            toBeCreatedAssetMapperIdSet.forEach(uuid -> {
                try {
                    LOGGER.debug("Creating asset mapper '{}'", uuid);
                    final DataCombiner dataCombiner = newAssetMapperMap.get(uuid);
                    internalCreateDataCombiner(dataCombiner);
                    eventService.createCombinerEvent(dataCombiner.id())
                            .withSeverity(Event.SEVERITY.INFO)
                            .withMessage(String.format("Asset mapper '%s' was successfully created.",
                                    AssetMapperTask.getName(dataCombiner)))
                            .fire();
                } catch (final Exception e) {
                    failedDataCombiners.add(uuid);
                    LOGGER.error("Failed adding asset mapper {}", uuid, e);
                }
            });
            // Update
            toBeUpdatedAssetMapperIdSet.forEach(uuid -> {
                try {
                    LOGGER.debug("Updating asset mapper '{}'", uuid);
                    final DataCombiner dataCombiner = newAssetMapperMap.get(uuid);
                    internalUpdateAssetMapper(dataCombiner);
                    eventService.createCombinerEvent(dataCombiner.id())
                            .withSeverity(Event.SEVERITY.INFO)
                            .withMessage(String.format("Asset mapper '%s' was successfully updated.",
                                    AssetMapperTask.getName(dataCombiner)))
                            .fire();
                } catch (final Exception e) {
                    failedDataCombiners.add(uuid);
                    LOGGER.error("Failed updating asset mapper {}", uuid, e);
                }
            });
            // Error reporting.
            if (failedDataCombiners.isEmpty()) {
                eventService.configurationEvent()
                        .withSeverity(Event.SEVERITY.INFO)
                        .withMessage("Configuration has been successfully updated")
                        .fire();
            } else {
                eventService.configurationEvent()
                        .withSeverity(Event.SEVERITY.CRITICAL)
                        .withMessage("Reloading of configuration failed")
                        .fire();
            }
        }
    }

    public @NotNull CompletableFuture<Void> stopAll() {
        synchronized (pulseExtractor.getPulseEntity().getLock()) {
            return CompletableFuture.runAsync(() -> {
                idToAssetMapperTaskMap.values()
                        .stream()
                        .map(AssetMapperTask::dataCombiningRuntimes)
                        .flatMap(Collection::stream)
                        .forEach(DataCombiningRuntime::stop);
            });
        }
    }

    private void internalUpdateAssetMapper(final @NotNull DataCombiner dataCombiner) {
        LOGGER.debug("Updating asset mapper '{}'", dataCombiner.id());
        internalDeleteDataCombiner(dataCombiner.id());
        internalCreateDataCombiner(dataCombiner);
    }


    private void internalCreateDataCombiner(final @NotNull DataCombiner dataCombiner) {
        LOGGER.debug("Creating asset mapper '{}'", dataCombiner.id());
        if (idToAssetMapperTaskMap.get(dataCombiner.id()) != null) {
            throw new IllegalArgumentException("Data combiner already exists by id '" + dataCombiner.id() + "'");
        }
        final List<DataCombiningRuntime> dataCombiningRuntimes = createDataCombiningStates(dataCombiner);
        idToAssetMapperTaskMap.put(dataCombiner.id(), new AssetMapperTask(dataCombiner, dataCombiningRuntimes));
        dataCombiningRuntimes.forEach(DataCombiningRuntime::start);
    }

    private void internalDeleteDataCombiner(final @NotNull UUID id) {
        LOGGER.debug("Deleting asset mapper '{}'", id);
        final AssetMapperTask assetMapperTask = idToAssetMapperTaskMap.remove(id);
        if (assetMapperTask != null) {
            try {
                stop(assetMapperTask).get();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (final ExecutionException e) {
                throw new RuntimeException(e);
            }
        } else {
            LOGGER.error("Tried removing non existing asset mapper '{}'", id);
        }
    }

    private @NotNull CompletableFuture<Void> stop(final @NotNull AssetMapperManager.AssetMapperTask assetMapperTask) {
        synchronized (pulseExtractor.getPulseEntity().getLock()) {
            // stopping is fast no reason for async
            assetMapperTask.dataCombiningRuntimes().forEach(DataCombiningRuntime::stop);
        }
        return CompletableFuture.completedFuture(null);
    }

    private @NotNull List<DataCombiningRuntime> createDataCombiningStates(final DataCombiner dataCombiner) {
        return dataCombiner.dataCombinings().stream().map(dataCombiningRuntimeFactory::build).toList();
    }

    record AssetMapperTask(DataCombiner dataCombiner, List<DataCombiningRuntime> dataCombiningRuntimes) {
        public static @NotNull String getName(final @NotNull DataCombiner dataCombiner) {
            final String name = dataCombiner.name();
            return name != null && !name.isEmpty() ? name : dataCombiner.id().toString();
        }

        public @NotNull String getName() {
            return getName(dataCombiner);
        }
    }
}
