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
package com.hivemq.edge.adapters.modbus.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.hivemq.adapter.sdk.api.adapters.config.AdapterSubscription;
import com.hivemq.adapter.sdk.api.adapters.data.DataPoint;
import com.hivemq.adapter.sdk.api.adapters.data.ProtocolAdapterDataSample;
import com.hivemq.adapter.sdk.api.adapters.factories.DataPointFactory;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Simon L Johnson
 */
public class ModBusData implements ProtocolAdapterDataSample {
    public enum TYPE {
        COILS,
        INPUT_REGISTERS,
        HOLDING_REGISTERS,
    }

    private final @NotNull TYPE type;
    private final DataPointFactory dataPointFactory;
    protected @NotNull Long timestamp = System.currentTimeMillis();
    protected @NotNull AdapterSubscription adapterSubscription;

    //-- Handle multiple tags in the same sample
    protected @NotNull List<DataPoint> dataPoints = new CopyOnWriteArrayList<>();

    public ModBusData(
            final @NotNull AdapterSubscription adapterSubscription,
            final @NotNull TYPE type,
            final @NotNull DataPointFactory dataPointFactory) {
        this.adapterSubscription = adapterSubscription;
        this.type = type;

        this.dataPointFactory = dataPointFactory;
    }

    public @NotNull TYPE getType() {
        return type;
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
