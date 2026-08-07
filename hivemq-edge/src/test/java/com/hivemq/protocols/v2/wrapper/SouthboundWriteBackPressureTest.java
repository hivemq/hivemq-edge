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
import com.hivemq.adapter.sdk.api.v2.model.WriteEntry;
import com.hivemq.protocols.v2.runtime.ProtocolAdapterMetrics;
import com.hivemq.protocols.v2.southbound.SouthboundWritePlane;
import com.hivemq.protocols.v2.southbound.SouthboundWriteQueue;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * The southbound contract at the wrapper boundary, driven through the whole real loop: a delivery plane is bound to
 * the wrapper exactly as the production factory binds it, so every command travels store → mailbox → channel →
 * mailbox → write aspect → mailbox → channel, all on the one dispatch thread.
 * <p>
 * What is pinned here is that the aspect stays strictly single-in-flight and never queues, and that the channel's
 * counters — commit, dead-letter, kept-for-redelivery, window violation — record exactly the outcome each write
 * reached.
 */
class SouthboundWriteBackPressureTest {

    private static final @NotNull String TAG = "setpoint";

    @Test
    void aCommandTravelsTheWholeLoop_andCommitsWhenTheDeviceAcknowledges() {
        final Rig rig = new Rig();
        rig.offer("1");

        assertThat(rig.fixture.writeState(TAG)).isEqualTo("WAITING_FOR_WRITE_RESULT");
        assertThat(rig.channel().committed()).isZero(); // nothing settles until the device answers

        rig.acknowledge(true, null);

        assertThat(rig.fixture.writeState(TAG)).isEqualTo("WAITING_FOR_WRITE_REQUEST");
        assertThat(rig.channel().committed()).isEqualTo(1);
        assertThat(rig.channel().windowViolations()).isZero();
    }

    @Test
    void aDeviceRejection_deadLetters_countsAFailure_andNeverFlapsTheTag() {
        final Rig rig = new Rig();
        rig.offer("1");

        rig.acknowledge(false, "device rejected the value");

        assertThat(rig.channel().deadLettered()).isEqualTo(1);
        assertThat(rig.channel().lastDeadLetterReason()).isEqualTo("device rejected the value");
        assertThat(rig.fixture.tag(TAG).failureCount()).isEqualTo(1);
        // One refused command is not a broken adapter.
        assertThat(rig.fixture.writeState(TAG)).isEqualTo("WAITING_FOR_WRITE_REQUEST");
    }

    @Test
    void aBurstIsPacedToTheWindow_soTheAdapterNeverSeesASecondInFlightWrite() {
        final Rig rig = new Rig();
        final int burst = 30;
        for (int i = 0; i < burst; i++) {
            rig.offer(Integer.toString(i));
        }

        // One write in flight, the rest waiting in the store — the adapter is never flooded.
        assertThat(rig.channel().deliveries()).isEqualTo(1);
        assertThat(rig.fixture.writeState(TAG)).isEqualTo("WAITING_FOR_WRITE_RESULT");

        for (int i = 0; i < burst; i++) {
            rig.acknowledge(true, null);
        }

        assertThat(rig.channel().committed()).isEqualTo(burst);
        assertThat(rig.channel().deliveries()).isEqualTo(burst);
        assertThat(rig.channel().windowViolations()).isZero();
        assertThat(rig.fixture.tag(TAG).failureCount()).isZero();
    }

    @Test
    void deactivationMidFlight_abortsTheWrite_keepsTheCommand_andIsNoWindowViolation() {
        final Rig rig = new Rig();
        rig.offer("1");
        assertThat(rig.fixture.writeState(TAG)).isEqualTo("WAITING_FOR_WRITE_RESULT");

        rig.fixture.deactivate(ProtocolAdapterDirection.SOUTHBOUND);
        rig.fixture.drain();

        assertThat(rig.fixture.writeState(TAG)).isEqualTo("DEACTIVATED");
        assertThat(rig.channel().keptForRedelivery()).isEqualTo(1);
        assertThat(rig.channel().committed()).isZero();
        assertThat(rig.channel().windowViolations()).isZero();
        assertThat(rig.channel().suspended()).isTrue();

        // Reactivating verifies again and redelivers the very same command.
        rig.fixture.activate(ProtocolAdapterDirection.SOUTHBOUND);
        rig.fixture.drain();
        assertThat(rig.channel().deliveries()).isEqualTo(2);
        rig.acknowledge(true, null);
        assertThat(rig.channel().committed()).isEqualTo(1);
    }

