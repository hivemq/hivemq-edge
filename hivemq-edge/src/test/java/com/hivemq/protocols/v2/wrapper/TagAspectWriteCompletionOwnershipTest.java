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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.hivemq.adapter.sdk.api.v2.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxSender;
import com.hivemq.adapter.sdk.api.v2.model.WriteEntry;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.protocols.v2.runtime.BatchCollector;
import com.hivemq.protocols.v2.runtime.FakeClock;
import com.hivemq.protocols.v2.runtime.PriorityTimerQueue;
import com.hivemq.protocols.v2.runtime.ProtocolAdapterMetrics;
import com.hivemq.protocols.v2.runtime.RetryPolicy;
import com.hivemq.protocols.v2.tag.SharedNodeVerification;
import com.hivemq.protocols.v2.tag.SouthboundWriteOutcome;
import com.hivemq.protocols.v2.tag.TagAspectGoal;
import com.hivemq.protocols.v2.tag.TagAspectWrite;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperSouthboundMessage.WriteSettled;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * The write aspect's ownership of the write in flight, driven directly rather than through the wrapper so the batch
 * collector can be made to fail.
 * <p>
 * The standing invariant is that <b>every write is reported exactly once, whatever happens</b>. A write the aspect
 * accepted but never reported can be reported by nobody else, so the delivering channel's single slot would stay
 * occupied for good and that tag would silently stop accepting writes — with no recovery short of an adapter
 * recreate. The dangerous window is the moment between accepting a write and taking ownership of its token, which
 * is why the aspect records the token <b>before</b> it posts the request.
 */
class TagAspectWriteCompletionOwnershipTest {

    private static final @NotNull String TAG = "setpoint";
    private static final long WRITE_RESULT_TIMEOUT_MILLIS = 30_000L;

    @Test
    void aWriteThatCannotBePostedIsReportedAborted_ratherThanGoingUnreported() {
        final RecordingSender sender = new RecordingSender();
        final BatchCollector batches = refusingCollector();
        final TagAspectWrite aspect = restingWriteAspect(batches, sender);

        assertThatThrownBy(() -> aspect.onWriteRequested(WrapperTestSupport.dataPoint(TAG, "42"), 7L))
                .isInstanceOf(IllegalStateException.class);

        // The failure still escapes — the wrapper's contract guard is what faults the adapter — but the channel is
        // released rather than left waiting on an answer that can never come.
        assertThat(sender.settlements).singleElement().satisfies(settled -> {
            assertThat(settled.deliveryToken()).isEqualTo(7L);
            assertThat(settled.outcome()).isEqualTo(SouthboundWriteOutcome.ABORTED);
        });
    }

    @Test
    void afterAFailedPost_theAspectStillAcceptsTheNextWrite_theTagIsNotWedged() {
        final RecordingSender sender = new RecordingSender();
        final BatchCollector batches = refusingCollector();
        final TagAspectWrite aspect = restingWriteAspect(batches, sender);
        assertThatThrownBy(() -> aspect.onWriteRequested(WrapperTestSupport.dataPoint(TAG, "1"), 1L))
                .isInstanceOf(IllegalStateException.class);

        // The aspect never took the write, so it must still be resting ready — a stuck in-flight token would have
        // made the retry report ABORTED as "superseded" before even trying.
        assertThatThrownBy(() -> aspect.onWriteRequested(WrapperTestSupport.dataPoint(TAG, "2"), 2L))
                .isInstanceOf(IllegalStateException.class);
        assertThat(sender.settlements).hasSize(2);
        assertThat(sender.settlements.get(1).deliveryToken()).isEqualTo(2L);
        assertThat(aspect.stateName()).isEqualTo("WAITING_FOR_WRITE_REQUEST");
    }

