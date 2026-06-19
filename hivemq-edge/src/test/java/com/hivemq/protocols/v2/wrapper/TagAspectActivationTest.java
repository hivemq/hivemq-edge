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

import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The three-condition rule applied to the read aspect through the running coordinator (design §7.1; scenario S27
 * at unit level — the read-side gate and the direction master switch; the write-side rows land in a later task).
 * Deactivating the read aspect's preference keeps it off while the adapter runs; a config-origin
 * {@code ApplyActivation} flips the preference atomically and the aspect verifies <b>without reconnecting</b>;
 * deactivating the direction returns the aspect to {@code DEACTIVATED}.
 */
class TagAspectActivationTest {

    private static long connectCount(final @NotNull WrapperTestFixture fixture) {
        return fixture.commands().stream().filter("connect"::equals).count();
    }

    @Test
    void readPreferenceOff_keepsTheAspectDeactivatedWhileTheAdapterRuns() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder()
                .runningCoordinator()
                .nodes(List.of(WrapperTestSupport.pair("temperature")))
                .activation(Map.of("temperature", new TagAspectActivationPreference(false, true)))
                .build();

        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);

        assertThat(fixture.state()).isEqualTo(CONNECTED);
        assertThat(fixture.readState("temperature")).isEqualTo("DEACTIVATED");
    }

    @Test
    void applyActivation_activatesAndVerifiesWithoutReconnecting() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder()
                .runningCoordinator()
                .nodes(List.of(WrapperTestSupport.pair("temperature")))
                .activation(Map.of("temperature", new TagAspectActivationPreference(false, true)))
                .build();
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.readState("temperature")).isEqualTo("DEACTIVATED");
        assertThat(connectCount(fixture)).isEqualTo(1);

        // Config reload flips read-activated on — an ACTIVATION_ONLY transition (design §8.2).
        fixture.send(new ProtocolAdapterWrapperCommand.ApplyActivation(
                new ProtocolAdapterGoalState(true, false),
                Map.of("temperature", TagAspectActivationPreference.defaults())));

        assertThat(fixture.state()).isEqualTo(CONNECTED); // still connected
        assertThat(connectCount(fixture)).isEqualTo(1); // never reconnected
        // The newly-activated aspect verifies itself against the live connection and begins polling.
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
    }

    @Test
    void deactivatingTheDirection_returnsTheAspectToDeactivated() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder()
                .runningCoordinator()
                .nodes(List.of(WrapperTestSupport.pair("temperature")))
                .build();
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");

        fixture.deactivate(ProtocolAdapterDirection.NORTHBOUND);

        assertThat(fixture.state()).isEqualTo(STOPPED);
        assertThat(fixture.readState("temperature")).isEqualTo("DEACTIVATED");
    }
}
