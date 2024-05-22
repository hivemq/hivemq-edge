package com.hivemq.protocols;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;
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
