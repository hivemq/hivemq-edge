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
import static org.assertj.core.api.Assertions.assertThatCode;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.schema.ScalarSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxSender;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.protocols.v2.runtime.ProtocolAdapterMetrics;
import com.hivemq.protocols.v2.tag.SouthboundWriteOutcome;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperMessage;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperSouthboundMessage.SouthboundRead;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperSouthboundMessage.TagWritability;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * The {@link SouthboundWritePlane} as the wrapper sees it: one channel per write-mapped tag, every input arriving
 * as a message it routes by tag name, and windows that open only when the tag's own aspect reports itself writable.
 */
class SouthboundWritePlaneTest {

    private static final @NotNull String TAG = "setpoint";
    private static final @NotNull String OTHER = "ramp-rate";

    /** A metrics sink for a plane standing on its own; the registry is discarded with the test. */
    private static @NotNull ProtocolAdapterMetrics metrics() {
        return new ProtocolAdapterMetrics(new MetricRegistry(), "a1", () -> 0);
    }

    @Test
    void channelsExistOnlyForWriteMappedTags_andStartWithAClosedWindow() {
        final CapturingSender sender = new CapturingSender();
        final SouthboundWritePlane plane =
                new SouthboundWritePlane("a1", sender, 10, List.of(pair(TAG), pair(OTHER)), Set.of(TAG), metrics());

        assertThat(plane.writeMappedTagNames()).containsExactly(TAG);
        assertThat(plane.offer(OTHER, value(1))).isFalse(); // not write-mapped — no channel

        // The window starts closed: the offer waits in the store, nothing reaches the adapter.
        assertThat(plane.offer(TAG, value(1))).isTrue();
        sender.pump(plane);
        assertThat(sender.requests).isEmpty();
        final SouthboundWritePlane.TagChannel channel = plane.channel(TAG);
        assertThat(channel).isNotNull();
        assertThat(channel.queue().suspended()).isTrue();
        assertThat(pending(channel)).isEqualTo(1);
    }

    @Test
    void aWritabilityReportOpensTheWindow_andLosingItClosesIt() {
        final CapturingSender sender = new CapturingSender();
        final SouthboundWritePlane plane =
                new SouthboundWritePlane("a1", sender, 10, List.of(pair(TAG)), Set.of(TAG), metrics());
        plane.offer(TAG, value(1));
        plane.offer(TAG, value(2));

        // The tag verified: the window opens and the head is delivered — exactly one, single-in-flight.
        plane.onMessage(new TagWritability(TAG, true));
        sender.pump(plane);
        assertThat(sender.requests).hasSize(1);

        // The tag became unwritable (disconnect or deactivation): the window closes, and the aspect's abort keeps
        // the command where it is.
        plane.onMessage(new TagWritability(TAG, false));
        sender.settleLast(SouthboundWriteOutcome.ABORTED);
        sender.pump(plane);
        assertThat(sender.requests).hasSize(1); // closed: nothing redelivered

        // Writable again: the SAME command is delivered again.
        plane.onMessage(new TagWritability(TAG, true));
        sender.pump(plane);
        assertThat(sender.requests).hasSize(2);
        assertThat(sender.requests.get(1).value())
                .isEqualTo(sender.requests.getFirst().value());
    }

    @Test
    void messagesForATagWithNoChannelAreDropped() {
        final CapturingSender sender = new CapturingSender();
        final SouthboundWritePlane plane =
                new SouthboundWritePlane("a1", sender, 10, List.of(pair(TAG)), Set.of(TAG), metrics());

        // A de-mapped or replaced tag can still have answers in flight behind it; they are stale by definition.
        assertThatCode(() -> plane.onMessage(new TagWritability("unknown", true)))
                .doesNotThrowAnyException();
        assertThat(sender.requests).isEmpty();
    }

