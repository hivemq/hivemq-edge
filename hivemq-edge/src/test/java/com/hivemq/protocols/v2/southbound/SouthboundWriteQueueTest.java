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

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.protocols.v2.runtime.ProtocolAdapterMetrics;
import com.hivemq.protocols.v2.tag.SouthboundWriteOutcome;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperSouthboundMessage.SouthboundRead;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperSouthboundMessage.SouthboundSize;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * The {@link SouthboundWriteQueue} — one tag's delivery channel — driven exactly as the wrapper drives it: every
 * store answer and every settlement arrives as a message pumped through {@link CapturingSender}, never as a direct
 * call, so what is under test is the shape the product actually has.
 * <p>
 * The channel paces the store to the write aspect's window of one, advances only when the delivered write settles,
 * and deletes a command only on a terminal outcome — commit on success, dead-letter on device failure, kept at the
 * head (window closed) on abort.
 */
class SouthboundWriteQueueTest {

    private static final @NotNull String ADAPTER = "a1";
    private static final @NotNull String TAG = "setpoint";
    private static final @NotNull Node NODE = new TestNode(TAG);

    /**
     * A token source for a channel standing on its own. In production the source is the plane's, shared by every
     * channel, so that a rebuilt channel can never mint a token one of its predecessors is still waiting on.
     */
    private static @NotNull LongSupplier tokens() {
        final AtomicLong counter = new AtomicLong();
        return counter::incrementAndGet;
    }

    /** A metrics sink for a plane standing on its own; the registry is discarded with the test. */
    private static @NotNull ProtocolAdapterMetrics metrics() {
        return new ProtocolAdapterMetrics(new MetricRegistry(), "a1", () -> 0);
    }

    @Test
    void windowsAreBornClosed_soNothingIsDeliveredBeforeTheTagIsWritable() {
        final Fixture fixture = new Fixture();
        fixture.backlog.offer(value(0));
        fixture.pump();

        assertThat(fixture.queue.suspended()).isTrue();
        assertThat(fixture.sender.requests).isEmpty();
        assertThat(fixture.backlog.pendingSize()).isEqualTo(1);
    }

    @Test
    void keepsOneInFlight_committingEachDeliversTheNext_inFifoOrder() {
        final Fixture fixture = new Fixture();
        fixture.queue.openWindow();
        fixture.backlog.offer(value(0));
        fixture.backlog.offer(value(1));
        fixture.backlog.offer(value(2));
        fixture.pump();

        // Only the first write reached the adapter; all three are still in the store (none committed yet).
        assertThat(fixture.sender.requests).hasSize(1);
        assertThat(fixture.queue.inFlight()).isTrue();
        assertThat(fixture.backlog.pendingSize()).isEqualTo(3);

        fixture.settleAndPump(SouthboundWriteOutcome.SUCCEEDED);
        assertThat(fixture.sender.requests).hasSize(2);
        assertThat(fixture.backlog.pendingSize()).isEqualTo(2);

        fixture.settleAndPump(SouthboundWriteOutcome.SUCCEEDED);
        fixture.settleAndPump(SouthboundWriteOutcome.SUCCEEDED);

        assertThat(fixture.queue.inFlight()).isFalse();
        assertThat(fixture.queue.committed()).isEqualTo(3);
        assertThat(fixture.backlog.pendingSize()).isZero();
        assertThat(fixture.queue.windowViolations()).isZero();
        // Strict FIFO: the commands were deleted in exactly the order they were offered.
        assertThat(fixture.backlog.deletedCommands())
                .extracting(command -> command.value().getTagValue())
                .containsExactly(0, 1, 2);
    }

