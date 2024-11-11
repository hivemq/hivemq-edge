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

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.ProtocolAdapterCategory;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.ProtocolAdapterTag;
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.EventBuilder;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.adapter.sdk.api.writing.WritingContext;
import com.hivemq.adapter.sdk.api.writing.WritingInput;
import com.hivemq.adapter.sdk.api.writing.WritingOutput;
import com.hivemq.adapter.sdk.api.writing.WritingPayload;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.HiveMQEdgeRemoteService;
import com.hivemq.edge.VersionProvider;
import com.hivemq.edge.modules.ModuleLoader;
import com.hivemq.edge.modules.adapters.impl.ModuleServicesImpl;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingService;
import com.hivemq.edge.modules.api.events.model.EventBuilderImpl;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterWritingService;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProtocolAdapterManagerTest {

    private final @NotNull ConfigurationService configurationService = mock();
    private final @NotNull MetricRegistry metricRegistry = mock();
    private final @NotNull ModuleServicesImpl moduleServices = mock();
    private final @NotNull ObjectMapper objectMapper = new ObjectMapper();
    private final @NotNull ModuleLoader moduleLoader = mock();
    private final @NotNull HiveMQEdgeRemoteService remoteService = mock();
    private final @NotNull EventService eventService = mock();
    private final @NotNull VersionProvider versionProvider = mock();
    private final @NotNull ProtocolAdapterPollingService protocolAdapterPollingService = mock();
    private final @NotNull ProtocolAdapterMetrics protocolAdapterMetrics = mock();
    private final @NotNull JsonPayloadDefaultCreator jsonPayloadDefaultCreator = mock();
    private final @NotNull ProtocolAdapterWritingService protocolAdapterWritingService = mock();

    private final @NotNull ExecutorService executorService = Executors.newSingleThreadExecutor();

    private @NotNull ProtocolAdapterManager protocolAdapterManager;

    @BeforeEach
    void setUp() {
        protocolAdapterManager = new ProtocolAdapterManager(
                configurationService,
                metricRegistry,
                moduleServices,
                objectMapper,
                moduleLoader,
                remoteService,
                eventService,
                versionProvider,
                protocolAdapterPollingService,
                protocolAdapterMetrics,
                jsonPayloadDefaultCreator,
                protocolAdapterWritingService,
                executorService);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    @Test
    void test_startWritingAdapterSucceeded_eventsFired() throws Exception {

        final EventBuilder eventBuilder = new EventBuilderImpl(mock());

        when(protocolAdapterWritingService.writingEnabled()).thenReturn(true);
        when(protocolAdapterWritingService.startWriting(any(),
                any())).thenReturn(CompletableFuture.completedFuture(null));
        when(eventService.createAdapterEvent(anyString(), anyString())).thenReturn(eventBuilder);

        final ProtocolAdapterWrapper<TestWritingAdapter> adapterWrapper = new ProtocolAdapterWrapper<>(
                mock(),
                new TestWritingAdapter(true),
                mock(),
                mock(),
                new ProtocolAdapterStateImpl(eventService, "test-adapter", "test-protocol"),
                mock(),
                List.of());

        protocolAdapterManager.start(adapterWrapper).get();

        assertEquals(ProtocolAdapterState.RuntimeStatus.STARTED, adapterWrapper.getRuntimeStatus());
        verify(remoteService).fireUsageEvent(any());
    }

    @Test
    void test_startWritingNotEnabled_notStarted() throws Exception {

        final EventBuilder eventBuilder = new EventBuilderImpl(mock());

        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);
        when(eventService.createAdapterEvent(anyString(), anyString())).thenReturn(eventBuilder);

        final ProtocolAdapterWrapper<TestWritingAdapter> adapterWrapper = new ProtocolAdapterWrapper<>(mock(),
                new TestWritingAdapter(true),
                mock(),
                mock(),
                new ProtocolAdapterStateImpl(eventService, "test-adapter", "test-protocol"),
                mock(),
                List.of());

        protocolAdapterManager.start(adapterWrapper).get();

        assertEquals(ProtocolAdapterState.RuntimeStatus.STARTED, adapterWrapper.getRuntimeStatus());
        verify(remoteService).fireUsageEvent(any());
        verify(protocolAdapterWritingService, never()).startWriting(any(), any());
    }

    @Test
    void test_startWriting_adapterFailedStart_resourcesCleanedUp() {

        final EventBuilder eventBuilder = new EventBuilderImpl(mock());

        when(protocolAdapterWritingService.writingEnabled()).thenReturn(true);
        when(protocolAdapterWritingService.startWriting(any(),
                any())).thenReturn(CompletableFuture.completedFuture(null));
        when(eventService.createAdapterEvent(anyString(), anyString())).thenReturn(eventBuilder);

        final ProtocolAdapterWrapper<TestWritingAdapter> adapterWrapper = new ProtocolAdapterWrapper<>(mock(),
                new TestWritingAdapter(false),
                mock(),
                mock(),
                new ProtocolAdapterStateImpl(eventService, "test-adapter", "test-protocol"),
                mock(),
                List.of());

        assertThrows(ExecutionException.class, () -> protocolAdapterManager.start(adapterWrapper).get());

        assertEquals(ProtocolAdapterState.RuntimeStatus.STOPPED, adapterWrapper.getRuntimeStatus());
        verify(remoteService).fireUsageEvent(any());
        verify(protocolAdapterWritingService).stopWriting(adapterWrapper.getAdapter());
    }

    @Test
    void test_startWriting_eventServiceFailedStart_resourcesCleanedUp() {

        when(protocolAdapterWritingService.writingEnabled()).thenReturn(true);
        when(protocolAdapterWritingService.startWriting(any(),
                any())).thenReturn(CompletableFuture.completedFuture(null));
        when(eventService.createAdapterEvent(anyString(),
                anyString())).thenThrow(new RuntimeException("we failed start"));

        final ProtocolAdapterWrapper<TestWritingAdapter> adapterWrapper = new ProtocolAdapterWrapper<>(mock(),
                new TestWritingAdapter(false),
                mock(),
                mock(),
                new ProtocolAdapterStateImpl(eventService, "test-adapter", "test-protocol"),
                mock(),
                List.of());

        assertThrows(ExecutionException.class, () -> protocolAdapterManager.start(adapterWrapper).get());

        assertEquals(ProtocolAdapterState.RuntimeStatus.STOPPED, adapterWrapper.getRuntimeStatus());
        verify(remoteService, never()).fireUsageEvent(any());
        verify(protocolAdapterWritingService).stopWriting(adapterWrapper.getAdapter());
    }

    @Test
    void test_stopWritingAdapterSucceeded_eventsFired() throws Exception {

        final EventBuilder eventBuilder = new EventBuilderImpl(mock());

        when(protocolAdapterWritingService.writingEnabled()).thenReturn(true);
        when(protocolAdapterWritingService.stopWriting(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(eventService.createAdapterEvent(anyString(), anyString())).thenReturn(eventBuilder);

        final ProtocolAdapterWrapper<TestWritingAdapter> adapterWrapper = new ProtocolAdapterWrapper<>(mock(),
                new TestWritingAdapter(true),
                mock(),
                mock(),
                new ProtocolAdapterStateImpl(eventService, "test-adapter", "test-protocol"),
                mock(),
                List.of());

        adapterWrapper.setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STARTED);

        protocolAdapterManager.stop(adapterWrapper).get();

        assertEquals(ProtocolAdapterState.RuntimeStatus.STOPPED, adapterWrapper.getRuntimeStatus());
    }

    @Test
    void test_stopWritingAdapterFailed_eventsFired() throws Exception {

        final EventBuilder eventBuilder = new EventBuilderImpl(mock());

        when(protocolAdapterWritingService.writingEnabled()).thenReturn(true);
        when(protocolAdapterWritingService.stopWriting(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(eventService.createAdapterEvent(anyString(), anyString())).thenReturn(eventBuilder);

        final ProtocolAdapterWrapper<TestWritingAdapter> adapterWrapper = new ProtocolAdapterWrapper<>(mock(),
                new TestWritingAdapter(false),
                mock(),
                mock(),
                new ProtocolAdapterStateImpl(eventService, "test-adapter", "test-protocol"),
                mock(),
                List.of());

        adapterWrapper.setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STARTED);

        protocolAdapterManager.stop(adapterWrapper).get();

        assertEquals(ProtocolAdapterState.RuntimeStatus.STARTED, adapterWrapper.getRuntimeStatus());
    }

    static class TestWritingProtocolAdapterInformation implements ProtocolAdapterInformation {

        @Override
        public @org.jetbrains.annotations.NotNull String getProtocolName() {
            return "Test Writing Protocol";
        }

        @Override
        public @org.jetbrains.annotations.NotNull String getProtocolId() {
            return "test-writing-protocol";
        }

        @Override
        public @org.jetbrains.annotations.NotNull String getDisplayName() {
            return "";
        }

        @Override
        public @org.jetbrains.annotations.NotNull String getDescription() {
            return "";
        }

        @Override
        public @org.jetbrains.annotations.NotNull String getUrl() {
            return "";
        }

        @Override
        public @org.jetbrains.annotations.NotNull String getVersion() {
            return "";
        }

        @Override
        public @org.jetbrains.annotations.NotNull String getLogoUrl() {
            return "";
        }

        @Override
        public @org.jetbrains.annotations.NotNull String getAuthor() {
            return "";
        }

        @Override
        public @Nullable ProtocolAdapterCategory getCategory() {
            return null;
        }

        @Override
        public @Nullable List<ProtocolAdapterTag> getTags() {
            return List.of();
        }

        @Override
        public @org.jetbrains.annotations.NotNull Class<? extends Tag> tagConfigurationClass() {
            return null;
        }

        @Override
        public @org.jetbrains.annotations.NotNull Class<? extends ProtocolAdapterConfig> configurationClassReading() {
            return null;
        }

        @Override
        public @org.jetbrains.annotations.NotNull Class<? extends ProtocolAdapterConfig> configurationClassWriting() {
            return null;
        }
    }

    static class TestWritingAdapter implements WritingProtocolAdapter<WritingContext> {

        final boolean success;

        TestWritingAdapter(final boolean success) {
            this.success = success;
        }

        @Override
        public void write(
                final @NotNull WritingInput writingInput, @NotNull final WritingOutput writingOutput) {

        }

        @Override
        public @NotNull List<WritingContext> getWritingContexts() {
            return List.of();
        }

        @Override
        public @NotNull Class<? extends WritingPayload> getMqttPayloadClass() {
            return null;
        }

        @Override
        public @NotNull String getId() {
            return "test-writing";
        }

        @Override
        public void start(
                final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
            if (success) {
                output.startedSuccessfully();
            } else {
                output.failStart(new RuntimeException("failed"), "could not start");
            }
        }

        @Override
        public void stop(
                final @NotNull ProtocolAdapterStopInput input, final @NotNull ProtocolAdapterStopOutput output) {
            if (success) {
                output.stoppedSuccessfully();
            } else {
                output.failStop(new RuntimeException("failed"), "could not stop");
            }
        }

        @Override
        public @NotNull @org.jetbrains.annotations.NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
            return new TestWritingProtocolAdapterInformation();
        }
    }
}
