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
package com.hivemq.combining.runtime;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.combining.model.DataCombiner;
import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.reader.DataCombiningExtractor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.hivemq.metrics.HiveMQMetrics.DATA_COMBINERS_COUNT_CURRENT;

@Singleton
public class DataCombinerManager {

    private static final Logger log = LoggerFactory.getLogger(DataCombinerManager.class);

    private final @NotNull DataCombiningExtractor dataCombiningConfig;
    private final @NotNull EventService eventService;
    private final @NotNull DataCombiningRuntimeFactory dataCombiningRuntimeFactory;
    private final @NotNull Map<UUID, DataCombiningInformation> idToDataCombiningInformation = new ConcurrentHashMap<>();

    @Inject
    public DataCombinerManager(
            final @NotNull EventService eventService,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull DataCombiningRuntimeFactory dataCombiningRuntimeFactory,
            final @NotNull ShutdownHooks shutdownHooks,
            final @NotNull DataCombiningExtractor dataCombiningConfig) {
        this.dataCombiningConfig = dataCombiningConfig;
        this.eventService = eventService;
        this.dataCombiningRuntimeFactory = dataCombiningRuntimeFactory;
        metricRegistry.registerGauge(DATA_COMBINERS_COUNT_CURRENT.name(),
                () -> idToDataCombiningInformation.values().size());
        shutdownHooks.add(new HiveMQShutdownHook() {
            @Override
            public @NotNull String name() {
                return "Data Combiner Manager Shutdown";
            }

            @Override
            public void run() {
                try {
                    stopAll().get();
                } catch (final InterruptedException e) {
                    log.warn("Shutdown of DataCombinerManager was interrupted.");
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                } catch (final ExecutionException e) {
                    log.warn("Exception was thrown during the shutdown of DataCombinerManager:", e);
                }
            }
        });
    }

    public void start() {
        log.debug("Starting data combiners");
        dataCombiningConfig.registerConsumer(this::refresh);
    }

