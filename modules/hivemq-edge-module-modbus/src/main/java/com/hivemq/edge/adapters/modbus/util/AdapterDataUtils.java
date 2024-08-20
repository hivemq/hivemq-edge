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
package com.hivemq.edge.adapters.modbus.util;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.edge.adapters.modbus.config.DataType;
import jdk.jshell.spi.ExecutionControl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AdapterDataUtils {
    public static boolean matches(final @NotNull DataPoint point, final @NotNull List<DataPoint> list) {
        return list.stream().filter(dp -> dp.getTagName().equals(point.getTagName())) // First filter by tagName
                .anyMatch(dp -> dp.getTagValue().equals(point.getTagValue())); // Then check for tagValue
    }

    public static @NotNull List<DataPoint> mergeChangedSamples(
            final @Nullable List<DataPoint> historicalSamples, final @NotNull List<DataPoint> currentSamples) {
        if (historicalSamples == null) {
            return currentSamples;
        }
        List<DataPoint> delta = new ArrayList<>();
        for (int i = 0; i < currentSamples.size(); i++) {
            DataPoint currentSample = currentSamples.get(i);
            // If the current sample does not match any in the historical samples, it has changed
            if (!matches(currentSample, historicalSamples)) {
                historicalSamples.set(i, currentSample);
                delta.add(currentSample);
            }
        }
        return delta;
    }
}