    @Test
    void updateTagSet_dropsGoneChannels_keepsSurvivingChannels_andRetargetsAChangedNode() {
        final CapturingSender sender = new CapturingSender();
        final NodeTagPair survivor = pair(TAG);
        final SouthboundWritePlane plane = new SouthboundWritePlane(
                "a1", sender, 10, List.of(survivor, pair(OTHER)), Set.of(TAG, OTHER), metrics());
        plane.offer(TAG, value(1));
        plane.offer(OTHER, value(2));
        plane.onMessage(new TagWritability(TAG, true));
        sender.pump(plane);
        // Abandon that delivery: the command is kept at the head and the window closes, which is the state a
        // reload actually finds — the aspects are torn down before the new tag set is applied.
        sender.settleLast(SouthboundWriteOutcome.ABORTED, "the tag was deactivated");
        sender.pump(plane);

        // Reload: OTHER is no longer write-mapped, TAG survives with the same node, "fresh" appears.
        plane.updateTagSet(List.of(survivor, pair("fresh")), Set.of(TAG, "fresh"));

        assertThat(plane.writeMappedTagNames()).containsExactlyInAnyOrder(TAG, "fresh");
        assertThat(plane.channel(OTHER)).isNull(); // dropped, its pending command with it

        // The survivor kept its store (the pending command rode out the reload) and had its window closed: the
        // rebuilt aspect re-verifies from scratch, and its writability report reopens it.
        final SouthboundWritePlane.TagChannel survived = plane.channel(TAG);
        assertThat(survived).isNotNull();
        assertThat(pending(survived)).isEqualTo(1);
        assertThat(survived.queue().suspended()).isTrue();

        final SouthboundWritePlane.TagChannel fresh = plane.channel("fresh");
        assertThat(fresh).isNotNull();
        assertThat(fresh.queue().suspended()).isTrue();

        // A node change retargets the channel rather than rebuilding it: the queued command survives and is
        // delivered to the node the tag now addresses. Rebuilding instead would have thrown the command away here
        // and — on the durable store, whose queue is keyed by the mapping topic and not by the node — read the very
        // same command straight back and delivered it to the new node regardless.
        final NodeTagPair movedTag = NodeTagPair.create(new TestNode("moved"), TAG, schema(), true, false);
        plane.updateTagSet(List.of(movedTag), Set.of(TAG));
        final SouthboundWritePlane.TagChannel retargeted = plane.channel(TAG);
        assertThat(retargeted).isNotNull();
        assertThat(retargeted).isSameAs(survived);
        assertThat(pending(retargeted)).isEqualTo(1);
        assertThat(retargeted.queue().suspended()).isTrue();

        sender.requests.clear();
        plane.onMessage(new TagWritability(TAG, true));
        sender.pump(plane);
        assertThat(sender.requests).hasSize(1);
        assertThat(sender.requests.getFirst().node().nodeId()).isEqualTo("moved");
    }

    @Test
    void closeSuspendsAndDropsEverything() {
        final CapturingSender sender = new CapturingSender();
        final SouthboundWritePlane plane =
                new SouthboundWritePlane("a1", sender, 10, List.of(pair(TAG)), Set.of(TAG), metrics());
        plane.offer(TAG, value(1));

        plane.close();

        assertThat(plane.writeMappedTagNames()).isEmpty();
        assertThat(plane.offer(TAG, value(2))).isFalse();
    }

    @Test
    void theTickReachesEveryChannel() {
        final AtomicInteger reads = new AtomicInteger();
        final CapturingSender sender = new CapturingSender();
        final SouthboundWritePlane plane = planeOverCountingStores(reads, null, sender);

        // Windows are closed, so no channel reads: the tick must be free on an idle adapter.
        for (int tick = 0; tick < SouthboundWriteQueue.POLL_TICKS; tick++) {
            plane.onTick();
        }
        assertThat(reads.get()).isZero();

        plane.onMessage(new TagWritability(TAG, true));
        plane.onMessage(new TagWritability(OTHER, true));
        sender.pump(plane);
        assertThat(reads.get()).isEqualTo(2); // opening a window reads once

        // And every poll cadence reads again on both.
        for (int tick = 0; tick < SouthboundWriteQueue.POLL_TICKS; tick++) {
            plane.onTick();
        }
        sender.pump(plane);
        assertThat(reads.get()).isEqualTo(4);
    }

