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

import com.hivemq.adapter.sdk.api.v2.model.ErrorScope;
import com.hivemq.protocols.v2.fsm.FSMAction;
import com.hivemq.protocols.v2.fsm.FSMGuard;
import com.hivemq.protocols.v2.fsm.FSMTransitionTable;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperEvent.AllVerified;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperEvent.BackoffFired;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperEvent.Connected;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperEvent.DataPointReceived;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperEvent.Disconnected;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperEvent.ErrorEvent;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperEvent.NodeErrorReceived;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperEvent.PollTimerFired;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperEvent.Started;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperEvent.Stopped;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperEvent.SubscriptionRetryTimerFired;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperEvent.VerificationRetryTimerFired;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperEvent.VerifyResultReceived;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperEvent.WatchdogFired;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperEvent.WriteResultReceived;
import org.jetbrains.annotations.NotNull;

/**
 * The adapter machine's transition table — a direct, reviewable encoding of the design's state diagram
 * plus the {@code ERROR} absorb rows. Every row maps to one labeled edge in the diagram; the mandatory
 * {@code unmatched} slot is the defensive reset for any event with no listed transition.
 * <p>
 * Goal-driven edges (every {@code goal→STOPPED} edge, and {@code STOPPED → WAITING_FOR_STARTED}) are <b>not</b>
 * here: they are handled by {@code stepTowardGoal} through the goal-command bypass, never the table.
 * The table carries only the event-driven edges: acknowledgments, the synthesized {@link AllVerified} gate
 * signal, connection errors and losses, and the watchdog/backoff timer expiries.
 * <p>
 * {@code ERROR} absorbs every protocol-adapter event as a named row so the defensive reset — which itself issues
 * {@code stop()} — cannot loop on the resulting {@code stopped()}/{@code disconnected()}. Verify,
 * data, write, browse, and aspect-timer events never reach the table in non-{@code ERROR} states (the wrapper
 * routes them to the tag plane); they appear below only as {@code ERROR} absorb rows, where the wrapper does feed
 * every event to the machine.
 */
public final class ProtocolAdapterFSMTransitions {

    private ProtocolAdapterFSMTransitions() {}

