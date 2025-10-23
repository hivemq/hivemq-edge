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
package com.hivemq.fsm;

import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.hivemq.fsm.ProtocolAdapterFSM.AdapterStateEnum;
import static com.hivemq.fsm.ProtocolAdapterFSM.State;
import static com.hivemq.fsm.ProtocolAdapterFSM.StateEnum;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

class ProtocolAdapterFSMTest {

    private static final @NotNull String ID = "adapterId";

    private @NotNull ProtocolAdapterFSM createBasicFSM() {
        return new ProtocolAdapterFSM(ID) {
            @Override
            public boolean onStarting() {
                return true;
            }

            @Override
            public void onStopping() {
                // no-op
            }

            @Override
            public boolean startSouthbound() {
                return true;
            }
        };
    }

    /**
     * Creates an FSM that transitions southbound to CONNECTING when northbound connects.
     */
    private @NotNull ProtocolAdapterFSM createFSMWithAutoSouthbound() {
        return new ProtocolAdapterFSM(ID) {
            @Override
            public boolean onStarting() {
                return true;
            }

            @Override
            public void onStopping() {
                // no-opL
            }

            @Override
            public boolean startSouthbound() {
                return transitionSouthboundState(StateEnum.CONNECTING);
            }
        };
    }

    private void assertState(
            final @NotNull ProtocolAdapterFSM fsm,
            final @NotNull AdapterStateEnum adapter,
            final @NotNull StateEnum north,
            final @NotNull StateEnum south) {
        assertThat(fsm.currentState()).isEqualTo(new State(adapter, north, south));
    }

    // ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
    //  A D A P T E R    L I F E C Y C L E
    // ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~

    @Test
    void adapter_startsInStoppedState() {
        final var fsm = createBasicFSM();
        assertState(fsm, AdapterStateEnum.STOPPED, StateEnum.DISCONNECTED, StateEnum.DISCONNECTED);
    }

    @Test
    void adapter_successfulStartup() {
        final var fsm = createBasicFSM();
        fsm.startAdapter();
        assertState(fsm, AdapterStateEnum.STARTED, StateEnum.DISCONNECTED, StateEnum.DISCONNECTED);
    }

    @Test
    void adapter_failedStartup_returnsToStopped() {
        final var fsm = new ProtocolAdapterFSM(ID) {
            @Override
            public boolean onStarting() {
                return false; // Simulate startup failure
            }

            @Override
            public void onStopping() {
            }

            @Override
            public boolean startSouthbound() {
                return true;
            }
        };

        fsm.startAdapter();
        assertState(fsm, AdapterStateEnum.STOPPED, StateEnum.DISCONNECTED, StateEnum.DISCONNECTED);
    }

    @Test
    void adapter_stopPreservesConnectionStates() {
        final var fsm = createBasicFSM();
        fsm.startAdapter();
        fsm.transitionNorthboundState(StateEnum.CONNECTED);

        fsm.stopAdapter();

        assertState(fsm, AdapterStateEnum.STOPPED, StateEnum.CONNECTED, StateEnum.DISCONNECTED);
    }

    // ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
    //  N O R T H B O U N D    C O N N E C T I O N
    // ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~

    @Test
    void northbound_legacyDirectConnect() {
        final var fsm = createBasicFSM();
        fsm.startAdapter();
        fsm.accept(ProtocolAdapterState.ConnectionStatus.CONNECTED);

        assertState(fsm, AdapterStateEnum.STARTED, StateEnum.CONNECTED, StateEnum.DISCONNECTED);
    }

