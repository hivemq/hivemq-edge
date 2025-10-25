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
import com.hivemq.fsm.ProtocolAdapterFSM;
import com.hivemq.persistence.mappings.NorthboundMapping;
import com.hivemq.persistence.mappings.SouthboundMapping;
import com.hivemq.protocols.northbound.NorthboundConsumerFactory;
import com.hivemq.protocols.northbound.NorthboundTagConsumer;
import com.hivemq.protocols.northbound.PerAdapterSampler;
import com.hivemq.protocols.northbound.PerContextSampler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ProtocolAdapterWrapper extends ProtocolAdapterFSM {

    private static final Logger log = LoggerFactory.getLogger(ProtocolAdapterWrapper.class);
    private static final long STOP_TIMEOUT_SECONDS = 30;
    private static final @NotNull Consumer<ProtocolAdapterState.ConnectionStatus> CONNECTION_STATUS_NOOP_CONSUMER =
            status -> {
                // Noop - adapter is stopping/stopped
            };

    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull ProtocolAdapter adapter;
    private final @NotNull ProtocolAdapterFactory<?> adapterFactory;
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterStateImpl protocolAdapterState;
    private final @NotNull InternalProtocolAdapterWritingService writingService;
    private final @NotNull ProtocolAdapterPollingService pollingService;
    private final @NotNull ProtocolAdapterConfig config;
    private final @NotNull NorthboundConsumerFactory northboundConsumerFactory;
    private final @NotNull TagManager tagManager;
    private final @NotNull List<NorthboundTagConsumer> consumers;
    private final @NotNull ReentrantLock operationLock;
    private final @NotNull Object adapterLock; // protects underlying adapter start/stop calls
    private final @NotNull ExecutorService sharedAdapterExecutor;
    protected volatile @Nullable Long lastStartAttemptTime;
    private @Nullable CompletableFuture<Void> currentStartFuture;
    private @Nullable CompletableFuture<Void> currentStopFuture;
    private @Nullable Consumer<ProtocolAdapterState.ConnectionStatus> connectionStatusListener;
    private volatile boolean startOperationInProgress;
    private volatile boolean stopOperationInProgress;

    public ProtocolAdapterWrapper(
            final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService,
            final @NotNull InternalProtocolAdapterWritingService writingService,
            final @NotNull ProtocolAdapterPollingService pollingService,
            final @NotNull ProtocolAdapterConfig config,
            final @NotNull ProtocolAdapter adapter,
            final @NotNull ProtocolAdapterFactory<?> adapterFactory,
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterStateImpl protocolAdapterState,
            final @NotNull NorthboundConsumerFactory northboundConsumerFactory,
            final @NotNull TagManager tagManager,
            final @NotNull ExecutorService sharedAdapterExecutor) {
        super(config.getAdapterId());
        this.writingService = writingService;
        this.pollingService = pollingService;
        this.protocolAdapterMetricsService = protocolAdapterMetricsService;
        this.adapter = adapter;
        this.adapterFactory = adapterFactory;
        this.adapterInformation = adapterInformation;
        this.protocolAdapterState = protocolAdapterState;
        this.config = config;
        this.northboundConsumerFactory = northboundConsumerFactory;
        this.tagManager = tagManager;
        this.consumers = new CopyOnWriteArrayList<>();
        this.operationLock = new ReentrantLock();
        this.sharedAdapterExecutor = sharedAdapterExecutor;
        this.adapterLock = new Object();

        if (log.isDebugEnabled()) {
            registerStateTransitionListener(state -> log.debug(
                    "Adapter {} FSM adapter transition: adapter={}, northbound={}, southbound={}",
                    adapter.getId(),
                    state.adapter(),
                    state.northbound(),
                    state.southbound()));
        }
    }

    @Override
    public boolean onStarting() {
        try {
            protocolAdapterState.setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STARTED);
            return true;
        } catch (final Exception e) {
            log.error("Adapter starting failed for adapter {}", adapter.getId(), e);
            return false;
        }
    }

    @Override
    public void onStopping() {
        protocolAdapterState.setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STOPPED);
    }

    @Override
    public boolean startSouthbound() {
        if (!isWriting()) {
            transitionSouthboundState(StateEnum.NOT_SUPPORTED);
            return true;
        }
        log.debug("Start writing for protocol adapter with id '{}'", getId());
        final boolean started = writingService.startWriting((WritingProtocolAdapter) adapter,
                protocolAdapterMetricsService,
                config.getSouthboundMappings()
                        .stream()
                        .map(InternalWritingContextImpl::new)
                        .collect(Collectors.<InternalWritingContext>toList()));
        if (started) {
            log.info("Southbound started for adapter {}", adapter.getId());
            transitionSouthboundState(StateEnum.CONNECTED);
        } else {
            log.error("Southbound start failed for adapter {}", adapter.getId());
            transitionSouthboundState(StateEnum.ERROR);
        }
        return started;
    }

    public @Nullable CompletableFuture<Void> startAsync(final @NotNull ModuleServices moduleServices) {
        operationLock.lock();
        try {
            if (startOperationInProgress) {
                log.info("Start operation already in progress for adapter '{}'", getId());
                return currentStartFuture;
            }
            if (adapterState.get() == AdapterStateEnum.STARTED) {
                log.info("Adapter '{}' is already started, returning success", getId());
                return CompletableFuture.completedFuture(null);
            }
            if (stopOperationInProgress) {
                log.warn("Stop operation in progress for adapter '{}', waiting for it to complete before starting",
                        getId());
                final CompletableFuture<Void> stopFuture = currentStopFuture;
                if (stopFuture != null) {
                    // Wait for stop to complete, then retry start
                    return stopFuture.handle((result, throwable) -> {
                        if (throwable != null) {
                            log.warn("Stop operation failed for adapter '{}', but proceeding with start",
                                    getId(),
                                    throwable);
                        }
                        return null;
                    }).thenCompose(v -> startAsync(moduleServices));
                }
                log.error("Stop operation in progress but currentStopFuture is null for adapter '{}'", getId());
                return CompletableFuture.failedFuture(new IllegalStateException("Cannot start adapter '" +
                        adapter.getId() +
                        "' while stop operation is in progress"));
            }

            startOperationInProgress = true;
            lastStartAttemptTime = System.currentTimeMillis();
            currentStartFuture =
                    CompletableFuture.supplyAsync(startProtocolAdapter(moduleServices), sharedAdapterExecutor)
                            .thenCompose(Function.identity())
                            .thenRun(() -> startConsumers(moduleServices.eventService()).ifPresent(startException -> {
                                log.error("Failed to start adapter with id {}", adapter.getId(), startException);
                                stopProtocolAdapterOnFailedStart();
                                throw new RuntimeException("Failed to start consumers", startException);
                            }))
                            .whenComplete((result, throwable) -> {
                                if (throwable != null) {
                                    log.error("Error starting adapter", throwable);
                                    stopProtocolAdapterOnFailedStart();
                                }
                                operationLock.lock();
                                try {
                                    startOperationInProgress = false;
                                    currentStartFuture = null;
                                } finally {
                                    operationLock.unlock();
                                }
                            });
            return currentStartFuture;
        } finally {
            operationLock.unlock();
        }
    }

    public @Nullable CompletableFuture<Void> stopAsync() {
        operationLock.lock();
        try {
            if (stopOperationInProgress) {
                log.info("Stop operation already in progress for adapter '{}'", getId());
                return currentStopFuture;
            }
            final var currentState = currentState();
            if (currentState.adapter() == AdapterStateEnum.STOPPED) {
                log.info("Adapter '{}' is already stopped, returning success", getId());
                return CompletableFuture.completedFuture(null);
            }
            if (startOperationInProgress) {
                log.warn("Start operation in progress for adapter '{}', waiting for it to complete before stopping",
                        getId());
                final CompletableFuture<Void> startFuture = currentStartFuture;
                if (startFuture != null) {
                    // Wait for start to complete, then retry stop
                    return startFuture.handle((result, throwable) -> {
                        if (throwable != null) {
                            log.warn("Start operation failed for adapter '{}', but proceeding with stop",
                                    getId(),
                                    throwable);
                        }
                        return null;
                    }).thenCompose(v -> stopAsync());
                }
                log.error("Start operation in progress but currentStartFuture is null for adapter '{}'", getId());
                return CompletableFuture.failedFuture(new IllegalStateException("Cannot stop adapter '" +
                        adapter.getId() +
                        "' while start operation is in progress"));
            }

            stopOperationInProgress = true;
            log.debug("Adapter '{}': Creating stop operation future", getId());
            currentStopFuture = CompletableFuture.supplyAsync(this::stopProtocolAdapter, sharedAdapterExecutor)
                    .thenCompose(Function.identity())
                    .whenComplete((result, throwable) -> {
                        log.debug("Adapter '{}': Stop operation completed, starting cleanup", getId());
                        try {
                            adapter.destroy();
                        } catch (final Exception destroyException) {
                            log.error("Error destroying adapter with id {}", adapter.getId(), destroyException);
                        }
                        if (throwable == null) {
                            log.info("Successfully stopped adapter with id '{}' successfully", adapter.getId());
                        } else {
                            log.error("Error stopping adapter with id {}", adapter.getId(), throwable);
                        }
                        operationLock.lock();
                        try {
                            stopOperationInProgress = false;
                            currentStopFuture = null;
                        } finally {
                            operationLock.unlock();
                        }
                    });
            return currentStopFuture;
        } finally {
            operationLock.unlock();
        }
    }

    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapter.getProtocolAdapterInformation();
    }

    public void discoverValues(
            final @NotNull ProtocolAdapterDiscoveryInput input,
            final @NotNull ProtocolAdapterDiscoveryOutput output) {
        adapter.discoverValues(input, output);
    }

    public @NotNull ProtocolAdapterState.ConnectionStatus getConnectionStatus() {
        return protocolAdapterState.getConnectionStatus();
    }

    public @NotNull ProtocolAdapterState.RuntimeStatus getRuntimeStatus() {
        return protocolAdapterState.getRuntimeStatus();
    }

    public void setRuntimeStatus(final @NotNull ProtocolAdapterState.RuntimeStatus runtimeStatus) {
        protocolAdapterState.setRuntimeStatus(runtimeStatus);
    }

    public @Nullable String getErrorMessage() {
        return protocolAdapterState.getLastErrorMessage();
    }

    public @NotNull ProtocolAdapterFactory<?> getAdapterFactory() {
        return adapterFactory;
    }

    public @NotNull ProtocolAdapterInformation getAdapterInformation() {
        return adapterInformation;
    }

    public @NotNull ProtocolSpecificAdapterConfig getConfigObject() {
        return config.getAdapterConfig();
    }

    public @NotNull ProtocolAdapterConfig getConfig() {
        return config;
    }

    public @NotNull List<? extends Tag> getTags() {
        return config.getTags();
    }

    public @Nullable Long getTimeOfLastStartAttempt() {
        return lastStartAttemptTime;
    }

    public @NotNull String getId() {
        return adapter.getId();
    }

    public @NotNull ProtocolAdapter getAdapter() {
        return adapter;
    }

    public @NotNull List<NorthboundMapping> getNorthboundMappings() {
        return config.getNorthboundMappings();
    }

    public @NotNull List<SouthboundMapping> getSouthboundMappings() {
        return config.getSouthboundMappings();
    }

    public @NotNull ProtocolAdapterMetricsService getProtocolAdapterMetricsService() {
        return protocolAdapterMetricsService;
    }

    public void setErrorConnectionStatus(final @NotNull Throwable exception, final @Nullable String errorMessage) {
        protocolAdapterState.setErrorConnectionStatus(exception, errorMessage);
    }

    public boolean isWriting() {
        return adapter instanceof WritingProtocolAdapter;
    }

    public boolean isPolling() {
        return adapter instanceof PollingProtocolAdapter;
    }

    public boolean isBatchPolling() {
        return adapter instanceof BatchPollingProtocolAdapter;
    }

    public void addTagHotReload(final @NotNull Tag tag, final @NotNull EventService eventService) {
        // Wait for any in-progress operations before proceeding
        waitForOperationsToComplete();

        operationLock.lock();
        try {
            if (startOperationInProgress || stopOperationInProgress) {
                throw new IllegalStateException("Cannot hot-reload tag for adapter '" +
                        getId() +
                        "': operation started during wait");
            }

            // Update config with new tag regardless of adapter state
            final List<Tag> updatedTags = new ArrayList<>(config.getTags());
            updatedTags.add(tag);
            config.setTags(updatedTags);
            if (adapterState.get() != AdapterStateEnum.STARTED) {
                log.debug("Adapter '{}' not started yet, only updating config for tag '{}'", getId(), tag.getName());
                return;
            }
            if (isPolling()) {
                log.debug("Starting polling for new tag '{}' on adapter '{}'", tag.getName(), getId());
                pollingService.schedulePolling(new PerContextSampler(this,
                        new PollingContextWrapper("unused",
                                tag.getName(),
                                MessageHandlingOptions.MQTTMessagePerTag,
                                false,
                                false,
                                List.of(),
                                1,
                                -1),
                        eventService,
                        tagManager));
            }
            log.info("Successfully added tag '{}' to adapter '{}' via hot-reload", tag.getName(), getId());
        } finally {
            operationLock.unlock();
        }
    }

    public void updateMappingsHotReload(
            final @Nullable List<NorthboundMapping> northboundMappings,
            final @Nullable List<SouthboundMapping> southboundMappings,
            final @NotNull EventService eventService) {
        waitForOperationsToComplete();

        operationLock.lock();
        try {
            if (startOperationInProgress || stopOperationInProgress) {
                throw new IllegalStateException("Cannot hot-reload mappings for adapter '" +
                        getId() +
                        "': operation started during wait");
            }

            if (northboundMappings != null) {
                config.setNorthboundMappings(northboundMappings);
            }
            if (southboundMappings != null) {
                config.setSouthboundMappings(southboundMappings);
            }
            if (adapterState.get() != AdapterStateEnum.STARTED) {
                log.debug("Adapter '{}' not started yet, only updating config for mappings", getId());
                return;
            }

            // Stop existing consumers and polling
            if (northboundMappings != null) {
                log.debug("Stopping existing consumers and polling for adapter '{}'", getId());
                consumers.forEach(tagManager::removeConsumer);
                consumers.clear();

                // Stop polling to restart with new mappings
                stopPolling();

                log.debug("Updating northbound mappings for adapter '{}'", getId());
                northboundMappings.stream()
                        .map(mapping -> northboundConsumerFactory.build(this, mapping, protocolAdapterMetricsService))
                        .forEach(consumer -> {
                            tagManager.addConsumer(consumer);
                            consumers.add(consumer);
                        });

                // Restart polling with new consumers
                log.debug("Restarting polling for adapter '{}'", getId());
                if (isBatchPolling()) {
                    log.debug("Schedule batch polling for protocol adapter with id '{}'", getId());
                    pollingService.schedulePolling(new PerAdapterSampler(this, eventService, tagManager));
                }
                if (isPolling()) {
                    config.getTags()
                            .forEach(tag -> pollingService.schedulePolling(new PerContextSampler(this,
                                    new PollingContextWrapper("unused",
                                            tag.getName(),
                                            MessageHandlingOptions.MQTTMessagePerTag,
                                            false,
                                            false,
                                            List.of(),
                                            1,
                                            -1),
                                    eventService,
                                    tagManager)));
                }
            }

            if (southboundMappings != null && isWriting()) {
                log.debug("Updating southbound mappings for adapter '{}'", getId());
                final StateEnum currentSouthboundState = currentState().southbound();
                final boolean wasConnected = (currentSouthboundState == StateEnum.CONNECTED);
                if (wasConnected) {
                    log.debug("Stopping southbound for adapter '{}' before hot-reload", getId());
                    stopWriting();
                    log.debug("Restarting southbound for adapter '{}' after hot-reload", getId());
                    startSouthbound();
                }
            }
            log.info("Successfully updated mappings for adapter '{}' via hot-reload", getId());
        } finally {
            operationLock.unlock();
        }
    }

    private void waitForOperationsToComplete() {
        CompletableFuture<Void> futureToWait = null;
        operationLock.lock();
        try {
            if (startOperationInProgress) {
                log.debug("Adapter '{}': Waiting for start operation to complete before hot-reload", getId());
                futureToWait = currentStartFuture;
            } else if (stopOperationInProgress) {
                log.debug("Adapter '{}': Waiting for stop operation to complete before hot-reload", getId());
                futureToWait = currentStopFuture;
            }
        } finally {
            operationLock.unlock();
        }

        if (futureToWait != null) {
            try {
                // Wait with a timeout to prevent indefinite blocking
                futureToWait.get(30, TimeUnit.SECONDS);
                log.debug("Adapter '{}': Operation completed, proceeding with hot-reload", getId());
            } catch (final TimeoutException e) {
                log.warn("Adapter '{}': Operation did not complete within 30 seconds, proceeding with hot-reload anyway",
                        getId());
            } catch (final Exception e) {
                log.warn("Adapter '{}': Operation completed with error, but proceeding with hot-reload", getId(), e);
            }
        }
    }

    private void cleanupConnectionStatusListener() {
        final Consumer<ProtocolAdapterState.ConnectionStatus> listenerToClean = connectionStatusListener;
        if (listenerToClean != null) {
            connectionStatusListener = null;
            protocolAdapterState.setConnectionStatusListener(CONNECTION_STATUS_NOOP_CONSUMER);
        }
    }

    private @NotNull Optional<Throwable> startConsumers(final @NotNull EventService eventService) {
        try {
            // create/subscribe tag consumer
            config.getNorthboundMappings()
                    .stream()
                    .map(mapping -> northboundConsumerFactory.build(this, mapping, protocolAdapterMetricsService))
                    .forEach(northboundTagConsumer -> {
                        tagManager.addConsumer(northboundTagConsumer);
                        consumers.add(northboundTagConsumer);
                    });

            // start polling
            if (isBatchPolling()) {
                log.debug("Schedule batch polling for protocol adapter with id '{}'", getId());
                pollingService.schedulePolling(new PerAdapterSampler(this, eventService, tagManager));
            }
            if (isPolling()) {
                config.getTags()
                        .forEach(tag -> pollingService.schedulePolling(new PerContextSampler(this,
                                new PollingContextWrapper("unused",
                                        tag.getName(),
                                        MessageHandlingOptions.MQTTMessagePerTag,
                                        false,
                                        false,
                                        List.of(),
                                        1,
                                        -1),
                                eventService,
                                tagManager)));
            }

            // FSM's accept() method handles:
            // 1. Transitioning northbound adapter
            // 2. Triggering startSouthbound() when CONNECTED (only for writing adapters)
            // For non-writing adapters that are only polling, southbound is not applicable
            // but we still need to track northbound connection status
            connectionStatusListener = this;
            protocolAdapterState.setConnectionStatusListener(connectionStatusListener);
            return Optional.empty();
        } catch (final Throwable e) {
            log.error("Protocol adapter start failed", e);
            return Optional.of(e);
        }
    }

    private void stopPolling() {
        if (isPolling() || isBatchPolling()) {
            log.debug("Stopping polling for protocol adapter with id '{}'", getId());
            try {
                pollingService.stopPollingForAdapterInstance(adapter);
                log.debug("Polling stopped successfully for adapter '{}'", getId());
            } catch (final Exception e) {
                log.error("Error stopping polling for adapter '{}'", getId(), e);
            }
        }
    }

    private void stopWriting() {
        if (isWriting()) {
            log.debug("Stopping writing for protocol adapter with id '{}'", getId());
            try {
                // Transition southbound state to indicate shutdown in progress
                final StateEnum currentSouthboundState = currentState().southbound();
                if (currentSouthboundState == StateEnum.CONNECTED) {
                    transitionSouthboundState(StateEnum.DISCONNECTING);
                }
                writingService.stopWriting((WritingProtocolAdapter) adapter,
                        config.getSouthboundMappings()
                                .stream()
                                .map(mapping -> (InternalWritingContext) new InternalWritingContextImpl(mapping))
                                .toList());
                if (currentSouthboundState == StateEnum.CONNECTED ||
                        currentSouthboundState == StateEnum.DISCONNECTING) {
                    transitionSouthboundState(StateEnum.DISCONNECTED);
                }
                log.debug("Writing stopped successfully for adapter '{}'", getId());
            } catch (final IllegalStateException stateException) {
                // State transition failed, log but continue cleanup
                log.warn("State transition failed while stopping writing for adapter '{}': {}",
                        getId(),
                        stateException.getMessage());
            } catch (final Exception e) {
                log.error("Error stopping writing for adapter '{}'", getId(), e);
            }
        }
    }

    private @NotNull Supplier<@NotNull CompletableFuture<Void>> startProtocolAdapter(
            final @NotNull ModuleServices moduleServices) {
        return () -> {
            startAdapter(); // start FSM
            final ProtocolAdapterStartOutputImpl output = new ProtocolAdapterStartOutputImpl();
            synchronized (adapterLock) {
                log.debug("Adapter '{}': Calling adapter.start() in thread '{}'",
                        getId(),
                        Thread.currentThread().getName());
                try {
                    adapter.start(new ProtocolAdapterStartInputImpl(moduleServices), output);
                } catch (final Throwable t) {
                    output.getStartFuture().completeExceptionally(t);
                }
            }
            return output.getStartFuture();
        };
    }

    private @NotNull CompletableFuture<Void> stopProtocolAdapter() {
        log.debug("Adapter '{}': Stop operation executing in thread '{}'",
                adapter.getId(),
                Thread.currentThread().getName());
        return performStopOperations();
    }

    private void stopProtocolAdapterOnFailedStart() {
        log.warn("Stopping adapter with id {} after a failed start", adapter.getId());
        final CompletableFuture<Void> stopFuture = performStopOperations();

        // Wait synchronously for stop to complete with timeout
        try {
            stopFuture.get(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (final TimeoutException e) {
            log.error("Timeout waiting for adapter {} to stop after failed start", adapter.getId());
        } catch (final Throwable throwable) {
            log.error("Stopping adapter after a start error failed", throwable);
        }

        // Always destroy adapter after failed start
        try {
            adapter.destroy();
        } catch (final Exception destroyException) {
            log.error("Error destroying adapter with id {} after failed start", adapter.getId(), destroyException);
        }
    }

    private @NotNull CompletableFuture<Void> performStopOperations() {
        stopAdapter();
        cleanupConnectionStatusListener();
        // Clean up consumers
        try {
            consumers.forEach(tagManager::removeConsumer);
            consumers.clear();
            log.debug("Adapter '{}': Consumers cleaned up successfully", getId());
        } catch (final Exception e) {
            log.error("Adapter '{}': Error cleaning up consumers", getId(), e);
        }

        stopPolling();
        stopWriting();

        // Initiate adapter stop
        final var output = new ProtocolAdapterStopOutputImpl();
        synchronized (adapterLock) {
            log.debug("Adapter '{}': Calling adapter.stop() in thread '{}'", getId(), Thread.currentThread().getName());
            try {
                adapter.stop(new ProtocolAdapterStopInputImpl(), output);
            } catch (final Throwable throwable) {
                log.error("Adapter '{}': Exception during adapter.stop()", adapter.getId(), throwable);
                output.getOutputFuture().completeExceptionally(throwable);
            }
        }

        log.debug("Adapter '{}': Waiting for stop output future", adapter.getId());
        return output.getOutputFuture();
    }
}
