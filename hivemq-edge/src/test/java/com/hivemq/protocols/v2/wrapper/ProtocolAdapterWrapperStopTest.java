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
package com.hivemq.protocols.v2.wrapper;

import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.CONNECTED;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.STOPPED;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_CONNECTED;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_CONNECTION_RETRY;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_STARTED;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_VERIFICATION;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Stopping from every waiting state and the named path to {@code STOPPED} (scenario S7 at unit
 * level). A goal change to "stopped" while still waiting for an acknowledgment records the intent and is acted on
 * when the acknowledgment lands. Also covers recovery out of {@code ERROR}: the goal cycle through
 * {@code STOPPED} re-arms a fresh start.
 */
class ProtocolAdapterWrapperStopTest {

    @Test
    void goalStoppedInWaitingForStarted_recordsIntent_thenStopsWhenStartedLands() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder().build();
        fixture.adapter.startReply = MockProtocolAdapter.Reply.DROP; // park in WAITING_FOR_STARTED
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.state()).isEqualTo(WAITING_FOR_STARTED);

        fixture.stopAdapter(); // intent recorded; cannot act while awaiting started()
        assertThat(fixture.state()).isEqualTo(WAITING_FOR_STARTED);

        fixture.output.started(); // started() now routes to stop(), not connect()
        fixture.drain();
        assertThat(fixture.state()).isEqualTo(STOPPED);
        assertThat(fixture.commands()).doesNotContain("connect");
    }

    @Test
    void goalStoppedInWaitingForConnected_disconnectsAndStops() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder().build();
        fixture.adapter.connectReply = MockProtocolAdapter.Reply.DROP;
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.state()).isEqualTo(WAITING_FOR_CONNECTED);

        fixture.stopAdapter();
        assertThat(fixture.state()).isEqualTo(STOPPED);
        assertThat(fixture.commands()).containsSubsequence("disconnect", "stop");
    }

    @Test
    void goalStoppedInWaitingForVerification_disconnectsAndStops() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder().build();
        fixture.adapter.verifyDrop = true;
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.state()).isEqualTo(WAITING_FOR_VERIFICATION);

        fixture.stopAdapter();
        assertThat(fixture.state()).isEqualTo(STOPPED);
        assertThat(fixture.commands()).containsSubsequence("disconnect", "stop");
    }

    @Test
    void goalStoppedWhileConnected_disconnectsAndStops() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder().build();
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.state()).isEqualTo(CONNECTED);

        fixture.stopAdapter();
        assertThat(fixture.state()).isEqualTo(STOPPED);
        assertThat(fixture.commands()).containsSubsequence("disconnect", "stop");
    }

    @Test
    void goalStoppedInConnectionRetry_cancelsBackoffAndStops() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder().build();
        fixture.adapter.connectReply = MockProtocolAdapter.Reply.FAIL_CONNECTION;
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.state()).isEqualTo(WAITING_FOR_CONNECTION_RETRY);

        fixture.stopAdapter();
        assertThat(fixture.state()).isEqualTo(STOPPED);

        // The backoff was canceled — advancing far past it never re-attempts a connect.
        fixture.advance(5000);
        assertThat(fixture.state()).isEqualTo(STOPPED);
    }

    @Test
    void recoveryFromError_goalCycleThroughStoppedReArmsAFreshStart() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder().build();
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        fixture.output.started(); // unexpected in CONNECTED → defensive reset → ERROR
        fixture.drain();
        assertThat(fixture.state()).isEqualTo(ProtocolAdapterWrapperState.ERROR);

        fixture.stopAdapter(); // goal → STOPPED: ERROR → stop() best-effort → STOPPED
        assertThat(fixture.state()).isEqualTo(STOPPED);

        fixture.activate(ProtocolAdapterDirection.NORTHBOUND); // fresh start
        assertThat(fixture.state()).isEqualTo(CONNECTED);
    }
}
