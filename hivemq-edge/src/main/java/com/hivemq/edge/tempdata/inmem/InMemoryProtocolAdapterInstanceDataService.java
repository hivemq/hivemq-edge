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