    private void refresh(List<DataCombiner> configs) {
        log.info("Refreshing data combiners");

        log.info("DataCombiners: {}", idToDataCombiningInformation.keySet());

        final Map<UUID, DataCombiner> mapOfNewCombinersByUUID = configs
                .stream()
                .collect(Collectors.toMap(DataCombiner::id, Function.identity()));

        final List<UUID> listOfExisitingCombiners = new ArrayList<>(idToDataCombiningInformation.keySet());

        final List<UUID> combinersToBeDeleted = new ArrayList<>(listOfExisitingCombiners);
        combinersToBeDeleted.removeAll(mapOfNewCombinersByUUID.keySet());

        final List<UUID> combinersToBeCreated = new ArrayList<>(mapOfNewCombinersByUUID.keySet());
        combinersToBeCreated.removeAll(listOfExisitingCombiners);

        final List<UUID> combinersToBeUpdated = new ArrayList<>(idToDataCombiningInformation.keySet());
        combinersToBeUpdated.removeAll(combinersToBeCreated);
        combinersToBeUpdated.removeAll(combinersToBeDeleted);

        final List<UUID> failedDataCombiners = new ArrayList<>();

        combinersToBeDeleted.forEach(uuid -> {
            try {
                log.debug("Deleting data combiner '{}'", uuid);
                var dataCombiner = mapOfNewCombinersByUUID.get(uuid);
                deleteDataCombinerInternal(uuid);
                var dataCombinerName = dataCombiner.name();
                var nameOrId = dataCombinerName != null && !dataCombinerName.isEmpty() ? dataCombinerName : dataCombiner.id();
                eventService.createCombinerEvent(dataCombiner.id())
                        .withSeverity(Event.SEVERITY.INFO)
                        .withMessage(String.format("Combiner '%s' was permanently deleted.", nameOrId))
                        .fire();
            } catch (final Exception e) {
                failedDataCombiners.add(uuid);
                log.error("Failed deleting data combiner {}", uuid, e);
            }
        });

        combinersToBeCreated.forEach(uuid -> {
            try {
                log.debug("Creating data combiner '{}'", uuid);
                var dataCombiner = mapOfNewCombinersByUUID.get(uuid);
                createDataCombinerInternal(dataCombiner);
                var dataCombinerName = dataCombiner.name();
                var nameOrId = dataCombinerName != null && !dataCombinerName.isEmpty() ? dataCombinerName : dataCombiner.id();
                eventService.createCombinerEvent(dataCombiner.id())
                        .withSeverity(Event.SEVERITY.INFO)
                        .withMessage(String.format("Combiner '%s' was successfully created.", nameOrId))
                        .fire();
            } catch (final Exception e) {
                failedDataCombiners.add(uuid);
                log.error("Failed adding data combiner {}", uuid, e);
            }
        });

        combinersToBeUpdated.forEach(uuid -> {
            try {
                log.debug("Updating data combiner '{}'", uuid);
                var dataCombiner = mapOfNewCombinersByUUID.get(uuid);
                internalUpdateDataCombiner(mapOfNewCombinersByUUID.get(uuid));
                var dataCombinerName = dataCombiner.name();
                var nameOrId = dataCombinerName != null && !dataCombinerName.isEmpty() ? dataCombinerName : dataCombiner.id();
                eventService.createCombinerEvent(dataCombiner.id())
                        .withSeverity(Event.SEVERITY.INFO)
                        .withMessage(String.format("Combiner '%s' was successfully updated.", nameOrId))
                        .fire();
            } catch (final Exception e) {
                failedDataCombiners.add(uuid);
                log.error("Failed updating data combiner {}", uuid, e);
            }

        });

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

    public synchronized @NotNull CompletableFuture<Void> stopAll() {
        return CompletableFuture.runAsync(() -> {
            idToDataCombiningInformation.values()
                    .stream()
                    .map(DataCombiningInformation::dataCombiningRuntimes)
                    .flatMap(Collection::stream)
                    .forEach(DataCombiningRuntime::stop);
        });
    }


    private void internalUpdateDataCombiner(final DataCombiner dataCombiner) {
        log.debug("Updating data combiner '{}'", dataCombiner.id());
        deleteDataCombinerInternal(dataCombiner.id());
        createDataCombinerInternal(dataCombiner);
    }


    private synchronized void createDataCombinerInternal(final @NotNull DataCombiner dataCombiner) {
        log.debug("Creating data combiner '{}'", dataCombiner.id());
        if (idToDataCombiningInformation.get(dataCombiner.id()) != null) {
            throw new IllegalArgumentException("Data combiner already exists by id '" + dataCombiner.id() + "'");
        }

        final List<DataCombiningRuntime> dataCombiningRuntimes = createDataCombiningStates(dataCombiner);
        idToDataCombiningInformation.put(dataCombiner.id(),
                new DataCombiningInformation(dataCombiner, dataCombiningRuntimes));

        dataCombiningRuntimes.forEach(DataCombiningRuntime::start);
    }


    private synchronized boolean deleteDataCombinerInternal(final @NotNull UUID id) {
        log.debug("Deleting data combiner '{}'", id);
        final DataCombiningInformation dataCombiningInformation = idToDataCombiningInformation.remove(id);
        if (dataCombiningInformation != null) {
            try {
                stop(dataCombiningInformation).get();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (final ExecutionException e) {
                throw new RuntimeException(e);
            }
        } else {
            log.error("Tried removing non existing data combiner '{}'", id);
        }
        return false;
    }

    private synchronized @NotNull CompletableFuture<Void> stop(final @NotNull DataCombiningInformation toBeStopped) {
        // stopping is fast no reason for async
        toBeStopped.dataCombiningRuntimes().forEach(DataCombiningRuntime::stop);
        return CompletableFuture.completedFuture(null);
    }

    private @NotNull List<DataCombiningRuntime> createDataCombiningStates(final DataCombiner dataCombiner) {
        return dataCombiner.dataCombinings().stream().map(dataCombiningRuntimeFactory::build).toList();
    }

    record DataCombiningInformation(DataCombiner dataCombiner, List<DataCombiningRuntime> dataCombiningRuntimes) {
    }

}
