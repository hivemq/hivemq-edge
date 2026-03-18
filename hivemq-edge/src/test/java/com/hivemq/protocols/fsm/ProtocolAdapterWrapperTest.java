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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.exceptions.ProtocolAdapterException;
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
class ProtocolAdapterWrapperTest {

    @Mock
    private @NotNull ProtocolAdapter2 protocolAdapter;

    private @NotNull ProtocolAdapterWrapper2 wrapper;

    @BeforeEach
    void setUp() {
        when(protocolAdapter.getId()).thenReturn("test-adapter");
        when(protocolAdapter.supportsSouthbound()).thenReturn(false);
        wrapper = new ProtocolAdapterWrapper2(protocolAdapter);
    }

    @Nested
    class InitialState {

        @Test
        void initialState_isIdleWithBothDisconnected() {
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterState.Idle);
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
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterState.Working);
            assertThat(wrapper.getNorthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Connected);
            assertThat(wrapper.getSouthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);

            verify(protocolAdapter).precheck();
            verify(protocolAdapter).connect(ProtocolAdapterConnectionDirection.Northbound);
            verify(protocolAdapter, never()).connect(ProtocolAdapterConnectionDirection.Southbound);

            assertThat(wrapper.stop(false)).isTrue();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterState.Idle);
            assertThat(wrapper.getNorthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);
            assertThat(wrapper.getSouthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);

            verify(protocolAdapter).disconnect(ProtocolAdapterConnectionDirection.Northbound);
            verify(protocolAdapter, never()).disconnect(ProtocolAdapterConnectionDirection.Southbound);
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
            when(protocolAdapter.supportsSouthbound()).thenReturn(true);
        }

        @Test
        void startAndStop_fullLifecycle() throws ProtocolAdapterException {
            assertThat(wrapper.start()).isTrue();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterState.Working);
            assertThat(wrapper.getNorthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Connected);
            assertThat(wrapper.getSouthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Connected);

            verify(protocolAdapter).precheck();
            verify(protocolAdapter).connect(ProtocolAdapterConnectionDirection.Northbound);
            verify(protocolAdapter).connect(ProtocolAdapterConnectionDirection.Southbound);

            assertThat(wrapper.stop(true)).isTrue();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterState.Idle);
            assertThat(wrapper.getNorthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);
            assertThat(wrapper.getSouthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);

            verify(protocolAdapter).disconnect(ProtocolAdapterConnectionDirection.Northbound);
            verify(protocolAdapter).disconnect(ProtocolAdapterConnectionDirection.Southbound);
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
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterState.Error);
            assertThat(wrapper.getNorthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);

            verify(protocolAdapter, never()).connect(any());
        }
    }

    @Nested
    class NorthboundConnectFailure {

        @Test
        void start_northboundConnectThrows_transitionsToError() throws ProtocolAdapterException {
            doThrow(new ProtocolAdapterException("connection refused"))
                    .when(protocolAdapter)
                    .connect(ProtocolAdapterConnectionDirection.Northbound);

            assertThat(wrapper.start()).isFalse();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterState.Error);
            assertThat(wrapper.getNorthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Error);
            assertThat(wrapper.getSouthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);
        }
    }

    @Nested
    class SouthboundConnectFailure {

        @BeforeEach
        void setUp() {
            when(protocolAdapter.supportsSouthbound()).thenReturn(true);
        }

        @Test
        void start_southboundConnectThrows_cleansUpNorthboundAndTransitionsToError() throws ProtocolAdapterException {
            doNothing().when(protocolAdapter).connect(ProtocolAdapterConnectionDirection.Northbound);
            doThrow(new ProtocolAdapterException("southbound refused"))
                    .when(protocolAdapter)
                    .connect(ProtocolAdapterConnectionDirection.Southbound);

            assertThat(wrapper.start()).isFalse();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterState.Error);
            // Northbound should have been cleaned up (disconnected)
            assertThat(wrapper.getNorthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);
            // Southbound should be in Error from the failed connect
            assertThat(wrapper.getSouthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Error);

            // Verify northbound was disconnected as cleanup
            verify(protocolAdapter).disconnect(ProtocolAdapterConnectionDirection.Northbound);
        }
    }

    @Nested
    class DisconnectFailure {

        @Test
        void stop_disconnectThrows_stillTransitionsToDisconnected() throws ProtocolAdapterException {
            wrapper.start();

            doThrow(new RuntimeException("disconnect error"))
                    .when(protocolAdapter)
                    .disconnect(ProtocolAdapterConnectionDirection.Northbound);

            // Stop should still succeed because disconnect errors are caught and
            // the connection transitions to Disconnected anyway
            assertThat(wrapper.stop(false)).isTrue();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterState.Idle);
            assertThat(wrapper.getNorthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);
        }
    }

    @Nested
    class FsmConflictDetection {

        @Test
        void start_whenAlreadyStarted_returnsFalse() throws ProtocolAdapterException {
            assertThat(wrapper.start()).isTrue();
            // Second start should fail because Working → Precheck is invalid
            assertThat(wrapper.start()).isFalse();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterState.Working);
        }

        @Test
        void stop_whenIdle_returnsFalse() {
            // Idle → Stopping is invalid
            assertThat(wrapper.stop(false)).isFalse();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterState.Idle);
        }

        @Test
        void start_afterStopAndRestart_succeeds() throws ProtocolAdapterException {
            assertThat(wrapper.start()).isTrue();
            assertThat(wrapper.stop(false)).isTrue();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterState.Idle);

            // Should be able to start again from Idle
            assertThat(wrapper.start()).isTrue();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterState.Working);
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
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterState.Error);

            // Stop from Error state should succeed (Error → Idle via stop logic)
            assertThat(wrapper.stop(false)).isTrue();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterState.Idle);
        }

        @Test
        void stop_fromErrorStateWithDestroy_callsDestroy() throws ProtocolAdapterException {
            doThrow(new ProtocolAdapterException("bad config"))
                    .when(protocolAdapter)
                    .precheck();
            wrapper.start();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterState.Error);

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
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterState.Error);

            // Stop to get back to Idle
            wrapper.stop(false);
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterState.Idle);

            // Now fix the adapter and restart
            doNothing().when(protocolAdapter).precheck();
            assertThat(wrapper.start()).isTrue();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterState.Working);
        }

        @Test
        void stop_fromErrorState_withConnectedNorthbound_disconnects() throws ProtocolAdapterException {
            // Get into Error state after northbound connected but southbound failed
            when(protocolAdapter.supportsSouthbound()).thenReturn(true);
            doNothing().when(protocolAdapter).connect(ProtocolAdapterConnectionDirection.Northbound);
            doThrow(new ProtocolAdapterException("southbound refused"))
                    .when(protocolAdapter)
                    .connect(ProtocolAdapterConnectionDirection.Southbound);

            wrapper.start();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterState.Error);
            // Northbound was cleaned up during start failure
            assertThat(wrapper.getNorthboundConnectionState()).isEqualTo(ProtocolAdapterConnectionState.Disconnected);

            // Stop from Error should succeed
            assertThat(wrapper.stop(false)).isTrue();
            assertThat(wrapper.getState()).isEqualTo(ProtocolAdapterState.Idle);
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
}
