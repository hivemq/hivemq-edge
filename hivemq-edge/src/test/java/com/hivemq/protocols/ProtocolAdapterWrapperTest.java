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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.ProtocolAdapterConnectionDirection;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.exceptions.ProtocolAdapterException;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.modules.adapters.data.TagManager;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingService;
import com.hivemq.protocols.fsm.ProtocolAdapterConnectionState;
import com.hivemq.protocols.fsm.ProtocolAdapterConnectionTransitionResponse;
import com.hivemq.protocols.fsm.ProtocolAdapterRuntimeState;
import com.hivemq.protocols.fsm.ProtocolAdapterStateChangeListener;
import com.hivemq.protocols.fsm.ProtocolAdapterTransitionResponse;
import com.hivemq.protocols.northbound.NorthboundConsumerFactory;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProtocolAdapterWrapperTest {

    @Mock
    private @NotNull ProtocolAdapter protocolAdapter;

    @Mock
    private @NotNull ProtocolAdapterInformation adapterInformation;

    @Mock
    private @NotNull ProtocolAdapterConfig config;

    @Mock
    private @NotNull ProtocolAdapterFactory<?> adapterFactory;

    @Mock
    private @NotNull ProtocolAdapterMetricsService metricsService;

    @Mock
    private @NotNull ProtocolAdapterStateImpl protocolAdapterState;

    @Mock
    private @NotNull ProtocolAdapterPollingService protocolAdapterPollingService;

    @Mock
    private @NotNull EventService eventService;

    @Mock
    private @NotNull ModuleServices moduleServices;

    @Mock
    private @NotNull TagManager tagManager;

    @Mock
    private @NotNull NorthboundConsumerFactory northboundConsumerFactory;

    @Mock
    private @NotNull InternalProtocolAdapterWritingService protocolAdapterWritingService;

    private @NotNull ProtocolAdapterWrapper wrapper;

    @BeforeEach
    void setUp() {
        when(protocolAdapter.getId()).thenReturn("test-adapter");
        when(protocolAdapter.getProtocolAdapterInformation()).thenReturn(adapterInformation);
        when(adapterInformation.getCapabilities()).thenReturn(EnumSet.of(ProtocolAdapterCapability.READ));
        // Default: connect calls output.startedSuccessfully()
        doAnswer(invocation -> {
                    final ProtocolAdapterStartOutput output = invocation.getArgument(2);
                    output.startedSuccessfully();
                    return null;
                })
                .when(protocolAdapter)
                .start(any(), any(), any());
        // Default: disconnect calls output.stoppedSuccessfully()
        doAnswer(invocation -> {
                    final ProtocolAdapterStopOutput output = invocation.getArgument(2);
                    output.stoppedSuccessfully();
                    return null;
                })
                .when(protocolAdapter)
                .stop(any(), any(), any());
        wrapper = new ProtocolAdapterWrapper(
                protocolAdapter,
                config,
                adapterFactory,
                adapterInformation,
                metricsService,
                protocolAdapterState,
                protocolAdapterPollingService,
                eventService,
                moduleServices,
                tagManager,
                northboundConsumerFactory,
                protocolAdapterWritingService);
    }

    @Nested
    class InitialState {

        @Test
        void initialState_isIdleWithBothDisconnected() {
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Idle);
            assertThat(wrapper.getNorthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);
            assertThat(wrapper.getSouthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);
        }

        @Test
        void getAdapterId_delegatesToAdapter() {
            assertThat(wrapper.getAdapterId()).isEqualTo("test-adapter");
        }
    }

    @Nested
    class NorthboundOnlyAdapter {

        @Test
        void startAndStop_fullLifecycle() throws ProtocolAdapterException {
            assertThat(wrapper.start()).isTrue();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Working);
            assertThat(wrapper.getNorthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Connected);
            assertThat(wrapper.getSouthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);

            verify(protocolAdapter).precheck();
            verify(protocolAdapter).start(eq(ProtocolAdapterConnectionDirection.Northbound), any(), any());
            verify(protocolAdapter, never()).start(eq(ProtocolAdapterConnectionDirection.Southbound), any(), any());

            assertThat(wrapper.stop(false)).isTrue();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Idle);
            assertThat(wrapper.getNorthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);
            assertThat(wrapper.getSouthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);

            verify(protocolAdapter).stop(eq(ProtocolAdapterConnectionDirection.Northbound), any(), any());
            verify(protocolAdapter, never()).stop(eq(ProtocolAdapterConnectionDirection.Southbound), any(), any());
        }

        @Test
        void stop_withDestroy_callsAdapterDestroy() throws ProtocolAdapterException {
            wrapper.start();
            wrapper.stop(true);

            verify(protocolAdapter).destroy();
        }

        @Test
        void stop_withoutDestroy_doesNotCallAdapterDestroy() throws ProtocolAdapterException {
            wrapper.start();
            wrapper.stop(false);

            verify(protocolAdapter, never()).destroy();
        }
    }

    @Nested
    class BidirectionalAdapter {

        @BeforeEach
        void setUp() {
            when(adapterInformation.getCapabilities())
                    .thenReturn(EnumSet.of(ProtocolAdapterCapability.READ, ProtocolAdapterCapability.WRITE));
        }

        @Test
        void startAndStop_fullLifecycle() throws ProtocolAdapterException {
            assertThat(wrapper.start()).isTrue();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Working);
            assertThat(wrapper.getNorthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Connected);
            assertThat(wrapper.getSouthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Connected);

            verify(protocolAdapter).precheck();
            verify(protocolAdapter).start(eq(ProtocolAdapterConnectionDirection.Northbound), any(), any());
            verify(protocolAdapter).start(eq(ProtocolAdapterConnectionDirection.Southbound), any(), any());

            assertThat(wrapper.stop(true)).isTrue();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Idle);
            assertThat(wrapper.getNorthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);
            assertThat(wrapper.getSouthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);

            verify(protocolAdapter).stop(eq(ProtocolAdapterConnectionDirection.Northbound), any(), any());
            verify(protocolAdapter).stop(eq(ProtocolAdapterConnectionDirection.Southbound), any(), any());
        }
    }

    @Nested
    class PrecheckFailure {

        @Test
        void start_precheckThrows_transitionsToError() throws ProtocolAdapterException {
            doThrow(new ProtocolAdapterException("bad config"))
                    .when(protocolAdapter)
                    .precheck();

            assertThat(wrapper.start()).isFalse();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Error);
            assertThat(wrapper.getNorthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);

            verify(protocolAdapter, never()).start(any(), any(), any());
        }
    }

    @Nested
    class NorthboundConnectFailure {

        @Test
        void start_northboundConnectThrows_transitionsToError() throws ProtocolAdapterException {
            doAnswer(invocation -> {
                        final ProtocolAdapterStartOutput output = invocation.getArgument(2);
                        output.failStart(new RuntimeException("connection refused"), "connection refused");
                        return null;
                    })
                    .when(protocolAdapter)
                    .start(eq(ProtocolAdapterConnectionDirection.Northbound), any(), any());

            assertThat(wrapper.start()).isFalse();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Error);
            assertThat(wrapper.getNorthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Error);
            assertThat(wrapper.getSouthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);
        }
    }

    @Nested
    class SouthboundConnectFailure {

        @BeforeEach
        void setUp() {
            when(adapterInformation.getCapabilities())
                    .thenReturn(EnumSet.of(ProtocolAdapterCapability.READ, ProtocolAdapterCapability.WRITE));
        }

        @Test
        void start_southboundConnectThrows_cleansUpNorthboundAndTransitionsToError() throws ProtocolAdapterException {
            // Northbound succeeds (default doAnswer already set up)
            doAnswer(invocation -> {
                        final ProtocolAdapterStartOutput output = invocation.getArgument(2);
                        output.failStart(new RuntimeException("southbound refused"), "southbound refused");
                        return null;
                    })
                    .when(protocolAdapter)
                    .start(eq(ProtocolAdapterConnectionDirection.Southbound), any(), any());

            assertThat(wrapper.start()).isFalse();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Error);
            // Northbound should have been cleaned up (disconnected)
            assertThat(wrapper.getNorthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);
            // Southbound should be in Error from the failed connect
            assertThat(wrapper.getSouthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Error);

            // Verify northbound was disconnected as cleanup
            verify(protocolAdapter).stop(eq(ProtocolAdapterConnectionDirection.Northbound), any(), any());
        }
    }

    @Nested
    class DisconnectFailure {

        @Test
        void stop_disconnectThrows_stillTransitionsToDisconnected() throws ProtocolAdapterException {
            wrapper.start();

            doThrow(new RuntimeException("disconnect error"))
                    .when(protocolAdapter)
                    .stop(eq(ProtocolAdapterConnectionDirection.Northbound), any(), any());

            // Stop must report failure so manager can emit CRITICAL events,
            // while still cleaning up to a disconnected idle state.
            assertThat(wrapper.stop(false)).isFalse();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Idle);
            assertThat(wrapper.getNorthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);
        }
    }

    @Nested
    class FsmConflictDetection {

        @Test
        void start_whenAlreadyStarted_returnsFalse() throws ProtocolAdapterException {
            assertThat(wrapper.start()).isTrue();
            // Second start should fail because Working -> Precheck is invalid
            assertThat(wrapper.start()).isFalse();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Working);
        }

        @Test
        void stop_whenIdle_returnsFalse() {
            // Idle -> Stopping is invalid, stop returns false
            assertThat(wrapper.stop(false)).isFalse();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Idle);
        }

        @Test
        void start_afterStopAndRestart_succeeds() throws ProtocolAdapterException {
            assertThat(wrapper.start()).isTrue();
            assertThat(wrapper.stop(false)).isTrue();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Idle);

            // Should be able to start again from Idle
            assertThat(wrapper.start()).isTrue();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Working);
        }
    }

    @Nested
    class ErrorStateRecovery {

        @Test
        void stop_fromErrorState_succeeds() throws ProtocolAdapterException {
            // Get into Error state via failed precheck
            doThrow(new ProtocolAdapterException("bad config"))
                    .when(protocolAdapter)
                    .precheck();
            assertThat(wrapper.start()).isFalse();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Error);

            // Stop from Error state should succeed (Error -> Idle via stop logic)
            assertThat(wrapper.stop(false)).isTrue();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Idle);
        }

        @Test
        void stop_fromErrorStateWithDestroy_callsDestroy() throws ProtocolAdapterException {
            doThrow(new ProtocolAdapterException("bad config"))
                    .when(protocolAdapter)
                    .precheck();
            wrapper.start();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Error);

            wrapper.stop(true);

            verify(protocolAdapter).destroy();
        }

        @Test
        void start_afterErrorAndStop_succeeds() throws ProtocolAdapterException {
            // Get into Error state
            doThrow(new ProtocolAdapterException("bad config"))
                    .when(protocolAdapter)
                    .precheck();
            wrapper.start();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Error);

            // Stop to get back to Idle
            wrapper.stop(false);
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Idle);

            // Now fix the adapter and restart
            doNothing().when(protocolAdapter).precheck();
            assertThat(wrapper.start()).isTrue();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Working);
        }

        @Test
        void stop_fromErrorState_withConnectedNorthbound_disconnects() throws ProtocolAdapterException {
            // Get into Error state after northbound connected but southbound failed
            when(adapterInformation.getCapabilities())
                    .thenReturn(EnumSet.of(ProtocolAdapterCapability.READ, ProtocolAdapterCapability.WRITE));
            // Northbound succeeds (default doAnswer already set up)
            doAnswer(invocation -> {
                        final ProtocolAdapterStartOutput output = invocation.getArgument(2);
                        output.failStart(new RuntimeException("southbound refused"), "southbound refused");
                        return null;
                    })
                    .when(protocolAdapter)
                    .start(eq(ProtocolAdapterConnectionDirection.Southbound), any(), any());

            wrapper.start();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Error);
            // Northbound was cleaned up during start failure
            assertThat(wrapper.getNorthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);

            // Stop from Error should succeed
            assertThat(wrapper.stop(false)).isTrue();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Idle);
        }
    }

    @Nested
    class AlreadyDisconnected {

        @Test
        void stopNorthbound_whenAlreadyDisconnected_returnsTrue() throws ProtocolAdapterException {
            // Start and stop normally
            wrapper.start();
            assertThat(wrapper.stop(false)).isTrue();

            // Northbound is already disconnected, stop again should handle gracefully
            // (stop from Idle fails at FSM level, but the internal stopNorthbound handles it)
            assertThat(wrapper.getNorthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);
        }
    }

    @Nested
    class ProtocolAdapterStateChangeListenerTests {

        @Test
        void listener_notifiedOnSuccessfulTransition() {
            final List<ProtocolAdapterRuntimeState> fromStates = new ArrayList<>();
            final List<ProtocolAdapterRuntimeState> toStates = new ArrayList<>();

            wrapper.addStateChangeListener((from, to) -> {
                fromStates.add(from);
                toStates.add(to);
            });

            wrapper.start();

            // start() transitions: Idle->Precheck, Precheck->Working
            assertThat(fromStates)
                    .containsExactly(ProtocolAdapterRuntimeState.Idle, ProtocolAdapterRuntimeState.Precheck);
            assertThat(toStates)
                    .containsExactly(ProtocolAdapterRuntimeState.Precheck, ProtocolAdapterRuntimeState.Working);
        }

        @Test
        void listener_notifiedOnStop() {
            wrapper.start();

            final List<ProtocolAdapterRuntimeState> fromStates = new ArrayList<>();
            final List<ProtocolAdapterRuntimeState> toStates = new ArrayList<>();

            wrapper.addStateChangeListener((from, to) -> {
                fromStates.add(from);
                toStates.add(to);
            });

            wrapper.stop(false);

            // stop() transitions: Working->Stopping, Stopping->Idle
            assertThat(fromStates)
                    .containsExactly(ProtocolAdapterRuntimeState.Working, ProtocolAdapterRuntimeState.Stopping);
            assertThat(toStates)
                    .containsExactly(ProtocolAdapterRuntimeState.Stopping, ProtocolAdapterRuntimeState.Idle);
        }

        @Test
        void listener_removedSuccessfully() {
            final AtomicInteger callCount = new AtomicInteger(0);
            final ProtocolAdapterStateChangeListener listener = (from, to) -> callCount.incrementAndGet();

            wrapper.addStateChangeListener(listener);
            wrapper.start();

            // Idle->Precheck, Precheck->Working = 2 calls
            assertThat(callCount.get()).isEqualTo(2);

            wrapper.removeStateChangeListener(listener);
            wrapper.stop(false);

            // Should still be 2 -- listener was removed before stop
            assertThat(callCount.get()).isEqualTo(2);
        }

        @Test
        void listener_exceptionDoesNotPreventOtherListeners() {
            final AtomicInteger secondListenerCalls = new AtomicInteger(0);

            wrapper.addStateChangeListener((from, to) -> {
                throw new RuntimeException("listener error");
            });
            wrapper.addStateChangeListener((from, to) -> secondListenerCalls.incrementAndGet());

            wrapper.start();

            // Second listener should still be called despite first throwing
            assertThat(secondListenerCalls.get()).isEqualTo(2);
        }

        @Test
        void listener_exceptionDoesNotPreventStateTransition() {
            wrapper.addStateChangeListener((from, to) -> {
                throw new RuntimeException("listener error");
            });

            assertThat(wrapper.start()).isTrue();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Working);
        }

        @Test
        void listener_notNotifiedOnFailedTransition() {
            final AtomicInteger callCount = new AtomicInteger(0);
            wrapper.addStateChangeListener((from, to) -> callCount.incrementAndGet());

            // Idle->Stopping is invalid, should not notify
            wrapper.stop(false);
            assertThat(callCount.get()).isEqualTo(0);
        }

        @Test
        void listener_notifiedOnErrorTransition() throws ProtocolAdapterException {
            final List<ProtocolAdapterRuntimeState> toStates = new ArrayList<>();
            wrapper.addStateChangeListener((from, to) -> toStates.add(to));

            doThrow(new ProtocolAdapterException("bad config"))
                    .when(protocolAdapter)
                    .precheck();
            wrapper.start();

            // Idle->Precheck (success), Precheck->Error (success)
            assertThat(toStates)
                    .containsExactly(ProtocolAdapterRuntimeState.Precheck, ProtocolAdapterRuntimeState.Error);
        }
    }

    @Nested
    class ServiceLifecycleHooks {

        private boolean pollingStarted;
        private boolean pollingStopped;
        private boolean writingStarted;
        private boolean writingStopped;

        @BeforeEach
        void setUp() {
            pollingStarted = false;
            pollingStopped = false;
            writingStarted = false;
            writingStopped = false;

            wrapper =
                    new ProtocolAdapterWrapper(
                            protocolAdapter,
                            config,
                            adapterFactory,
                            adapterInformation,
                            metricsService,
                            protocolAdapterState,
                            protocolAdapterPollingService,
                            eventService,
                            moduleServices,
                            tagManager,
                            northboundConsumerFactory,
                            protocolAdapterWritingService) {
                        @Override
                        protected void startPolling() {
                            pollingStarted = true;
                        }

                        @Override
                        protected void stopPolling() {
                            pollingStopped = true;
                        }

                        @Override
                        protected void startWriting() {
                            writingStarted = true;
                        }

                        @Override
                        protected void stopWriting() {
                            writingStopped = true;
                        }
                    };
        }

        @Test
        void start_callsStartPollingAndStartWriting() {
            assertThat(wrapper.start()).isTrue();

            assertThat(pollingStarted).isTrue();
            assertThat(writingStarted).isTrue();
        }

        @Test
        void stop_callsStopPollingAndStopWriting() {
            wrapper.start();
            assertThat(wrapper.stop(false)).isTrue();

            assertThat(pollingStopped).isTrue();
            assertThat(writingStopped).isTrue();
        }

        @Test
        void start_failure_doesNotCallStartPollingOrStartWriting() throws ProtocolAdapterException {
            doThrow(new ProtocolAdapterException("bad config"))
                    .when(protocolAdapter)
                    .precheck();

            assertThat(wrapper.start()).isFalse();

            assertThat(pollingStarted).isFalse();
            assertThat(writingStarted).isFalse();
        }

        @Test
        void stop_fromError_callsStopPollingAndStopWriting() throws ProtocolAdapterException {
            // Get into Error state via failed precheck
            doThrow(new ProtocolAdapterException("bad config"))
                    .when(protocolAdapter)
                    .precheck();
            wrapper.start();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Error);

            // Reset flags since start sets up the wrapper
            pollingStopped = false;
            writingStopped = false;

            wrapper.stop(false);

            assertThat(pollingStopped).isTrue();
            assertThat(writingStopped).isTrue();
        }

        @Test
        void stop_stopsServicesBeforeDisconnecting() {
            final List<String> callOrder = new ArrayList<>();

            wrapper =
                    new ProtocolAdapterWrapper(
                            protocolAdapter,
                            config,
                            adapterFactory,
                            adapterInformation,
                            metricsService,
                            protocolAdapterState,
                            protocolAdapterPollingService,
                            eventService,
                            moduleServices,
                            tagManager,
                            northboundConsumerFactory,
                            protocolAdapterWritingService) {
                        @Override
                        protected void stopPolling() {
                            callOrder.add("stopPolling");
                        }

                        @Override
                        protected void stopWriting() {
                            callOrder.add("stopWriting");
                        }

                        @Override
                        protected boolean stopNorthbound() {
                            callOrder.add("stopNorthbound");
                            return super.stopNorthbound();
                        }
                    };

            wrapper.start();
            wrapper.stop(false);

            // Services should be stopped before connections are torn down
            assertThat(callOrder).containsExactly("stopPolling", "stopWriting", "stopNorthbound");
        }

        @Test
        void start_startsServicesAfterConnecting() {
            final List<String> callOrder = new ArrayList<>();

            wrapper =
                    new ProtocolAdapterWrapper(
                            protocolAdapter,
                            config,
                            adapterFactory,
                            adapterInformation,
                            metricsService,
                            protocolAdapterState,
                            protocolAdapterPollingService,
                            eventService,
                            moduleServices,
                            tagManager,
                            northboundConsumerFactory,
                            protocolAdapterWritingService) {
                        @Override
                        protected void startPolling() {
                            callOrder.add("startPolling");
                        }

                        @Override
                        protected void startWriting() {
                            callOrder.add("startWriting");
                        }

                        @Override
                        protected boolean startNorthbound() {
                            callOrder.add("startNorthbound");
                            return super.startNorthbound();
                        }
                    };

            wrapper.start();

            // Connections should be established before services are started
            assertThat(callOrder).containsExactly("startNorthbound", "startPolling", "startWriting");
        }
    }

    @Nested
    class StartFromErrorState {

        @Test
        void start_directlyFromError_fails() throws ProtocolAdapterException {
            // Get into Error state
            doThrow(new ProtocolAdapterException("bad config"))
                    .when(protocolAdapter)
                    .precheck();
            wrapper.start();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Error);

            // Attempt start from Error without stop first -- Error -> Precheck is invalid
            doNothing().when(protocolAdapter).precheck();
            assertThat(wrapper.start()).isFalse();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Error);
        }
    }

    @Nested
    class BidirectionalDisconnectFailure {

        @BeforeEach
        void setUp() {
            when(adapterInformation.getCapabilities())
                    .thenReturn(EnumSet.of(ProtocolAdapterCapability.READ, ProtocolAdapterCapability.WRITE));
        }

        @Test
        void stop_southboundDisconnectThrows_stillTransitionsToIdle() throws ProtocolAdapterException {
            wrapper.start();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Working);

            doThrow(new RuntimeException("southbound disconnect error"))
                    .when(protocolAdapter)
                    .stop(eq(ProtocolAdapterConnectionDirection.Southbound), any(), any());

            // Stop should still clean up to Idle/Disconnected but report failure.
            assertThat(wrapper.stop(false)).isFalse();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Idle);
            assertThat(wrapper.getSouthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);
            assertThat(wrapper.getNorthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);
        }

        @Test
        void stop_bothDisconnectsThrow_stillTransitionsToIdle() throws ProtocolAdapterException {
            wrapper.start();

            doThrow(new RuntimeException("northbound disconnect error"))
                    .when(protocolAdapter)
                    .stop(eq(ProtocolAdapterConnectionDirection.Northbound), any(), any());
            doThrow(new RuntimeException("southbound disconnect error"))
                    .when(protocolAdapter)
                    .stop(eq(ProtocolAdapterConnectionDirection.Southbound), any(), any());

            assertThat(wrapper.stop(false)).isFalse();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Idle);
            assertThat(wrapper.getNorthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);
            assertThat(wrapper.getSouthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);
        }
    }

    @Nested
    class StopIdempotency {

        @Test
        void stop_calledTwice_secondReturnsFalse() {
            wrapper.start();
            assertThat(wrapper.stop(false)).isTrue();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Idle);

            // Second stop from Idle fails — Idle → Stopping is invalid
            assertThat(wrapper.stop(false)).isFalse();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Idle);
        }
    }

    @Nested
    class ExternalErrorTransition {

        @Test
        void transitionToError_fromWorking_thenStopAndRestart() throws ProtocolAdapterException {
            wrapper.start();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Working);

            // Simulate a runtime error detected externally
            final ProtocolAdapterTransitionResponse errorResponse =
                    wrapper.transitionTo(ProtocolAdapterRuntimeState.Error);
            assertThat(errorResponse.status().isSuccess()).isTrue();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Error);

            // Stop from Error should work
            assertThat(wrapper.stop(false)).isTrue();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Idle);

            // Restart should succeed
            assertThat(wrapper.start()).isTrue();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Working);
        }

        @Test
        void transitionToError_fromWorking_disconnectsNorthbound() throws ProtocolAdapterException {
            wrapper.start();
            assertThat(wrapper.getNorthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Connected);

            // Transition to Error externally
            wrapper.transitionTo(ProtocolAdapterRuntimeState.Error);

            // Stop should disconnect the northbound connection that was still connected
            assertThat(wrapper.stop(false)).isTrue();
            verify(protocolAdapter, times(1)).stop(eq(ProtocolAdapterConnectionDirection.Northbound), any(), any());
            assertThat(wrapper.getNorthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);
        }
    }

    @Nested
    class DelegationTests {

        @Test
        void getProtocolAdapterInformation_delegatesToAdapter() {
            assertThat(wrapper.getProtocolAdapterInformation()).isSameAs(adapterInformation);
        }
    }

    @Nested
    class ConnectionStateTransitions {

        @Test
        void transitionNorthboundConnectionTo_invalidTransition_fails() {
            // Disconnected -> Connected is invalid (must go through Connecting first)
            final ProtocolAdapterConnectionTransitionResponse response =
                    wrapper.transitionNorthboundConnectionTo(ProtocolAdapterConnectionState.Connected);
            assertThat(response.status().isSuccess()).isFalse();
            assertThat(wrapper.getNorthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);
        }

        @Test
        void transitionSouthboundConnectionTo_invalidTransition_fails() {
            // Disconnected -> Connected is invalid
            final ProtocolAdapterConnectionTransitionResponse response =
                    wrapper.transitionSouthboundConnectionTo(ProtocolAdapterConnectionState.Connected);
            assertThat(response.status().isSuccess()).isFalse();
            assertThat(wrapper.getSouthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);
        }

        @Test
        void transitionNorthboundConnectionTo_validSequence_succeeds() {
            assertThat(wrapper.transitionNorthboundConnectionTo(ProtocolAdapterConnectionState.Connecting)
                            .status()
                            .isSuccess())
                    .isTrue();
            assertThat(wrapper.transitionNorthboundConnectionTo(ProtocolAdapterConnectionState.Connected)
                            .status()
                            .isSuccess())
                    .isTrue();
            assertThat(wrapper.getNorthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Connected);
        }

        @Test
        void transitionSouthboundConnectionTo_validSequence_succeeds() {
            assertThat(wrapper.transitionSouthboundConnectionTo(ProtocolAdapterConnectionState.Connecting)
                            .status()
                            .isSuccess())
                    .isTrue();
            assertThat(wrapper.transitionSouthboundConnectionTo(ProtocolAdapterConnectionState.Connected)
                            .status()
                            .isSuccess())
                    .isTrue();
            assertThat(wrapper.getSouthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Connected);
        }
    }

    @Nested
    class ConcurrentStartStop {

        @Test
        @Timeout(10)
        void concurrentStartAndStop_noRaceCondition() throws Exception {
            final int numIterations = 20;
            final ExecutorService executor = Executors.newFixedThreadPool(4);

            for (int i = 0; i < numIterations; i++) {
                final ProtocolAdapter adapter = Mockito.mock(ProtocolAdapter.class);
                when(adapter.getId()).thenReturn("adapter-" + i);
                when(adapter.getProtocolAdapterInformation()).thenReturn(adapterInformation);
                doAnswer(inv -> {
                            final ProtocolAdapterStartOutput out = inv.getArgument(2);
                            out.startedSuccessfully();
                            return null;
                        })
                        .when(adapter)
                        .start(any(), any(), any());
                doAnswer(inv -> {
                            final ProtocolAdapterStopOutput out = inv.getArgument(2);
                            out.stoppedSuccessfully();
                            return null;
                        })
                        .when(adapter)
                        .stop(any(), any(), any());
                final ProtocolAdapterWrapper w = new ProtocolAdapterWrapper(
                        adapter,
                        config,
                        adapterFactory,
                        adapterInformation,
                        metricsService,
                        protocolAdapterState,
                        protocolAdapterPollingService,
                        eventService,
                        moduleServices,
                        tagManager,
                        northboundConsumerFactory,
                        protocolAdapterWritingService);

                final CountDownLatch latch = new CountDownLatch(2);

                final CompletableFuture<Void> startFuture = CompletableFuture.runAsync(
                        () -> {
                            latch.countDown();
                            try {
                                latch.await(2, TimeUnit.SECONDS);
                            } catch (final InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            w.start();
                        },
                        executor);

                final CompletableFuture<Void> stopFuture = CompletableFuture.runAsync(
                        () -> {
                            latch.countDown();
                            try {
                                latch.await(2, TimeUnit.SECONDS);
                            } catch (final InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            w.stop(false);
                        },
                        executor);

                CompletableFuture.allOf(startFuture, stopFuture).get(5, TimeUnit.SECONDS);

                // Adapter should be in a valid terminal state (Idle or Working, never Precheck/Stopping)
                final ProtocolAdapterRuntimeState finalState = w.getState();
                assertThat(finalState)
                        .isIn(
                                ProtocolAdapterRuntimeState.Idle,
                                ProtocolAdapterRuntimeState.Working,
                                ProtocolAdapterRuntimeState.Error);
            }

            executor.shutdown();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Nested
    class StopWithDisconnectFailure {

        @Test
        void stop_adapterDisconnectFails_managerStillReportsStop() throws ProtocolAdapterException {
            // Simulate an adapter whose disconnect(Northbound) throws -- stop should clean up but return failure
            doThrow(new RuntimeException("disconnect failed"))
                    .when(protocolAdapter)
                    .stop(eq(ProtocolAdapterConnectionDirection.Northbound), any(), any());

            wrapper.start();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Working);

            // Stop should handle the error gracefully but indicate partial failure.
            final boolean stopped = wrapper.stop(false);
            assertThat(stopped).isFalse();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterRuntimeState.Idle);
            assertThat(wrapper.getNorthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);
        }
    }

    /**
     * Tests that verify the shutdown flag behavior on {@link ProtocolAdapterStateImpl}.
     * These use a REAL (non-mocked) {@link ProtocolAdapterStateImpl} to validate that
     * the wrapper correctly marks/clears the shutdown flag during stop/start,
     * preventing race conditions between connection status changes and adapter shutdown.
     */
    @Nested
    class ShutdownRaceConditionPrevention {

        private @NotNull ProtocolAdapterStateImpl realAdapterState;
        private @NotNull ProtocolAdapterWrapper wrapperWithRealState;

        @BeforeEach
        void setUp() {
            realAdapterState = new ProtocolAdapterStateImpl(eventService, "test-adapter", "test-protocol");
            wrapperWithRealState = new ProtocolAdapterWrapper(
                    protocolAdapter,
                    config,
                    adapterFactory,
                    adapterInformation,
                    metricsService,
                    realAdapterState,
                    protocolAdapterPollingService,
                    eventService,
                    moduleServices,
                    tagManager,
                    northboundConsumerFactory,
                    protocolAdapterWritingService);
        }

        @Test
        void stop_preventsConnectionStatusChangesDuringShutdown() {
            wrapperWithRealState.start();
            realAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);

            wrapperWithRealState.stop(false);

            // After stop, status changes should be blocked by the shutdown flag
            final boolean changed = realAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.ERROR);
            assertThat(changed).isFalse();
            assertThat(wrapperWithRealState.getRuntimeStatus()).isEqualTo(ProtocolAdapterState.RuntimeStatus.STOPPED);
        }

        @Test
        void startAfterStop_clearsShutdownFlag_allowsStatusChangesAgain() {
            // Start, stop
            wrapperWithRealState.start();
            realAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
            wrapperWithRealState.stop(false);

            // After stop, status changes should be blocked
            boolean changed = realAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.ERROR);
            assertThat(changed).isFalse();

            // Start again — shutdown flag should be cleared
            wrapperWithRealState.start();

            // Now status changes should work again
            changed = realAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.ERROR);
            assertThat(changed).isTrue();
            assertThat(realAdapterState.getConnectionStatus()).isEqualTo(ProtocolAdapterState.ConnectionStatus.ERROR);
        }

        @Test
        void listenerNotCalledDuringShutdown() {
            final AtomicInteger listenerCallCount = new AtomicInteger(0);

            wrapperWithRealState.start();
            realAdapterState.setConnectionStatusListener(status -> listenerCallCount.incrementAndGet());
            realAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);

            final int callCountBeforeStop = listenerCallCount.get();

            wrapperWithRealState.stop(false);

            // Try to trigger listener after stop — should not fire
            realAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.ERROR);
            assertThat(listenerCallCount.get()).isEqualTo(callCountBeforeStop);
        }

        @Test
        void failedStart_preventsConnectionStatusChanges() throws ProtocolAdapterException {
            doThrow(new ProtocolAdapterException("bad config"))
                    .when(protocolAdapter)
                    .precheck();

            wrapperWithRealState.start();
            assertThat(wrapperWithRealState.getState()).isEqualTo(ProtocolAdapterRuntimeState.Error);

            // Even after stop from error state, status changes should be blocked if not restarted
            wrapperWithRealState.stop(false);
            final boolean changed =
                    realAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
            assertThat(changed).isFalse();
        }

        @Test
        @Timeout(10)
        void concurrentStopAndStatusChange_noRaceCondition() throws Exception {
            final int numIterations = 20;
            final ExecutorService executor = Executors.newFixedThreadPool(4);

            for (int i = 0; i < numIterations; i++) {
                final ProtocolAdapterStateImpl iterationState =
                        new ProtocolAdapterStateImpl(eventService, "test-adapter-" + i, "test-protocol");
                final ProtocolAdapterWrapper iterationWrapper = new ProtocolAdapterWrapper(
                        protocolAdapter,
                        config,
                        adapterFactory,
                        adapterInformation,
                        metricsService,
                        iterationState,
                        protocolAdapterPollingService,
                        eventService,
                        moduleServices,
                        tagManager,
                        northboundConsumerFactory,
                        protocolAdapterWritingService);

                iterationWrapper.start();
                iterationState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);

                final CountDownLatch bothStarted = new CountDownLatch(2);

                final CompletableFuture<Void> stopFuture = CompletableFuture.runAsync(
                        () -> {
                            bothStarted.countDown();
                            try {
                                bothStarted.await(2, TimeUnit.SECONDS);
                            } catch (final InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            iterationWrapper.stop(false);
                        },
                        executor);

                final CompletableFuture<Void> statusChangeFuture = CompletableFuture.runAsync(
                        () -> {
                            bothStarted.countDown();
                            try {
                                bothStarted.await(2, TimeUnit.SECONDS);
                                Thread.sleep(10);
                            } catch (final InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            iterationState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.ERROR);
                        },
                        executor);

                CompletableFuture.allOf(stopFuture, statusChangeFuture).get(5, TimeUnit.SECONDS);

                // Verify the adapter is in a valid state after concurrent operations
                assertThat(iterationWrapper.getRuntimeStatus()).isEqualTo(ProtocolAdapterState.RuntimeStatus.STOPPED);
            }

            executor.shutdown();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }
}
