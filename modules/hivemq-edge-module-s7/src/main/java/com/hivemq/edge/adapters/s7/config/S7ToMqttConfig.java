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
package com.hivemq.edge.adapters.s7.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class S7ToMqttConfig {

    public static final String DEFAULT_POLLING_INTERVAL_MS = "1000";
    public static final String DEFAULT_MAX_POLLING_ERRORS = "10";
    public static final String DEFAULT_PUBLISH_CHANGED_DATA_ONLY = "true";

    public static final String PROPERTY_POLLING_INTERVAL_MILLIS = "pollingIntervalMillis";
    public static final String PROPERTY_MAX_POLLING_ERRORS_BEFORE_REMOVAL = "maxPollingErrorsBeforeRemoval";
    public static final String PROPERTY_PUBLISH_CHANGED_DATA_ONLY = "publishChangedDataOnly";

    @JsonProperty(PROPERTY_POLLING_INTERVAL_MILLIS)
    @ModuleConfigField(title = "Polling Interval [ms]",
                       description = "Time in millisecond that this endpoint will be polled",
                       numberMin = 1,
                       defaultValue = DEFAULT_POLLING_INTERVAL_MS)
    private final int pollingIntervalMillis;

    @JsonProperty(PROPERTY_MAX_POLLING_ERRORS_BEFORE_REMOVAL)
    @ModuleConfigField(title = "Max. Polling Errors",
                       description = "Max. errors polling the endpoint before the polling daemon is stopped (-1 for unlimited retries)",
                       numberMin = -1,
                       defaultValue = DEFAULT_MAX_POLLING_ERRORS)
    private final int maxPollingErrorsBeforeRemoval;

    @JsonProperty(PROPERTY_PUBLISH_CHANGED_DATA_ONLY)
    @ModuleConfigField(title = "Only publish data items that have changed since last poll",
                       defaultValue = DEFAULT_PUBLISH_CHANGED_DATA_ONLY,
                       format = ModuleConfigField.FieldType.BOOLEAN)
    private final boolean publishChangedDataOnly;


    public S7ToMqttConfig(
            @JsonProperty(value = PROPERTY_POLLING_INTERVAL_MILLIS) final @Nullable Integer pollingIntervalMillis,
            @JsonProperty(value = PROPERTY_MAX_POLLING_ERRORS_BEFORE_REMOVAL) final @Nullable Integer maxPollingErrorsBeforeRemoval,
            @JsonProperty(value = PROPERTY_PUBLISH_CHANGED_DATA_ONLY) final @Nullable Boolean publishChangedDataOnly) {
        this.pollingIntervalMillis = Objects.requireNonNullElse(pollingIntervalMillis,
                Integer.valueOf(DEFAULT_POLLING_INTERVAL_MS));
        this.maxPollingErrorsBeforeRemoval = Objects.requireNonNullElse(maxPollingErrorsBeforeRemoval,
                Integer.valueOf(DEFAULT_MAX_POLLING_ERRORS));
        this.publishChangedDataOnly = Objects.requireNonNullElse(publishChangedDataOnly,
                Boolean.valueOf(DEFAULT_PUBLISH_CHANGED_DATA_ONLY));
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
    public String toString() {
        return "S7ToMqttConfig{" +
                "pollingIntervalMillis=" +
                pollingIntervalMillis +
                ", maxPollingErrorsBeforeRemoval=" +
                maxPollingErrorsBeforeRemoval +
                ", publishChangedDataOnly=" +
                publishChangedDataOnly +
                '}';
    }
}
