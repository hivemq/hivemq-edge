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
package com.hivemq.edge.adapters.snmp.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

public class SnmpToMqttConfig {

    public static final int DEFAULT_POLLING_INTERVAL_MILLIS = 1000;
    public static final int DEFAULT_MAX_POLLING_ERRORS_BEFORE_REMOVAL = 10;

    @JsonProperty(value = "pollingIntervalMillis")
    @ModuleConfigField(
            title = "Polling Interval",
            description = "Interval in milliseconds between polling attempts",
            numberMin = 100,
            numberMax = 86400000,
            defaultValue = "1000")
    private final int pollingIntervalMillis;

    @JsonProperty(value = "maxPollingErrorsBeforeRemoval")
    @ModuleConfigField(
            title = "Max Polling Errors",
            description = "Maximum number of consecutive polling errors before the adapter is stopped",
            numberMin = 1,
            numberMax = 100,
            defaultValue = "10")
    private final int maxPollingErrorsBeforeRemoval;

    @JsonProperty(value = "publishChangedDataOnly")
    @ModuleConfigField(
            title = "Publish Changed Data Only",
            description = "Only publish data points when their values have changed",
            defaultValue = "false")
    private final boolean publishChangedDataOnly;

    @JsonCreator
    public SnmpToMqttConfig(
            @JsonProperty(value = "pollingIntervalMillis") final @Nullable Integer pollingIntervalMillis,
            @JsonProperty(value = "maxPollingErrorsBeforeRemoval")
                    final @Nullable Integer maxPollingErrorsBeforeRemoval,
            @JsonProperty(value = "publishChangedDataOnly") final @Nullable Boolean publishChangedDataOnly) {
        this.pollingIntervalMillis = Objects.requireNonNullElse(pollingIntervalMillis, DEFAULT_POLLING_INTERVAL_MILLIS);
        this.maxPollingErrorsBeforeRemoval =
                Objects.requireNonNullElse(maxPollingErrorsBeforeRemoval, DEFAULT_MAX_POLLING_ERRORS_BEFORE_REMOVAL);
        this.publishChangedDataOnly = Objects.requireNonNullElse(publishChangedDataOnly, false);
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
        if (!(o instanceof SnmpToMqttConfig that)) {
            return false;
        }
        return pollingIntervalMillis == that.pollingIntervalMillis
                && maxPollingErrorsBeforeRemoval == that.maxPollingErrorsBeforeRemoval
                && publishChangedDataOnly == that.publishChangedDataOnly;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pollingIntervalMillis, maxPollingErrorsBeforeRemoval, publishChangedDataOnly);
    }
}