    @Test
    void aSecondWriteWhileOneIsInFlight_isRejectedBusy_andCountsTheMetric() {
        // The channel's pacing makes this impossible, so it is injected directly: the aspect must still refuse it
        // observably rather than queue it, and the first write must be untouched.
        final Rig rig = new Rig();
        rig.offer("1");

        rig.fixture.send(new ProtocolAdapterWrapperWriteRequest(rig.fixture.nodeFor(TAG), TAG, value("2")));

        assertThat(writesRejected(rig.fixture)).isEqualTo(1);
        assertThat(rig.fixture.writeState(TAG)).isEqualTo("WAITING_FOR_WRITE_RESULT");
        rig.acknowledge(true, null);
        assertThat(rig.channel().committed()).isEqualTo(1);
        // NOTE: this asserts the metric and the first write's survival only. That the rejection is also REPORTED
        // back to the delivering channel is pinned in TagAspectWriteCompletionOwnershipTest, where the settlement
        // itself is observable — the metric here increments before the report is emitted, so it cannot stand in
        // for one.
    }

    @Test
    void aWriteReachingADeactivatedAspect_isAborted_notAWindowViolation() {
        final Rig rig = new Rig();
        rig.fixture.deactivate(ProtocolAdapterDirection.SOUTHBOUND);
        rig.fixture.drain();

        rig.fixture.send(new ProtocolAdapterWrapperWriteRequest(rig.fixture.nodeFor(TAG), TAG, value("1")));

        assertThat(writesRejected(rig.fixture)).isZero();
        assertThat(rig.fixture.tag(TAG).failureCount()).isZero();
        // As above: that the write is REPORTED aborted rather than silently dropped — the difference between a
        // redelivered command and a permanently occupied delivery slot — is pinned on the settlement itself in
        // TagAspectWriteCompletionOwnershipTest.
    }

    @Test
    void overflowShedsAtTheStore_andTheAdapterNeverNotices() {
        final Rig rig = new Rig(4);
        for (int i = 0; i < 7; i++) {
            rig.offer(Integer.toString(i));
        }

        // The bound is the back-pressure limit and it applies at the store, never at the adapter.
        assertThat(rig.channel().deliveries()).isEqualTo(1);
        assertThat(rig.fixture.tag(TAG).failureCount()).isZero();
        assertThat(rig.channel().windowViolations()).isZero();
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────────────────

    /** A write-only adapter with a delivery plane bound, activated southbound and resting ready. */
    private static final class Rig {

        private final @NotNull WrapperTestFixture fixture;
        private final @NotNull SouthboundWritePlane plane;

        private Rig() {
            this(1_000);
        }

        private Rig(final int capacity) {
            fixture = WrapperTestFixture.builder()
                    .runningCoordinator()
                    .nodes(List.of(WrapperTestSupport.pair(TAG)))
                    .readUsed(Set.of())
                    .writeUsed(Set.of(TAG))
                    .build();
            plane = new SouthboundWritePlane(
                    fixture.adapterId, fixture.mailbox, capacity, fixture.nodes, Set.of(TAG), fixture.metrics);
            fixture.bindSouthboundPlane(plane);
            fixture.activate(ProtocolAdapterDirection.SOUTHBOUND);
            fixture.drain();
        }

        private @NotNull SouthboundWriteQueue channel() {
            final SouthboundWritePlane.TagChannel channel = plane.channel(TAG);
            assertThat(channel).isNotNull();
            return channel.queue();
        }

        /** Offer a command to the store and let the loop run to quiescence. */
        private void offer(final @NotNull String value) {
            plane.offer(TAG, value(value));
            fixture.drain();
        }

        /**
         * Answer the outstanding device write, then let the loop run to quiescence. The tick first: a write sits in
         * the batch collector until a tick hands it to the adapter, so a device cannot answer before one has run —
         * and the aspect rejects a result that arrives earlier, because it can only be a duplicate of an older one.
         */
        private void acknowledge(final boolean success, final @Nullable String reason) {
            fixture.advance(100);
            fixture.output.writeResult(fixture.nodeFor(TAG), WriteEntry.UNCORRELATED, success, reason);
            fixture.drain();
        }
    }

    private static @NotNull DataPoint value(final @NotNull String value) {
        return WrapperTestSupport.dataPoint(TAG, value);
    }

    private static long writesRejected(final @NotNull WrapperTestFixture fixture) {
        return fixture.metricRegistry
                .counter(ProtocolAdapterMetrics.ADAPTER_PREFIX + fixture.adapterId + ".tag." + TAG + ".writes.rejected")
                .getCount();
    }
}
