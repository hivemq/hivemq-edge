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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperMessage;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperSouthboundMessage.SouthboundArrival;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperSouthboundMessage.SouthboundRead;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperSouthboundMessage.SouthboundSize;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The in-memory stand-in store: a read leases the head <b>without removing it</b> — so an abandoned command is
 * simply still there to be read again — only {@link InMemorySouthboundWriteBacklog#delete} removes anything, and
 * overflow sheds the newest offer. Every answer leaves as a mailbox message, never as a return value.
 */
class InMemorySouthboundWriteBacklogTest {

    private static final @NotNull String TAG = "setpoint";

    private final @NotNull RecordingSender sender = new RecordingSender();

    @Test
    void aReadLeasesTheHeadWithoutRemovingIt_andIsIdempotent() {
        final InMemorySouthboundWriteBacklog backlog = new InMemorySouthboundWriteBacklog(10, TAG, sender);
        backlog.offer(value("a"));
        backlog.offer(value("b"));

        backlog.requestRead(1);
        backlog.requestRead(2);

        // Both reads answered with the same command, and nothing was removed — that is what makes an abandoned
        // command redeliver for free.
        assertThat(sender.reads).hasSize(2);
        assertThat(sender.reads).allSatisfy(read -> assertThat(read.command()).isNotNull());
        assertThat(sender.reads.getFirst().command().value().getTagValue()).isEqualTo("a");
        assertThat(sender.reads.get(1).command().value().getTagValue()).isEqualTo("a");
        assertThat(backlog.pendingSize()).isEqualTo(2);
    }

    @Test
    void deleteAdvancesTheHead_inFifoOrder() {
        final InMemorySouthboundWriteBacklog backlog = new InMemorySouthboundWriteBacklog(10, TAG, sender);
        backlog.offer(value("a"));
        backlog.offer(value("b"));
        backlog.requestRead(1);
        final SouthboundCommand first = sender.reads.getFirst().command();
        assertThat(first).isNotNull();

        backlog.delete(first.id());
        backlog.requestRead(2);

        assertThat(backlog.pendingSize()).isEqualTo(1);
        assertThat(sender.reads.get(1).command().value().getTagValue()).isEqualTo("b");
        assertThat(backlog.deletedCommands())
                .extracting(command -> command.value().getTagValue())
                .containsExactly("a");
    }

    @Test
    void deletingSomethingOtherThanTheHeadIsACallerBug_andSaysSo() {
        final InMemorySouthboundWriteBacklog backlog = new InMemorySouthboundWriteBacklog(10, TAG, sender);
        backlog.offer(value("a"));

        assertThatThrownBy(() -> backlog.delete("not-the-head"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not the head");
        assertThat(backlog.pendingSize()).isEqualTo(1); // untouched
    }

    @Test
    void overflowShedsTheNewest_andCounts() {
        final InMemorySouthboundWriteBacklog backlog = new InMemorySouthboundWriteBacklog(3, TAG, sender);
        for (int i = 0; i < 5; i++) {
            backlog.offer(value("v" + i));
        }

        assertThat(backlog.pendingSize()).isEqualTo(3);
        assertThat(backlog.offered()).isEqualTo(5);
        assertThat(backlog.droppedByOverflow()).isEqualTo(2);
        // The oldest three survived: the bound sheds the newest offer, never the queued work.
        backlog.requestRead(1);
        assertThat(sender.reads.getFirst().command().value().getTagValue()).isEqualTo("v0");
    }

    @Test
    void everyOfferHintsTheDeliverySide() {
        final InMemorySouthboundWriteBacklog backlog = new InMemorySouthboundWriteBacklog(10, TAG, sender);
        backlog.offer(value("a"));
        backlog.offer(value("b"));

        assertThat(sender.arrivals).hasSize(2);
        assertThat(sender.arrivals.getFirst().tagName()).isEqualTo(TAG);
    }

    @Test
    void aSizeRequestReportsTheDepth() {
        final InMemorySouthboundWriteBacklog backlog = new InMemorySouthboundWriteBacklog(10, TAG, sender);
        backlog.offer(value("a"));
        backlog.offer(value("b"));

        backlog.requestSize(7);

        assertThat(sender.sizes).hasSize(1);
        assertThat(sender.sizes.getFirst().readToken()).isEqualTo(7);
        assertThat(sender.sizes.getFirst().size()).isEqualTo(2);
    }

    @Test
    void closeDropsThePendingCommands_soAStaleWindowCannotDeliverThem() {
        // A dropped or replaced channel's old aspect can still report itself writable; nothing it says may deliver
        // a command that was meant to be discarded.
        final InMemorySouthboundWriteBacklog backlog = new InMemorySouthboundWriteBacklog(10, TAG, sender);
        backlog.offer(value("a"));

        backlog.close();

        assertThat(backlog.isClosed()).isTrue();
        assertThat(backlog.pendingSize()).isZero();
        backlog.requestRead(1);
        assertThat(sender.reads.getFirst().command()).isNull();
        backlog.offer(value("late"));
        assertThat(backlog.pendingSize()).isZero();
    }

    private static @NotNull DataPoint value(final @NotNull String v) {
        return new TestDataPoint(TAG, v);
    }

    /** Sorts the store's answers by kind so each assertion reads for itself. */
    private static final class RecordingSender
            implements com.hivemq.adapter.sdk.api.v2.messaging.MailboxSender<ProtocolAdapterWrapperMessage> {

        private final @NotNull List<SouthboundRead> reads = new ArrayList<>();
        private final @NotNull List<SouthboundSize> sizes = new ArrayList<>();
        private final @NotNull List<SouthboundArrival> arrivals = new ArrayList<>();

        @Override
        public void tell(final @NotNull ProtocolAdapterWrapperMessage message) {
            switch (message) {
                case final SouthboundRead read -> reads.add(read);
                case final SouthboundSize size -> sizes.add(size);
                case final SouthboundArrival arrival -> arrivals.add(arrival);
                default -> throw new IllegalStateException("unexpected message from a store: " + message);
            }
        }
    }
}
