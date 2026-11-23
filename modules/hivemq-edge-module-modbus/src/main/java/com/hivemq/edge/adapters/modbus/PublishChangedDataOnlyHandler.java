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
package com.hivemq.edge.adapters.modbus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.NotNull;

/**
 * This class is here TEMPORARY, the functionality will be moved into NorthboundMappings
 */
public class PublishChangedDataOnlyHandler {
    private final @NotNull Map<String, Object> lastSamples = new ConcurrentHashMap<>();

    public boolean replaceIfValueIsNew(final @NotNull String tagName, final @NotNull Object newValue) {
        // Record whether the sample was replaced. Comparing the result of compute() with newValue by reference cannot
        // tell: Boolean and small integers are cached boxes, so the old and the new sample are the same object
        // (EDG-1209).
        final var replaced = new AtomicBoolean(false);
        lastSamples.compute(tagName, (key, value) -> {
            if (value == null) {
                replaced.set(true);
                return newValue;
            } else if (value.equals(newValue)) {
                return value;
            } else {
                replaced.set(true);
                return newValue;
            }
        });
        return replaced.get();
    }

    /**
     * Whether {@code newValue} differs from the last sample recorded for the tag; records nothing.
     */
    public boolean isNew(final @NotNull String tagName, final @NotNull Object newValue) {
        return !newValue.equals(lastSamples.get(tagName));
    }

    public void clear() {
        lastSamples.clear();
    }
}