    @Test
    void northbound_standardConnectFlow() {
        final var fsm = createBasicFSM();
        fsm.startAdapter();

        fsm.accept(ProtocolAdapterState.ConnectionStatus.CONNECTING);
        assertThat(fsm.currentState().northbound()).isEqualTo(StateEnum.CONNECTING);

        fsm.accept(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        assertThat(fsm.currentState().northbound()).isEqualTo(StateEnum.CONNECTED);
    }

    @Test
    void northbound_errorState() {
        final var fsm = createBasicFSM();
        fsm.startAdapter();
        fsm.accept(ProtocolAdapterState.ConnectionStatus.CONNECTING);
        fsm.accept(ProtocolAdapterState.ConnectionStatus.ERROR);

        assertState(fsm, AdapterStateEnum.STARTED, StateEnum.ERROR, StateEnum.DISCONNECTED);
    }

    @Test
    void northbound_disconnecting() {
        final var fsm = createBasicFSM();
        fsm.startAdapter();
        fsm.transitionNorthboundState(StateEnum.CONNECTED);

        assertThat(fsm.startDisconnecting()).isTrue();
        assertThat(fsm.currentState().northbound()).isEqualTo(StateEnum.DISCONNECTING);

        fsm.transitionNorthboundState(StateEnum.DISCONNECTED);
        assertThat(fsm.currentState().northbound()).isEqualTo(StateEnum.DISCONNECTED);
    }

    @Test
    void northbound_closingSequence() {
        final var fsm = createBasicFSM();
        fsm.startAdapter();
        fsm.transitionNorthboundState(StateEnum.CONNECTED);

        assertThat(fsm.startClosing()).isTrue();
        assertThat(fsm.currentState().northbound()).isEqualTo(StateEnum.CLOSING);

        assertThat(fsm.markAsClosed()).isTrue();
        assertThat(fsm.currentState().northbound()).isEqualTo(StateEnum.CLOSED);
    }

    @Test
    void northbound_errorClosingSequence() {
        final var fsm = createBasicFSM();
        fsm.startAdapter();
        fsm.transitionNorthboundState(StateEnum.CONNECTED);

        assertThat(fsm.startErrorClosing()).isTrue();
        assertThat(fsm.currentState().northbound()).isEqualTo(StateEnum.ERROR_CLOSING);

        assertThat(fsm.transitionNorthboundState(StateEnum.ERROR)).isTrue();
        assertThat(fsm.currentState().northbound()).isEqualTo(StateEnum.ERROR);
    }

    @Test
    void northbound_errorRecovery() {
        final var fsm = createBasicFSM();
        fsm.startAdapter();
        fsm.transitionNorthboundState(StateEnum.CONNECTING);
        fsm.transitionNorthboundState(StateEnum.ERROR);

        assertThat(fsm.recoverFromError()).isTrue();
        assertThat(fsm.currentState().northbound()).isEqualTo(StateEnum.CONNECTING);
    }

    @Test
    void northbound_closedRestart() {
        final var fsm = createBasicFSM();
        fsm.startAdapter();
        fsm.transitionNorthboundState(StateEnum.CLOSED);

        assertThat(fsm.restartFromClosed()).isTrue();
        assertThat(fsm.currentState().northbound()).isEqualTo(StateEnum.DISCONNECTED);
    }

    @Test
    void northbound_reconnectFromConnected() {
        final var fsm = createBasicFSM();
        fsm.startAdapter();
        fsm.transitionNorthboundState(StateEnum.CONNECTED);

        // Can transition back to CONNECTING for reconnection
        assertThat(fsm.transitionNorthboundState(StateEnum.CONNECTING)).isTrue();
        assertThat(fsm.currentState().northbound()).isEqualTo(StateEnum.CONNECTING);
    }

    @Test
    void northbound_directDisconnectFromConnected() {
        final var fsm = createBasicFSM();
        fsm.startAdapter();
        fsm.transitionNorthboundState(StateEnum.CONNECTED);

        // Can transition directly to DISCONNECTED
        assertThat(fsm.transitionNorthboundState(StateEnum.DISCONNECTED)).isTrue();
        assertThat(fsm.currentState().northbound()).isEqualTo(StateEnum.DISCONNECTED);
    }

    @Test
    void northbound_abortConnecting() {
        final var fsm = createBasicFSM();
        fsm.startAdapter();
        fsm.transitionNorthboundState(StateEnum.CONNECTING);

        // Can abort connection attempt
        assertThat(fsm.transitionNorthboundState(StateEnum.DISCONNECTED)).isTrue();
        assertThat(fsm.currentState().northbound()).isEqualTo(StateEnum.DISCONNECTED);
    }

    @Test
    void northbound_connectingToError() {
        final var fsm = createBasicFSM();
        fsm.startAdapter();
        fsm.transitionNorthboundState(StateEnum.CONNECTING);

        assertThat(fsm.transitionNorthboundState(StateEnum.ERROR)).isTrue();
        assertThat(fsm.currentState().northbound()).isEqualTo(StateEnum.ERROR);
    }

    @Test
    void northbound_errorToDisconnected() {
        final var fsm = createBasicFSM();
        fsm.startAdapter();
        fsm.transitionNorthboundState(StateEnum.CONNECTING);
        fsm.transitionNorthboundState(StateEnum.ERROR);

        // Can give up and go to DISCONNECTED
        assertThat(fsm.transitionNorthboundState(StateEnum.DISCONNECTED)).isTrue();
        assertThat(fsm.currentState().northbound()).isEqualTo(StateEnum.DISCONNECTED);
    }

    @Test
    void northbound_disconnectingToClosing() {
        final var fsm = createBasicFSM();
        fsm.startAdapter();
        fsm.transitionNorthboundState(StateEnum.CONNECTED);
        fsm.startDisconnecting();

        // Can escalate disconnect to permanent close
        assertThat(fsm.transitionNorthboundState(StateEnum.CLOSING)).isTrue();
        assertThat(fsm.currentState().northbound()).isEqualTo(StateEnum.CLOSING);
    }

    // ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
    // S O U T H B O U N D    C O N N E C T I O N
    // ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~

    @Test
    void southbound_startsWhenNorthboundConnects() {
        final var fsm = createFSMWithAutoSouthbound();
        fsm.startAdapter();
        fsm.accept(ProtocolAdapterState.ConnectionStatus.CONNECTED);

        assertThat(fsm.currentState().southbound()).isEqualTo(StateEnum.CONNECTING);
    }

    @Test
    void southbound_errorWhileNorthboundConnected() {
        final var fsm = createFSMWithAutoSouthbound();
        fsm.startAdapter();
        fsm.accept(ProtocolAdapterState.ConnectionStatus.CONNECTED);

        // Simulate async error
        CompletableFuture.runAsync(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            fsm.transitionSouthboundState(StateEnum.ERROR);
        });

        await().atMost(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertState(fsm, AdapterStateEnum.STARTED, StateEnum.CONNECTED, StateEnum.ERROR));
    }