    @Test
    void theTickIsGuardedPerChannel_soOneSickStoreCannotFaultTheAdapter() {
        // This runs inside the wrapper's tick, ahead of the batch dispatch. An escaping throwable would skip the
        // remaining channels AND that tick's batch dispatch, and the wrapper's contract guard would fault the whole
        // adapter into ERROR — far too much blast radius for a hiccup on one tag.
        final AtomicInteger reads = new AtomicInteger();
        final CapturingSender sender = new CapturingSender();
        final SouthboundWritePlane plane = planeOverCountingStores(reads, TAG, sender);
        plane.onMessage(new TagWritability(TAG, true));
        plane.onMessage(new TagWritability(OTHER, true));
        sender.pump(plane);
        sicken(plane, TAG);
        reads.set(0);

        assertThatCode(() -> {
                    for (int tick = 0; tick < SouthboundWriteQueue.POLL_TICKS; tick++) {
                        plane.onTick();
                    }
                })
                .doesNotThrowAnyException();

        sender.pump(plane);
        assertThat(reads.get()).isEqualTo(1); // only the healthy channel polled
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────────────────

    /** A plane over two stores that only count their reads; {@code sickTag}'s store can be made to refuse them. */
    private static @NotNull SouthboundWritePlane planeOverCountingStores(
            final @NotNull AtomicInteger reads, final @Nullable String sickTag, final @NotNull CapturingSender sender) {
        final SouthboundWriteBacklogFactory factory =
                (tagName, node, storeSender) -> new CountingStore(reads, tagName.equals(sickTag), tagName, storeSender);
        return new SouthboundWritePlane(
                "a1", sender, factory, List.of(pair(TAG), pair(OTHER)), Set.of(TAG, OTHER), metrics());
    }

    /** A store that never answers and only records the reads asked of it — or refuses them, once armed. */
    private static final class CountingStore implements SouthboundWriteBacklog {

        private final @NotNull AtomicInteger reads;
        private final boolean canSicken;
        private final @NotNull String tagName;
        private final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> sender;
        private boolean sick;

        private CountingStore(
                final @NotNull AtomicInteger reads,
                final boolean canSicken,
                final @NotNull String tagName,
                final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> sender) {
            this.reads = reads;
            this.canSicken = canSicken;
            this.tagName = tagName;
            this.sender = sender;
        }

        private void sicken() {
            sick = canSicken;
        }

        @Override
        public void requestRead(final long readToken) {
            if (sick) {
                throw new IllegalStateException("scripted read failure");
            }
            reads.incrementAndGet();
            // Answer empty: an unanswered read would leave the channel's read slot occupied, and no later poll
            // would ever issue another — which is precisely the wedge this design exists to make impossible.
            sender.tell(new SouthboundRead(tagName, readToken, null, null, null, null));
        }

        @Override
        public void requestSize(final long readToken) {}

        @Override
        public void delete(final @NotNull String commandId) {}

        @Override
        public void releaseMarkers() {}

        @Override
        public void close() {}
    }

    private static void sicken(final @NotNull SouthboundWritePlane plane, final @NotNull String tagName) {
        final SouthboundWritePlane.TagChannel channel = plane.channel(tagName);
        assertThat(channel).isNotNull();
        ((CountingStore) channel.backlog()).sicken();
    }

    private static int pending(final @NotNull SouthboundWritePlane.TagChannel channel) {
        return ((InMemorySouthboundWriteBacklog) channel.backlog()).pendingSize();
    }

    private static @NotNull NodeTagPair pair(final @NotNull String tagName) {
        return NodeTagPair.create(new TestNode(tagName), tagName, schema(), true, false);
    }

    private static @NotNull Schema schema() {
        return new ScalarSchema(ScalarType.STRING, null, null, null, null, false, true, false);
    }

    private static @NotNull DataPoint value(final int i) {
        return new TestDataPoint(TAG, i);
    }
}
