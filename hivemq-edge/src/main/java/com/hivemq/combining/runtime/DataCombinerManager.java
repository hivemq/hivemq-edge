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
import com.hivemq.edge.modules.adapters.data.TagManager;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
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
import static com.hivemq.persistence.util.FutureUtils.syncFuture;

@Singleton
public class DataCombinerManager {

    private static final Logger log = LoggerFactory.getLogger(DataCombinerManager.class);

    private final @NotNull ConfigPersistence configPersistence;
    private final @NotNull EventService eventService;
    private final @NotNull MetricRegistry metricRegistry;
    private final @NotNull LocalTopicTree localTopicTree;
    private final @NotNull TagManager tagManager;
    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull SingleWriterService singleWriterService;
    private final @NotNull DataCombiningPublishService dataCombiningPublishService;
    private final Map<UUID, DataCombiner> idToDataCombiner = new ConcurrentHashMap<>();
    private final Map<UUID, List<DataCombiningRuntime>> idToDataCombiningStates = new ConcurrentHashMap<>();


    @Inject
    public DataCombinerManager(
            final @NotNull ConfigPersistence configPersistence,
            final @NotNull EventService eventService,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull LocalTopicTree localTopicTree,
            final @NotNull TagManager tagManager,
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull SingleWriterService singleWriterService,
            final @NotNull DataCombiningPublishService dataCombiningPublishService) {
        this.configPersistence = configPersistence;
        this.eventService = eventService;
        this.metricRegistry = metricRegistry;
        this.localTopicTree = localTopicTree;
        this.tagManager = tagManager;
        this.clientQueuePersistence = clientQueuePersistence;
        this.singleWriterService = singleWriterService;
        this.dataCombiningPublishService = dataCombiningPublishService;
        metricRegistry.registerGauge(DATA_COMBINERS_COUNT_CURRENT.name(), () -> idToDataCombiner.values().size());
    }

    public void start() {
        log.debug("Starting data combiners");
        configPersistence.allDataCombiners()
                .forEach(dataCombiner -> idToDataCombiningStates.put(dataCombiner.id(),
                        createDataCombiningStates(dataCombiner)));

        idToDataCombiningStates.values().stream().flatMap(Collection::stream).forEach(DataCombiningRuntime::start);
    }

    private @NotNull List<DataCombiningRuntime> createDataCombiningStates(final DataCombiner dataCombiner) {
        final List<DataCombiningRuntime> dataCombiningRuntimes = dataCombiner.dataCombinings()
                .stream()
                .map(dataCombining -> new DataCombiningRuntime(dataCombining,
                        localTopicTree,
                        tagManager,
                        clientQueuePersistence,
                        singleWriterService,
                        dataCombiningPublishService))
                .toList();
        return dataCombiningRuntimes;
    }

    public @NotNull CompletableFuture<AddResult> startCombiner(final @NotNull DataCombiner dataCombiner) {
        log.debug("Starting data combiner with id '{}'", dataCombiner.id());
        if (idToDataCombiner.putIfAbsent(dataCombiner.id(), dataCombiner) != null) {
            return CompletableFuture.completedFuture(AddResult.failed(AddResult.PutStatus.ALREADY_EXISTS));
        }
        return CompletableFuture.runAsync(() -> {
            final List<DataCombiningRuntime> dataCombiningRuntimes = createDataCombiningStates(dataCombiner);
            dataCombiningRuntimes.forEach(DataCombiningRuntime::start);
            idToDataCombiningStates.put(dataCombiner.id(), dataCombiningRuntimes);
        }).thenApply(ignored -> AddResult.success());
    }

    public @NotNull CompletableFuture<Void> stop(final @NotNull UUID dataCombinerId) {
        final var toBeStopped = idToDataCombiningStates.remove(dataCombinerId);
        if (toBeStopped != null) {
            return CompletableFuture.runAsync(() -> toBeStopped.forEach(DataCombiningRuntime::stop));
        }
        return CompletableFuture.completedFuture(null);
    }

    public @NotNull CompletableFuture<Void> stopAll() {
        return CompletableFuture.runAsync(() -> {
            idToDataCombiningStates.values().stream().flatMap(Collection::stream).forEach(DataCombiningRuntime::stop);
        });
    }

    public @NotNull CompletableFuture<AddResult> addDataCombiner(final @NotNull DataCombiner dataCombiner) {
        final DataCombiner previousValue = idToDataCombiner.putIfAbsent(dataCombiner.id(), dataCombiner);
        if (previousValue != null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("data combiner already exists by id '" +
                    dataCombiner.id() +
                    "'"));
        }
        return CompletableFuture.runAsync(() -> {
            configPersistence.addDataCombiner(dataCombiner);
            final List<DataCombiningRuntime> dataCombiningRuntimes = createDataCombiningStates(dataCombiner);
            dataCombiningRuntimes.forEach(DataCombiningRuntime::start);
            idToDataCombiningStates.put(dataCombiner.id(), dataCombiningRuntimes);
        }).thenApply(ignored -> AddResult.success());
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

    public @NotNull Optional<DataCombiner> getCombinerById(final @NotNull UUID id) {
        return Optional.ofNullable(idToDataCombiner.get(id));
    }

    public @NotNull List<DataCombiner> getAllCombiners() {
        return idToDataCombiner.values().stream().toList();
    }


    private void internalUpdateDataCombiner(final DataCombiner dataCombiner) {
        deleteDataCombinerInternal(dataCombiner.id());
        createDataCombinerInternal(dataCombiner);
        syncFuture(startCombiner(dataCombiner));
        configPersistence.updateDataCombiner(dataCombiner);
    }


    private synchronized void createDataCombinerInternal(final @NotNull DataCombiner dataCombiner) {
        if (idToDataCombiner.get(dataCombiner.id()) != null) {
            throw new IllegalArgumentException("adapter already exists by id '" + dataCombiner.id() + "'");
        }
        idToDataCombiner.put(dataCombiner.id(), dataCombiner);
    }


    private synchronized boolean deleteDataCombinerInternal(final @NotNull UUID id) {
        final DataCombiner dataCombiner = idToDataCombiner.remove(id);
        if (dataCombiner != null) {
            try {
                // stop in any case as some resources must be cleaned up even if the adapter is still being started and is not yet in started state
                stop(dataCombiner.id()).get();
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


}