    @Test
    void southbound_fullLifecycle() {
        final var fsm = createFSMWithAutoSouthbound();
        fsm.startAdapter();
        fsm.accept(ProtocolAdapterState.ConnectionStatus.CONNECTED);

        // CONNECTING → CONNECTED
        assertThat(fsm.transitionSouthboundState(StateEnum.CONNECTED)).isTrue();

        // CONNECTED → CLOSING → CLOSED
        assertThat(fsm.startSouthboundClosing()).isTrue();
        assertThat(fsm.markSouthboundAsClosed()).isTrue();
        assertThat(fsm.currentState().southbound()).isEqualTo(StateEnum.CLOSED);
    }

    @Test
    void southbound_errorRecovery() {
        final var fsm = createBasicFSM();
        fsm.startAdapter();
        fsm.transitionSouthboundState(StateEnum.CONNECTING);
        fsm.transitionSouthboundState(StateEnum.ERROR);

        assertThat(fsm.recoverSouthboundFromError()).isTrue();
        assertThat(fsm.currentState().southbound()).isEqualTo(StateEnum.CONNECTING);
    }

    @Test
    void southbound_closedRestart() {
        final var fsm = createBasicFSM();
        fsm.startAdapter();
        fsm.transitionSouthboundState(StateEnum.CLOSED);

        assertThat(fsm.restartSouthboundFromClosed()).isTrue();
        assertThat(fsm.currentState().southbound()).isEqualTo(StateEnum.DISCONNECTED);
    }

    @Test
    void southbound_errorClosingSequence() {
        final var fsm = createBasicFSM();
        fsm.startAdapter();
        fsm.transitionSouthboundState(StateEnum.CONNECTED);

        assertThat(fsm.startSouthboundErrorClosing()).isTrue();
        assertThat(fsm.currentState().southbound()).isEqualTo(StateEnum.ERROR_CLOSING);

        assertThat(fsm.transitionSouthboundState(StateEnum.ERROR)).isTrue();
        assertThat(fsm.currentState().southbound()).isEqualTo(StateEnum.ERROR);
    }

    @Test
    void southbound_disconnecting() {
        final var fsm = createBasicFSM();
        fsm.startAdapter();
        fsm.transitionSouthboundState(StateEnum.CONNECTED);

        assertThat(fsm.startSouthboundDisconnecting()).isTrue();
        assertThat(fsm.currentState().southbound()).isEqualTo(StateEnum.DISCONNECTING);

        assertThat(fsm.transitionSouthboundState(StateEnum.DISCONNECTED)).isTrue();
        assertThat(fsm.currentState().southbound()).isEqualTo(StateEnum.DISCONNECTED);
    }

    // ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
    // N O T    V A L I D    T R A N S I T I O N S
    // ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~