    @Test
    void aWriteArrivingWhileTheAspectCannotWrite_isReportedAborted_neverSilentlyDropped() {
        // The delivering channel holds its slot until the write it sent is answered. A write the aspect cannot take
        // must therefore still produce a settlement — the counters and the rejection metric say nothing about that,
        // so only the settlement itself can pin it.
        final RecordingSender sender = new RecordingSender();
        final TagAspectWrite aspect = restingWriteAspect(new BatchCollector(), sender);
        aspect.applyGoal(new TagAspectGoal(false, false, false)); // deactivated: it cannot write at all
        sender.settlements.clear();

        aspect.onWriteRequested(WrapperTestSupport.dataPoint(TAG, "42"), 9L);

        assertThat(sender.settlements).singleElement().satisfies(settled -> {
            assertThat(settled.deliveryToken()).isEqualTo(9L);
            assertThat(settled.outcome()).isEqualTo(SouthboundWriteOutcome.ABORTED);
        });
    }

    @Test
    void aSecondWriteWhileOneIsInFlight_isReportedRejectedBusy_againstItsOwnToken() {
        // The window violation is counted before the report is emitted, so the metric cannot stand in for it. What
        // matters to the channel is that the SECOND token comes back, and that the first write is left alone.
        final RecordingSender sender = new RecordingSender();
        final TagAspectWrite aspect = restingWriteAspect(new BatchCollector(), sender);
        aspect.onWriteRequested(WrapperTestSupport.dataPoint(TAG, "1"), 1L);
        assertThat(sender.settlements).isEmpty(); // the first write is in flight, unanswered

        aspect.onWriteRequested(WrapperTestSupport.dataPoint(TAG, "2"), 2L);

        assertThat(sender.settlements).singleElement().satisfies(settled -> {
            assertThat(settled.deliveryToken()).isEqualTo(2L);
            assertThat(settled.outcome()).isEqualTo(SouthboundWriteOutcome.REJECTED_BUSY);
        });
        assertThat(aspect.stateName()).isEqualTo("WAITING_FOR_WRITE_RESULT");
    }

    @Test
    void aLateDuplicateResult_isDroppedByItsAttemptId_soTheFollowingWriteIsNotCommittedUnwritten() {
        // B4, the case the dispatch-count guard below cannot see. writeResult identifies its write by node, and this
        // aspect serves at most one write per node, so a result reported twice for the same node looks exactly like
        // the acknowledgment of the write that followed. Crediting it would settle that write SUCCEEDED — deleting a
        // durable command the device was never asked to execute, and counting it committed. A lost command recorded
        // as delivered is worse than a duplicated one, which at-least-once already tolerates.
        final RecordingSender sender = new RecordingSender();
        final BatchCollector batches = new BatchCollector();
        final TagAspectWrite aspect = restingWriteAspect(batches, sender);

        aspect.onWriteRequested(WrapperTestSupport.dataPoint(TAG, "1"), 1L);
        final long firstAttempt = dispatchedAttemptId(batches);
        aspect.onWriteResult(firstAttempt, true, null);
        assertThat(sender.settlements).singleElement().satisfies(settled -> {
            assertThat(settled.deliveryToken()).isEqualTo(1L);
            assertThat(settled.outcome()).isEqualTo(SouthboundWriteOutcome.SUCCEEDED);
        });

        // The next command is delivered, reaches the adapter, and is genuinely in flight.
        sender.settlements.clear();
        aspect.onWriteRequested(WrapperTestSupport.dataPoint(TAG, "2"), 2L);
        final long secondAttempt = dispatchedAttemptId(batches);
        assertThat(secondAttempt).isNotEqualTo(firstAttempt);

        // Now the adapter re-reports the FIRST write. It names an attempt this aspect is no longer serving.
        aspect.onWriteResult(firstAttempt, true, null);

        assertThat(sender.settlements).isEmpty(); // the second write is untouched — nothing was committed unwritten
        assertThat(aspect.stateName()).isEqualTo("WAITING_FOR_WRITE_RESULT");

        // The second write's own acknowledgment still settles it.
        aspect.onWriteResult(secondAttempt, true, null);
        assertThat(sender.settlements).singleElement().satisfies(settled -> {
            assertThat(settled.deliveryToken()).isEqualTo(2L);
            assertThat(settled.outcome()).isEqualTo(SouthboundWriteOutcome.SUCCEEDED);
        });
    }