    @Test
    void deviceFailure_deadLettersTheCommand_carriesItsReason_andAdvances() {
        final Fixture fixture = new Fixture();
        fixture.queue.openWindow();
        fixture.backlog.offer(value(0));
        fixture.backlog.offer(value(1));
        fixture.pump();

        fixture.settleAndPump(SouthboundWriteOutcome.FAILED, "protected register");

        assertThat(fixture.queue.deadLettered()).isEqualTo(1);
        // The dead-letter record is the operator's only account of a refused command, so the device's own words
        // must survive rather than being replaced by the generic fallback.
        assertThat(fixture.queue.lastDeadLetterReason()).isEqualTo("protected register");
        assertThat(fixture.sender.requests).hasSize(2); // advanced past the dead-lettered command
        assertThat(fixture.queue.inFlight()).isTrue();
    }

    @Test
    void deviceFailureWithNoReason_fallsBackToAGenericDeadLetterReason() {
        final Fixture fixture = new Fixture();
        fixture.queue.openWindow();
        fixture.backlog.offer(value(0));
        fixture.pump();

        fixture.settleAndPump(SouthboundWriteOutcome.FAILED, null);

        assertThat(fixture.queue.lastDeadLetterReason()).isEqualTo("device rejected the write");
    }

    @Test
    void abortedWrite_isKeptAtTheHead_windowCloses_thenReopeningRedeliversTheSameCommand() {
        final Fixture fixture = new Fixture();
        fixture.queue.openWindow();
        fixture.backlog.offer(value(0));
        fixture.backlog.offer(value(1));
        fixture.pump();
        final DataPoint firstValue = fixture.sender.requests.getFirst().value();

        // The adapter abandons the in-flight write (connection lost / deactivated).
        fixture.settleAndPump(SouthboundWriteOutcome.ABORTED);

        assertThat(fixture.queue.keptForRedelivery()).isEqualTo(1);
        assertThat(fixture.queue.committed()).isZero();
        assertThat(fixture.backlog.pendingSize()).isEqualTo(2); // nothing was deleted
        assertThat(fixture.queue.suspended()).isTrue();
        assertThat(fixture.queue.inFlight()).isFalse();
        assertThat(fixture.sender.requests).hasSize(1); // window closed: not redelivered

        // Writable again: the very same command is delivered again — durability, not loss.
        fixture.queue.openWindow();
        fixture.pump();
        assertThat(fixture.sender.requests).hasSize(2);
        assertThat(fixture.sender.requests.get(1).value()).isEqualTo(firstValue);
    }

    @Test
    void rejectedBusy_keepsTheCommand_leavesTheWindowOpen_andRetriesOnTheNextPoll() {
        // A busy aspect never crosses its writability boundary, so no TagWritability(true) would ever arrive to
        // reopen a window closed here — closing it would stop the tag delivering for good. The command is kept and
        // the backstop poll retries it, which is already rate-bounded to one attempt per POLL_TICKS.
        final Fixture fixture = new Fixture();
        fixture.queue.openWindow();
        fixture.backlog.offer(value(0));
        fixture.pump();
        assertThat(fixture.sender.requests).hasSize(1);

        fixture.settleAndPump(SouthboundWriteOutcome.REJECTED_BUSY);

        assertThat(fixture.queue.windowViolations()).isEqualTo(1);
        assertThat(fixture.backlog.pendingSize()).isEqualTo(1); // kept — never deleted
        assertThat(fixture.queue.suspended()).isFalse();
        assertThat(fixture.sender.requests).hasSize(1); // not redelivered immediately — no tight loop

        for (int tick = 0; tick < SouthboundWriteQueue.POLL_TICKS; tick++) {
            fixture.queue.onTick();
        }
        fixture.pump();

        assertThat(fixture.sender.requests).hasSize(2); // the poll retried it
    }