    @Test
    void invalidTransition_disconnectedToClosing() {
        final var fsm = createBasicFSM();
        fsm.startAdapter();

        assertThatThrownBy(() -> fsm.transitionNorthboundState(StateEnum.CLOSING)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition northbound state to CLOSING");
    }

    @Test
    void invalidTransition_connectingToErrorClosing() {
        final var fsm = createBasicFSM();
        fsm.startAdapter();
        fsm.transitionNorthboundState(StateEnum.CONNECTING);

        assertThatThrownBy(() -> fsm.transitionNorthboundState(StateEnum.ERROR_CLOSING)).isInstanceOf(
                        IllegalStateException.class)
                .hasMessageContaining("Cannot transition northbound state to ERROR_CLOSING");
    }

    @Test
    void invalidTransition_southboundDisconnectedToClosing() {
        final var fsm = createBasicFSM();
        fsm.startAdapter();

        assertThatThrownBy(() -> fsm.transitionSouthboundState(StateEnum.CLOSING)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition southbound state to CLOSING");
    }

    // ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
    // S T A T E    L I S T E N E R
    // ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~

    @Test
    void stateListener_notifiedOnTransition() {
        final var fsm = createBasicFSM();
        final var capturedState = new AtomicReference<State>();

        fsm.registerStateTransitionListener(capturedState::set);
        fsm.startAdapter();

        assertThat(capturedState.get()).isNotNull();
        assertThat(capturedState.get().adapter()).isEqualTo(AdapterStateEnum.STARTED);
    }

    @Test
    void stateListener_multipleNotifications() {
        final var fsm = createBasicFSM();
        final var stateCount = new java.util.concurrent.atomic.AtomicInteger(0);

        fsm.registerStateTransitionListener(state -> stateCount.incrementAndGet());

        fsm.startAdapter(); // Triggers 2 transitions: STOPPED→STARTING, STARTING→STARTED
        fsm.transitionNorthboundState(StateEnum.CONNECTING);
        fsm.transitionNorthboundState(StateEnum.CONNECTED);

        assertThat(stateCount.get()).isEqualTo(4); // STOPPED→STARTING, STARTING→STARTED, DISCONNECTED→CONNECTING, CONNECTING→CONNECTED
    }

    @Test
    void stateListener_unregister() {
        final var fsm = createBasicFSM();
        final var stateCount = new java.util.concurrent.atomic.AtomicInteger(0);
        final Consumer<State> listener = state -> stateCount.incrementAndGet();

        fsm.registerStateTransitionListener(listener);
        fsm.startAdapter(); // Should notify twice: STOPPED→STARTING, STARTING→STARTED

        fsm.unregisterStateTransitionListener(listener);
        fsm.transitionNorthboundState(StateEnum.CONNECTING); // Should NOT notify

        assertThat(stateCount.get()).isEqualTo(2); // Two startup notifications
    }

    @Test
    void concurrentTransition_casFailure() {
        final var fsm = createBasicFSM();
        fsm.startAdapter();
        fsm.transitionNorthboundState(StateEnum.CONNECTING);

        // First transition succeeds
        final var result1 = fsm.transitionNorthboundState(StateEnum.CONNECTED);
        assertThat(result1).isTrue();

        // Second transition from CONNECTED - demonstrates sequential transitions work
        // CONNECTED → CONNECTING is valid (reconnection scenario)
        final var result2 = fsm.transitionNorthboundState(StateEnum.CONNECTING);
        assertThat(result2).isTrue();
    }

    @Test
    void diagramSequence_idealShutdown() {
        final var fsm = createFSMWithAutoSouthbound();

        // Step 1: Both DISCONNECTED (initial state)
        assertState(fsm, AdapterStateEnum.STOPPED, StateEnum.DISCONNECTED, StateEnum.DISCONNECTED);

        // Step 2: Start adapter, northbound CONNECTING
        fsm.startAdapter();
        fsm.transitionNorthboundState(StateEnum.CONNECTING);
        assertState(fsm, AdapterStateEnum.STARTED, StateEnum.CONNECTING, StateEnum.DISCONNECTED);

        // Step 3: Northbound CONNECTED (triggers southbound start)
        fsm.accept(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        assertState(fsm, AdapterStateEnum.STARTED, StateEnum.CONNECTED, StateEnum.CONNECTING);

        // Step 4: Southbound CONNECTING (already done by accept), transition to CONNECTED
        fsm.transitionSouthboundState(StateEnum.CONNECTED);
        assertState(fsm, AdapterStateEnum.STARTED, StateEnum.CONNECTED, StateEnum.CONNECTED);

        // Step 5: Southbound CLOSING
        fsm.startSouthboundClosing();
        assertState(fsm, AdapterStateEnum.STARTED, StateEnum.CONNECTED, StateEnum.CLOSING);

        // Step 6: Southbound CLOSED
        fsm.markSouthboundAsClosed();
        assertState(fsm, AdapterStateEnum.STARTED, StateEnum.CONNECTED, StateEnum.CLOSED);

        // Step 7: Northbound CLOSING
        fsm.startClosing();
        assertState(fsm, AdapterStateEnum.STARTED, StateEnum.CLOSING, StateEnum.CLOSED);

        // Step 8: Northbound CLOSED
        fsm.markAsClosed();
        assertState(fsm, AdapterStateEnum.STARTED, StateEnum.CLOSED, StateEnum.CLOSED);
    }
}
