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
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Defensive tolerance and the {@code ERROR} absorption rule (scenarios S9, S25 at unit level). An
 * unexpected event resets once — stop best-effort, notify the supervisor, enter {@code ERROR}. The
 * {@code stopped()} / {@code disconnected()} that the reset's own {@code stop()} provokes are then absorbed in
 * {@code ERROR}: no second {@code stop()}, no reset loop, exactly one reset counted.
 */
class ProtocolAdapterWrapperDefensiveResetTest {

    private static long countOf(final WrapperTestFixture fixture, final String command) {
        return fixture.commands().stream().filter(command::equals).count();
    }

    @Test
    void unexpectedEvent_triggersOneDefensiveReset() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder().build();
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND); // CONNECTED, goal stays "connected"

        fixture.output.started(); // started() is unexpected while CONNECTED
        fixture.drain();

        assertThat(fixture.state()).isEqualTo(ERROR);
        assertThat(fixture.defensiveResets()).isEqualTo(1);
        assertThat(fixture.health.errorReasons).hasSize(1);
        assertThat(countOf(fixture, "stop")).isEqualTo(1); // best-effort stop issued exactly once
    }

    @Test
    void stoppedAndDisconnectedArrivingInError_areAbsorbed_noSecondStop_noLoop() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder().build();
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);

        fixture.output.started(); // → defensive reset → ERROR; the reset's stop() provokes a stopped()
        fixture.drain();
        assertThat(fixture.state()).isEqualTo(ERROR);
        assertThat(countOf(fixture, "stop")).isEqualTo(1);

        // More stopped()/disconnected() arriving in ERROR are absorbed — no new stop, no new reset.
        fixture.output.stopped();
        fixture.output.disconnected();
        fixture.drain();

        assertThat(fixture.state()).isEqualTo(ERROR);
        assertThat(countOf(fixture, "stop")).isEqualTo(1);
        assertThat(fixture.defensiveResets()).isEqualTo(1);
    }

    @Test
    void everyStaleEventInError_isAbsorbed() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder().build();
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        fixture.output.started(); // → ERROR
        fixture.drain();
        assertThat(fixture.state()).isEqualTo(ERROR);

        fixture.output.connected();
        fixture.output.dataPoint(WrapperTestSupport.node("x"), WrapperTestSupport.dataPoint("temperature", "v"));
        fixture.output.pollComplete(WrapperTestSupport.node("x"));
        fixture.output.nodeError(WrapperTestSupport.node("x"), "boom", true);
        fixture.output.error(com.hivemq.adapter.sdk.api.v2.model.ErrorScope.CONNECTION, "noise");
        fixture.drain();

        assertThat(fixture.state()).isEqualTo(ERROR);
        assertThat(fixture.defensiveResets()).isEqualTo(1); // still exactly one reset
    }
}
