package com.hivemq.edge.tempdata;

import com.hivemq.adapter.sdk.api.services.ProtocolAdapterTemporaryDataService;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface TempDataStorageFactory {
    CompletableFuture<ProtocolAdapterTemporaryDataService> getOrCreate(final @NotNull String protocolId, final @NotNull String adapterId);
    CompletableFuture<Void> destroy(final @NotNull String protocolId, final @NotNull String adapterId);
    CompletableFuture<List<String>> listByProtocolId(final @NotNull String protocolId);

}
