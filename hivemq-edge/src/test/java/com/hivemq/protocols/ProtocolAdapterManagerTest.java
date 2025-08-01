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
import com.hivemq.adapter.sdk.api.ProtocolAdapterCategory;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.ProtocolAdapterTag;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.EventBuilder;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.adapter.sdk.api.writing.WritingInput;
import com.hivemq.adapter.sdk.api.writing.WritingOutput;
import com.hivemq.adapter.sdk.api.writing.WritingPayload;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.configuration.reader.ProtocolAdapterExtractor;
import com.hivemq.edge.HiveMQEdgeRemoteService;
import com.hivemq.edge.VersionProvider;
import com.hivemq.edge.modules.adapters.data.TagManager;
import com.hivemq.edge.modules.adapters.impl.ModuleServicesImpl;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingService;
import com.hivemq.edge.modules.api.events.model.EventBuilderImpl;
import com.hivemq.protocols.northbound.NorthboundConsumerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProtocolAdapterManagerTest {

    private final @NotNull MetricRegistry metricRegistry = mock();
    private final @NotNull ModuleServicesImpl moduleServices = mock();
    private final @NotNull HiveMQEdgeRemoteService remoteService = mock();
    private final @NotNull EventService eventService = mock();
    private final @NotNull VersionProvider versionProvider = mock();
    private final @NotNull ProtocolAdapterPollingService protocolAdapterPollingService = mock();
    private final @NotNull ProtocolAdapterMetrics protocolAdapterMetrics = mock();
    private final @NotNull InternalProtocolAdapterWritingService protocolAdapterWritingService = mock();
    private final @NotNull ProtocolAdapterFactoryManager protocolAdapterFactoryManager = mock();
    private final @NotNull NorthboundConsumerFactory northboundConsumerFactory = mock();
    private final @NotNull TagManager tagManager = mock();
    private final @NotNull ProtocolAdapterExtractor protocolAdapterExtractor = mock();

    private final @NotNull ProtocolAdapterConfigConverter protocolAdapterConfigConverter = mock();

    private @NotNull ProtocolAdapterManager protocolAdapterManager;

    @BeforeEach
    void setUp() {
        protocolAdapterManager = new ProtocolAdapterManager(
                metricRegistry,
                moduleServices,
                remoteService,
                eventService,
                protocolAdapterConfigConverter,
                versionProvider,
                protocolAdapterPollingService,
                protocolAdapterMetrics,
                protocolAdapterWritingService,
                protocolAdapterFactoryManager,
                northboundConsumerFactory,
                tagManager,
                protocolAdapterExtractor);
    }

    @Test
    void test_startWritingAdapterSucceeded_eventsFired() throws Exception {
        final EventBuilder eventBuilder = new EventBuilderImpl(mock());

        when(protocolAdapterWritingService.writingEnabled()).thenReturn(true);
        when(protocolAdapterWritingService.startWriting(any(),
                any(),
                any())).thenReturn(true);
        when(eventService.createAdapterEvent(anyString(), anyString())).thenReturn(eventBuilder);
        final var adapterState = new ProtocolAdapterStateImpl(eventService, "test-adapter", "test-protocol");
        final ProtocolAdapterWrapper adapterWrapper = new ProtocolAdapterWrapper(mock(),
                protocolAdapterWritingService,
                protocolAdapterPollingService,
                mock(),
                new TestWritingAdapter(true, adapterState),
                mock(),
                mock(),
                adapterState,
                northboundConsumerFactory,
                tagManager);

        protocolAdapterManager.startAsync(adapterWrapper).get();

        assertThat(adapterWrapper.getRuntimeStatus()).isEqualTo(ProtocolAdapterState.RuntimeStatus.STARTED);
        verify(remoteService).fireUsageEvent(any());
    }

    @Test
    void test_startWritingNotEnabled_writingNotStarted() throws Exception {
        final EventBuilder eventBuilder = new EventBuilderImpl(mock());

        when(protocolAdapterWritingService.writingEnabled()).thenReturn(false);
        when(eventService.createAdapterEvent(anyString(), anyString())).thenReturn(eventBuilder);
        final var adapterState = new ProtocolAdapterStateImpl(eventService, "test-adapter", "test-protocol");

        final ProtocolAdapterWrapper adapterWrapper = new ProtocolAdapterWrapper(mock(),
                protocolAdapterWritingService,
                protocolAdapterPollingService,
                mock(),
                new TestWritingAdapter(true, adapterState),
                mock(),
                mock(),
                adapterState,
                northboundConsumerFactory,
                tagManager);

        protocolAdapterManager.startAsync(adapterWrapper).get();

        assertThat(adapterWrapper.getRuntimeStatus()).isEqualTo(ProtocolAdapterState.RuntimeStatus.STARTED);
        verify(remoteService).fireUsageEvent(any());
        verify(protocolAdapterWritingService, never()).startWriting(any(), any(), any());
    }

    @Test
    void test_startWriting_adapterFailedStart_resourcesCleanedUp() throws Exception{
        final EventBuilder eventBuilder = new EventBuilderImpl(mock());

        when(protocolAdapterWritingService.writingEnabled()).thenReturn(true);
        when(protocolAdapterWritingService
                .startWriting(any(), any(), any()))
                .thenReturn(true);
        when(eventService.createAdapterEvent(anyString(), anyString())).thenReturn(eventBuilder);

        final var adapterState = new ProtocolAdapterStateImpl(eventService, "test-adapter", "test-protocol");

        final ProtocolAdapterWrapper adapterWrapper = new ProtocolAdapterWrapper(mock(),
                protocolAdapterWritingService,
                protocolAdapterPollingService,
                mock(),
                new TestWritingAdapter(false, adapterState),
                mock(),
                mock(),
                adapterState,
                northboundConsumerFactory,
                tagManager);

        protocolAdapterManager.startAsync(adapterWrapper).get();

        assertThat(adapterWrapper.getRuntimeStatus()).isEqualTo(ProtocolAdapterState.RuntimeStatus.STOPPED);
        verify(remoteService).fireUsageEvent(any());
        verify(protocolAdapterWritingService).stopWriting(eq((WritingProtocolAdapter) adapterWrapper.getAdapter()),
                any());
    }

    @Test
    void test_startWriting_eventServiceFailedStart_resourcesCleanedUp() throws Exception {

        final EventBuilder eventBuilder = new EventBuilderImpl(mock());
        when(eventService.createAdapterEvent(anyString(), anyString())).thenReturn(eventBuilder);

        when(protocolAdapterWritingService.writingEnabled()).thenReturn(true);
        when(protocolAdapterWritingService.startWriting(any(),
                any(),
                any())).thenReturn(true);

        final var adapterState = new ProtocolAdapterStateImpl(eventService, "test-adapter", "test-protocol");
        final ProtocolAdapterWrapper adapterWrapper = new ProtocolAdapterWrapper(mock(),
                protocolAdapterWritingService,
                protocolAdapterPollingService,
                mock(),
                new TestWritingAdapter(false, adapterState),
                mock(),
                mock(),
                adapterState,
                northboundConsumerFactory,
                tagManager);

        protocolAdapterManager.startAsync(adapterWrapper).get();

        assertThat(adapterWrapper.getRuntimeStatus()).isEqualTo(ProtocolAdapterState.RuntimeStatus.STOPPED);
        verify(protocolAdapterWritingService).stopWriting(eq((WritingProtocolAdapter) adapterWrapper.getAdapter()),
                any());
    }

    @Test
    void test_stopWritingAdapterSucceeded_eventsFired() throws Exception {
        final EventBuilder eventBuilder = new EventBuilderImpl(mock());

        when(protocolAdapterWritingService.writingEnabled()).thenReturn(true);
        when(eventService.createAdapterEvent(anyString(), anyString())).thenReturn(eventBuilder);

        final var adapterState = new ProtocolAdapterStateImpl(eventService, "test-adapter", "test-protocol");
        final ProtocolAdapterWrapper adapterWrapper = new ProtocolAdapterWrapper(mock(),
                protocolAdapterWritingService,
                protocolAdapterPollingService,
                mock(),
                new TestWritingAdapter(true, adapterState),
                mock(),
                mock(),
                adapterState,
                northboundConsumerFactory,
                tagManager);

        adapterWrapper.setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STARTED);

        protocolAdapterManager.stopAsync(adapterWrapper, false).get();

        assertThat(adapterWrapper.getRuntimeStatus()).isEqualTo(ProtocolAdapterState.RuntimeStatus.STOPPED);
    }

    @Test
    void test_stopWritingAdapterFailed_eventsFired() throws Exception {
        final EventBuilder eventBuilder = new EventBuilderImpl(mock());

        when(protocolAdapterWritingService.writingEnabled()).thenReturn(true);
        when(eventService.createAdapterEvent(anyString(), anyString())).thenReturn(eventBuilder);

        final var adapterState = new ProtocolAdapterStateImpl(eventService, "test-adapter", "test-protocol");
        final ProtocolAdapterWrapper adapterWrapper = new ProtocolAdapterWrapper(mock(),
                protocolAdapterWritingService,
                protocolAdapterPollingService,
                mock(),
                new TestWritingAdapter(false, adapterState),
                mock(),
                mock(),
                adapterState,
                northboundConsumerFactory,
                tagManager);

        adapterWrapper.setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STARTED);

        assertThatThrownBy(() -> protocolAdapterManager.stopAsync(adapterWrapper, false).get())
                .rootCause()
                .isInstanceOf(RuntimeException.class)
                .hasMessage("failed");

        assertThat(adapterWrapper.getRuntimeStatus()).isEqualTo(ProtocolAdapterState.RuntimeStatus.STOPPED);
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
        public @org.jetbrains.annotations.NotNull Class<? extends ProtocolSpecificAdapterConfig> configurationClassNorthbound() {
            return null;
        }

        @Override
        public @org.jetbrains.annotations.NotNull Class<? extends ProtocolSpecificAdapterConfig> configurationClassNorthAndSouthbound() {
            return null;
        }

        @Override
        public int getCurrentConfigVersion() {
            return 1;
        }
    }

    static class TestWritingAdapter implements WritingProtocolAdapter {

        final boolean success;
        final ProtocolAdapterState adapterState;

        TestWritingAdapter(final boolean success, final @NotNull ProtocolAdapterState adapterState) {
            this.success = success;
            this.adapterState = adapterState;
        }

        @Override
        public void write(
                final @NotNull WritingInput writingInput, final @NotNull WritingOutput writingOutput) {

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
                adapterState.setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STARTED);
                output.startedSuccessfully();
            } else {
                output.failStart(new RuntimeException("failed"), "could not start");
            }
        }

        @Override
        public void stop(
                final @NotNull ProtocolAdapterStopInput input, final @NotNull ProtocolAdapterStopOutput output) {
            if (success) {
                adapterState.setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STOPPED);
                output.stoppedSuccessfully();
            } else {
                output.failStop(new RuntimeException("failed"), "could not stop");
            }
        }

        @Override
        public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
            return new TestWritingProtocolAdapterInformation();
        }
    }
}
