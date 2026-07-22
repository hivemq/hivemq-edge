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

import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.STOPPED;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_VERIFICATION;

import com.hivemq.adapter.sdk.api.v2.messaging.MessageHandler;
import com.hivemq.protocols.v2.fsm.FSM;
import com.hivemq.protocols.v2.view.AdapterStatusSnapshot;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;

/**
 * The Protocol Adapter Wrapper — a {@link MessageHandler} that owns the adapter machine and <b>all</b> policy
 *. Every input arrives as one {@link ProtocolAdapterWrapperMessage}:
 * <ul>
 * <li>a {@link ProtocolAdapterWrapperTick} fires the due timers and dispatches the pending batches;</li>
 * <li>a {@link ProtocolAdapterWrapperCommand} runs through the goal-command bypass (mutate the goal, then
 * {@code stepTowardGoal}), so it is valid in every state and never triggers a defensive reset;</li>
 * <li>a {@link ProtocolAdapterWrapperEvent} is routed: protocol-adapter lifecycle events and the synthesized
 * {@code AllVerified} drive the transition table, while verification, data, and write events are routed to the tag
 * plane and a browse result completes the pending browse future. In {@code ERROR} every event
 * is fed to the machine so the absorb rows can swallow it;</li>
 * <li>a {@link ProtocolAdapterWrapperWriteRequest} routes a southbound write to the node's write aspect — it changes no adapter goal or machine state;</li>
 * <li>a {@link ProtocolAdapterWrapperBrowseRequest} bridges a REST browse to the protocol adapter —
 * it changes no adapter goal or machine state.</li>
 * </ul>
 * After every message it publishes an immutable {@link AdapterStatusSnapshot} — the only state that
 * crosses the actor boundary outward. {@code receive} runs on the actor's single dispatch thread; the wrapper
 * holds no locks.
 */
public final class ProtocolAdapterWrapper implements MessageHandler<ProtocolAdapterWrapperMessage> {

    private final @NotNull ProtocolAdapterWrapperContext context;
    private final @NotNull AtomicReference<AdapterStatusSnapshot> snapshot;
    private final @NotNull FSM<ProtocolAdapterWrapperState, ProtocolAdapterWrapperEvent, ProtocolAdapterWrapperContext>
            machine;

    /**
     * @param context  the machinery and collaborators the machine acts through.
     * @param snapshot the reference the wrapper publishes its status into — created by the manager's handle in
     *                 production, by the test fixture in unit tests.
     */
    private static final @NotNull org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(ProtocolAdapterWrapper.class);

    public ProtocolAdapterWrapper(
            final @NotNull ProtocolAdapterWrapperContext context,
            final @NotNull AtomicReference<AdapterStatusSnapshot> snapshot) {
        this.context = context;
        this.snapshot = snapshot;
        this.machine = new FSM<>(STOPPED, ProtocolAdapterFSMTransitions.table(), context);
        context.bindMachine(machine);
        publishSnapshot();
    }

    @Override
    public void receive(final @NotNull ProtocolAdapterWrapperMessage message) {
        final ProtocolAdapterWrapperState before = machine.state();
        try {
            switch (message) {
                case ProtocolAdapterWrapperTick tick -> {
                    context.onTick(tick.nowMillis());
                    context.stepTowardGoal();
                }
                case ProtocolAdapterWrapperCommand command ->
                    machine.onGoalChange(() -> {
                        context.applyCommand(command);
                        context.stepTowardGoal();
                    });
                case ProtocolAdapterWrapperEvent event -> {
                    handleEvent(event);
                    context.stepTowardGoal();
                }
                case ProtocolAdapterWrapperWriteRequest write ->
                    // A southbound write: route it to the node's write aspect. It changes no adapter
                    // goal or machine state, so no stepTowardGoal — only the write aspect (and the snapshot) move.
                    context.routeWriteRequestToTags(write.node(), write.value());
                case ProtocolAdapterWrapperBrowseRequest browse ->
                    // A REST browse request: bridge it to the protocol adapter. It changes no adapter
                    // goal or machine state — it issues one browse() when CONNECTED and stashes the future.
                    context.handleBrowseRequest(browse.filter(), browse.completion());
            }
        } catch (final Throwable exception) {
            // The green-while-dead guard (EDG-824 #7): a contract-violating adapter throwing from a synchronous
            // adapter-facing call must not kill the dispatch loop and freeze the last (GREEN) snapshot — the
            // wrapper enters ERROR and keeps processing messages. Throwable, not RuntimeException: a mispackaged
            // adapter jar throws LinkageErrors, the very case this guard exists for. The handler itself is guarded
            // too: if the error path also throws (a half-mutated tag plane), the machine still lands in ERROR.
            try {
                machine.transitionTo(context.adapterThrowStep(machine.state(), exception));
            } catch (final Throwable secondary) {
                log.error("The error path of a v2 adapter wrapper failed too; forcing ERROR", secondary);
                machine.transitionTo(ProtocolAdapterWrapperState.ERROR);
            }
        }
        if (machine.state() != before) {
            context.recordTransition();
        }
        publishSnapshot();
    }

