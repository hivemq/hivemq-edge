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
package com.hivemq.edge.adapters.chaos.harness;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.edge.adapters.chaos.ChaosDataPoint;
import com.hivemq.edge.adapters.chaos.ChaosEvent;
import com.hivemq.edge.adapters.chaos.ChaosScript;
import com.hivemq.edge.adapters.chaos.NodeMatcher;
import com.hivemq.edge.adapters.chaos.PollBehavior;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The harness is deterministic (test plan): the same immutable {@link ChaosScript} driven through the
 * same operations produces byte-for-byte identical command and event traces across runs — the property the whole
 * deterministic matrix relies on. Because {@code ChaosScript} is immutable and stateless, the very same script
 * instance drives every run; all per-run state lives in the simulator and the actor, both rebuilt fresh.
 */
class HarnessDeterminismTest {

    private static final @NotNull DataPoint VALUE = new ChaosDataPoint("temperature", "21");

    @Test
    void sameScriptAndSameAdvance_produceIdenticalCommandAndEventTraces() {
        final ChaosScript script = ChaosScript.builder()
                .poll(NodeMatcher.all(), PollBehavior.value(VALUE))
                .injectAtTick(2, ChaosEvent.disconnect()) // a spontaneous loss that drives a backoff + reconnect
                .build();

        final List<String> firstCommands = run(script).commandsSent();
        final List<String> firstEvents = run(script).eventsSeen();
        final ProtocolAdapterWrapperTestHarness second = run(script);

        assertThat(second.commandsSent()).isEqualTo(firstCommands);
        assertThat(second.eventsSeen()).isEqualTo(firstEvents);
        // And not vacuously empty — the run exercised real command and event traffic.
        assertThat(firstCommands).contains("start", "connect", "verifyBatch");
        assertThat(firstEvents).contains("started", "connected", "disconnected");
    }

    private static @NotNull ProtocolAdapterWrapperTestHarness run(final @NotNull ChaosScript script) {
        final ProtocolAdapterWrapperTestHarness harness = ProtocolAdapterWrapperTestHarness.with(script)
                .tickPeriodMillis(1000)
                .watchdogTimeoutMillis(1000)
                .pollIntervalMillis(1000);
        harness.activateNorthbound();
        harness.advance(4); // through the poll cadence and the injected spontaneous disconnect at tick 2
        return harness;
    }
}
