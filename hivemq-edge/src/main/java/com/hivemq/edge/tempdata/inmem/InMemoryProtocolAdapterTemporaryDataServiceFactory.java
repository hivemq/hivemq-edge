package com.hivemq.edge.tempdata.inmem;

import com.hivemq.adapter.sdk.api.services.ProtocolAdapterTemporaryDataService;
import com.hivemq.edge.tempdata.TempDataStorageFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryProtocolAdapterTemporaryDataServiceFactory implements TempDataStorageFactory {

    private final @NotNull ConcurrentHashMap<String, ProtocolAdapterTemporaryDataService>
            protocolAdapterTemporaryDataServiceMap = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<ProtocolAdapterTemporaryDataService> getOrCreate(
            final @NotNull String protocolId,
            final @NotNull String adapterId) {
        return CompletableFuture.supplyAsync(() ->
                protocolAdapterTemporaryDataServiceMap.computeIfAbsent(
                        generateKey(protocolId, adapterId), k -> new InMemoryProtocolAdapterTemporaryDataService()));
    }

    @Override
    public CompletableFuture<Void> destroy(final String protocolId, final String adapterId) {
        return CompletableFuture.runAsync(() ->
                protocolAdapterTemporaryDataServiceMap.remove(generateKey(protocolId, adapterId)));
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
