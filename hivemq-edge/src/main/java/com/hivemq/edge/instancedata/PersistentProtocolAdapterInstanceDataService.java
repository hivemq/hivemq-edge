package com.hivemq.edge.instancedata;

import com.hivemq.adapter.sdk.api.services.ProtocolAdapterInstanceDataService;
import com.hivemq.http.core.Files;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class PersistentProtocolAdapterInstanceDataService implements ProtocolAdapterInstanceDataService {
    private static final Logger log = LoggerFactory.getLogger(PersistentProtocolAdapterInstanceDataService.class);

    private final @NotNull HTreeMap<byte[], byte[]> stateKeyToValue;

    private final @NotNull DB db;

    private final @NotNull File dbFile;

    public PersistentProtocolAdapterInstanceDataService(final @NotNull Path dbFile) {
        this.dbFile = dbFile.toFile();
        db = DBMaker.fileDB(this.dbFile).transactionEnable().make();
        this.stateKeyToValue = db.hashMap("map", Serializer.BYTE_ARRAY, Serializer.BYTE_ARRAY).createOrOpen();
    }

    @Override
    public @NotNull CompletableFuture<Void> putValue(@NotNull final String key, @Nullable final String value) {
        if (value == null) {
            return CompletableFuture.runAsync(() -> stateKeyToValue.remove(key.getBytes(StandardCharsets.UTF_8)));
        } else {
            return CompletableFuture.runAsync(() -> stateKeyToValue.put(key.getBytes(StandardCharsets.UTF_8), value.getBytes(StandardCharsets.UTF_8)));
        }
    }

    @Override
    public @NotNull CompletableFuture<Optional<String>> getValue(@NotNull final String key) {
        return CompletableFuture.supplyAsync(() -> Optional
                .ofNullable(stateKeyToValue.get(key.getBytes(StandardCharsets.UTF_8)))
                .map(String::new));
    }

    public @NotNull void destroy() {
        db.close();
        try {
            Files.delete(dbFile);
        } catch (final IOException ioe) {
            log.error("Unable to delete persistent instance data file: {}", dbFile, ioe);
        }
    }
}
