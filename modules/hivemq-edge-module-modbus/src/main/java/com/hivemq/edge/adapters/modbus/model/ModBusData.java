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
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Simon L Johnson
 */
public class ModBusData  {
    public enum TYPE {
        COILS,
        INPUT_REGISTERS,
        HOLDING_REGISTERS,
    }

    private final DataPointFactory dataPointFactory;
    protected @NotNull PollingContext pollingContext;

    //-- Handle multiple tags in the same sample
    protected @NotNull List<DataPoint> dataPoints = new CopyOnWriteArrayList<>();

    public ModBusData(
            final @NotNull PollingContext pollingContext,
            final @NotNull DataPointFactory dataPointFactory) {
        this.pollingContext = pollingContext;
        this.dataPointFactory = dataPointFactory;
    }


    @JsonIgnore
    public @NotNull PollingContext getPollingContext() {
        return pollingContext;
    }

    public void addDataPoint(final @NotNull String tagName, final @NotNull Object tagValue) {
        dataPoints.add(dataPointFactory.create(tagName, tagValue));
    }


    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public @NotNull List<DataPoint> getDataPoints() {
        return dataPoints;
    }
}
