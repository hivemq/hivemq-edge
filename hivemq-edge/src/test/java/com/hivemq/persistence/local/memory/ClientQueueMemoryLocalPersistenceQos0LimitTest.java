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
package com.hivemq.persistence.local.memory;

import static com.hivemq.configuration.service.InternalConfigurations.PERSISTENCE_BUCKET_COUNT;
import static com.hivemq.configuration.service.MqttConfigurationService.QueuedMessagesStrategy;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.configuration.service.impl.InternalConfigurationServiceImpl;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * EDG-885: the QoS 0 branch of {@code add} never read the {@code max} it was handed, so a queue whose
 * publishers use QoS 0 was bounded only by the node-wide QoS 0 memory budget — 25 % of heap by default,
 * shared with every other QoS 0 consumer on the node.
 * <p>
 * Sampler queues opt in to the count bound because they are ring buffers of the most recent
 * {@code SamplingService.SAMPLE_SIZE} payloads. Everything else must keep the old behaviour: imposing a
 * count limit on QoS 0 elsewhere would silently shrink the outage buffer of every {@code persist=false}
 * bridge, which is a separate decision from this one.
 */
@SuppressWarnings("NullabilityAnnotations")
public class ClientQueueMemoryLocalPersistenceQos0LimitTest {

    private static final @NotNull String QUEUE_ID = "$SAMPLER::plant/line1";
    private static final int BUCKET = 0;
    private static final long MAX = 10;

    private final @NotNull PublishPayloadPersistence payloadPersistence = mock();
    private final @NotNull MessageDroppedService messageDroppedService = mock();
    private final @NotNull InternalConfigurationServiceImpl internalConfigurationService =
            new InternalConfigurationServiceImpl();

    private ClientQueueMemoryLocalPersistence persistence;

    /**
     * The node-wide QoS 0 settings as this class found them.
     * <p>
     * {@code InternalConfigurations} holds plain statics and Gradle runs many test classes in one JVM, so
     * what is set here outlives the class. Left set, the budget this class wants — the whole heap, and no
     * per-client limit — is precisely the regime in which QoS 0 drop behaviour can no longer be observed,
     * so every later class that constructs a queue persistence would quietly stop testing what it says it
     * tests (EDG-882 review v02, R2-15).
     * <p>
     * Captured once in {@code @BeforeAll} rather than on each test: a per-test capture reads whatever the
     * previous test left behind, so a single missed restore would be recorded as the value to restore
     * <em>to</em> and the pollution would become permanent instead of being corrected.
     */
    private static int originalQos0HardLimitDivisor;

    private static int originalQos0LimitPerClientBytes;

    @BeforeAll
    public static void captureTheNodeWideQos0Settings() {
        originalQos0HardLimitDivisor = InternalConfigurations.QOS_0_MEMORY_HARD_LIMIT_DIVISOR.get();
        originalQos0LimitPerClientBytes = InternalConfigurations.QOS_0_MEMORY_LIMIT_PER_CLIENT_BYTES.get();
    }

    @AfterEach
    public void restoreTheNodeWideQos0Settings() {
        InternalConfigurations.QOS_0_MEMORY_HARD_LIMIT_DIVISOR.set(originalQos0HardLimitDivisor);
        InternalConfigurations.QOS_0_MEMORY_LIMIT_PER_CLIENT_BYTES.set(originalQos0LimitPerClientBytes);
    }

    /**
     * The restore above cannot be asserted by any test in this class — what it protects is whatever runs
     * next in the same JVM. This is the closest thing to an oracle for it, and it is what fails if a
     * later change adds a mutation without a matching restore.
     */
    @AfterAll
    public static void theNodeWideQos0SettingsAreAsTheyWereFound() {
        assertEquals(
                originalQos0HardLimitDivisor,
                InternalConfigurations.QOS_0_MEMORY_HARD_LIMIT_DIVISOR.get(),
                "this class leaked its QoS 0 memory divisor into every test class that runs after it");
        assertEquals(
                originalQos0LimitPerClientBytes,
                InternalConfigurations.QOS_0_MEMORY_LIMIT_PER_CLIENT_BYTES.get(),
                "this class leaked its per-client QoS 0 limit into every test class that runs after it");
    }

    @BeforeEach
    public void setUp() {
        internalConfigurationService.set(PERSISTENCE_BUCKET_COUNT, "4");
        // a generous global QoS 0 budget, so that what this test observes is the count bound and never
        // the memory backstop. Undone by restoreTheNodeWideQos0Settings; these are process-global.
        InternalConfigurations.QOS_0_MEMORY_HARD_LIMIT_DIVISOR.set(1);
        InternalConfigurations.QOS_0_MEMORY_LIMIT_PER_CLIENT_BYTES.set(Integer.MAX_VALUE);
        persistence = new ClientQueueMemoryLocalPersistence(
                payloadPersistence, messageDroppedService, new MetricRegistry(), internalConfigurationService);
    }

