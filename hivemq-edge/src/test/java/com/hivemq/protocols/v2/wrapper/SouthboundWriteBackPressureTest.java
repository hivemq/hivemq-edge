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
import com.hivemq.protocols.v2.southbound.InMemorySouthboundWriteBacklog;
import com.hivemq.protocols.v2.southbound.SouthboundWriteQueue;
import com.hivemq.protocols.v2.tag.SouthboundWriteOutcome;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The southbound write contract at the wrapper boundary: the write aspect stays strictly single-in-flight and
 * <b>never queues</b> (an advertised in-flight window of one), every write carries a completion the aspect settles
 * once, and a {@link SouthboundWriteQueue} paces delivery to that window — so a burst never reaches the adapter as
 * a second in-flight write, and the backlog waits in the durable store behind the queue. Driven on
 * {@code FakeClock} + {@code ManualDispatcher} through the real wrapper, observed through the published snapshot
 * and the shared metric registry.
 */
class SouthboundWriteBackPressureTest {

    private static final @NotNull String TAG = "setpoint";

    @Test
    void writeCompletionSettlesWithTheDeviceOutcome() {
        final WrapperTestFixture fixture = writeOnlyFixture();
        fixture.activate(ProtocolAdapterDirection.SOUTHBOUND);
        final Node node = fixture.nodeFor(TAG);
        final List<SouthboundWriteOutcome> outcomes = new ArrayList<>();

        fixture.send(new ProtocolAdapterWrapperWriteRequest(
                node, dataPoint("1"), (outcome, reason) -> outcomes.add(outcome)));
        assertThat(fixture.writeState(TAG)).isEqualTo("WAITING_FOR_WRITE_RESULT");
        assertThat(outcomes).isEmpty(); // not settled until the device acknowledges

        fixture.output.writeResult(node, true, null);
        fixture.drain();
        assertThat(fixture.writeState(TAG)).isEqualTo("WAITING_FOR_WRITE_REQUEST");
        assertThat(outcomes).containsExactly(SouthboundWriteOutcome.SUCCEEDED);

        // A failed write settles FAILED (and counts) but never flaps the tag to ERROR.
        fixture.send(new ProtocolAdapterWrapperWriteRequest(
                node, dataPoint("2"), (outcome, reason) -> outcomes.add(outcome)));
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

        fixture.send(
                new ProtocolAdapterWrapperWriteRequest(node, dataPoint("1"), (outcome, reason) -> first.add(outcome)));
        assertThat(fixture.writeState(TAG)).isEqualTo("WAITING_FOR_WRITE_RESULT");

        // A second write arriving while one is in flight is rejected immediately — not queued.
        fixture.send(
                new ProtocolAdapterWrapperWriteRequest(node, dataPoint("2"), (outcome, reason) -> second.add(outcome)));
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

        fixture.send(new ProtocolAdapterWrapperWriteRequest(
                node, dataPoint("1"), (outcome, reason) -> outcomes.add(outcome)));
        assertThat(fixture.writeState(TAG)).isEqualTo("WAITING_FOR_WRITE_RESULT");

        // Southbound is turned off while a write is in flight: the write is abandoned, and its completion is
        // settled ABORTED so a back-pressuring producer is released rather than left waiting forever.
        fixture.deactivate(ProtocolAdapterDirection.SOUTHBOUND);
        assertThat(outcomes).containsExactly(SouthboundWriteOutcome.ABORTED);
        assertThat(fixture.writeState(TAG)).isEqualTo("DEACTIVATED");
    }

    @Test
    void queue_pacesABurstToTheWindow_soTheAdapterNeverSeesASecondInFlightWrite() {
        final WrapperTestFixture fixture = writeOnlyFixture();
        fixture.activate(ProtocolAdapterDirection.SOUTHBOUND);
        final Node node = fixture.nodeFor(TAG);
        final InMemorySouthboundWriteBacklog backlog = new InMemorySouthboundWriteBacklog(1_000);
        final SouthboundWriteQueue queue = new SouthboundWriteQueue(fixture.mailbox, node, backlog);

        final int burst = 30;
        for (int i = 0; i < burst; i++) {
            backlog.offer(dataPoint(Integer.toString(i)));
        }
        fixture.drain(); // process the single write the queue delivered

        // Exactly one write reached the adapter; all the rest wait in the (durable) backlog, none committed yet.
        assertThat(fixture.writeState(TAG)).isEqualTo("WAITING_FOR_WRITE_RESULT");
        assertThat(queue.inFlight()).isTrue();
        assertThat(backlog.pendingSize()).isEqualTo(burst);

        // Each acknowledgment commits the current command and delivers exactly the next; drain and repeat.
        int acknowledgments = 0;
        while (queue.inFlight()) {
            fixture.output.writeResult(node, true, null);
            fixture.drain();
            acknowledgments++;
        }

        assertThat(acknowledgments).isEqualTo(burst); // every write was delivered, one at a time
        assertThat(queue.deliveries()).isEqualTo(burst);
        assertThat(queue.committed()).isEqualTo(burst);
        assertThat(backlog.pendingSize()).isZero(); // all committed — deleted from the backlog
        assertThat(queue.windowViolations()).isZero();
        // The adapter never rejected a write — the single-in-flight invariant held for the whole burst.
        assertThat(writesRejected(fixture)).isZero();
        assertThat(fixture.tag(TAG).failureCount()).isZero();
        assertThat(fixture.writeState(TAG)).isEqualTo("WAITING_FOR_WRITE_REQUEST");
    }

    @Test
    void writeArrivingWhileTheAspectCannotWrite_settlesAborted_notAWindowViolation() {
        final WrapperTestFixture fixture = writeOnlyFixture();
        fixture.activate(ProtocolAdapterDirection.SOUTHBOUND);
        final Node node = fixture.nodeFor(TAG);
        fixture.deactivate(ProtocolAdapterDirection.SOUTHBOUND);
        assertThat(fixture.writeState(TAG)).isEqualTo("DEACTIVATED");
        final List<SouthboundWriteOutcome> outcomes = new ArrayList<>();

        // A write reaching a deactivated aspect is aborted — the retryable outcome — so a delivering queue keeps
        // the command for redelivery. It is NOT a window violation: nothing was in flight.
        fixture.send(new ProtocolAdapterWrapperWriteRequest(
                node, dataPoint("1"), (outcome, reason) -> outcomes.add(outcome)));

        assertThat(outcomes).containsExactly(SouthboundWriteOutcome.ABORTED);
        assertThat(writesRejected(fixture)).isZero();
        assertThat(fixture.tag(TAG).failureCount()).isZero();
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
