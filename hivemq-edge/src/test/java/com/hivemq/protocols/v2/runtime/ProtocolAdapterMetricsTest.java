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
package com.hivemq.protocols.v2.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.MetricRegistry;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ProtocolAdapterMetricsTest {

    @Test
    void registersTheGaugesAndCounters() {
        final MetricRegistry registry = new MetricRegistry();
        try (ProtocolAdapterMetrics metrics = new ProtocolAdapterMetrics(registry, "adapter-1", () -> 0)) {
            assertThat(registry.getGauges())
                    .containsKeys(
                            "protocol-adapter-v2.adapter.adapter-1.mailbox.depth",
                            "protocol-adapter-v2.adapter.adapter-1.tick.lag");
            assertThat(registry.getCounters())
                    .containsKeys(
                            "protocol-adapter-v2.adapter.adapter-1.state.transitions",
                            "protocol-adapter-v2.adapter.adapter-1.defensive.resets");
        }
    }

    @Test
    void mailboxDepthGaugeReflectsItsSource() {
        final MetricRegistry registry = new MetricRegistry();
        final AtomicInteger depth = new AtomicInteger(3);
        try (ProtocolAdapterMetrics metrics = new ProtocolAdapterMetrics(registry, "adapter-1", depth::get)) {
            assertThat(registry.getGauges()
                            .get("protocol-adapter-v2.adapter.adapter-1.mailbox.depth")
                            .getValue())
                    .isEqualTo(3);
            depth.set(7);
            assertThat(registry.getGauges()
                            .get("protocol-adapter-v2.adapter.adapter-1.mailbox.depth")
                            .getValue())
                    .isEqualTo(7);
        }
    }

    @Test
    void countersIncrement() {
        final MetricRegistry registry = new MetricRegistry();
        try (ProtocolAdapterMetrics metrics = new ProtocolAdapterMetrics(registry, "adapter-1", () -> 0)) {
            metrics.incrementStateTransition();
            metrics.incrementStateTransition();
            metrics.incrementDefensiveReset();
            metrics.incrementTagFailure("temperature");
            metrics.incrementTagFailure("temperature");
            metrics.incrementTagFailure("pressure");

            assertThat(registry.counter("protocol-adapter-v2.adapter.adapter-1.state.transitions")
                            .getCount())
                    .isEqualTo(2);
            assertThat(registry.counter("protocol-adapter-v2.adapter.adapter-1.defensive.resets")
                            .getCount())
                    .isEqualTo(1);
            assertThat(registry.counter("protocol-adapter-v2.adapter.adapter-1.tag.temperature.failures")
                            .getCount())
                    .isEqualTo(2);
            assertThat(registry.counter("protocol-adapter-v2.adapter.adapter-1.tag.pressure.failures")
                            .getCount())
                    .isEqualTo(1);
        }
    }

    @Test
    void tickLagGaugeReflectsTheRecordedValue() {
        final MetricRegistry registry = new MetricRegistry();
        try (ProtocolAdapterMetrics metrics = new ProtocolAdapterMetrics(registry, "adapter-1", () -> 0)) {
            assertThat(registry.getGauges()
                            .get("protocol-adapter-v2.adapter.adapter-1.tick.lag")
                            .getValue())
                    .isEqualTo(0L);
            metrics.recordTickLag(42);
            assertThat(registry.getGauges()
                            .get("protocol-adapter-v2.adapter.adapter-1.tick.lag")
                            .getValue())
                    .isEqualTo(42L);
        }
    }

    @Test
    void closeDeregistersEveryMetric() {
        final MetricRegistry registry = new MetricRegistry();
        final ProtocolAdapterMetrics metrics = new ProtocolAdapterMetrics(registry, "adapter-1", () -> 0);
        metrics.incrementTagFailure("temperature");

        metrics.close();

        assertThat(registry.getMetrics()).isEmpty();
    }

    @Test
    void closeFreesTheNamesSoTheSameAdapterCanBeRecreated() {
        final MetricRegistry registry = new MetricRegistry();
        final ProtocolAdapterMetrics first = new ProtocolAdapterMetrics(registry, "adapter-1", () -> 0);
        first.close();

        // Re-registering the same names must not throw "duplicate metric".
        final ProtocolAdapterMetrics second = new ProtocolAdapterMetrics(registry, "adapter-1", () -> 0);
        second.close();

        assertThat(registry.getMetrics()).isEmpty();
    }
}