    @Test
    void aDeadLetter_isCountedOnTheAdapterMetrics_notOnlyInTheLog() {
        // Dead-lettering is the only southbound outcome that destroys a command. Everything else keeps it, so this
        // is the one an operator has to be able to alert on rather than grep for.
        final MetricRegistry registry = new MetricRegistry();
        final CapturingSender sender = new CapturingSender();
        final InMemorySouthboundWriteBacklog backlog = new InMemorySouthboundWriteBacklog(100, TAG, sender);
        final SouthboundWriteQueue queue = new SouthboundWriteQueue(
                ADAPTER, TAG, NODE, backlog, sender, tokens(), new ProtocolAdapterMetrics(registry, ADAPTER, () -> 0));
        queue.openWindow();
        backlog.offer(value(0));
        sender.pump(queue);

        sender.settleLast(SouthboundWriteOutcome.FAILED, "protected register");
        sender.pump(queue);

        assertThat(registry.counter(ProtocolAdapterMetrics.ADAPTER_PREFIX + ADAPTER + ".tag." + TAG
                                + ".writes.dead-lettered")
                        .getCount())
                .isEqualTo(1);
    }

    @Test
    void aStaleSettle_fromASupersededDelivery_cannotDisposeItsRedelivery() {
        final Fixture fixture = new Fixture();
        fixture.queue.openWindow();
        fixture.backlog.offer(value(0));
        fixture.pump();
        final long abandonedToken = fixture.sender.requests.getFirst().deliveryToken();

        // The write is abandoned and the very same command is redelivered under a new token.
        fixture.settleAndPump(SouthboundWriteOutcome.ABORTED);
        fixture.queue.openWindow();
        fixture.pump();
        assertThat(fixture.sender.requests).hasSize(2);

        // The abandoned attempt's acknowledgment finally lands. Acting on it would commit a command whose live
        // delivery is still outstanding.
        fixture.queue.onSettled(abandonedToken, SouthboundWriteOutcome.SUCCEEDED, null);

        assertThat(fixture.queue.committed()).isZero();
        assertThat(fixture.backlog.pendingSize()).isEqualTo(1);
        assertThat(fixture.queue.inFlight()).isTrue(); // the live delivery is untouched
    }

    @Test
    void theBackstopPollFindsACommand_evenWhenNoArrivalHintEverFires() {
        // The broker's publish-available callback is edge-triggered and can be missed; correctness rests on the
        // poll, not on the hint. This store never hints at all.
        final CapturingSender sender = new CapturingSender();
        final SilentBacklog backlog = new SilentBacklog(sender);
        final SouthboundWriteQueue queue =
                new SouthboundWriteQueue(ADAPTER, TAG, NODE, backlog, sender, tokens(), metrics());
        queue.openWindow(); // opening reads once and finds nothing
        sender.pump(queue);
        assertThat(sender.requests).isEmpty();

        backlog.queued = new SouthboundCommand("1", value(7));
        for (int tick = 0; tick < SouthboundWriteQueue.POLL_TICKS; tick++) {
            queue.onTick();
        }
        sender.pump(queue);

        assertThat(sender.requests).hasSize(1);
        assertThat(sender.requests.getFirst().value().getTagValue()).isEqualTo(7);
    }

    @Test
    void repeatedEmptyReadsAgainstANonEmptyStore_sweepTheStrandedMarkers() {
        // The hidden-head case: a head behind an ownerless in-flight marker makes every read come back empty while
        // the store still holds it. The depth cross-check is what catches the lie.
        final CapturingSender sender = new CapturingSender();
        final SilentBacklog backlog = new SilentBacklog(sender);
        backlog.reportedSize = 1; // the store holds a command that reads will never surface
        final SouthboundWriteQueue queue =
                new SouthboundWriteQueue(ADAPTER, TAG, NODE, backlog, sender, tokens(), metrics());
        queue.openWindow();

        for (int round = 0; round < SouthboundWriteQueue.EMPTY_READS_BEFORE_SIZE_CHECK; round++) {
            sender.pump(queue);
            for (int tick = 0; tick < SouthboundWriteQueue.POLL_TICKS; tick++) {
                queue.onTick();
            }
        }
        sender.pump(queue);

        assertThat(backlog.markerReleases).isPositive();
    }

