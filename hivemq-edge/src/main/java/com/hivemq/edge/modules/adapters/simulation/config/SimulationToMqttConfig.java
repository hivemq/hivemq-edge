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
package com.hivemq.edge.modules.adapters.simulation.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class SimulationToMqttConfig {

    public static final SimulationToMqttConfig DEFAULT = new SimulationToMqttConfig(List.of(), null, null);

    @JsonProperty("pollingIntervalMillis")
    @ModuleConfigField(title = "Polling Interval [ms]",
                       description = "Time in millisecond that this endpoint will be polled",
                       numberMin = 1,
                       defaultValue = "1000")
    private final int pollingIntervalMillis;

    @JsonProperty("maxPollingErrorsBeforeRemoval")
    @ModuleConfigField(title = "Max. Polling Errors",
                       description = "Max. errors polling the endpoint before the polling daemon is stopped (-1 for unlimited retries)",
                       numberMin = -1,
                       defaultValue = "10")
    private final int maxPollingErrorsBeforeRemoval;

    @JsonCreator
    public SimulationToMqttConfig(
            @JsonProperty("simulationToMqttMappings") final @Nullable List<SimulationToMqttMapping> simulationToMqttMappings,
            @JsonProperty("pollingIntervalMillis") final @Nullable Integer pollingIntervalMillis,
            @JsonProperty("maxPollingErrorsBeforeRemoval") final @Nullable Integer maxPollingErrorsBeforeRemoval) {
        this.pollingIntervalMillis = Objects.requireNonNullElse(pollingIntervalMillis, 1000);
        this.maxPollingErrorsBeforeRemoval = Objects.requireNonNullElse(maxPollingErrorsBeforeRemoval, 10);
    }

    public int getPollingIntervalMillis() {
        return pollingIntervalMillis;
    }

    public int getMaxPollingErrorsBeforeRemoval() {
        return maxPollingErrorsBeforeRemoval;
    }

    @Override
    public String toString() {
        return "SimulationToMqttConfig{" +
                "pollingIntervalMillis=" +
                pollingIntervalMillis +
                ", maxPollingErrorsBeforeRemoval=" +
                maxPollingErrorsBeforeRemoval +
                '}';
    }
}
