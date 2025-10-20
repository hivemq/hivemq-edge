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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ProtocolAdapterWrapper extends ProtocolAdapterFSM {

    private static final Logger log = LoggerFactory.getLogger(ProtocolAdapterWrapper.class);

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
    private final List<NorthboundTagConsumer> consumers = new CopyOnWriteArrayList<>();
    private final AtomicReference<CompletableFuture<Void>> startFutureRef = new AtomicReference<>(null);
    private final AtomicReference<CompletableFuture<Void>> stopFutureRef = new AtomicReference<>(null);
    protected volatile @Nullable Long lastStartAttemptTime;

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
        super(config.getAdapterId());
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
        if (log.isDebugEnabled()) {
            registerStateTransitionListener(state -> log.debug(
                    "Adapter {} FSM state transition: adapter={}, northbound={}, southbound={}",
                    adapter.getId(),
                    state.state(),
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

        final boolean started = startWriting(protocolAdapterWritingService);
        if (started) {
            log.info("Southbound started for adapter {}", adapter.getId());
            transitionSouthboundState(StateEnum.CONNECTED);
        } else {
            log.error("Southbound start failed for adapter {}", adapter.getId());
            transitionSouthboundState(StateEnum.ERROR);
        }
        return started;
    }

    public @NotNull CompletableFuture<Void> startAsync(
            final boolean writingEnabled,
            final @NotNull ModuleServices moduleServices) {

        // Atomically check state and claim the operation in a single step
        while (true) {
            final var existingFuture = startFutureRef.get();

            // If there's already a start operation in progress, return it
            if (existingFuture != null && !existingFuture.isDone()) {
                log.info("Start operation already in progress for adapter '{}'", getId());
                return existingFuture;
            }

            // Check if adapter is already started - make start operation idempotent
            final var currentState = currentState();
            if (currentState.state() == AdapterStateEnum.STARTED) {
                log.info("Adapter '{}' is already started, returning success", getId());
                return CompletableFuture.completedFuture(null);
            }

            // Check if stop operation is in progress
            final var stopFuture = stopFutureRef.get();
            if (stopFuture != null && !stopFuture.isDone()) {
                log.warn("Cannot start adapter '{}' while stop operation is in progress", getId());
                return CompletableFuture.failedFuture(new IllegalStateException("Cannot start adapter '" +
                        getId() +
                        "' while stop operation is in progress"));
            }

            // Create a placeholder future and try to claim ownership atomically
            // This ensures only one thread proceeds to actually start the adapter
            final CompletableFuture<Void> placeholderFuture = new CompletableFuture<>();

            if (!startFutureRef.compareAndSet(existingFuture, placeholderFuture)) {
                // CAS failed - another thread won the race, loop back to get their future
                continue;
            }

            // We won the CAS - we now own the start operation
            // Create the actual future and execute the start sequence
            initStartAttempt();
            final var output = new ProtocolAdapterStartOutputImpl();
            final var input = new ProtocolAdapterStartInputImpl(moduleServices);

            final var startFuture = CompletableFuture.supplyAsync(() -> {
                // Signal FSM to start (calls onStarting() internally)
                startAdapter();

                try {
                    adapter.start(input, output);
                } catch (final Throwable throwable) {
                    output.getStartFuture().completeExceptionally(throwable);
                }
                return output.getStartFuture();
            }).thenCompose(Function.identity()).handle((ignored, error) -> {
                if (error != null) {
                    log.error("Error starting adapter", error);
                    stopAfterFailedStart();
                    //we still return the initial error since that's the most significant information
                    return CompletableFuture.failedFuture(error);
                } else {
                    return attemptStartingConsumers(writingEnabled,
                            moduleServices.eventService()).map(startException -> {
                        log.error("Failed to start adapter with id {}", adapter.getId(), startException);
                        stopAfterFailedStart();
                        //we still return the initial error since that's the most significant information
                        return CompletableFuture.failedFuture(startException);
                    }).orElseGet(() -> CompletableFuture.completedFuture(null));
                }
            }).thenApply(ignored -> (Void) null).whenComplete((result, throwable) -> startFutureRef.set(null));

            // Replace the placeholder with the actual future and complete the placeholder to unblock any waiters
            startFutureRef.set(startFuture);
            startFuture.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    placeholderFuture.completeExceptionally(throwable);
                } else {
                    placeholderFuture.complete(result);
                }
            });

            return placeholderFuture;
        }
    }

    private void stopAfterFailedStart() {
        log.warn("Stopping adapter with id {} after a failed start", adapter.getId());
        final var stopInput = new ProtocolAdapterStopInputImpl();
        final var stopOutput = new ProtocolAdapterStopOutputImpl();

        // Transition FSM state back to STOPPED
        stopAdapter();

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
        // Always destroy to clean up resources after failed start
        try {
            log.info("Destroying adapter with id '{}' after failed start to release all resources", getId());
            adapter.destroy();
        } catch (final Exception destroyException) {
            log.error("Error destroying adapter with id {} after failed start", adapter.getId(), destroyException);
        }
    }

    private @NotNull Optional<Throwable> attemptStartingConsumers(
            final boolean writingEnabled,
            final @NotNull EventService eventService) {
        try {
            //Adapter started successfully, now start the consumers
            createAndSubscribeTagConsumer();
            startPolling(protocolAdapterPollingService, eventService);

            // Wire connection status events to FSM for all adapters
            // FSM's accept() method handles:
            // 1. Transitioning northbound state
            // 2. Triggering startSouthbound() when CONNECTED (only for writing adapters)
            protocolAdapterState.setConnectionStatusListener(status -> {
                this.accept(status);

                // For non-writing adapters that are only polling, southbound is not applicable
                // but we still need to track northbound connection status
            });
        } catch (final Throwable e) {
            log.error("Protocol adapter start failed", e);
            return Optional.of(e);
        }
        return Optional.empty();
    }

    public @NotNull CompletableFuture<Void> stopAsync(final boolean destroy) {
        // Atomically check state and claim the operation in a single step
        while (true) {
            final var existingFuture = stopFutureRef.get();

            // If there's already a stop operation in progress, return it
            if (existingFuture != null && !existingFuture.isDone()) {
                log.info("Stop operation already in progress for adapter '{}'", getId());
                return existingFuture;
            }

            // Check if adapter is already stopped - make stop operation idempotent
            final var currentState = currentState();
            if (currentState.state() == AdapterStateEnum.STOPPED) {
                log.info("Adapter '{}' is already stopped, returning success", getId());
                return CompletableFuture.completedFuture(null);
            }

            // Check if start operation is in progress
            final var startFuture = startFutureRef.get();
            if (startFuture != null && !startFuture.isDone()) {
                log.warn("Cannot stop adapter '{}' while start operation is in progress", getId());
                return CompletableFuture.failedFuture(new IllegalStateException("Cannot stop adapter '" +
                        getId() +
                        "' while start operation is in progress"));
            }

            // Create a placeholder future and try to claim ownership atomically
            // This ensures only one thread proceeds to actually stop the adapter
            final CompletableFuture<Void> placeholderFuture = new CompletableFuture<>();

            if (!stopFutureRef.compareAndSet(existingFuture, placeholderFuture)) {
                // CAS failed - another thread won the race, loop back to get their future
                continue;
            }

            // We won the CAS - we now own the stop operation
            // Create the actual future and execute the stop sequence
            consumers.forEach(tagManager::removeConsumer);
            final var input = new ProtocolAdapterStopInputImpl();
            final var output = new ProtocolAdapterStopOutputImpl();

            final var stopFuture = CompletableFuture.supplyAsync(() -> {
                // Signal FSM to stop (calls onStopping() internally)
                stopAdapter();

                stopPolling(protocolAdapterPollingService);
                stopWriting(protocolAdapterWritingService);
                try {
                    adapter.stop(input, output);
                } catch (final Throwable throwable) {
                    output.getOutputFuture().completeExceptionally(throwable);
                }
                return output.getOutputFuture();
            }).thenCompose(Function.identity()).whenComplete((result, throwable) -> {
                // Always call destroy() to ensure all resources (threads, connections, etc.) are properly released
                // This prevents resource leaks from underlying client libraries (OPC UA Milo, database drivers, etc.)
                try {
                    log.info("Destroying adapter with id '{}' to release all resources", getId());
                    adapter.destroy();
                } catch (final Exception destroyException) {
                    log.error("Error destroying adapter with id {}", adapter.getId(), destroyException);
                }

                if (throwable == null) {
                    log.info("Stopped adapter with id {}", adapter.getId());
                } else {
                    log.error("Error stopping adapter with id {}", adapter.getId(), throwable);
                }
                stopFutureRef.set(null);
            });

            // Replace the placeholder with the actual future and complete the placeholder to unblock any waiters
            stopFutureRef.set(stopFuture);
            stopFuture.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    placeholderFuture.completeExceptionally(throwable);
                } else {
                    placeholderFuture.complete(result);
                }
            });

            return placeholderFuture;
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

    private boolean startWriting(final @NotNull InternalProtocolAdapterWritingService protocolAdapterWritingService) {
        log.debug("Start writing for protocol adapter with id '{}'", getId());

        final var southboundMappings = getSouthboundMappings();
        final var writingContexts = southboundMappings.stream()
                .map(InternalWritingContextImpl::new)
                .collect(Collectors.<InternalWritingContext>toList());

        return protocolAdapterWritingService.startWriting((WritingProtocolAdapter) getAdapter(),
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
}
