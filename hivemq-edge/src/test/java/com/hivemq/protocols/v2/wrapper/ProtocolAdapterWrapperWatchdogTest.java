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

import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.ERROR;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_CONNECTED;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_CONNECTION_RETRY;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_DISCONNECTED_BEFORE_RECONNECT;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_STARTED;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_VERIFICATION;
import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.protocols.v2.runtime.RetryPolicy;
import org.junit.jupiter.api.Test;

/**
 * Watchdog policy (scenario S8 at unit level): every acknowledgment-waiting state arms a watchdog;
 * the standard response is a reset to {@code ERROR}, while the verification watchdog is the named exception that
 * disconnects to reconnect; and {@code WAITING_FOR_CONNECTION_RETRY} arms no watchdog at all.
 */
class ProtocolAdapterWrapperWatchdogTest {

    @Test
    void watchdogInWaitingForStarted_resetsToError() {
        final WrapperTestFixture fixture =
                WrapperTestFixture.builder().watchdogTimeoutMillis(500).build();
        fixture.adapter.startReply = MockProtocolAdapter.Reply.DROP;
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.state()).isEqualTo(WAITING_FOR_STARTED);

        fixture.advance(500);
        assertThat(fixture.state()).isEqualTo(ERROR);
        assertThat(fixture.health.errorReasons).hasSize(1);
    }

    @Test
    void watchdogInWaitingForConnected_resetsToError() {
        final WrapperTestFixture fixture =
                WrapperTestFixture.builder().watchdogTimeoutMillis(500).build();
        fixture.adapter.connectReply = MockProtocolAdapter.Reply.DROP;
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.state()).isEqualTo(WAITING_FOR_CONNECTED);

        fixture.advance(500);
        assertThat(fixture.state()).isEqualTo(ERROR);
    }

    @Test
    void verificationWatchdog_disconnectsToReconnect_doesNotReset() {
        final WrapperTestFixture fixture =
                WrapperTestFixture.builder().watchdogTimeoutMillis(500).build();
        fixture.adapter.verifyDrop = true;
        fixture.adapter.disconnectReply = MockProtocolAdapter.Reply.DROP; // observe the intermediate state
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.state()).isEqualTo(WAITING_FOR_VERIFICATION);

        fixture.advance(500);
        assertThat(fixture.state()).isEqualTo(WAITING_FOR_DISCONNECTED_BEFORE_RECONNECT);
        assertThat(fixture.health.errorReasons).isEmpty(); // a recoverable condition, not an ERROR
        assertThat(fixture.commands()).contains("disconnect");
    }

    @Test
    void connectionRetry_armsNoWatchdog() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder()
                .watchdogTimeoutMillis(500) // shorter than the 1 s backoff
                .retryPolicy(RetryPolicy.defaults())
                .build();
        fixture.adapter.connectReply = MockProtocolAdapter.Reply.FAIL_CONNECTION;
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.state()).isEqualTo(WAITING_FOR_CONNECTION_RETRY);

        // Advance past the watchdog timeout but before the backoff: no watchdog exists, so nothing fires.
        fixture.advance(500);
        assertThat(fixture.state()).isEqualTo(WAITING_FOR_CONNECTION_RETRY);
        assertThat(fixture.health.errorReasons).isEmpty();
    }
}
