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
package com.hivemq.edge.modules.adapters.simulation.config.legacy;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class LegacySimulationAdapterConfig {

    @JsonProperty("subscriptions")
    private @NotNull List<LegacySimulationPollingContext> pollingContexts = new ArrayList<>();

    @JsonProperty(value = "id", required = true)
    protected @NotNull String id;

    @JsonProperty("pollingIntervalMillis")
    @JsonAlias(value = "publishingInterval") //-- Ensure we cater for properties created with legacy configuration
    private int pollingIntervalMillis = 1000; //1 second

    @JsonProperty("maxPollingErrorsBeforeRemoval")
    private int maxPollingErrorsBeforeRemoval = 10;

    @JsonProperty("minValue")
    private int minValue = 0;

    @JsonProperty("maxValue")
    private int maxValue = 1000;

    @JsonProperty("minDelay")
    private int minDelay = 0;

    @JsonProperty("maxDelay")
    private int maxDelay = 0;

    public @NotNull List<LegacySimulationPollingContext> getPollingContexts() {
       return pollingContexts;
    }

    public @NotNull String getId() {
        return id;
    }

    public int getMinValue() {
        return minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public int getPollingIntervalMillis() {
        return pollingIntervalMillis;
    }

    public int getMaxPollingErrorsBeforeRemoval() {
        return maxPollingErrorsBeforeRemoval;
    }

    public int getMaxDelay() {
        return maxDelay;
    }

    public int getMinDelay() {
        return minDelay;
    }
}
