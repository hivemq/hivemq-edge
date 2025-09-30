package com.hivemq.fsm;

import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class ProtocolAdapterFSMTest {

    private static final @NotNull String ID = "adapterId";

    @Test
    public void test_startAdapter_withLegacyConnectBehavior() throws Exception{
        final var protocolAdapterFSM = new ProtocolAdapterFSM(ID) {
            @Override
            public boolean onStarting() {
                return true;
            }

            @Override
            public void onStopping() {
                throw new IllegalStateException("Shouldn't be triggered");
            }

            @Override
            public boolean startSouthbound() {
                return true;
            }
        };

        assertThat(protocolAdapterFSM.currentState())
                .isEqualTo(new ProtocolAdapterFSM.State(
                        ProtocolAdapterFSM.AdapterStateEnum.STOPPED,
                        ProtocolAdapterFSM.StateEnum.DISCONNECTED,
                        ProtocolAdapterFSM.StateEnum.DISCONNECTED));

        protocolAdapterFSM.startAdapter();

        assertThat(protocolAdapterFSM.currentState())
                .isEqualTo(new ProtocolAdapterFSM.State(
                        ProtocolAdapterFSM.AdapterStateEnum.STARTED,
                        ProtocolAdapterFSM.StateEnum.DISCONNECTED,
                        ProtocolAdapterFSM.StateEnum.DISCONNECTED));

        // northbound is connected
        protocolAdapterFSM.accept(ProtocolAdapterState.ConnectionStatus.CONNECTED);

        assertThat(protocolAdapterFSM.currentState())
                .isEqualTo(new ProtocolAdapterFSM.State(
                        ProtocolAdapterFSM.AdapterStateEnum.STARTED,
                        ProtocolAdapterFSM.StateEnum.CONNECTED,
                        ProtocolAdapterFSM.StateEnum.DISCONNECTED));
    }

    @Test
    public void test_startAdapter_withGoingThroughConnectingState() throws Exception{
        final var protocolAdapterFSM = new ProtocolAdapterFSM(ID) {
            @Override
            public boolean onStarting() {
                return true;
            }

            @Override
            public void onStopping() {
                throw new IllegalStateException("Shouldn't be triggered");
            }

            @Override
            public boolean startSouthbound() {
                return true;
            }
        };

        assertThat(protocolAdapterFSM.currentState())
                .isEqualTo(new ProtocolAdapterFSM.State(
                        ProtocolAdapterFSM.AdapterStateEnum.STOPPED,
                        ProtocolAdapterFSM.StateEnum.DISCONNECTED,
                        ProtocolAdapterFSM.StateEnum.DISCONNECTED));

        protocolAdapterFSM.startAdapter();

        assertThat(protocolAdapterFSM.currentState())
                .isEqualTo(new ProtocolAdapterFSM.State(
                        ProtocolAdapterFSM.AdapterStateEnum.STARTED,
                        ProtocolAdapterFSM.StateEnum.DISCONNECTED,
                        ProtocolAdapterFSM.StateEnum.DISCONNECTED));

        protocolAdapterFSM.accept(ProtocolAdapterState.ConnectionStatus.CONNECTING);

        assertThat(protocolAdapterFSM.currentState())
                .isEqualTo(new ProtocolAdapterFSM.State(
                        ProtocolAdapterFSM.AdapterStateEnum.STARTED,
                        ProtocolAdapterFSM.StateEnum.CONNECTING,
                        ProtocolAdapterFSM.StateEnum.DISCONNECTED));

        protocolAdapterFSM.accept(ProtocolAdapterState.ConnectionStatus.CONNECTED);

        assertThat(protocolAdapterFSM.currentState())
                .isEqualTo(new ProtocolAdapterFSM.State(
                        ProtocolAdapterFSM.AdapterStateEnum.STARTED,
                        ProtocolAdapterFSM.StateEnum.CONNECTED,
                        ProtocolAdapterFSM.StateEnum.DISCONNECTED));
    }

    @Test
    public void test_startAdapter_northboundError() throws Exception{
        final var protocolAdapterFSM = new ProtocolAdapterFSM(ID) {
            @Override
            public boolean onStarting() {
                return true;
            }

            @Override
            public void onStopping() {
                throw new IllegalStateException("Shouldn't be triggered");
            }

            @Override
            public boolean startSouthbound() {
                return true;
            }
        };

        assertThat(protocolAdapterFSM.currentState())
                .isEqualTo(new ProtocolAdapterFSM.State(
                        ProtocolAdapterFSM.AdapterStateEnum.STOPPED,
                        ProtocolAdapterFSM.StateEnum.DISCONNECTED,
                        ProtocolAdapterFSM.StateEnum.DISCONNECTED));

        protocolAdapterFSM.startAdapter();

        assertThat(protocolAdapterFSM.currentState())
                .isEqualTo(new ProtocolAdapterFSM.State(
                        ProtocolAdapterFSM.AdapterStateEnum.STARTED,
                        ProtocolAdapterFSM.StateEnum.DISCONNECTED,
                        ProtocolAdapterFSM.StateEnum.DISCONNECTED));

        protocolAdapterFSM.accept(ProtocolAdapterState.ConnectionStatus.CONNECTING);

        assertThat(protocolAdapterFSM.currentState())
                .isEqualTo(new ProtocolAdapterFSM.State(
                        ProtocolAdapterFSM.AdapterStateEnum.STARTED,
                        ProtocolAdapterFSM.StateEnum.CONNECTING,
                        ProtocolAdapterFSM.StateEnum.DISCONNECTED));

        protocolAdapterFSM.accept(ProtocolAdapterState.ConnectionStatus.ERROR);

        assertThat(protocolAdapterFSM.currentState())
                .isEqualTo(new ProtocolAdapterFSM.State(
                        ProtocolAdapterFSM.AdapterStateEnum.STARTED,
                        ProtocolAdapterFSM.StateEnum.ERROR,
                        ProtocolAdapterFSM.StateEnum.DISCONNECTED));
    }

    @Test
    public void test_startAdapter_northbound_connected_southbound_error() throws Exception{

        final CountDownLatch latch = new CountDownLatch(1);
        final var protocolAdapterFSM = new ProtocolAdapterFSM(ID) {
            @Override
            public boolean onStarting() {
                return true;
            }

            @Override
            public void onStopping() {
            }

            @Override
            public boolean startSouthbound() {
                CompletableFuture.runAsync(() -> {
                    try {
                        TimeUnit.SECONDS.sleep(2);
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                    transitionSouthboundState(StateEnum.ERROR);
                });
                return transitionSouthboundState(StateEnum.CONNECTING);
            }
        };

        protocolAdapterFSM.startAdapter();
        protocolAdapterFSM.accept(ProtocolAdapterState.ConnectionStatus.CONNECTING);
        protocolAdapterFSM.accept(ProtocolAdapterState.ConnectionStatus.CONNECTED);

        assertThat(protocolAdapterFSM.currentState())
                .isEqualTo(new ProtocolAdapterFSM.State(
                        ProtocolAdapterFSM.AdapterStateEnum.STARTED,
                        ProtocolAdapterFSM.StateEnum.CONNECTED,
                        ProtocolAdapterFSM.StateEnum.CONNECTING));

        await().untilAsserted(() -> {
            assertThat(protocolAdapterFSM.currentState())
                    .isEqualTo(new ProtocolAdapterFSM.State(
                            ProtocolAdapterFSM.AdapterStateEnum.STARTED,
                            ProtocolAdapterFSM.StateEnum.CONNECTED,
                            ProtocolAdapterFSM.StateEnum.ERROR)); // this is a valid state for EDGE
        });
    }

    @Test
    public void test_startAndStopAdapter() throws Exception{
        final var protocolAdapterFSM = new ProtocolAdapterFSM(ID) {
            @Override
            public boolean onStarting() {
                return true;
            }

            @Override
            public void onStopping() {
            }

            @Override
            public boolean startSouthbound() {
                return true;
            }
        };

        assertThat(protocolAdapterFSM.currentState())
                .isEqualTo(new ProtocolAdapterFSM.State(
                        ProtocolAdapterFSM.AdapterStateEnum.STOPPED,
                        ProtocolAdapterFSM.StateEnum.DISCONNECTED,
                        ProtocolAdapterFSM.StateEnum.DISCONNECTED));

        protocolAdapterFSM.startAdapter();

        assertThat(protocolAdapterFSM.currentState())
                .isEqualTo(new ProtocolAdapterFSM.State(
                        ProtocolAdapterFSM.AdapterStateEnum.STARTED,
                        ProtocolAdapterFSM.StateEnum.DISCONNECTED,
                        ProtocolAdapterFSM.StateEnum.DISCONNECTED));

        protocolAdapterFSM.accept(ProtocolAdapterState.ConnectionStatus.CONNECTING);

        assertThat(protocolAdapterFSM.currentState())
                .isEqualTo(new ProtocolAdapterFSM.State(
                        ProtocolAdapterFSM.AdapterStateEnum.STARTED,
                        ProtocolAdapterFSM.StateEnum.CONNECTING,
                        ProtocolAdapterFSM.StateEnum.DISCONNECTED));

        protocolAdapterFSM.accept(ProtocolAdapterState.ConnectionStatus.CONNECTED);

        assertThat(protocolAdapterFSM.currentState())
                .isEqualTo(new ProtocolAdapterFSM.State(
                        ProtocolAdapterFSM.AdapterStateEnum.STARTED,
                        ProtocolAdapterFSM.StateEnum.CONNECTED,
                        ProtocolAdapterFSM.StateEnum.DISCONNECTED));

        protocolAdapterFSM.stopAdapter();

        assertThat(protocolAdapterFSM.currentState())
                .isEqualTo(new ProtocolAdapterFSM.State(
                        ProtocolAdapterFSM.AdapterStateEnum.STOPPED,
                        ProtocolAdapterFSM.StateEnum.CONNECTED,
                        ProtocolAdapterFSM.StateEnum.DISCONNECTED));
    }
}
