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
 * The interim {@link InMemorySouthboundCommandSource}: FIFO read-without-remove with a single outstanding read,
 * commit/release/dead-letter disposition, a bounded backlog that sheds the newest on overflow, and a wakeup fired
 * outside its own lock.
 */
class InMemorySouthboundCommandSourceTest {

    @Test
    void pollReadsHeadWithoutRemoving_andCommitAdvancesFifo() {
        final InMemorySouthboundCommandSource source = new InMemorySouthboundCommandSource(10);
        source.offer(value("a"));
        source.offer(value("b"));

        final SouthboundCommand first = source.poll();
        assertThat(first).isNotNull();
        assertThat(first.value().getTagValue()).isEqualTo("a");
        assertThat(source.pendingSize()).isEqualTo(2); // still present — not removed by poll

        // A second poll returns nothing while the first is outstanding (single-in-flight).
        assertThat(source.poll()).isNull();

        source.commit(first.id());
        assertThat(source.pendingSize()).isEqualTo(1);
        final SouthboundCommand second = source.poll();
        assertThat(second).isNotNull();
        assertThat(second.value().getTagValue()).isEqualTo("b");
    }

    @Test
    void releaseKeepsTheHead_soItIsPolledAgain() {
        final InMemorySouthboundCommandSource source = new InMemorySouthboundCommandSource(10);
        source.offer(value("a"));

        final SouthboundCommand polled = source.poll();
        assertThat(polled).isNotNull();
        source.release(polled.id());

        assertThat(source.released()).isEqualTo(1);
        assertThat(source.pendingSize()).isEqualTo(1);
        final SouthboundCommand again = source.poll();
        assertThat(again).isNotNull();
        assertThat(again.id()).isEqualTo(polled.id()); // the very same command
    }

    @Test
    void deadLetterRemovesAndRecords() {
        final InMemorySouthboundCommandSource source = new InMemorySouthboundCommandSource(10);
        source.offer(value("a"));
        final SouthboundCommand polled = source.poll();
        assertThat(polled).isNotNull();

        source.deadLetter(polled.id(), "device rejected");

        assertThat(source.deadLettered()).isEqualTo(1);
        assertThat(source.deadLetters())
                .extracting(c -> c.value().getTagValue())
                .containsExactly("a");
        assertThat(source.pendingSize()).isZero();
    }

    @Test
    void overflowShedsTheNewest_andCounts() {
        final InMemorySouthboundCommandSource source = new InMemorySouthboundCommandSource(3);
        for (int i = 0; i < 5; i++) {
            source.offer(value(Integer.toString(i)));
        }

        assertThat(source.pendingSize()).isEqualTo(3);
        assertThat(source.offered()).isEqualTo(5);
        assertThat(source.droppedByOverflow()).isEqualTo(2);
    }

    @Test
    void wakeupFiresOnOffer_outsideTheLock() {
        final InMemorySouthboundCommandSource source = new InMemorySouthboundCommandSource(10);
        final AtomicInteger nudges = new AtomicInteger();
        source.onAvailable(nudges::incrementAndGet);

        source.offer(value("a"));
        source.offer(value("b"));

        assertThat(nudges.get()).isEqualTo(2);
    }

    @Test
    void disposingANonHeldCommandIsRejected() {
        final InMemorySouthboundCommandSource source = new InMemorySouthboundCommandSource(10);
        source.offer(value("a"));

        assertThatThrownBy(() -> source.commit("not-the-head")).isInstanceOf(IllegalStateException.class);
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
