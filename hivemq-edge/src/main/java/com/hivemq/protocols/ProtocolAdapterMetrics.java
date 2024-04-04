package com.hivemq.protocols;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.hivemq.metrics.HiveMQMetrics.ADAPTERS_CURRENT;
import static com.hivemq.metrics.HiveMQMetrics.PROTOCOL_ADAPTER_PREFIX;

@Singleton
public class ProtocolAdapterMetrics {
    private final @NotNull MetricRegistry metricRegistry;

    @Inject
    public ProtocolAdapterMetrics(final @NotNull MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    public void increaseProtocolAdapterMetric(final @NotNull String protocolType) {
        metricRegistry.counter(PROTOCOL_ADAPTER_PREFIX + protocolType + ".current").inc();
        metricRegistry.counter(ADAPTERS_CURRENT.name()).inc();
    }

    public void decreaseProtocolAdapterMetric(final @NotNull String protocolType) {
        metricRegistry.counter(PROTOCOL_ADAPTER_PREFIX + protocolType + ".current").dec();
        metricRegistry.counter(ADAPTERS_CURRENT.name()).inc();
    }
}
