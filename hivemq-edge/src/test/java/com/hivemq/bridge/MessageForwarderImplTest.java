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
package com.hivemq.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.HivemqId;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.SingleWriterService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class MessageForwarderImplTest {

    // EDG-882: the Base64 subscription hash may contain '/'
    private static final String FORWARDER_ID = "bridge-Bt80p78iNo/w7W1W7bGwcg==";
    private static final String TOPIC = "miele/v1/production/sapdm/dev/+/+/from-plc-to-dm";
    private static final String QUEUE_ID = MessageForwarderImpl.FORWARDER_PREFIX + FORWARDER_ID + "/" + TOPIC;

    // a second bridge whose hash also carries a '/', to pin cross-forwarder isolation
    private static final String OTHER_FORWARDER_ID = "other-LHHqscdwUV/C1PCBYfZ6Bg==";
    private static final String OTHER_TOPIC = "other/topic/+";
    private static final String OTHER_QUEUE_ID =
            MessageForwarderImpl.FORWARDER_PREFIX + OTHER_FORWARDER_ID + "/" + OTHER_TOPIC;

    private MessageForwarderImpl messageForwarder;
    private MqttForwarder mqttForwarder;
    private MqttForwarder otherMqttForwarder;

    @BeforeEach
    public void setUp() {
        messageForwarder = new MessageForwarderImpl(
                mock(LocalTopicTree.class),
                new HivemqId(),
                () -> null,
                mock(SingleWriterService.class),
                mock(ShutdownHooks.class));
        mqttForwarder = forwarder(FORWARDER_ID, TOPIC);
        otherMqttForwarder = forwarder(OTHER_FORWARDER_ID, OTHER_TOPIC);
    }

    private static MqttForwarder forwarder(final String id, final String... topics) {
        final MqttForwarder forwarder = mock(MqttForwarder.class);
        when(forwarder.getId()).thenReturn(id);
        when(forwarder.getTopics()).thenReturn(List.of(topics));
        return forwarder;
    }

    private static String queueId(final String forwarderId, final String topic) {
        return MessageForwarderImpl.FORWARDER_PREFIX + forwarderId + "/" + topic;
    }

    @Test
    @Timeout(5)
    public void test_isForwarderQueue_follows_forwarder_registration() {
        assertFalse(messageForwarder.isForwarderQueue(QUEUE_ID));

        messageForwarder.addForwarder(mqttForwarder);
        assertTrue(messageForwarder.isForwarderQueue(QUEUE_ID));
        assertFalse(messageForwarder.isForwarderQueue(MessageForwarderImpl.FORWARDER_PREFIX + "other/" + TOPIC));

        messageForwarder.removeForwarder(mqttForwarder, false);
        assertFalse(messageForwarder.isForwarderQueue(QUEUE_ID));
    }

    @Test
    @Timeout(5)
    public void test_isForwarderQueue_resolves_each_forwarder_independently() {
        messageForwarder.addForwarder(mqttForwarder);
        messageForwarder.addForwarder(otherMqttForwarder);

        assertTrue(messageForwarder.isForwarderQueue(QUEUE_ID));
        assertTrue(messageForwarder.isForwarderQueue(OTHER_QUEUE_ID));
        // a queue may not be claimed by the wrong forwarder's registration
        assertFalse(messageForwarder.isForwarderQueue(queueId(FORWARDER_ID, OTHER_TOPIC)));

        // removing one forwarder must not deregister the other's queue
        messageForwarder.removeForwarder(mqttForwarder, false);
        assertFalse(messageForwarder.isForwarderQueue(QUEUE_ID));
        assertTrue(messageForwarder.isForwarderQueue(OTHER_QUEUE_ID));
    }

    @Test
    @Timeout(5)
    public void test_isForwarderQueue_covers_every_topic_of_a_multi_topic_forwarder() {
        messageForwarder.addForwarder(forwarder(FORWARDER_ID, TOPIC, OTHER_TOPIC));

        assertTrue(messageForwarder.isForwarderQueue(QUEUE_ID));
        assertTrue(messageForwarder.isForwarderQueue(queueId(FORWARDER_ID, OTHER_TOPIC)));
    }

    /**
     * EDG-882, the case that rules out a plain {@code Map<queueId, ownerId>} index. A share name may
     * contain '/', so two different forwarders can mint byte-identical queue IDs: {@code ("a", "b/c")}
     * and {@code ("a/b", "c")} both produce {@code $FORWARDER::a/b/c}. A map keyed by queue ID drops the
     * entry when the first of them unregisters, and the periodic clean-up then clears a queue the
     * second still owns. Counting owners survives it.
     * <p>
     * Only constructible at unit level, where forwarder IDs are unconstrained; in production the bridge
     * ID regex and the fixed digest shape make it unreachable. It is here to pin the guarantee
     * unconditionally, so the index cannot later be "simplified" into the unsafe form.
     */
    @Test
    @Timeout(5)
    public void test_a_queue_claimed_by_two_forwarders_survives_the_first_unregistering() {
        final MqttForwarder first = forwarder("a", "b/c");
        final MqttForwarder second = forwarder("a/b", "c");
        final String collidingQueueId = queueId("a", "b/c");
        assertEquals(collidingQueueId, queueId("a/b", "c"), "the two registrations must collide");

        messageForwarder.addForwarder(first);
        messageForwarder.addForwarder(second);
        assertTrue(messageForwarder.isForwarderQueue(collidingQueueId));

        messageForwarder.removeForwarder(first, false);
        assertTrue(
                messageForwarder.isForwarderQueue(collidingQueueId),
                "the second forwarder still owns this queue; clearing it would destroy its messages");

        messageForwarder.removeForwarder(second, false);
        assertFalse(messageForwarder.isForwarderQueue(collidingQueueId));
    }

    /**
     * EDG-882, the case that rules out an {@code if (put(...) == null)} skip guard. A forwarder ID does
     * not determine its queue set: the ID embeds a digest over the filters joined with an <em>empty</em>
     * separator, so {@code {"ab","c"}} and {@code {"a","bc"}} share an ID with different queue sets. A
     * guard that skipped the re-registration would leave the index stale in the message-losing
     * direction — reporting the new queue as unowned.
     */
    @Test
    @Timeout(5)
    public void test_reregistration_with_a_changed_topic_set_updates_ownership() {
        messageForwarder.addForwarder(forwarder(FORWARDER_ID, "ab", "c"));
        assertTrue(messageForwarder.isForwarderQueue(queueId(FORWARDER_ID, "ab")));
        assertTrue(messageForwarder.isForwarderQueue(queueId(FORWARDER_ID, "c")));

        messageForwarder.addForwarder(forwarder(FORWARDER_ID, "a", "bc"));

        assertTrue(messageForwarder.isForwarderQueue(queueId(FORWARDER_ID, "a")));
        assertTrue(messageForwarder.isForwarderQueue(queueId(FORWARDER_ID, "bc")));
        assertFalse(messageForwarder.isForwarderQueue(queueId(FORWARDER_ID, "ab")));
        assertFalse(messageForwarder.isForwarderQueue(queueId(FORWARDER_ID, "c")));
    }

    /** A topic kept across a re-registration must never read as unowned, before or after. */
    @Test
    @Timeout(5)
    public void test_reregistration_keeps_ownership_of_an_unchanged_topic() {
        messageForwarder.addForwarder(forwarder(FORWARDER_ID, TOPIC, OTHER_TOPIC));
        messageForwarder.addForwarder(forwarder(FORWARDER_ID, TOPIC));

        assertTrue(messageForwarder.isForwarderQueue(QUEUE_ID));
        assertFalse(messageForwarder.isForwarderQueue(queueId(FORWARDER_ID, OTHER_TOPIC)));
    }

    @Test
    @Timeout(5)
    public void test_repeated_registration_of_the_same_forwarder_is_idempotent() {
        messageForwarder.addForwarder(mqttForwarder);
        messageForwarder.addForwarder(mqttForwarder);
        messageForwarder.addForwarder(mqttForwarder);
        assertTrue(messageForwarder.isForwarderQueue(QUEUE_ID));

        // a single removal must fully deregister: the repeated adds must not have stacked references
        messageForwarder.removeForwarder(mqttForwarder, false);
        assertFalse(messageForwarder.isForwarderQueue(QUEUE_ID));
    }

    @Test
    @Timeout(5)
    public void test_repeated_removal_of_the_same_forwarder_is_idempotent() {
        messageForwarder.addForwarder(mqttForwarder);
        messageForwarder.removeForwarder(mqttForwarder, false);
        messageForwarder.removeForwarder(mqttForwarder, false);
        assertFalse(messageForwarder.isForwarderQueue(QUEUE_ID));

        // the counts must not have gone negative: a fresh registration still works
        messageForwarder.addForwarder(mqttForwarder);
        assertTrue(messageForwarder.isForwarderQueue(QUEUE_ID));
    }

    @Test
    @Timeout(5)
    public void test_removing_a_forwarder_that_was_never_added_is_a_no_op() {
        messageForwarder.removeForwarder(mqttForwarder, false);
        assertFalse(messageForwarder.isForwarderQueue(QUEUE_ID));

        messageForwarder.addForwarder(mqttForwarder);
        assertTrue(messageForwarder.isForwarderQueue(QUEUE_ID));
    }

    @Test
    @Timeout(5)
    public void test_a_forwarder_without_topics_owns_nothing() {
        final MqttForwarder empty = forwarder(FORWARDER_ID);
        messageForwarder.addForwarder(empty);

        assertFalse(messageForwarder.isForwarderQueue(QUEUE_ID));
        assertFalse(messageForwarder.isForwarderQueue(MessageForwarderImpl.FORWARDER_PREFIX + FORWARDER_ID + "/"));

        messageForwarder.removeForwarder(empty, false);
        assertFalse(messageForwarder.isForwarderQueue(QUEUE_ID));
    }

    /**
     * Three forwarders colliding on one queue name. The queue must survive until the <em>last</em>
     * owner unregisters — a design that merely handled the two-owner case (a boolean "shared" flag,
     * say) would release it on the second removal.
     */
    @Test
    @Timeout(5)
    public void test_a_queue_claimed_by_three_forwarders_survives_until_the_last_one_leaves() {
        final MqttForwarder first = forwarder("a", "b/c/d");
        final MqttForwarder second = forwarder("a/b", "c/d");
        final MqttForwarder third = forwarder("a/b/c", "d");
        final String queue = queueId("a", "b/c/d");
        assertEquals(queue, queueId("a/b", "c/d"));
        assertEquals(queue, queueId("a/b/c", "d"));

        messageForwarder.addForwarder(first);
        messageForwarder.addForwarder(second);
        messageForwarder.addForwarder(third);

        messageForwarder.removeForwarder(first, false);
        assertTrue(messageForwarder.isForwarderQueue(queue), "two owners remain");
        messageForwarder.removeForwarder(second, false);
        assertTrue(messageForwarder.isForwarderQueue(queue), "one owner remains");
        messageForwarder.removeForwarder(third, false);
        assertFalse(messageForwarder.isForwarderQueue(queue));
    }

    /**
     * {@code getTopics()} is a {@code List} and may repeat a topic. The queue IDs are collected into a
     * set, so a duplicate must claim exactly one reference — otherwise a single removal would leave the
     * queue permanently claimed and the clean-up could never reclaim it.
     */
    @Test
    @Timeout(5)
    public void test_a_duplicated_topic_claims_only_one_reference() {
        final MqttForwarder duplicated = forwarder(FORWARDER_ID, TOPIC, TOPIC, TOPIC);
        messageForwarder.addForwarder(duplicated);
        assertTrue(messageForwarder.isForwarderQueue(QUEUE_ID));

        messageForwarder.removeForwarder(duplicated, false);
        assertFalse(messageForwarder.isForwarderQueue(QUEUE_ID), "duplicate topics leaked a reference");
    }

    /** Degenerate identifiers must not be treated specially by the index. */
    @Test
    @Timeout(5)
    public void test_empty_forwarder_id_and_empty_topic_are_handled_like_any_other() {
        final MqttForwarder emptyId = forwarder("", "");
        messageForwarder.addForwarder(emptyId);
        assertTrue(messageForwarder.isForwarderQueue(MessageForwarderImpl.FORWARDER_PREFIX + "/"));

        messageForwarder.removeForwarder(emptyId, false);
        assertFalse(messageForwarder.isForwarderQueue(MessageForwarderImpl.FORWARDER_PREFIX + "/"));
    }

    /** A forwarder id that itself contains the forwarder prefix must not confuse ownership. */
    @Test
    @Timeout(5)
    public void test_a_forwarder_id_containing_the_prefix_is_resolved_normally() {
        final String nestedId = MessageForwarderImpl.FORWARDER_PREFIX + "inner-Ab/cd==";
        final MqttForwarder nested = forwarder(nestedId, TOPIC);
        messageForwarder.addForwarder(nested);

        assertTrue(messageForwarder.isForwarderQueue(queueId(nestedId, TOPIC)));
        messageForwarder.removeForwarder(nested, false);
        assertFalse(messageForwarder.isForwarderQueue(queueId(nestedId, TOPIC)));
    }

    /** Topics with leading, trailing and repeated separators are ordinary inputs here. */
    @Test
    @Timeout(5)
    public void test_topics_full_of_separators_are_resolved_normally() {
        final List<String> awkward = List.of("/", "//", "/leading", "trailing/", "a//b", "///");
        final MqttForwarder awkwardForwarder = forwarder(FORWARDER_ID, awkward.toArray(new String[0]));
        messageForwarder.addForwarder(awkwardForwarder);

        for (final String topic : awkward) {
            assertTrue(messageForwarder.isForwarderQueue(queueId(FORWARDER_ID, topic)), topic);
        }
        messageForwarder.removeForwarder(awkwardForwarder, false);
        for (final String topic : awkward) {
            assertFalse(messageForwarder.isForwarderQueue(queueId(FORWARDER_ID, topic)), topic);
        }
    }

    /**
     * {@code removeForwarder(_, true)} additionally clears the persisted queues. Ownership must be
     * dropped exactly as it is on the non-clearing path — the two must not diverge.
     */
    @Test
    @Timeout(5)
    public void test_removal_with_queue_clearing_deregisters_identically() {
        messageForwarder.addForwarder(mqttForwarder);
        assertTrue(messageForwarder.isForwarderQueue(QUEUE_ID));

        messageForwarder.removeForwarder(mqttForwarder, true);
        assertFalse(messageForwarder.isForwarderQueue(QUEUE_ID));
    }

    /**
     * Ownership is released against the set that was actually registered, not against whatever
     * {@code getTopics()} happens to return at removal time. A forwarder object whose topics changed
     * after registration must still fully deregister — otherwise the stale queue is claimed forever.
     */
    @Test
    @Timeout(5)
    public void test_removal_releases_the_registered_set_not_the_current_topics() {
        messageForwarder.addForwarder(forwarder(FORWARDER_ID, TOPIC));
        assertTrue(messageForwarder.isForwarderQueue(QUEUE_ID));

        // same id, different topics — as if the forwarder object had been rebuilt in the meantime
        messageForwarder.removeForwarder(forwarder(FORWARDER_ID, OTHER_TOPIC), false);

        assertFalse(
                messageForwarder.isForwarderQueue(QUEUE_ID),
                "the originally registered queue leaked a reference and can never be reclaimed");
    }

    /**
     * The equivalence property: for every queue ID, at every step of an arbitrary registration
     * sequence, the index must answer exactly what a full scan over the registered sets would answer.
     * The ID and topic pools are chosen so that colliding queue IDs and shared topics occur often.
     * <p>
     * Deterministic by construction — a fixed seed, so a failure is reproducible from the report alone.
     */
    @Test
    @Timeout(30)
    public void test_isForwarderQueue_matches_a_full_scan_over_random_registration_sequences() {
        final List<String> ids = List.of("a", "a/b", "bridge-Ab/cd==", "bridge-Abcd==", "x");
        final List<String> topics = List.of("b/c", "c", "cd==/t", "t", "a/b/c");

        final Set<String> universe = new LinkedHashSet<>();
        for (final String id : ids) {
            for (final String topic : topics) {
                universe.add(queueId(id, topic));
            }
        }

        final Map<String, Set<String>> oracle = new HashMap<>();
        final Random random = new Random(882L);

        for (int step = 0; step < 400; step++) {
            final String id = ids.get(random.nextInt(ids.size()));
            if (random.nextInt(3) == 0) {
                messageForwarder.removeForwarder(forwarder(id), false);
                oracle.remove(id);
            } else {
                final List<String> chosen = new ArrayList<>();
                for (final String topic : topics) {
                    if (random.nextBoolean()) {
                        chosen.add(topic);
                    }
                }
                messageForwarder.addForwarder(forwarder(id, chosen.toArray(new String[0])));
                final Set<String> registered = new HashSet<>();
                for (final String topic : chosen) {
                    registered.add(queueId(id, topic));
                }
                oracle.put(id, registered);
            }

            for (final String candidate : universe) {
                assertEquals(
                        scan(oracle, candidate),
                        messageForwarder.isForwarderQueue(candidate),
                        "step " + step + ", queue " + candidate);
            }
        }
    }

    /** The pre-fix implementation, kept as the oracle the reference-counted index must match. */
    private static boolean scan(final Map<String, Set<String>> registry, final String queueId) {
        for (final Set<String> queueIds : registry.values()) {
            if (queueIds.contains(queueId)) {
                return true;
            }
        }
        return false;
    }
}
