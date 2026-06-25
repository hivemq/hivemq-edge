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
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_CONNECTION_RETRY;
import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.adapter.sdk.api.v2.model.VerifyOutcome;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The polled read aspect (scenarios S1, S3, S4, S5, S12 at unit level). The aspect verifies on
 * connect, then polls on a cadence; a poll failure is its own retry; transient verification failures retry on a
 * timer, permanent ones suspend; an adapter loss parks the aspect and re-verifies on reconnect. All driven on
 * {@code FakeClock} + {@code ManualDispatcher} through the running coordinator, observed only through the
 * published snapshot.
 */
class TagAspectReadPolledTest {

    private static @NotNull WrapperTestFixture polledFixture() {
        return WrapperTestFixture.builder()
                .runningCoordinator()
                .nodes(List.of(WrapperTestSupport.pair("temperature")))
                .pollIntervalMillis(1000)
                .build();
    }

    @Test
    void verifyThenPollCadence_deliversValuesAndKeepsPolling() {
        final WrapperTestFixture fixture = polledFixture();

        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.state()).isEqualTo(CONNECTED);
        // Verified on connect via the gate's routed result, now resting at the poll interval.
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");

        fixture.advance(1000); // the poll interval elapses: a poll is requested
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_DATAPOINT");
        assertThat(fixture.commands()).contains("pollBatch");

        fixture.output.dataPoint(fixture.nodeFor("temperature"), WrapperTestSupport.dataPoint("temperature", "21"));
        fixture.drain();
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
        assertThat(fixture.tag("temperature").failureCount()).isZero();
    }

    @Test
    void pollFailure_returnsToPollIntervalAndCountsWithNoNewState() {
        final WrapperTestFixture fixture = polledFixture();
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        fixture.advance(1000); // WAITING_FOR_POLL_DATAPOINT

        fixture.output.nodeError(fixture.nodeFor("temperature"), "read timeout", false);
        fixture.drain();

        // The next scheduled poll is the retry — no new state, only the counter advances.
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
        assertThat(fixture.tag("temperature").failureCount()).isEqualTo(1);
        assertThat(fixture.tag("temperature").lastFailureReason()).isEqualTo("read timeout");
    }

    @Test
    void transientVerificationFailure_retriesOnTimerThenSucceeds() {
        final WrapperTestFixture fixture = polledFixture();
        fixture.adapter.verifyOutcome = new VerifyOutcome.TransientFailure("device busy");

        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);

        // The gate still reaches CONNECTED (any outcome counts); the aspect schedules a verification retry.
        assertThat(fixture.state()).isEqualTo(CONNECTED);
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_VERIFICATION_RETRY");

        fixture.adapter.verifyOutcome = new VerifyOutcome.Success();
        fixture.advance(1000); // the retry timer fires: re-verify, succeed, begin polling
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
    }

    @Test
    void permanentVerificationFailure_suspendsTheTagButLeavesTheAdapterConnected() {
        final WrapperTestFixture fixture = polledFixture();
        fixture.adapter.verifyOutcome = new VerifyOutcome.PermanentFailure("unknown address");

        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);

        assertThat(fixture.state()).isEqualTo(CONNECTED); // the adapter does not reconnect (S4)
        assertThat(fixture.readState("temperature")).isEqualTo("ERROR_PERMANENT_VERIFICATION_FAILURE");

        fixture.advance(10_000); // no retry timer — a permanent failure stays put without a user-commanded retry
        assertThat(fixture.readState("temperature")).isEqualTo("ERROR_PERMANENT_VERIFICATION_FAILURE");
    }

    @Test
    void adapterLoss_parksTheAspectAndReVerifiesOnReconnect() {
        final WrapperTestFixture fixture = polledFixture();
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");

        fixture.output.disconnected(); // a spontaneous loss while CONNECTED (S5)
        fixture.drain();
        assertThat(fixture.state()).isEqualTo(WAITING_FOR_CONNECTION_RETRY);
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_ADAPTER_READY");

        fixture.advance(1000); // the connection backoff fires: reconnect, re-verify, resume polling
        assertThat(fixture.state()).isEqualTo(CONNECTED);
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
    }

    @Test
    void unusedTag_staysDeactivatedEvenWhenActivated() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder()
                .runningCoordinator()
                .nodes(List.of(WrapperTestSupport.pair("temperature")))
                .readUsed(Set.of()) // no mapping consumes the tag — the third condition fails
                .build();

        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);

        assertThat(fixture.state()).isEqualTo(CONNECTED);
        assertThat(fixture.readState("temperature")).isEqualTo("DEACTIVATED");
    }
}