    private static @NotNull PUBLISH qos0(final @NotNull String payload) {
        return new PUBLISHFactory.Mqtt5Builder()
                .withQoS(QoS.AT_MOST_ONCE)
                .withOnwardQos(QoS.AT_MOST_ONCE)
                .withPublishId(1L)
                .withPayload(payload.getBytes(UTF_8))
                .withTopic("plant/line1")
                .withHivemqId("hivemqId")
                .build();
    }

    private void add(final @NotNull String payload, final boolean applyMaxToQos0) {
        add(payload, MAX, applyMaxToQos0);
    }

    private void add(final @NotNull String payload, final long max, final boolean applyMaxToQos0) {
        persistence.add(
                QUEUE_ID,
                true,
                qos0(payload),
                max,
                QueuedMessagesStrategy.DISCARD_OLDEST,
                false,
                applyMaxToQos0,
                BUCKET);
    }

    private @NotNull List<String> payloadsInQueue() {
        final List<String> payloads = new ArrayList<>();
        for (final PUBLISH publish : persistence.peek(QUEUE_ID, true, Long.MAX_VALUE, Integer.MAX_VALUE, BUCKET)) {
            payloads.add(new String(publish.getPayload(), UTF_8));
        }
        return payloads;
    }

    @Test
    public void test_whenOptedIn_thenTheQueueNeverExceedsTheLimit() {
        for (int i = 0; i < 100; i++) {
            add("m" + i, true);
        }

        assertEquals(MAX, persistence.size(QUEUE_ID, true, BUCKET), "queue must be capped at max");
    }

    /** Drop-oldest, not drop-newest: a sample is only useful if it is recent. */
    @Test
    public void test_whenOptedIn_thenTheOldestAreDroppedAndTheNewestKept() {
        for (int i = 0; i < 25; i++) {
            add("m" + i, true);
        }

        final List<String> expected = new ArrayList<>();
        for (int i = 15; i < 25; i++) {
            expected.add("m" + i);
        }
        assertEquals(expected, payloadsInQueue(), "the ring must hold the most recent MAX payloads, in order");
    }

    /**
     * EDG-882 QA round 1: rotation is not a drop. Reporting it through the dropped-message service put
     * every sample a UI topic preview replaces on the node-wide dropped-message counter and wrote an
     * event.log line per publish — so opening a topic in the UI made the node look like it was losing
     * customer messages, and the one metric an operator watches for real loss became useless.
     */
    @Test
    public void test_whenOptedIn_thenRotationIsNotReportedAsADroppedMessage() {
        for (int i = 0; i < 25; i++) {
            add("m" + i, true);
        }

        verifyNoInteractions(messageDroppedService);
    }

    /** The control: a real drop is still reported. */
    @Test
    public void test_whenNotOptedIn_andTheQos1LimitBites_thenTheDropIsStillReported() {
        for (int i = 0; i < 25; i++) {
            persistence.add(QUEUE_ID, true, qos1("m" + i), 5L, QueuedMessagesStrategy.DISCARD, false, false, BUCKET);
        }

        verify(messageDroppedService, atLeastOnce()).queueFullShared(eq(QUEUE_ID), anyString(), anyInt());
    }

    private static @NotNull PUBLISH qos1(final @NotNull String payload) {
        return new PUBLISHFactory.Mqtt5Builder()
                .withQoS(QoS.AT_LEAST_ONCE)
                .withOnwardQos(QoS.AT_LEAST_ONCE)
                .withPublishId(1L)
                .withPayload(payload.getBytes(UTF_8))
                .withTopic("plant/line1")
                .withHivemqId("hivemqId")
                .build();
    }

    @Test
    public void test_whenOptedIn_andBelowTheLimit_thenNothingIsDropped() {
        for (int i = 0; i < 3; i++) {
            add("m" + i, true);
        }

        assertEquals(3, persistence.size(QUEUE_ID, true, BUCKET));
        assertEquals(List.of("m0", "m1", "m2"), payloadsInQueue());
    }

    @Test
    public void test_whenOptedIn_andExactlyAtTheLimit_thenNothingIsDropped() {
        for (int i = 0; i < MAX; i++) {
            add("m" + i, true);
        }

        assertEquals(MAX, persistence.size(QUEUE_ID, true, BUCKET));
        assertEquals("m0", payloadsInQueue().get(0), "the first message is still present at exactly max");
    }

    /**
     * The guard that keeps this change contained. Every other QoS 0 queue — ordinary client queues, and
     * the {@code persist=false} bridges deliberately downgraded to QoS 0 — must be untouched.
     */
    @Test
    public void test_whenNotOptedIn_thenTheCountLimitIsIgnoredExactlyAsBefore() {
        for (int i = 0; i < 100; i++) {
            add("m" + i, false);
        }

        assertEquals(100, persistence.size(QUEUE_ID, true, BUCKET), "QoS 0 must stay memory-bounded only");
    }

