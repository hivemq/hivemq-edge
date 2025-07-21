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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ProtocolAdapterWrapper {

    private static final Logger log = LoggerFactory.getLogger(ProtocolAdapterWrapper.class);

    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull ProtocolAdapter adapter;
    private final @NotNull ProtocolAdapterFactory<?> adapterFactory;
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull InternalProtocolAdapterWritingService protocolAdapterWritingService;
    private final @NotNull ProtocolAdapterPollingService protocolAdapterPollingService;
    private final @NotNull ProtocolAdapterConfig config;
    private final @NotNull NorthboundConsumerFactory northboundConsumerFactory;
    private final @NotNull TagManager tagManager;
    protected @Nullable Long lastStartAttemptTime;
    private final List<NorthboundTagConsumer> consumers = new ArrayList<>();

    private final AtomicReference<CompletableFuture<Boolean>> startFutureRef = new AtomicReference<>(null);
    private final AtomicReference<CompletableFuture<Boolean>> stopFutureRef = new AtomicReference<>(null);

    public ProtocolAdapterWrapper(
            final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService,
            final @NotNull InternalProtocolAdapterWritingService protocolAdapterWritingService,
            final @NotNull ProtocolAdapterPollingService protocolAdapterPollingService,
            final @NotNull ProtocolAdapterConfig config,
            final @NotNull ProtocolAdapter adapter,
            final @NotNull ProtocolAdapterFactory<?> adapterFactory,
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterState protocolAdapterState,
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

    public @NotNull CompletableFuture<Boolean> startAsync(
            final boolean writingEnabled,
            final @NotNull ModuleServices moduleServices) {
        final var currentFuture = startFutureRef.get();
        if(currentFuture == null) {
            synchronized (startFutureRef) {
                final var currentFutureDoubleCheck = startFutureRef.get();
                if(currentFutureDoubleCheck != null) {
                    return currentFutureDoubleCheck;
                }
                initStartAttempt();
                final ProtocolAdapterStartOutputImpl output = new ProtocolAdapterStartOutputImpl();
                final ProtocolAdapterStartInputImpl input = new ProtocolAdapterStartInputImpl(moduleServices);

                final CompletableFuture<Boolean> startFuture = new CompletableFuture<>();
                startFutureRef.set(startFuture);

                CompletableFuture
                        .supplyAsync(() -> {
                            adapter.start(input, output);
                            return output.getStartFuture();
                        })
                        .thenCompose(Function.identity())
                        .handle((ignored, t) -> {
                            if(t == null) {
                                createAndSubscribeTagConsumer();
                                startPolling(protocolAdapterPollingService, input.moduleServices().eventService());
                                return startWriting(writingEnabled, protocolAdapterWritingService)
                                        .thenApply(v -> {
                                            log.info("Successfully started adapter with id {}", adapter.getId());
                                            setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STARTED);
                                            startFutureRef.get().complete(true);
                                            startFutureRef.set(null);
                                            return true;
                                        });
                            } else {
                                log.error("Error starting protocol adapter", t);
                                stopPolling(protocolAdapterPollingService);
                                return stopWriting(protocolAdapterWritingService)
                                        .thenApply(v -> {
                                            log.error("Error starting adapter with id {}", adapter.getId(), t);
                                            setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STOPPED);
                                            setErrorConnectionStatus(t, t.getMessage());
                                            startFutureRef.get().complete(false);
                                            startFutureRef.set(null);
                                            return false;
                                        });
                            }
                        });
                return startFuture;
            }
        } else {
            log.info("Multiple start requests received for adapter with id '{}', returning existing future", getId());
            return currentFuture;
        }
    }

    public @NotNull CompletableFuture<Boolean> stopAsync(final boolean destroy) {
        final var currentFuture = stopFutureRef.get();
        if( currentFuture == null) {
            synchronized (stopFutureRef) {
                final var currentFutureDoubleCheck = stopFutureRef.get();
                if (currentFutureDoubleCheck != null) {
                    return currentFutureDoubleCheck;
                }
                consumers.forEach(tagManager::removeConsumer);
                final ProtocolAdapterStopInputImpl input = new ProtocolAdapterStopInputImpl();
                final ProtocolAdapterStopOutputImpl output = new ProtocolAdapterStopOutputImpl();


                final CompletableFuture<Boolean> stopFuture = new CompletableFuture<>();
                stopFutureRef.set(stopFuture);
                CompletableFuture.supplyAsync(() -> {
                            stopPolling(protocolAdapterPollingService);
                            return stopWriting(protocolAdapterWritingService);
                        }).thenCompose(Function.identity()).handle((stopped, t) -> {
                            adapter.stop(input, output);
                            return output.getOutputFuture();
                        }).thenCompose(Function.identity()).handle((ignored, throwable) -> {
                            if (destroy) {
                                log.info("Destroying adapter with id '{}'", getId());
                                adapter.destroy();
                            }
                            if (throwable == null) {
                                setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STOPPED);
                                log.info("Stopped adapter with id {}", adapter.getId());
                                stopFutureRef.get().complete(true);
                                stopFutureRef.set(null);
                                return true;
                            } else {
                                log.error("Error stopping adapter with id {}", adapter.getId(), throwable);
                                stopFutureRef.get().complete(false);
                                stopFutureRef.set(null);
                                return false;
                            }
                        });

                return stopFuture;
            }
        } else {
            log.info("Multiple stop requests received for adapter with id '{}', returning existing future", getId());
            return currentFuture;
        }
    }

    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapter.getProtocolAdapterInformation();
    }

    public void discoverValues(
            final @NotNull ProtocolAdapterDiscoveryInput input, final @NotNull ProtocolAdapterDiscoveryOutput output) {
        adapter.discoverValues(input, output);
    }

    public @NotNull ProtocolAdapterState.ConnectionStatus getConnectionStatus() {
        return protocolAdapterState.getConnectionStatus();
    }

    public @NotNull ProtocolAdapterState.RuntimeStatus getRuntimeStatus() {
        return protocolAdapterState.getRuntimeStatus();
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

    public void setRuntimeStatus(final @NotNull ProtocolAdapterState.RuntimeStatus runtimeStatus) {
        protocolAdapterState.setRuntimeStatus(runtimeStatus);
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
            final PerAdapterSampler sampler =
                    new PerAdapterSampler(this, eventService, tagManager);
            protocolAdapterPollingService.schedulePolling(sampler);
        }

        if (isPolling()) {
            config.getTags().forEach(tag -> {
                final PerContextSampler sampler =
                        new PerContextSampler(
                                this,
                            new PollingContextWrapper(
                                    "unused",
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

    private @NotNull CompletableFuture<Void> startWriting(
            final boolean writingEnabled,
            final @NotNull InternalProtocolAdapterWritingService protocolAdapterWritingService) {
        if (writingEnabled && isWriting()) {
            log.debug("Start writing for protocol adapter with id '{}'", getId());

            final List<SouthboundMapping> southboundMappings = getSouthboundMappings();
            final List<InternalWritingContext> writingContexts =
                    southboundMappings.stream().map(InternalWritingContextImpl::new).collect(Collectors.toList());

            return protocolAdapterWritingService
                    .startWriting(
                        (WritingProtocolAdapter) getAdapter(),
                        getProtocolAdapterMetricsService(),
                        writingContexts);
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    private @NotNull CompletableFuture<Void> stopWriting(final @NotNull InternalProtocolAdapterWritingService protocolAdapterWritingService) {
        //no check for 'writing is enabled', as we have to stop it anyway since the license could have been removed in the meantime.
        if (isWriting()) {
            log.debug("Stopping writing for protocol adapter with id '{}'", getId());
            final var writingContexts =
                    getSouthboundMappings().stream()
                            .map(mapping -> (InternalWritingContext)new InternalWritingContextImpl(mapping))
                            .toList();
            return protocolAdapterWritingService.stopWriting((WritingProtocolAdapter) getAdapter(), writingContexts);
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    private void createAndSubscribeTagConsumer() {
        getNorthboundMappings().stream()
                .map(northboundMapping -> northboundConsumerFactory.build(this, northboundMapping, protocolAdapterMetricsService))
                .forEach(northboundTagConsumer -> {
                    tagManager.addConsumer(northboundTagConsumer);
                    consumers.add(northboundTagConsumer);
                });
    }


}