    @Test
    void anUndeliverableCommand_isDeadLetteredHere_andTheChannelAdvances() {
        // "Executed at least once, OR removed with a logged reason" — this is the second half. The store reports a
        // publish it could not decode rather than disposing of it; deciding its fate belongs on this side, beside
        // every other disposition. Leaving it undeleted would wedge the tag behind a command nobody can deliver.
        final CapturingSender sender = new CapturingSender();
        final SilentBacklog backlog = new SilentBacklog(sender);
        final SouthboundWriteQueue queue =
                new SouthboundWriteQueue(ADAPTER, TAG, NODE, backlog, sender, tokens(), metrics());
        backlog.undeliverableId = "bad-1";
        backlog.queued = new SouthboundCommand("2", value(9)); // the one behind it, which must still get through

        queue.openWindow();
        sender.pump(queue);

        assertThat(queue.deadLettered()).isEqualTo(1);
        assertThat(queue.lastDeadLetterReason()).isNotNull();
        assertThat(backlog.deleted).containsExactly("bad-1");
        assertThat(sender.requests).hasSize(1); // the channel read on and delivered the next command
        assertThat(sender.requests.getFirst().value().getTagValue()).isEqualTo(9);
    }

    @Test
    void aDepthCheckThatLandsAfterTheChannelLeasedAHead_doesNotSweep() {
        // The sweep releases EVERY in-flight marker on this tag's queue — including the one covering the command
        // this channel is delivering right now. It is sound only while the channel holds nothing, so a depth answer
        // that lost the race to a read must be ignored rather than acted on. The depth answer is held back here
        // because the store normally answers it inline, which closes the race and hides the bug.
        final CapturingSender sender = new CapturingSender();
        final SilentBacklog backlog = new SilentBacklog(sender);
        final SouthboundWriteQueue queue =
                new SouthboundWriteQueue(ADAPTER, TAG, NODE, backlog, sender, tokens(), metrics());
        backlog.reportedSize = 1; // the store insists it holds a command every read comes back empty on
        backlog.deferSizeAnswer = true;

        queue.openWindow();
        for (int round = 0; round < SouthboundWriteQueue.EMPTY_READS_BEFORE_SIZE_CHECK; round++) {
            sender.pump(queue);
            for (int tick = 0; tick < SouthboundWriteQueue.POLL_TICKS; tick++) {
                queue.onTick();
            }
        }
        sender.pump(queue);
        assertThat(backlog.deferredSizeToken).isNotNull(); // the ladder reached the depth check

        // The command surfaces and is leased and delivered before the depth answer lands.
        backlog.queued = new SouthboundCommand("1", value(4));
        for (int tick = 0; tick < SouthboundWriteQueue.POLL_TICKS; tick++) {
            queue.onTick();
        }
        sender.pump(queue);
        assertThat(queue.head()).isNotNull();

        backlog.releaseSizeAnswer();
        sender.pump(queue);

        assertThat(backlog.markerReleases).isZero();
        assertThat(queue.head()).isNotNull(); // still leased, still deliverable
    }

    @Test
    void aFailedRead_isNotFatal_theNextPollRecovers() {
        final CapturingSender sender = new CapturingSender();
        final SilentBacklog backlog = new SilentBacklog(sender);
        backlog.failNextRead = true;
        final SouthboundWriteQueue queue =
                new SouthboundWriteQueue(ADAPTER, TAG, NODE, backlog, sender, tokens(), metrics());
        queue.openWindow();
        sender.pump(queue);
        assertThat(sender.requests).isEmpty();

        backlog.queued = new SouthboundCommand("1", value(3));
        for (int tick = 0; tick < SouthboundWriteQueue.POLL_TICKS; tick++) {
            queue.onTick();
        }
        sender.pump(queue);

        assertThat(sender.requests).hasSize(1);
    }

