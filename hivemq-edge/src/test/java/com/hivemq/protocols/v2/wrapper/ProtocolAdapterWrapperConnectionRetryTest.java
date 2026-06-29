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
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.ERROR;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_CONNECTION_RETRY;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_DISCONNECTED_BEFORE_RECONNECT;
import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.adapter.sdk.api.v2.model.ErrorScope;
import com.hivemq.protocols.v2.runtime.RetryPolicy;
import org.junit.jupiter.api.Test;

/**
 * Connection loss and recovery (scenarios S2, S5, S6, S19 at unit level): a connect failure
 * backs off and reconnects; a spontaneous {@code disconnected()} while connected differs from an
 * {@code error(CONNECTION)} (which disconnects cleanly first); and exhausting the retry policy escalates to
 * {@code ERROR} and notifies the supervisor.
 */
class ProtocolAdapterWrapperConnectionRetryTest {

    @Test
    void connectFailsThenSucceeds_recoversAfterBackoff() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder().build();
        fixture.adapter.connectReplies.add(MockProtocolAdapter.Reply.FAIL_CONNECTION);
        fixture.adapter.connectReplies.add(MockProtocolAdapter.Reply.ACK);

        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.state()).isEqualTo(WAITING_FOR_CONNECTION_RETRY);

        fixture.advance(1000); // the 1 s backoff fires → reconnect → verify → CONNECTED
        assertThat(fixture.state()).isEqualTo(CONNECTED);
        assertThat(fixture.commands()).containsExactly("start", "connect", "connect", "verifyBatch");
    }

    @Test
    void spontaneousDisconnectWhileConnected_goesStraightToConnectionRetry() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder().build();
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.state()).isEqualTo(CONNECTED);

        fixture.output.disconnected(); // spontaneous loss
        fixture.drain();

        assertThat(fixture.state()).isEqualTo(WAITING_FOR_CONNECTION_RETRY);
        // No disconnect() was issued — a spontaneous loss goes straight to backoff (contrast with S6).
        assertThat(fixture.commands()).containsExactly("start", "connect", "verifyBatch");
    }

    @Test
    void connectionErrorWhileConnected_disconnectsCleanlyBeforeBackingOff() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder().build();
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        fixture.adapter.disconnectReply = MockProtocolAdapter.Reply.DROP; // pause to observe the intermediate state

        fixture.output.error(ErrorScope.CONNECTION, "lost");
        fixture.drain();

        assertThat(fixture.state()).isEqualTo(WAITING_FOR_DISCONNECTED_BEFORE_RECONNECT);
        assertThat(fixture.commands()).contains("disconnect"); // a clean disconnect first (contrast with S5)

        fixture.output.disconnected();
        fixture.drain();
        assertThat(fixture.state()).isEqualTo(WAITING_FOR_CONNECTION_RETRY);
    }

    @Test
    void maximumRetriesExceeded_escalatesToErrorAndNotifiesTheSupervisor() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder()
                .retryPolicy(new RetryPolicy(1000, 1.41, 32000, 1)) // one retry, then give up
                .build();
        fixture.adapter.connectReply = MockProtocolAdapter.Reply.FAIL_CONNECTION; // every attempt fails

        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.state()).isEqualTo(WAITING_FOR_CONNECTION_RETRY);

        fixture.advance(1100); // backoff fires → second connect fails → retries exhausted → ERROR
        assertThat(fixture.state()).isEqualTo(ERROR);
        assertThat(fixture.health.errorReasons).hasSize(1);
        assertThat(fixture.snapshot().lastErrorReason()).contains("retries");
    }
}
