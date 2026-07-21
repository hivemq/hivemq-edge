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
package com.hivemq.edge.adapters.databases.v2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * How a poll's result rows are shaped into northbound data points. This is the tag-level choice carried by a
 * {@link DatabaseNode}, replacing the v1 adapter's {@code spiltLinesInIndividualMessages} boolean with an explicit
 * three-way mode.
 * <p>
 * The JSON representation is the PascalCase name ({@code "AllInOne"}, {@code "OnePerRow"}, {@code "OnePerBatch"}) so a
 * configured node reads naturally; an absent value means {@link #ALL_IN_ONE}, and an unrecognized value is rejected
 * rather than silently defaulted.
 */
public enum SplitMode {

    /**
     * Every result row is carried in a single data point as one JSON array. The batch size is ignored.
     */
    ALL_IN_ONE("AllInOne"),

    /**
     * Each result row is carried in its own data point (its own JSON object, hence its own MQTT message). The batch
     * size determines how many rows are drained per output call (a cursor page size); each row is still its own data
     * point.
     */
    ONE_PER_ROW("OnePerRow"),

    /**
     * Each batch of rows is carried in its own data point as a JSON array of at most {@link DatabaseNode#batchSize()}
     * rows. The batch size determines how many rows each such array holds.
     */
    ONE_PER_BATCH("OnePerBatch");

    private final @NotNull String jsonValue;

    SplitMode(final @NotNull String jsonValue) {
        this.jsonValue = jsonValue;
    }

    /**
     * @return the PascalCase name this mode serializes to and is configured with.
     */
    @JsonValue
    public @NotNull String jsonValue() {
        return jsonValue;
    }

    /**
     * Resolve a configured split-mode value. An absent value takes the {@link #ALL_IN_ONE} default; a recognized
     * PascalCase name maps to its mode; any other value is rejected so a typo surfaces as a clear error rather than
     * silently becoming a different mode.
     *
     * @param value the configured value, or {@code null} when absent.
     * @return the resolved mode.
     * @throws IllegalArgumentException when {@code value} is present but not one of the recognized names.
     */
    @JsonCreator
    public static @NotNull SplitMode fromJson(final @Nullable String value) {
        if (value == null) {
            return ALL_IN_ONE;
        }
        for (final SplitMode mode : values()) {
            if (mode.jsonValue.equals(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException(
                "unknown split mode '" + value + "' (expected one of AllInOne, OnePerRow, OnePerBatch)");
    }
}