    /**
     * Route one event. In {@code ERROR} every event is fed to the machine so the named absorb rows
     * can swallow it. Otherwise lifecycle events and the gate signal drive the machine, while
     * verification, data, and write events are routed to the tag plane and a browse result completes the pending
     * browse future. The wrapper-level aspect-timer
     * events ({@code PollTimerFired} etc.) are never produced: each aspect schedules its own poll,
     * verification-retry, and subscription-retry timers on the actor's single timer queue and feeds its own
     * machine directly, so these remain only as part of the sealed hierarchy and its {@code ERROR}
     * absorb rows.
     */
    private void handleEvent(final @NotNull ProtocolAdapterWrapperEvent event) {
        // Browse events never drive the adapter machine: the wrapper's browse engine consumes them and
        // ignores any that are stale or arrive while no browse is in flight — in ERROR too.
        if (event instanceof final ProtocolAdapterWrapperEvent.BrowsePageReceived page) {
            context.onBrowsePage(page.requestId(), page.entries(), page.continuation());
            return;
        }
        if (event instanceof final ProtocolAdapterWrapperEvent.AttributesResolved resolved) {
            context.onAttributesResolved(resolved.requestId(), resolved.attributes());
            return;
        }
        if (event instanceof final ProtocolAdapterWrapperEvent.BrowseFailed failed) {
            context.onBrowseFailed(failed.requestId(), failed.reason());
            return;
        }
        if (machine.state() == ProtocolAdapterWrapperState.ERROR) {
            machine.onEvent(event);
            return;
        }
        switch (event) {
            case ProtocolAdapterWrapperEvent.Started ignored -> machine.onEvent(event);
            case ProtocolAdapterWrapperEvent.Stopped ignored -> machine.onEvent(event);
            case ProtocolAdapterWrapperEvent.Connected ignored -> machine.onEvent(event);
            case ProtocolAdapterWrapperEvent.Disconnected ignored -> machine.onEvent(event);
            case ProtocolAdapterWrapperEvent.ErrorEvent ignored -> machine.onEvent(event);
            case ProtocolAdapterWrapperEvent.WatchdogFired ignored -> machine.onEvent(event);
            case ProtocolAdapterWrapperEvent.BackoffFired ignored -> machine.onEvent(event);
            case ProtocolAdapterWrapperEvent.AllVerified ignored -> {
                if (machine.state() == WAITING_FOR_VERIFICATION) {
                    machine.onEvent(event);
                }
                // Otherwise the gate result is stale (verification was abandoned); ignore it.
            }
            case ProtocolAdapterWrapperEvent.VerifyResultReceived verify ->
                context.onVerifyResultReceived(verify.node(), verify.outcome());
            case ProtocolAdapterWrapperEvent.DataPointReceived data ->
                context.routeDataPointToTags(data.node(), data.value());
            case ProtocolAdapterWrapperEvent.NodeErrorReceived nodeError ->
                context.routeNodeErrorToTags(nodeError.node(), nodeError.reason(), nodeError.spontaneous());
            case ProtocolAdapterWrapperEvent.WriteResultReceived write ->
                context.routeWriteResultToTags(write.node(), write.success(), write.reason());
            case ProtocolAdapterWrapperEvent.PollTimerFired ignored -> {
                // Unused: aspects schedule and fire their own timers on the actor's single timer queue.
            }
            case ProtocolAdapterWrapperEvent.VerificationRetryTimerFired ignored -> {
                // Unused: aspects schedule and fire their own timers on the actor's single timer queue.
            }
            case ProtocolAdapterWrapperEvent.SubscriptionRetryTimerFired ignored -> {
                // Unused: aspects schedule and fire their own timers on the actor's single timer queue.
            }
            default -> {
                // The browse events (BrowsePageReceived / AttributesResolved / BrowseFailed) are handled
                // ahead of this switch and routed to the browse engine, never the adapter machine.
            }
        }
    }

    private void publishSnapshot() {
        try {
            snapshot.set(context.buildSnapshot(machine.state()));
        } catch (final Throwable exception) {
            // Never leave a stale healthy-looking snapshot published: a failing snapshot build is itself an ERROR.
            log.error("Failed to build the status snapshot of a v2 adapter; publishing a fallback ERROR", exception);
            snapshot.set(context.fallbackErrorSnapshot("status snapshot failed: " + exception.getMessage()));
        }
    }
}
