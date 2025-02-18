package com.hivemq.combining;

import com.google.common.base.Preconditions;
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
import java.util.Collection;
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
    private final Map<UUID, DataCombiner> idToDataCombiner = new ConcurrentHashMap<>();

    @Inject
    public CombiningManager(
            final @NotNull ConfigPersistence configPersistence, final @NotNull EventService eventService) {
        this.configPersistence = configPersistence;
        this.eventService = eventService;
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

    public @NotNull CompletableFuture<Void> stop(final @NotNull DataCombiner dataCombiner) {
        // TODO
        return CompletableFuture.completedFuture(null);
    }

    public @NotNull CompletableFuture<Void> stopAll() {
        // TODO
        return CompletableFuture.completedFuture(null);
    }

    public @NotNull CompletableFuture<AddResult> addDataCombiner(final @NotNull DataCombiner dataCombiner) {
        if (getCombinerById(dataCombiner.id()).isPresent()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("data combiner already exists by id '" +
                    dataCombiner.id() +
                    "'"));
        }

        idToDataCombiner.put(dataCombiner.id(), dataCombiner);
        final CompletableFuture<AddResult> ret = startCombiner(dataCombiner);
        configPersistence.addDataCombiner(dataCombiner);
        return ret;
    }

    public boolean updateDataCombiner(final @NotNull DataCombiner dataCombining) {
        Preconditions.checkNotNull(dataCombining);
        return getCombinerById(dataCombining.id()).map(oldInstance -> {
            internalUpdateDataCombiner(dataCombining);
            return true;
        }).orElse(false);
    }


    public @NotNull CompletableFuture<Void> deleteDataCombiner(final @NotNull UUID combinerId) {

        return CompletableFuture.completedFuture(null);
    }

    public @NotNull Optional<DataCombiner> getCombinerById(final @NotNull UUID id) {
        return Optional.ofNullable(idToDataCombiner.get(id));
    }

    public @NotNull Collection<DataCombiner> getAllCombiners() {
        return idToDataCombiner.values();
    }


    private void internalUpdateDataCombiner(final DataCombiner dataCombiner) {
        deleteDataCombinerInternal(dataCombiner.id());
        createDataCombinerInternal(dataCombiner);
        syncFuture(startCombiner(dataCombiner));
        configPersistence.updateDataCombiners(dataCombiner);
    }


    private synchronized void createDataCombinerInternal(final @NotNull DataCombiner dataCombiner) {

        if (idToDataCombiner.get(dataCombiner.id()) != null) {
            throw new IllegalArgumentException("adapter already exists by id '" + dataCombiner.id() + "'");
        }

        //TODO
        //    protocolAdapterMetrics.increaseProtocolAdapterMetric(config.getProtocolId());


    }


    private synchronized boolean deleteDataCombinerInternal(final @NotNull UUID id) {
        Preconditions.checkNotNull(id);
        final DataCombiner dataCombiner = idToDataCombiner.remove(id);
        if (dataCombiner != null) {
            // TODO
            //    protocolAdapterMetrics.decreaseProtocolAdapterMetric(adapterWrapper.getAdapterInformation()
            //            .getProtocolId());
            try {
                // stop in any case as some resources must be cleaned up even if the adapter is still being started and is not yet in started state
                stop(dataCombiner).get();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (final ExecutionException e) {
                throw new RuntimeException(e);
            }

            try {
                //ensure the instance releases any hard state
                // TODO
                //   adapterWrapper.getAdapter().destroy();
                return true;
            } finally {
                eventService.createDataCombiningEvent(id)
                        .withSeverity(Event.SEVERITY.WARN)
                        .withMessage(String.format("Data Combininh '%s' was deleted from the system permanently.", id))
                        .fire();
            }
        } else {
            log.error("Tried removing non existing adapter '{}'", id);
        }
        return false;
    }


}
