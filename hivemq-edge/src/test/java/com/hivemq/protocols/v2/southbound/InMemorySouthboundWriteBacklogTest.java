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
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The interim {@link InMemorySouthboundWriteBacklog}: head-without-remove FIFO where only a terminal outcome
 * deletes (commit or dead-letter — an abandoned command simply stays at the head), a bounded backlog that sheds
 * the newest on overflow, and a wakeup fired outside its own lock.
 */
class InMemorySouthboundWriteBacklogTest {

    @Test
    void headReadsWithoutRemoving_andRemoveHeadAdvancesFifo() {
        final InMemorySouthboundWriteBacklog backlog = new InMemorySouthboundWriteBacklog(10);
        backlog.offer(value("a"));
        backlog.offer(value("b"));

        final SouthboundCommand first = backlog.head();
        assertThat(first).isNotNull();
        assertThat(first.value().getTagValue()).isEqualTo("a");
        assertThat(backlog.pendingSize()).isEqualTo(2); // still present — not removed by head()

        // head() is idempotent: until the head is deleted, it is the same command — redelivery for free.
        final SouthboundCommand again = backlog.head();
        assertThat(again).isNotNull();
        assertThat(again.id()).isEqualTo(first.id());

        backlog.removeHead(first.id());
        assertThat(backlog.pendingSize()).isEqualTo(1);
        assertThat(backlog.committed()).isEqualTo(1);
        final SouthboundCommand second = backlog.head();
        assertThat(second).isNotNull();
        assertThat(second.value().getTagValue()).isEqualTo("b");
    }

    @Test
    void deadLetterHeadRemovesAndRecords() {
        final InMemorySouthboundWriteBacklog backlog = new InMemorySouthboundWriteBacklog(10);
        backlog.offer(value("a"));
        final SouthboundCommand head = backlog.head();
        assertThat(head).isNotNull();

        backlog.deadLetterHead(head.id(), "device rejected");

        assertThat(backlog.deadLettered()).isEqualTo(1);
        assertThat(backlog.deadLetters())
                .extracting(command -> command.value().getTagValue())
                .containsExactly("a");
        assertThat(backlog.pendingSize()).isZero();
    }

    @Test
    void overflowShedsTheNewest_andCounts() {
        final InMemorySouthboundWriteBacklog backlog = new InMemorySouthboundWriteBacklog(3);
        for (int i = 0; i < 5; i++) {
            backlog.offer(value(Integer.toString(i)));
        }

        assertThat(backlog.pendingSize()).isEqualTo(3);
        assertThat(backlog.offered()).isEqualTo(5);
        assertThat(backlog.droppedByOverflow()).isEqualTo(2);
        // The survivors are the oldest three — the newest were shed.
        final SouthboundCommand head = backlog.head();
        assertThat(head).isNotNull();
        assertThat(head.value().getTagValue()).isEqualTo("0");
    }

    @Test
    void wakeupFiresOnOffer_outsideTheLock() {
        final InMemorySouthboundWriteBacklog backlog = new InMemorySouthboundWriteBacklog(10);
        final AtomicInteger nudges = new AtomicInteger();
        backlog.onAvailable(() -> {
            // Re-entering the backlog from the wakeup must not deadlock — the wakeup fires outside the monitor.
            backlog.head();
            nudges.incrementAndGet();
        });

        backlog.offer(value("a"));
        backlog.offer(value("b"));

        assertThat(nudges.get()).isEqualTo(2);
    }

    @Test
    void deletingANonHeadCommandIsRejected() {
        final InMemorySouthboundWriteBacklog backlog = new InMemorySouthboundWriteBacklog(10);
        backlog.offer(value("a"));

        assertThatThrownBy(() -> backlog.removeHead("not-the-head")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> backlog.deadLetterHead("not-the-head", "reason"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(backlog.pendingSize()).isEqualTo(1); // untouched
    }

    private static @NotNull DataPoint value(final @NotNull String v) {
        return new TestDataPoint("setpoint", v);
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
}