    /** Trimming must keep the queue usable indefinitely, not merely bounded once. */
    @Test
    public void test_whenOptedIn_thenTheRingKeepsRotatingOverManyCycles() {
        for (int i = 0; i < 10_000; i++) {
            add("m" + i, true);
        }

        assertEquals(MAX, persistence.size(QUEUE_ID, true, BUCKET));
        assertEquals("m9999", payloadsInQueue().get((int) MAX - 1), "the most recent payload must survive");
    }

    /**
     * A limit of one is the tightest ring and the likeliest off-by-one: only the newest may remain.
     */
    @Test
    public void test_aLimitOfOneKeepsOnlyTheNewest() {
        add("first", 1, true);
        add("second", 1, true);
        add("third", 1, true);

        assertEquals(1, persistence.size(QUEUE_ID, true, BUCKET));
        assertEquals(List.of("third"), payloadsInQueue());
    }

    /** A non-positive limit disables the bound rather than trimming forever or emptying the queue. */
    @Test
    public void test_aNonPositiveLimitDisablesTheBound() {
        add("only", 0, true);
        add("second", 0, true);

        assertEquals(2, persistence.size(QUEUE_ID, true, BUCKET));
    }

    /**
     * EDG-882 F-03, mirrored from the file-native side, where the same scenario emptied the ring.
     * <p>
     * The node sits at its global QoS 0 ceiling — the budget is node-wide and shared with every other
     * QoS 0 consumer, so this is the ordinary state of a busy node. The trim evicts the old sample and
     * releases its memory, and that release is precisely the room the new one needs, so the ring must
     * rotate rather than empty itself. This implementation reads the counter after the trim and gets it
     * right; the assertion is here so that it cannot quietly stop doing so, and so that the two
     * implementations of one contract are held to the same case.
     * <p>
     * Sized so that the sampler's own sample carries the node over the line, because that is the only
     * case the trim can rescue: unrelated traffic that exceeds the budget on its own is over it before
     * and after, and is meant to be rejected.
     */
    @Test
    public void test_atTheGlobalCeiling_theRingStillRotates() {
        withGlobalBudgetOfAbout(8 * 1024);

        // unrelated QoS 0 traffic, comfortably inside the budget on its own
        persistence.add(
                "some/other/consumer",
                true,
                qos0("x".repeat(512)),
                Long.MAX_VALUE,
                QueuedMessagesStrategy.DISCARD_OLDEST,
                false,
                false,
                BUCKET);
        // and the sample that takes the node over it
        add("x".repeat(32 * 1024), 1, true);
        assertEquals(1, persistence.size(QUEUE_ID, true, BUCKET), "the ring must start with the sample it rotates out");

        add("new", 1, true);

        assertEquals(1, persistence.size(QUEUE_ID, true, BUCKET), "the ring emptied itself instead of rotating");
        assertEquals(List.of("new"), payloadsInQueue(), "the newest sample must be the one that survived");
    }

    /**
     * The ceiling still has to mean something: a queue that does no trimming frees nothing, and once
     * the node is over the budget its messages must be dropped exactly as before.
     */
    @Test
    public void test_atTheGlobalCeiling_aQueueThatFreesNothingIsStillBounded() {
        withGlobalBudgetOfAbout(8 * 1024);

        persistence.add(
                "some/other/consumer",
                true,
                qos0("x".repeat(32 * 1024)),
                Long.MAX_VALUE,
                QueuedMessagesStrategy.DISCARD_OLDEST,
                false,
                false,
                BUCKET);
        final int sizeAtCeiling = persistence.size("some/other/consumer", true, BUCKET);

        persistence.add(
                "some/other/consumer",
                true,
                qos0("rejected"),
                Long.MAX_VALUE,
                QueuedMessagesStrategy.DISCARD_OLDEST,
                false,
                false,
                BUCKET);

        assertEquals(
                sizeAtCeiling,
                persistence.size("some/other/consumer", true, BUCKET),
                "the node-wide QoS 0 budget must still reject what it cannot hold");
    }

    /**
     * Rebuilds the persistence with a node-wide QoS 0 budget of roughly {@code bytes}. Derived from the
     * heap rather than fixed, because the limit is {@code maxHeap / divisor} and the heap differs
     * between a laptop and a CI agent; the tests using it work with messages an order of magnitude
     * either side of the budget, so they never depend on the exact per-message overhead.
     */
    private void withGlobalBudgetOfAbout(final int bytes) {
        InternalConfigurations.QOS_0_MEMORY_HARD_LIMIT_DIVISOR.set(
                (int) Math.max(1, Runtime.getRuntime().maxMemory() / bytes));
        persistence = new ClientQueueMemoryLocalPersistence(
                payloadPersistence, messageDroppedService, new MetricRegistry(), internalConfigurationService);
    }

