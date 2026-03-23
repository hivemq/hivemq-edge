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
package com.hivemq.protocols;

import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterConnectionDirection;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryInput;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryOutput;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.polling.PollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.edge.modules.adapters.data.TagManager;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingService;
import com.hivemq.persistence.mappings.NorthboundMapping;
import com.hivemq.persistence.mappings.SouthboundMapping;
import com.hivemq.protocols.fsm.ProtocolAdapterConnectionState;
import com.hivemq.protocols.fsm.ProtocolAdapterConnectionTransitionResponse;
import com.hivemq.protocols.fsm.ProtocolAdapterRuntimeState;
import com.hivemq.protocols.fsm.ProtocolAdapterStateChangeListener;
import com.hivemq.protocols.fsm.ProtocolAdapterTransitionResponse;
import com.hivemq.protocols.northbound.NorthboundConsumerFactory;
import com.hivemq.protocols.northbound.NorthboundTagConsumer;
import com.hivemq.protocols.northbound.PerAdapterSampler;
import com.hivemq.protocols.northbound.PerContextSampler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the lifecycle and connection states of a single protocol adapter instance.
 * <p>
 * Responsibilities:
 * <ol>
 *   <li>Owns {@link ProtocolAdapterRuntimeState} and both
 *       {@link ProtocolAdapterConnectionState} instances (northbound + southbound)</li>
 *   <li>Coordinates transitions between states</li>
 *   <li>Calls {@link ProtocolAdapter} methods synchronously</li>
 *   <li>Notifies {@link ProtocolAdapterStateChangeListener}s on successful state transitions</li>
 *   <li>Manages service lifecycle (polling, writing) around connect/disconnect</li>
 *   <li>Provides backward-compatible API for consumers that previously used
 *       {@link com.hivemq.protocols.ProtocolAdapterWrapper}</li>
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
public class ProtocolAdapterWrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolAdapterWrapper.class);

    private final @NotNull ProtocolAdapter adapter;
    private final @NotNull List<ProtocolAdapterStateChangeListener> stateChangeListeners = new CopyOnWriteArrayList<>();
    private volatile @NotNull ProtocolAdapterRuntimeState state;
    private volatile @NotNull ProtocolAdapterConnectionState northboundConnectionState;
    private volatile @NotNull ProtocolAdapterConnectionState southboundConnectionState;

    // Additional context for backward compatibility with consumers of the old ProtocolAdapterWrapper
    private final @NotNull ProtocolAdapterConfig config;
    private final @NotNull ProtocolAdapterFactory<?> adapterFactory;
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterMetricsService metricsService;
    private final @NotNull ProtocolAdapterStateImpl protocolAdapterState;
    private volatile @Nullable Long lastStartAttemptTime;

    // Services for polling/writing lifecycle
    private final @NotNull ProtocolAdapterPollingService protocolAdapterPollingService;
    private final @NotNull EventService eventService;
    private final @NotNull ModuleServices moduleServices;
    private final @NotNull TagManager tagManager;
    private final @NotNull NorthboundConsumerFactory northboundConsumerFactory;
    private final @NotNull InternalProtocolAdapterWritingService protocolAdapterWritingService;
    private final @NotNull List<NorthboundTagConsumer> consumers = new ArrayList<>();

    /**
     * Full constructor with all context needed for production use.
     */
    public ProtocolAdapterWrapper(
            final @NotNull ProtocolAdapter adapter,
            final @NotNull ProtocolAdapterConfig config,
            final @NotNull ProtocolAdapterFactory<?> adapterFactory,
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterMetricsService metricsService,
            final @NotNull ProtocolAdapterStateImpl protocolAdapterState,
            final @NotNull ProtocolAdapterPollingService protocolAdapterPollingService,
            final @NotNull EventService eventService,
            final @NotNull ModuleServices moduleServices,
            final @NotNull TagManager tagManager,
            final @NotNull NorthboundConsumerFactory northboundConsumerFactory,
            final @NotNull InternalProtocolAdapterWritingService protocolAdapterWritingService) {
        this.adapter = adapter;
        this.config = config;
        this.adapterFactory = adapterFactory;
        this.adapterInformation = adapterInformation;
        this.metricsService = metricsService;
        this.protocolAdapterState = protocolAdapterState;
        this.protocolAdapterPollingService = protocolAdapterPollingService;
        this.eventService = eventService;
        this.moduleServices = moduleServices;
        this.tagManager = tagManager;
        this.northboundConsumerFactory = northboundConsumerFactory;
        this.protocolAdapterWritingService = protocolAdapterWritingService;
        this.northboundConnectionState = ProtocolAdapterConnectionState.Disconnected;
        this.southboundConnectionState = ProtocolAdapterConnectionState.Disconnected;
        this.state = ProtocolAdapterRuntimeState.Idle;
    }

    // ===== FSM State Accessors =====

    /**
     * @return the current southbound connection state
     */
    public @NotNull ProtocolAdapterConnectionState getSouthboundConnectionState() {
        return southboundConnectionState;
    }

    /**
     * @return the current adapter state
     */
    public @NotNull ProtocolAdapterRuntimeState getState() {
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
     * Alias for {@link #getAdapterId()} — matches the old ProtocolAdapterWrapper API.
     *
     * @return the unique identifier of the wrapped adapter
     */
    public @NotNull String getId() {
        return adapter.getId();
    }

    /**
     * @return the protocol adapter information (protocol type, capabilities, etc.)
     */
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapter.getProtocolAdapterInformation();
    }

    // ===== Backward-Compatible Accessors =====

    /**
     * Returns the underlying {@link ProtocolAdapter}.
     *
     * @return the underlying ProtocolAdapter
     */
    public @NotNull ProtocolAdapter getAdapter() {
        return adapter;
    }

    /**
     * @return the adapter factory that created this adapter
     */
    public @NotNull ProtocolAdapterFactory<?> getAdapterFactory() {
        return adapterFactory;
    }

    /**
     * @return the adapter type information
     */
    public @NotNull ProtocolAdapterInformation getAdapterInformation() {
        return adapterInformation;
    }

    /**
     * @return the adapter-specific configuration object
     */
    public @NotNull ProtocolSpecificAdapterConfig getConfigObject() {
        return config.getAdapterConfig();
    }

    /**
     * @return the full adapter configuration
     */
    public @NotNull ProtocolAdapterConfig getConfig() {
        return config;
    }

    /**
     * @return the tags configured for this adapter
     */
    public @NotNull List<? extends Tag> getTags() {
        return config.getTags();
    }

    /**
     * @return the northbound mappings for this adapter
     */
    public @NotNull List<NorthboundMapping> getNorthboundMappings() {
        return config.getNorthboundMappings();
    }

    /**
     * @return the southbound mappings for this adapter
     */
    public @NotNull List<SouthboundMapping> getSouthboundMappings() {
        return config.getSouthboundMappings();
    }

    /**
     * @return the metrics service for this adapter
     */
    public @NotNull ProtocolAdapterMetricsService getProtocolAdapterMetricsService() {
        return metricsService;
    }

    /**
     * @return the timestamp of the last start attempt, or null if never started
     */
    public @Nullable Long getTimeOfLastStartAttempt() {
        return lastStartAttemptTime;
    }

    /**
     * Maps the FSM adapter state to the old RuntimeStatus enum.
     *
     * @return the runtime status compatible with the old API
     */
    public @NotNull ProtocolAdapterState.RuntimeStatus getRuntimeStatus() {
        return switch (state) {
            case Working -> ProtocolAdapterState.RuntimeStatus.STARTED;
            default -> ProtocolAdapterState.RuntimeStatus.STOPPED;
        };
    }

    /**
     * Maps the connection state to the old ConnectionStatus enum.
     * <p>
     * Delegates to {@link ProtocolAdapterStateImpl} when available, since old adapters
     * update their connection status internally (e.g., OPC UA health check detects
     * connection loss and sets DISCONNECTED) without going through the wrapper's FSM.
     * Falls back to the FSM northbound connection state for test-only wrappers.
     *
     * @return the connection status compatible with the old API
     */
    public @NotNull ProtocolAdapterState.ConnectionStatus getConnectionStatus() {
        return protocolAdapterState.getConnectionStatus();
    }

    /**
     * @return the last error message, or null if no error
     */
    public @Nullable String getErrorMessage() {
        return protocolAdapterState.getLastErrorMessage();
    }

    /**
     * Sets the connection status to ERROR with the given exception.
     * This is called by the polling infrastructure when a sampler error occurs.
     *
     * @param exception    the error that occurred
     * @param errorMessage an optional error message
     */
    public void setErrorConnectionStatus(final @NotNull Throwable exception, final @Nullable String errorMessage) {
        protocolAdapterState.setErrorConnectionStatus(exception, errorMessage);
        // Also transition the FSM northbound connection to Error if currently Connected
        if (northboundConnectionState.isConnected()) {
            transitionNorthboundConnectionTo(ProtocolAdapterConnectionState.Error);
        }
    }

    /**
     * Sets the runtime status on the old state object (backward compatibility).
     *
     * @param runtimeStatus the runtime status to set
     */
    public void setRuntimeStatus(final @NotNull ProtocolAdapterState.RuntimeStatus runtimeStatus) {
        protocolAdapterState.setRuntimeStatus(runtimeStatus);
    }

    /**
     * Delegates discovery to the underlying adapter.
     */
    public void discoverValues(
            final @NotNull ProtocolAdapterDiscoveryInput input, final @NotNull ProtocolAdapterDiscoveryOutput output) {
        getAdapter().discoverValues(input, output);
    }

    /**
     * @return true if the underlying adapter supports writing (southbound)
     */
    public boolean isWriting() {
        return getAdapter() instanceof WritingProtocolAdapter;
    }

    /**
     * @return true if the underlying adapter supports polling
     */
    public boolean isPolling() {
        return getAdapter() instanceof PollingProtocolAdapter;
    }

    /**
     * @return true if the underlying adapter supports batch polling
     */
    public boolean isBatchPolling() {
        return getAdapter() instanceof BatchPollingProtocolAdapter;
    }

    /**
     * Async wrapper for {@link #start()} that matches the old API signature.
     * The REST API expects async start/stop for status transitions.
     *
     * @return a future that completes when the adapter is started
     */
    public @NotNull CompletableFuture<Void> startAsync() {
        return CompletableFuture.supplyAsync(() -> {
            final boolean success = start();
            if (!success) {
                throw new RuntimeException("Failed to start adapter: " + getAdapterId());
            }
            return null;
        });
    }

    /**
     * Async wrapper for {@link #stop(boolean)} that matches the old API signature.
     *
     * @param destroy whether to destroy the adapter after stopping
     * @return a future that completes when the adapter is stopped
     */
    public @NotNull CompletableFuture<Void> stopAsync(final boolean destroy) {
        return CompletableFuture.supplyAsync(() -> {
            stop(destroy);
            return null;
        });
    }

    // ===== Listener Management =====

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

    // ===== FSM Lifecycle Methods =====

    /**
     * Start the adapter.
     * <p>
     * This method is {@code synchronized} because the FSM alone is not sufficient to prevent
     * concurrent corruption. The FSM guards individual transition points, but {@code start()}
     * is a multi-step operation with real work (precheck, connect, service setup) happening
     * <em>between</em> transitions. Without {@code synchronized}, a concurrent {@code stop()}
     * could transition the FSM to Stopping/Idle while this method is still connecting or
     * starting services — leaving the adapter with active connections and running services
     * in an Idle state.
     * <p>
     * Flow:
     * <ol>
     *   <li>Idle → Precheck</li>
     *   <li>Call {@link ProtocolAdapter#precheck()}</li>
     *   <li>Precheck → Working</li>
     *   <li>Call {@link #startNorthbound()} → {@link ProtocolAdapter#start(ProtocolAdapterConnectionDirection, com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput, com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput)}</li>
     *   <li>Call {@link #startSouthbound()} → {@link ProtocolAdapter#start(ProtocolAdapterConnectionDirection, com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput, com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput)} (if supported)</li>
     *   <li>Start polling and writing services</li>
     * </ol>
     * If any step fails, transitions to Error and cleans up any partially started connections.
     *
     * @return {@code true} if started successfully, {@code false} if FSM rejected or error occurred
     */
    public synchronized boolean start() {
        LOGGER.info("Starting protocol adapter '{}'.", getAdapterId());
        lastStartAttemptTime = System.currentTimeMillis();
        protocolAdapterState.clearShuttingDown();

        // Step 1: Idle → Precheck
        if (!transitionTo(ProtocolAdapterRuntimeState.Precheck).status().isSuccess()) {
            return false;
        }

        // Step 2: Run precheck
        try {
            adapter.precheck();
        } catch (final Exception e) {
            LOGGER.error("Precheck failed for adapter '{}'.", getAdapterId(), e);
            transitionTo(ProtocolAdapterRuntimeState.Error);
            return false;
        }

        // Step 3: Precheck → Working
        if (!transitionTo(ProtocolAdapterRuntimeState.Working).status().isSuccess()) {
            return false;
        }

        // Update old state for backward compat
        protocolAdapterState.setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STARTED);

        // Step 4 & 5: Start connections
        final boolean northboundSuccess = startNorthbound();
        final boolean southboundSuccess = northboundSuccess && startSouthbound();

        if (!northboundSuccess || !southboundSuccess) {
            // Cleanup on failure: disconnect any connection that was started
            if (northboundSuccess) {
                stopNorthbound();
            }
            transitionTo(ProtocolAdapterRuntimeState.Error);
            protocolAdapterState.setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STOPPED);
            return false;
        }

        // Step 6: Start services
        createAndSubscribeTagConsumers();
        startPolling();
        startWriting();

        return true;
    }

    /**
     * Stop the adapter.
     * <p>
     * This method is {@code synchronized} for the same reason as {@link #start()}: it is a
     * multi-step operation with real work (service teardown, disconnect) between FSM transitions.
     * Without {@code synchronized}, a concurrent {@code start()} could transition the FSM to
     * Precheck/Working while this method is still disconnecting — leaving the adapter partially
     * connected in a Working state, or with services started against a disconnected adapter.
     * <p>
     * Flow:
     * <ol>
     *   <li>Working → Stopping (or Error → Stopping)</li>
     *   <li>Stop polling and writing services</li>
     *   <li>Disconnect southbound → {@link ProtocolAdapter#stop(ProtocolAdapterConnectionDirection, com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput, com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput)} (if supported)</li>
     *   <li>Disconnect northbound → {@link ProtocolAdapter#stop(ProtocolAdapterConnectionDirection, com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput, com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput)}</li>
     *   <li>Stopping → Idle</li>
     *   <li>If {@code destroy} is true, call {@link ProtocolAdapter#destroy()}</li>
     * </ol>
     *
     * @param destroy whether to call {@link ProtocolAdapter#destroy()} after stopping
     * @return {@code true} if stopped fully cleanly, {@code false} if FSM rejected or any disconnect step failed
     */
    public synchronized boolean stop(final boolean destroy) {
        LOGGER.info("Stopping protocol adapter '{}'.", getAdapterId());

        // Step 1: Working → Stopping (or Error → Stopping)
        if (!transitionTo(ProtocolAdapterRuntimeState.Stopping).status().isSuccess()) {
            return false;
        }

        protocolAdapterState.markShuttingDown();

        // Step 2: Stop services
        removeTagConsumers();
        stopPolling();
        stopWriting();

        // Step 3 & 4: Stop connections (southbound first, then northbound)
        final boolean southboundSuccess = stopSouthbound();
        final boolean northboundSuccess = stopNorthbound();

        // Step 5: Transition to Idle
        final boolean stateTransitionSuccess =
                transitionTo(ProtocolAdapterRuntimeState.Idle).status().isSuccess();
        protocolAdapterState.setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STOPPED);
        if (destroy) {
            adapter.destroy();
        }
        return stateTransitionSuccess && southboundSuccess && northboundSuccess;
    }

    // ===== Connection Management =====

    /**
     * Start the northbound connection.
     * Transitions: Disconnected → Connecting → Connected.
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
            invokeStart(ProtocolAdapterConnectionDirection.Northbound);
            // Connecting → Connected
            final boolean success = transitionNorthboundConnectionTo(ProtocolAdapterConnectionState.Connected)
                    .status()
                    .isSuccess();
            // Only set CONNECTED if the adapter hasn't already set its own status during connect().
            // Stateless adapters (e.g., File, Simulation) set STATELESS during start() — we must not overwrite that.
            if (success
                    && protocolAdapterState.getConnectionStatus()
                            == ProtocolAdapterState.ConnectionStatus.DISCONNECTED) {
                protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
            }
            return success;
        } catch (final Exception e) {
            LOGGER.error("Northbound connection failed for adapter '{}'.", getAdapterId(), e);
            // Connecting → Error
            transitionNorthboundConnectionTo(ProtocolAdapterConnectionState.Error);
            protocolAdapterState.setErrorConnectionStatus(e, e.getMessage());
            return false;
        }
    }

    /**
     * Start the southbound connection.
     * Only starts if the adapter supports southbound communication.
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
            invokeStart(ProtocolAdapterConnectionDirection.Southbound);
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

        boolean disconnectSuccess = true;
        try {
            invokeStop(ProtocolAdapterConnectionDirection.Northbound);
        } catch (final Exception e) {
            LOGGER.warn("Error during northbound disconnect for adapter '{}'.", getAdapterId(), e);
            // Continue anyway — we want to reach Disconnected state
            disconnectSuccess = false;
        }

        // Disconnecting → Disconnected
        final boolean success = transitionNorthboundConnectionTo(ProtocolAdapterConnectionState.Disconnected)
                .status()
                .isSuccess();
        if (success) {
            protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
        }
        return disconnectSuccess && success;
    }

    /**
     * Stop the southbound connection.
     * Only stops if the adapter supports southbound communication.
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

        boolean disconnectSuccess = true;
        try {
            invokeStop(ProtocolAdapterConnectionDirection.Southbound);
        } catch (final Exception e) {
            LOGGER.warn("Error during southbound disconnect for adapter '{}'.", getAdapterId(), e);
            // Continue anyway — we want to reach Disconnected state
            disconnectSuccess = false;
        }

        // Disconnecting → Disconnected
        final boolean transitionSuccess = transitionSouthboundConnectionTo(ProtocolAdapterConnectionState.Disconnected)
                .status()
                .isSuccess();
        return disconnectSuccess && transitionSuccess;
    }

    // ===== Service Lifecycle Hooks =====

    /**
     * Start polling for this adapter.
     * Called after all connections are established during {@link #start()}.
     */
    protected void startPolling() {
        if (isBatchPolling()) {
            LOGGER.debug("Schedule batch polling for protocol adapter with id '{}'", getId());
            final PerAdapterSampler sampler = new PerAdapterSampler(this, eventService, tagManager);
            protocolAdapterPollingService.schedulePolling(sampler);
        }

        if (isPolling()) {
            config.getTags().forEach(tag -> {
                final PerContextSampler sampler = new PerContextSampler(
                        this,
                        new PollingContextWrapper(
                                "unused",
                                tag.getName(),
                                MessageHandlingOptions.MQTTMessagePerTag,
                                false,
                                false,
                                List.of(),
                                1,
                                -1L),
                        eventService,
                        tagManager);
                protocolAdapterPollingService.schedulePolling(sampler);
            });
        }
    }

    /**
     * Stop polling for this adapter.
     * Called before disconnecting connections during {@link #stop(boolean)}.
     */
    protected void stopPolling() {
        if (isPolling() || isBatchPolling()) {
            LOGGER.debug("Stopping polling for protocol adapter with id '{}'", getId());
            protocolAdapterPollingService.stopPollingForAdapterInstance(getAdapter());
        }
    }

    /**
     * Start writing for this adapter.
     * Called after all connections are established during {@link #start()}.
     */
    protected void startWriting() {
        if (!protocolAdapterWritingService.writingEnabled()) {
            return;
        }
        if (!(getAdapter() instanceof WritingProtocolAdapter writingAdapter)) {
            return;
        }
        LOGGER.debug("Start writing for protocol adapter with id '{}'", getId());
        final var writingContexts = getSouthboundMappings().stream()
                .map(InternalWritingContextImpl::new)
                .collect(Collectors.<InternalWritingContext>toList());
        try {
            protocolAdapterWritingService
                    .startWritingAsync(writingAdapter, metricsService, writingContexts)
                    .get();
        } catch (final Exception e) {
            LOGGER.error("Failed to start writing for adapter with id '{}'.", getId(), e);
        }
    }

    /**
     * Stop writing for this adapter.
     * Called before disconnecting connections during {@link #stop(boolean)}.
     */
    protected void stopWriting() {
        if (!(getAdapter() instanceof WritingProtocolAdapter writingAdapter)) {
            return;
        }
        LOGGER.debug("Stopping writing for protocol adapter with id '{}'", getId());
        final var writingContexts = getSouthboundMappings().stream()
                .map(mapping -> (InternalWritingContext) new InternalWritingContextImpl(mapping))
                .toList();
        protocolAdapterWritingService.stopWriting(writingAdapter, writingContexts);
    }

    /**
     * Create and subscribe tag consumers for northbound data flow.
     * Called during {@link #start()} after connections are established.
     */
    protected void createAndSubscribeTagConsumers() {
        getNorthboundMappings().forEach(northboundMapping -> {
            final NorthboundTagConsumer consumer =
                    northboundConsumerFactory.build(this, northboundMapping, metricsService);
            tagManager.addConsumer(consumer);
            consumers.add(consumer);
        });
    }

    /**
     * Remove tag consumers. Called during {@link #stop(boolean)}.
     */
    protected void removeTagConsumers() {
        consumers.forEach(tagManager::removeConsumer);
        consumers.clear();
    }

    // ===== Adapter Invocation Helpers =====

    /**
     * Invoke {@link ProtocolAdapter#start(ProtocolAdapterConnectionDirection, ProtocolAdapterStartInput, ProtocolAdapterStartOutput)}
     * and block until the adapter signals success or failure via the output callbacks.
     *
     * @param direction the connection direction (northbound or southbound)
     * @throws Exception if the adapter signals failure or throws
     */
    private void invokeStart(final @NotNull ProtocolAdapterConnectionDirection direction) throws Exception {
        final ProtocolAdapterStartInputImpl input = new ProtocolAdapterStartInputImpl(moduleServices);
        final ProtocolAdapterStartOutputImpl output = new ProtocolAdapterStartOutputImpl();
        try {
            adapter.start(direction, input, output);
        } catch (final Exception e) {
            if (!output.getStartFuture().isDone()) {
                output.getStartFuture().completeExceptionally(e);
            }
        }
        try {
            output.getStartFuture().get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            throw cause instanceof final Exception exception ? exception : new RuntimeException(cause);
        }
    }

    /**
     * Invoke {@link ProtocolAdapter#stop(ProtocolAdapterConnectionDirection, ProtocolAdapterStopInput, ProtocolAdapterStopOutput)}
     * and block until the adapter signals success or failure via the output callbacks.
     *
     * @param direction the connection direction (northbound or southbound)
     * @throws Exception if the adapter signals failure or throws
     */
    private void invokeStop(final @NotNull ProtocolAdapterConnectionDirection direction) throws Exception {
        final ProtocolAdapterStopInputImpl input = new ProtocolAdapterStopInputImpl();
        final ProtocolAdapterStopOutputImpl output = new ProtocolAdapterStopOutputImpl();
        try {
            adapter.stop(direction, input, output);
        } catch (final Exception e) {
            if (!output.getOutputFuture().isDone()) {
                output.getOutputFuture().completeExceptionally(e);
            }
        }
        try {
            output.getOutputFuture().get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            throw cause instanceof final Exception exception ? exception : new RuntimeException(cause);
        }
    }

    // ===== FSM Transition Methods =====

    /**
     * Transition the adapter state to the given new state.
     * On successful transitions, all registered {@link ProtocolAdapterStateChangeListener}s are notified.
     *
     * @param newState the target state
     * @return the transition response indicating success, failure, or no change
     */
    public synchronized @NotNull ProtocolAdapterTransitionResponse transitionTo(
            final @NotNull ProtocolAdapterRuntimeState newState) {
        final ProtocolAdapterRuntimeState fromState = state;
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
                LOGGER.warn(
                        "Protocol adapter '{}' failed to transition from {} to {}.", getAdapterId(), fromState, state);
            case NotChanged -> LOGGER.warn("Protocol adapter '{}' state {} is unchanged.", getAdapterId(), state);
        }
        return response;
    }

    /**
     * Transition the southbound connection state to the given new state.
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
            final @NotNull ProtocolAdapterRuntimeState fromState, final @NotNull ProtocolAdapterRuntimeState toState) {
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
