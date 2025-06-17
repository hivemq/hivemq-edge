package com.hivemq.edge.tempdata.inmem;

import com.hivemq.adapter.sdk.api.services.ProtocolAdapterTemporaryDataService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryProtocolAdapterTemporaryDataService implements ProtocolAdapterTemporaryDataService {

    private final @NotNull Map<String, String> keyValueStore;

    public InMemoryProtocolAdapterTemporaryDataService() {
        this.keyValueStore = new ConcurrentHashMap<>();
    }

    @Override
    public @NotNull CompletableFuture<Void> putTemporaryValue(@NotNull final String key, @Nullable final String value) {
        return CompletableFuture.runAsync(() -> keyValueStore.putIfAbsent(key, value));
    }

    @Override
    public @NotNull CompletableFuture<Optional<String>> getTemporaryValue(@NotNull final String key) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(keyValueStore.get(key)));
    }
}
