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
package com.hivemq.edge.modules.adapters.simulation.tag;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class RandomValueConfig {

    @JsonProperty(value = "valueType", required = true)
    @ModuleConfigField(
            title = "Value Type",
            description = "Numeric type emitted by this tag.",
            enumDisplayValues = {"Integer (32-bit)", "Long (64-bit)", "Double"},
            required = true)
    private final @NotNull SimulationValueType valueType;

    @JsonProperty(value = "minValue", required = true)
    @ModuleConfigField(title = "Minimum Value", description = "Inclusive lower bound.", required = true)
    private final double minValue;

    @JsonProperty(value = "maxValue", required = true)
    @ModuleConfigField(title = "Maximum Value (Excl.)", description = "Exclusive upper bound.", required = true)
    private final double maxValue;

    @JsonCreator
    public RandomValueConfig(
            @JsonProperty("valueType") final @NotNull SimulationValueType valueType,
            @JsonProperty("minValue") final double minValue,
            @JsonProperty("maxValue") final double maxValue) {
        if (valueType == SimulationValueType.STRING) {
            throw new IllegalArgumentException("randomValue.valueType must be INT, LONG or DOUBLE");
        }
        if (minValue >= maxValue) {
            throw new IllegalArgumentException(
                    "randomValue requires minValue < maxValue, was: " + minValue + " / " + maxValue);
        }

        if(valueType == SimulationValueType.INT) {
            if (minValue < Integer.MIN_VALUE || maxValue > (long) Integer.MAX_VALUE + 1) {
                throw new IllegalArgumentException(
                        "randomValue with valueType INT requires minValue >= " + Integer.MIN_VALUE
                                + " and maxValue <= " + ((long) Integer.MAX_VALUE + 1));
            }
        } else if (valueType == SimulationValueType.LONG) {
            if (minValue < Long.MIN_VALUE || maxValue > (double) Long.MAX_VALUE + 1) {
                throw new IllegalArgumentException(
                        "randomValue with valueType LONG requires minValue >= " + Long.MIN_VALUE
                                + " and maxValue <= " + ((double) Long.MAX_VALUE + 1));
            }
        }
        this.valueType = valueType;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public @NotNull SimulationValueType getValueType() {
        return valueType;
    }

    public double getMinValue() {
        return minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    @Override
    public boolean equals(final Object o) {
        return o instanceof final RandomValueConfig that
                && valueType == that.valueType
                && Double.compare(minValue, that.minValue) == 0
                && Double.compare(maxValue, that.maxValue) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(valueType, minValue, maxValue);
    }

    @Override
    public String toString() {
        return "RandomValueConfig{valueType=" + valueType + ", minValue=" + minValue + ", maxValue=" + maxValue + '}';
    }
}
