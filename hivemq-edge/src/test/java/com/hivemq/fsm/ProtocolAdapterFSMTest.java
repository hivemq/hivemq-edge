package com.hivemq.fsm;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

class ProtocolAdapterFSMTest {

    public static final ProtocolAdapterFSM.State STATE_FULLY_STOPPED =
            new ProtocolAdapterFSM.State(ProtocolAdapterFSM.AdapterState.STOPPED,
                    ProtocolAdapterFSM.StateEnum.DISCONNECTED,
                    ProtocolAdapterFSM.StateEnum.DISCONNECTED);

    public static final ProtocolAdapterFSM.State STATE_STARTED_NOT_CONNECTED = new ProtocolAdapterFSM.State(
            ProtocolAdapterFSM.AdapterState.STARTED,
            ProtocolAdapterFSM.StateEnum.DISCONNECTED,
            ProtocolAdapterFSM.StateEnum.DISCONNECTED);

    @Test
    public void test_initialValue() {
        final var protocolAdapterFSM = new ProtocolAdapterFSM("adapterId");
        assertThat(protocolAdapterFSM.currentState())
                .isEqualTo(STATE_FULLY_STOPPED);
    }

    @Test
    public void test_listenersReceiveUpdates() throws Exception{
        final var protocolAdapterFSM = new ProtocolAdapterFSM("adapterId");
        final var latchListener1 = new CountDownLatch(1);
        final var latchListener2 = new CountDownLatch(1);

        protocolAdapterFSM.registerStateTransitionListener(newState -> latchListener1.countDown());
        protocolAdapterFSM.registerStateTransitionListener(newState -> latchListener2.countDown());

        assertThat(protocolAdapterFSM.currentState())
                .isEqualTo(STATE_FULLY_STOPPED);

        protocolAdapterFSM.startAdapter();

        latchListener1.await();
        latchListener2.await();

        assertThat(latchListener1.getCount())
                .isEqualTo(0);

        assertThat(latchListener2.getCount())
                .isEqualTo(0);

        assertThat(protocolAdapterFSM.currentState())
                .isEqualTo(STATE_STARTED_NOT_CONNECTED);

    }

    @Test
    public void test_startThenStopAdapter() throws Exception{
        final var protocolAdapterFSM = new ProtocolAdapterFSM("adapterId");
        final var latchListener1 = new CountDownLatch(1);
        final var latchListener2 = new CountDownLatch(1);

        protocolAdapterFSM.registerStateTransitionListener(newState -> latchListener1.countDown());
        protocolAdapterFSM.registerStateTransitionListener(newState -> latchListener2.countDown());

        assertThat(protocolAdapterFSM.currentState())
                .isEqualTo(STATE_FULLY_STOPPED);

        protocolAdapterFSM.startAdapter();

        latchListener1.await();
        latchListener2.await();

        assertThat(latchListener1.getCount())
                .isEqualTo(0);

        assertThat(latchListener2.getCount())
                .isEqualTo(0);

        assertThat(protocolAdapterFSM.currentState())
                .isEqualTo(STATE_STARTED_NOT_CONNECTED);

    }

}
