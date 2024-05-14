package com.hivemq.protocols;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import org.junit.jupiter.api.Test;

import static com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService.PROTOCOL_ADAPTER_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProtocolAdapterMetricsTest {

    private final MetricRegistry metricRegistry = new MetricRegistry();
    private final ProtocolAdapterMetrics protocolAdapterMetrics = new ProtocolAdapterMetrics(metricRegistry);


    @Test
    void increaseProtocolAdapterMetric_whenAlreadyExists_thenIncrementAndRegister() {
        protocolAdapterMetrics.increaseProtocolAdapterMetric("test");

        final Counter counter = metricRegistry.getCounters().get(PROTOCOL_ADAPTER_PREFIX + "test.current");
        assertNotNull(counter);
        assertEquals(1L, counter.getCount());
    }

    @Test
    void increaseProtocolAdapterMetric_whenAlreadyExists_thenIncrement() {
        metricRegistry.counter(PROTOCOL_ADAPTER_PREFIX + "test.current").inc();

        protocolAdapterMetrics.increaseProtocolAdapterMetric("test");

        final Counter counter = metricRegistry.getCounters().get(PROTOCOL_ADAPTER_PREFIX + "test.current");
        assertNotNull(counter);
        assertEquals(2L, counter.getCount());
    }

    @Test
    void decreaseProtocolAdapterMetric() {
        metricRegistry.counter(PROTOCOL_ADAPTER_PREFIX + "test.current").inc(2);

        protocolAdapterMetrics.decreaseProtocolAdapterMetric("test");

        final Counter counter = metricRegistry.getCounters().get(PROTOCOL_ADAPTER_PREFIX + "test.current");
        assertNotNull(counter);
        assertEquals(1L, counter.getCount());
    }
}
