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
package com.hivemq.protocols.v2.wrapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.protocols.v2.runtime.ProtocolAdapterMetrics;
import com.hivemq.protocols.v2.southbound.InMemorySouthboundCommandSource;
import com.hivemq.protocols.v2.southbound.SouthboundWritePump;
import com.hivemq.protocols.v2.tag.SouthboundWriteOutcome;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * Option D at the wrapper boundary: the write aspect stays strictly single-in-flight and <b>never queues</b>,
 * every write carries a completion the aspect settles once, and a {@link SouthboundWritePump} uses that signal to
 * hold the next write until the current one completes — so a burst never reaches the adapter as a second in-flight
 * write, and the backlog waits in the pump. Driven on {@code FakeClock} + {@code ManualDispatcher} through the real
 * wrapper, observed through the published snapshot and the shared metric registry.
 */
class SouthboundWriteBackPressureTest {

    private static final @NotNull String TAG = "setpoint";

    @Test
    void writeCompletionSettlesWithTheDeviceOutcome() {
        final WrapperTestFixture fixture = writeOnlyFixture();
        fixture.activate(ProtocolAdapterDirection.SOUTHBOUND);
        final Node node = fixture.nodeFor(TAG);
        final List<SouthboundWriteOutcome> outcomes = new ArrayList<>();

        fixture.send(new ProtocolAdapterWrapperWriteRequest(node, dataPoint("1"), outcomes::add));
        assertThat(fixture.writeState(TAG)).isEqualTo("WAITING_FOR_WRITE_RESULT");
        assertThat(outcomes).isEmpty(); // not settled until the device acknowledges

        fixture.output.writeResult(node, true, null);
        fixture.drain();
        assertThat(fixture.writeState(TAG)).isEqualTo("WAITING_FOR_WRITE_REQUEST");
        assertThat(outcomes).containsExactly(SouthboundWriteOutcome.SUCCEEDED);

        // A failed write settles FAILED (and counts) but never flaps the tag to ERROR.
        fixture.send(new ProtocolAdapterWrapperWriteRequest(node, dataPoint("2"), outcomes::add));
        fixture.output.writeResult(node, false, "device rejected the value");
        fixture.drain();
        assertThat(outcomes).containsExactly(SouthboundWriteOutcome.SUCCEEDED, SouthboundWriteOutcome.FAILED);
        assertThat(fixture.tag(TAG).failureCount()).isEqualTo(1);
        assertThat(fixture.writeState(TAG)).isEqualTo("WAITING_FOR_WRITE_REQUEST");
    }

    @Test
    void secondWriteWhileOneIsInFlight_isRejectedBusy_notQueued() {
        final WrapperTestFixture fixture = writeOnlyFixture();
        fixture.activate(ProtocolAdapterDirection.SOUTHBOUND);
        final Node node = fixture.nodeFor(TAG);
        final List<SouthboundWriteOutcome> first = new ArrayList<>();
        final List<SouthboundWriteOutcome> second = new ArrayList<>();

        fixture.send(new ProtocolAdapterWrapperWriteRequest(node, dataPoint("1"), first::add));
        assertThat(fixture.writeState(TAG)).isEqualTo("WAITING_FOR_WRITE_RESULT");

        // A second write arriving while one is in flight is rejected immediately — not queued.
        fixture.send(new ProtocolAdapterWrapperWriteRequest(node, dataPoint("2"), second::add));
        assertThat(second).containsExactly(SouthboundWriteOutcome.REJECTED_BUSY);
        assertThat(first).isEmpty(); // the first write is still in flight, untouched
        assertThat(fixture.writeState(TAG)).isEqualTo("WAITING_FOR_WRITE_RESULT");
        assertThat(writesRejected(fixture)).isEqualTo(1);

        // Acknowledging the first returns straight to rest — there was no queued second write to issue.
        fixture.output.writeResult(node, true, null);
        fixture.drain();
        assertThat(fixture.writeState(TAG)).isEqualTo("WAITING_FOR_WRITE_REQUEST");
        assertThat(first).containsExactly(SouthboundWriteOutcome.SUCCEEDED);
    }

