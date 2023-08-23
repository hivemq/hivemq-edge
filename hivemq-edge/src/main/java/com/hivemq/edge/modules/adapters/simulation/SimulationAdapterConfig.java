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
package com.hivemq.edge.modules.adapters.simulation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.edge.modules.adapters.annotations.ModuleConfigField;
import com.hivemq.edge.modules.config.impl.AbstractProtocolAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SimulationAdapterConfig extends AbstractProtocolAdapterConfig {

    @JsonProperty(value = "pollingIntervalMillis")
    @ModuleConfigField(title = "Polling interval [ms]",
                       description = "Interval in milliseconds to poll for changes",
                       defaultValue = "10000",
                       numberMin = 100,
                       numberMax = 86400000) //24h
    private int pollingIntervalMillis = 10000;

    @JsonProperty("subscriptions")
    @ModuleConfigField(title = "Subscriptions",
                       description = "List of subscriptions for the simulation",
                       required = true)
    private @NotNull List<Subscription> subscriptions = new ArrayList<>();

    public SimulationAdapterConfig() {
    }

    public SimulationAdapterConfig(
            final @NotNull String id,
            final @NotNull List<Subscription> subscriptions) {
        this.id = id;
        this.subscriptions = subscriptions;
    }

    public int getPollingIntervalMillis() {
        return pollingIntervalMillis;
    }

    public void setPollingIntervalMillis(int pollingIntervalMillis) {
        this.pollingIntervalMillis = pollingIntervalMillis;
    }

    public void setSubscriptions(List<Subscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public @NotNull List<Subscription> getSubscriptions() {
        return subscriptions;
    }

}
