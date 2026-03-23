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
package com.hivemq.edge.modules.adapters.adaptermetrics.tag;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.tag.TagDefinition;
import com.hivemq.edge.modules.adapters.adaptermetrics.AdapterMetric;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class AdapterMetricsTagDefinition implements TagDefinition {

    @JsonProperty(value = "observedAdapterType", required = true)
    @ModuleConfigField(
            title = "Observed Adapter Type",
            description = "The protocol type of the adapter to observe (e.g. 'opcua', 'modbus')",
            required = true)
    private final @NotNull String observedAdapterType;

    @JsonProperty(value = "observedAdapterId", required = true)
    @ModuleConfigField(
            title = "Observed Adapter ID",
            description = "The ID of the adapter instance to observe",
            required = true)
    private final @NotNull String observedAdapterId;

    @JsonProperty(value = "metric", required = true)
    @ModuleConfigField(
            title = "Metric",
            description = "The metric to observe for the specified adapter",
            required = true)
    private final @NotNull AdapterMetric metric;

    @JsonCreator
    public AdapterMetricsTagDefinition(
            @JsonProperty(value = "observedAdapterType", required = true) final @NotNull String observedAdapterType,
            @JsonProperty(value = "observedAdapterId", required = true) final @NotNull String observedAdapterId,
            @JsonProperty(value = "metric", required = true) final @NotNull AdapterMetric metric) {
        this.observedAdapterType = observedAdapterType;
        this.observedAdapterId = observedAdapterId;
        this.metric = metric;
    }

    public @NotNull String getObservedAdapterType() {
        return observedAdapterType;
    }

    public @NotNull String getObservedAdapterId() {
        return observedAdapterId;
    }

    public @NotNull AdapterMetric getMetric() {
        return metric;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AdapterMetricsTagDefinition that)) return false;
        return Objects.equals(observedAdapterType, that.observedAdapterType)
                && Objects.equals(observedAdapterId, that.observedAdapterId)
                && metric == that.metric;
    }

    @Override
    public int hashCode() {
        return Objects.hash(observedAdapterType, observedAdapterId, metric);
    }
}
