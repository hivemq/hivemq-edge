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
 * window of exactly one write. Each write carries a {@link SouthboundWriteCompletion}; the aspect requests it,
 * remembers that completion as the in-flight one, and settles it exactly once when the device acknowledges
 * ({@link SouthboundWriteOutcome#SUCCEEDED}/{@code FAILED}) or the write is abandoned
 * ({@link SouthboundWriteOutcome#ABORTED} on deactivation or a lost connection). A write arriving while one is in
 * flight is <b>not queued</b>: it is rejected immediately ({@link SouthboundWriteOutcome#REJECTED_BUSY}, counted
 * as a window violation); a write arriving while the aspect cannot write at all settles
 * {@link SouthboundWriteOutcome#ABORTED} so its sender keeps the command queued for redelivery. Back-pressure
 * therefore lives in the queue in front of the aspect
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
    private final @NotNull Backoff verificationRetryBackoff;

    private final @NotNull FSM<TagAspectState, TagAspectEvent, TagAspectWrite> machine;

    private @NotNull TagAspectGoal goal = TagAspectGoal.inactive();
    private @NotNull AdapterPhase adapterPhase = AdapterPhase.DISCONNECTED;
    private int failureCount;
    private @Nullable String lastFailureReason;
    private long lastTransitionAtMillis;
    private @Nullable TimerHandle activeTimer;

    /** The completion of the write currently in flight, settled exactly once when it reaches a terminal outcome. */
    private @Nullable SouthboundWriteCompletion inFlightCompletion;

    /**
     * @param adapterId              the owning adapter's id.
     * @param node                   the protocol-specific node.
     * @param tag                    Edge's half of the pair.
     * @param clock                  the actor clock the timers are scheduled against.
     * @param timers                 the actor's single timer queue.
     * @param batches                the actor's batch collector — where write requests are posted.
     * @param metrics                the per-adapter metrics (per-tag failure counters).
     * @param sharedNodeVerification the shared verification authority for re-verifications.
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
            final @NotNull RetryPolicy retryPolicy) {
        this.adapterId = adapterId;
        this.node = node;
        this.tag = tag;
        this.clock = clock;
        this.timers = timers;
        this.batches = batches;
        this.metrics = metrics;
        this.sharedNodeVerification = sharedNodeVerification;
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
        settleInFlight(SouthboundWriteOutcome.ABORTED);
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
            settleInFlight(SouthboundWriteOutcome.ABORTED);
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
     * @param value      the reused v1 value to write.
     * @param completion the one-shot back-pressure signal for this write.
     */
    public void onWriteRequested(final @NotNull DataPoint value, final @NotNull SouthboundWriteCompletion completion) {
        dispatch(new TagAspectEvent.WriteRequested(value, completion));
    }

    /**
     * The adapter acknowledged the in-flight write.
     *
     * @param success whether the write succeeded.
     * @param reason  the failure reason, or {@code null} on success.
     */
    public void onWriteResult(final boolean success, final @Nullable String reason) {
        if (success) {
            dispatch(new TagAspectEvent.WriteSucceeded());
        } else {
            dispatch(new TagAspectEvent.WriteFailed(reason != null ? reason : "write failed"));
        }
    }

    // ── actions the transition table runs ───────────────────────────────────────────────────────────────────────

    @Override
    public @NotNull TagAspectState enterVerified() {
        verificationRetryBackoff.reset();
        // The healthy resting goal state: ready to accept southbound writes. No kickoff work — unlike the read
        // aspect there is no poll to schedule or subscription to request.
        return TagAspectWriteState.WAITING_FOR_WRITE_REQUEST;
    }

    @Override
    public void requestVerification() {
        sharedNodeVerification.requestVerification(node);
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
        batches.write(new WriteEntry(node, value));
    }

    /**
     * Begin the single in-flight write: post it to the batch collector and remember its completion so the
     * device's acknowledgment can settle it.
     *
     * @param event the write request event carrying the value and its completion.
     */
    void beginWrite(final @NotNull TagAspectEvent.WriteRequested event) {
        // Defensive: the single-in-flight invariant means no completion should linger when a new write begins. If one
        // somehow does, abort it rather than leak it (a leaked completion would strand a back-pressuring producer).
        if (inFlightCompletion != null) {
            settleInFlight(SouthboundWriteOutcome.ABORTED);
        }
        requestWrite(event.value());
        inFlightCompletion = event.completion();
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
        if (!success) {
            recordFailure(reason != null ? reason : "write failed");
        }
        settleInFlight(success ? SouthboundWriteOutcome.SUCCEEDED : SouthboundWriteOutcome.FAILED);
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
                writeRequested.completion().settle(SouthboundWriteOutcome.REJECTED_BUSY);
                return;
            }
            // A write arriving while the aspect cannot write (deactivated, waiting for the adapter, verifying, or
            // permanently failed) is not a window violation: settle it ABORTED so the sender keeps the command
            // queued for redelivery — never a silent drop, never a leaked completion.
            log.debug(
                    "Write aspect of tag '{}' on adapter '{}' aborted a southbound write arriving in {}",
                    tag.name(),
                    adapterId,
                    machine.state());
            writeRequested.completion().settle(SouthboundWriteOutcome.ABORTED);
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
     * Settle the in-flight write's completion exactly once, then clear it. A no-op when nothing is in flight.
     *
     * @param outcome the terminal outcome to report.
     */
    private void settleInFlight(final @NotNull SouthboundWriteOutcome outcome) {
        final SouthboundWriteCompletion completion = inFlightCompletion;
        if (completion != null) {
            inFlightCompletion = null;
            completion.settle(outcome);
        }
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
        if (machine.state() != before) {
            lastTransitionAtMillis = clock.nowMillis();
        }
    }

    private void moveTo(final @NotNull TagAspectState next) {
        if (machine.state() != next) {
            machine.transitionTo(next);
            lastTransitionAtMillis = clock.nowMillis();
        }
    }

    private void scheduleTimer(final long delayMillis, final @NotNull Runnable onFire) {
        cancelActiveTimer();
        activeTimer = timers.schedule(clock.nowMillis() + delayMillis, onFire);
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
