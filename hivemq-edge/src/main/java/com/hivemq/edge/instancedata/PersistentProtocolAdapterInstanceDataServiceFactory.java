package com.hivemq.edge.instancedata;

import com.hivemq.adapter.sdk.api.services.ProtocolAdapterInstanceDataService;
import com.hivemq.configuration.info.SystemInformation;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bouncycastle.util.encoders.Base32;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class PersistentProtocolAdapterInstanceDataServiceFactory implements InstanceDataStorageFactory {

    private static final @NotNull Logger log = LoggerFactory.getLogger(PersistentProtocolAdapterInstanceDataServiceFactory.class);
    public static final String PATH_DATADIR_INSTANCEDATA = "instancedata";

    private final @NotNull Path dbsPath;
    private final @NotNull ConcurrentHashMap<String, PersistentProtocolAdapterInstanceDataService>
            protocolAdapterIdToDataServiceMap = new ConcurrentHashMap<>();

    @Inject
    public PersistentProtocolAdapterInstanceDataServiceFactory(final @NotNull SystemInformation systemInformation) {
        this.dbsPath = systemInformation.getDataFolder().toPath().resolve(PATH_DATADIR_INSTANCEDATA);
        try {
            Files.createDirectories(dbsPath);
            Files.list(dbsPath)
                    .filter(path -> {
                        if (path.toFile().isDirectory()) {
                            return true;
                        } else {
                            log.warn("Found unexpected file in instance data directory: {}.", path);
                            return false;
                        }
                    })
                    .forEach(dbDirectory ->
                            protocolAdapterIdToDataServiceMap.put(
                                    new String(
                                            Base32.decode(dbDirectory.getFileName().toString().getBytes(StandardCharsets.UTF_8)),
                                            StandardCharsets.UTF_8),
                                    new PersistentProtocolAdapterInstanceDataService(dbDirectory)));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<ProtocolAdapterInstanceDataService> getOrCreate(
            final @NotNull String protocolId,
            final @NotNull String adapterId) {
        return CompletableFuture.supplyAsync(() -> {
            final var key = generateKey(protocolId, adapterId);
            final var direcotryName = Base32.toBase32String(key.getBytes(StandardCharsets.UTF_8));
            return protocolAdapterIdToDataServiceMap.computeIfAbsent(
                    key, unused -> new PersistentProtocolAdapterInstanceDataService(dbsPath.resolve(direcotryName)));
            }
        );
    }

    @Override
    public CompletableFuture<Void> destroy(final String protocolId, final String adapterId) {
        return CompletableFuture.runAsync(() -> {
            final var db = protocolAdapterIdToDataServiceMap.remove(generateKey(protocolId, adapterId));
            if(db != null) {
                db.destroy();
            }
        });
    }

    @Override
    public CompletableFuture<List<String>> listByProtocolId(final String protocolId) {
        return CompletableFuture.supplyAsync(() ->
                protocolAdapterIdToDataServiceMap.keySet().stream()
                        .filter(key -> key.startsWith(protocolId))
                        .map(key -> key.replace(protocolId + "-", ""))
                        .toList());
    }

    public static String generateKey(final @NotNull String protocolId, final @NotNull String adapterId) {
        return protocolId + "-" + adapterId;
    }
}
