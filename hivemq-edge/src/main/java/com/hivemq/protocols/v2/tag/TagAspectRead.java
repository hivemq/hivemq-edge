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
 * The read half of a tag's behavior — one of the two independent aspects every tag has. A
 * read aspect is a {@link FSM} over an {@link TagAspectState} enum: <b>polled</b> (poll-interval cadence) when the
 * tag is not subscribable, <b>subscribed</b> (push) when it is. Both share the five pre-operating states; their
 * tables are built by {@link TagAspectReadTransitions}, the shared rows by one builder.
 * <p>
 * The aspect lives inside the wrapper actor and runs only on its single dispatch thread. It owns no thread: it
 * requests work by appending to the shared {@link BatchCollector}, schedules its own poll / verification-retry /
 * subscription-retry timers on the actor's single {@link PriorityTimerQueue}, observes the events
 * the wrapper routes to it, and re-verifies through the shared {@link SharedNodeVerification}.
 * <p>
 * Two kinds of input drive it, mirroring the adapter machine:
 * <ul>
 * <li><b>events</b> ({@link TagAspectEvent}) run through the transition table — verification outcomes, values,
 * per-node failures, and the aspect's own timer expiries;</li>
 * <li><b>goal and adapter-readiness changes</b> bypass the table (like the adapter machine's goal commands): the
 * three-condition goal ({@link TagAspectGoal}) and the {@code DEACTIVATED} ↔ operating coupling to the adapter's
 * connection are applied directly, never through the table, so they can never trigger a defensive transition.</li>
 * </ul>
 */
public final class TagAspectRead implements TagAspectVerifying {

    private static final @NotNull Logger log = LoggerFactory.getLogger(TagAspectRead.class);

    /**
     * The failure count past which a tag's failures are logged at {@code ERROR} rather than {@code WARN} — a few
     * hiccups are routine, sustained failures are not.
     */
    private static final int SUSTAINED_FAILURE_THRESHOLD = 5;

    /**
     * Consecutive poll failures (missing results or poll-time node errors) after which the aspect escalates through
     * re-verification instead of silently riding the cadence (EDG-824 #15): if the device still answers, the tag
     * resumes; if the adapter is mute, the aspect parks in verification — active-but-not-operating — and the coarse
     * {@code TagStatus} folds to {@code ERROR} instead of a healthy-looking {@code NORTHBOUND_ONLY}.
     */
    private static final int POLL_FAILURE_ESCALATION_THRESHOLD = 3;

    /**
     * How long a <b>polled</b> aspect may go without a single published reading before it is declared stale
     * (EDG-824 #15, Sam round 2 finding 5).
     * <p>
     * The escalation above answers "is the device still there?" and clears on a successful re-verification. That is
     * the wrong question for an operator: a device that answers cheap verification forever while every poll stalls
     * passes it every round, so the tag returned to a producing-looking {@code NORTHBOUND_ONLY} within a second of
     * each escalation and spent essentially all of its observable life reading healthy — having never published a
     * value. This deadline asks the question that matters instead — <i>has a reading actually arrived?</i> — and
     * only a published reading answers it.
     * <p>
     * Deliberately much longer than one escalation round (~45 s on defaults) so it is a verdict, not a retry: the
     * escalation stays the fast "re-verify and try again", this is the slow "this tag is not readable". Hardcoded for
     * now; a per-adapter configuration knob is the natural follow-up.
     */
    private static final long STALE_AFTER_NO_VALUE_MILLIS = 5 * 60 * 1000L;

    /**
     * The two read-aspect variants — which transition table and operating cycle the aspect runs.
     */
    private enum Variant {
        POLLED,
        SUBSCRIBED
    }

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
    private final @NotNull Variant variant;

    private final @NotNull Clock clock;
    private final @NotNull PriorityTimerQueue timers;
    private final @NotNull BatchCollector batches;
    private final @NotNull ProtocolAdapterMetrics metrics;
    private final @NotNull SharedNodeVerification sharedNodeVerification;
    private final long pollIntervalMillis;
    private final long pollResultTimeoutMillis;
    private final @NotNull Backoff verificationRetryBackoff;
    private final @NotNull Backoff subscriptionRetryBackoff;

    // The variant's shared pre-operating constants and the state operation begins in (poll interval / subscribe).
    private final @NotNull TagAspectState deactivated;
    private final @NotNull TagAspectState waitingForAdapterReady;
    private final @NotNull TagAspectState waitingForVerification;
    private final @NotNull TagAspectState verifiedEntry;

    private final @NotNull FSM<TagAspectState, TagAspectEvent, TagAspectRead> machine;

    private @NotNull TagAspectGoal goal = TagAspectGoal.inactive();
    private @NotNull AdapterPhase adapterPhase = AdapterPhase.DISCONNECTED;
    private int consecutivePollFailures;
    private int failureCount;
    private @Nullable String lastFailureReason;
    private long lastTransitionAtMillis;
    private @Nullable TimerHandle activeTimer;

    /**
     * When the staleness deadline started counting: the clock time of the last published reading, or of the moment
     * the aspect began operating if it has not published one yet. {@code -1} means the deadline is not running —
     * the aspect is deactivated or the adapter is down, and a tag cannot be blamed for producing nothing then.
     * <p>
     * Crucially this is NOT reset by a successful verification: that is exactly what let a verify-answering,
     * poll-stalling device look healthy forever.
     */
    private long producingSinceMillis = -1;

    /** Whether the aspect has passed {@link #STALE_AFTER_NO_VALUE_MILLIS} without publishing a reading. */
    private boolean stale;

    /**
     * @param adapterId               the owning adapter's id.
     * @param node                    the protocol-specific node.
     * @param tag                     Edge's half of the pair; its {@code subscribable} flag selects the variant.
     * @param clock                   the actor clock the timers are scheduled against.
     * @param timers                  the actor's single timer queue.
     * @param batches                 the actor's batch collector — where poll / subscription requests are posted.
     * @param metrics                 the per-adapter metrics (per-tag failure counters).
     * @param sharedNodeVerification the shared verification authority for re-verifications.
     * @param pollIntervalMillis      the poll cadence for a polled aspect, in milliseconds.
     * @param pollResultTimeoutMillis the deadline for a requested poll's result — the adapter's command timeout; a
     *                                poll that never answers is failed instead of waiting forever (EDG-824 #15).
     * @param retryPolicy             the backoff policy for verification and subscription retries.
     */
    public TagAspectRead(
            final @NotNull String adapterId,
            final @NotNull Node node,
            final @NotNull Tag tag,
            final @NotNull Clock clock,
            final @NotNull PriorityTimerQueue timers,
            final @NotNull BatchCollector batches,
            final @NotNull ProtocolAdapterMetrics metrics,
            final @NotNull SharedNodeVerification sharedNodeVerification,
            final long pollIntervalMillis,
            final long pollResultTimeoutMillis,
            final @NotNull RetryPolicy retryPolicy) {
        this.adapterId = adapterId;
        this.node = node;
        this.tag = tag;
        this.variant = tag.subscribable() ? Variant.SUBSCRIBED : Variant.POLLED;
        this.clock = clock;
        this.timers = timers;
        this.batches = batches;
        this.metrics = metrics;
        this.sharedNodeVerification = sharedNodeVerification;
        this.pollIntervalMillis = pollIntervalMillis;
        this.pollResultTimeoutMillis = pollResultTimeoutMillis;
        this.verificationRetryBackoff = new Backoff(retryPolicy);
        this.subscriptionRetryBackoff = new Backoff(retryPolicy);
        if (variant == Variant.SUBSCRIBED) {
            this.deactivated = TagAspectReadSubscribedState.DEACTIVATED;
            this.waitingForAdapterReady = TagAspectReadSubscribedState.WAITING_FOR_ADAPTER_READY;
            this.waitingForVerification = TagAspectReadSubscribedState.WAITING_FOR_VERIFICATION;
            this.verifiedEntry = TagAspectReadSubscribedState.WAITING_FOR_SUBSCRIPTION;
            this.machine = new FSM<>(deactivated, TagAspectReadTransitions.subscribedTable(), this);
        } else {
            this.deactivated = TagAspectReadPolledState.DEACTIVATED;
            this.waitingForAdapterReady = TagAspectReadPolledState.WAITING_FOR_ADAPTER_READY;
            this.waitingForVerification = TagAspectReadPolledState.WAITING_FOR_VERIFICATION;
            this.verifiedEntry = TagAspectReadPolledState.WAITING_FOR_POLL_INTERVAL;
            this.machine = new FSM<>(deactivated, TagAspectReadTransitions.polledTable(), this);
        }
    }

    // ── goal and adapter-readiness coupling (bypass the table) ───────────────────────────────

    /**
     * Apply a new aspect goal (the three-condition rule). When the goal becomes active the aspect
     * leaves {@code DEACTIVATED}; when it becomes inactive the aspect returns to {@code DEACTIVATED}, tearing down
     * any subscription and cancelling timers — never reconnecting the adapter.
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
            case DISCONNECTED -> moveTo(waitingForAdapterReady);
            case VERIFYING, READY -> {
                // Activated while the adapter is up: this node missed the connect-time gate verification, so ask
                // for a fresh one of its own — no reconnect.
                moveTo(waitingForVerification);
                requestVerification();
            }
        }
    }

    private void deactivate() {
        if (machine.state().isDeactivated()) {
            return;
        }
        if (variant == Variant.SUBSCRIBED && adapterPhase == AdapterPhase.READY && holdsSubscription()) {
            batches.removeSubscription(node);
        }
        cancelActiveTimer();
        suspendStaleness();
        moveTo(deactivated);
    }

    /**
     * The adapter began verifying: an active aspect waiting for the adapter moves into verification
     * and consumes the connect-time gate result the wrapper routes to it — it does not request its own.
     */
    public void onAdapterVerifying() {
        adapterPhase = AdapterPhase.VERIFYING;
        if (machine.state() == waitingForAdapterReady) {
            moveTo(waitingForVerification);
        }
    }

    /**
     * The adapter reached {@code CONNECTED}. When verification was skipped the aspect is still
     * waiting for the adapter — treat the connection as verified and begin operating; otherwise it has already
     * advanced through verification and nothing happens here.
     */
    public void onAdapterReady() {
        adapterPhase = AdapterPhase.READY;
        if (machine.state() == waitingForAdapterReady) {
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
            verificationRetryBackoff.reset();
            subscriptionRetryBackoff.reset();
            consecutivePollFailures = 0;
            suspendStaleness();
            moveTo(waitingForAdapterReady);
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
        suspendStaleness(); // an explicit operator retry starts the tag's whole record over, staleness included
        if (adapterPhase == AdapterPhase.READY) {
            moveTo(waitingForVerification);
            requestVerification();
        } else {
            moveTo(waitingForAdapterReady);
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
            case VerifyOutcome.Success ignored -> dispatch(new TagAspectEvent.VerifySucceeded());
            case VerifyOutcome.TransientFailure transientFailure ->
                dispatch(new TagAspectEvent.VerifyTransientlyFailed(transientFailure.reason()));
            case VerifyOutcome.PermanentFailure permanentFailure ->
                dispatch(new TagAspectEvent.VerifyPermanentlyFailed(permanentFailure.reason()));
        }
    }

    /**
     * Feed a received value — a poll response or a subscription push.
     *
     * @param value         the reused v1 value.
     * @param completesPoll whether this value also completes the node's poll (a single completing {@code dataPoint})
     *                      or leaves it open for more (a non-terminating {@code dataPoints} value); the subscribed
     *                      variant ignores it.
     * @return whether the value was expected in the current read-aspect state.
     */
    public boolean onValue(final @NotNull DataPoint value, final boolean completesPoll) {
        final TagAspectState state = machine.state();
        final boolean accepted = state == TagAspectReadPolledState.WAITING_FOR_POLL_DATAPOINT
                || state == TagAspectReadSubscribedState.WAITING_FOR_SUBSCRIPTION
                || state == TagAspectReadSubscribedState.SUBSCRIBED;
        dispatch(new TagAspectEvent.ValueReceived(value, completesPoll));
        return accepted;
    }

    /**
     * Feed a poll completion — the poll has produced all its values (possibly zero), so the poll cadence resumes.
     * Only the polled variant has a transition for it; anywhere else (a late completion after a poll failure already
     * ended the poll, or a completion reaching a subscribed aspect) it is harmlessly absorbed by the lenient
     * {@code unmatched} slot.
     */
    public void onPollComplete() {
        dispatch(new TagAspectEvent.PollCompleted());
    }

    /**
     * Feed a per-node failure.
     *
     * @param reason      a human-readable description.
     * @param spontaneous whether the failure arrived outside a command-response exchange.
     */
    public void onNodeError(final @NotNull String reason, final boolean spontaneous) {
        dispatch(new TagAspectEvent.NodeFailed(reason, spontaneous));
    }

    /**
     * Record a declared-schema conformance failure: counted and surfaced, no machine event — the
     * transport is alive, only the value was refused (EDG-824 #6).
     *
     * @param reason a human-readable description of the violation.
     */
    public void recordConformanceFailure(final @NotNull String reason) {
        recordFailure(reason);
    }

    // ── actions the transition table runs (package-private) ─────────────────────────────────────────────────────

    @Override
    public @NotNull TagAspectState enterVerified() {
        cancelActiveTimer(); // clear the verify-result deadline (V-DEADLINE); the poll branch re-arms below
        verificationRetryBackoff.reset();
        consecutivePollFailures = 0;
        if (variant == Variant.SUBSCRIBED) {
            batches.addSubscription(node);
        } else {
            // Start the staleness deadline the first time this aspect begins operating, and — deliberately — do NOT
            // restart it on a later re-verification. A device that answers verification but stalls every poll passes
            // through here once per escalation round; restarting the deadline here would reset it every ~45 s and it
            // could never trip, which is the whole failure mode (finding 5).
            if (producingSinceMillis < 0) {
                producingSinceMillis = clock.nowMillis();
            }
            scheduleNextPoll();
        }
        return verifiedEntry;
    }

    /**
     * A reading of this tag was accepted and published northbound. This is the only thing that satisfies the
     * staleness deadline — not a verification, and not a value the declared schema refused (that one is proof the
     * transport is alive, but the consumer still received nothing).
     */
    public void onValuePublished() {
        producingSinceMillis = clock.nowMillis();
        if (stale) {
            stale = false;
            log.info(
                    "Tag '{}' on adapter '{}' is readable again: a reading was published after the stale period",
                    tag.name(),
                    adapterId);
        }
    }

    /**
     * Stop the staleness deadline: the aspect is deactivated or its adapter is down, and producing nothing is the
     * correct behaviour. It restarts from zero when the aspect next begins operating, so a long outage does not make
     * every tag report stale the instant the adapter reconnects.
     */
    private void suspendStaleness() {
        producingSinceMillis = -1;
        stale = false;
    }

    /**
     * Trip the staleness verdict once the deadline has passed with nothing published. Evaluated at the top of every
     * poll request and on every poll failure — between them the cadence guarantees one evaluation within
     * {@code pollInterval + commandTimeout} of the deadline whatever the device does, so no second timer is added to
     * the aspect's single timer slot.
     * <p>
     * Both call sites are needed: the request covers the cycles that <i>succeed</i> and still publish nothing (a
     * refused value, or a completion with no values), the failure covers a stall that escalates into re-verification
     * and may not reach another request for a while.
     */
    /**
     * The deadline this aspect is actually judged against: five minutes, or one whole poll cycle when the configured
     * cadence is slower than that. A device polled every ten minutes cannot publish within five however healthy it
     * is, and judging it on the shorter figure would declare it unreadable during every single in-flight poll.
     */
    private long staleAfterMillis() {
        return Math.max(STALE_AFTER_NO_VALUE_MILLIS, pollIntervalMillis + pollResultTimeoutMillis);
    }

    private void evaluateStaleness() {
        if (stale || producingSinceMillis < 0) {
            return;
        }
        final long withoutValueMillis = clock.nowMillis() - producingSinceMillis;
        if (withoutValueMillis < staleAfterMillis()) {
            return;
        }
        stale = true;
        log.error(
                "Tag '{}' on adapter '{}' has published no reading for {} ms: the tag is not readable. "
                        + "The device is answering verification but not delivering poll results; last failure: {}",
                tag.name(),
                adapterId,
                withoutValueMillis,
                lastFailureReason);
    }

    void requestPoll() {
        // The deadline is evaluated here, at the top of each cycle, and not when a poll completes: the coordinator
        // records a published reading AFTER the completing value has already driven the machine, so evaluating at
        // completion would judge a cycle before its own publish was counted and trip a perfectly healthy tag one
        // interval in every five minutes. By the next request the previous cycle's outcome is settled, whatever it
        // was — a published reading, a value the schema refused, or a completion carrying no values at all
        // (Sam, round 3 finding 3).
        evaluateStaleness();
        batches.poll(node);
        // A poll that never answers must not read healthy forever (EDG-824 #15): arm the result deadline on the
        // aspect's single timer slot — a received value or failure replaces it with the next-poll timer.
        scheduleTimer(
                pollResultTimeoutMillis,
                () -> dispatch(new TagAspectEvent.NodeFailed(
                        "no poll result within " + pollResultTimeoutMillis + " ms", false)));
    }

    void onPollSucceeded() {
        consecutivePollFailures = 0;
        scheduleNextPoll();
    }

    /**
     * A value arrived outside the poll window — the answer to a poll already failed at its result deadline. The
     * value is discarded (the cadence has moved on) and it is NOT a published reading, so the missed publish already
     * counted by that poll-result timeout must STAND. A device that answers every poll just after the deadline
     * produces no data, and clearing the escalation counter here is exactly what let such a tag read healthy forever
     * while publishing nothing (EDG-824 #15); it must instead escalate like any stalled poll. Only an on-time value
     * ({@link #onPollSucceeded()}) clears the counter.
     */
    void onLateValueDiscarded() {
        // Intentionally leaves consecutivePollFailures untouched — see the contract above.
    }

    void scheduleNextPoll() {
        scheduleTimer(pollIntervalMillis, () -> dispatch(new TagAspectEvent.PollIntervalElapsed()));
    }

    void requestAddSubscription() {
        batches.addSubscription(node);
    }

    void confirmSubscription() {
        subscriptionRetryBackoff.reset();
    }

    @Override
    public void requestVerification() {
        sharedNodeVerification.requestVerification(node);
        armVerifyResultDeadline();
    }

    /**
     * Arm the verify-result deadline on the aspect's single timer slot. Without it, an adapter that accepts a
     * post-connect re-verification but never reports an outcome would park the aspect in WAITING_FOR_VERIFICATION
     * forever — silent, yet the adapter status stays CONNECTED/green (the connect-gate watchdog covers only the
     * connect-time gate). On expiry the outstanding verify is abandoned and a transient failure is raised so the
     * aspect retries on the verification backoff instead of hanging. This is the verify-path analogue of the
     * poll-result deadline (EDG-824 #15) and reuses the same adapter command timeout (QA finding V-DEADLINE).
     */
    private void armVerifyResultDeadline() {
        scheduleTimer(pollResultTimeoutMillis, () -> {
            sharedNodeVerification.abandonVerification(node);
            dispatch(new TagAspectEvent.VerifyTransientlyFailed(
                    "no verify result within " + pollResultTimeoutMillis + " ms"));
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
        cancelActiveTimer(); // clear the verify-result deadline (V-DEADLINE)
        recordFailure(reason);
    }

    /**
     * A poll failed — a poll-time node error or a missing result. The next scheduled poll is the retry; after
     * {@link #POLL_FAILURE_ESCALATION_THRESHOLD} consecutive failures the aspect escalates through re-verification
     * instead (EDG-824 #15), so a persistently-stalled poll surfaces at the coarse status level.
     *
     * @return the state the transition row moves to.
     */
    @NotNull
    TagAspectState onPollFailure(final @NotNull String reason) {
        recordFailure(reason);
        // The slow verdict, layered on the fast retry below: this one survives the re-verification the escalation
        // triggers, so it is what a device that verifies-but-never-answers eventually trips (finding 5).
        evaluateStaleness();
        consecutivePollFailures++;
        if (consecutivePollFailures >= POLL_FAILURE_ESCALATION_THRESHOLD) {
            consecutivePollFailures = 0;
            cancelActiveTimer();
            requestVerification();
            return waitingForVerification;
        }
        scheduleNextPoll();
        return TagAspectReadPolledState.WAITING_FOR_POLL_INTERVAL;
    }

    void onSubscriptionFailure(final @NotNull String reason) {
        recordFailure(reason);
        scheduleTimer(
                subscriptionRetryBackoff.nextDelayMillis(),
                () -> dispatch(new TagAspectEvent.SubscriptionRetryElapsed()));
    }

    void onSpontaneousSubscriptionLoss(final @NotNull String reason) {
        // The documented power cycle (EDG-824 #16): cancel the subscription FIRST, then re-verify, then re-subscribe
        // only after the verify succeeds. Without the explicit cancel the old subscription is never released — a
        // shadow-set-consistency deviation the adapter cannot repair on its own.
        //
        // The re-verify is posted through the BatchCollector rather than issued eagerly, so its verifyBatch
        // dispatches AFTER this tick's removeSubscriptionBatch: the adapter observes remove -> verify -> add, not
        // verify -> remove -> add. The in-flight registration keeps the dedup and the WAITING_FOR_VERIFICATION
        // gating; the outcome still flows back through SharedNodeVerification.onVerifyResult -> enterVerified,
        // which queues the re-subscribe. Costs one tick on this (rare, adapter-driven) path only.
        batches.removeSubscription(node);
        recordFailure(reason);
        if (sharedNodeVerification.beginDeferredVerification(node)) {
            batches.verify(node);
        }
        armVerifyResultDeadline(); // unconditional: a de-duplicated aspect still needs its own liveness deadline
    }

    void logUnexpectedEvent(final @NotNull TagAspectEvent event) {
        log.debug(
                "Read aspect of tag '{}' on adapter '{}' ignored unexpected {} in {}",
                tag.name(),
                adapterId,
                event.getClass().getSimpleName(),
                machine.state());
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
     * @return whether the aspect is operating at its goal (producing values), per {@link TagAspectState#isOperating()}
     *         — and, for a polled aspect, actually producing them: a tag that has published nothing for
     *         {@link #STALE_AFTER_NO_VALUE_MILLIS} is not operating however healthy its state machine looks, so the
     *         coarse {@code TagStatus} folds to {@code ERROR} until a reading arrives (finding 5). No new status
     *         value, and therefore no API change.
     */
    public boolean operating() {
        return machine.state().isOperating() && !stale;
    }

    /**
     * @return whether the aspect has gone {@link #STALE_AFTER_NO_VALUE_MILLIS} without publishing a reading.
     */
    public boolean stale() {
        return stale;
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
        return machine.state() == waitingForVerification;
    }

    /**
     * @return the cumulative failure count (poll / subscription / verification).
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

    private boolean holdsSubscription() {
        final TagAspectState current = machine.state();
        return current == TagAspectReadSubscribedState.SUBSCRIBED
                || current == TagAspectReadSubscribedState.WAITING_FOR_SUBSCRIPTION
                || current == TagAspectReadSubscribedState.WAITING_FOR_SUBSCRIPTION_RETRY;
    }

    private void scheduleTimer(final long delayMillis, final @NotNull Runnable onFire) {
        cancelActiveTimer();
        // Saturate: a near-Long.MAX_VALUE configured delay must mean "practically never", not an overflowed
        // negative deadline that fires immediately.
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
            log.debug("Read aspect of tag '{}' on adapter '{}' failed: {}", tag.name(), adapterId, reason);
        } else if (failureCount < SUSTAINED_FAILURE_THRESHOLD) {
            log.warn(
                    "Read aspect of tag '{}' on adapter '{}' failed ({} times): {}",
                    tag.name(),
                    adapterId,
                    failureCount,
                    reason);
        } else {
            log.error(
                    "Read aspect of tag '{}' on adapter '{}' has failed {} times: {}",
                    tag.name(),
                    adapterId,
                    failureCount,
                    reason);
        }
    }
}