    @Test
    void aCommandWhoseDeleteNeverTakes_isNeverExecutedTwice() {
        // The store accepts the delete and keeps the entry anyway — what a client-queue persistence that has run out
        // of delete attempts leaves behind. The entry then reappears on every marker sweep. Executing it again would
        // write a stale command to a physical device every few seconds, indefinitely; at-least-once permits a
        // duplicate, not a runaway.
        final CapturingSender sender = new CapturingSender();
        final SilentBacklog backlog = new SilentBacklog(sender);
        backlog.refuseDeletes = true;
        backlog.queued = new SouthboundCommand("stuck-1", value(5));
        final SouthboundWriteQueue queue =
                new SouthboundWriteQueue(ADAPTER, TAG, NODE, backlog, sender, tokens(), metrics());
        queue.openWindow();
        sender.pump(queue);

        assertThat(sender.requests).hasSize(1); // delivered once, as it should be

        // Committing deletes it, the store keeps it, and the read the commit issues finds it straight back.
        sender.settleLast(SouthboundWriteOutcome.SUCCEEDED, null);
        sender.pump(queue);
        assertThat(queue.committed()).isEqualTo(1);
        assertThat(queue.redeliveriesRefused()).isEqualTo(1);

        // Every later poll finds the very same entry still there, and refuses it again.
        for (int poll = 0; poll < 3; poll++) {
            pollOnce(queue);
            sender.pump(queue);
        }

        assertThat(sender.requests).hasSize(1); // still one: no second execution, ever
        assertThat(queue.redeliveriesRefused()).isEqualTo(4);
        assertThat(queue.head()).isNull();
        // One delete for the commit, then one retry per sighting — so the channel heals itself if the store does.
        assertThat(backlog.deleted).containsExactly("stuck-1", "stuck-1", "stuck-1", "stuck-1", "stuck-1");
    }

    @Test
    void refusingADisposedCommandNeverSpins_evenIfTheStoreKeepsHandingItBack() {
        // A read leases what it returns, so a healthy store re-marks the entry and the next read moves past it. This
        // pins the behaviour against a store that does NOT: refusing must not read on, or the refusal path would
        // recurse without bound on the dispatch thread. Exactly one refusal per poll, whatever the store does.
        final CapturingSender sender = new CapturingSender();
        final SilentBacklog backlog = new SilentBacklog(sender);
        backlog.refuseDeletes = true;
        backlog.queued = new SouthboundCommand("stuck-1", value(1));
        final SouthboundWriteQueue queue =
                new SouthboundWriteQueue(ADAPTER, TAG, NODE, backlog, sender, tokens(), metrics());
        queue.openWindow();
        sender.pump(queue);
        sender.settleLast(SouthboundWriteOutcome.SUCCEEDED, null);
        sender.pump(queue);

        final long afterCommit = queue.redeliveriesRefused();
        pollOnce(queue);
        sender.pump(queue);

        assertThat(queue.redeliveriesRefused()).isEqualTo(afterCommit + 1);
    }

    @Test
    void theDisposedMemoryIsBounded_soAnOldCommitCannotBlockALegitimateCommand() {
        // The memory that refuses a disposed command must not grow without bound on the dispatch thread. Bounding it
        // is only safe because the case it guards is one stuck entry surfacing among empty reads — so pin the bound
        // itself, and that a command far enough back is treated as new again.
        final CapturingSender sender = new CapturingSender();
        final SilentBacklog backlog = new SilentBacklog(sender);
        final SouthboundWriteQueue queue =
                new SouthboundWriteQueue(ADAPTER, TAG, NODE, backlog, sender, tokens(), metrics());
        queue.openWindow();
        sender.pump(queue);

        for (int i = 0; i <= SouthboundWriteQueue.DISPOSED_IDS_REMEMBERED; i++) {
            backlog.queued = new SouthboundCommand("c-" + i, value(i));
            pollOnce(queue);
            sender.pump(queue);
            sender.settleLast(SouthboundWriteOutcome.SUCCEEDED, null);
            sender.pump(queue);
        }
        assertThat(queue.committed()).isEqualTo(SouthboundWriteQueue.DISPOSED_IDS_REMEMBERED + 1L);
        assertThat(queue.redeliveriesRefused()).isZero();

        // "c-0" has been evicted by the newer disposals, so it reads as an ordinary command again.
        backlog.queued = new SouthboundCommand("c-0", value(0));
        pollOnce(queue);
        sender.pump(queue);

        assertThat(queue.redeliveriesRefused()).isZero();
        assertThat(queue.head()).isNotNull();
    }

