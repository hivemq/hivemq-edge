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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unused", "FieldCanBeLocal", "FieldMayBeFinal"})
@JsonPropertyOrder({"minValue", "maxValue", "subscriptions"})
public class SimulationAdapterConfig implements ProtocolAdapterConfig {

    private static final @NotNull String ID_REGEX = "^([a-zA-Z_0-9-_])*$";

    @JsonProperty("subscriptions")
    @ModuleConfigField(title = "subscription", description = "List of subscriptions for the simulation")
    private @NotNull List<SimulationPollingContext> pollingContexts = new ArrayList<>();

    @JsonProperty(value = "id", required = true)
    @ModuleConfigField(title = "Identifier",
                       description = "Unique identifier for this protocol adapter",
                       format = ModuleConfigField.FieldType.IDENTIFIER,
                       required = true,
                       stringPattern = ID_REGEX,
                       stringMinLength = 1,
                       stringMaxLength = 1024)
    protected @NotNull String id;

    @JsonProperty("pollingIntervalMillis")
    @JsonAlias(value = "publishingInterval") //-- Ensure we cater for properties created with legacy configuration
    @ModuleConfigField(title = "Polling Interval [ms]",
                       description = "Time in millisecond that this endpoint will be polled",
                       numberMin = 1,
                       required = true,
                       defaultValue = "1000")
    private int pollingIntervalMillis = 1000; //1 second

    @JsonProperty("maxPollingErrorsBeforeRemoval")
    @ModuleConfigField(title = "Max. Polling Errors",
                       description = "Max. errors polling the endpoint before the polling daemon is stopped",
                       defaultValue = "10")
    private int maxPollingErrorsBeforeRemoval = 10;

    @JsonProperty("minValue")
    @ModuleConfigField(title = "Min. Generated Value",
                       description = "Minimum value of the generated decimal number",
                       numberMin = 0,
                       defaultValue = "0")
    private int minValue = 0;

    @JsonProperty("maxValue")
    @ModuleConfigField(title = "Max. Generated Value (Excl.)",
                       description = "Maximum value of the generated decimal number (excluded)",
                       numberMax = 1000,
                       defaultValue = "1000")
    private int maxValue = 1000;

    @JsonProperty("minDelay")
    @ModuleConfigField(title = "Minimum of delay",
                       description = "Minimum of artificial delay before the polling method generates a value",
                       numberMin = 0,
                       defaultValue = "0")
    private int minDelay = 0;

    @JsonProperty("maxDelay")
    @ModuleConfigField(title = "Maximum of delay",
                       description = "Maximum of artificial delay before the polling method generates a value",
                       numberMin = 0,
                       defaultValue = "0")
    private int maxDelay = 0;

    public SimulationAdapterConfig() {
    }

    public @NotNull List<SimulationPollingContext> getPollingContexts() {
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
