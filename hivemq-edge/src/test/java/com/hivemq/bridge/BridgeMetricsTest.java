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
package com.hivemq.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.bridge.metrics.PerBridgeMetrics;
import org.junit.jupiter.api.Test;

/**
 * @author Simon L Johnson
 */
public class BridgeMetricsTest {

    static final String ARBITRARY_METRIC = "arbitrary-metric";

    @Test
    void testTearDownMetrics() {

        MetricRegistry registry = new MetricRegistry();

        // adapter helper creates 10 metrics
        PerBridgeMetrics perBridgeMetrics = new PerBridgeMetrics("bridge-name", registry);

        assertEquals(10, registry.getMetrics().size(), "Number of metrics should match");

        PerBridgeMetrics perBridgeMetrics2 = new PerBridgeMetrics("bridge-name2", registry);

        assertEquals(20, registry.getMetrics().size(), "Number of metrics should match");

        // add an arbitrary fifth
        registry.counter(ARBITRARY_METRIC).inc();

        assertEquals(21, registry.getMetrics().size(), "Number of metrics should match");

        perBridgeMetrics.clearAll(registry);

        assertEquals(11, registry.getMetrics().size(), "Number of metrics should match");

        perBridgeMetrics2.clearAll(registry);

        assertEquals(1, registry.getMetrics().size(), "Number of metrics should match");

        assertEquals(
                1,
                registry.getCounters().get(ARBITRARY_METRIC).getCount(),
                "Matching success data point should be incremented");
    }
}
