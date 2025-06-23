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
package com.hivemq.edge.adapters.modbus.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static java.util.Objects.requireNonNullElse;

/**
 * @since 4.9.0
 */
public class ModbusToMqttConfig {

    private static final String DEFAULT_POLL_INTERVAL_MILLIS_STRING = "1000";
    public static final int DEFAULT_POLL_INTERVAL_MILLIS = Integer.parseInt(DEFAULT_POLL_INTERVAL_MILLIS_STRING);
    private static final String DEFAULT_MAX_POLL_ERRORS_BEFORE_REMOVAL_STRING = "10";

    public static final int DEFAULT_MAX_POLL_ERRORS_BEFORE_REMOVAL =
            Integer.parseInt(DEFAULT_MAX_POLL_ERRORS_BEFORE_REMOVAL_STRING);
    private static final String DEFAULT_PUBLISH_CHANGED_DATA_ONLY_STRING = "true";
    public static final boolean DEFAULT_PUBLISH_CHANGED_DATA_ONLY =
            Boolean.parseBoolean(DEFAULT_PUBLISH_CHANGED_DATA_ONLY_STRING);

    @JsonProperty("pollingIntervalMillis")
    @ModuleConfigField(title = "Polling Interval [ms]",
                       description = "Time in millisecond that this endpoint will be polled",
                       numberMin = 1,
                       defaultValue = DEFAULT_POLL_INTERVAL_MILLIS_STRING)
    private final int pollingIntervalMillis;

    @JsonProperty("maxPollingErrorsBeforeRemoval")
    @ModuleConfigField(title = "Max. Polling Errors",
                       description = "Max. errors polling the endpoint before the polling daemon is stopped (-1 for unlimited retries)",
                       numberMin = -1,
                       defaultValue = DEFAULT_MAX_POLL_ERRORS_BEFORE_REMOVAL_STRING)
    private final int maxPollingErrorsBeforeRemoval;

    @JsonProperty("publishChangedDataOnly")
    @ModuleConfigField(title = "Only publish data items that have changed since last poll",
                       defaultValue = DEFAULT_PUBLISH_CHANGED_DATA_ONLY_STRING,
                       format = ModuleConfigField.FieldType.BOOLEAN)
    private final boolean publishChangedDataOnly;

    @JsonCreator
    public ModbusToMqttConfig(
            @JsonProperty(value = "pollingIntervalMillis") final @Nullable Integer pollingIntervalMillis,
            @JsonProperty(value = "maxPollingErrorsBeforeRemoval") final @Nullable Integer maxPollingErrorsBeforeRemoval,
            @JsonProperty(value = "publishChangedDataOnly") final @Nullable Boolean publishChangedDataOnly) {
        this.pollingIntervalMillis = requireNonNullElse(pollingIntervalMillis, DEFAULT_POLL_INTERVAL_MILLIS);
        this.maxPollingErrorsBeforeRemoval =
                requireNonNullElse(maxPollingErrorsBeforeRemoval, DEFAULT_MAX_POLL_ERRORS_BEFORE_REMOVAL);
        this.publishChangedDataOnly = requireNonNullElse(publishChangedDataOnly, DEFAULT_PUBLISH_CHANGED_DATA_ONLY);
    }

    public int getPollingIntervalMillis() {
        return pollingIntervalMillis;
    }

    public int getMaxPollingErrorsBeforeRemoval() {
        return maxPollingErrorsBeforeRemoval;
    }

    public boolean getPublishChangedDataOnly() {
        return publishChangedDataOnly;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ModbusToMqttConfig that = (ModbusToMqttConfig) o;
        return getPollingIntervalMillis() == that.getPollingIntervalMillis() &&
                getMaxPollingErrorsBeforeRemoval() == that.getMaxPollingErrorsBeforeRemoval() &&
                getPublishChangedDataOnly() == that.getPublishChangedDataOnly();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPollingIntervalMillis(),
                getMaxPollingErrorsBeforeRemoval(),
                getPublishChangedDataOnly());
    }
}
