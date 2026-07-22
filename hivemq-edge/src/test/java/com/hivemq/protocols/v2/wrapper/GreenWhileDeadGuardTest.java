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
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The green-while-dead guard (EDG-824 #7): a contract-violating adapter that throws from a
 * synchronous adapter-facing call no longer kills the dispatch loop with a frozen GREEN snapshot — the wrapper
 * counts a defensive reset, enters {@code ERROR} with the throw as the reason, and keeps processing messages.
 */
class GreenWhileDeadGuardTest {

    private static WrapperTestFixture connectedFixture() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder()
                .runningCoordinator()
                .nodes(List.of(WrapperTestSupport.pair("temperature")))
                .pollIntervalMillis(1000)
                .build();
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        return fixture;
    }

    @Test
    void adapterThrowingFromAPollDispatch_entersErrorInsteadOfFrozenGreen() {
        final WrapperTestFixture fixture = connectedFixture();
        assertThat(fixture.state()).isEqualTo(CONNECTED);
        fixture.adapter.pollThrow = true;

        fixture.advance(1000); // the cadence polls; the batch dispatch throws on the dispatch thread

        assertThat(fixture.state()).isEqualTo(ERROR);
        assertThat(fixture.snapshot().lastErrorReason()).contains("adapter threw IllegalStateException");
        assertThat(fixture.defensiveResets()).isEqualTo(1);
    }

    @Test
    void afterTheThrow_theWrapperStillProcessesMessages() {
        final WrapperTestFixture fixture = connectedFixture();
        fixture.adapter.pollThrow = true;
        fixture.advance(1000);
        assertThat(fixture.state()).isEqualTo(ERROR);

        // The actor is alive: a follow-up command is processed, not queued into a dead mailbox. ERROR is manual
        // recovery territory — the state stays ERROR, but the snapshot keeps being republished.
        fixture.send(new ProtocolAdapterWrapperCommand.DeactivateDirection(ProtocolAdapterDirection.NORTHBOUND));
        assertThat(fixture.pending()).isZero();
        assertThat(fixture.snapshot()).isNotNull();
    }
}
