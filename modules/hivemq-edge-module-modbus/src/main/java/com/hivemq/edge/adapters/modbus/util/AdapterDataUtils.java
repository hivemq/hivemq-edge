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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AdapterDataUtils {

    public static @NotNull List<DataPoint> mergeChangedSamples(
            final @Nullable List<DataPoint> historicalSamples, final @NotNull List<DataPoint> currentSamples) {
        if (historicalSamples == null) {
            return currentSamples;
        }

        final Map<String, DataPoint> historicalSamplesMap = historicalSamples.stream()
                .collect(Collectors.toMap(DataPoint::getTagName, Function.identity()));

        return currentSamples.stream()
                .filter(sample ->
                        !(historicalSamplesMap.containsKey(sample.getTagName()) && historicalSamplesMap.get(sample.getTagName()).getTagValue().equals(sample.getTagValue())))
                .collect(Collectors.toList());
    }
}
