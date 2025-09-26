package com.hivemq.fsm;

import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class ProtocolAdapterFSMTest {

    @Test
    public void test_startAdapter_withLegacyConnectBehavior() throws Exception{
        final var protocolAdapterFSM = new ProtocolAdapterFSM("adapterId") {
            @Override
            public boolean onStarting() {
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

        protocolAdapterFSM.accept(ProtocolAdapterState.ConnectionStatus.CONNECTED);

        assertThat(protocolAdapterFSM.currentState())
                .isEqualTo(new ProtocolAdapterFSM.State(
                        ProtocolAdapterFSM.AdapterStateEnum.STARTED,
                        ProtocolAdapterFSM.StateEnum.CONNECTED,
                        ProtocolAdapterFSM.StateEnum.DISCONNECTED));
    }

    @Test
    public void test_startAdapter_withGoingThroughConnectingState() throws Exception{
        final var protocolAdapterFSM = new ProtocolAdapterFSM("adapterId") {
            @Override
            public boolean onStarting() {
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
        final var protocolAdapterFSM = new ProtocolAdapterFSM("adapterId") {
            @Override
            public boolean onStarting() {
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

}
