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
import com.hivemq.persistence.generic.AddResult;
import com.hivemq.protocols.ConfigPersistence;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import static com.hivemq.metrics.HiveMQMetrics.DATA_COMBINERS_COUNT_CURRENT;

@Singleton
public class DataCombinerManager {

    private static final Logger log = LoggerFactory.getLogger(DataCombinerManager.class);

    private final @NotNull ConfigPersistence configPersistence;
    private final @NotNull EventService eventService;
    private final @NotNull DataCombiningRuntimeFactory dataCombiningRuntimeFactory;
    private final @NotNull Map<UUID, DataCombiningInformation> idToDataCombiningInformation = new ConcurrentHashMap<>();

    @Inject
    public DataCombinerManager(
            final @NotNull ConfigPersistence configPersistence,
            final @NotNull EventService eventService,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull DataCombiningRuntimeFactory dataCombiningRuntimeFactory,
            final @NotNull ShutdownHooks shutdownHooks) {
        this.configPersistence = configPersistence;
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
                stopAll();
            }
        });
    }

    public void start() {
        log.debug("Starting data combiners");
        configPersistence.allDataCombiners().forEach(dataCombiner -> {
            idToDataCombiningInformation.put(dataCombiner.id(),
                    new DataCombiningInformation(dataCombiner, createDataCombiningStates(dataCombiner)));
        });
        idToDataCombiningInformation.values()
                .stream()
                .map(DataCombiningInformation::dataCombiningRuntimes)
                .flatMap(Collection::stream)
                .forEach(DataCombiningRuntime::start);
    }


    public synchronized @NotNull CompletableFuture<AddResult> addDataCombiner(final @NotNull DataCombiner dataCombiner) {
        final DataCombiningInformation previousValue = idToDataCombiningInformation.get(dataCombiner.id());
        if (previousValue != null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("data combiner already exists by id '" +
                    dataCombiner.id() +
                    "'"));
        }


        createDataCombinerInternal(dataCombiner);
        return CompletableFuture.completedFuture(AddResult.success());
    }

    public boolean updateDataCombiner(final @NotNull DataCombiner dataCombining) {
        return getCombinerById(dataCombining.id()).map(oldInstance -> {
            internalUpdateDataCombiner(dataCombining);
            return true;
        }).orElse(false);
    }


    public @NotNull CompletableFuture<Boolean> deleteDataCombiner(final @NotNull UUID combinerId) {
        final boolean deleted = deleteDataCombinerInternal(combinerId);
        return CompletableFuture.completedFuture(deleted);
    }

    public synchronized @NotNull CompletableFuture<Void> stop(final @NotNull UUID dataCombinerId) {
        final var toBeStopped = idToDataCombiningInformation.remove(dataCombinerId);
        if (toBeStopped != null) {
            return CompletableFuture.runAsync(() -> toBeStopped.dataCombiningRuntimes()
                    .forEach(DataCombiningRuntime::stop));
        }
        return CompletableFuture.completedFuture(null);
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

    public @NotNull Optional<DataCombiner> getCombinerById(final @NotNull UUID id) {
        final DataCombiningInformation dataCombiningInformation = idToDataCombiningInformation.get(id);
        if (dataCombiningInformation == null) {
            return Optional.empty();
        } else {
            return Optional.of(dataCombiningInformation.dataCombiner());
        }
    }

    public @NotNull List<DataCombiner> getAllCombiners() {
        return idToDataCombiningInformation.values().stream().map(DataCombiningInformation::dataCombiner).toList();
    }


    private void internalUpdateDataCombiner(final DataCombiner dataCombiner) {
        deleteDataCombinerInternal(dataCombiner.id());
        createDataCombinerInternal(dataCombiner);
        configPersistence.updateDataCombiner(dataCombiner);
    }


    private synchronized void createDataCombinerInternal(final @NotNull DataCombiner dataCombiner) {
        if (idToDataCombiningInformation.get(dataCombiner.id()) != null) {
            throw new IllegalArgumentException("adapter already exists by id '" + dataCombiner.id() + "'");
        }

        final List<DataCombiningRuntime> dataCombiningRuntimes = createDataCombiningStates(dataCombiner);
        idToDataCombiningInformation.put(dataCombiner.id(),
                new DataCombiningInformation(dataCombiner, dataCombiningRuntimes));
        dataCombiningRuntimes.forEach(DataCombiningRuntime::start);
        configPersistence.addDataCombiner(dataCombiner);
    }


    private synchronized boolean deleteDataCombinerInternal(final @NotNull UUID id) {
        final DataCombiningInformation dataCombiningInformation = idToDataCombiningInformation.remove(id);
        if (dataCombiningInformation != null) {
            try {
                // stop in any case as some resources must be cleaned up even if the adapter is still being started and is not yet in started state
                stop(dataCombiningInformation.dataCombiner().id()).get();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (final ExecutionException e) {
                throw new RuntimeException(e);
            }

            eventService.createDataCombiningEvent(id)
                    .withSeverity(Event.SEVERITY.WARN)
                    .withMessage(String.format("Data Combininh '%s' was deleted from the system permanently.", id))
                    .fire();
        } else {
            log.error("Tried removing non existing adapter '{}'", id);
        }
        return false;
    }

    private @NotNull List<DataCombiningRuntime> createDataCombiningStates(final DataCombiner dataCombiner) {
        return dataCombiner.dataCombinings()
                .stream()
                .map(dataCombiningRuntimeFactory::build)
                .toList();
    }

    record DataCombiningInformation(DataCombiner dataCombiner, List<DataCombiningRuntime> dataCombiningRuntimes) {
    }

}
