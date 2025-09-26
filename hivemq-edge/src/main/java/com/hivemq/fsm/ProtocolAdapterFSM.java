package com.hivemq.fsm;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ProtocolAdapterFSM {

    private static final Logger log = LoggerFactory.getLogger(ProtocolAdapterFSM.class);

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

    public enum AdapterState {
        STARTED,
        STOPPING,
        STOPPED
    }

    public static final Map<StateEnum, Set<StateEnum>> possibleTransitions = Map.of(
            StateEnum.DISCONNECTED, Set.of(StateEnum.CONNECTING),
            StateEnum.CONNECTING, Set.of(StateEnum.CONNECTED, StateEnum.ERROR),
            StateEnum.CONNECTED, Set.of(StateEnum.DISCONNECTING, StateEnum.CLOSING, StateEnum.ERROR_CLOSING),
            StateEnum.DISCONNECTING, Set.of(StateEnum.CONNECTING),
            StateEnum.CLOSING, Set.of(StateEnum.CLOSED),
            StateEnum.ERROR_CLOSING, Set.of(StateEnum.ERROR)
    );

    private final AtomicReference<StateEnum> northboundState = new AtomicReference<>(StateEnum.DISCONNECTED);
    private final AtomicReference<StateEnum> southboundState = new AtomicReference<>(StateEnum.DISCONNECTED);
    private final AtomicReference<AdapterState> adapterState = new AtomicReference<>(AdapterState.STOPPED);

    private final List<Consumer<State>> stateTransitionListeners = new CopyOnWriteArrayList<>();

    public record State(AdapterState state, StateEnum northbound, StateEnum southbound) { }

    private final String adapterId;

    public ProtocolAdapterFSM(final @NotNull String adapterId) {
        this.adapterId = adapterId;
    }

    public void registerStateTransitionListener(final @NotNull Consumer<State> stateTransitionListener) {
        stateTransitionListeners.add(stateTransitionListener);
    }

    public void unregisterStateTransitionListener(final @NotNull Consumer<StateEnum> stateTransitionListener) {
        stateTransitionListeners.remove(stateTransitionListener);
    }

    public State currentState() {
        return new State(adapterState.get(), northboundState.get(), southboundState.get());
    }

    public void startAdapter() {
        if(adapterState.compareAndSet(AdapterState.STOPPED, AdapterState.STARTED)) {
            log.debug("Protocol adapter {} started", adapterId);
            notifyListenersAboutStateTransition(getCurrentState());
        } else {
            log.info("Protocol adapter {} already started", adapterId);
        }
    }

    public void stopAdapter() {
        if(adapterState.compareAndSet(AdapterState.STARTED, AdapterState.STOPPING)) {
            log.debug("Protocol adapter {} stopped", adapterId);
            notifyListenersAboutStateTransition(getCurrentState());
        } else {
            log.info("Protocol adapter {} already stopped or stopping", adapterId);
        }
    }

    public void transitionNorthboundState(final @NotNull StateEnum newState) {
        if(canTransition(northboundState.get(), newState)) {
            synchronized (northboundState) {
                final StateEnum oldState = northboundState.getAndSet(newState);
                log.debug("Northbound state transition from {} to {} for adapter {}", oldState, newState, adapterId);
                notifyListenersAboutStateTransition(getCurrentState());
            }
        } else {
            throw new IllegalStateException("Cannot transition northbound state to " + newState);
        }
    }

    private void notifyListenersAboutStateTransition(final @NotNull State newState) {
        stateTransitionListeners.forEach(listener -> listener.accept(newState));
    }

    private boolean canTransition(final @NotNull StateEnum currentState, final @NotNull StateEnum newState) {
        final Set<StateEnum> allowedTransitions = possibleTransitions.get(currentState);
        return allowedTransitions != null && allowedTransitions.contains(newState);
    }

    private State getCurrentState() {
        return new State(adapterState.get(), northboundState.get(), southboundState.get());
    }
}

