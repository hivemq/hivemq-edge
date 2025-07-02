package com.hivemq.edge.tempdata.inmem;

import com.hivemq.adapter.sdk.api.services.ProtocolAdapterInstanceDataService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryProtocolAdapterInstanceDataService implements ProtocolAdapterInstanceDataService {

    private final @NotNull HTreeMap<byte[], byte[]> stateKeyToValue;

    private final @NotNull DB db;

    public InMemoryProtocolAdapterInstanceDataService() {
        this.db = DBMaker.memoryDB().make();
        this.stateKeyToValue = db.hashMap("map", Serializer.BYTE_ARRAY, Serializer.BYTE_ARRAY).createOrOpen();
    }

    @Override
    public @NotNull CompletableFuture<Void> putValue(@NotNull final String key, @Nullable final String value) {
        if (value == null) {
            return CompletableFuture.runAsync(() -> stateKeyToValue.remove(key));
        } else {
            return CompletableFuture.runAsync(() -> stateKeyToValue.put(key.getBytes(), value.getBytes()));
        }
    }

    @Override
    public @NotNull CompletableFuture<Optional<String>> getValue(@NotNull final String key) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(stateKeyToValue.get(key)).map(String::new));
    }

    public @NotNull DB getDb() {
        return db;
    }
}
