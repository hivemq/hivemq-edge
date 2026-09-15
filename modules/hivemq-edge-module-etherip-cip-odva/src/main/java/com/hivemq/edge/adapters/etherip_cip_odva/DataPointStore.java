/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.etherip_cip_odva;

import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTag;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataPointStore {
    private final @NotNull Map<String, Object> dataPointsMap = new ConcurrentHashMap<>();
    private final @NotNull Map<String, Long> dataPointsLastupdatedMap = new ConcurrentHashMap<>();

    public @Nullable Object get(@NotNull CipTag tag) {
        return dataPointsMap.get(tag.getName());
    }

    public void put(@NotNull CipTag tag, Object value, @NotNull Long nowMs) {
        dataPointsMap.put(tag.getName(), value);
        dataPointsLastupdatedMap.put(tag.getName(), nowMs);
    }

    public boolean isValueOlderThan(@NotNull CipTag tag, @NotNull Long nowMs) {

        Integer updateIntervalMs = tag.getDefinition().getMinUpdateIntervalMs();
        if (updateIntervalMs == null || updateIntervalMs == 0) {
            return false;
        }

        Long lastUpdatedMs = dataPointsLastupdatedMap.getOrDefault(tag.getName(), 0L);
        return lastUpdatedMs <= nowMs - updateIntervalMs;
    }

    public void clear() {
        dataPointsMap.clear();
        dataPointsLastupdatedMap.clear();
    }
}
