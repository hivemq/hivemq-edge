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

import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.CONNECTED;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.ERROR;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_CONNECTED;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_CONNECTION_RETRY;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_DISCONNECTED;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_DISCONNECTED_BEFORE_RECONNECT;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_STARTED;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_STOPPED;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_VERIFICATION;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxSender;
import com.hivemq.adapter.sdk.api.v2.model.BrowseFilter;
import com.hivemq.adapter.sdk.api.v2.model.BrowseResultEntry;
import com.hivemq.adapter.sdk.api.v2.model.VerifyOutcome;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.protocols.v2.fsm.FSM;
import com.hivemq.protocols.v2.runtime.Backoff;
import com.hivemq.protocols.v2.runtime.BatchCollector;
import com.hivemq.protocols.v2.runtime.Clock;
import com.hivemq.protocols.v2.runtime.PriorityTimerQueue;
import com.hivemq.protocols.v2.runtime.ProtocolAdapterMetrics;
import com.hivemq.protocols.v2.runtime.RetryPolicy;
import com.hivemq.protocols.v2.runtime.TimerHandle;
import com.hivemq.protocols.v2.tag.TagAspectCoordinator;
import com.hivemq.protocols.v2.view.AdapterStatusSnapshot;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The collaborators and machinery the adapter machine acts through — the bag named in the design's
 * package layout. It owns the goal, the timer queue, the batch collector, the backoff, the verification gate, and
 * the centralized {@code stepTowardGoal}; the transition table's guards and actions reach the protocol adapter,
 * the timers, and the tag plane only through here. Everything runs on the wrapper's single dispatch thread, so it
 * holds no locks.
 * <p>
 * The {@link TagAspectCoordinator} (in the {@code tag} package) is the wrapper's view of the tag aspect machines.
 * The <b>verification gate</b> is owned by the shared verification authority behind that coordinator:
 * on entry to {@code WAITING_FOR_VERIFICATION} the coordinator verifies the nodes its active aspects need as one
 * batch, and the wrapper synthesizes {@link ProtocolAdapterWrapperEvent.AllVerified} once the coordinator reports
 * {@code allReported()} — every node in the batch has reported some outcome (failures do not block
 * {@code CONNECTED}). The single verify stream feeds both this gate and the per-tag aspects.
 * <p>
 * The machine is bound after construction through {@link #bindMachine(FSM)} because the machine's
 * constructor needs this context — the cycle is closed once, before any message is handled. {@code stepTowardGoal}
 * and the timer callbacks advance the current state through the bound machine.
 */
public final class ProtocolAdapterWrapperContext {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ProtocolAdapterWrapperContext.class);

    /**
     * The actor-side browse deadline. The REST request timeout is the primary service-level bound
     * (it returns {@code 504}); this deadline is the backstop that guarantees the future always completes and the
     * single in-flight browse slot is released even when the protocol adapter never reports a result.
     */
    private static final long BROWSE_DEADLINE_MILLIS = 60_000L;

    private final @NotNull String adapterId;
    private final @NotNull ProtocolAdapter protocolAdapter;
    private final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> selfSender;
    private final @NotNull Clock clock;
    private final @NotNull PriorityTimerQueue timers = new PriorityTimerQueue();
    private final @NotNull BatchCollector batches = new BatchCollector();
    private final @NotNull Backoff backoff;
    private final long watchdogTimeoutMillis;
    private final boolean skipVerification;
    private final @NotNull TagAspectCoordinator tagPlane;
    private final @NotNull ProtocolAdapterWrapperEventListener healthListener;
    private final @NotNull ProtocolAdapterMetrics metrics;

    private @NotNull ProtocolAdapterGoalState goal;
    private @NotNull Map<String, TagAspectActivationPreference> activation;
    private long lastTransitionAtMillis;
    private @Nullable String lastErrorReason;

    private @Nullable TimerHandle watchdog;
    private @Nullable TimerHandle backoffTimer;
    private @Nullable PendingBrowse pendingBrowse;
    private @Nullable FSM<ProtocolAdapterWrapperState, ProtocolAdapterWrapperEvent, ProtocolAdapterWrapperContext>
            machine;

    /**
     * @param adapterId             the adapter instance id.
     * @param protocolAdapter       the pure-mechanism adapter the machine commands.
     * @param selfSender            the wrapper's own mailbox sender — used to synthesize
     *                              {@link ProtocolAdapterWrapperEvent.AllVerified}.
     * @param clock                 the clock the timers are scheduled against.
     * @param retryPolicy           the connection backoff policy.
     * @param watchdogTimeoutMillis the per-state acknowledgment watchdog timeout, in milliseconds.
     * @param skipVerification      whether to skip verification and reach {@code CONNECTED} straight from
     *                              {@code WAITING_FOR_CONNECTED}.
     * @param initialGoal           the initial direction goal (from the configuration).
     * @param activation            the per-tag activation preferences.
     * @param tagPlane              the wrapper's view of the tag aspect machines.
     * @param healthListener        the supervisor notification seam.
     * @param metrics               the per-adapter metrics.
     */
    public ProtocolAdapterWrapperContext(
            final @NotNull String adapterId,
            final @NotNull ProtocolAdapter protocolAdapter,
            final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> selfSender,
            final @NotNull Clock clock,
            final @NotNull RetryPolicy retryPolicy,
            final long watchdogTimeoutMillis,
            final boolean skipVerification,
            final @NotNull ProtocolAdapterGoalState initialGoal,
            final @NotNull Map<String, TagAspectActivationPreference> activation,
            final @NotNull TagAspectCoordinator tagPlane,
            final @NotNull ProtocolAdapterWrapperEventListener healthListener,
            final @NotNull ProtocolAdapterMetrics metrics) {
        this.adapterId = adapterId;
        this.protocolAdapter = protocolAdapter;
        this.selfSender = selfSender;
        this.clock = clock;
        this.backoff = new Backoff(retryPolicy);
        this.watchdogTimeoutMillis = watchdogTimeoutMillis;
        this.skipVerification = skipVerification;
        this.goal = initialGoal;
        this.activation = Map.copyOf(activation);
        this.tagPlane = tagPlane;
        this.healthListener = healthListener;
        this.metrics = metrics;
    }

    /**
     * Close the construction cycle: the machine's constructor needs this context, so the machine is bound back
     * here once, before any message is handled.
     *
     * @param machine the adapter machine this context drives.
     */
    public void bindMachine(
            final @NotNull FSM<ProtocolAdapterWrapperState, ProtocolAdapterWrapperEvent, ProtocolAdapterWrapperContext>
                            machine) {
        this.machine = machine;
    }

    private @NotNull FSM<ProtocolAdapterWrapperState, ProtocolAdapterWrapperEvent, ProtocolAdapterWrapperContext>
            machine() {
        return Objects.requireNonNull(machine, "the adapter machine must be bound before the context is used");
    }

    // ──────────────────────────────────────────────────────────────────────────────────────────────────────────
    //  Goal seeking — the single piece of non-trivial logic
    // ──────────────────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Take the one step the current state allows toward the goal. Called after every message: on a
     * goal command it issues the next command immediately; on an event it catches up when an acknowledgment landed
     * in a resting state whose goal had changed while the machine was waiting. In a state that is awaiting an
     * acknowledgment it does nothing — the goal is consulted when that acknowledgment arrives, through the table.
     */
    public void stepTowardGoal() {
        switch (machine().state()) {
            case STOPPED -> {
                if (goal.wantConnected()) {
                    machine().transitionTo(startStep());
                }
            }
            case CONNECTED, WAITING_FOR_CONNECTED, WAITING_FOR_VERIFICATION -> {
                if (!goal.wantConnected()) {
                    machine().transitionTo(disconnectToStopStep());
                }
            }
            case WAITING_FOR_CONNECTION_RETRY -> {
                if (!goal.wantConnected()) {
                    machine().transitionTo(stopFromRetryStep());
                }
            }
            case ERROR -> {
                if (!goal.wantConnected()) {
                    machine().transitionTo(stopFromErrorStep());
                }
            }
            default -> {
                // WAITING_FOR_STARTED / WAITING_FOR_DISCONNECTED / WAITING_FOR_DISCONNECTED_BEFORE_RECONNECT /
                // WAITING_FOR_STOPPED: awaiting an acknowledgment; the goal is consulted when it lands.
            }
        }
    }

    /**
     * Apply a goal or lifecycle command. Mutates the goal and tag-set state; the subsequent
     * {@link #stepTowardGoal()} issues any command the new goal calls for. Never consults the transition table, so
     * a command is valid in every state and can never trigger a defensive reset.
     *
     * @param command the command to apply.
     */
    public void applyCommand(final @NotNull ProtocolAdapterWrapperCommand command) {
        switch (command) {
            case ProtocolAdapterWrapperCommand.ActivateDirection activate -> {
                goal = goal.withActivated(activate.direction());
                tagPlane.applyActivation(goal, activation);
            }
            case ProtocolAdapterWrapperCommand.DeactivateDirection deactivate -> {
                goal = goal.withDeactivated(deactivate.direction());
                tagPlane.applyActivation(goal, activation);
            }
            case ProtocolAdapterWrapperCommand.StopAdapter ignored -> {
                goal = ProtocolAdapterGoalState.stopped();
                tagPlane.applyActivation(goal, activation);
            }
            case ProtocolAdapterWrapperCommand.UpdateTagSet update -> {
                activation = Map.copyOf(update.activation());
                tagPlane.updateTagSet(
                        update.nodes(), update.activation(), update.readUsedTagNames(), update.writeUsedTagNames());
            }
            case ProtocolAdapterWrapperCommand.ApplyActivation apply -> {
                goal = apply.adapterDirections();
                activation = Map.copyOf(apply.tagActivation());
                tagPlane.applyActivation(goal, activation);
            }
            case ProtocolAdapterWrapperCommand.RetryTag retry -> tagPlane.retryTag(retry.tagName());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────────────────────────────────
    //  FSMTransition helpers — each issues the protocol-adapter command, manages timers, and returns the next state
    // ──────────────────────────────────────────────────────────────────────────────────────────────────────────

    @NotNull
    ProtocolAdapterWrapperState startStep() {
        clearTimers();
        protocolAdapter.start();
        armWatchdog();
        return WAITING_FOR_STARTED;
    }

    @NotNull
    ProtocolAdapterWrapperState connectStep() {
        clearTimers();
        protocolAdapter.connect();
        armWatchdog();
        return WAITING_FOR_CONNECTED;
    }

    @NotNull
    ProtocolAdapterWrapperState verifyStep() {
        clearTimers();
        // Move active aspects into verification and issue the nodes they need as one verifyBatch through the
        // shared verification authority.
        tagPlane.onAdapterVerifying();
        if (tagPlane.allReported()) {
            // No aspect needs verification — connect straight away.
            synthesizeAllVerified();
        } else {
            armWatchdog();
        }
        return WAITING_FOR_VERIFICATION;
    }

    @NotNull
    ProtocolAdapterWrapperState connectedStep() {
        clearTimers();
        clearVerification();
        backoff.reset();
        lastErrorReason = null;
        tagPlane.onAdapterReady();
        healthListener.wrapperStarted(adapterId);
        return CONNECTED;
    }

    @NotNull
    ProtocolAdapterWrapperState connectionRetryStep() {
        clearVerification();
        return enterConnectionRetry();
    }

    @NotNull
    ProtocolAdapterWrapperState connectionRetryFromConnectedStep() {
        tagPlane.onAdapterUnavailable();
        failPendingBrowse();
        return enterConnectionRetry();
    }

    @NotNull
    ProtocolAdapterWrapperState disconnectBeforeReconnectStep() {
        clearTimers();
        clearVerification();
        protocolAdapter.disconnect();
        armWatchdog();
        return WAITING_FOR_DISCONNECTED_BEFORE_RECONNECT;
    }

    @NotNull
    ProtocolAdapterWrapperState disconnectBeforeReconnectFromConnectedStep() {
        tagPlane.onAdapterUnavailable();
        failPendingBrowse();
        return disconnectBeforeReconnectStep();
    }

    @NotNull
    ProtocolAdapterWrapperState disconnectToStopStep() {
        clearTimers();
        clearVerification();
        tagPlane.onAdapterUnavailable();
        failPendingBrowse();
        protocolAdapter.disconnect();
        armWatchdog();
        return WAITING_FOR_DISCONNECTED;
    }

    @NotNull
    ProtocolAdapterWrapperState stopStep() {
        clearTimers();
        protocolAdapter.stop();
        armWatchdog();
        return WAITING_FOR_STOPPED;
    }

    @NotNull
    ProtocolAdapterWrapperState stopFromRetryStep() {
        clearTimers();
        protocolAdapter.stop();
        armWatchdog();
        return WAITING_FOR_STOPPED;
    }

    @NotNull
    ProtocolAdapterWrapperState stopFromErrorStep() {
        clearTimers();
        protocolAdapter.stop();
        armWatchdog();
        return WAITING_FOR_STOPPED;
    }

    @NotNull
    ProtocolAdapterWrapperState stoppedStep() {
        clearTimers();
        clearVerification();
        lastErrorReason = null;
        tagPlane.onAdapterUnavailable();
        healthListener.wrapperStopped(adapterId);
        return ProtocolAdapterWrapperState.STOPPED;
    }

    @NotNull
    ProtocolAdapterWrapperState adapterErrorStep(final @NotNull String reason) {
        return enterError(reason, false);
    }

    @NotNull
    ProtocolAdapterWrapperState watchdogErrorStep(final @NotNull ProtocolAdapterWrapperState current) {
        return enterError("watchdog timeout while in " + current, true);
    }

    /**
     * The mandatory {@code unmatched} action: an event with no listed transition means the wrapper and adapter are
     * no longer consistent. Log it, count it, stop best-effort, notify the supervisor, and enter
     * {@code ERROR} — where the resulting acknowledgments are absorbed so the reset fires at most once.
     */
    @NotNull
    ProtocolAdapterWrapperState defensiveReset(
            final @NotNull ProtocolAdapterWrapperState current, final @NotNull ProtocolAdapterWrapperEvent event) {
        metrics.incrementDefensiveReset();
        final String reason = "unexpected event " + event.getClass().getSimpleName() + " while in " + current;
        log.warn("Defensive reset of adapter '{}': {}", adapterId, reason);
        return enterError(reason, true);
    }

    /**
     * An {@code ERROR} absorb row: log at debug, stay in {@code ERROR}, take no action.
     */
    @NotNull
    ProtocolAdapterWrapperState absorbInError(final @NotNull ProtocolAdapterWrapperEvent event) {
        log.debug(
                "Adapter '{}' in ERROR absorbed {}", adapterId, event.getClass().getSimpleName());
        return ERROR;
    }

    private @NotNull ProtocolAdapterWrapperState enterError(final @NotNull String reason, final boolean issueStop) {
        clearTimers();
        clearVerification();
        if (issueStop) {
            protocolAdapter.stop();
        }
        lastErrorReason = reason;
        tagPlane.onAdapterUnavailable();
        failPendingBrowse();
        healthListener.wrapperError(adapterId, reason);
        return ERROR;
    }

    private @NotNull ProtocolAdapterWrapperState enterConnectionRetry() {
        clearTimers();
        if (backoff.exhausted()) {
            log.warn("Adapter '{}' exhausted its connection retries", adapterId);
            return enterError("maximum connection retries exceeded", false);
        }
        final long delay = backoff.nextDelayMillis();
        backoffTimer = timers.schedule(
                clock.nowMillis() + delay, () -> machine().onEvent(new ProtocolAdapterWrapperEvent.BackoffFired()));
        return WAITING_FOR_CONNECTION_RETRY;
    }

    // ──────────────────────────────────────────────────────────────────────────────────────────────────────────
    //  Verification gate (owned by the shared verification authority)
    // ──────────────────────────────────────────────────────────────────────────────────────────────────────────

    private void clearVerification() {
        tagPlane.resetVerificationGate();
    }

    /**
     * Handle one node's verification outcome: fan it out to the node's aspects through the shared
     * verification authority — which also clears the connect-gate count — then, while the machine is still gating
     * in {@code WAITING_FOR_VERIFICATION}, synthesize {@link ProtocolAdapterWrapperEvent.AllVerified} once every
     * node in the batch has reported some outcome. The single verify stream feeds both consumers (the adapter gate
     * and the per-tag aspects), on the one dispatch thread.
     *
     * @param node    the verified node.
     * @param outcome the verification outcome.
     */
    public void onVerifyResultReceived(final @NotNull Node node, final @NotNull VerifyOutcome outcome) {
        tagPlane.routeVerifyResult(node, outcome);
        if (machine().state() == WAITING_FOR_VERIFICATION && tagPlane.allReported()) {
            synthesizeAllVerified();
        }
    }

    /**
     * Route one value to its read aspect.
     *
     * @param node  the node the value belongs to.
     * @param value the reused v1 value.
     */
    public void routeDataPointToTags(final @NotNull Node node, final @NotNull DataPoint value) {
        tagPlane.routeDataPoint(node, value);
    }

    /**
     * Route a per-node failure to its read aspect.
     *
     * @param node        the node the failure belongs to.
     * @param reason      a human-readable description.
     * @param spontaneous whether the failure arrived outside a command-response exchange.
     */
    public void routeNodeErrorToTags(
            final @NotNull Node node, final @NotNull String reason, final boolean spontaneous) {
        tagPlane.routeNodeError(node, reason, spontaneous);
    }

    /**
     * Route a write acknowledgment to its write aspect.
     *
     * @param node    the node the write targeted.
     * @param success whether the write succeeded.
     * @param reason  the failure reason, or {@code null} on success.
     */
    public void routeWriteResultToTags(final @NotNull Node node, final boolean success, final @Nullable String reason) {
        tagPlane.routeWriteResult(node, success, reason);
    }

    /**
     * Route a southbound write request to its write aspect — the "write arrives" trigger.
     *
     * @param node  the node to write to.
     * @param value the reused v1 value to write.
     */
    public void routeWriteRequestToTags(final @NotNull Node node, final @NotNull DataPoint value) {
        tagPlane.submitWrite(node, value);
    }

    private void synthesizeAllVerified() {
        selfSender.tell(new ProtocolAdapterWrapperEvent.AllVerified());
    }

    // ──────────────────────────────────────────────────────────────────────────────────────────────────────────
    //  Browse bridge — one in-flight browse per adapter, completed from the result or the deadline
    // ──────────────────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Bridge a REST browse to the protocol adapter, on the dispatch thread. A browse runs only when
     * the adapter is {@code CONNECTED} and no browse is already in flight; otherwise the future is failed with a
     * {@link BrowseRejectedException} the resource maps to {@code 409}. On acceptance, one {@code browse(filter)} is
     * issued, the future is stashed, and a deadline timer is armed so the slot is always released — the matching
     * {@link ProtocolAdapterWrapperEvent.BrowseResultReceived} completes it through {@link #completeBrowse(List)}.
     *
     * @param filter     the browse filter.
     * @param completion the future the REST thread awaits.
     */
    public void handleBrowseRequest(
            final @NotNull BrowseFilter filter, final @NotNull CompletableFuture<List<BrowseResultEntry>> completion) {
        if (machine().state() != CONNECTED) {
            completion.completeExceptionally(new BrowseRejectedException(
                    BrowseRejectedException.Reason.NOT_CONNECTED,
                    "adapter '" + adapterId + "' is not connected (" + machine().state() + ")"));
            return;
        }
        if (pendingBrowse != null) {
            completion.completeExceptionally(new BrowseRejectedException(
                    BrowseRejectedException.Reason.ALREADY_IN_FLIGHT,
                    "a browse is already in flight on adapter '" + adapterId + "'"));
            return;
        }
        final TimerHandle deadline =
                timers.schedule(clock.nowMillis() + BROWSE_DEADLINE_MILLIS, this::onBrowseDeadline);
        pendingBrowse = new PendingBrowse(completion, deadline);
        protocolAdapter.browse(filter);
    }

    /**
     * Complete the pending browse with its results. A result arriving with nothing waiting — a stale
     * or duplicate {@code browseResult} — is dropped.
     *
     * @param entries the browse result entries.
     */
    public void completeBrowse(final @NotNull List<BrowseResultEntry> entries) {
        final PendingBrowse pending = pendingBrowse;
        if (pending == null) {
            return;
        }
        pendingBrowse = null;
        timers.cancel(pending.deadline());
        pending.completion().complete(List.copyOf(entries));
    }

    private void onBrowseDeadline() {
        final PendingBrowse pending = pendingBrowse;
        if (pending == null) {
            return;
        }
        pendingBrowse = null;
        pending.completion()
                .completeExceptionally(new BrowseRejectedException(
                        BrowseRejectedException.Reason.TIMED_OUT,
                        "browse on adapter '" + adapterId + "' did not complete before the deadline"));
    }

    private void failPendingBrowse() {
        final PendingBrowse pending = pendingBrowse;
        if (pending == null) {
            return;
        }
        pendingBrowse = null;
        timers.cancel(pending.deadline());
        pending.completion()
                .completeExceptionally(new BrowseRejectedException(
                        BrowseRejectedException.Reason.NOT_CONNECTED,
                        "adapter '" + adapterId + "' lost its connection before the browse completed"));
    }

    // ──────────────────────────────────────────────────────────────────────────────────────────────────────────
    //  Timers
    // ──────────────────────────────────────────────────────────────────────────────────────────────────────────

    private void armWatchdog() {
        cancelWatchdog();
        watchdog = timers.schedule(clock.nowMillis() + watchdogTimeoutMillis, () -> machine()
                .onEvent(new ProtocolAdapterWrapperEvent.WatchdogFired()));
    }

    private void cancelWatchdog() {
        if (watchdog != null) {
            timers.cancel(watchdog);
            watchdog = null;
        }
    }

    private void cancelBackoff() {
        if (backoffTimer != null) {
            timers.cancel(backoffTimer);
            backoffTimer = null;
        }
    }

    private void clearTimers() {
        cancelWatchdog();
        cancelBackoff();
    }

    // ──────────────────────────────────────────────────────────────────────────────────────────────────────────
    //  Tick handling, snapshot, and guards used by the transition table
    // ──────────────────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Handle a tick on the dispatch thread: record the tick lag, fire every due timer — the
     * adapter machine's watchdog and backoff, and every aspect's poll, verification-retry, and subscription-retry
     * timers, each feeding an event straight to its machine and posting any due work — then dispatch the pending
     * batches to the adapter.
     *
     * @param tickMillis the tick's logical time, in milliseconds.
     */
    public void onTick(final long tickMillis) {
        metrics.recordTickLag(clock.nowMillis() - tickMillis);
        timers.fireDue(tickMillis);
        batches.dispatch(protocolAdapter);
    }

    /**
     * Record that the machine transitioned: stamp the transition time from the clock and count it. Called once per message that changed the state.
     */
    public void recordTransition() {
        lastTransitionAtMillis = clock.nowMillis();
        metrics.incrementStateTransition();
    }

    /**
     * Build the immutable status snapshot for the given machine state.
     *
     * @param state the current machine state.
     * @return the snapshot to publish.
     */
    public @NotNull AdapterStatusSnapshot buildSnapshot(final @NotNull ProtocolAdapterWrapperState state) {
        return new AdapterStatusSnapshot(
                adapterId,
                state,
                goal.northboundActivated(),
                goal.southboundActivated(),
                tagPlane.tagSnapshots(),
                lastTransitionAtMillis,
                lastErrorReason);
    }

    /**
     * @return {@code true} when the goal wants the adapter connected — a guard for the {@code Started} transition.
     */
    public boolean goalWantsConnected() {
        return goal.wantConnected();
    }

    /**
     * @return whether verification is skipped — a guard for the {@code Connected} transition.
     */
    public boolean skipVerification() {
        return skipVerification;
    }

    // ──────────────────────────────────────────────────────────────────────────────────────────────────────────
    //  Runtime accessors — the per-actor primitives shared with the tag plane
    // ──────────────────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * @return the actor clock the timers are scheduled against.
     */
    public @NotNull Clock clock() {
        return clock;
    }

    /**
     * @return the actor's single timer queue — shared by the adapter machine and the tag aspects.
     */
    public @NotNull PriorityTimerQueue timers() {
        return timers;
    }

    /**
     * @return the actor's batch collector — where the tag aspects post poll and subscription requests.
     */
    public @NotNull BatchCollector batches() {
        return batches;
    }

    /**
     * @return the per-adapter metrics — the source of the per-tag failure counters.
     */
    public @NotNull ProtocolAdapterMetrics metrics() {
        return metrics;
    }

    /**
     * @return the protocol adapter the machine commands — the verify seam the tag plane re-verifies through.
     */
    public @NotNull ProtocolAdapter protocolAdapter() {
        return protocolAdapter;
    }

    /**
     * The single in-flight browse: the future the REST thread awaits and the deadline timer that releases the slot
     * if the protocol adapter never reports a result.
     *
     * @param completion the future to complete with the browse result or a {@link BrowseRejectedException}.
     * @param deadline   the deadline timer, canceled when the browse completes.
     */
    private record PendingBrowse(
            @NotNull CompletableFuture<List<BrowseResultEntry>> completion,
            @NotNull TimerHandle deadline) {}
}
