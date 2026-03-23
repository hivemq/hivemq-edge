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

import com.codahale.metrics.MetricRegistry;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import org.jetbrains.annotations.NotNull;

public class AdapterMetricsProtocolAdapterFactory
        implements ProtocolAdapterFactory<AdapterMetricsSpecificAdapterConfig> {

    private final @NotNull MetricRegistry metricRegistry;

    public AdapterMetricsProtocolAdapterFactory(final @NotNull MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @Override
    public @NotNull ProtocolAdapterInformation getInformation() {
        return AdapterMetricsProtocolAdapterInformation.INSTANCE;
    }

    @Override
    public @NotNull ProtocolAdapter createAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<AdapterMetricsSpecificAdapterConfig> input) {
        return new AdapterMetricsProtocolAdapter(adapterInformation, input, metricRegistry);
    }
}
