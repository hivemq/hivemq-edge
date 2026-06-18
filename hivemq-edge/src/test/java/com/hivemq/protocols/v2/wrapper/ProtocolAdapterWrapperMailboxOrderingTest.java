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
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_CONNECTED;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_DISCONNECTED;
import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.adapter.sdk.api.v2.node.Node;
import org.junit.jupiter.api.Test;

/**
 * The mailbox priority ladder seen through the wrapper (design §5.1, §6.3; scenario S24 at unit level): an
 * acknowledgment ({@code EVENT}) enqueued alongside a due tick ({@code TICK}) is processed first, so the watchdog
 * it cancels never fires; and a goal command ({@code CONTROL}) jumps ahead of a backlog of data points
 * ({@code DATA}).
 */
class ProtocolAdapterWrapperMailboxOrderingTest {

    @Test
    void acknowledgmentOutranksTick_soTheWatchdogItCancelsNeverFires() {
        final WrapperTestFixture fixture =
                WrapperTestFixture.builder().watchdogTimeoutMillis(1000).build();
        fixture.adapter.connectReply =
                MockProtocolAdapter.Reply.DROP; // park in WAITING_FOR_CONNECTED, watchdog at 1000
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.state()).isEqualTo(WAITING_FOR_CONNECTED);

        // Enqueue the tick at the watchdog's fire time FIRST, then the connected() acknowledgment.
        fixture.tell(new ProtocolAdapterWrapperTick(1000));
        fixture.output.connected();
        fixture.drain();

        // EVENT beats TICK: connected() ran first and canceled the watchdog, so the tick fired nothing.
        assertThat(fixture.state()).isEqualTo(CONNECTED);
        assertThat(fixture.health.errorReasons).isEmpty();
    }

    @Test
    void controlCommandOutranksADataBacklog() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder().build();
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        fixture.adapter.disconnectReply = MockProtocolAdapter.Reply.DROP; // keep the stop path from enqueuing more

        final Node node = WrapperTestSupport.node("x");
        fixture.tell(new ProtocolAdapterWrapperEvent.DataPointReceived(
                node, WrapperTestSupport.dataPoint("temperature", "1")));
        fixture.tell(new ProtocolAdapterWrapperEvent.DataPointReceived(
                node, WrapperTestSupport.dataPoint("temperature", "2")));
        fixture.tell(new ProtocolAdapterWrapperEvent.DataPointReceived(
                node, WrapperTestSupport.dataPoint("temperature", "3")));
        fixture.tell(new ProtocolAdapterWrapperCommand.StopAdapter());

        fixture.deliverOne(); // the highest-priority message must be the CONTROL command

        assertThat(fixture.state()).isEqualTo(WAITING_FOR_DISCONNECTED); // the stop command took effect first
        assertThat(fixture.pending()).isEqualTo(3); // the three data points are still queued behind it
    }
}
