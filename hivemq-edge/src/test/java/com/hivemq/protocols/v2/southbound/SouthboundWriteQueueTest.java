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
package com.hivemq.protocols.v2.southbound;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.protocols.v2.tag.SouthboundWriteOutcome;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The {@link SouthboundWriteQueue} in front of an {@link InMemorySouthboundWriteBacklog}: it paces delivery to the
 * write aspect's advertised in-flight window of one, advances only when the delivered write settles, and deletes a
 * command from the backlog only on a terminal outcome — commit on success, dead-letter on device failure, kept at
 * the head (queue suspended) on abort. The adapter (here a capturing sender the test settles by hand) never sees a
 * second write while one is outstanding.
 */
class SouthboundWriteQueueTest {

    private static final @NotNull Node NODE = new TestNode("setpoint");

    @Test
    void keepsOneInFlight_committingEachDeliversTheNext_inFifoOrder() {
        final InMemorySouthboundWriteBacklog backlog = new InMemorySouthboundWriteBacklog(100);
        final CapturingSender sender = new CapturingSender();
        final SouthboundWriteQueue queue = new SouthboundWriteQueue(sender, NODE, backlog);
        queue.resume();

        backlog.offer(value(0));
        backlog.offer(value(1));
        backlog.offer(value(2));

        // Only the first write reached the adapter; all three are still in the backlog (none committed yet).
        assertThat(sender.requests).hasSize(1);
        assertThat(queue.inFlight()).isTrue();
        assertThat(backlog.pendingSize()).isEqualTo(3);

        sender.settleLast(SouthboundWriteOutcome.SUCCEEDED);
        assertThat(backlog.committed()).isEqualTo(1);
        assertThat(sender.requests).hasSize(2); // the next one delivered
        assertThat(backlog.pendingSize()).isEqualTo(2);

        sender.settleLast(SouthboundWriteOutcome.SUCCEEDED);
        sender.settleLast(SouthboundWriteOutcome.SUCCEEDED);

        assertThat(queue.inFlight()).isFalse();
        assertThat(queue.committed()).isEqualTo(3);
        assertThat(backlog.pendingSize()).isZero();
        assertThat(queue.windowViolations()).isZero();
        // Strict FIFO: the commands were committed in exactly the order they were offered.
        assertThat(backlog.committedCommands())
                .extracting(command -> command.value().getTagValue())
                .containsExactly(0, 1, 2);
    }

    @Test
    void deviceFailure_deadLettersTheCommand_andAdvances() {
        final InMemorySouthboundWriteBacklog backlog = new InMemorySouthboundWriteBacklog(100);
        final CapturingSender sender = new CapturingSender();
        final SouthboundWriteQueue queue = new SouthboundWriteQueue(sender, NODE, backlog);
        queue.resume();
        backlog.offer(value(0));
        backlog.offer(value(1));

        sender.settleLast(SouthboundWriteOutcome.FAILED);

        assertThat(queue.deadLettered()).isEqualTo(1);
        assertThat(backlog.deadLettered()).isEqualTo(1);
        assertThat(backlog.deadLetters()).hasSize(1);
        assertThat(sender.requests).hasSize(2); // advanced past the dead-lettered command
        assertThat(queue.inFlight()).isTrue();
    }

    @Test
    void abortedWrite_isKeptAtTheHead_queueSuspends_thenResumeRedeliversTheSameCommand() {
        final InMemorySouthboundWriteBacklog backlog = new InMemorySouthboundWriteBacklog(100);
        final CapturingSender sender = new CapturingSender();
        final SouthboundWriteQueue queue = new SouthboundWriteQueue(sender, NODE, backlog);
        queue.resume();
        backlog.offer(value(0));
        backlog.offer(value(1));
        final DataPoint firstValue = sender.requests.getFirst().value();

        // The adapter aborts the in-flight write (connection lost / deactivated).
        sender.settleLast(SouthboundWriteOutcome.ABORTED);

        assertThat(queue.keptForRedelivery()).isEqualTo(1);
        assertThat(backlog.committed()).isZero(); // nothing was delivered — and nothing was removed
        assertThat(backlog.pendingSize()).isEqualTo(2);
        assertThat(queue.suspended()).isTrue();
        assertThat(queue.inFlight()).isFalse();
        assertThat(sender.requests).hasSize(1); // suspended: not redelivered automatically

        // Resuming (adapter ready again) redelivers the very same command — durability, not loss.
        queue.resume();
        assertThat(queue.suspended()).isFalse();
        assertThat(sender.requests).hasSize(2);
        assertThat(sender.requests.get(1).value()).isEqualTo(firstValue);
    }

