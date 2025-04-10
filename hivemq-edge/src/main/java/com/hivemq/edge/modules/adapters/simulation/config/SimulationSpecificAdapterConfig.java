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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@SuppressWarnings({"unused", "FieldCanBeLocal", "FieldMayBeFinal"})
@JsonPropertyOrder({"minValue", "maxValue", "subscriptions"})
public class SimulationSpecificAdapterConfig
        implements ProtocolSpecificAdapterConfig {

    private static final @NotNull String ID_REGEX = "^([a-zA-Z_0-9-_])*$";

    @JsonProperty(value = "id", required = true, access = JsonProperty.Access.WRITE_ONLY)
    @ModuleConfigField(title = "Identifier",
                       description = "Unique identifier for this protocol adapter",
                       format = ModuleConfigField.FieldType.IDENTIFIER,
                       required = true,
                       stringPattern = ID_REGEX,
                       stringMinLength = 1,
                       stringMaxLength = 1024)
    private @Nullable String id;

    @JsonProperty(value = "simulationToMqtt")
    @ModuleConfigField(title = "simulationToMqtt",
                       description = "Define Simulations to create MQTT messages.",
                       required = true)
    private final @NotNull SimulationToMqttConfig simulationToMqttConfig;

    @JsonProperty("minValue")
    @ModuleConfigField(title = "Min. Generated Value",
                       description = "Minimum value of the generated decimal number",
                       numberMin = 0,
                       defaultValue = "0")
    private final int minValue;

    @JsonProperty("maxValue")
    @ModuleConfigField(title = "Max. Generated Value (Excl.)",
                       description = "Maximum value of the generated decimal number (excluded)",
                       numberMax = 1000,
                       defaultValue = "1000")
    private final int maxValue;

    @JsonProperty("minDelay")
    @ModuleConfigField(title = "Minimum of delay",
                       description = "Minimum of artificial delay before the polling method generates a value",
                       numberMin = 0,
                       defaultValue = "0")
    private final int minDelay;

    @JsonProperty("maxDelay")
    @ModuleConfigField(title = "Maximum of delay",
                       description = "Maximum of artificial delay before the polling method generates a value",
                       numberMin = 0,
                       defaultValue = "0")
    private final int maxDelay;

    @JsonCreator
    public SimulationSpecificAdapterConfig(
            @JsonProperty(value = "simulationToMqtt") final @Nullable SimulationToMqttConfig simulationToMqttConfig,
            @JsonProperty("minValue") final @Nullable Integer minValue,
            @JsonProperty("maxValue") final @Nullable Integer maxValue,
            @JsonProperty("minDelay") final @Nullable Integer minDelay,
            @JsonProperty("maxDelay") final @Nullable Integer maxDelay) {
        this.simulationToMqttConfig =
                Objects.requireNonNullElse(simulationToMqttConfig, SimulationToMqttConfig.DEFAULT);
        this.minValue = Objects.requireNonNullElse(minValue, 0);
        this.maxValue = Objects.requireNonNullElse(maxValue, 1000);
        this.minDelay = Objects.requireNonNullElse(minDelay, 0);
        this.maxDelay = Objects.requireNonNullElse(maxDelay, 0);
    }

    public @NotNull SimulationToMqttConfig getSimulationToMqttConfig() {
        return simulationToMqttConfig;
    }

    public int getMinValue() {
        return minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public int getMaxDelay() {
        return maxDelay;
    }

    public int getMinDelay() {
        return minDelay;
    }

    @Override
    public String toString() {
        return "SimulationSpecificAdapterConfig{" +
                "id='" +
                id +
                '\'' +
                ", simulationToMqttConfig=" +
                simulationToMqttConfig +
                ", minValue=" +
                minValue +
                ", maxValue=" +
                maxValue +
                ", minDelay=" +
                minDelay +
                ", maxDelay=" +
                maxDelay +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final SimulationSpecificAdapterConfig that = (SimulationSpecificAdapterConfig) o;
        return getMinValue() == that.getMinValue() &&
                getMaxValue() == that.getMaxValue() &&
                getMinDelay() == that.getMinDelay() &&
                getMaxDelay() == that.getMaxDelay() &&
                Objects.equals(id, that.id) &&
                Objects.equals(getSimulationToMqttConfig(), that.getSimulationToMqttConfig());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id,
                getSimulationToMqttConfig(),
                getMinValue(),
                getMaxValue(),
                getMinDelay(),
                getMaxDelay());
    }
}
