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
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The happy path and the activation-driven goal (design §6.2; scenarios S1, S18 at unit level). Start, connect,
 * verify, reach {@code CONNECTED} in the exact command sequence; the skip-verification flag reaches
 * {@code CONNECTED} without a verify batch; and a config-origin {@code ApplyActivation} drives the adapter goal up
 * to {@code CONNECTED} and back down to {@code STOPPED}.
 */
class ProtocolAdapterWrapperLifecycleTest {

    @Test
    void happyPath_reachesConnectedInTheExactCommandSequence() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder().build();

        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);

        assertThat(fixture.state()).isEqualTo(CONNECTED);
        assertThat(fixture.commands()).containsExactly("start", "connect", "verifyBatch");
        assertThat(fixture.snapshot().northboundActivated()).isTrue();
        assertThat(fixture.snapshot().southboundActivated()).isFalse();
    }

    @Test
    void skipVerification_reachesConnectedWithoutAVerifyBatch() {
        final WrapperTestFixture fixture =
                WrapperTestFixture.builder().skipVerification(true).build();

        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);

        assertThat(fixture.state()).isEqualTo(CONNECTED);
        assertThat(fixture.commands()).containsExactly("start", "connect");
    }

    @Test
    void applyActivation_drivesTheGoalUpToConnected() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder().build();

        fixture.send(new ProtocolAdapterWrapperCommand.ApplyActivation(
                new ProtocolAdapterGoalState(true, false),
                Map.of("temperature", TagAspectActivationPreference.defaults())));

        assertThat(fixture.state()).isEqualTo(CONNECTED);
    }

    @Test
    void applyActivation_drivesTheGoalDownToStopped() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder().build();
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.state()).isEqualTo(CONNECTED);

        fixture.send(new ProtocolAdapterWrapperCommand.ApplyActivation(
                ProtocolAdapterGoalState.stopped(), Map.of("temperature", TagAspectActivationPreference.defaults())));

        assertThat(fixture.state()).isEqualTo(STOPPED);
        assertThat(fixture.commands()).containsSubsequence("disconnect", "stop");
    }
}