    /** Dispatch the pending batch and return the attempt id the framework stamped on the single write in it. */
    private static long dispatchedAttemptId(final @NotNull BatchCollector batches) {
        final ProtocolAdapter adapter = mock(ProtocolAdapter.class);
        final ArgumentCaptor<List<WriteEntry>> captor = ArgumentCaptor.captor();
        batches.dispatch(adapter);
        verify(adapter, atLeastOnce()).writeBatch(captor.capture());
        return captor.getValue().getLast().attemptId();
    }

    @Test
    void aWriteResultArrivingBeforeTheWriteReachedTheAdapter_isIgnored_soNoCommandIsCommittedUnwritten() {
        // A duplicate acknowledgment of an EARLIER write would otherwise settle whatever token is current, and the
        // channel would delete a durable command the device has not even been asked to execute. Until the batch is
        // dispatched the adapter has not seen this write, so any result reaching the aspect cannot be its own.
        final RecordingSender sender = new RecordingSender();
        final BatchCollector batches = new BatchCollector();
        final TagAspectWrite aspect = restingWriteAspect(batches, sender);
        aspect.onWriteRequested(WrapperTestSupport.dataPoint(TAG, "1"), 1L);

        aspect.onWriteResult(WriteEntry.UNCORRELATED, true, null);

        assertThat(sender.settlements).isEmpty();
        assertThat(aspect.stateName()).isEqualTo("WAITING_FOR_WRITE_RESULT");

        // Once the batch has actually gone to the adapter, the very same result is accepted.
        batches.dispatch(mock(ProtocolAdapter.class));
        aspect.onWriteResult(WriteEntry.UNCORRELATED, true, null);

        assertThat(sender.settlements).singleElement().satisfies(settled -> {
            assertThat(settled.deliveryToken()).isEqualTo(1L);
            assertThat(settled.outcome()).isEqualTo(SouthboundWriteOutcome.SUCCEEDED);
        });
    }

    @Test
    void aWriteTheAdapterNeverAcknowledges_isReportedAbortedAtItsDeadline_andTheTagReVerifies() {
        // Without the deadline this is a permanent, silent wedge: WAITING_FOR_WRITE_RESULT is an operating state,
        // so no writability crossing is emitted, the tag reads healthy, and the channel's slot is never freed —
        // every later poll and window reopen is a no-op and the command is stranded with no logged reason.
        final RecordingSender sender = new RecordingSender();
        final FakeClock clock = new FakeClock();
        final PriorityTimerQueue timers = new PriorityTimerQueue();
        final BatchCollector batches = new BatchCollector();
        final NodeTagPair pair = WrapperTestSupport.pair(TAG);
        final TagAspectWrite aspect = new TagAspectWrite(
                "a1",
                pair.node(),
                pair.tag(),
                clock,
                timers,
                batches,
                mock(ProtocolAdapterMetrics.class),
                mock(SharedNodeVerification.class),
                sender,
                WRITE_RESULT_TIMEOUT_MILLIS,
                new RetryPolicy(1000, 2.0, 30000, 100));
        aspect.applyGoal(new TagAspectGoal(true, true, true));
        aspect.onAdapterReady();
        sender.settlements.clear();

        aspect.onWriteRequested(WrapperTestSupport.dataPoint(TAG, "42"), 5L);
        batches.dispatch(mock(ProtocolAdapter.class)); // the adapter has the write; it simply never answers
        assertThat(sender.settlements).isEmpty();

        clock.advance(WRITE_RESULT_TIMEOUT_MILLIS);
        timers.fireDue(clock.nowMillis());

        // ABORTED, never FAILED: a missing acknowledgment says nothing about whether the device executed the
        // command, so at-least-once resolves the ambiguity by keeping it for redelivery.
        assertThat(sender.settlements).singleElement().satisfies(settled -> {
            assertThat(settled.deliveryToken()).isEqualTo(5L);
            assertThat(settled.outcome()).isEqualTo(SouthboundWriteOutcome.ABORTED);
        });
        // And the aspect re-verifies, which crosses out of operating (closing the delivery window) and back in on
        // success — that crossing is what redelivers the kept command.
        assertThat(aspect.stateName()).isEqualTo("WAITING_FOR_VERIFICATION");
        assertThat(aspect.failureCount()).isEqualTo(1);
    }