    @Test
    void inFlightWriteIsAbortedWhenTheWriteCycleIsTornDown() {
        final WrapperTestFixture fixture = writeOnlyFixture();
        fixture.activate(ProtocolAdapterDirection.SOUTHBOUND);
        final Node node = fixture.nodeFor(TAG);
        final List<SouthboundWriteOutcome> outcomes = new ArrayList<>();

        fixture.send(new ProtocolAdapterWrapperWriteRequest(node, dataPoint("1"), outcomes::add));
        assertThat(fixture.writeState(TAG)).isEqualTo("WAITING_FOR_WRITE_RESULT");

        // Southbound is turned off while a write is in flight: the write is abandoned, and its completion is
        // settled ABORTED so a back-pressuring producer is released rather than left waiting forever.
        fixture.deactivate(ProtocolAdapterDirection.SOUTHBOUND);
        assertThat(outcomes).containsExactly(SouthboundWriteOutcome.ABORTED);
        assertThat(fixture.writeState(TAG)).isEqualTo("DEACTIVATED");
    }

    @Test
    void pump_serializesABurst_soTheAdapterNeverSeesASecondInFlightWrite() {
        final WrapperTestFixture fixture = writeOnlyFixture();
        fixture.activate(ProtocolAdapterDirection.SOUTHBOUND);
        final Node node = fixture.nodeFor(TAG);
        final InMemorySouthboundCommandSource source = new InMemorySouthboundCommandSource(1_000);
        final SouthboundWritePump pump = new SouthboundWritePump(fixture.mailbox, node, source);

        final int burst = 30;
        for (int i = 0; i < burst; i++) {
            source.offer(dataPoint(Integer.toString(i)));
        }
        fixture.drain(); // process the single write the pump forwarded

        // Exactly one write reached the adapter; all the rest wait in the (durable) source, none committed yet.
        assertThat(fixture.writeState(TAG)).isEqualTo("WAITING_FOR_WRITE_RESULT");
        assertThat(pump.inFlight()).isTrue();
        assertThat(source.pendingSize()).isEqualTo(burst);

        // Each acknowledgment commits the current command and releases exactly the next; drain and repeat.
        int acknowledgments = 0;
        while (pump.inFlight()) {
            fixture.output.writeResult(node, true, null);
            fixture.drain();
            acknowledgments++;
        }

        assertThat(acknowledgments).isEqualTo(burst); // every write was delivered, one at a time
        assertThat(pump.forwarded()).isEqualTo(burst);
        assertThat(pump.delivered()).isEqualTo(burst);
        assertThat(source.pendingSize()).isZero(); // all committed — drained from the source
        assertThat(pump.rejectedByAdapter()).isZero();
        // The adapter never rejected a write — the single-in-flight invariant held for the whole burst.
        assertThat(writesRejected(fixture)).isZero();
        assertThat(fixture.tag(TAG).failureCount()).isZero();
        assertThat(fixture.writeState(TAG)).isEqualTo("WAITING_FOR_WRITE_REQUEST");
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────────────────

    private static @NotNull WrapperTestFixture writeOnlyFixture() {
        return WrapperTestFixture.builder()
                .runningCoordinator()
                .nodes(List.of(WrapperTestSupport.pair(TAG)))
                .readUsed(Set.of())
                .writeUsed(Set.of(TAG))
                .build();
    }

    private static @NotNull DataPoint dataPoint(final @NotNull String value) {
        return WrapperTestSupport.dataPoint(TAG, value);
    }

    private static long writesRejected(final @NotNull WrapperTestFixture fixture) {
        return fixture.metricRegistry
                .counter(ProtocolAdapterMetrics.ADAPTER_PREFIX + fixture.adapterId + ".tag." + TAG + ".writes.rejected")
                .getCount();
    }
}