    @Test
    void rejectedBusy_isCountedAsAWindowViolation_andSuspends() {
        final InMemorySouthboundWriteBacklog backlog = new InMemorySouthboundWriteBacklog(100);
        final CapturingSender sender = new CapturingSender();
        final SouthboundWriteQueue queue = new SouthboundWriteQueue(sender, NODE, backlog);
        queue.resume();
        backlog.offer(value(0));

        sender.settleLast(SouthboundWriteOutcome.REJECTED_BUSY);

        assertThat(queue.windowViolations()).isEqualTo(1);
        assertThat(backlog.pendingSize()).isEqualTo(1); // kept — never removed
        assertThat(queue.suspended()).isTrue();
    }

    @Test
    void suspendClosesTheWindow_commandsAccumulate_untilResumeDelivers() {
        final InMemorySouthboundWriteBacklog backlog = new InMemorySouthboundWriteBacklog(100);
        final CapturingSender sender = new CapturingSender();
        final SouthboundWriteQueue queue = new SouthboundWriteQueue(sender, NODE, backlog);

        // Close the window before anything arrives (the adapter is known not ready).
        queue.suspend();
        backlog.offer(value(0));
        backlog.offer(value(1));

        // Nothing is delivered; the backlog absorbs the burst.
        assertThat(sender.requests).isEmpty();
        assertThat(queue.inFlight()).isFalse();
        assertThat(backlog.pendingSize()).isEqualTo(2);

        // Reopening the window delivers the head.
        queue.resume();
        assertThat(sender.requests).hasSize(1);
        assertThat(queue.inFlight()).isTrue();
    }

    @Test
    void suspendLeavesTheInFlightWriteUntouched_itsOutcomeStillDisposes() {
        final InMemorySouthboundWriteBacklog backlog = new InMemorySouthboundWriteBacklog(100);
        final CapturingSender sender = new CapturingSender();
        final SouthboundWriteQueue queue = new SouthboundWriteQueue(sender, NODE, backlog);
        queue.resume();
        backlog.offer(value(0));
        backlog.offer(value(1));
        assertThat(queue.inFlight()).isTrue();

        // The window closes while a write is outstanding: the write settles normally and is committed, but the
        // next command is not delivered until the window reopens.
        queue.suspend();
        sender.settleLast(SouthboundWriteOutcome.SUCCEEDED);

        assertThat(queue.committed()).isEqualTo(1);
        assertThat(queue.inFlight()).isFalse();
        assertThat(sender.requests).hasSize(1);

        queue.resume();
        assertThat(sender.requests).hasSize(2);
    }

    @Test
    void crashReplay_aFreshQueueOverTheSameBacklog_redeliversTheUncommittedHead() {
        final InMemorySouthboundWriteBacklog backlog = new InMemorySouthboundWriteBacklog(100);
        final CapturingSender sender = new CapturingSender();
        new SouthboundWriteQueue(sender, NODE, backlog).resume();
        backlog.offer(value(0));
        backlog.offer(value(1));

        // The first command is in flight but never settles — the process "crashes". Because delivery never
        // removed it, the backlog still holds both commands.
        assertThat(sender.requests).hasSize(1);
        assertThat(backlog.pendingSize()).isEqualTo(2);

        // "Restart": a fresh queue over the same (durable) backlog redelivers the very same head command.
        final CapturingSender senderAfterRestart = new CapturingSender();
        final SouthboundWriteQueue queueAfterRestart = new SouthboundWriteQueue(senderAfterRestart, NODE, backlog);
        queueAfterRestart.resume();

        assertThat(senderAfterRestart.requests).hasSize(1);
        assertThat(senderAfterRestart.requests.getFirst().value())
                .isEqualTo(sender.requests.getFirst().value());

        // Draining after the restart delivers every command exactly once — at-least-once, nothing lost.
        senderAfterRestart.settleLast(SouthboundWriteOutcome.SUCCEEDED);
        senderAfterRestart.settleLast(SouthboundWriteOutcome.SUCCEEDED);
        assertThat(backlog.pendingSize()).isZero();
        assertThat(backlog.committedCommands())
                .extracting(command -> command.value().getTagValue())
                .containsExactly(0, 1);
    }

    private static @NotNull DataPoint value(final int i) {
        return new TestDataPoint("setpoint", i);
    }
}
