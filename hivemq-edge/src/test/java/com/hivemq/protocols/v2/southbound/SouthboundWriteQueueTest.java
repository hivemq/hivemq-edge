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
import static org.assertj.core.api.Assertions.assertThatCode;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.protocols.v2.tag.SouthboundWriteOutcome;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    @Test
    void aLateSettleOfAnAbandonedAttempt_neverDisposesItsRedelivery() {
        // QA finding N4: without a per-delivery token, a late duplicate settle of an abandoned attempt carried
        // the same command as its live redelivery and would commit or dead-letter it mid-flight.
        final InMemorySouthboundWriteBacklog backlog = new InMemorySouthboundWriteBacklog(10);
        final CapturingSender sender = new CapturingSender();
        final SouthboundWriteQueue queue = new SouthboundWriteQueue(sender, NODE, backlog);
        backlog.offer(value(0));
        queue.resume();
        assertThat(sender.requests).hasSize(1);

        // The adapter goes away mid-write: the attempt is abandoned, the command kept for redelivery.
        sender.requests.get(0).completion().settle(SouthboundWriteOutcome.ABORTED, "connection lost");
        assertThat(queue.suspended()).isTrue();
        queue.resume(); // reconnect — the SAME command is redelivered as a new attempt
        assertThat(sender.requests).hasSize(2);

        // The abandoned attempt's result arrives late, through the OLD completion: it must be ignored.
        sender.requests.get(0).completion().settle(SouthboundWriteOutcome.SUCCEEDED, null);
        assertThat(queue.committed()).isZero();
        assertThat(queue.inFlight()).isTrue();
        assertThat(backlog.pendingSize()).isEqualTo(1);

        // Only the live attempt's settle disposes the command.
        sender.requests.get(1).completion().settle(SouthboundWriteOutcome.SUCCEEDED, null);
        assertThat(queue.committed()).isEqualTo(1);
        assertThat(backlog.pendingSize()).isZero();
    }

    @Test
    void aDuplicateSettleOfTheSameDelivery_isIgnored_notADoubleDisposal() {
        // A settle is consumed exactly once; a second call through the same completion must neither throw into
        // the settling thread nor dispose twice.
        final InMemorySouthboundWriteBacklog backlog = new InMemorySouthboundWriteBacklog(10);
        final CapturingSender sender = new CapturingSender();
        final SouthboundWriteQueue queue = new SouthboundWriteQueue(sender, NODE, backlog);
        backlog.offer(value(0));
        queue.resume();

        sender.requests.get(0).completion().settle(SouthboundWriteOutcome.SUCCEEDED, null);
        assertThat(queue.committed()).isEqualTo(1);

        assertThatCode(() -> sender.requests.get(0).completion().settle(SouthboundWriteOutcome.SUCCEEDED, null))
                .doesNotThrowAnyException();
        assertThat(queue.committed()).isEqualTo(1);
        assertThat(queue.windowViolations()).isZero();

        // The queue is unharmed: the next command flows normally.
        backlog.offer(value(1));
        assertThat(sender.requests).hasSize(2);
        sender.settleLast(SouthboundWriteOutcome.SUCCEEDED);
        assertThat(queue.committed()).isEqualTo(2);
    }

    @Test
    void terminalDisposal_runsOutsideTheQueueMonitor() {
        // QA finding N3: disposal reaches broker persistence, which in in-memory persistence mode can execute
        // work caller-side — it must not run under the queue monitor, or suspend()/resume()/readiness block for
        // the duration.
        final AtomicReference<SouthboundWriteQueue> queueRef = new AtomicReference<>();
        final AtomicBoolean monitorHeldDuringDisposal = new AtomicBoolean();
        final InMemorySouthboundWriteBacklog delegate = new InMemorySouthboundWriteBacklog(10);
        final SouthboundWriteBacklog probing = new SouthboundWriteBacklog() {
            @Override
            public @Nullable SouthboundCommand head() {
                return delegate.head();
            }

            @Override
            public void removeHead(final @NotNull String id) {
                recordMonitor();
                delegate.removeHead(id);
            }

            @Override
            public void deadLetterHead(final @NotNull String id, final @NotNull String reason) {
                recordMonitor();
                delegate.deadLetterHead(id, reason);
            }

            @Override
            public void onAvailable(final @NotNull Runnable wakeup) {
                delegate.onAvailable(wakeup);
            }

            @Override
            public void close() {
                delegate.close();
            }

            private void recordMonitor() {
                final SouthboundWriteQueue queue = queueRef.get();
                if (queue != null && Thread.holdsLock(queue)) {
                    monitorHeldDuringDisposal.set(true);
                }
            }
        };
        final CapturingSender sender = new CapturingSender();
        final SouthboundWriteQueue queue = new SouthboundWriteQueue(sender, NODE, probing);
        queueRef.set(queue);
        delegate.offer(value(0));
        delegate.offer(value(1));
        queue.resume();

        sender.settleLast(SouthboundWriteOutcome.SUCCEEDED); // the commit path
        sender.settleLast(SouthboundWriteOutcome.FAILED); // the dead-letter path

        assertThat(queue.committed()).isEqualTo(1);
        assertThat(queue.deadLettered()).isEqualTo(1);
        assertThat(monitorHeldDuringDisposal).isFalse();
    }

    @Test
    void aBacklogThatThrowsFromDisposal_doesNotWedgeTheTag_itReleasesTheSlotAndAdvances() {
        // QA round-2 finding: terminal disposal runs off the queue monitor with inFlightId still set; a
        // synchronous throw from the backlog (contract-forbidden, but previously undefended) skipped the
        // inFlightId clear, wedging the tag forever — every future deliverNext no-ops on inFlightId != null. The
        // queue must release the slot and advance even when disposal throws; at-least-once still holds.
        final AtomicBoolean firstRemoveThrew = new AtomicBoolean();
        final InMemorySouthboundWriteBacklog delegate = new InMemorySouthboundWriteBacklog(10);
        final SouthboundWriteBacklog throwing = new SouthboundWriteBacklog() {
            @Override
            public @Nullable SouthboundCommand head() {
                return delegate.head();
            }

            @Override
            public void removeHead(final @NotNull String id) {
                if (firstRemoveThrew.compareAndSet(false, true)) {
                    throw new RuntimeException("scripted disposal failure");
                }
                delegate.removeHead(id);
            }

            @Override
            public void deadLetterHead(final @NotNull String id, final @NotNull String reason) {
                delegate.deadLetterHead(id, reason);
            }

            @Override
            public void onAvailable(final @NotNull Runnable wakeup) {
                delegate.onAvailable(wakeup);
            }

            @Override
            public void close() {
                delegate.close();
            }
        };
        final CapturingSender sender = new CapturingSender();
        final SouthboundWriteQueue queue = new SouthboundWriteQueue(sender, NODE, throwing);
        delegate.offer(value(0));
        queue.resume(); // delivers the command
        assertThat(sender.requests).hasSize(1);

        // The commit disposal throws — the settle must not propagate it, and the slot must be released so the tag
        // keeps flowing. The command was never removed, so it is redelivered (at-least-once).
        assertThatCode(() -> sender.settleLast(SouthboundWriteOutcome.SUCCEEDED))
                .doesNotThrowAnyException();
        assertThat(queue.inFlight()).isTrue();
        assertThat(sender.requests).hasSize(2);

        // The redelivery's disposal succeeds; the tag drains cleanly.
        sender.settleLast(SouthboundWriteOutcome.SUCCEEDED);
        assertThat(delegate.committed()).isEqualTo(1);
        assertThat(queue.inFlight()).isFalse();
    }

    private static @NotNull DataPoint value(final int i) {
        return new TestDataPoint("setpoint", i);
    }
}
