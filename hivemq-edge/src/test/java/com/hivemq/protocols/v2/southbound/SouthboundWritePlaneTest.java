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

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.schema.ScalarSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.protocols.v2.tag.SouthboundWriteOutcome;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * The {@link SouthboundWritePlane} in isolation (a capturing sender stands in for the wrapper): one suspended
 * channel per write-mapped tag, opened and closed by the readiness notifications; commands offered before the tag
 * is writable wait in the backlog; a tags-only update drops gone channels, keeps a surviving tag's backlog, and
 * replaces a channel whose node changed.
 */
class SouthboundWritePlaneTest {

    private static final @NotNull String TAG = "setpoint";
    private static final @NotNull String OTHER = "ramp-rate";

    @Test
    void channelsExistOnlyForWriteMappedTags_andStartSuspended() {
        final CapturingSender sender = new CapturingSender();
        final SouthboundWritePlane plane =
                new SouthboundWritePlane("a1", sender, 10, List.of(pair(TAG), pair(OTHER)), Set.of(TAG));

        assertThat(plane.writeMappedTagNames()).containsExactly(TAG);
        assertThat(plane.offer(OTHER, value(1))).isFalse(); // not write-mapped — no channel

        // The channel starts suspended: the offer waits in the backlog, nothing reaches the adapter.
        assertThat(plane.offer(TAG, value(1))).isTrue();
        assertThat(sender.requests).isEmpty();
        final SouthboundWritePlane.TagChannel channel = plane.channel(TAG);
        assertThat(channel).isNotNull();
        assertThat(channel.queue().suspended()).isTrue();
        assertThat(pending(channel)).isEqualTo(1);
    }

    @Test
    void tagWritableOpensTheWindow_tagUnwritableClosesIt() {
        final CapturingSender sender = new CapturingSender();
        final SouthboundWritePlane plane = new SouthboundWritePlane("a1", sender, 10, List.of(pair(TAG)), Set.of(TAG));
        plane.offer(TAG, value(1));
        plane.offer(TAG, value(2));

        // The tag verified: the window opens and the head is delivered — exactly one, single-in-flight.
        plane.tagWritable(TAG);
        assertThat(sender.requests).hasSize(1);

        // The tag became unwritable (disconnect/deactivation): the window closes; the in-flight write is aborted by
        // the aspect in production — here the test settles it — and the command stays at the head.
        plane.tagUnwritable(TAG);
        sender.settleLast(SouthboundWriteOutcome.ABORTED);
        assertThat(sender.requests).hasSize(1); // suspended: nothing redelivered

        // Writable again: the SAME command is redelivered.
        plane.tagWritable(TAG);
        assertThat(sender.requests).hasSize(2);
        assertThat(sender.requests.get(1).value())
                .isEqualTo(sender.requests.getFirst().value());

        // Notifications for unknown tags are ignored.
        plane.tagWritable("unknown");
        plane.tagUnwritable("unknown");
        assertThat(sender.requests).hasSize(2);
    }

    @Test
    void updateTagSet_dropsGoneChannels_keepsSurvivingBacklogs_replacesChangedNodes() {
        final CapturingSender sender = new CapturingSender();
        final NodeTagPair survivor = pair(TAG);
        final SouthboundWritePlane plane =
                new SouthboundWritePlane("a1", sender, 10, List.of(survivor, pair(OTHER)), Set.of(TAG, OTHER));
        plane.offer(TAG, value(1));
        plane.offer(OTHER, value(2));
        plane.tagWritable(TAG); // TAG's window is open with one in flight

        // Reload: OTHER is no longer write-mapped, TAG survives with the same node, "fresh" appears.
        plane.updateTagSet(List.of(survivor, pair("fresh")), Set.of(TAG, "fresh"));

        assertThat(plane.writeMappedTagNames()).containsExactlyInAnyOrder(TAG, "fresh");
        assertThat(plane.channel(OTHER)).isNull(); // dropped, its pending command with it

        // The survivor kept its backlog (the pending command rode out the reload) and was re-suspended: the rebuilt
        // aspect re-verifies from scratch and its tagWritable reopens the window.
        final SouthboundWritePlane.TagChannel survived = plane.channel(TAG);
        assertThat(survived).isNotNull();
        assertThat(pending(survived)).isEqualTo(1);
        assertThat(survived.queue().suspended()).isTrue();

        // The new tag's channel exists and is suspended, holding nothing yet.
        final SouthboundWritePlane.TagChannel fresh = plane.channel("fresh");
        assertThat(fresh).isNotNull();
        assertThat(fresh.queue().suspended()).isTrue();

        // A node change replaces the channel: pending commands aimed at the old node are not delivered to the new.
        plane.offer(TAG, value(3));
        final NodeTagPair movedTag = NodeTagPair.create(new TestNode("moved"), TAG, schema(), true, false);
        plane.updateTagSet(List.of(movedTag), Set.of(TAG));
        final SouthboundWritePlane.TagChannel replaced = plane.channel(TAG);
        assertThat(replaced).isNotNull();
        assertThat(replaced.node().nodeId()).isEqualTo("moved");
        assertThat(pending(replaced)).isZero();
    }

