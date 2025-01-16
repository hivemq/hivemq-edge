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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryInput;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryOutput;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.polling.PollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterPublishService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingService;
import com.hivemq.persistence.mappings.NorthboundMapping;
import com.hivemq.persistence.mappings.SouthboundMapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ProtocolAdapterWrapper {

    private static final Logger log = LoggerFactory.getLogger(ProtocolAdapterWrapper.class);

    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull ProtocolAdapter adapter;
    private final @NotNull ProtocolAdapterFactory<?> adapterFactory;
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull ProtocolSpecificAdapterConfig configObject;
    private final @NotNull List<? extends Tag> tags;
    private final @NotNull List<SouthboundMapping> southboundMappings;
    private final @NotNull List<NorthboundMapping> northboundMappings;
    private final @NotNull InternalProtocolAdapterWritingService protocolAdapterWritingService;
    private final @NotNull ProtocolAdapterPollingService protocolAdapterPollingService;
    protected @Nullable Long lastStartAttemptTime;

    public ProtocolAdapterWrapper(
            final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService,
            final @NotNull InternalProtocolAdapterWritingService protocolAdapterWritingService,
            final @NotNull ProtocolAdapterPollingService protocolAdapterPollingService,
            final @NotNull ProtocolAdapter adapter,
            final @NotNull ProtocolAdapterFactory<?> adapterFactory,
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterState protocolAdapterState,
            final @NotNull ProtocolSpecificAdapterConfig configObject,
            final @NotNull List<? extends Tag> tags,
            final @NotNull List<SouthboundMapping> southboundMappings,
            final @NotNull List<NorthboundMapping> northboundMappings) {
        this.protocolAdapterWritingService = protocolAdapterWritingService;
        this.protocolAdapterPollingService = protocolAdapterPollingService;
        this.protocolAdapterMetricsService = protocolAdapterMetricsService;
        this.adapter = adapter;
        this.adapterFactory = adapterFactory;
        this.adapterInformation = adapterInformation;
        this.protocolAdapterState = protocolAdapterState;
        this.configObject = configObject;
        this.tags = tags;
        this.southboundMappings = southboundMappings;
        this.northboundMappings = northboundMappings;
    }

    public @NotNull CompletableFuture<Void> start(
            final boolean writingEnabled,
            final @NotNull ModuleServices moduleServices,
            final @NotNull ObjectMapper objectMapper,
            final @NotNull JsonPayloadDefaultCreator jsonPayloadCreator) {
        initStartAttempt();
        final ProtocolAdapterStartOutputImpl output = new ProtocolAdapterStartOutputImpl();
        final ProtocolAdapterStartInputImpl input = new ProtocolAdapterStartInputImpl(moduleServices);
        //Instantly go to started so it has to be stopped first before retrying
        adapter.start(input, output);
        final CompletableFuture<Void> startFuture = output.getStartFuture();

        return startFuture.thenCompose(r -> {
            startPolling(protocolAdapterPollingService,
                    objectMapper,
                    input.moduleServices().adapterPublishService(),
                    input.moduleServices().eventService(),
                    jsonPayloadCreator);
            return startWriting(writingEnabled, protocolAdapterWritingService);
        }).thenApply(r -> {
            setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STARTED);
            return null;
        });

    }

    public CompletableFuture<Void> stop() {
        final ProtocolAdapterStopInputImpl input = new ProtocolAdapterStopInputImpl();
        final ProtocolAdapterStopOutputImpl output = new ProtocolAdapterStopOutputImpl();
        adapter.stop(input, output);
        stopPolling(protocolAdapterPollingService);
        return stopWriting(protocolAdapterWritingService)
                .thenApply(r -> {
                    //only transition to stopped whne it succeeded
                    setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STOPPED);
                    return null;
                });
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
        return configObject;
    }

    public @NotNull List<? extends Tag> getTags() {
        return tags;
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
        return northboundMappings;
    }

    public @NotNull List<SouthboundMapping> getSouthboundMappings() {
        return southboundMappings;
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

    private void startPolling(
            final @NotNull ProtocolAdapterPollingService protocolAdapterPollingService,
            final @NotNull ObjectMapper objectMapper,
            final @NotNull ProtocolAdapterPublishService adapterPublishService,
            final @NotNull EventService eventService,
            final @NotNull JsonPayloadDefaultCreator jsonPayloadCreator) {
        if (isPolling()) {
            log.debug("Schedule polling for protocol adapter with id '{}'", getId());
            getNorthboundMappings()
                    .forEach(adapterSubscription -> {
                        final PerSubscriptionSampler sampler = new PerSubscriptionSampler(
                                this,
                                objectMapper,
                                adapterPublishService,
                                adapterSubscription,
                                eventService,
                                jsonPayloadCreator);
                        protocolAdapterPollingService.schedulePolling(sampler);
                    });
        }
    }

    private void stopPolling(
            final @NotNull ProtocolAdapterPollingService protocolAdapterPollingService) {
        if (isPolling()) {
            log.debug("Stopping polling for protocol adapter with id '{}'", getId());
            protocolAdapterPollingService.stopPollingForAdapterInstance(getAdapter());
        }
    }

    private @NotNull CompletableFuture<Void> startWriting(final boolean writingEnabled, final @NotNull InternalProtocolAdapterWritingService protocolAdapterWritingService) {
        if (writingEnabled && isWriting()) {
            log.debug("Start writing for protocol adapter with id '{}'", getId());

            final List<SouthboundMapping> southboundMappings = getSouthboundMappings();
            final List<InternalWritingContext> writingContexts = southboundMappings.stream()
                    .map(InternalWritingContextImpl::new)
                    .collect(Collectors.toList());

            return protocolAdapterWritingService.startWriting((WritingProtocolAdapter) getAdapter(),
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
            final List<InternalWritingContext> writingContexts =
                    getSouthboundMappings().stream().map(InternalWritingContextImpl::new).collect(Collectors.toList());
            return protocolAdapterWritingService.stopWriting((WritingProtocolAdapter) getAdapter(), writingContexts);
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

}
