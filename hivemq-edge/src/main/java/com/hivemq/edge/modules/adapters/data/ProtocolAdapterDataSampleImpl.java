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
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.data.ProtocolAdapterDataSample;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * A protocol adapter sample, is a sampled measurement taken at a point in time. It can encapsulate more than one
 * tag and value pair, and will result in dataPointValues#size being published to the MQTT system.
 *
 * @author Simon L Johnson
 */
public class ProtocolAdapterDataSampleImpl implements ProtocolAdapterDataSample {

    private final @NotNull Long timestamp = System.currentTimeMillis();
    private final @NotNull List<DataPoint> dataPoints = Collections.synchronizedList(new ArrayList<>());
    private final @NotNull String adapterid;

    public ProtocolAdapterDataSampleImpl(final @NotNull String adapterid) {
        this.adapterid = adapterid;
    }

    @Override
    @JsonIgnore
    public @NotNull Long getTimestamp() {
        return timestamp;
    }

    @Override
    public void addDataPoint(final @NotNull String tagName, final @NotNull Object tagValue) {
        final DataPointImpl dataPoint = new DataPointImpl(tagName, tagValue, adapterid);
        addDataPoint(dataPoint);
    }

    @Override
    public void addDataPoint(final @NotNull DataPoint dataPoint) {
        dataPoints.add(dataPoint);
    }

    @Override
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public @NotNull Map<String, List<DataPoint>> getDataPoints() {
        if (!dataPoints.isEmpty()) {
            final var firstTag = dataPoints.getFirst();
            if (firstTag != null) {
                return Map.of(firstTag.getTagName(), dataPoints);
            } else {
                return Collections.emptyMap();
            }
        } else {
            return Collections.emptyMap();
        }
    }

    @Override
    public @NotNull List<DataPoint> getDataPointList() {
        return List.copyOf(dataPoints);
    }
}