    /**
     * @return a fresh adapter-machine transition table.
     */
    public static @NotNull FSMTransitionTable<
                    ProtocolAdapterWrapperState, ProtocolAdapterWrapperEvent, ProtocolAdapterWrapperContext>
            table() {
        final FSMAction<ProtocolAdapterWrapperState, ProtocolAdapterWrapperEvent, ProtocolAdapterWrapperContext>
                absorb = (current, event, context) -> context.absorbInError(event);
        final FSMAction<ProtocolAdapterWrapperState, ProtocolAdapterWrapperEvent, ProtocolAdapterWrapperContext>
                watchdogToError = (current, event, context) -> context.watchdogErrorStep(current);
        final FSMAction<ProtocolAdapterWrapperState, ProtocolAdapterWrapperEvent, ProtocolAdapterWrapperContext>
                connectionRetry = (current, event, context) -> context.connectionRetryStep();
        final FSMAction<ProtocolAdapterWrapperState, ProtocolAdapterWrapperEvent, ProtocolAdapterWrapperContext>
                adapterToError = (current, event, context) -> context.adapterErrorStep(((ErrorEvent) event).reason());
        final FSMGuard<ProtocolAdapterWrapperState, ProtocolAdapterWrapperEvent, ProtocolAdapterWrapperContext>
                connectionScope = (current, event, context) ->
                        event instanceof final ErrorEvent error && error.scope() == ErrorScope.CONNECTION;
        final FSMGuard<ProtocolAdapterWrapperState, ProtocolAdapterWrapperEvent, ProtocolAdapterWrapperContext>
                adapterScope = (current, event, context) ->
                        event instanceof final ErrorEvent error && error.scope() == ErrorScope.ADAPTER;

        return FSMTransitionTable
                .<ProtocolAdapterWrapperState, ProtocolAdapterWrapperEvent, ProtocolAdapterWrapperContext>builder()

                // ── WAITING_FOR_STARTED ────────────────────────────────────────────────────────────────────────
                .on(WAITING_FOR_STARTED, Started.class)
                .when((current, event, context) -> context.goalWantsConnected())
                .then((current, event, context) -> context.connectStep())
                .on(WAITING_FOR_STARTED, Started.class)
                .otherwise((current, event, context) -> context.stopStep()) // stop intent
                .on(WAITING_FOR_STARTED, ErrorEvent.class)
                .when(adapterScope)
                .then(adapterToError)
                .on(WAITING_FOR_STARTED, WatchdogFired.class)
                .then(watchdogToError)

                // ── WAITING_FOR_CONNECTED ──────────────────────────────────────────────────────────────────────
                .on(WAITING_FOR_CONNECTED, Connected.class)
                .when((current, event, context) -> !context.skipVerification())
                .then((current, event, context) -> context.verifyStep())
                .on(WAITING_FOR_CONNECTED, Connected.class)
                .otherwise((current, event, context) -> context.connectedStep()) // skip-verification flag
                .on(WAITING_FOR_CONNECTED, Disconnected.class)
                .then(connectionRetry)
                .on(WAITING_FOR_CONNECTED, ErrorEvent.class)
                .when(connectionScope)
                .then(connectionRetry)
                .on(WAITING_FOR_CONNECTED, WatchdogFired.class)
                .then(watchdogToError)

                // ── WAITING_FOR_VERIFICATION ───────────────────────────────────────────────────────────────────
                .on(WAITING_FOR_VERIFICATION, AllVerified.class)
                .then((current, event, context) -> context.connectedStep())
                .on(WAITING_FOR_VERIFICATION, Disconnected.class)
                .then(connectionRetry)
                .on(WAITING_FOR_VERIFICATION, ErrorEvent.class)
                .when(connectionScope)
                .then(connectionRetry)
                .on(WAITING_FOR_VERIFICATION, WatchdogFired.class)
                .then((current, event, context) -> context.disconnectBeforeReconnectStep()) // verification watchdog

                // ── CONNECTED ──────────────────────────────────────────────────────────────────────────────────
                .on(CONNECTED, Disconnected.class)
                .then((current, event, context) -> context.connectionRetryFromConnectedStep())
                .on(CONNECTED, ErrorEvent.class)
                .when(connectionScope)
                .then((current, event, context) -> context.disconnectBeforeReconnectFromConnectedStep())
                .on(CONNECTED, ErrorEvent.class)
                .when(adapterScope)
                .then(adapterToError)

                // ── WAITING_FOR_CONNECTION_RETRY (no watchdog; bounded by backoff + retry policy) ────────────────
                .on(WAITING_FOR_CONNECTION_RETRY, BackoffFired.class)
                .then((current, event, context) -> context.connectStep())

                // ── WAITING_FOR_DISCONNECTED_BEFORE_RECONNECT ────────────────────────────────────────────────────
                .on(WAITING_FOR_DISCONNECTED_BEFORE_RECONNECT, Disconnected.class)
                .then(connectionRetry)
                .on(WAITING_FOR_DISCONNECTED_BEFORE_RECONNECT, WatchdogFired.class)
                .then(watchdogToError)

                // ── WAITING_FOR_DISCONNECTED ─────────────────────────────────────────────────────────────────────
                .on(WAITING_FOR_DISCONNECTED, Disconnected.class)
                .then((current, event, context) -> context.stopStep())
                .on(WAITING_FOR_DISCONNECTED, WatchdogFired.class)
                .then(watchdogToError)

                // ── WAITING_FOR_STOPPED ──────────────────────────────────────────────────────────────────────────
                .on(WAITING_FOR_STOPPED, Stopped.class)
                .then((current, event, context) -> context.stoppedStep())
                .on(WAITING_FOR_STOPPED, ErrorEvent.class)
                .when(adapterScope)
                .then(adapterToError)
                .on(WAITING_FOR_STOPPED, WatchdogFired.class)
                .then(watchdogToError)

                // ── ERROR absorbs every protocol-adapter event and timer expiry ────────────────────
                .on(ERROR, Started.class)
                .then(absorb)
                .on(ERROR, Stopped.class)
                .then(absorb)
                .on(ERROR, Connected.class)
                .then(absorb)
                .on(ERROR, Disconnected.class)
                .then(absorb)
                .on(ERROR, ErrorEvent.class)
                .then(absorb)
                .on(ERROR, VerifyResultReceived.class)
                .then(absorb)
                .on(ERROR, AllVerified.class)
                .then(absorb)
                .on(ERROR, DataPointReceived.class)
                .then(absorb)
                .on(ERROR, NodeErrorReceived.class)
                .then(absorb)
                .on(ERROR, WriteResultReceived.class)
                .then(absorb)
                .on(ERROR, WatchdogFired.class)
                .then(absorb)
                .on(ERROR, BackoffFired.class)
                .then(absorb)
                .on(ERROR, PollTimerFired.class)
                .then(absorb)
                .on(ERROR, VerificationRetryTimerFired.class)
                .then(absorb)
                .on(ERROR, SubscriptionRetryTimerFired.class)
                .then(absorb)

                // ── Defensive reset for any event with no listed transition ─────────────────────────
                .unmatched((current, event, context) -> context.defensiveReset(current, event))
                .build();
    }
}
