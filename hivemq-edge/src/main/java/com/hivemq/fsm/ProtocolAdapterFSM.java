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
package com.hivemq.fsm;

import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ProtocolAdapterFSM implements Consumer<ProtocolAdapterState.ConnectionStatus> {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ProtocolAdapterFSM.class);

    public enum StateEnum {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
        ERROR_CLOSING,
        CLOSING,
        ERROR,
        CLOSED,
        NOT_SUPPORTED
    }

    public static final @NotNull Map<StateEnum, Set<StateEnum>> possibleTransitions = Map.of(
            StateEnum.DISCONNECTED,
                    Set.of(
                            StateEnum.CONNECTING,
                            StateEnum.CONNECTED,
                            StateEnum.CLOSED), // for compatibility, we allow to go from CONNECTING to CONNECTED
            // directly, and allow testing transition to CLOSED
            StateEnum.CONNECTING,
                    Set.of(StateEnum.CONNECTED, StateEnum.ERROR, StateEnum.DISCONNECTED), // can go back to DISCONNECTED
            StateEnum.CONNECTED,
                    Set.of(
                            StateEnum.DISCONNECTING,
                            StateEnum.CONNECTING,
                            StateEnum.CLOSING,
                            StateEnum.ERROR_CLOSING,
                            StateEnum.DISCONNECTED), // transition to CONNECTING in case of recovery, DISCONNECTED for
            // direct transition
            StateEnum.DISCONNECTING,
                    Set.of(StateEnum.DISCONNECTED, StateEnum.CLOSING), // can go to DISCONNECTED or CLOSING
            StateEnum.CLOSING, Set.of(StateEnum.CLOSED),
            StateEnum.ERROR_CLOSING, Set.of(StateEnum.ERROR),
            StateEnum.ERROR, Set.of(StateEnum.CONNECTING, StateEnum.DISCONNECTED), // can recover from error
            StateEnum.CLOSED,
                    Set.of(StateEnum.DISCONNECTED, StateEnum.CLOSING) // can restart from closed or go to closing
            );

    public enum AdapterStateEnum {
        STARTING,
        STARTED,
        STOPPING,
        STOPPED
    }

    public static final Map<AdapterStateEnum, Set<AdapterStateEnum>> possibleAdapterStateTransitions = Map.of(
            AdapterStateEnum.STOPPED, Set.of(AdapterStateEnum.STARTING),
            AdapterStateEnum.STARTING, Set.of(AdapterStateEnum.STARTED, AdapterStateEnum.STOPPED),
            AdapterStateEnum.STARTED, Set.of(AdapterStateEnum.STOPPING),
            AdapterStateEnum.STOPPING, Set.of(AdapterStateEnum.STOPPED));

    private final AtomicReference<StateEnum> northboundState = new AtomicReference<>(StateEnum.DISCONNECTED);
    private final AtomicReference<StateEnum> southboundState = new AtomicReference<>(StateEnum.DISCONNECTED);
    private final AtomicReference<AdapterStateEnum> adapterState = new AtomicReference<>(AdapterStateEnum.STOPPED);

    private final List<Consumer<State>> stateTransitionListeners = new CopyOnWriteArrayList<>();

    public record State(AdapterStateEnum state, StateEnum northbound, StateEnum southbound) {}

    private final String adapterId;

    public ProtocolAdapterFSM(final @NotNull String adapterId) {
        this.adapterId = adapterId;
    }

    public abstract boolean onStarting();

    public abstract void onStopping();

    public abstract boolean startSouthbound();

    // ADAPTER signals
    public void startAdapter() {
        if (transitionAdapterState(AdapterStateEnum.STARTING)) {
            log.debug("Protocol adapter {} starting", adapterId);
            if (onStarting()) {
                if (!transitionAdapterState(AdapterStateEnum.STARTED)) {
                    log.warn("Protocol adapter {} already started", adapterId);
                }
            } else {
                transitionAdapterState(AdapterStateEnum.STOPPED);
            }
        } else {
            log.info("Protocol adapter {} already started or starting", adapterId);
        }
    }

    public void stopAdapter() {
        if (transitionAdapterState(AdapterStateEnum.STOPPING)) {
            onStopping();
            if (!transitionAdapterState(AdapterStateEnum.STOPPED)) {
                log.warn("Protocol adapter {} already stopped", adapterId);
            }
        } else {
            log.info("Protocol adapter {} already stopped or stopping", adapterId);
        }
    }

    public boolean transitionAdapterState(final @NotNull AdapterStateEnum newState) {
        final var currentState = adapterState.get();
        if (canTransition(currentState, newState)) {
            if (adapterState.compareAndSet(currentState, newState)) {
                log.debug("Adapter state transition from {} to {} for adapter {}", currentState, newState, adapterId);
                notifyListenersAboutStateTransition(currentState());
                return true;
            }
        } else {
            throw new IllegalStateException("Cannot transition adapter state to " + newState);
        }
        return false;
    }

    public boolean transitionNorthboundState(final @NotNull StateEnum newState) {
        final var currentState = northboundState.get();
        if (canTransition(currentState, newState)) {
            if (northboundState.compareAndSet(currentState, newState)) {
                log.debug(
                        "Northbound state transition from {} to {} for adapter {}", currentState, newState, adapterId);
                notifyListenersAboutStateTransition(currentState());
                return true;
            }
        } else {
            throw new IllegalStateException("Cannot transition northbound state to " + newState);
        }
        return false;
    }

    public boolean transitionSouthboundState(final @NotNull StateEnum newState) {
        final var currentState = southboundState.get();
        if (canTransition(currentState, newState)) {
            if (southboundState.compareAndSet(currentState, newState)) {
                log.debug(
                        "Southbound state transition from {} to {} for adapter {}", currentState, newState, adapterId);
                notifyListenersAboutStateTransition(currentState());
                return true;
            }
        } else {
            throw new IllegalStateException("Cannot transition southbound state to " + newState);
        }
        return false;
    }

    @Override
    public void accept(final ProtocolAdapterState.ConnectionStatus connectionStatus) {
        final var transitionResult =
                switch (connectionStatus) {
                    case CONNECTED -> transitionNorthboundState(StateEnum.CONNECTED) && startSouthbound();

                    case CONNECTING -> transitionNorthboundState(StateEnum.CONNECTING);
                    case DISCONNECTED -> transitionNorthboundState(StateEnum.DISCONNECTED);
                    case ERROR -> transitionNorthboundState(StateEnum.ERROR);
                    case UNKNOWN -> transitionNorthboundState(StateEnum.DISCONNECTED);
                    case STATELESS -> transitionNorthboundState(StateEnum.NOT_SUPPORTED);
                };
        if (!transitionResult) {
            log.warn("Failed to transition connection state to {} for adapter {}", connectionStatus, adapterId);
        }
    }

    // Additional methods to support full state machine functionality

    public boolean startDisconnecting() {
        return transitionNorthboundState(StateEnum.DISCONNECTING);
    }

    public boolean startClosing() {
        return transitionNorthboundState(StateEnum.CLOSING);
    }

    public boolean startErrorClosing() {
        return transitionNorthboundState(StateEnum.ERROR_CLOSING);
    }

    public boolean markAsClosed() {
        return transitionNorthboundState(StateEnum.CLOSED);
    }

    public boolean recoverFromError() {
        return transitionNorthboundState(StateEnum.CONNECTING);
    }

    public boolean restartFromClosed() {
        return transitionNorthboundState(StateEnum.DISCONNECTED);
    }

    // Southbound equivalents
    public boolean startSouthboundDisconnecting() {
        return transitionSouthboundState(StateEnum.DISCONNECTING);
    }

    public boolean startSouthboundClosing() {
        return transitionSouthboundState(StateEnum.CLOSING);
    }

    public boolean startSouthboundErrorClosing() {
        return transitionSouthboundState(StateEnum.ERROR_CLOSING);
    }

    public boolean markSouthboundAsClosed() {
        return transitionSouthboundState(StateEnum.CLOSED);
    }

    public boolean recoverSouthboundFromError() {
        return transitionSouthboundState(StateEnum.CONNECTING);
    }

    public boolean restartSouthboundFromClosed() {
        return transitionSouthboundState(StateEnum.DISCONNECTED);
    }

    public void registerStateTransitionListener(final @NotNull Consumer<State> stateTransitionListener) {
        stateTransitionListeners.add(stateTransitionListener);
    }

    public void unregisterStateTransitionListener(final @NotNull Consumer<State> stateTransitionListener) {
        stateTransitionListeners.remove(stateTransitionListener);
    }

    public State currentState() {
        return new State(adapterState.get(), northboundState.get(), southboundState.get());
    }

    private void notifyListenersAboutStateTransition(final @NotNull State newState) {
        stateTransitionListeners.forEach(listener -> listener.accept(newState));
    }

    private static boolean canTransition(final @NotNull StateEnum currentState, final @NotNull StateEnum newState) {
        final var allowedTransitions = possibleTransitions.get(currentState);
        return allowedTransitions != null && allowedTransitions.contains(newState);
    }

    private static boolean canTransition(
            final @NotNull AdapterStateEnum currentState, final @NotNull AdapterStateEnum newState) {
        final var allowedTransitions = possibleAdapterStateTransitions.get(currentState);
        return allowedTransitions != null && allowedTransitions.contains(newState);
    }
}