    @Test
    void closeSuspendsAndDropsEverything() {
        final CapturingSender sender = new CapturingSender();
        final SouthboundWritePlane plane = new SouthboundWritePlane("a1", sender, 10, List.of(pair(TAG)), Set.of(TAG));
        plane.offer(TAG, value(1));

        plane.close();

        assertThat(plane.writeMappedTagNames()).isEmpty();
        assertThat(plane.offer(TAG, value(2))).isFalse();
    }

    @Test
    void rearmBacklogs_reachesEveryChannel() {
        // The tick hook behind QA finding N1: the wrapper's tick → plane → every channel's backlog. The plane
        // must fan the poke out to each write-mapped tag's backlog, and only to those.
        final AtomicInteger rearms = new AtomicInteger();
        final SouthboundWritePlane plane = planeOverCountingBacklogs(rearms, null);

        plane.rearmBacklogs();
        assertThat(rearms.get()).isEqualTo(2);

        plane.close();
        plane.rearmBacklogs(); // no channels, no pokes
        assertThat(rearms.get()).isEqualTo(2);
    }

    @Test
    void rearmBacklogs_isGuardedPerChannel_soOneSickBacklogCannotFaultTheAdapter() {
        // This runs on the wrapper's dispatch thread inside the tick, ahead of the batch dispatch. An escaping
        // throwable would skip the remaining channels AND that tick's batch dispatch, and the wrapper's contract
        // guard would fault the whole adapter into ERROR — far too much blast radius for a hiccup on one tag's
        // recovery path.
        final AtomicInteger rearms = new AtomicInteger();
        final SouthboundWritePlane plane = planeOverCountingBacklogs(rearms, TAG);

        assertThatCode(plane::rearmBacklogs).doesNotThrowAnyException();

        // The throwing channel forfeits only its own re-arm; the healthy one still got its poke.
        assertThat(rearms.get()).isEqualTo(1);
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * A plane over two channels whose backlogs only count their re-arms — except the one named by
     * {@code throwingTag}, which throws instead.
     */
    private static @NotNull SouthboundWritePlane planeOverCountingBacklogs(
            final @NotNull AtomicInteger rearms, final @Nullable String throwingTag) {
        final SouthboundWriteBacklogFactory factory =
                (tagName, node) -> new CountingBacklog(rearms, tagName.equals(throwingTag));
        return new SouthboundWritePlane(
                "a1", new CapturingSender(), factory, List.of(pair(TAG), pair(OTHER)), Set.of(TAG, OTHER));
    }

    /** A backlog that holds nothing and only records (or refuses) its tick re-arms. */
    private record CountingBacklog(@NotNull AtomicInteger rearms, boolean throwOnRearm)
            implements SouthboundWriteBacklog {

        @Override
        public @Nullable SouthboundCommand head() {
            return null;
        }

        @Override
        public void removeHead(final @NotNull String id) {}

        @Override
        public void deadLetterHead(final @NotNull String id, final @NotNull String reason) {}

        @Override
        public void onAvailable(final @NotNull Runnable wakeup) {}

        @Override
        public void rearmIfRequested() {
            if (throwOnRearm) {
                throw new IllegalStateException("scripted re-arm failure");
            }
            rearms.incrementAndGet();
        }

        @Override
        public void close() {}
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
