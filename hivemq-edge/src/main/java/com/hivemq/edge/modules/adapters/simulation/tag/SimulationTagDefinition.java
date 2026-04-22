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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.annotations.MutuallyExclusiveFields;
import com.hivemq.adapter.sdk.api.tag.TagDefinition;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@MutuallyExclusiveFields(
        value = {"randomValue", "staticValue"},
        titles = {"Random Value", "Static Value"},
        includeDefault = true,
        defaultTitle = "Default (adapter-level random double)",
        groupTitle = "Tag Configuration")
public class SimulationTagDefinition implements TagDefinition {

    @JsonProperty("randomValue")
    @ModuleConfigField(
            title = "Random Value",
            description = "Configure the tag to emit a random number in a per-tag range.")
    private final @Nullable RandomValueConfig randomValue;

    @JsonProperty("staticValue")
    @ModuleConfigField(
            title = "Static Value",
            description = "Configure the tag to emit a fixed typed value on every poll.")
    private final @Nullable StaticValueConfig staticValue;

    @JsonCreator
    public SimulationTagDefinition(
            @JsonProperty("randomValue") final @Nullable RandomValueConfig randomValue,
            @JsonProperty("staticValue") final @Nullable StaticValueConfig staticValue) {
        if (randomValue != null && staticValue != null) {
            throw new IllegalArgumentException(
                    "SimulationTagDefinition: at most one of `randomValue` / `staticValue` may be set");
        }
        this.randomValue = randomValue;
        this.staticValue = staticValue;
    }

    public SimulationTagDefinition() {
        this(null, null);
    }

    public @Nullable RandomValueConfig getRandomValue() {
        return randomValue;
    }

    public @Nullable StaticValueConfig getStaticValue() {
        return staticValue;
    }

    /**
     * Derived kind: {@link SimulationTagType#RANDOM_NUMBER} iff {@code randomValue} is set,
     * {@link SimulationTagType#STATIC_VALUE} iff {@code staticValue} is set, otherwise
     * {@link SimulationTagType#LEGACY_RANDOM_DOUBLE} (default behavior — random double from adapter-level min/max).
     */
    public @NotNull SimulationTagType getType() {
        if (randomValue != null) {
            return SimulationTagType.RANDOM_NUMBER;
        }
        if (staticValue != null) {
            return SimulationTagType.STATIC_VALUE;
        }
        return SimulationTagType.LEGACY_RANDOM_DOUBLE;
    }

    @Override
    public boolean equals(final Object o) {
        return o instanceof SimulationTagDefinition that
                && Objects.equals(randomValue, that.randomValue)
                && Objects.equals(staticValue, that.staticValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(randomValue, staticValue);
    }

    @Override
    public String toString() {
        return "SimulationTagDefinition{randomValue=" + randomValue + ", staticValue=" + staticValue + '}';
    }
}
