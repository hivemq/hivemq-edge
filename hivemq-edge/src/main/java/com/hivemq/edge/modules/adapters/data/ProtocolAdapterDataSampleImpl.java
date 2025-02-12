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
package com.hivemq.edge.modules.adapters.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Preconditions;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.data.ProtocolAdapterDataSample;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A protocol adapter sample, is a sampled measurement taken at a point in time. It can encapsulate more than one
 * tag and value pair, and will result in dataPointValues#size being published to the MQTT system.
 *
 * @author Simon L Johnson
 */
public class ProtocolAdapterDataSampleImpl implements ProtocolAdapterDataSample {

    private final @NotNull Long timestamp = System.currentTimeMillis();
    final @NotNull Map<String, List<DataPoint>> tagNameToDataPoints = new ConcurrentHashMap<>();


    public ProtocolAdapterDataSampleImpl() {
    }

    @Override
    @JsonIgnore
    public @NotNull Long getTimestamp() {
        return timestamp;
    }

    @Override
    public void addDataPoint(final @NotNull String tagName, final @NotNull Object tagValue) {
        Preconditions.checkNotNull(tagName);
        Preconditions.checkNotNull(tagValue);
        final DataPointImpl dataPoint = new DataPointImpl(tagName, tagValue);
        addDataPoint(dataPoint);
    }

    @Override
    public void addDataPoint(final @NotNull DataPoint dataPoint) {
        tagNameToDataPoints.compute(dataPoint.getTagName(), (key, current) -> {
            if (current != null) {
                current.add(dataPoint);
                return current;
            } else {
                final List<DataPoint> dataPoints = new ArrayList<>();
                dataPoints.add(dataPoint);
                return dataPoints;
            }
        });

    }

    @Override
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public @NotNull Map<String, List<DataPoint>> getDataPoints() {
        return tagNameToDataPoints;
    }

}
