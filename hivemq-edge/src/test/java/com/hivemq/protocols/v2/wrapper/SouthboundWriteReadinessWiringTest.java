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
import com.hivemq.protocols.v2.southbound.SouthboundWritePlane;
import com.hivemq.protocols.v2.tag.TagWriteReadinessListener;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The suspend/resume wiring end to end at the wrapper boundary: the write aspects notify their writability
 * boundary ({@link TagWriteReadinessListener}), the {@link SouthboundWritePlane} turns the notifications into
 * delivery-window calls, and <b>no test code ever calls {@code resume()} by hand</b> — the tag's own readiness
 * drives redelivery. Driven on {@code FakeClock} + {@code ManualDispatcher} through the real wrapper and real
 * aspect machines.
 */
class SouthboundWriteReadinessWiringTest {

    private static final @NotNull String TAG = "setpoint";

    @Test
    void commandsOfferedBeforeActivation_waitSuspended_andFlowWhenTheTagVerifies() {
        final Rig rig = new Rig();

        // Southbound is not activated yet: commands accumulate in the backlog, nothing reaches the adapter.
        rig.plane.offer(TAG, dataPoint(1));
        rig.plane.offer(TAG, dataPoint(2));
        rig.fixture.drain();
        assertThat(rig.fixture.writeState(TAG)).isEqualTo("DEACTIVATED");
        assertThat(rig.pending()).isEqualTo(2);
        assertThat(rig.channel().queue().deliveries()).isZero();

        // Activation connects and verifies the tag; its tagWritable opens the window and the head flows — no
        // manual resume anywhere.
        rig.fixture.activate(ProtocolAdapterDirection.SOUTHBOUND);
        assertThat(rig.fixture.writeState(TAG)).isEqualTo("WAITING_FOR_WRITE_RESULT");
        assertThat(rig.channel().queue().deliveries()).isEqualTo(1);

        // Acknowledge both; each settle commits and delivers the next.
        rig.ackInFlight();
        assertThat(rig.fixture.writeState(TAG)).isEqualTo("WAITING_FOR_WRITE_RESULT"); // the second one
        rig.ackInFlight();
        assertThat(rig.fixture.writeState(TAG)).isEqualTo("WAITING_FOR_WRITE_REQUEST");
        assertThat(rig.channel().queue().committed()).isEqualTo(2);
        assertThat(rig.pending()).isZero();
        assertThat(rig.channel().queue().windowViolations()).isZero();
    }

    @Test
    void disconnectMidFlight_reconnectRedeliversTheSameCommand_withoutManualResume() {
        final Rig rig = new Rig();
        rig.fixture.activate(ProtocolAdapterDirection.SOUTHBOUND);
        rig.plane.offer(TAG, dataPoint(1));
        rig.plane.offer(TAG, dataPoint(2));
        rig.fixture.drain();
        assertThat(rig.fixture.writeState(TAG)).isEqualTo("WAITING_FOR_WRITE_RESULT");
        assertThat(rig.channel().queue().deliveries()).isEqualTo(1);

        // The connection drops with the first command in flight: the aspect aborts it, the queue keeps it at the
        // head and closes its window.
        rig.fixture.send(new ProtocolAdapterWrapperEvent.Disconnected());
        assertThat(rig.channel().queue().suspended()).isTrue();
        assertThat(rig.channel().queue().keptForRedelivery()).isEqualTo(1);
        assertThat(rig.pending()).isEqualTo(2); // nothing lost, nothing committed

        // The retry backoff fires, the adapter reconnects and the tag re-verifies; its tagWritable redelivers the
        // SAME command — nobody called resume().
        rig.fixture.advance(1000);
        assertThat(rig.fixture.state()).isEqualTo(ProtocolAdapterWrapperState.CONNECTED);
        assertThat(rig.fixture.writeState(TAG)).isEqualTo("WAITING_FOR_WRITE_RESULT");
        assertThat(rig.channel().queue().deliveries()).isEqualTo(2); // the redelivery

        rig.ackInFlight();
        rig.ackInFlight();
        assertThat(rig.channel().queue().committed()).isEqualTo(2);
        assertThat(rig.pending()).isZero();
        assertThat(rig.channel().queue().windowViolations()).isZero();
        assertThat(rig.fixture
                        .metricRegistry
                        .counter(ProtocolAdapterMetrics.ADAPTER_PREFIX + rig.fixture.adapterId + ".tag." + TAG
                                + ".writes.rejected")
                        .getCount())
                .isZero();
    }

