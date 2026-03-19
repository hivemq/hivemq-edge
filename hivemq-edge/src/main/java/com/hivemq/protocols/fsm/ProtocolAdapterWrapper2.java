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
package com.hivemq.protocols.fsm;

import com.hivemq.adapter.sdk.api.ProtocolAdapter2;
import com.hivemq.adapter.sdk.api.ProtocolAdapterConnectionDirection;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the lifecycle and connection states of a single protocol adapter instance.
 * <p>
 * Responsibilities:
 * <ol>
 *   <li>Owns {@link ProtocolAdapterState} and both {@link ProtocolAdapterConnectionState} instances
 *       (northbound + southbound)</li>
 *   <li>Coordinates transitions between states</li>
 *   <li>Calls {@link ProtocolAdapter2} methods synchronously</li>
 *   <li>Notifies {@link ProtocolAdapterStateChangeListener}s on successful state transitions</li>
 *   <li>Manages service lifecycle (polling, writing) around connect/disconnect</li>
 * </ol>
 * <p>
 * Threading Model:
 * <ul>
 *   <li>{@link #start()} and {@link #stop(boolean)} are synchronized — only one thread can execute at a time</li>
 *   <li>All state transitions are synchronized</li>
 *   <li>Connect/disconnect operations run on the caller's thread</li>
 *   <li>State change notifications run synchronously on the caller's thread</li>
 * </ul>
 */
public class ProtocolAdapterWrapper2 {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolAdapterWrapper2.class);

    private final @NotNull ProtocolAdapter2 adapter;
    private final @NotNull List<ProtocolAdapterStateChangeListener> stateChangeListeners = new CopyOnWriteArrayList<>();
    private volatile @NotNull ProtocolAdapterState state;
    private volatile @NotNull ProtocolAdapterConnectionState northboundConnectionState;
    private volatile @NotNull ProtocolAdapterConnectionState southboundConnectionState;

    public ProtocolAdapterWrapper2(final @NotNull ProtocolAdapter2 adapter) {
        this.adapter = adapter;
        this.northboundConnectionState = ProtocolAdapterConnectionState.Disconnected;
        this.southboundConnectionState = ProtocolAdapterConnectionState.Disconnected;
        this.state = ProtocolAdapterState.Idle;
    }

    /**
     * @return the current southbound connection state
     */
    public @NotNull ProtocolAdapterConnectionState getSouthboundConnectionState() {
        return southboundConnectionState;
    }

    /**
     * @return the current adapter state
     */
    public @NotNull ProtocolAdapterState getState() {
        return state;
    }

    /**
     * @return the current northbound connection state
     */
    public @NotNull ProtocolAdapterConnectionState getNorthboundConnectionState() {
        return northboundConnectionState;
    }

    /**
     * @return the unique identifier of the wrapped adapter
     */
    public @NotNull String getAdapterId() {
        return adapter.getId();
    }

    /**
     * @return the protocol adapter information (protocol type, capabilities, etc.)
     */
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapter.getProtocolAdapterInformation();
    }

    /**
     * Register a listener that will be notified on successful adapter state transitions.
     *
     * @param listener the listener to add
     */
    public void addStateChangeListener(final @NotNull ProtocolAdapterStateChangeListener listener) {
        stateChangeListeners.add(listener);
    }

    /**
     * Remove a previously registered state change listener.
     *
     * @param listener the listener to remove
     */
    public void removeStateChangeListener(final @NotNull ProtocolAdapterStateChangeListener listener) {
        stateChangeListeners.remove(listener);
    }

    /**
     * Start the adapter.
     * <p>
     * This method is synchronized — only one thread can execute at a time.
     * The FSM state transitions handle conflict detection:
     * if already in Precheck/Working/Stopping, the transition to Precheck fails
     * and the caller receives {@code false}.
     * <p>
     * Flow:
     * <ol>
     *   <li>Idle → Precheck</li>
     *   <li>Call {@link ProtocolAdapter2#precheck()}</li>
     *   <li>Precheck → Working</li>
     *   <li>Call {@link #startNorthbound()} → {@link ProtocolAdapter2#connect(ProtocolAdapterConnectionDirection)}</li>
     *   <li>Call {@link #startSouthbound()} → {@link ProtocolAdapter2#connect(ProtocolAdapterConnectionDirection)} (if supported)</li>
     *   <li>Start polling and writing services</li>
     * </ol>
     * If any step fails, transitions to Error and cleans up any partially started connections.
     *
     * @return {@code true} if started successfully, {@code false} if FSM rejected or error occurred
     */
    public synchronized boolean start() {
        LOGGER.info("Starting protocol adapter '{}'.", getAdapterId());

        // Step 1: Idle → Precheck
        if (!transitionTo(ProtocolAdapterState.Precheck).status().isSuccess()) {
            return false;
        }

        // Step 2: Run precheck
        try {
            adapter.precheck();
        } catch (final Exception e) {
            LOGGER.error("Precheck failed for adapter '{}'.", getAdapterId(), e);
            transitionTo(ProtocolAdapterState.Error);
            return false;
        }

        // Step 3: Precheck → Working
        if (!transitionTo(ProtocolAdapterState.Working).status().isSuccess()) {
            return false;
        }

        // Step 4 & 5: Start connections
        final boolean northboundSuccess = startNorthbound();
        final boolean southboundSuccess = northboundSuccess && startSouthbound();

        if (!northboundSuccess || !southboundSuccess) {
            // Cleanup on failure: disconnect any connection that was started
            if (northboundSuccess) {
                stopNorthbound();
            }
            transitionTo(ProtocolAdapterState.Error);
            return false;
        }

        // Step 6: Start services
        startPolling();
        startWriting();

        return true;
    }

    /**
     * Stop the adapter.
     * <p>
     * This method is synchronized — only one thread can execute at a time.
     * The FSM state transitions handle conflict detection:
     * if in Idle/Precheck, the transition to Stopping fails.
     * If already in Stopping or Error, the method proceeds with disconnection.
     * <p>
     * Flow:
     * <ol>
     *   <li>Working → Stopping (or continue from Stopping/Error)</li>
     *   <li>Stop polling and writing services</li>
     *   <li>Disconnect southbound → {@link ProtocolAdapter2#disconnect(ProtocolAdapterConnectionDirection)} (if supported)</li>
     *   <li>Disconnect northbound → {@link ProtocolAdapter2#disconnect(ProtocolAdapterConnectionDirection)}</li>
     *   <li>When both disconnected → transition to Idle</li>
     *   <li>If {@code destroy} is true, call {@link ProtocolAdapter2#destroy()}</li>
     * </ol>
     *
     * @param destroy whether to call {@link ProtocolAdapter2#destroy()} after stopping
     * @return {@code true} if stopped successfully, {@code false} if FSM rejected or error occurred
     */
    public synchronized boolean stop(final boolean destroy) {
        LOGGER.info("Stopping protocol adapter '{}'.", getAdapterId());

        // Step 1: Working → Stopping
        if (!transitionTo(ProtocolAdapterState.Stopping).status().isSuccess()) {
            // Allow proceeding if already in Stopping or Error state
            if (!state.isStopping() && !state.isError()) {
                return false;
            }
        }

        // Step 2: Stop services
        stopPolling();
        stopWriting();

        // Step 3 & 4: Stop connections (southbound first, then northbound)
        final boolean southboundSuccess = stopSouthbound();
        final boolean northboundSuccess = stopNorthbound();

        // Step 5: Check if connections are ready for Idle transition
        final boolean northboundReady = northboundConnectionState.isDisconnected();
        final boolean southboundReady = !adapter.supportsSouthbound() || southboundConnectionState.isDisconnected();

        if (northboundReady && southboundReady) {
            transitionTo(ProtocolAdapterState.Idle);
            if (destroy) {
                adapter.destroy();
            }
            return true;
        }

        // If not fully disconnected, transition to Error
        if (!northboundSuccess || !southboundSuccess) {
            transitionTo(ProtocolAdapterState.Error);
        }
        return false;
    }

    /**
     * Start the northbound connection.
     * Transitions: Disconnected → Connecting → Connected.
     * Calls {@link ProtocolAdapter2#connect(ProtocolAdapterConnectionDirection)} with
     * {@link ProtocolAdapterConnectionDirection#Northbound}.
     *
     * @return {@code true} if the northbound connection was established successfully
     */
    protected boolean startNorthbound() {
        LOGGER.info("Starting northbound for protocol adapter '{}'.", getAdapterId());

        // Disconnected → Connecting
        if (!transitionNorthboundConnectionTo(ProtocolAdapterConnectionState.Connecting)
                .status()
                .isSuccess()) {
            return false;
        }

        try {
            adapter.connect(ProtocolAdapterConnectionDirection.Northbound);
            // Connecting → Connected
            return transitionNorthboundConnectionTo(ProtocolAdapterConnectionState.Connected)
                    .status()
                    .isSuccess();
        } catch (final Exception e) {
            LOGGER.error("Northbound connection failed for adapter '{}'.", getAdapterId(), e);
            // Connecting → Error
            transitionNorthboundConnectionTo(ProtocolAdapterConnectionState.Error);
            return false;
        }
    }

    /**
     * Start the southbound connection.
     * Only starts if the adapter supports southbound communication
     * ({@link ProtocolAdapter2#supportsSouthbound()} returns {@code true}).
     * Transitions: Disconnected → Connecting → Connected.
     *
     * @return {@code true} if the southbound connection was established or not needed
     */
    protected boolean startSouthbound() {
        if (!adapter.supportsSouthbound()) {
            LOGGER.debug("Adapter '{}' does not support southbound, skipping.", getAdapterId());
            return true;
        }

        LOGGER.info("Starting southbound for protocol adapter '{}'.", getAdapterId());

        // Disconnected → Connecting
        if (!transitionSouthboundConnectionTo(ProtocolAdapterConnectionState.Connecting)
                .status()
                .isSuccess()) {
            return false;
        }

        try {
            adapter.connect(ProtocolAdapterConnectionDirection.Southbound);
            // Connecting → Connected
            return transitionSouthboundConnectionTo(ProtocolAdapterConnectionState.Connected)
                    .status()
                    .isSuccess();
        } catch (final Exception e) {
            LOGGER.error("Southbound connection failed for adapter '{}'.", getAdapterId(), e);
            // Connecting → Error
            transitionSouthboundConnectionTo(ProtocolAdapterConnectionState.Error);
            return false;
        }
    }

    /**
     * Stop the northbound connection.
     * If already disconnected, returns {@code true} immediately.
     * Otherwise transitions: current → Disconnecting → Disconnected.
     * Calls {@link ProtocolAdapter2#disconnect(ProtocolAdapterConnectionDirection)} with
     * {@link ProtocolAdapterConnectionDirection#Northbound}.
     *
     * @return {@code true} if the northbound connection was disconnected successfully
     */
    protected boolean stopNorthbound() {
        if (northboundConnectionState.isDisconnected()) {
            return true;
        }

        LOGGER.info("Stopping northbound for protocol adapter '{}'.", getAdapterId());

        // current → Disconnecting
        if (!transitionNorthboundConnectionTo(ProtocolAdapterConnectionState.Disconnecting)
                .status()
                .isSuccess()) {
            return false;
        }

        try {
            adapter.disconnect(ProtocolAdapterConnectionDirection.Northbound);
        } catch (final Exception e) {
            LOGGER.warn("Error during northbound disconnect for adapter '{}'.", getAdapterId(), e);
            // Continue anyway — we want to reach Disconnected state
        }

        // Disconnecting → Disconnected
        return transitionNorthboundConnectionTo(ProtocolAdapterConnectionState.Disconnected)
                .status()
                .isSuccess();
    }

    /**
     * Stop the southbound connection.
     * Only stops if the adapter supports southbound communication.
     * If already disconnected, returns {@code true} immediately.
     * Otherwise transitions: current → Disconnecting → Disconnected.
     *
     * @return {@code true} if the southbound connection was disconnected or not needed
     */
    protected boolean stopSouthbound() {
        if (!adapter.supportsSouthbound()) {
            return true;
        }

        if (southboundConnectionState.isDisconnected()) {
            return true;
        }

        LOGGER.info("Stopping southbound for protocol adapter '{}'.", getAdapterId());

        // current → Disconnecting
        if (!transitionSouthboundConnectionTo(ProtocolAdapterConnectionState.Disconnecting)
                .status()
                .isSuccess()) {
            return false;
        }

        try {
            adapter.disconnect(ProtocolAdapterConnectionDirection.Southbound);
        } catch (final Exception e) {
            LOGGER.warn("Error during southbound disconnect for adapter '{}'.", getAdapterId(), e);
            // Continue anyway — we want to reach Disconnected state
        }

        // Disconnecting → Disconnected
        return transitionSouthboundConnectionTo(ProtocolAdapterConnectionState.Disconnected)
                .status()
                .isSuccess();
    }

    /**
     * Start polling for this adapter.
     * Called after all connections are established during {@link #start()}.
     * <p>
     * This is an extension point for service integration. The default implementation
     * is a no-op. Subclasses or the manager can override or configure this behavior.
     */
    protected void startPolling() {
        LOGGER.debug("Starting polling for protocol adapter '{}'.", getAdapterId());
    }

    /**
     * Stop polling for this adapter.
     * Called before disconnecting connections during {@link #stop(boolean)}.
     * <p>
     * This is an extension point for service integration. The default implementation
     * is a no-op. Subclasses or the manager can override or configure this behavior.
     */
    protected void stopPolling() {
        LOGGER.debug("Stopping polling for protocol adapter '{}'.", getAdapterId());
    }

    /**
     * Start writing for this adapter.
     * Called after all connections are established during {@link #start()}.
     * <p>
     * This is an extension point for service integration. The default implementation
     * is a no-op. Subclasses or the manager can override or configure this behavior.
     */
    protected void startWriting() {
        LOGGER.debug("Starting writing for protocol adapter '{}'.", getAdapterId());
    }

    /**
     * Stop writing for this adapter.
     * Called before disconnecting connections during {@link #stop(boolean)}.
     * <p>
     * This is an extension point for service integration. The default implementation
     * is a no-op. Subclasses or the manager can override or configure this behavior.
     */
    protected void stopWriting() {
        LOGGER.debug("Stopping writing for protocol adapter '{}'.", getAdapterId());
    }

    /**
     * Transition the adapter state to the given new state.
     * This method is synchronized to ensure atomic state transitions.
     * On successful transitions, all registered {@link ProtocolAdapterStateChangeListener}s are notified.
     *
     * @param newState the target state
     * @return the transition response indicating success, failure, or no change
     */
    public synchronized @NotNull ProtocolAdapterTransitionResponse transitionTo(
            final @NotNull ProtocolAdapterState newState) {
        final ProtocolAdapterState fromState = state;
        final ProtocolAdapterTransitionResponse response = fromState.transition(newState);
        state = response.toState();
        switch (response.status()) {
            case Success -> {
                LOGGER.debug(
                        "Protocol adapter '{}' transitioned from {} to {} successfully.",
                        getAdapterId(),
                        fromState,
                        state);
                notifyProtocolAdapterStateChangeListeners(fromState, state);
            }
            case Failure ->
                LOGGER.error(
                        "Protocol adapter '{}' failed to transition from {} to {}.", getAdapterId(), fromState, state);
            case NotChanged -> LOGGER.warn("Protocol adapter '{}' state {} is unchanged.", getAdapterId(), state);
        }
        return response;
    }

    /**
     * Transition the southbound connection state to the given new state.
     * This method is synchronized to ensure atomic state transitions.
     *
     * @param newState the target connection state
     * @return the transition response indicating success, failure, or no change
     */
    public synchronized @NotNull ProtocolAdapterConnectionTransitionResponse transitionSouthboundConnectionTo(
            final @NotNull ProtocolAdapterConnectionState newState) {
        final ProtocolAdapterConnectionState fromState = southboundConnectionState;
        final ProtocolAdapterConnectionTransitionResponse response = fromState.transition(newState);
        southboundConnectionState = response.toState();
        switch (response.status()) {
            case Success ->
                LOGGER.debug(
                        "Protocol adapter '{}' southbound connection transitioned from {} to {} successfully.",
                        getAdapterId(),
                        fromState,
                        southboundConnectionState);
            case Failure ->
                LOGGER.error(
                        "Protocol adapter '{}' southbound connection failed to transition from {} to {}.",
                        getAdapterId(),
                        fromState,
                        southboundConnectionState);
            case NotChanged ->
                LOGGER.warn(
                        "Protocol adapter '{}' southbound connection state {} is unchanged.",
                        getAdapterId(),
                        southboundConnectionState);
        }
        return response;
    }

    /**
     * Transition the northbound connection state to the given new state.
     * This method is synchronized to ensure atomic state transitions.
     *
     * @param newState the target connection state
     * @return the transition response indicating success, failure, or no change
     */
    public synchronized @NotNull ProtocolAdapterConnectionTransitionResponse transitionNorthboundConnectionTo(
            final @NotNull ProtocolAdapterConnectionState newState) {
        final ProtocolAdapterConnectionState fromState = northboundConnectionState;
        final ProtocolAdapterConnectionTransitionResponse response = fromState.transition(newState);
        northboundConnectionState = response.toState();
        switch (response.status()) {
            case Success ->
                LOGGER.debug(
                        "Protocol adapter '{}' northbound connection transitioned from {} to {} successfully.",
                        getAdapterId(),
                        fromState,
                        northboundConnectionState);
            case Failure ->
                LOGGER.error(
                        "Protocol adapter '{}' northbound connection failed to transition from {} to {}.",
                        getAdapterId(),
                        fromState,
                        northboundConnectionState);
            case NotChanged ->
                LOGGER.warn(
                        "Protocol adapter '{}' northbound connection state {} is unchanged.",
                        getAdapterId(),
                        northboundConnectionState);
        }
        return response;
    }

    private void notifyProtocolAdapterStateChangeListeners(
            final @NotNull ProtocolAdapterState fromState, final @NotNull ProtocolAdapterState toState) {
        for (final ProtocolAdapterStateChangeListener listener : stateChangeListeners) {
            try {
                listener.onStateChanged(fromState, toState);
            } catch (final Exception e) {
                LOGGER.warn(
                        "State change listener threw exception for adapter '{}' ({} → {}).",
                        getAdapterId(),
                        fromState,
                        toState,
                        e);
            }
        }
    }
}
