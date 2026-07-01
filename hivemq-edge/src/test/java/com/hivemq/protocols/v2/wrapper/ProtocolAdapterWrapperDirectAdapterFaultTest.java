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
 * A direct {@code ProtocolAdapter} that throws a synchronous command exception on the wrapper's dispatch thread —
 * instead of reporting the failure through the output façade — must still drive the wrapper to {@code ERROR} and
 * notify the supervisor, not merely be logged. The wrapper's fault fence converts the throw into the adapter-error
 * path exactly as a reported {@code error(ADAPTER, ...)} would. A template adapter cannot reach this fence (its command
 * methods only tell its own mailbox), but a direct implementation that runs its work inline on the dispatch thread can.
 */
class ProtocolAdapterWrapperDirectAdapterFaultTest {

    @Test
    void throwingStart_entersErrorAndNotifiesTheSupervisor() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder().build();
        fixture.adapter.throwOnStart = true;

        fixture.activate(ProtocolAdapterDirection.NORTHBOUND); // STOPPED → start() throws on the dispatch thread

        assertThat(fixture.state()).isEqualTo(ERROR);
        assertThat(fixture.health.errorReasons).hasSize(1);
        assertThat(fixture.snapshot().lastErrorReason()).contains("protocol adapter failed");
        assertThat(fixture.commands()).containsExactly("start");
    }

    @Test
    void throwingConnect_entersErrorAndNotifiesTheSupervisor() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder().build();
        fixture.adapter.throwOnConnect = true;

        fixture.activate(ProtocolAdapterDirection.NORTHBOUND); // start acknowledges, then connect() throws

        assertThat(fixture.state()).isEqualTo(ERROR);
        assertThat(fixture.health.errorReasons).hasSize(1);
        assertThat(fixture.commands()).containsExactly("start", "connect");
    }

    @Test
    void throwingPollBatch_entersErrorAndNotifiesTheSupervisor() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder()
                .runningCoordinator()
                .nodes(List.of(WrapperTestSupport.pair("temperature")))
                .pollIntervalMillis(1000)
                .build();
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.state()).isEqualTo(CONNECTED);
        fixture.adapter.throwOnPollBatch = true;

        fixture.advance(1000); // the poll interval elapses; the due poll dispatches pollBatch, which throws

        assertThat(fixture.state()).isEqualTo(ERROR);
        assertThat(fixture.health.errorReasons).hasSize(1);
        assertThat(fixture.commands()).contains("pollBatch");
    }
}
