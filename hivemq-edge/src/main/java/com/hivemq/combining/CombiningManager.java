package com.hivemq.combining;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.persistence.generic.AddResult;
import com.hivemq.protocols.ConfigPersistence;
import com.hivemq.protocols.ProtocolAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import static com.hivemq.persistence.util.FutureUtils.syncFuture;

@Singleton
public class CombiningManager {

    private static final Logger log = LoggerFactory.getLogger(CombiningManager.class);

    private final @NotNull ConfigPersistence configPersistence;
    private final @NotNull EventService eventService;
    private final @NotNull MetricRegistry metricRegistry;
    private final Map<String, DataCombiner> idToDataCombiner = new ConcurrentHashMap<>();

    @Inject
    public CombiningManager(
            final @NotNull ConfigPersistence configPersistence,
            final @NotNull EventService eventService,
            final @NotNull MetricRegistry metricRegistry) {
        this.configPersistence = configPersistence;
        this.eventService = eventService;
        this.metricRegistry = metricRegistry;
        // TODO move to HiveMQMetrics
        metricRegistry.registerGauge("com.hivemq.edge.data-combining.data-combiners.current",
                () -> idToDataCombiner.values().size());
    }


    public void start() {
        log.debug("Starting data combiners");
        for (final ProtocolAdapterConfig adapterConfig : configPersistence.allAdapters()) {

            // TODO start the actual data combining
        }
    }

    public @NotNull CompletableFuture<AddResult> startCombiner(final @NotNull DataCombiner dataCombiner) {
        log.debug("Starting data combiners with id '{}'", dataCombiner.id());
        // TODO
        return CompletableFuture.completedFuture(AddResult.success());
    }

    public @NotNull CompletableFuture<Void> stop(final @NotNull String dataCombinerId) {
        // TODO
        return CompletableFuture.completedFuture(null);
    }

    public @NotNull CompletableFuture<Void> stopAll() {
        // TODO
        return CompletableFuture.completedFuture(null);
    }

    public @NotNull CompletableFuture<AddResult> addDataCombiner(final @NotNull DataCombiner dataCombiner) {
        final DataCombiner previousValue = idToDataCombiner.putIfAbsent(dataCombiner.id().toString(), dataCombiner);
        if(previousValue!=null){
            return CompletableFuture.failedFuture(new IllegalArgumentException("data combiner already exists by id '" +
                    dataCombiner.id() +
                    "'"));
        }


        final CompletableFuture<AddResult> ret = startCombiner(dataCombiner);
        configPersistence.addDataCombiner(dataCombiner);
        return ret;
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
        return Optional.ofNullable(idToDataCombiner.get(id.toString()));
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
        if (idToDataCombiner.get(dataCombiner.id().toString()) != null) {
            throw new IllegalArgumentException("adapter already exists by id '" + dataCombiner.id() + "'");
        }
        idToDataCombiner.put(dataCombiner.id().toString(), dataCombiner);
    }


    private synchronized boolean deleteDataCombinerInternal(final @NotNull UUID id) {
        final DataCombiner dataCombiner = idToDataCombiner.remove(id.toString());
        if (dataCombiner != null) {
            try {
                // stop in any case as some resources must be cleaned up even if the adapter is still being started and is not yet in started state
                stop(dataCombiner.id().toString()).get();
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
