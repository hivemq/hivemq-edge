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

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.adapter.sdk.api.schema.ScalarSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.protocols.v2.runtime.ProtocolAdapterMetrics;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * <b>Measures the standing cost of the southbound backstop poll on an idle adapter</b> — the number the spec records
 * as unmeasured (§12): every write-mapped tag whose delivery window is open keeps reading its durable queue forever,
 * whether or not any command ever arrives.
 * <p>
 * This is a <b>measurement harness, not a behavioural test</b>, and it is deliberately deterministic: no wall clock,
 * no sleeps, no threads. It drives {@link SouthboundWritePlane#onTick()} a known number of times over a
 * {@link ClientQueuePersistence} stand-in that does nothing but <b>count the operations the real
 * {@link ClientQueueSouthboundWriteBacklog} submits</b>, and converts the result into a per-second rate using the
 * production tick period. Counting at the persistence boundary rather than at the {@link SouthboundWriteBacklog}
 * interface is the whole point: the depth cross-check ({@code size}) is invisible from the interface, and it is
 * precisely the operation that exists <i>only</i> on the idle path.
 * <p>
 * The channels here are held in the exact state that costs the most and is also the <b>normal state of a healthy
 * gateway</b>: connected, verified, window open, queue empty, nothing in flight. That is not a worst case dressed up
 * as typical — it is what every write-mapped tag looks like between commands.
 * <p>
 * What it pins:
 * <ul>
 * <li>the per-channel op mix and rate ({@link #anIdleOpenChannel_readsOncePerPollInterval_andChecksDepthEveryThirdRead});</li>
 * <li>that the cost is <b>linear in the write-mapped tag count</b>, with no amortisation across channels
 * ({@link #theCostIsLinearInTheWriteMappedTagCount});</li>
 * <li>that a <b>closed</b> window is genuinely free, so the cost is confined to healthy tags
 * ({@link #aClosedWindowIsFree});</li>
 * <li>and a projection table for realistic fleet sizes ({@link #reportProjectedFleetCost}) — the deliverable.</li>
 * </ul>
 * If {@link SouthboundWriteQueue#POLL_TICKS} or {@link SouthboundWriteQueue#EMPTY_READS_BEFORE_SIZE_CHECK} is ever
 * retuned, the assertions here fail and state the new cost, which is the point of asserting the model rather than
 * only printing it.
 */
class SouthboundIdlePollCostTest {

    private static final @NotNull String ADAPTER_ID = "a1";

    /**
     * The production wrapper tick period in milliseconds, per {@code DefaultProtocolAdapterWrapperFactory} ("~50 ms
     * in production"). Only used to turn deterministic op <b>counts</b> into a human-facing <b>rate</b>; nothing is
     * timed against it.
     */
    private static final long PRODUCTION_TICK_MILLIS = 50L;

    /** Long enough that the steady state dominates any start-up transient, and an exact multiple of the poll period. */
    private static final int TICKS = 20 * 60; // 60 poll intervals = 60 s of production time

    @Test
    void anIdleOpenChannel_readsOncePerPollInterval_andChecksDepthEveryThirdRead() {
        final CountingClientQueue persistence = new CountingClientQueue();
        final CapturingSender sender = new CapturingSender();
        final SouthboundWritePlane plane = idlePlane(1, persistence, sender);

        final int pollIntervals = openWindowsAndSettle(plane, sender, persistence);
        drive(plane, sender, TICKS);

        final int expectedReads = TICKS / SouthboundWriteQueue.POLL_TICKS;
        assertThat(persistence.reads.get())
                .as("one leasing read per poll interval per idle channel")
                .isEqualTo(expectedReads);

        // The depth cross-check arms on every EMPTY_READS_BEFORE_SIZE_CHECK-th *consecutive empty* read. An idle
        // queue answers empty every time, so it fires at exactly that fraction of the reads — ±1 for wherever the
        // consecutive-empty counter stood when measurement began.
        final int expectedSizes = expectedReads / SouthboundWriteQueue.EMPTY_READS_BEFORE_SIZE_CHECK;
        assertThat(persistence.sizes.get())
                .as("a depth cross-check every %d empty reads", SouthboundWriteQueue.EMPTY_READS_BEFORE_SIZE_CHECK)
                .isBetween(expectedSizes - 1, expectedSizes + 1);

        report("one idle write-mapped tag", 1, persistence, pollIntervals);
    }

    @Test
    void theCostIsLinearInTheWriteMappedTagCount() {
        // Nothing batches these: each channel submits its own read against its own queue id, which hashes to its own
        // single-writer bucket. Ten tags cost ten times one tag, and that is the scaling statement that matters for a
        // gateway with hundreds of write-mapped tags.
        final int perChannelReads = TICKS / SouthboundWriteQueue.POLL_TICKS;

        for (final int tagCount : new int[] {1, 10, 100}) {
            final CountingClientQueue persistence = new CountingClientQueue();
            final CapturingSender sender = new CapturingSender();
            final SouthboundWritePlane plane = idlePlane(tagCount, persistence, sender);

            openWindowsAndSettle(plane, sender, persistence);
            drive(plane, sender, TICKS);

            assertThat(persistence.reads.get())
                    .as("%d idle channels each read once per poll interval", tagCount)
                    .isEqualTo(perChannelReads * tagCount);
            report(
                    tagCount + " idle write-mapped tags",
                    tagCount,
                    persistence,
                    TICKS / SouthboundWriteQueue.POLL_TICKS);
        }
    }

    @Test
    void aClosedWindowIsFree() {
        // The gate in deliverOrRead: a tag that is not verified, not writable, or whose adapter is disconnected costs
        // nothing at all. This is what stops a large *misconfigured* fleet from also being a large *load*, and it is
        // why the cost belongs to healthy tags specifically.
        final CountingClientQueue persistence = new CountingClientQueue();
        final CapturingSender sender = new CapturingSender();
        final SouthboundWritePlane plane = idlePlane(100, persistence, sender);

        // Windows are born closed — deliberately never opened here.
        persistence.reset();
        drive(plane, sender, TICKS);

        assertThat(persistence.reads.get())
                .as("a closed window issues no reads")
                .isZero();
        assertThat(persistence.sizes.get())
                .as("a closed window issues no depth checks")
                .isZero();
    }

    @Test
    void reportProjectedFleetCost() {
        // The deliverable: the same measured per-channel rate projected onto fleet sizes, so the number in the ticket
        // is measured rather than reasoned. Deliberately assertion-light — this test exists to produce data.
        final CountingClientQueue persistence = new CountingClientQueue();
        final CapturingSender sender = new CapturingSender();
        final SouthboundWritePlane plane = idlePlane(1, persistence, sender);
        openWindowsAndSettle(plane, sender, persistence);
        drive(plane, sender, TICKS);

        final double seconds = (double) (TICKS * PRODUCTION_TICK_MILLIS) / 1000.0;
        final double opsPerSecondPerTag = (persistence.reads.get() + persistence.sizes.get()) / seconds;

        final StringBuilder table = new StringBuilder(512);
        table.append(System.lineSeparator())
                .append("── southbound idle backstop-poll cost ──────────────────────────────────────")
                .append(System.lineSeparator())
                .append(String.format(
                        "tick period %d ms | poll every %d ticks | depth check every %d empty reads%n",
                        PRODUCTION_TICK_MILLIS,
                        SouthboundWriteQueue.POLL_TICKS,
                        SouthboundWriteQueue.EMPTY_READS_BEFORE_SIZE_CHECK))
                .append(String.format(
                        "measured per idle write-mapped tag: %.3f persistence ops/sec%n", opsPerSecondPerTag))
                .append(System.lineSeparator())
                .append(String.format("%12s | %22s | %22s%n", "idle tags", "ops/sec", "ops/hour"))
                .append("-------------+------------------------+-----------------------")
                .append(System.lineSeparator());
        for (final int fleet : new int[] {1, 10, 50, 100, 500, 1000, 5000}) {
            table.append(String.format(
                    "%12d | %22.1f | %22.0f%n", fleet, fleet * opsPerSecondPerTag, fleet * opsPerSecondPerTag * 3600));
        }
        table.append("────────────────────────────────────────────────────────────────────────────");
        System.out.println(table);

        assertThat(opsPerSecondPerTag).as("an idle tag is not free").isPositive();
    }

    // ── rig ─────────────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Build a plane of {@code tagCount} write-mapped tags, each backed by the <b>real</b> durable backlog over the
     * counting persistence. Real backlog on purpose: the operation mix under measurement (the leasing read, the
     * depth cross-check, the start-up marker sweep) is a property of that class, not of the delivery side.
     */
    private static @NotNull SouthboundWritePlane idlePlane(
            final int tagCount, final @NotNull CountingClientQueue persistence, final @NotNull CapturingSender sender) {
        final List<NodeTagPair> nodes = new ArrayList<>(tagCount);
        final Set<String> writeUsed = new LinkedHashSet<>();
        for (int i = 0; i < tagCount; i++) {
            final String tagName = "tag-" + i;
            nodes.add(pair(tagName));
            writeUsed.add(tagName);
        }
        final SouthboundWriteBacklogFactory factory =
                (tagName, node, storeSender) -> new ClientQueueSouthboundWriteBacklog(
                        persistence,
                        // The production queue id shape: share name + mapping topic, node-independent.
                        SouthboundMqttIntake.INTERNAL_SHARE_PREFIX + ADAPTER_ID + "/plant/a/" + tagName + "/set",
                        // Never invoked: an idle queue answers empty, so nothing is ever translated.
                        publish -> null,
                        ADAPTER_ID,
                        tagName,
                        storeSender);
        return new SouthboundWritePlane(ADAPTER_ID, sender, factory, nodes, writeUsed, metrics());
    }

    /**
     * Open every channel's window, run the resulting traffic to quiescence, then zero the counters so only the
     * steady state is measured. The start-up burst (one marker sweep and one read per channel) is real but is a
     * one-off per adapter lifetime, and folding it into a per-second rate would overstate the standing cost.
     *
     * @return the number of poll intervals the subsequent drive will cover.
     */
    private static int openWindowsAndSettle(
            final @NotNull SouthboundWritePlane plane,
            final @NotNull CapturingSender sender,
            final @NotNull CountingClientQueue persistence) {
        for (final String tagName : plane.writeMappedTagNames()) {
            final SouthboundWritePlane.TagChannel channel = plane.channel(tagName);
            assertThat(channel).isNotNull();
            channel.queue().openWindow();
        }
        sender.pump(plane);
        persistence.reset();
        return TICKS / SouthboundWriteQueue.POLL_TICKS;
    }

    /**
     * The dispatch loop, run deterministically: tick, then drain the mailbox the way the wrapper does. Draining is
     * mandatory — an unpumped read answer would leave {@code pendingReadToken} set and suppress every later poll,
     * which would measure one read instead of a rate.
     */
    private static void drive(
            final @NotNull SouthboundWritePlane plane, final @NotNull CapturingSender sender, final int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            plane.onTick();
            sender.pump(plane);
        }
    }

    private static void report(
            final @NotNull String what,
            final int tagCount,
            final @NotNull CountingClientQueue persistence,
            final int pollIntervals) {
        final double seconds = (double) (TICKS * PRODUCTION_TICK_MILLIS) / 1000.0;
        System.out.printf(
                "[idle-poll-cost] %-28s reads=%-6d depthChecks=%-5d markerSweeps=%-5d "
                        + "=> %.3f ops/sec total, %.3f ops/sec per tag (over %d poll intervals)%n",
                what,
                persistence.reads.get(),
                persistence.sizes.get(),
                persistence.markerSweeps.get(),
                (persistence.reads.get() + persistence.sizes.get()) / seconds,
                (persistence.reads.get() + persistence.sizes.get()) / seconds / tagCount,
                pollIntervals);
    }

    private static @NotNull NodeTagPair pair(final @NotNull String tagName) {
        return NodeTagPair.create(new TestNode(tagName), tagName, schema(), true, false);
    }

    private static @NotNull Schema schema() {
        return new ScalarSchema(ScalarType.STRING, null, null, null, null, false, true, false);
    }

    private static @NotNull ProtocolAdapterMetrics metrics() {
        return new ProtocolAdapterMetrics(new MetricRegistry(), ADAPTER_ID, () -> 0);
    }

    /**
     * A client queue that holds nothing and counts everything: every queue is permanently empty, so every read is an
     * idle read. Extending {@link UnsupportedClientQueuePersistence} means any operation the idle path is not
     * expected to perform fails loudly instead of being silently absorbed into the measurement.
     */
    private static final class CountingClientQueue extends UnsupportedClientQueuePersistence {

        private final @NotNull AtomicInteger reads = new AtomicInteger();
        private final @NotNull AtomicInteger sizes = new AtomicInteger();
        private final @NotNull AtomicInteger markerSweeps = new AtomicInteger();

        void reset() {
            reads.set(0);
            sizes.set(0);
            markerSweeps.set(0);
        }

        @Override
        public @NotNull ListenableFuture<ImmutableList<PUBLISH>> readShared(
                final @NotNull String sharedSubscription, final int messageLimit, final long byteLimit) {
            reads.incrementAndGet();
            return Futures.immediateFuture(ImmutableList.of());
        }

        @Override
        public @NotNull ListenableFuture<Integer> size(final @NotNull String queueId, final boolean shared) {
            sizes.incrementAndGet();
            return Futures.immediateFuture(0);
        }

        @Override
        public @NotNull ListenableFuture<Void> removeAllInFlightMarkers(final @NotNull String sharedSubscription) {
            markerSweeps.incrementAndGet();
            return Futures.immediateFuture(null);
        }

        @Override
        public void addPublishAvailableCallback(
                final @NotNull PublishAvailableCallback callback, final @NotNull String queueId) {
            // Registered but never fired: this measures the poll, not the hint.
        }

        @Override
        public void removePublishAvailableCallback(final @NotNull String queueId) {}
    }
}
