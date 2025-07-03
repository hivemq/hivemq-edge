/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.edge.tempdata.inmem;

import com.hivemq.adapter.sdk.api.services.ProtocolAdapterInstanceDataService;
import com.hivemq.edge.tempdata.InstanceDataStorageFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class InMemoryProtocolAdapterInstanceDataServiceFactory implements InstanceDataStorageFactory {

    private final @NotNull ConcurrentHashMap<String, InMemoryProtocolAdapterInstanceDataService>
            protocolAdapterTemporaryDataServiceMap = new ConcurrentHashMap<>();

    @Inject
    public InMemoryProtocolAdapterInstanceDataServiceFactory() {
    }

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
                    final var db = protocolAdapterTemporaryDataServiceMap.remove(generateKey(protocolId, adapterId));
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
