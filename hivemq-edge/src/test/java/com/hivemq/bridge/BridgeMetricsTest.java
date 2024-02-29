package com.hivemq.bridge;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.bridge.metrics.PerBridgeMetrics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Simon L Johnson
 */
public class BridgeMetricsTest {

    static final String ARBITRARY_METRIC = "arbitrary-metric";
    @Test
    void testTearDownMetrics() {

        MetricRegistry registry = new MetricRegistry();

        //adapter helper creates 10 metrics
        PerBridgeMetrics perBridgeMetrics = new PerBridgeMetrics("bridge-name", registry);

        assertEquals(10, registry.getMetrics().size(), "Number of metrics should match");

        PerBridgeMetrics perBridgeMetrics2 = new PerBridgeMetrics("bridge-name2", registry);

        assertEquals(20, registry.getMetrics().size(), "Number of metrics should match");

        //add an arbitrary fifth
        registry.counter(ARBITRARY_METRIC).inc();

        assertEquals(21, registry.getMetrics().size(), "Number of metrics should match");

        perBridgeMetrics.clearAll(registry);

        assertEquals(11, registry.getMetrics().size(), "Number of metrics should match");

        perBridgeMetrics2.clearAll(registry);

        assertEquals(1, registry.getMetrics().size(), "Number of metrics should match");

        assertEquals(1, registry.getCounters().get(ARBITRARY_METRIC).getCount(), "Matching success data point should be incremented");

    }

}
