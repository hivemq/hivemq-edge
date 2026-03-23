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
package com.hivemq.edge.modules.adapters.adaptermetrics;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class AdapterMetricsSpecificAdapterConfig implements ProtocolSpecificAdapterConfig {

    @JsonProperty(value = "id", required = true, access = JsonProperty.Access.WRITE_ONLY)
    @ModuleConfigField(
            title = "Identifier",
            description = "Unique identifier for this protocol adapter",
            format = ModuleConfigField.FieldType.IDENTIFIER,
            required = true)
    private @Nullable String id;

    @JsonProperty("pollingIntervalMillis")
    @ModuleConfigField(
            title = "Polling Interval (ms)",
            description = "How often to sample the observed adapter metrics, in milliseconds",
            numberMin = 100,
            defaultValue = "60000")
    private final int pollingIntervalMillis;

    @JsonProperty("maxPollingErrorsBeforeRemoval")
    @ModuleConfigField(
            title = "Max Polling Errors",
            description = "Number of consecutive polling errors before the adapter is removed",
            numberMin = 1,
            defaultValue = "3")
    private final int maxPollingErrorsBeforeRemoval;

    @JsonCreator
    public AdapterMetricsSpecificAdapterConfig(
            @JsonProperty("pollingIntervalMillis") final @Nullable Integer pollingIntervalMillis,
            @JsonProperty("maxPollingErrorsBeforeRemoval") final @Nullable Integer maxPollingErrorsBeforeRemoval) {
        this.pollingIntervalMillis = Objects.requireNonNullElse(pollingIntervalMillis, 60_000);
        this.maxPollingErrorsBeforeRemoval = Objects.requireNonNullElse(maxPollingErrorsBeforeRemoval, 3);
    }

    public int getPollingIntervalMillis() {
        return pollingIntervalMillis;
    }

    public int getMaxPollingErrorsBeforeRemoval() {
        return maxPollingErrorsBeforeRemoval;
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof AdapterMetricsSpecificAdapterConfig that)) return false;
        return pollingIntervalMillis == that.pollingIntervalMillis
                && maxPollingErrorsBeforeRemoval == that.maxPollingErrorsBeforeRemoval;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pollingIntervalMillis, maxPollingErrorsBeforeRemoval);
    }
}