    /** Advance the channel by one backstop-poll interval. */
    private static void pollOnce(final @NotNull SouthboundWriteQueue queue) {
        for (int tick = 0; tick < SouthboundWriteQueue.POLL_TICKS; tick++) {
            queue.onTick();
        }
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────────────────

    /** A channel over the in-memory store, with the sender that plays the wrapper. */
    private static final class Fixture {
        private final CapturingSender sender = new CapturingSender();
        private final InMemorySouthboundWriteBacklog backlog = new InMemorySouthboundWriteBacklog(100, TAG, sender);
        private final SouthboundWriteQueue queue =
                new SouthboundWriteQueue(ADAPTER, TAG, NODE, backlog, sender, tokens(), metrics());

        private void pump() {
            sender.pump(queue);
        }

        private void settleAndPump(final @NotNull SouthboundWriteOutcome outcome) {
            settleAndPump(outcome, null);
        }

        private void settleAndPump(final @NotNull SouthboundWriteOutcome outcome, final String reason) {
            sender.settleLast(outcome, reason);
            sender.pump(queue);
        }
    }

    /**
     * A store that never hints and answers reads from one slot — for the cases where the arrival notification is
     * exactly what must not be relied upon.
     */
    private static final class SilentBacklog implements SouthboundWriteBacklog {

        private final @NotNull CapturingSender answers;
        private @Nullable SouthboundCommand queued;
        private @Nullable String undeliverableId;
        private final @NotNull List<String> deleted = new ArrayList<>();
        private int reportedSize;
        private int markerReleases;
        private boolean failNextRead;
        private boolean deferSizeAnswer;
        /** Models a store whose removeShared never takes: the entry stays queued and keeps coming back. */
        private boolean refuseDeletes;

        private @Nullable Long deferredSizeToken;

        private SilentBacklog(final @NotNull CapturingSender answers) {
            this.answers = answers;
        }

        @Override
        public void requestRead(final long readToken) {
            if (failNextRead) {
                failNextRead = false;
                answers.tell(new SouthboundRead(
                        TAG, readToken, null, null, null, new RuntimeException("scripted read failure")));
                return;
            }
            if (undeliverableId != null) {
                final String id = undeliverableId;
                undeliverableId = null;
                answers.tell(new SouthboundRead(TAG, readToken, null, id, "the command could not be decoded", null));
                return;
            }
            answers.tell(new SouthboundRead(TAG, readToken, queued, null, null, null));
        }

        @Override
        public void requestSize(final long readToken) {
            if (deferSizeAnswer) {
                deferredSizeToken = readToken;
                return; // held back so a test can decide what the channel is doing when it lands
            }
            answers.tell(new SouthboundSize(TAG, readToken, reportedSize, null));
        }

        /** Release a depth answer held back by {@link #deferSizeAnswer}. */
        private void releaseSizeAnswer() {
            answers.tell(new SouthboundSize(TAG, requireNonNull(deferredSizeToken), reportedSize, null));
            deferredSizeToken = null;
        }

        @Override
        public void delete(final @NotNull String commandId) {
            deleted.add(commandId);
            if (!refuseDeletes && queued != null && queued.id().equals(commandId)) {
                queued = null;
            }
        }

        @Override
        public void discardAll() {
            queued = null;
        }

        @Override
        public void releaseMarkers() {
            markerReleases++;
        }

        @Override
        public void close() {}
    }

    private static @NotNull DataPoint value(final int i) {
        return new TestDataPoint(TAG, i);
    }
}
