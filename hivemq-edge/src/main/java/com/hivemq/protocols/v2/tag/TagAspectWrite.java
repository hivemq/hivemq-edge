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
package com.hivemq.protocols.v2.tag;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxSender;
import com.hivemq.adapter.sdk.api.v2.model.VerifyOutcome;
import com.hivemq.adapter.sdk.api.v2.model.WriteEntry;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.Tag;
import com.hivemq.protocols.v2.fsm.FSM;
import com.hivemq.protocols.v2.runtime.Backoff;
import com.hivemq.protocols.v2.runtime.BatchCollector;
import com.hivemq.protocols.v2.runtime.Clock;
import com.hivemq.protocols.v2.runtime.PriorityTimerQueue;
import com.hivemq.protocols.v2.runtime.ProtocolAdapterMetrics;
import com.hivemq.protocols.v2.runtime.RetryPolicy;
import com.hivemq.protocols.v2.runtime.TimerHandle;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperMessage;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperSouthboundMessage.TagWritability;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperSouthboundMessage.WriteSettled;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The write half of a tag's behavior — one of the two independent aspects every tag has. A write
 * aspect is a {@link FSM} over {@link TagAspectWriteState}: it shares the five pre-operating states with the read
 * aspect (built by {@link TagAspectPreOperatingTransitions}, the same shared rows) and adds the
 * {@code WAITING_FOR_WRITE_REQUEST} ⇄ {@code WAITING_FOR_WRITE_RESULT} write cycle.
 * <p>
 * Like the read aspect it lives inside the wrapper actor and runs only on its single dispatch thread; it owns no
 * thread. It requests writes by appending to the shared {@link BatchCollector}, schedules its verification-retry
 * timer on the actor's single {@link PriorityTimerQueue}, observes the events the wrapper routes to it, and
 * re-verifies through the shared {@link SharedNodeVerification}.
 * <p>
 * Two kinds of input drive it, mirroring the read aspect:
 * <ul>
 * <li><b>events</b> ({@link TagAspectEvent}) run through the transition table — verification outcomes, the
 * verification-retry timer expiry, and the southbound write request and its acknowledgment;</li>
 * <li><b>goal and adapter-readiness changes</b> bypass the table: the three-condition goal ({@link TagAspectGoal})
 * and the {@code DEACTIVATED} ↔ operating coupling to the adapter's connection are applied directly.</li>
 * </ul>
 * <b>One write is in flight at a time, and the aspect never queues</b> — it advertises, in effect, an in-flight
 * window of exactly one write. Each write carries a delivery token; the aspect requests the write, remembers that
 * token as the in-flight one, and reports the outcome exactly once — as a {@link WriteSettled} message back to its
 * own mailbox — when the device acknowledges ({@link SouthboundWriteOutcome#SUCCEEDED}/{@code FAILED}) or the write
 * is abandoned ({@link SouthboundWriteOutcome#ABORTED} on deactivation or a lost connection). A write arriving
 * while one is in flight is <b>not queued</b>: it is reported {@link SouthboundWriteOutcome#REJECTED_BUSY} at once
 * and counted as a window violation; a write arriving while the aspect cannot write at all is reported
 * {@link SouthboundWriteOutcome#ABORTED} so its sender keeps the command queued for redelivery. Back-pressure
 * therefore lives in the channel in front of the aspect
 * ({@link com.hivemq.protocols.v2.southbound.SouthboundWriteQueue}, which holds the next write until the current
 * one settles) and the durable backlog behind it — not in the adapter.
 */
public final class TagAspectWrite implements TagAspectVerifying {

    private static final @NotNull Logger log = LoggerFactory.getLogger(TagAspectWrite.class);

    /**
     * The failure count past which a tag's failures are logged at {@code ERROR} rather than {@code WARN} — a few
     * hiccups are routine, sustained failures are not (mirrors the read aspect).
     */
    private static final int SUSTAINED_FAILURE_THRESHOLD = 5;

    /**
     * The adapter's connection phase as the aspect last saw it — decides whether activating verifies now or waits.
     */
    private enum AdapterPhase {
        DISCONNECTED,
        VERIFYING,
        READY
    }

    private final @NotNull String adapterId;
    private final @NotNull Node node;
    private final @NotNull Tag tag;

    private final @NotNull Clock clock;
    private final @NotNull PriorityTimerQueue timers;
    private final @NotNull BatchCollector batches;
    private final @NotNull ProtocolAdapterMetrics metrics;
    private final @NotNull SharedNodeVerification sharedNodeVerification;
    private final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> selfSender;
    private final long writeResultTimeoutMillis;
    private final @NotNull Backoff verificationRetryBackoff;

    private final @NotNull FSM<TagAspectState, TagAspectEvent, TagAspectWrite> machine;

    private @NotNull TagAspectGoal goal = TagAspectGoal.inactive();
    private @NotNull AdapterPhase adapterPhase = AdapterPhase.DISCONNECTED;
    private int failureCount;
    private @Nullable String lastFailureReason;
    private long lastTransitionAtMillis;
    private @Nullable TimerHandle activeTimer;

    /**
     * The delivery token of the write currently in flight, reported exactly once when it reaches a terminal
     * outcome. {@code null} means no write is in flight.
     */
    private @Nullable Long inFlightDeliveryToken;

    /**
     * The correlation id stamped on the in-flight write, echoed back by the adapter's acknowledgment.
     * <p>
     * This is what makes a <b>late</b> duplicate result harmless. {@code writeResult} identifies its write by node,
     * and this aspect serves at most one write per node at a time, so without the id a result reported twice is
     * indistinguishable from the acknowledgment of the write that followed — and acting on it settles, and so
     * <b>deletes from the durable store</b>, a command the device was never asked to execute, while counting it
     * committed. A lost command recorded as delivered is worse than a duplicated one, which is what at-least-once
     * already tolerates.
     */
    private long inFlightAttemptId;

    /**
     * The batch collector's write-dispatch count when the in-flight write was posted. While it is unchanged the
     * write is still sitting in the batch, so any result arriving cannot be its own — see {@link #acceptWriteResult}.
     */
    private long writeDispatchesAtRequest;

    /**
     * @param adapterId              the owning adapter's id.
     * @param node                   the protocol-specific node.
     * @param tag                    Edge's half of the pair.
     * @param clock                  the actor clock the timers are scheduled against.
     * @param timers                 the actor's single timer queue.
     * @param batches                the actor's batch collector — where write requests are posted.
     * @param metrics                the per-adapter metrics (per-tag failure counters).
     * @param sharedNodeVerification the shared verification authority for re-verifications.
     * @param selfSender             the wrapper's own mailbox — where this aspect reports settlements and
     *                               writability changes, so the delivery side never needs a callback into it.
     * @param writeResultTimeoutMillis the deadline for a requested write's result — the adapter's command timeout,
     *                                 the same one the read aspect arms on a poll (EDG-824 #15).
     * @param retryPolicy            the backoff policy for verification retries.
     */
    public TagAspectWrite(
            final @NotNull String adapterId,
            final @NotNull Node node,
            final @NotNull Tag tag,
            final @NotNull Clock clock,
            final @NotNull PriorityTimerQueue timers,
            final @NotNull BatchCollector batches,
            final @NotNull ProtocolAdapterMetrics metrics,
            final @NotNull SharedNodeVerification sharedNodeVerification,
            final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> selfSender,
            final long writeResultTimeoutMillis,
            final @NotNull RetryPolicy retryPolicy) {
        this.adapterId = adapterId;
        this.node = node;
        this.tag = tag;
        this.clock = clock;
        this.timers = timers;
        this.batches = batches;
        this.metrics = metrics;
        this.sharedNodeVerification = sharedNodeVerification;
        this.selfSender = selfSender;
        this.writeResultTimeoutMillis = writeResultTimeoutMillis;
        this.verificationRetryBackoff = new Backoff(retryPolicy);
        this.machine = new FSM<>(TagAspectWriteState.DEACTIVATED, TagAspectWriteTransitions.table(), this);
    }

    // ── goal and adapter-readiness coupling (bypass the table) ───────────────────────────────

    /**
     * Apply a new aspect goal (the three-condition rule; for a write aspect the direction is
     * southbound). When the goal becomes active the aspect leaves {@code DEACTIVATED}; when it becomes inactive it
     * returns to {@code DEACTIVATED}, cancelling any timer — never reconnecting the adapter.
     *
     * @param newGoal the recomputed goal.
     */
    public void applyGoal(final @NotNull TagAspectGoal newGoal) {
        final boolean wasActive = goal.active();
        goal = newGoal;
        final boolean nowActive = goal.active();
        if (wasActive == nowActive) {
            return;
        }
        if (nowActive) {
            activate();
        } else {
            deactivate();
        }
    }

    private void activate() {
        switch (adapterPhase) {
            case DISCONNECTED -> moveTo(TagAspectWriteState.WAITING_FOR_ADAPTER_READY);
            case VERIFYING, READY -> {
                // Activated while the adapter is up: this node missed the connect-time gate verification, so ask
                // for a fresh one of its own — no reconnect.
                moveTo(TagAspectWriteState.WAITING_FOR_VERIFICATION);
                requestVerification();
            }
        }
    }

    private void deactivate() {
        if (machine.state().isDeactivated()) {
            return;
        }
        cancelActiveTimer();
        settleInFlight(SouthboundWriteOutcome.ABORTED, "the tag was deactivated");
        moveTo(TagAspectWriteState.DEACTIVATED);
    }

    /**
     * The adapter began verifying: an active aspect waiting for the adapter moves into verification
     * and consumes the connect-time gate result the wrapper routes to it — it does not request its own.
     */
    public void onAdapterVerifying() {
        adapterPhase = AdapterPhase.VERIFYING;
        if (machine.state() == TagAspectWriteState.WAITING_FOR_ADAPTER_READY) {
            moveTo(TagAspectWriteState.WAITING_FOR_VERIFICATION);
        }
    }

    /**
     * The adapter reached {@code CONNECTED}. When verification was skipped the aspect is still
     * waiting for the adapter — treat the connection as verified and rest ready for writes; otherwise it has
     * already advanced through verification and nothing happens here.
     */
    public void onAdapterReady() {
        adapterPhase = AdapterPhase.READY;
        if (machine.state() == TagAspectWriteState.WAITING_FOR_ADAPTER_READY) {
            moveTo(enterVerified());
        }
    }

    /**
     * The adapter is no longer connected: every aspect except a deactivated or permanently-failed
     * one returns to waiting for the adapter and re-verifies on the next connection. A permanent verification
     * failure is sticky — only a user-commanded retry clears it.
     */
    public void onAdapterUnavailable() {
        adapterPhase = AdapterPhase.DISCONNECTED;
        final TagAspectState current = machine.state();
        if (!current.isDeactivated() && !current.isPermanentVerificationFailure()) {
            cancelActiveTimer();
            settleInFlight(SouthboundWriteOutcome.ABORTED, "the adapter connection was lost");
            verificationRetryBackoff.reset();
            moveTo(TagAspectWriteState.WAITING_FOR_ADAPTER_READY);
        }
    }

    /**
     * A user-commanded tag retry: if the aspect is in permanent verification failure, reset its
     * counters and re-verify (or wait for the adapter). Any other state is left untouched — a no-op here, reported
     * as a skip reason by the REST layer in a later task.
     */
    public void retry() {
        if (!machine.state().isPermanentVerificationFailure()) {
            return;
        }
        failureCount = 0;
        lastFailureReason = null;
        verificationRetryBackoff.reset();
        if (adapterPhase == AdapterPhase.READY) {
            moveTo(TagAspectWriteState.WAITING_FOR_VERIFICATION);
            requestVerification();
        } else {
            moveTo(TagAspectWriteState.WAITING_FOR_ADAPTER_READY);
        }
    }

    // ── routed events (drive the table) ─────────────────────────────────────────────────────────────────────────

    /**
     * Feed the node's verification outcome to the machine.
     *
     * @param outcome the verification outcome.
     */
    public void onVerifyResult(final @NotNull VerifyOutcome outcome) {
        switch (outcome) {
            case final VerifyOutcome.Success ignored -> dispatch(new TagAspectEvent.VerifySucceeded());
            case final VerifyOutcome.TransientFailure transientFailure ->
                dispatch(new TagAspectEvent.VerifyTransientlyFailed(transientFailure.reason()));
            case final VerifyOutcome.PermanentFailure permanentFailure ->
                dispatch(new TagAspectEvent.VerifyPermanentlyFailed(permanentFailure.reason()));
        }
    }

    /**
     * A southbound write arrived for the tag. Drives the write cycle when the aspect is resting at
     * {@code WAITING_FOR_WRITE_REQUEST} — the completion is settled later with the device's result. In any other
     * state the table's {@code unmatched} slot settles the completion immediately, and the aspect never queues:
     * {@link SouthboundWriteOutcome#REJECTED_BUSY} while a write is in flight (a window violation), or
     * {@link SouthboundWriteOutcome#ABORTED} while the aspect cannot write at all — so the sender keeps the
     * command queued for redelivery.
     *
     * @param value         the reused v1 value to write.
     * @param deliveryToken the delivering channel's correlation, echoed in the settlement report.
     */
    public void onWriteRequested(final @NotNull DataPoint value, final long deliveryToken) {
        dispatch(new TagAspectEvent.WriteRequested(value, deliveryToken));
    }

    /**
     * The adapter acknowledged the in-flight write.
     *
     * @param success whether the write succeeded.
     * @param reason  the failure reason, or {@code null} on success.
     */
    public void onWriteResult(final long attemptId, final boolean success, final @Nullable String reason) {
        if (!acceptWriteResult(attemptId)) {
            return;
        }
        if (success) {
            dispatch(new TagAspectEvent.WriteSucceeded());
        } else {
            dispatch(new TagAspectEvent.WriteFailed(reason != null ? reason : "write failed"));
        }
    }

    // ── actions the transition table runs ───────────────────────────────────────────────────────────────────────

    @Override
    public @NotNull TagAspectState enterVerified() {
        cancelActiveTimer(); // clear the verify-result deadline; nothing is armed in the resting write state
        verificationRetryBackoff.reset();
        // The healthy resting goal state: ready to accept southbound writes. No kickoff work — unlike the read
        // aspect there is no poll to schedule or subscription to request.
        return TagAspectWriteState.WAITING_FOR_WRITE_REQUEST;
    }

    @Override
    public void requestVerification() {
        sharedNodeVerification.requestVerification(node);
        armVerifyResultDeadline();
    }

    /**
     * Arm the verify-result deadline, the write-path twin of the read aspect's. Without it an adapter that accepts
     * a re-verification and never reports an outcome parks this aspect in {@code WAITING_FOR_VERIFICATION} forever
     * — no timer armed, no backoff consulted, no writability crossing left to emit — while the adapter's own
     * connection stays up and no watchdog runs.
     * <p>
     * That is not a hypothetical here: the write-result deadline's recovery path re-verifies, and the adapter it
     * re-verifies against is by definition one that has just failed to answer. Without this deadline the recovery
     * from a mute adapter is itself a permanent wedge, with the command still sitting in the store — the exact
     * failure the write-result deadline exists to prevent, moved one step down the road.
     */
    private void armVerifyResultDeadline() {
        scheduleTimer(writeResultTimeoutMillis, () -> {
            activeTimer = null;
            sharedNodeVerification.abandonVerification(node);
            dispatch(new TagAspectEvent.VerifyTransientlyFailed(
                    "no verify result within " + writeResultTimeoutMillis + " ms"));
        });
    }

    @Override
    public void onTransientVerificationFailure(final @NotNull String reason) {
        recordFailure(reason);
        scheduleTimer(
                verificationRetryBackoff.nextDelayMillis(),
                () -> dispatch(new TagAspectEvent.VerificationRetryElapsed()));
    }

    @Override
    public void onPermanentVerificationFailure(final @NotNull String reason) {
        recordFailure(reason);
    }

    void requestWrite(final @NotNull DataPoint value) {
        batches.write(new WriteEntry(node, value, inFlightAttemptId));
    }

    /**
     * Begin the single in-flight write: post it to the batch collector and remember its completion so the
     * device's acknowledgment can settle it.
     *
     * @param event the write request event carrying the value and its delivery token.
     */
    void beginWrite(final @NotNull TagAspectEvent.WriteRequested event) {
        // Defensive: the single-in-flight invariant means no token should linger when a new write begins. If one
        // somehow does, abort it rather than leave its channel waiting on a report that never comes.
        if (inFlightDeliveryToken != null) {
            settleInFlight(SouthboundWriteOutcome.ABORTED, "superseded by a new write");
        }
        // Take ownership of the token BEFORE posting the request. A write this aspect never recorded could not be
        // reported by any later path — not the acknowledgment, not deactivation, not a lost connection — so the
        // channel's delivery slot would stay occupied for good and the tag would silently stop accepting writes.
        inFlightDeliveryToken = event.deliveryToken();
        // Minted before the entry is built, so the id travels with the write and comes back on its acknowledgment.
        inFlightAttemptId = batches.nextWriteAttemptId();
        try {
            requestWrite(event.value());
        } catch (final RuntimeException postFailure) {
            settleInFlight(SouthboundWriteOutcome.ABORTED, "the write could not be posted to the adapter");
            throw postFailure;
        }
        // The batch this write joins has not been handed to the adapter yet; the next tick does that. Remembering
        // the dispatch count now is what lets onWriteResult tell this write's acknowledgment from a duplicate of
        // the PREVIOUS one — see acceptWriteResult.
        writeDispatchesAtRequest = batches.writeDispatches();
        armWriteResultDeadline();
    }

    /**
     * Arm the write-result deadline on the aspect's single timer slot. Without it a write the adapter accepts but
     * never acknowledges parks the aspect in {@code WAITING_FOR_WRITE_RESULT} forever: that state
     * {@link TagAspectState#isOperating() is operating}, so no writability crossing is emitted, the tag snapshot
     * stays green, and the delivering channel's slot never frees — every later poll, arrival hint and window
     * reopen is a no-op and the command is stranded with no logged reason. The adapter's own connection stays up,
     * so no watchdog covers it. This is the write-path analogue of the read aspect's poll-result deadline
     * (EDG-824 #15) and reuses the same adapter command timeout.
     * <p>
     * On expiry the write is settled {@link SouthboundWriteOutcome#ABORTED} — <b>kept</b>, never dead-lettered: a
     * missing acknowledgment says nothing about whether the device executed the command, and at-least-once means
     * resolving that ambiguity in favour of redelivery. The aspect then re-verifies, which crosses out of
     * operating (closing the delivery window) and back in on success (reopening it), so the very same command is
     * redelivered rather than silently lost.
     */
    private void armWriteResultDeadline() {
        scheduleTimer(writeResultTimeoutMillis, () -> {
            activeTimer = null;
            if (inFlightDeliveryToken == null) {
                return; // already settled — a result landed in the same tick the deadline came due
            }
            if (batches.writeDispatches() == writeDispatchesAtRequest) {
                // The write is still sitting in the batch: the tick runs `timers.fireDue` BEFORE
                // `batches.dispatch`, so a deadline armed when the write was posted can come due before the adapter
                // has ever seen it. Aborting here would blame the adapter for the framework's own latency — and at a
                // command timeout shorter than the tick it would do so every time, redelivering and rewriting the
                // same command forever without ever committing it. Restart the clock from the dispatch instead.
                armWriteResultDeadline();
                return;
            }
            final String reason = "no write result within " + writeResultTimeoutMillis + " ms";
            // Explicitly at WARN, not left to recordFailure's escalating severity: the first occurrence of this is
            // an adapter breaking its acknowledgment contract, and recordFailure logs a first failure at DEBUG.
            // The command survives, but it is being re-executed on the device on every recovery cycle.
            log.warn(
                    "Write aspect of tag '{}' on adapter '{}' abandoned a write: {}. The command is kept and will "
                            + "be delivered again once the tag re-verifies.",
                    tag.name(),
                    adapterId,
                    reason);
            metrics.incrementWriteTimeout(tag.name());
            recordFailure(reason);
            settleInFlight(SouthboundWriteOutcome.ABORTED, reason);
            moveTo(TagAspectWriteState.WAITING_FOR_VERIFICATION);
            requestVerification();
        });
    }

    /**
     * @return whether a write result arriving now can plausibly belong to the write currently in flight. It cannot
     *         if that write has not yet left the batch collector: the result must then be a duplicate of an earlier
     *         one, and acting on it would settle — and so <b>delete from the durable store</b> — a command the
     *         device has not even been asked to execute yet.
     *         <p>
     *         This dispatch-count test is the <b>fallback</b>, kept for results that carry no correlation id
     *         ({@link WriteEntry#UNCORRELATED} — a test rig, or an adapter written against the older contract). It
     *         closes only the immediate window. A result that does carry an id is checked against the in-flight
     *         attempt first, which closes the late duplicate the dispatch count cannot see.
     */
    private boolean acceptWriteResult(final long attemptId) {
        // The correlation the SDK now carries. An adapter that echoes the entry's attempt id makes a stale result
        // self-identifying, whenever it arrives: it names a write this aspect is no longer serving, so it is simply
        // dropped instead of being credited to whatever is in flight now. Results with UNCORRELATED fall through to
        // the older heuristic below — that is what a rig or an adapter predating this contract sends.
        if (attemptId != WriteEntry.UNCORRELATED && attemptId != inFlightAttemptId) {
            log.warn(
                    "Write aspect of tag '{}' on adapter '{}' ignored a write result for attempt {} while serving "
                            + "attempt {} — the adapter acknowledged a write that is no longer in flight",
                    tag.name(),
                    adapterId,
                    attemptId,
                    inFlightAttemptId);
            return false;
        }
        if (inFlightDeliveryToken == null || batches.writeDispatches() != writeDispatchesAtRequest) {
            return true;
        }
        log.warn(
                "Write aspect of tag '{}' on adapter '{}' ignored a write result that arrived before its write "
                        + "reached the adapter — the adapter reported more results than it was given writes",
                tag.name(),
                adapterId);
        return false;
    }

    /**
     * The device acknowledged the in-flight write: settle its completion and return to the resting goal state. A
     * failure is recorded and counted but does not flap the tag to {@code ERROR}.
     *
     * @param success whether the write succeeded.
     * @param reason  the failure reason, or {@code null} on success.
     * @return the resting goal state {@code WAITING_FOR_WRITE_REQUEST}.
     */
    @NotNull
    TagAspectState completeInFlightWrite(final boolean success, final @Nullable String reason) {
        cancelActiveTimer(); // the result arrived — stand the write-result deadline down
        if (!success) {
            recordFailure(reason != null ? reason : "write failed");
        }
        settleInFlight(success ? SouthboundWriteOutcome.SUCCEEDED : SouthboundWriteOutcome.FAILED, reason);
        return TagAspectWriteState.WAITING_FOR_WRITE_REQUEST;
    }

    void logUnexpectedEvent(final @NotNull TagAspectEvent event) {
        if (event instanceof final TagAspectEvent.WriteRequested writeRequested) {
            if (machine.state() == TagAspectWriteState.WAITING_FOR_WRITE_RESULT) {
                // A second write while one is in flight: the aspect never queues — reject it observably as a
                // violation of the advertised window of one. This stays at zero when the sender paces deliveries
                // to the window.
                metrics.incrementWriteRejected(tag.name());
                log.warn(
                        "Write aspect of tag '{}' on adapter '{}' rejected a southbound write: one is already in "
                                + "flight (the sender must hold the next write until the current one settles)",
                        tag.name(),
                        adapterId);
                report(
                        writeRequested.deliveryToken(),
                        SouthboundWriteOutcome.REJECTED_BUSY,
                        "a write is already in flight");
                return;
            }
            // A write arriving while the aspect cannot write (deactivated, waiting for the adapter, verifying, or
            // permanently failed) is not a window violation: report it ABORTED so the sender keeps the command
            // queued for redelivery — never a silent drop, never an unreported write.
            log.debug(
                    "Write aspect of tag '{}' on adapter '{}' aborted a southbound write arriving in {}",
                    tag.name(),
                    adapterId,
                    machine.state());
            report(
                    writeRequested.deliveryToken(),
                    SouthboundWriteOutcome.ABORTED,
                    "the tag cannot write in " + machine.state());
            return;
        }
        log.debug(
                "Write aspect of tag '{}' on adapter '{}' ignored unexpected {} in {}",
                tag.name(),
                adapterId,
                event.getClass().getSimpleName(),
                machine.state());
    }

    /**
     * Report the in-flight write's terminal outcome exactly once, then forget it. A no-op when nothing is in
     * flight.
     *
     * @param outcome the terminal outcome to report.
     * @param reason  what made it terminal, or {@code null} — travels into the dead-letter log line.
     */
    private void settleInFlight(final @NotNull SouthboundWriteOutcome outcome, final @Nullable String reason) {
        final Long token = inFlightDeliveryToken;
        if (token != null) {
            inFlightDeliveryToken = null;
            // If the write never left the batch collector, retract it: it has been abandoned, and dispatching it a
            // moment later would write to the device a value already reported as not written — or, when a reload
            // re-pointed this tag, write it to a node the configuration no longer maps.
            if (batches.writeDispatches() == writeDispatchesAtRequest && batches.retractWrite(node)) {
                log.debug(
                        "Write aspect of tag '{}' on adapter '{}' retracted a write that had not reached the "
                                + "adapter yet",
                        tag.name(),
                        adapterId);
            }
            report(token, outcome, reason);
        }
    }

    /**
     * Tell the delivering channel what became of one write. This goes through the wrapper's own mailbox rather than
     * a direct call: the aspect stays ignorant of the delivery side, and because every southbound message shares
     * one priority band, a report emitted before a writability change is always seen before it.
     *
     * @param deliveryToken the channel's correlation for the write being reported.
     * @param outcome       the terminal outcome.
     * @param reason        the device's own words, or {@code null}.
     */
    private void report(
            final long deliveryToken, final @NotNull SouthboundWriteOutcome outcome, final @Nullable String reason) {
        selfSender.tell(new WriteSettled(tag.name(), deliveryToken, outcome, reason));
    }

    // ── snapshot accessors (pure reads on the dispatch thread) ───────────────────────────────

    /**
     * @return the current aspect state.
     */
    public @NotNull TagAspectState state() {
        return machine.state();
    }

    /**
     * @return the current aspect state name for the published snapshot.
     */
    public @NotNull String stateName() {
        return machine.state().toString();
    }

    /**
     * @return whether the aspect's goal is currently active (the three-condition rule holds).
     */
    public boolean goalActive() {
        return goal.active();
    }

    /**
     * @return whether the aspect is operating at its goal (ready for or performing a write), per
     *         {@link TagAspectState#isOperating()}.
     */
    public boolean operating() {
        return machine.state().isOperating();
    }

    /**
     * @return whether the aspect is suspended after a permanent verification failure.
     */
    public boolean permanentFailure() {
        return machine.state().isPermanentVerificationFailure();
    }

    /**
     * @return whether the aspect is awaiting the connect-time verification result — the signal the
     *         coordinator uses to select this node for the single connect verification batch.
     */
    public boolean awaitingVerification() {
        return machine.state() == TagAspectWriteState.WAITING_FOR_VERIFICATION;
    }

    /**
     * @return the cumulative failure count (verification / write).
     */
    public int failureCount() {
        return failureCount;
    }

    /**
     * @return the most recent failure reason, or {@code null} if none.
     */
    public @Nullable String lastFailureReason() {
        return lastFailureReason;
    }

    /**
     * @return the clock time of the last aspect transition, in milliseconds.
     */
    public long lastTransitionAtMillis() {
        return lastTransitionAtMillis;
    }

    // ── internals ───────────────────────────────────────────────────────────────────────────────────────────────

    private void dispatch(final @NotNull TagAspectEvent event) {
        final TagAspectState before = machine.state();
        machine.onEvent(event);
        final TagAspectState after = machine.state();
        if (after != before) {
            lastTransitionAtMillis = clock.nowMillis();
            notifyReadinessCrossing(before, after);
        }
    }

    private void moveTo(final @NotNull TagAspectState next) {
        final TagAspectState before = machine.state();
        if (before != next) {
            machine.transitionTo(next);
            lastTransitionAtMillis = clock.nowMillis();
            notifyReadinessCrossing(before, next);
        }
    }

    /**
     * Notify the readiness listener when a transition crossed the writability boundary
     * ({@link TagAspectState#isOperating()}). Transitions within the operating pair — the normal write
     * round-trip — never notify: a tag mid-write is busy, not unwritable.
     *
     * @param before the state before the transition.
     * @param after  the state after it.
     */
    private void notifyReadinessCrossing(final @NotNull TagAspectState before, final @NotNull TagAspectState after) {
        if (before.isOperating() == after.isOperating()) {
            return;
        }
        selfSender.tell(new TagWritability(tag.name(), after.isOperating()));
    }

    private void scheduleTimer(final long delayMillis, final @NotNull Runnable onFire) {
        cancelActiveTimer();
        // Saturate: a near-Long.MAX_VALUE configured delay must mean "practically never", not an overflowed
        // negative deadline that fires immediately (the read aspect saturates the same configured value).
        final long fireAtMillis = clock.nowMillis() + delayMillis;
        activeTimer = timers.schedule(fireAtMillis < 0 ? Long.MAX_VALUE : fireAtMillis, onFire);
    }

    private void cancelActiveTimer() {
        if (activeTimer != null) {
            timers.cancel(activeTimer);
            activeTimer = null;
        }
    }

    private void recordFailure(final @NotNull String reason) {
        failureCount++;
        lastFailureReason = reason;
        metrics.incrementTagFailure(tag.name());
        // Escalating severity: a first hiccup is routine, sustained failures are not.
        if (failureCount == 1) {
            log.debug("Write aspect of tag '{}' on adapter '{}' failed: {}", tag.name(), adapterId, reason);
        } else if (failureCount < SUSTAINED_FAILURE_THRESHOLD) {
            log.warn(
                    "Write aspect of tag '{}' on adapter '{}' failed ({} times): {}",
                    tag.name(),
                    adapterId,
                    failureCount,
                    reason);
        } else {
            log.error(
                    "Write aspect of tag '{}' on adapter '{}' has failed {} times: {}",
                    tag.name(),
                    adapterId,
                    failureCount,
                    reason);
        }
    }
}
