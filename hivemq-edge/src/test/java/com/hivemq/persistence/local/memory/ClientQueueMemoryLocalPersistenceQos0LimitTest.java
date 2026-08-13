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
import static org.mockito.Mockito.mock;

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

    @BeforeEach
    public void setUp() {
        internalConfigurationService.set(PERSISTENCE_BUCKET_COUNT, "4");
        // a generous global QoS 0 budget, so that what this test observes is the count bound and never
        // the memory backstop
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
}