    /**
     * QoS 1 traffic on an opted-in queue must still take the ordinary path. Sampling subscribes at
     * {@code AT_LEAST_ONCE}, so a QoS 1 publisher produces QoS 1 samples, and those were never the
     * problem — they were always count-bounded.
     */
    @Test
    public void test_qos1MessagesOnAnOptedInQueueAreStillBoundedAsBefore() {
        for (int i = 0; i < 25; i++) {
            persistence.add(
                    QUEUE_ID,
                    true,
                    new PUBLISHFactory.Mqtt5Builder()
                            .withQoS(QoS.AT_LEAST_ONCE)
                            .withOnwardQos(QoS.AT_LEAST_ONCE)
                            .withPublishId(1L)
                            .withPayload(("q" + i).getBytes(UTF_8))
                            .withTopic("plant/line1")
                            .withHivemqId("hivemqId")
                            .withPersistence(payloadPersistence)
                            .build(),
                    MAX,
                    QueuedMessagesStrategy.DISCARD_OLDEST,
                    false,
                    true,
                    BUCKET);
        }

        assertEquals(MAX, persistence.size(QUEUE_ID, true, BUCKET));
    }

    /**
     * The case {@code test_atTheGlobalCeiling_theRingStillRotates} does not cover: the node is over the
     * QoS 0 budget because of traffic that belongs to <em>another</em> queue, so the space the ring
     * frees by rotating cannot bring it back under.
     * <p>
     * Trimming first and checking the guards afterwards destroyed the sample the ring held and then
     * rejected its replacement against pressure the ring never caused — leaving it empty, and leaving it
     * empty again on every sample that followed, because each one repeated the trade. The diagnostic the
     * bound exists to protect showed nothing at all (EDG-882 review v03, R3-05).
     * <p>
     * The oracle is the surviving payload, not the size: a ring that rejected the incoming sample
     * <em>and</em> kept the old one is the whole point, and a size assertion alone would pass on a ring
     * that had swapped one for the other.
     */
    @Test
    public void test_atTheGlobalCeiling_unrelatedPressureDoesNotEmptyTheRing() {
        withGlobalBudgetOfAbout(8 * 1024);

        add("old", 1, true);
        assertEquals(List.of("old"), payloadsInQueue(), "the ring must start holding the sample under test");

        // pressure that belongs to somebody else, and that the ring cannot free by rotating
        persistence.add(
                "some/other/consumer",
                true,
                qos0("x".repeat(32 * 1024)),
                Long.MAX_VALUE,
                QueuedMessagesStrategy.DISCARD_OLDEST,
                false,
                false,
                BUCKET);

        add("new", 1, true);

        assertEquals(
                List.of("old"),
                payloadsInQueue(),
                "the ring gave up the sample it held for a replacement that was then rejected");
        assertEquals(1, persistence.size(QUEUE_ID, true, BUCKET), "the ring must not have emptied itself");
    }

    /**
     * And it must not empty itself over many attempts either. One rejected sample leaving the ring intact
     * is the fix; the defect's real shape was that every sample after the first repeated it, so a ring
     * that survives one attempt but erodes over ten would still be broken.
     */
    @Test
    public void test_atTheGlobalCeiling_unrelatedPressureLeavesTheRingIntactOverManyAttempts() {
        withGlobalBudgetOfAbout(8 * 1024);

        add("old", 1, true);
        persistence.add(
                "some/other/consumer",
                true,
                qos0("x".repeat(32 * 1024)),
                Long.MAX_VALUE,
                QueuedMessagesStrategy.DISCARD_OLDEST,
                false,
                false,
                BUCKET);

        for (int i = 0; i < 10; i++) {
            add("rejected-" + i, 1, true);
        }

        assertEquals(List.of("old"), payloadsInQueue(), "the ring eroded under repeated rejected samples");
    }

    /**
     * The rejection is still a rejection: refusing to destroy what the ring holds must not also stop the
     * node reporting that it could not accept the message.
     */
    @Test
    public void test_atTheGlobalCeiling_unrelatedPressureStillReportsTheDrop() {
        withGlobalBudgetOfAbout(8 * 1024);

        add("old", 1, true);
        persistence.add(
                "some/other/consumer",
                true,
                qos0("x".repeat(32 * 1024)),
                Long.MAX_VALUE,
                QueuedMessagesStrategy.DISCARD_OLDEST,
                false,
                false,
                BUCKET);

        add("new", 1, true);

        verify(messageDroppedService, atLeastOnce())
                .qos0MemoryExceededShared(eq(QUEUE_ID), anyString(), anyInt(), anyLong(), anyLong());
    }
}
