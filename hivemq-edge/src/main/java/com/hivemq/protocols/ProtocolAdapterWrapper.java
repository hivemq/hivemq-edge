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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ProtocolAdapterWrapper {

    private static final Logger log = LoggerFactory.getLogger(ProtocolAdapterWrapper.class);
    private static final int WRITE_FUTURE_COMPLETE_DELAY = 300;
    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull ProtocolAdapter adapter;
    private final @NotNull ProtocolAdapterFactory<?> adapterFactory;
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterStateImpl protocolAdapterState;
    private final @NotNull InternalProtocolAdapterWritingService protocolAdapterWritingService;
    private final @NotNull ProtocolAdapterPollingService protocolAdapterPollingService;
    private final @NotNull ProtocolAdapterConfig config;
    private final @NotNull NorthboundConsumerFactory northboundConsumerFactory;
    private final @NotNull TagManager tagManager;
    private final List<NorthboundTagConsumer> consumers = Collections.synchronizedList(new ArrayList<>());
    private final AtomicReference<CompletableFuture<Void>> startFutureRef = new AtomicReference<>(null);
    private final AtomicReference<CompletableFuture<Void>> stopFutureRef = new AtomicReference<>(null);
    private final AtomicReference<OperationState> operationState = new AtomicReference<>(OperationState.IDLE);
    private final AtomicReference<ScheduledExecutorService> schedulerRef = new AtomicReference<>(null);
    private final AtomicReference<ScheduledFuture<?>> schedulerFutureRef = new AtomicReference<>(null);
    protected @Nullable Long lastStartAttemptTime;

    public ProtocolAdapterWrapper(
            final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService,
            final @NotNull InternalProtocolAdapterWritingService protocolAdapterWritingService,
            final @NotNull ProtocolAdapterPollingService protocolAdapterPollingService,
            final @NotNull ProtocolAdapterConfig config,
            final @NotNull ProtocolAdapter adapter,
            final @NotNull ProtocolAdapterFactory<?> adapterFactory,
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterStateImpl protocolAdapterState,
            final @NotNull NorthboundConsumerFactory northboundConsumerFactory,
            final @NotNull TagManager tagManager) {
        this.protocolAdapterWritingService = protocolAdapterWritingService;
        this.protocolAdapterPollingService = protocolAdapterPollingService;
        this.protocolAdapterMetricsService = protocolAdapterMetricsService;
        this.adapter = adapter;
        this.adapterFactory = adapterFactory;
        this.adapterInformation = adapterInformation;
        this.protocolAdapterState = protocolAdapterState;
        this.config = config;
        this.northboundConsumerFactory = northboundConsumerFactory;
        this.tagManager = tagManager;
    }

    public @NotNull CompletableFuture<Void> startAsync(
            final boolean writingEnabled,
            final @NotNull ModuleServices moduleServices) {
        final var existingStartFuture = getOngoingOperationIfPresent(operationState.get(), OperationState.STARTING);
        if (existingStartFuture != null) {
            return existingStartFuture;
        }
        // Try to atomically transition from IDLE to STARTING
        if (!operationState.compareAndSet(OperationState.IDLE, OperationState.STARTING)) {
            // State changed between check and set, retry
            return startAsync(writingEnabled, moduleServices);
        }
        // Clear shutdown flag when starting adapter to allow state changes
        protocolAdapterState.clearShuttingDown();
        initStartAttempt();
        final var output = new ProtocolAdapterStartOutputImpl();
        final var input = new ProtocolAdapterStartInputImpl(moduleServices);
        final var startFuture = CompletableFuture
                .supplyAsync(() -> {
                    try {
                        adapter.start(input, output);
                    } catch (final Throwable throwable) {
                        output.getStartFuture().completeExceptionally(throwable);
                    }
                    return output.getStartFuture();
                })
                .thenCompose(Function.identity()).handle((ignored, error) -> {
                    if (error != null) {
                        log.error("Error starting adapter", error);
                        stopAfterFailedStart();
                        protocolAdapterState.setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STOPPED);
                        //we still return the initial error since that's the most significant information
                        return CompletableFuture.failedFuture(error);
                    } else {
                        protocolAdapterState.setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STARTED);
                        return attemptStartingConsumers(writingEnabled,
                                moduleServices.eventService()).handle((success, startException) -> {
                            if (startException == null) {
                                if (success) {
                                    log.debug("Successfully started adapter with id {}", adapter.getId());
                                } else {
                                    log.debug("Partially started adapter with id {}", adapter.getId());
                                }
                            } else {
                                log.error("Failed to start adapter with id {}", adapter.getId(), startException);
                                stopAfterFailedStart();
                                //we still return the initial error since that's the most significant information
                                protocolAdapterState.setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STOPPED);
                                throw new RuntimeException("Failed to start adapter with id " + adapter.getId(),
                                        startException);
                            }
                            return success;
                        });
                    }
                }).thenApply(ignored -> (Void) null).whenComplete((result, throwable) -> {
                    //always clean up state
                    startFutureRef.set(null);
                    operationState.set(OperationState.IDLE);
                });

        startFutureRef.set(startFuture);
        return startFuture;
    }

    private void stopAfterFailedStart() {
        log.warn("Stopping adapter with id {} after a failed start", adapter.getId());
        // Mark as shutting down to prevent race conditions
        protocolAdapterState.markShuttingDown();
        final var stopInput = new ProtocolAdapterStopInputImpl();
        final var stopOutput = new ProtocolAdapterStopOutputImpl();
        stopPolling(protocolAdapterPollingService);
        stopWriting(protocolAdapterWritingService);
        try {
            adapter.stop(stopInput, stopOutput);
        } catch (final Throwable throwable) {
            log.error("Stopping adapter after a start error failed", throwable);
        }
        //force waiting for the stop future to complete, we are in a separate thread so no harm caused
        try {
            stopOutput.getOutputFuture().get();
        } catch (final Throwable throwable) {
            log.error("Stopping adapter after a start error failed", throwable);
        }
    }

    private void cleanUpScheduler() {
        Optional.ofNullable(schedulerFutureRef.getAndSet(null))
                .filter(scheduledFuture -> !scheduledFuture.isCancelled())
                .ifPresent(scheduledFuture -> scheduledFuture.cancel(true));
        Optional.ofNullable(schedulerRef.getAndSet(null)).ifPresent(ExecutorService::shutdownNow);
    }

    private @NotNull CompletableFuture<Boolean> attemptStartingConsumers(
            final boolean writingEnabled,
            final @NotNull EventService eventService) {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            //Adapter started successfully, now start the consumers
            createAndSubscribeTagConsumer();
            startPolling(protocolAdapterPollingService, eventService);

            if (writingEnabled && isWriting()) {
                final AtomicBoolean futureCompleted = new AtomicBoolean(false);
                final AtomicBoolean firstCallToStatusListener = new AtomicBoolean(true);
                cleanUpScheduler();
                schedulerRef.set(Executors.newSingleThreadScheduledExecutor(runnable -> {
                    final Thread thread = new Thread(runnable);
                    thread.setName("adapter-start-timeout-thread-" + adapter.getId());
                    thread.setDaemon(false);
                    return thread;
                }));
                schedulerFutureRef.set(schedulerRef.get().schedule(() -> {
                    if (futureCompleted.compareAndSet(false, true)) {
                        log.error("Protocol adapter with id {} start writing failed because of timeout.",
                                adapter.getId());
                        future.complete(false);
                    }
                }, WRITE_FUTURE_COMPLETE_DELAY, TimeUnit.SECONDS));
                future.thenAccept(success -> cleanUpScheduler());
                final var initialConnectionStatus = protocolAdapterState.getConnectionStatus();
                protocolAdapterState.setConnectionStatusListener(status -> {
                    if (firstCallToStatusListener.compareAndSet(true, false) && status == initialConnectionStatus) {
                        log.trace("Ignoring initial status {} for adapter {}",
                                initialConnectionStatus,
                                adapter.getId());
                        return;
                    }

                    // Process all other statuses (including ERROR on first call, or any subsequent status changes)
                    switch (status) {
                        case CONNECTED, STATELESS -> {
                            try {
                                if (startWritingAsync(protocolAdapterWritingService).get()) {
                                    log.info("Successfully started adapter with id {}", adapter.getId());
                                    if (futureCompleted.compareAndSet(false, true)) {
                                        future.complete(true);
                                    }
                                } else {
                                    log.error(
                                            "Protocol adapter with id {} start writing failed as data hub is not available.",
                                            adapter.getId());
                                    if (futureCompleted.compareAndSet(false, true)) {
                                        future.complete(false);
                                    }
                                }
                            } catch (final Exception e) {
                                log.error("Failed to start writing for adapter with id {}.", adapter.getId(), e);
                                if (futureCompleted.compareAndSet(false, true)) {
                                    future.complete(true);
                                }
                            }
                        }
                        case ERROR, DISCONNECTED, UNKNOWN -> {
                            log.error("Failed to start writing for adapter with id {} because the status is {}.",
                                    adapter.getId(),
                                    status);
                            if (futureCompleted.compareAndSet(false, true)) {
                                future.complete(false);
                            }
                        }
                    }
                });
            } else {
                log.debug("Protocol adapter with id {} does not support Southbound.", adapter.getId());
                future.complete(true);
            }
        } catch (final Throwable e) {
            log.error("Protocol adapter with id {} failed to be started.", adapter.getId());
            future.completeExceptionally(e);
        }
        return future;
    }

    public @NotNull CompletableFuture<Void> stopAsync(final boolean destroy) {
        final var existingStopFuture = getOngoingOperationIfPresent(operationState.get(), OperationState.STOPPING);
        if (existingStopFuture != null) {
            return existingStopFuture;
        }
        cleanUpScheduler();
        // Try to atomically transition from IDLE to STOPPING
        if (!operationState.compareAndSet(OperationState.IDLE, OperationState.STOPPING)) {
            // State changed between check and set, retry
            return stopAsync(destroy);
        }
        // Double-check that no stop future exists
        final var existingFuture = stopFutureRef.get();
        if (existingFuture != null) {
            log.info("Stop operation already in progress for adapter with id '{}', returning existing future", getId());
            return existingFuture;
        }

        // Mark the adapter state as shutting down to prevent race conditions with async operations
        protocolAdapterState.markShuttingDown();

        consumers.forEach(tagManager::removeConsumer);
        final var input = new ProtocolAdapterStopInputImpl();
        final var output = new ProtocolAdapterStopOutputImpl();

        final var stopFuture = new CompletableFuture<Void>();
        stopFutureRef.set(stopFuture);

        final var actualFuture = CompletableFuture
                .supplyAsync(() -> {
                    stopPolling(protocolAdapterPollingService);
                    stopWriting(protocolAdapterWritingService);
                    try {
                        adapter.stop(input, output);
                    } catch (final Throwable throwable) {
                        output.getOutputFuture().completeExceptionally(throwable);
                    }
                    return output.getOutputFuture();
                })
                .thenApply(v -> {
                    stopFutureRef.set(null);
                    protocolAdapterState.setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STOPPED);
                    operationState.set(OperationState.IDLE);
                    if (destroy) {
                        log.info("Destroying adapter with id '{}'", getId());
                        adapter.destroy();
                    }
                    return v;
                })
                .thenCompose(Function.identity())
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        log.info("Stopped adapter with id {}", adapter.getId());
                    } else {
                        log.error("Error stopping adapter with id {}", adapter.getId(), throwable);
                    }
                });

        stopFuture.thenCompose(v -> actualFuture);
        return stopFuture;
    }

    private @Nullable CompletableFuture<Void> getOngoingOperationIfPresent(
            final @NotNull OperationState currentState,
            final @NotNull OperationState targetState) {
        switch (currentState) {
            case STARTING:
                if (targetState == OperationState.STARTING) {
                    // If already starting, return existing future
                    final var existingStartFuture = startFutureRef.get();
                    if (existingStartFuture != null) {
                        log.info(
                                "Start operation already in progress for adapter with id '{}', returning existing future",
                                getId());
                        return existingStartFuture;
                    }
                } else {
                    log.warn("Cannot stop adapter with id '{}' while start operation is in progress", getId());
                    return CompletableFuture.failedFuture(new AdapterStopConflictException(getId()));
                }
                break;
            case STOPPING:
                if (targetState == OperationState.STOPPING) {
                    // If already stopping, return existing future
                    final var existingStopFuture = stopFutureRef.get();
                    if (existingStopFuture != null) {
                        log.info(
                                "Stop operation already in progress for adapter with id '{}', returning existing future",
                                getId());
                        return existingStopFuture;
                    }
                    break;
                }
                // If stopping, return failed future immediately
                log.warn("Cannot start adapter with id '{}' while stop operation is in progress", getId());
                return CompletableFuture.failedFuture(new AdapterStartConflictException(getId()));
            case IDLE:
                // Proceed with start operation
                break;
        }
        return null;
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

    protected void initStartAttempt() {
        lastStartAttemptTime = System.currentTimeMillis();
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

    private void startPolling(
            final @NotNull ProtocolAdapterPollingService protocolAdapterPollingService,
            final @NotNull EventService eventService) {

        if (isBatchPolling()) {
            log.debug("Schedule batch polling for protocol adapter with id '{}'", getId());
            final PerAdapterSampler sampler = new PerAdapterSampler(this, eventService, tagManager);
            protocolAdapterPollingService.schedulePolling(sampler);
        }

        if (isPolling()) {
            config.getTags().forEach(tag -> {
                final PerContextSampler sampler = new PerContextSampler(this,
                        new PollingContextWrapper("unused",
                                tag.getName(),
                                MessageHandlingOptions.MQTTMessagePerTag,
                                false,
                                false,
                                List.of(),
                                1,
                                -1),
                        eventService,
                        tagManager);
                protocolAdapterPollingService.schedulePolling(sampler);
            });
        }
    }

    private void stopPolling(
            final @NotNull ProtocolAdapterPollingService protocolAdapterPollingService) {
        if (isPolling() || isBatchPolling()) {
            log.debug("Stopping polling for protocol adapter with id '{}'", getId());
            protocolAdapterPollingService.stopPollingForAdapterInstance(getAdapter());
        }
    }

    private @NotNull CompletableFuture<Boolean> startWritingAsync(
            final @NotNull InternalProtocolAdapterWritingService protocolAdapterWritingService) {
        log.debug("Start writing for protocol adapter with id '{}'", getId());

        final var southboundMappings = getSouthboundMappings();
        final var writingContexts = southboundMappings.stream()
                .map(InternalWritingContextImpl::new)
                .collect(Collectors.<InternalWritingContext>toList());

        return protocolAdapterWritingService.startWritingAsync((WritingProtocolAdapter) getAdapter(),
                getProtocolAdapterMetricsService(),
                writingContexts);
    }

    private void stopWriting(final @NotNull InternalProtocolAdapterWritingService protocolAdapterWritingService) {
        //no check for 'writing is enabled', as we have to stop it anyway since the license could have been removed in the meantime.
        if (isWriting()) {
            log.debug("Stopping writing for protocol adapter with id '{}'", getId());
            final var writingContexts = getSouthboundMappings().stream()
                    .map(mapping -> (InternalWritingContext) new InternalWritingContextImpl(mapping))
                    .toList();
            protocolAdapterWritingService.stopWriting((WritingProtocolAdapter) getAdapter(), writingContexts);
        }
    }

    private void createAndSubscribeTagConsumer() {
        getNorthboundMappings().stream()
                .map(northboundMapping -> northboundConsumerFactory.build(this,
                        northboundMapping,
                        protocolAdapterMetricsService))
                .forEach(northboundTagConsumer -> {
                    tagManager.addConsumer(northboundTagConsumer);
                    consumers.add(northboundTagConsumer);
                });
    }

    /**
     * Represents the current operation state of the adapter to handle concurrent start/stop operations.
     */
    private enum OperationState {
        IDLE,
        STARTING,
        STOPPING
    }

    /**
     * Exception thrown when attempting to start an adapter while a stop operation is in progress.
     */
    public static class AdapterStartConflictException extends RuntimeException {
        public AdapterStartConflictException(final String adapterId) {
            super("Cannot start adapter '" + adapterId + "' while stop operation is in progress");
        }
    }

    /**
     * Exception thrown when attempting to stop an adapter while a start operation is in progress.
     */
    public static class AdapterStopConflictException extends RuntimeException {
        public AdapterStopConflictException(final String adapterId) {
            super("Cannot stop adapter '" + adapterId + "' while start operation is in progress");
        }
    }
}
