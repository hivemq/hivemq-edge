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
package com.hivemq.protocols.v2.southbound;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxSender;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.NodeProperty;
import com.hivemq.protocols.v2.tag.SouthboundWriteOutcome;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperMessage;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperWriteRequest;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The {@link SouthboundWritePump} driving an {@link InMemorySouthboundCommandSource}: it keeps a single write in
 * flight, advances only when the current one settles, and disposes each source command by outcome — commit on
 * success, dead-letter on device failure, release-and-pause on abort/rejection. The adapter (here a capturing sender
 * the test settles by hand) never sees a second write while one is outstanding.
 */
class SouthboundWritePumpTest {

    private static final @NotNull Node NODE = new TestNode("setpoint");

    @Test
    void keepsOneInFlight_committingEachReleasesTheNext() {
        final InMemorySouthboundCommandSource source = new InMemorySouthboundCommandSource(100);
        final CapturingSender sender = new CapturingSender();
        final SouthboundWritePump pump = new SouthboundWritePump(sender, NODE, source);

        source.offer(value(0));
        source.offer(value(1));
        source.offer(value(2));

        // Only the first write reached the adapter; all three are still in the source (none committed yet).
        assertThat(sender.requests).hasSize(1);
        assertThat(pump.inFlight()).isTrue();
        assertThat(source.pendingSize()).isEqualTo(3);

        sender.settleLast(SouthboundWriteOutcome.SUCCEEDED);
        assertThat(source.committed()).isEqualTo(1);
        assertThat(sender.requests).hasSize(2); // next one forwarded
        assertThat(source.pendingSize()).isEqualTo(2);

        sender.settleLast(SouthboundWriteOutcome.SUCCEEDED);
        sender.settleLast(SouthboundWriteOutcome.SUCCEEDED);

        assertThat(pump.inFlight()).isFalse();
        assertThat(pump.delivered()).isEqualTo(3);
        assertThat(source.pendingSize()).isZero();
        assertThat(pump.rejectedByAdapter()).isZero();
    }

    @Test
    void deviceFailure_deadLettersTheCommand_andAdvances() {
        final InMemorySouthboundCommandSource source = new InMemorySouthboundCommandSource(100);
        final CapturingSender sender = new CapturingSender();
        final SouthboundWritePump pump = new SouthboundWritePump(sender, NODE, source);
        source.offer(value(0));
        source.offer(value(1));

        sender.settleLast(SouthboundWriteOutcome.FAILED);

        assertThat(pump.deadLettered()).isEqualTo(1);
        assertThat(source.deadLettered()).isEqualTo(1);
        assertThat(source.deadLetters()).hasSize(1);
        assertThat(sender.requests).hasSize(2); // advanced past the rejected command
        assertThat(pump.inFlight()).isTrue();
    }

    @Test
    void abortedWrite_isReleasedAndPauses_thenResumeRedeliversTheSameCommand() {
        final InMemorySouthboundCommandSource source = new InMemorySouthboundCommandSource(100);
        final CapturingSender sender = new CapturingSender();
        final SouthboundWritePump pump = new SouthboundWritePump(sender, NODE, source);
        source.offer(value(0));
        source.offer(value(1));
        final DataPoint firstValue = sender.requests.get(0).value();

        // The adapter aborts the in-flight write (connection lost / deactivated).
        sender.settleLast(SouthboundWriteOutcome.ABORTED);

        assertThat(pump.aborted()).isEqualTo(1);
        assertThat(source.released()).isEqualTo(1);
        assertThat(source.committed()).isZero(); // nothing was delivered
        assertThat(pump.paused()).isTrue();
        assertThat(pump.inFlight()).isFalse();
        assertThat(sender.requests).hasSize(1); // paused: not re-forwarded automatically

        // Resuming (adapter ready again) redelivers the very same command — durability, not loss.
        pump.resume();
        assertThat(pump.paused()).isFalse();
        assertThat(sender.requests).hasSize(2);
        assertThat(sender.requests.get(1).value()).isEqualTo(firstValue);
    }

    @Test
    void rejectedBusy_isCountedAsAnAlarm_andReleasesAndPauses() {
        final InMemorySouthboundCommandSource source = new InMemorySouthboundCommandSource(100);
        final CapturingSender sender = new CapturingSender();
        final SouthboundWritePump pump = new SouthboundWritePump(sender, NODE, source);
        source.offer(value(0));

        sender.settleLast(SouthboundWriteOutcome.REJECTED_BUSY);

        assertThat(pump.rejectedByAdapter()).isEqualTo(1);
        assertThat(source.released()).isEqualTo(1);
        assertThat(pump.paused()).isTrue();
    }

    private static @NotNull DataPoint value(final int i) {
        return new TestDataPoint("setpoint", i);
    }

    /** A send-only mailbox stand-in that records each write request and lets the test settle it as the adapter would. */
    private static final class CapturingSender implements MailboxSender<ProtocolAdapterWrapperMessage> {

        private final @NotNull List<ProtocolAdapterWrapperWriteRequest> requests = new ArrayList<>();

        @Override
        public void tell(final @NotNull ProtocolAdapterWrapperMessage message) {
            requests.add((ProtocolAdapterWrapperWriteRequest) message);
        }

        private void settleLast(final @NotNull SouthboundWriteOutcome outcome) {
            requests.get(requests.size() - 1).completion().settle(outcome);
        }
    }

    private record TestDataPoint(
            @NotNull String tagName, @NotNull Object value) implements DataPoint {

        @Override
        public @NotNull Object getTagValue() {
            return value;
        }

        @Override
        public @NotNull String getTagName() {
            return tagName;
        }
    }

    private static final class TestNode extends Node {

        private final @NotNull String identifier;

        private TestNode(final @NotNull String identifier) {
            this.identifier = identifier;
        }

        @Override
        public @NotNull String nodeId() {
            return identifier;
        }

        @Override
        public @NotNull String nodeString() {
            return "{\"identifier\":\"" + identifier + "\"}";
        }

        @Override
        public @NotNull EnumSet<NodeProperty> properties() {
            return EnumSet.of(NodeProperty.UNIQUE);
        }
    }
}