    @Test
    void deactivation_closesTheWindow_reactivationReopensIt() {
        final Rig rig = new Rig();
        rig.fixture.activate(ProtocolAdapterDirection.SOUTHBOUND);
        rig.fixture.drain();
        assertThat(rig.fixture.writeState(TAG)).isEqualTo("WAITING_FOR_WRITE_REQUEST");

        // Southbound goes off: the window closes. A command arriving now waits in the backlog — it is never even
        // delivered to an aspect that could only abort it.
        rig.fixture.deactivate(ProtocolAdapterDirection.SOUTHBOUND);
        assertThat(rig.channel().queue().suspended()).isTrue();
        rig.plane.offer(TAG, dataPoint(1));
        rig.fixture.drain();
        assertThat(rig.channel().queue().deliveries()).isZero();
        assertThat(rig.pending()).isEqualTo(1);

        // Southbound comes back: re-verification completes, tagWritable reopens the window, the command flows.
        rig.fixture.activate(ProtocolAdapterDirection.SOUTHBOUND);
        assertThat(rig.fixture.writeState(TAG)).isEqualTo("WAITING_FOR_WRITE_RESULT");
        rig.ackInFlight();
        assertThat(rig.channel().queue().committed()).isEqualTo(1);
        assertThat(rig.pending()).isZero();
    }

    // ── the rig: a write-only fixture with a real plane attached as the readiness listener ────────────────────────

    /**
     * The fixture and its plane. The plane needs the fixture's mailbox and the fixture's coordinator needs the
     * listener, so the listener is a forwarding shim whose delegate is set to the plane once both exist.
     */
    private static final class Rig {

        private final @NotNull WrapperTestFixture fixture;
        private final @NotNull SouthboundWritePlane plane;

        private Rig() {
            final AtomicReference<TagWriteReadinessListener> delegate =
                    new AtomicReference<>(TagWriteReadinessListener.NONE);
            this.fixture = WrapperTestFixture.builder()
                    .runningCoordinator()
                    .nodes(List.of(WrapperTestSupport.pair(TAG)))
                    .readUsed(Set.of())
                    .writeUsed(Set.of(TAG))
                    .writeReadinessListener(new TagWriteReadinessListener() {
                        @Override
                        public void tagWritable(final @NotNull String tagName) {
                            delegate.get().tagWritable(tagName);
                        }

                        @Override
                        public void tagUnwritable(final @NotNull String tagName) {
                            delegate.get().tagUnwritable(tagName);
                        }
                    })
                    .build();
            this.plane = new SouthboundWritePlane(fixture.adapterId, fixture.mailbox, 100, fixture.nodes, Set.of(TAG));
            delegate.set(plane);
        }

        private @NotNull SouthboundWritePlane.TagChannel channel() {
            final SouthboundWritePlane.TagChannel channel = plane.channel(TAG);
            assertThat(channel).isNotNull();
            return channel;
        }

        private int pending() {
            return ((InMemorySouthboundWriteBacklog) channel().backlog()).pendingSize();
        }

        /** Acknowledge the in-flight write with success and drain the cascade (settle → commit → deliver next). */
        private void ackInFlight() {
            final Node node = fixture.nodeFor(TAG);
            fixture.output.writeResult(node, true, null);
            fixture.drain();
        }
    }

    private static @NotNull DataPoint dataPoint(final int value) {
        return WrapperTestSupport.dataPoint(TAG, value);
    }
}
