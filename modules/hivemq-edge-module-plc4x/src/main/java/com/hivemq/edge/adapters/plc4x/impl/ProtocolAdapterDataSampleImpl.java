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
package com.hivemq.edge.adapters.plc4x.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.hivemq.adapter.sdk.api.config.AdapterSubscription;
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.data.ProtocolAdapterDataSample;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A protocol adapter sample, is a sampled measurement taken at a point in time. It can encapsulate more than one
 * tag and value pair, and will result in dataPointValues#size being published to the MQTT system.
 *
 * @author Simon L Johnson
 */
public class ProtocolAdapterDataSampleImpl<T extends ProtocolAdapterConfig> implements ProtocolAdapterDataSample {

    protected @NotNull Long timestamp = System.currentTimeMillis();
    protected @NotNull AdapterSubscription adapterSubscription;
    private final @NotNull DataPointFactory dataPointFactory;

    //-- Handle multiple tags in the same sample
    protected @NotNull List<DataPoint> dataPoints = new CopyOnWriteArrayList<>();

    public ProtocolAdapterDataSampleImpl(
            final @NotNull AdapterSubscription adapterSubscription, final @NotNull DataPointFactory dataPointFactory) {
        this.adapterSubscription = adapterSubscription;
        this.dataPointFactory = dataPointFactory;
    }

    @Override
    @JsonIgnore
    public @NotNull AdapterSubscription getSubscription() {
        return adapterSubscription;
    }

    @Override
    @JsonIgnore
    public @NotNull Long getTimestamp() {
        return timestamp;
    }

    @Override
    public void addDataPoint(final @NotNull String tagName, final @NotNull Object tagValue) {
        dataPoints.add(dataPointFactory.create(tagName, tagValue));
    }

    @Override
    public void setDataPoints(@NotNull List<DataPoint> list) {
        this.dataPoints = list;
    }

    @Override
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public @NotNull List<DataPoint> getDataPoints() {
        return dataPoints;
    }

}
