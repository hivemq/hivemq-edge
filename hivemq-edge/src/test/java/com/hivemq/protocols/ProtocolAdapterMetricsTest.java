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

import static com.hivemq.protocols.ProtocolAdapterMetrics.PROTOCOL_ADAPTER_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import org.junit.jupiter.api.Test;

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
