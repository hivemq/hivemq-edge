package com.hivemq.combining;

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

@Singleton
public class CombiningManager {

    private static final Logger log = LoggerFactory.getLogger(CombiningManager.class);

    private final @NotNull ConfigPersistence configPersistence;
    private final Map<UUID, DataCombiner> idToDataCombiner = new ConcurrentHashMap<>();

    @Inject
    public CombiningManager(final @NotNull ConfigPersistence configPersistence) {
        this.configPersistence = configPersistence;
    }


    public void start(){
        log.debug("Starting data combiners");
        for (final ProtocolAdapterConfig adapterConfig : configPersistence.allAdapters()) {

            // TODO start the actual data combining
        }
    }

    public @NotNull CompletableFuture<Void> stop(final @NotNull String dataCombinerId) {
        // TODO
        return CompletableFuture.completedFuture(null);
    }

    public @NotNull CompletableFuture<Void> stopAll() {
        // TODO
        return CompletableFuture.completedFuture(null);
    }


    public @NotNull CompletableFuture<Void> addDataCombiner(final @NotNull DataCombiner dataCombining){
        // TODO
        return CompletableFuture.completedFuture(null);
    }

    public @NotNull CompletableFuture<Void> updateDataCombiner(final @NotNull DataCombiner dataCombining){
        // TODO
        return CompletableFuture.completedFuture(null);
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


}
