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

public final class StaticValueConfig {

    @JsonProperty(value = "valueType", required = true)
    @ModuleConfigField(
            title = "Value Type",
            description = "Type of the static value.",
            enumDisplayValues = {"Integer (32-bit)", "Long (64-bit)", "Double", "String"},
            required = true)
    private final @NotNull SimulationValueType valueType;

    @JsonProperty(value = "value", required = true)
    @ModuleConfigField(
            title = "Value",
            description = "String form of the static value; parsed per valueType.",
            required = true)
    private final @NotNull String value;

    private final @NotNull Object parsedValue;

    @JsonCreator
    public StaticValueConfig(
            @JsonProperty("valueType") final @NotNull SimulationValueType valueType,
            @JsonProperty("value") final @NotNull String value) {
        this.valueType = valueType;
        this.value = value;
        this.parsedValue = parse(valueType, value);
    }

    private static @NotNull Object parse(
            final @NotNull SimulationValueType valueType, final @NotNull String value) {
        try {
            return switch (valueType) {
                case INT -> Integer.parseInt(value);
                case LONG -> Long.parseLong(value);
                case DOUBLE -> Double.parseDouble(value);
                case STRING -> value;
            };
        } catch (final NumberFormatException nfe) {
            throw new IllegalArgumentException(
                    "staticValue could not parse `" + value + "` as " + valueType, nfe);
        }
    }

    public @NotNull SimulationValueType getValueType() {
        return valueType;
    }

    public @NotNull String getValue() {
        return value;
    }

    public @NotNull Object getParsedValue() {
        return parsedValue;
    }

    @Override
    public boolean equals(final Object o) {
        return o instanceof StaticValueConfig that && valueType == that.valueType && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valueType, value);
    }

    @Override
    public String toString() {
        return "StaticValueConfig{valueType=" + valueType + ", value='" + value + "'}";
    }
}
