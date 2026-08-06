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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import com.codahale.metrics.MetricRegistry;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.events.model.EventBuilder;
import com.hivemq.adapter.sdk.api.exceptions.ProtocolAdapterException;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterPublishService;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.reader.ProtocolAdapterExtractor;
import com.hivemq.edge.HiveMQEdgeRemoteService;
import com.hivemq.edge.VersionProvider;
import com.hivemq.edge.modules.adapters.data.TagManager;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingService;
import com.hivemq.persistence.mappings.NorthboundMapping;
import com.hivemq.persistence.mappings.SouthboundMapping;
import com.hivemq.protocols.fsm.ProtocolAdapterManagerState;
import com.hivemq.protocols.northbound.NorthboundConsumerFactory;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;
import util.LogbackCapturingAppender;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProtocolAdapterManagerTest {

    @Mock
    private @NotNull MetricRegistry metricRegistry;

    @Mock
    private @NotNull ProtocolAdapterPublishService protocolAdapterPublishService;

    @Mock
    private @NotNull HiveMQEdgeRemoteService remoteService;

    @Mock
    private @NotNull EventService eventService;

    @Mock
    private @NotNull ProtocolAdapterConfigConverter configConverter;

    @Mock
    private @NotNull VersionProvider versionProvider;

    @Mock
    private @NotNull ProtocolAdapterPollingService pollingService;

    @Mock
    private @NotNull ProtocolAdapterMetrics protocolAdapterMetrics;

    @Mock
    private @NotNull InternalProtocolAdapterWritingService writingService;

    @Mock
    private @NotNull ProtocolAdapterFactoryManager factoryManager;

    @Mock
    private @NotNull NorthboundConsumerFactory northboundConsumerFactory;

    @Mock
    private @NotNull TagManager tagManager;

    @Mock
    private @NotNull ProtocolAdapterExtractor protocolAdapterExtractor;

    @Mock
    private @NotNull EventBuilder eventBuilder;

    private @NotNull ProtocolAdapterManager manager;

    @BeforeEach
    void setUp() {
        when(eventService.createAdapterEvent(anyString(), anyString())).thenReturn(eventBuilder);
        when(eventService.configurationEvent()).thenReturn(eventBuilder);
        when(eventBuilder.withSeverity(any())).thenReturn(eventBuilder);
        when(eventBuilder.withMessage(anyString())).thenReturn(eventBuilder);

        manager = new ProtocolAdapterManager(
                metricRegistry,
                remoteService,
                eventService,
                configConverter,
                versionProvider,
                pollingService,
                protocolAdapterMetrics,
                writingService,
                factoryManager,
                northboundConsumerFactory,
                tagManager,
                protocolAdapterExtractor,
                protocolAdapterPublishService);
    }

    private @NotNull ProtocolAdapter createSuccessAdapter(final @NotNull String adapterId) {
        final ProtocolAdapter adapter = mock(ProtocolAdapter.class);
        final ProtocolAdapterInformation info = mock(ProtocolAdapterInformation.class);
        when(adapter.getId()).thenReturn(adapterId);
        when(adapter.getProtocolAdapterInformation()).thenReturn(info);
        when(info.getProtocolId()).thenReturn("test-protocol");
        when(info.getCapabilities()).thenReturn(EnumSet.of(ProtocolAdapterCapability.READ));
        doAnswer(invocation -> {
                    final ProtocolAdapterStartOutput output = invocation.getArgument(2);
                    output.startedSuccessfully();
                    return null;
                })
                .when(adapter)
                .start(any(), any(), any());
        doAnswer(invocation -> {
                    final ProtocolAdapterStopOutput output = invocation.getArgument(2);
                    output.stoppedSuccessfully();
                    return null;
                })
                .when(adapter)
                .stop(any(), any(), any());
        return adapter;
    }

    private @NotNull ProtocolAdapter createFailingPrecheckAdapter(final @NotNull String adapterId) {
        final ProtocolAdapter adapter = createSuccessAdapter(adapterId);
        try {
            doThrow(new ProtocolAdapterException("precheck failed"))
                    .when(adapter)
                    .precheck();
        } catch (final ProtocolAdapterException e) {
            throw new RuntimeException(e);
        }
        return adapter;
    }

    /**
     * Adds an adapter directly to the manager's internal map by creating a wrapper
     * and using reflection to access the protected map.
     */
    private void addAdapterToManager(final @NotNull String adapterId, final @NotNull ProtocolAdapter adapter) {
        addAdapterToManager(adapterId, adapter, mock(ProtocolAdapterConfig.class));
    }

    private void addAdapterToManager(
            final @NotNull String adapterId,
            final @NotNull ProtocolAdapter adapter,
            final @NotNull ProtocolAdapterConfig adapterConfig) {
        addAdapterToManager(manager, adapterId, adapter, adapterConfig);
    }

    private void addAdapterToManager(
            final @NotNull ProtocolAdapterManager managerUnderTest,
            final @NotNull String adapterId,
            final @NotNull ProtocolAdapter adapter,
            final @NotNull ProtocolAdapterConfig adapterConfig) {
        final ProtocolAdapterFactory<?> factory = mock(ProtocolAdapterFactory.class);
        final ProtocolAdapterMetricsService metricsService = mock(ProtocolAdapterMetricsService.class);
        final ProtocolAdapterStateImpl state = mock(ProtocolAdapterStateImpl.class);
        final ModuleServices moduleServicesMock = mock(ModuleServices.class);

        final ProtocolAdapterWrapper wrapper = new ProtocolAdapterWrapper(
                adapter,
                adapterConfig,
                factory,
                adapter.getProtocolAdapterInformation(),
                metricsService,
                state,
                pollingService,
                eventService,
                moduleServicesMock,
                tagManager,
                northboundConsumerFactory,
                writingService,
                Runnable::run);
        try {
            final var field = ProtocolAdapterManager.class.getDeclaredField("protocolAdapterMap");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            final var map = (Map<String, ProtocolAdapterWrapper>) field.get(managerUnderTest);
            map.put(adapterId, wrapper);
        } catch (final Exception e) {
            throw new RuntimeException("Failed to add adapter to manager", e);
        }
    }

    private void addWrapperToManager(
            final @NotNull ProtocolAdapterManager managerUnderTest,
            final @NotNull String adapterId,
            final @NotNull ProtocolAdapterWrapper wrapper) {
        try {
            final var field = ProtocolAdapterManager.class.getDeclaredField("protocolAdapterMap");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            final var map = (Map<String, ProtocolAdapterWrapper>) field.get(managerUnderTest);
            map.put(adapterId, wrapper);
        } catch (final Exception e) {
            throw new RuntimeException("Failed to add adapter wrapper to manager", e);
        }
    }

    private void waitUntilNotBusy(final @NotNull ProtocolAdapterManager managerUnderTest) throws InterruptedException {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (managerUnderTest.isBusy() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertThat(managerUnderTest.isBusy()).isFalse();
        assertThat(managerUnderTest.getState()).isEqualTo(ProtocolAdapterManagerState.Idle);
    }

    @Nested
    class StartTests {

        @Test
        void start_adapterNotFound_throwsException() {
            assertThatThrownBy(() -> manager.start("nonexistent")).isInstanceOf(ProtocolAdapterException.class);
        }

        @Test
        void start_success_firesInfoEvent() throws ProtocolAdapterException {
            final ProtocolAdapter adapter = createSuccessAdapter("adapter-1");
            addAdapterToManager("adapter-1", adapter);

            manager.start("adapter-1");

            verify(eventService).createAdapterEvent("adapter-1", "test-protocol");
            verify(eventBuilder).withSeverity(Event.SEVERITY.INFO);
            verify(eventBuilder).withMessage("Adapter 'adapter-1' started OK.");
            verify(eventBuilder).fire();
        }

        @Test
        void start_failure_firesErrorEventAndThrows() throws ProtocolAdapterException {
            final ProtocolAdapter adapter = createFailingPrecheckAdapter("adapter-1");
            addAdapterToManager("adapter-1", adapter);

            assertThatThrownBy(() -> manager.start("adapter-1"))
                    .isInstanceOf(ProtocolAdapterException.class)
                    .hasMessageContaining("Failed to start adapter: adapter-1");

            verify(eventService).createAdapterEvent("adapter-1", "test-protocol");
            verify(eventBuilder).withSeverity(Event.SEVERITY.CRITICAL);
            verify(eventBuilder).withMessage("Error starting adapter 'adapter-1'.");
            verify(eventBuilder).fire();
        }
    }

    @Nested
    class StopTests {

        @Test
        void stop_adapterNotFound_throwsException() {
            assertThatThrownBy(() -> manager.stop("nonexistent", false)).isInstanceOf(ProtocolAdapterException.class);
        }

        @Test
        void stop_success_firesInfoEvent() throws ProtocolAdapterException {
            final ProtocolAdapter adapter = createSuccessAdapter("adapter-1");
            addAdapterToManager("adapter-1", adapter);

            manager.start("adapter-1");
            // Reset mocks to clear the start event interactions
            org.mockito.Mockito.reset(eventService, eventBuilder);
            when(eventService.createAdapterEvent(anyString(), anyString())).thenReturn(eventBuilder);
            when(eventBuilder.withSeverity(any())).thenReturn(eventBuilder);
            when(eventBuilder.withMessage(anyString())).thenReturn(eventBuilder);

            manager.stop("adapter-1", false);

            verify(eventService).createAdapterEvent("adapter-1", "test-protocol");
            verify(eventBuilder).withSeverity(Event.SEVERITY.INFO);
            verify(eventBuilder).withMessage("Adapter 'adapter-1' stopped OK.");
            verify(eventBuilder).fire();
        }

        @Test
        void stop_whenIdle_isIdempotent_firesSuccessEvent() throws ProtocolAdapterException {
            final ProtocolAdapter adapter = createSuccessAdapter("adapter-1");
            addAdapterToManager("adapter-1", adapter);

            // Stop from Idle short-circuits as a no-op success.
            manager.stop("adapter-1", false);

            verify(eventService).createAdapterEvent("adapter-1", "test-protocol");
            verify(eventBuilder).withSeverity(Event.SEVERITY.INFO);
            verify(eventBuilder).withMessage("Adapter 'adapter-1' stopped OK.");
            verify(eventBuilder).fire();
        }

        @Test
        void stop_withDestroy_callsDestroyOnAdapter() throws ProtocolAdapterException {
            final ProtocolAdapter adapter = createSuccessAdapter("adapter-1");
            addAdapterToManager("adapter-1", adapter);

            manager.start("adapter-1");
            manager.stop("adapter-1", true);

            verify(adapter).destroy();
        }
    }

    @Nested
    class WrapperLookup {

        @Test
        void getProtocolAdapterWrapperByAdapterId_found() {
            final ProtocolAdapter adapter = createSuccessAdapter("adapter-1");
            addAdapterToManager("adapter-1", adapter);

            assertThat(manager.getProtocolAdapterWrapperByAdapterId("adapter-1"))
                    .isPresent();
        }

        @Test
        void getProtocolAdapterWrapperByAdapterId_notFound() {
            assertThat(manager.getProtocolAdapterWrapperByAdapterId("nonexistent"))
                    .isEmpty();
        }
    }

    @Nested
    class DeleteTests {

        @Test
        void deleteProtocolAdapterByAdapterId_removesWrapper() {
            final ProtocolAdapter adapter = createSuccessAdapter("adapter-1");
            addAdapterToManager("adapter-1", adapter);

            assertThat(manager.getProtocolAdapterWrapperByAdapterId("adapter-1"))
                    .isPresent();

            manager.deleteProtocolAdapterByAdapterId("adapter-1");

            assertThat(manager.getProtocolAdapterWrapperByAdapterId("adapter-1"))
                    .isEmpty();
        }

        @Test
        void deleteProtocolAdapterByAdapterId_decreasesMetric() {
            final ProtocolAdapter adapter = createSuccessAdapter("adapter-1");
            addAdapterToManager("adapter-1", adapter);

            manager.deleteProtocolAdapterByAdapterId("adapter-1");

            verify(protocolAdapterMetrics).decreaseProtocolAdapterMetric("test-protocol");
        }

        @Test
        void deleteProtocolAdapterByAdapterId_firesEvent() {
            final ProtocolAdapter adapter = createSuccessAdapter("adapter-1");
            addAdapterToManager("adapter-1", adapter);

            manager.deleteProtocolAdapterByAdapterId("adapter-1");

            verify(eventService).createAdapterEvent("adapter-1", "test-protocol");
            verify(eventBuilder).withSeverity(Event.SEVERITY.WARN);
            verify(eventBuilder).fire();
        }

        @Test
        void deleteProtocolAdapterByAdapterId_nonexistent_doesNotThrow() {
            // Should not throw, just log a warning
            manager.deleteProtocolAdapterByAdapterId("nonexistent");

            // No event should be fired for nonexistent adapter
            verify(eventService, org.mockito.Mockito.never()).createAdapterEvent(anyString(), anyString());
        }
    }

    @Nested
    class AdapterIdSetTests {

        @Test
        void getProtocolAdapterIdSet_empty() {
            assertThat(manager.getProtocolAdapterIdSet()).isEmpty();
        }

        @Test
        void getProtocolAdapterIdSet_returnsAllIds() {
            addAdapterToManager("adapter-1", createSuccessAdapter("adapter-1"));
            addAdapterToManager("adapter-2", createSuccessAdapter("adapter-2"));
            addAdapterToManager("adapter-3", createSuccessAdapter("adapter-3"));

            assertThat(manager.getProtocolAdapterIdSet())
                    .containsExactlyInAnyOrder("adapter-1", "adapter-2", "adapter-3");
        }

        @Test
        void getProtocolAdapterIdSet_returnsDefensiveCopy() {
            addAdapterToManager("adapter-1", createSuccessAdapter("adapter-1"));

            final var idSet = manager.getProtocolAdapterIdSet();
            idSet.add("injected");

            // Should not affect the manager's internal state
            assertThat(manager.getProtocolAdapterIdSet()).containsExactly("adapter-1");
        }
    }

    @Nested
    class StartStopInteraction {

        @Test
        void startThenStopThenStart_succeeds() throws ProtocolAdapterException {
            final ProtocolAdapter adapter = createSuccessAdapter("adapter-1");
            addAdapterToManager("adapter-1", adapter);

            manager.start("adapter-1");
            manager.stop("adapter-1", false);

            // Reset event mocks for clean verification
            org.mockito.Mockito.reset(eventService, eventBuilder);
            when(eventService.createAdapterEvent(anyString(), anyString())).thenReturn(eventBuilder);
            when(eventBuilder.withSeverity(any())).thenReturn(eventBuilder);
            when(eventBuilder.withMessage(anyString())).thenReturn(eventBuilder);

            manager.start("adapter-1");

            verify(eventBuilder).withSeverity(Event.SEVERITY.INFO);
            verify(eventBuilder).withMessage("Adapter 'adapter-1' started OK.");
        }

        @Test
        void startTwice_secondThrows() throws ProtocolAdapterException {
            final ProtocolAdapter adapter = createSuccessAdapter("adapter-1");
            addAdapterToManager("adapter-1", adapter);

            manager.start("adapter-1");

            assertThatThrownBy(() -> manager.start("adapter-1"))
                    .isInstanceOf(ProtocolAdapterException.class)
                    .hasMessageContaining("Failed to start adapter: adapter-1");
        }
    }

    @Nested
    class StopFailureTests {

        @Test
        void stop_whenAdapterStartFails_firesWarnEvent() throws ProtocolAdapterException {
            // Adapter that fails start → goes to Error state → stop from Error fires WARN
            final ProtocolAdapter adapter = createFailingPrecheckAdapter("adapter-1");
            addAdapterToManager("adapter-1", adapter);

            // Start fails (precheck throws), adapter is in Error state
            assertThatThrownBy(() -> manager.start("adapter-1")).isInstanceOf(ProtocolAdapterException.class);

            // Reset mocks for stop verification
            org.mockito.Mockito.reset(eventService, eventBuilder);
            when(eventService.createAdapterEvent(anyString(), anyString())).thenReturn(eventBuilder);
            when(eventBuilder.withSeverity(any())).thenReturn(eventBuilder);
            when(eventBuilder.withMessage(anyString())).thenReturn(eventBuilder);

            // Stop from Error state should succeed (transitions Error→Idle)
            manager.stop("adapter-1", false);

            verify(eventService).createAdapterEvent("adapter-1", "test-protocol");
            verify(eventBuilder).withSeverity(Event.SEVERITY.INFO);
            verify(eventBuilder).withMessage("Adapter 'adapter-1' stopped OK.");
            verify(eventBuilder).fire();
        }

        @Test
        void stop_whenDisconnectFails_firesCriticalEvent() throws ProtocolAdapterException {
            final ProtocolAdapter adapter = createSuccessAdapter("adapter-1");
            doThrow(new RuntimeException("disconnect failed")).when(adapter).stop(any(), any(), any());
            addAdapterToManager("adapter-1", adapter);

            manager.start("adapter-1");

            org.mockito.Mockito.reset(eventService, eventBuilder);
            when(eventService.createAdapterEvent(anyString(), anyString())).thenReturn(eventBuilder);
            when(eventBuilder.withSeverity(any())).thenReturn(eventBuilder);
            when(eventBuilder.withMessage(anyString())).thenReturn(eventBuilder);

            manager.stop("adapter-1", false);

            verify(eventService).createAdapterEvent("adapter-1", "test-protocol");
            verify(eventBuilder).withSeverity(Event.SEVERITY.CRITICAL);
            verify(eventBuilder).withMessage("Error stopping adapter 'adapter-1'.");
            verify(eventBuilder).fire();
        }
    }

    @Nested
    class AsyncTests {

        @Test
        void startAsync_success() throws Exception {
            final ProtocolAdapter adapter = createSuccessAdapter("adapter-1");
            addAdapterToManager("adapter-1", adapter);

            manager.startAsync("adapter-1").get();

            verify(eventService).createAdapterEvent("adapter-1", "test-protocol");
        }

        @Test
        void stopAsync_success() throws Exception {
            final ProtocolAdapter adapter = createSuccessAdapter("adapter-1");
            addAdapterToManager("adapter-1", adapter);

            manager.startAsync("adapter-1").get();

            // Reset mocks for stop verification
            org.mockito.Mockito.reset(eventService, eventBuilder);
            when(eventService.createAdapterEvent(anyString(), anyString())).thenReturn(eventBuilder);
            when(eventBuilder.withSeverity(any())).thenReturn(eventBuilder);
            when(eventBuilder.withMessage(anyString())).thenReturn(eventBuilder);

            manager.stopAsync("adapter-1", false).get();

            verify(eventService).createAdapterEvent("adapter-1", "test-protocol");
            verify(eventBuilder).withSeverity(Event.SEVERITY.INFO);
            verify(eventBuilder).withMessage("Adapter 'adapter-1' stopped OK.");
        }

        @Test
        void startAsync_adapterNotFound_throwsException() {
            assertThat(manager.startAsync("nonexistent")).isCompletedExceptionally();
        }
    }

    @Nested
    class ShutdownBehaviorTests {

        @Test
        void shutdown_usesAsyncStopPathWithTimeoutGuard() {
            final ProtocolAdapterWrapper wrapper = mock(ProtocolAdapterWrapper.class);
            when(wrapper.stopAsync(true)).thenReturn(CompletableFuture.completedFuture(null));
            addWrapperToManager(manager, "adapter-1", wrapper);

            manager.shutdown();

            verify(wrapper).stopAsync(true);
            verify(wrapper, org.mockito.Mockito.never()).stop(true);
        }
    }

    /**
     * A configuration that cannot be read belongs to one adapter, not to the deployment.
     *
     * <p>The conversion used to be a single stream with a {@code Collectors.toMap} terminal, so any
     * throw from {@code fromEntity} — an unrecognised field, a value that will not coerce, a duplicated
     * adapter id — aborted the refresh before a single adapter was created, updated or deleted, and was
     * swallowed by the outer catch with no event fired.
     */
    @Nested
    class RefreshIsolationTests {

        private @NotNull LogbackCapturingAppender logCapture;

        @BeforeEach
        void weaveLogCapture() {
            logCapture =
                    LogbackCapturingAppender.Factory.weaveInto(LoggerFactory.getLogger(ProtocolAdapterManager.class));
        }

        @AfterEach
        void detachLogCapture() {
            LogbackCapturingAppender.Factory.cleanUp();
        }

        @Test
        void anUnreadableConfig_doesNotStopTheOtherAdaptersFromBeingRefreshed() throws Exception {
            final ProtocolAdapterManager spyManager = org.mockito.Mockito.spy(manager);
            org.mockito.Mockito.doNothing().when(spyManager).createProtocolAdapter(any(), anyString());
            org.mockito.Mockito.doNothing().when(spyManager).start(anyString());
            when(versionProvider.getVersion()).thenReturn("test-version");

            final ProtocolAdapterEntity unreadable = entity("unreadable-adapter");
            final ProtocolAdapterEntity healthy = entity("healthy-adapter");
            final ProtocolAdapterConfig healthyConfig = config("healthy-adapter");
            when(configConverter.fromEntity(unreadable))
                    .thenThrow(new IllegalArgumentException("Unrecognized field \"hostame\""));
            when(configConverter.fromEntity(healthy)).thenReturn(healthyConfig);

            spyManager.refresh(List.of(unreadable, healthy));
            waitUntilNotBusy(spyManager);

            verify(spyManager).createProtocolAdapter(healthyConfig, "test-version");
            verify(spyManager).start("healthy-adapter");
            verify(spyManager, org.mockito.Mockito.never()).start("unreadable-adapter");
        }

        @Test
        void anUnreadableConfigForARunningAdapter_doesNotDeleteIt() throws Exception {
            // The regression guard for the obvious-but-wrong fix. Drop a failed adapter from the map of
            // converted configs and it disappears from the new-id set, which makes it look deleted -
            // so a running, healthy adapter gets stopped and removed because its new configuration had
            // a typo in it. That is worse than the bug being fixed, where nothing happened at all.
            final ProtocolAdapterManager spyManager = org.mockito.Mockito.spy(manager);
            addAdapterToManager(spyManager, "adapter-1", createSuccessAdapter("adapter-1"), config("adapter-1"));

            final ProtocolAdapterEntity unreadable = entity("adapter-1");
            when(configConverter.fromEntity(unreadable))
                    .thenThrow(new IllegalArgumentException("Cannot coerce empty String"));

            spyManager.refresh(List.of(unreadable));
            waitUntilNotBusy(spyManager);

            verify(spyManager, org.mockito.Mockito.never()).stop("adapter-1", true);
            verify(spyManager, org.mockito.Mockito.never()).deleteProtocolAdapterByAdapterId("adapter-1");
            verify(spyManager, org.mockito.Mockito.never()).createProtocolAdapter(any(), anyString());
            assertThat(spyManager.getProtocolAdapterIdSet()).containsExactly("adapter-1");
        }

        @Test
        void anUnreadableConfigForANewAdapter_doesNotCreateIt() throws Exception {
            // The other half of the outcome-neutral message: with no existing instance there is
            // nothing to leave unchanged, and no adapter must come into being from a configuration
            // that could not be read.
            final ProtocolAdapterManager spyManager = org.mockito.Mockito.spy(manager);
            final ProtocolAdapterEntity unreadable = entity("brand-new-adapter");
            when(configConverter.fromEntity(unreadable))
                    .thenThrow(new IllegalArgumentException("Cannot coerce empty String"));

            spyManager.refresh(List.of(unreadable));
            waitUntilNotBusy(spyManager);

            verify(spyManager, org.mockito.Mockito.never()).createProtocolAdapter(any(), anyString());
            verify(spyManager, org.mockito.Mockito.never()).start("brand-new-adapter");
            assertThat(spyManager.getProtocolAdapterIdSet()).isEmpty();
        }

        @Test
        void aDuplicatedAdapterId_doesNotStopTheOtherAdaptersFromBeingRefreshed() throws Exception {
            // Collectors.toMap threw IllegalStateException on a duplicate key, which killed the refresh
            // the same way. Which of the two configurations the operator meant is not knowable, so
            // neither is applied and that adapter alone is left as it is.
            final ProtocolAdapterManager spyManager = org.mockito.Mockito.spy(manager);
            org.mockito.Mockito.doNothing().when(spyManager).createProtocolAdapter(any(), anyString());
            org.mockito.Mockito.doNothing().when(spyManager).start(anyString());
            when(versionProvider.getVersion()).thenReturn("test-version");

            final ProtocolAdapterEntity first = entity("duplicated");
            final ProtocolAdapterEntity second = entity("duplicated");
            final ProtocolAdapterEntity healthy = entity("healthy-adapter");
            // Built before the stubbing that returns them: a mock created inside when(...) reads to
            // Mockito as an unfinished stubbing.
            final ProtocolAdapterConfig firstConfig = config("duplicated");
            final ProtocolAdapterConfig secondConfig = config("duplicated");
            final ProtocolAdapterConfig healthyConfig = config("healthy-adapter");
            when(configConverter.fromEntity(first)).thenReturn(firstConfig);
            when(configConverter.fromEntity(second)).thenReturn(secondConfig);
            when(configConverter.fromEntity(healthy)).thenReturn(healthyConfig);

            spyManager.refresh(List.of(first, second, healthy));
            waitUntilNotBusy(spyManager);

            verify(spyManager).createProtocolAdapter(healthyConfig, "test-version");
            verify(spyManager, org.mockito.Mockito.never()).start("duplicated");
        }

        @Test
        void anUnreadableConfig_firesTheConfigurationFailedEvent() throws Exception {
            // Both event branches used to sit after the throw point, so the operator got a single stack
            // trace and nothing at all in the event stream.
            final ProtocolAdapterManager spyManager = org.mockito.Mockito.spy(manager);
            final ProtocolAdapterEntity unreadable = entity("unreadable-adapter");
            when(configConverter.fromEntity(unreadable))
                    .thenThrow(new IllegalArgumentException("Unrecognized field \"hostame\""));

            spyManager.refresh(List.of(unreadable));
            waitUntilNotBusy(spyManager);

            verify(eventBuilder).withMessage("Reloading of configuration failed");
            verify(eventBuilder, org.mockito.Mockito.never())
                    .withMessage("Configuration has been successfully updated");
        }

        @Test
        void aDuplicatedId_whoseFirstEntityFailsConversion_isStillReportedAsDuplicated() throws Exception {
            // The duplicate ERROR used to be gated on failedAdapterSet.add, which the conversion
            // failure had already claimed - so the duplicate was never logged.
            final ProtocolAdapterManager spyManager = org.mockito.Mockito.spy(manager);
            org.mockito.Mockito.doNothing().when(spyManager).createProtocolAdapter(any(), anyString());
            org.mockito.Mockito.doNothing().when(spyManager).start(anyString());
            when(versionProvider.getVersion()).thenReturn("test-version");

            final ProtocolAdapterEntity broken = entity("duplicated");
            final ProtocolAdapterEntity valid = entity("duplicated");
            final ProtocolAdapterConfig validConfig = config("duplicated");
            when(configConverter.fromEntity(broken))
                    .thenThrow(new IllegalArgumentException("Unrecognized field \"hostame\""));
            when(configConverter.fromEntity(valid)).thenReturn(validConfig);

            spyManager.refresh(List.of(broken, valid));
            waitUntilNotBusy(spyManager);

            assertThat(duplicatedIdErrorCount("duplicated")).isEqualTo(1);
            verify(spyManager, org.mockito.Mockito.never()).createProtocolAdapter(any(), anyString());
            verify(spyManager, org.mockito.Mockito.never()).start("duplicated");
            verify(eventBuilder).withMessage("Reloading of configuration failed");
        }

        @Test
        void aDuplicatedId_whoseSecondEntityFailsConversion_doesNotApplyTheValidConfig() throws Exception {
            // The failure branch used to miss the duplicate entirely, leaving the first entity's
            // config in the converted map with no duplicate diagnostic at all.
            final ProtocolAdapterManager spyManager = org.mockito.Mockito.spy(manager);
            org.mockito.Mockito.doNothing().when(spyManager).createProtocolAdapter(any(), anyString());
            org.mockito.Mockito.doNothing().when(spyManager).start(anyString());
            when(versionProvider.getVersion()).thenReturn("test-version");

            final ProtocolAdapterEntity valid = entity("duplicated");
            final ProtocolAdapterEntity broken = entity("duplicated");
            final ProtocolAdapterConfig validConfig = config("duplicated");
            when(configConverter.fromEntity(valid)).thenReturn(validConfig);
            when(configConverter.fromEntity(broken))
                    .thenThrow(new IllegalArgumentException("Cannot coerce empty String"));

            spyManager.refresh(List.of(valid, broken));
            waitUntilNotBusy(spyManager);

            assertThat(duplicatedIdErrorCount("duplicated")).isEqualTo(1);
            verify(spyManager, org.mockito.Mockito.never()).createProtocolAdapter(any(), anyString());
            verify(spyManager, org.mockito.Mockito.never()).start("duplicated");
            verify(eventBuilder).withMessage("Reloading of configuration failed");
        }

        @Test
        void threeEntitiesSharingAnId_reportTheDuplicateOnce() throws Exception {
            final ProtocolAdapterManager spyManager = org.mockito.Mockito.spy(manager);
            org.mockito.Mockito.doNothing().when(spyManager).createProtocolAdapter(any(), anyString());
            org.mockito.Mockito.doNothing().when(spyManager).start(anyString());
            when(versionProvider.getVersion()).thenReturn("test-version");

            final ProtocolAdapterEntity first = entity("duplicated");
            final ProtocolAdapterEntity second = entity("duplicated");
            final ProtocolAdapterEntity third = entity("duplicated");
            final ProtocolAdapterConfig firstConfig = config("duplicated");
            final ProtocolAdapterConfig secondConfig = config("duplicated");
            final ProtocolAdapterConfig thirdConfig = config("duplicated");
            when(configConverter.fromEntity(first)).thenReturn(firstConfig);
            when(configConverter.fromEntity(second)).thenReturn(secondConfig);
            when(configConverter.fromEntity(third)).thenReturn(thirdConfig);

            spyManager.refresh(List.of(first, second, third));
            waitUntilNotBusy(spyManager);

            assertThat(duplicatedIdErrorCount("duplicated")).isEqualTo(1);
            verify(spyManager, org.mockito.Mockito.never()).start("duplicated");
        }

        @Test
        void anUnreadableConfig_firesAnAdapterScopedCriticalEvent() throws Exception {
            // The global configuration event says the reload failed; the adapter-scoped event names
            // the adapter whose configuration could not be read.
            final ProtocolAdapterManager spyManager = org.mockito.Mockito.spy(manager);
            final ProtocolAdapterEntity unreadable = entity("unreadable-adapter");
            when(configConverter.fromEntity(unreadable))
                    .thenThrow(new IllegalArgumentException("Unrecognized field \"hostame\""));

            spyManager.refresh(List.of(unreadable));
            waitUntilNotBusy(spyManager);

            verify(eventService).createAdapterEvent("unreadable-adapter", "test-protocol");
            // CRITICAL is set twice on the shared builder mock: once for the adapter-scoped event,
            // once for the global configuration event.
            verify(eventBuilder, times(2)).withSeverity(Event.SEVERITY.CRITICAL);
            verify(eventBuilder)
                    .withMessage("Adapter 'unreadable-adapter' configuration could not be read. "
                            + "An existing instance, if any, was left unchanged; a new adapter was not created.");
        }

        @Test
        void aConverterThrowingNoClassDefFoundError_isContainedLikeAnException() throws Exception {
            // A broken adapter-module classloader surfaces as NoClassDefFoundError, which
            // catch (Exception) does not cover; it must not abort the whole refresh.
            final ProtocolAdapterManager spyManager = org.mockito.Mockito.spy(manager);
            org.mockito.Mockito.doNothing().when(spyManager).createProtocolAdapter(any(), anyString());
            org.mockito.Mockito.doNothing().when(spyManager).start(anyString());
            when(versionProvider.getVersion()).thenReturn("test-version");

            final ProtocolAdapterEntity broken = entity("broken-module-adapter");
            final ProtocolAdapterEntity healthy = entity("healthy-adapter");
            final ProtocolAdapterConfig healthyConfig = config("healthy-adapter");
            when(configConverter.fromEntity(broken))
                    .thenThrow(new NoClassDefFoundError("com/example/adapter/MissingDependency"));
            when(configConverter.fromEntity(healthy)).thenReturn(healthyConfig);

            spyManager.refresh(List.of(broken, healthy));
            waitUntilNotBusy(spyManager);

            verify(spyManager).createProtocolAdapter(healthyConfig, "test-version");
            verify(spyManager).start("healthy-adapter");
            verify(spyManager, org.mockito.Mockito.never()).start("broken-module-adapter");
            verify(eventBuilder).withMessage("Reloading of configuration failed");
        }

        private long duplicatedIdErrorCount(final @NotNull String adapterId) {
            return logCapture.getCapturedLogs().stream()
                    .filter(event -> event.getLevel() == Level.ERROR)
                    .filter(event -> event.getFormattedMessage()
                            .startsWith("Adapter id '" + adapterId + "' is used by more than one adapter"))
                    .count();
        }

        private @NotNull ProtocolAdapterEntity entity(final @NotNull String adapterId) {
            final ProtocolAdapterEntity entity = mock(ProtocolAdapterEntity.class);
            when(entity.getAdapterId()).thenReturn(adapterId);
            when(entity.getProtocolId()).thenReturn("test-protocol");
            return entity;
        }

        private @NotNull ProtocolAdapterConfig config(final @NotNull String adapterId) {
            final ProtocolAdapterConfig config = mock(ProtocolAdapterConfig.class);
            when(config.getAdapterId()).thenReturn(adapterId);
            return config;
        }
    }

    @Nested
    class RefreshParityAndConcurrencyTests {

        @Test
        void refresh_unchangedConfig_doesNotRestartAdapter() throws Exception {
            final ProtocolAdapterManager spyManager = org.mockito.Mockito.spy(manager);
            final ProtocolAdapter adapter = createSuccessAdapter("adapter-1");
            final ProtocolAdapterConfig unchangedConfig = mock(ProtocolAdapterConfig.class);
            final ProtocolAdapterEntity entity = mock(ProtocolAdapterEntity.class);

            when(unchangedConfig.getAdapterId()).thenReturn("adapter-1");
            when(configConverter.fromEntity(entity)).thenReturn(unchangedConfig);
            addAdapterToManager(spyManager, "adapter-1", adapter, unchangedConfig);

            spyManager.refresh(List.of(entity));
            waitUntilNotBusy(spyManager);

            verify(spyManager, org.mockito.Mockito.never()).stop("adapter-1", true);
            verify(spyManager, org.mockito.Mockito.never()).deleteProtocolAdapterByAdapterId("adapter-1");
            verify(spyManager, org.mockito.Mockito.never()).createProtocolAdapter(any(), anyString());
            verify(spyManager, org.mockito.Mockito.never()).start("adapter-1");
        }

        @Test
        void createProtocolAdapter_concurrentCalls_sameAdapterId_isAtomicAndCountsMetricOnce() throws Exception {
            final ProtocolAdapterConfig config = mock(ProtocolAdapterConfig.class);
            final ProtocolSpecificAdapterConfig adapterConfig = mock(ProtocolSpecificAdapterConfig.class);
            final ProtocolAdapterFactory<?> factory = mock(ProtocolAdapterFactory.class);
            final ProtocolAdapterInformation info = mock(ProtocolAdapterInformation.class);
            final ProtocolAdapter createdAdapter = mock(ProtocolAdapter.class);

            when(config.getAdapterId()).thenReturn("adapter-1");
            when(config.getProtocolId()).thenReturn("test-protocol");
            when(config.getAdapterConfig()).thenReturn(adapterConfig);
            org.mockito.Mockito.doReturn(List.of()).when(config).getTags();
            when(config.getNorthboundMappings()).thenReturn(List.<NorthboundMapping>of());
            when(config.getSouthboundMappings()).thenReturn(List.<SouthboundMapping>of());
            when(config.missingTags()).thenReturn(Optional.empty());

            when(factoryManager.get("test-protocol")).thenReturn(Optional.of(factory));
            when(factory.getInformation()).thenReturn(info);
            when(info.getProtocolId()).thenReturn("test-protocol");
            when(factory.createAdapter(any(), any())).thenReturn(createdAdapter);
            when(createdAdapter.getId()).thenReturn("adapter-1");
            when(createdAdapter.getProtocolAdapterInformation()).thenReturn(info);

            final int calls = 16;
            final ExecutorService executor = Executors.newFixedThreadPool(calls);
            final CountDownLatch startLatch = new CountDownLatch(1);
            final List<CompletableFuture<Void>> futures = new java.util.ArrayList<>(calls);

            try {
                for (int i = 0; i < calls; i++) {
                    futures.add(CompletableFuture.runAsync(
                            () -> {
                                try {
                                    startLatch.await(2, TimeUnit.SECONDS);
                                    manager.createProtocolAdapter(config, "1.0.0");
                                } catch (final InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            },
                            executor));
                }
                startLatch.countDown();
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .get(5, TimeUnit.SECONDS);
            } finally {
                executor.shutdownNow();
            }

            assertThat(manager.getProtocolAdapters()).hasSize(1);
            verify(factory, times(1)).createAdapter(any(), any());
            verify(protocolAdapterMetrics, times(1)).increaseProtocolAdapterMetric("test-protocol");
        }

        @Test
        void isBusy_trueWhileRefreshRuns_thenFalseAfterCompletion() throws Exception {
            final ProtocolAdapterManager spyManager = org.mockito.Mockito.spy(manager);
            final ProtocolAdapterEntity entity = mock(ProtocolAdapterEntity.class);
            final ProtocolAdapterConfig config = mock(ProtocolAdapterConfig.class);
            final CountDownLatch startCalled = new CountDownLatch(1);
            final CountDownLatch continueStart = new CountDownLatch(1);

            when(config.getAdapterId()).thenReturn("adapter-1");
            when(configConverter.fromEntity(entity)).thenReturn(config);
            when(versionProvider.getVersion()).thenReturn("1.0.0");

            org.mockito.Mockito.doNothing().when(spyManager).createProtocolAdapter(any(), anyString());
            org.mockito.Mockito.doAnswer(invocation -> {
                        startCalled.countDown();
                        continueStart.await(2, TimeUnit.SECONDS);
                        return null;
                    })
                    .when(spyManager)
                    .start("adapter-1");

            spyManager.refresh(List.of(entity));

            assertThat(spyManager.isBusy()).isTrue();
            assertThat(spyManager.getState()).isEqualTo(ProtocolAdapterManagerState.Running);
            assertThat(startCalled.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(spyManager.isBusy()).isTrue();
            assertThat(spyManager.getState()).isEqualTo(ProtocolAdapterManagerState.Running);

            continueStart.countDown();
            waitUntilNotBusy(spyManager);
        }

        @Test
        void refresh_changedConfig_swapsInPlaceWithoutDeletingTheAdapter() throws Exception {
            // EDG-602: an update must never remove the adapter id from the map. The previous delete-then-recreate
            // sequence left a window where getAdapterDomainTags resolved no wrapper and returned 404. The new
            // config is now swapped in place — the old wrapper is stopped, the new one started, and the adapter
            // stays resolvable throughout.
            final ProtocolAdapter oldAdapter = createSuccessAdapter("adapter-1");
            final ProtocolAdapterConfig oldConfig = mock(ProtocolAdapterConfig.class);
            addAdapterToManager("adapter-1", oldAdapter, oldConfig);
            manager.start("adapter-1");

            final ProtocolAdapterEntity entity = mock(ProtocolAdapterEntity.class);
            final ProtocolAdapterConfig newConfig = mock(ProtocolAdapterConfig.class);
            final ProtocolSpecificAdapterConfig newAdapterConfig = mock(ProtocolSpecificAdapterConfig.class);
            final ProtocolAdapterFactory<?> factory = mock(ProtocolAdapterFactory.class);
            final ProtocolAdapterInformation info = mock(ProtocolAdapterInformation.class);
            final ProtocolAdapter newAdapter = createSuccessAdapter("adapter-1");

            when(configConverter.fromEntity(entity)).thenReturn(newConfig);
            when(versionProvider.getVersion()).thenReturn("1.0.0");
            when(newConfig.getAdapterId()).thenReturn("adapter-1");
            when(newConfig.getProtocolId()).thenReturn("test-protocol");
            when(newConfig.getAdapterConfig()).thenReturn(newAdapterConfig);
            org.mockito.Mockito.doReturn(List.of()).when(newConfig).getTags();
            when(newConfig.getNorthboundMappings()).thenReturn(List.<NorthboundMapping>of());
            when(newConfig.getSouthboundMappings()).thenReturn(List.<SouthboundMapping>of());
            when(newConfig.missingTags()).thenReturn(Optional.empty());
            when(factoryManager.get("test-protocol")).thenReturn(Optional.of(factory));
            when(factory.getInformation()).thenReturn(info);
            when(info.getProtocolId()).thenReturn("test-protocol");
            when(factory.createAdapter(any(), any())).thenReturn(newAdapter);

            manager.refresh(List.of(entity));
            waitUntilNotBusy(manager);

            // the adapter id never disappeared: it resolves, and to the NEW wrapper (new config swapped in)
            assertThat(manager.getProtocolAdapterWrapperByAdapterId("adapter-1"))
                    .isPresent()
                    .get()
                    .extracting(ProtocolAdapterWrapper::getConfig)
                    .isSameAs(newConfig);
            // the displaced instance was stopped and destroyed; the replacement was started
            verify(oldAdapter).stop(any(), any(), any());
            verify(oldAdapter).destroy();
            verify(newAdapter).start(any(), any(), any());
            // an update is metric-neutral and must never run the delete path that caused the 404
            verify(protocolAdapterMetrics, org.mockito.Mockito.never()).decreaseProtocolAdapterMetric(anyString());
            verify(protocolAdapterMetrics, org.mockito.Mockito.never()).increaseProtocolAdapterMetric(anyString());
        }
    }
}
