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
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.data.ProtocolAdapterDataSample;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A protocol adapter sample, is a sampled measurement taken at a point in time. It can encapsulate more than one
 * tag and value pair, and will result in dataPointValues#size being published to the MQTT system.
 *
 * @author Simon L Johnson
 */
public class ProtocolAdapterDataSampleImpl<T extends PollingContext> implements ProtocolAdapterDataSample {

    private final @NotNull Long timestamp = System.currentTimeMillis();
    private final @NotNull T pollingContext;

    //-- Handle multiple tags in the same sample
    private @NotNull List<DataPoint> dataPoints = new CopyOnWriteArrayList<>();


    public ProtocolAdapterDataSampleImpl(final @NotNull T pollingContext) {
        this.pollingContext = pollingContext;
    }

    @Override
    @JsonIgnore
    public @NotNull T getPollingContext() {
        return pollingContext;
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
        dataPoints.add(new DataPointImpl(tagName, tagValue));
    }

    @Override
    public void addDataPoint(final @NotNull DataPoint dataPoint) {
        dataPoints.add(dataPoint);
    }

    @Override
    public void setDataPoints(final @NotNull List<DataPoint> list) {
        this.dataPoints = list;
    }

    @Override
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public @NotNull List<DataPoint> getDataPoints() {
        return dataPoints;
    }

}