    @Test
    void theRecoveryFromAMuteAdapterIsItselfDeadlined_soTheTagCannotParkInVerification() {
        // The write-result deadline recovers by re-verifying — against an adapter that has just proved it does not
        // answer. If that verification is not itself deadlined, the aspect parks in WAITING_FOR_VERIFICATION with no
        // timer, no backoff and no writability crossing left to emit: the same permanent wedge, one step further on,
        // with the command still in the store.
        final RecordingSender sender = new RecordingSender();
        final FakeClock clock = new FakeClock();
        final PriorityTimerQueue timers = new PriorityTimerQueue();
        final BatchCollector batches = new BatchCollector();
        final NodeTagPair pair = WrapperTestSupport.pair(TAG);
        final SharedNodeVerification verification = mock(SharedNodeVerification.class);
        final TagAspectWrite aspect = new TagAspectWrite(
                "a1",
                pair.node(),
                pair.tag(),
                clock,
                timers,
                batches,
                mock(ProtocolAdapterMetrics.class),
                verification,
                sender,
                WRITE_RESULT_TIMEOUT_MILLIS,
                new RetryPolicy(1000, 2.0, 30000, 100));
        aspect.applyGoal(new TagAspectGoal(true, true, true));
        aspect.onAdapterReady();

        aspect.onWriteRequested(WrapperTestSupport.dataPoint(TAG, "42"), 5L);
        batches.dispatch(mock(ProtocolAdapter.class));
        clock.advance(WRITE_RESULT_TIMEOUT_MILLIS);
        timers.fireDue(clock.nowMillis());
        assertThat(aspect.stateName()).isEqualTo("WAITING_FOR_VERIFICATION");

        // The adapter stays mute through the re-verification too. A timer must be pending for it.
        assertThat(timers.size()).isPositive();
        clock.advance(WRITE_RESULT_TIMEOUT_MILLIS);
        timers.fireDue(clock.nowMillis());

        verify(verification).abandonVerification(pair.node());
        assertThat(aspect.stateName()).isEqualTo("WAITING_FOR_VERIFICATION_RETRY");
        assertThat(timers.size()).isPositive(); // and the retry backoff keeps it moving
    }

