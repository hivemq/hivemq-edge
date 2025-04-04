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
package com.hivemq.edge.adapters.plc4x;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is here TEMPORARY, the functionality will be moved into NorthboundMappings
 */
public class PublishChangedDataOnlyHandler {
    private final @NotNull Map<String, List<DataPoint>> lastSamples = new ConcurrentHashMap<>();

    public boolean replaceIfValueIsNew(final @NotNull String tagName, final @NotNull List<DataPoint> newValue) {
        final var computedValue = lastSamples.compute(tagName, (key,value) -> {
            if (value == null) {
                return newValue;
            } else if (value.equals(newValue)) {
                return value;
            } else {
                return newValue;
            }
        });

        return newValue != computedValue;
    }

    public void clear() {
        lastSamples.clear();
    }
}
