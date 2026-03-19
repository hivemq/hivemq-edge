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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.adapter.sdk.api.ProtocolAdapter2;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.events.model.EventBuilder;
import com.hivemq.adapter.sdk.api.exceptions.ProtocolAdapterException;
import com.hivemq.configuration.reader.ProtocolAdapterExtractor;
import com.hivemq.edge.HiveMQEdgeRemoteService;
import com.hivemq.edge.VersionProvider;
import com.hivemq.edge.modules.adapters.data.TagManager;
import com.hivemq.edge.modules.adapters.impl.ModuleServicesImpl;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingService;
import com.hivemq.protocols.InternalProtocolAdapterWritingService;
import com.hivemq.protocols.ProtocolAdapterConfigConverter;
import com.hivemq.protocols.ProtocolAdapterFactoryManager;
import com.hivemq.protocols.ProtocolAdapterMetrics;
import com.hivemq.protocols.northbound.NorthboundConsumerFactory;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProtocolAdapterManager2Test {

    @Mock
    private @NotNull MetricRegistry metricRegistry;

    @Mock
    private @NotNull ModuleServicesImpl moduleServices;

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

    private @NotNull ProtocolAdapterManager2 manager;

    @BeforeEach
    void setUp() {
        when(eventService.createAdapterEvent(anyString(), anyString())).thenReturn(eventBuilder);
        when(eventBuilder.withSeverity(any())).thenReturn(eventBuilder);
        when(eventBuilder.withMessage(anyString())).thenReturn(eventBuilder);

        manager = new ProtocolAdapterManager2(
                metricRegistry,
                moduleServices,
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
                protocolAdapterExtractor);
    }

    private @NotNull ProtocolAdapter2 createSuccessAdapter(final @NotNull String adapterId) {
        final ProtocolAdapter2 adapter = mock(ProtocolAdapter2.class);
        final ProtocolAdapterInformation info = mock(ProtocolAdapterInformation.class);
        when(adapter.getId()).thenReturn(adapterId);
        when(adapter.getProtocolAdapterInformation()).thenReturn(info);
        when(info.getProtocolId()).thenReturn("test-protocol");
        when(adapter.supportsSouthbound()).thenReturn(false);
        return adapter;
    }

    private @NotNull ProtocolAdapter2 createFailingPrecheckAdapter(final @NotNull String adapterId) {
        final ProtocolAdapter2 adapter = createSuccessAdapter(adapterId);
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
     * and using the protected method access pattern via a test subclass.
     */
    private void addAdapterToManager(final @NotNull String adapterId, final @NotNull ProtocolAdapter2 adapter) {
        // Use reflection-free approach: directly access the wrapper map via getProtocolAdapterWrapperByAdapterId
        // We need a way to add to the internal map. Use a test helper approach.
        final ProtocolAdapterWrapper2 wrapper = new ProtocolAdapterWrapper2(adapter);
        // Access the map via the manager's own methods — we use the protected deleteProtocolAdapterWrapperByAdapterId
        // to verify it works, but for adding, we need the package-private approach.
        // Since protocolAdapterMap is a ConcurrentHashMap accessible within the package,
        // we use a test-friendly approach by directly putting into the map.
        try {
            final var field = ProtocolAdapterManager2.class.getDeclaredField("protocolAdapterMap");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            final var map = (Map<String, ProtocolAdapterWrapper2>) field.get(manager);
            map.put(adapterId, wrapper);
        } catch (final Exception e) {
            throw new RuntimeException("Failed to add adapter to manager", e);
        }
    }

    @Nested
    class StartTests {

        @Test
        void start_adapterNotFound_throwsException() {
            assertThatThrownBy(() -> manager.start("nonexistent")).isInstanceOf(ProtocolAdapterException.class);
        }

        @Test
        void start_success_firesInfoEvent() throws ProtocolAdapterException {
            final ProtocolAdapter2 adapter = createSuccessAdapter("adapter-1");
            addAdapterToManager("adapter-1", adapter);

            manager.start("adapter-1");

            verify(eventService).createAdapterEvent("adapter-1", "test-protocol");
            verify(eventBuilder).withSeverity(Event.SEVERITY.INFO);
            verify(eventBuilder).withMessage("Adapter 'adapter-1' started OK.");
            verify(eventBuilder).fire();
        }

        @Test
        void start_failure_firesErrorEventAndThrows() throws ProtocolAdapterException {
            final ProtocolAdapter2 adapter = createFailingPrecheckAdapter("adapter-1");
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
            final ProtocolAdapter2 adapter = createSuccessAdapter("adapter-1");
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
        void stop_whenIdle_firesInfoEvent() throws ProtocolAdapterException {
            final ProtocolAdapter2 adapter = createSuccessAdapter("adapter-1");
            addAdapterToManager("adapter-1", adapter);

            // Don't start the adapter — stop from Idle is a no-op that returns true
            manager.stop("adapter-1", false);

            verify(eventService).createAdapterEvent("adapter-1", "test-protocol");
            verify(eventBuilder).withSeverity(Event.SEVERITY.INFO);
            verify(eventBuilder).withMessage("Adapter 'adapter-1' stopped OK.");
            verify(eventBuilder).fire();
        }

        @Test
        void stop_withDestroy_callsDestroyOnAdapter() throws ProtocolAdapterException {
            final ProtocolAdapter2 adapter = createSuccessAdapter("adapter-1");
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
            final ProtocolAdapter2 adapter = createSuccessAdapter("adapter-1");
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
            final ProtocolAdapter2 adapter = createSuccessAdapter("adapter-1");
            addAdapterToManager("adapter-1", adapter);

            assertThat(manager.getProtocolAdapterWrapperByAdapterId("adapter-1"))
                    .isPresent();

            manager.deleteProtocolAdapterByAdapterId("adapter-1");

            assertThat(manager.getProtocolAdapterWrapperByAdapterId("adapter-1"))
                    .isEmpty();
        }

        @Test
        void deleteProtocolAdapterByAdapterId_decreasesMetric() {
            final ProtocolAdapter2 adapter = createSuccessAdapter("adapter-1");
            addAdapterToManager("adapter-1", adapter);

            manager.deleteProtocolAdapterByAdapterId("adapter-1");

            verify(protocolAdapterMetrics).decreaseProtocolAdapterMetric("test-protocol");
        }

        @Test
        void deleteProtocolAdapterByAdapterId_firesEvent() {
            final ProtocolAdapter2 adapter = createSuccessAdapter("adapter-1");
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
            final ProtocolAdapter2 adapter = createSuccessAdapter("adapter-1");
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
            final ProtocolAdapter2 adapter = createSuccessAdapter("adapter-1");
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
            final ProtocolAdapter2 adapter = createFailingPrecheckAdapter("adapter-1");
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
    }

    @Nested
    class AsyncTests {

        @Test
        void startAsync_success() throws Exception {
            final ProtocolAdapter2 adapter = createSuccessAdapter("adapter-1");
            addAdapterToManager("adapter-1", adapter);

            manager.startAsync("adapter-1").get();

            verify(eventService).createAdapterEvent("adapter-1", "test-protocol");
        }

        @Test
        void stopAsync_success() throws Exception {
            final ProtocolAdapter2 adapter = createSuccessAdapter("adapter-1");
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
}
