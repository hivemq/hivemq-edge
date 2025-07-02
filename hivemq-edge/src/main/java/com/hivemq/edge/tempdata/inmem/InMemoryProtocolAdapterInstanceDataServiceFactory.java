package com.hivemq.edge.tempdata.inmem;

import com.hivemq.adapter.sdk.api.services.ProtocolAdapterInstanceDataService;
import com.hivemq.edge.tempdata.InstanceDataStorageFactory;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class InMemoryProtocolAdapterInstanceDataServiceFactory implements InstanceDataStorageFactory {

    private final @NotNull ConcurrentHashMap<String, InMemoryProtocolAdapterInstanceDataService>
            protocolAdapterTemporaryDataServiceMap = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<ProtocolAdapterInstanceDataService> getOrCreate(
            final @NotNull String protocolId,
            final @NotNull String adapterId) {
        return CompletableFuture.supplyAsync(() ->
                protocolAdapterTemporaryDataServiceMap.computeIfAbsent(
                        generateKey(protocolId, adapterId), k -> new InMemoryProtocolAdapterInstanceDataService()));
    }

    @Override
    public CompletableFuture<Void> destroy(final String protocolId, final String adapterId) {
        return CompletableFuture.runAsync(() -> {
                    var db = protocolAdapterTemporaryDataServiceMap.remove(generateKey(protocolId, adapterId));
                    if(db != null) {
                        db.getDb().close();
                    }
                });
    }

    @Override
    public CompletableFuture<List<String>> listByProtocolId(final String protocolId) {
        return CompletableFuture.supplyAsync(() ->
                protocolAdapterTemporaryDataServiceMap.keySet().stream()
                        .filter(key -> key.startsWith(protocolId))
                        .map(key -> key.replace(protocolId + "-", ""))
                        .toList());
    }

    public static String generateKey(final @NotNull String protocolId, final @NotNull String adapterId) {
        return protocolId + "-" + adapterId;
    }
}
