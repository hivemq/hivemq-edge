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
package com.hivemq.edge.adapters.etherip.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class EipToMqttConfig {

    @JsonProperty("pollingIntervalMillis")
    @ModuleConfigField(title = "Polling Interval [ms]",
                       description = "Time in millisecond that this endpoint will be polled",
                       numberMin = 1,
                       defaultValue = "1000")
    private final int pollingIntervalMillis;

    @JsonProperty("maxPollingErrorsBeforeRemoval")
    @ModuleConfigField(title = "Max. Polling Errors",
                       description = "Max. errors polling the endpoint before the polling daemon is stopped (-1 for unlimited retries)",
                       numberMin = -1,
                       defaultValue = "10")
    private final int maxPollingErrorsBeforeRemoval;

    @JsonProperty("publishChangedDataOnly")
    @ModuleConfigField(title = "Only publish data items that have changed since last poll",
                       defaultValue = "true",
                       format = ModuleConfigField.FieldType.BOOLEAN)
    private final boolean publishChangedDataOnly;

    @JsonCreator
    public EipToMqttConfig(
            @JsonProperty(value = "pollingIntervalMillis") final @Nullable Integer pollingIntervalMillis,
            @JsonProperty(value = "maxPollingErrorsBeforeRemoval") final @Nullable Integer maxPollingErrorsBeforeRemoval,
            @JsonProperty(value = "publishChangedDataOnly") final @Nullable Boolean publishChangedDataOnly) {
        this.pollingIntervalMillis = Objects.requireNonNullElse(pollingIntervalMillis, 1000);
        this.maxPollingErrorsBeforeRemoval = Objects.requireNonNullElse(maxPollingErrorsBeforeRemoval, 10);
        this.publishChangedDataOnly = Objects.requireNonNullElse(publishChangedDataOnly, true);
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
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final EipToMqttConfig that = (EipToMqttConfig) o;
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
