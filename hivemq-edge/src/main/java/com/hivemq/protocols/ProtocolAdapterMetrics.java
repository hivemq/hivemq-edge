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
package com.hivemq.protocols;

import com.codahale.metrics.MetricRegistry;
import org.jetbrains.annotations.NotNull;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.atomic.AtomicInteger;

import static com.hivemq.metrics.HiveMQMetrics.ADAPTERS_CURRENT;

@Singleton
public class ProtocolAdapterMetrics {

    public static final @NotNull String PROTOCOL_ADAPTER_PREFIX = "com.hivemq.edge.protocol-adapters.";

    private final @NotNull MetricRegistry metricRegistry;
    // we use this AtomicInteger in order to have a gauge for the current amount of adapters to
    // be consistent with the other metrics
    private final AtomicInteger currentAdapters = new AtomicInteger();

    @Inject
    public ProtocolAdapterMetrics(final @NotNull MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
        metricRegistry.registerGauge(ADAPTERS_CURRENT.name(), currentAdapters::intValue);
    }

    public void increaseProtocolAdapterMetric(final @NotNull String protocolType) {
        metricRegistry.counter(PROTOCOL_ADAPTER_PREFIX + protocolType + ".current").inc();
        currentAdapters.incrementAndGet();
    }

    public void decreaseProtocolAdapterMetric(final @NotNull String protocolType) {
        metricRegistry.counter(PROTOCOL_ADAPTER_PREFIX + protocolType + ".current").dec();
        currentAdapters.decrementAndGet();
    }
}