    @Test
    void theDeadlineDoesNotRunWhileTheWriteIsStillInTheBatch_soAShortTimeoutCannotLoopForever() {
        // The tick fires timers BEFORE it dispatches batches, so a deadline armed when the write was posted can come
        // due before the adapter has ever seen it. Aborting there blames the adapter for the framework's own
        // latency — and at a command timeout shorter than the tick it would do so on every attempt, redelivering and
        // rewriting the same command forever without ever committing it. Nothing in config validation forbids such a
        // timeout, so the deadline has to start from the dispatch.
        final RecordingSender sender = new RecordingSender();
        final FakeClock clock = new FakeClock();
        final PriorityTimerQueue timers = new PriorityTimerQueue();
        final BatchCollector batches = new BatchCollector();
        final NodeTagPair pair = WrapperTestSupport.pair(TAG);
        final TagAspectWrite aspect = new TagAspectWrite(
                "a1",
                pair.node(),
                pair.tag(),
                clock,
                timers,
                batches,
                mock(ProtocolAdapterMetrics.class),
                mock(SharedNodeVerification.class),
                sender,
                1L, // a legal command timeout far shorter than the 50 ms tick
                new RetryPolicy(1000, 2.0, 30000, 100));
        aspect.applyGoal(new TagAspectGoal(true, true, true));
        aspect.onAdapterReady();
        sender.settlements.clear();

        aspect.onWriteRequested(WrapperTestSupport.dataPoint(TAG, "42"), 5L);

        // Several ticks' worth of timer firing with the write still undispatched: it must not be abandoned.
        for (int tick = 0; tick < 5; tick++) {
            clock.advance(50);
            timers.fireDue(clock.nowMillis());
        }
        assertThat(sender.settlements).isEmpty();
        assertThat(aspect.stateName()).isEqualTo("WAITING_FOR_WRITE_RESULT");

        // Once the adapter actually has it, the clock runs and the deadline bites as designed.
        batches.dispatch(mock(ProtocolAdapter.class));
        clock.advance(50);
        timers.fireDue(clock.nowMillis());

        assertThat(sender.settlements).singleElement().satisfies(settled -> {
            assertThat(settled.deliveryToken()).isEqualTo(5L);
            assertThat(settled.outcome()).isEqualTo(SouthboundWriteOutcome.ABORTED);
        });
    }

    @Test
    void aWriteAbandonedBeforeItReachedTheAdapter_isRetractedFromTheBatch_notSentAnyway() {
        // A write lives in the batch collector for up to a tick. Everything that abandons it in that window —
        // deactivation, a lost connection, the result deadline, a reload re-pointing the tag — reports it ABORTED so
        // the command is kept and redelivered. Dispatching the entry afterwards would write a value already reported
        // as not written, and after a reload would write it to a node the configuration no longer maps.
        final RecordingSender sender = new RecordingSender();
        final BatchCollector batches = new BatchCollector();
        final TagAspectWrite aspect = restingWriteAspect(batches, sender);
        aspect.onWriteRequested(WrapperTestSupport.dataPoint(TAG, "42"), 3L);

        aspect.applyGoal(new TagAspectGoal(false, false, false)); // deactivated mid-flight

        assertThat(sender.settlements).singleElement().satisfies(settled -> {
            assertThat(settled.deliveryToken()).isEqualTo(3L);
            assertThat(settled.outcome()).isEqualTo(SouthboundWriteOutcome.ABORTED);
        });

        final ProtocolAdapter adapter = mock(ProtocolAdapter.class);
        batches.dispatch(adapter);
        verify(adapter, never()).writeBatch(any());
    }

    private static @NotNull BatchCollector refusingCollector() {
        final BatchCollector batches = mock(BatchCollector.class);
        doThrow(new IllegalStateException("the batch collector refused the write"))
                .when(batches)
                .write(any());
        return batches;
    }

    /** A write aspect driven to its resting goal state, ready to accept a write. */
    private static @NotNull TagAspectWrite restingWriteAspect(
            final @NotNull BatchCollector batches, final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> sender) {
        final NodeTagPair pair = WrapperTestSupport.pair(TAG);
        final TagAspectWrite aspect = new TagAspectWrite(
                "a1",
                pair.node(),
                pair.tag(),
                new FakeClock(),
                mock(PriorityTimerQueue.class),
                batches,
                mock(ProtocolAdapterMetrics.class),
                mock(SharedNodeVerification.class),
                sender,
                30_000L,
                new RetryPolicy(1000, 2.0, 30000, 100));
        aspect.applyGoal(new TagAspectGoal(true, true, true));
        aspect.onAdapterReady();
        return aspect;
    }

    /** Collects the settlement reports the aspect posts to its own mailbox. */
    private static final class RecordingSender implements MailboxSender<ProtocolAdapterWrapperMessage> {

        private final @NotNull List<WriteSettled> settlements = new ArrayList<>();

        @Override
        public void tell(final @NotNull ProtocolAdapterWrapperMessage message) {
            if (message instanceof final WriteSettled settled) {
                settlements.add(settled);
            }
        }
    }
}
