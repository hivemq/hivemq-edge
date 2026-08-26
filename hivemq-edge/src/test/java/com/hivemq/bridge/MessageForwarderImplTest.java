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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.HivemqId;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriberWithQoS;
import com.hivemq.mqtt.topic.SubscriptionFlag;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.ProducerQueues;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.clientsession.ClientSessionSubscriptionPersistence;
import com.hivemq.persistence.clientsession.SharedSubscriptionServiceImpl.SharedSubscription;
import com.hivemq.util.Topics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private LocalTopicTree topicTree;
    private MqttForwarder mqttForwarder;
    private MqttForwarder otherMqttForwarder;

    private final ClientSessionSubscriptionPersistence subscriptionPersistence =
            mock(ClientSessionSubscriptionPersistence.class);

    @BeforeEach
    public void setUp() {
        topicTree = mock(LocalTopicTree.class);
        // production returns an empty set, never null; the eviction pass reads it on every registration
        when(topicTree.getSharedSubscriber(anyString(), anyString())).thenReturn(ImmutableSet.of());
        messageForwarder = new MessageForwarderImpl(
                topicTree,
                new HivemqId(),
                () -> null,
                () -> subscriptionPersistence,
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

    /**
     * EDG-882 QA round 2. Shared subscribers of one group take turns, so a client that holds this
     * forwarder's group receives the messages the bridge is there to forward and they never reach the
     * remote broker. A SUBSCRIBE is refused while the queue is claimed, and BridgeService claims it from
     * the top of every start and restart — so the one window left is a client subscribing to the group
     * of a bridge that does not exist yet, which becomes a collision when the operator creates it. That
     * window is closed here, at the moment the forwarder registers.
     */
    @Test
    @Timeout(5)
    public void test_addForwarder_removes_a_client_squatting_on_the_forwarders_group() {
        when(topicTree.getSharedSubscriber(MessageForwarderImpl.FORWARDER_PREFIX + FORWARDER_ID, TOPIC))
                .thenReturn(ImmutableSet.of(new SubscriberWithQoS("squatter", 1, (byte) 0, null)));
        when(subscriptionPersistence.remove(anyString(), anyString())).thenReturn(Futures.immediateFuture(null));

        messageForwarder.addForwarder(mqttForwarder);

        // through the persistence, not the topic tree alone: the client's session would otherwise
        // restore the subscription on its next reconnect and take the group straight back
        verify(subscriptionPersistence)
                .remove("squatter", "$share/" + MessageForwarderImpl.FORWARDER_PREFIX + FORWARDER_ID + "/" + TOPIC);
    }

    /**
     * EDG-882 review v02, R2-02. The test above stubs the tree under the forwarder's <em>own</em> split,
     * which is the only split a forwarder itself ever registers — it passes the group and the filter to
     * the tree directly. A client cannot: it sends one string and the broker splits it at the first '/'
     * after {@code $share/}, which for this ticket's slash-bearing digest falls inside the digest. That
     * puts the intruder on a different node entirely, so an eviction that looks only under the
     * forwarder's own split finds nothing — while the queue the client polls is keyed off the
     * concatenated string and is byte-identical to the bridge's.
     * <p>
     * A real {@link LocalTopicTree} and the real {@link Topics} split, because the whole defect lives in
     * the disagreement between the two decompositions; a mock would only replay whichever one the test
     * author had in mind.
     */
    @Test
    @Timeout(5)
    public void test_addForwarder_removes_a_squatter_stored_under_the_alternative_split() {
        final LocalTopicTree realTree = new LocalTopicTree(new MetricsHolder(new MetricRegistry()));
        final MessageForwarderImpl forwarderOverRealTree = new MessageForwarderImpl(
                realTree,
                new HivemqId(),
                () -> null,
                () -> subscriptionPersistence,
                mock(SingleWriterService.class),
                mock(ShutdownHooks.class));
        when(subscriptionPersistence.remove(anyString(), anyString())).thenReturn(Futures.immediateFuture(null));

        // exactly what a client's SUBSCRIBE goes through
        final SharedSubscription asStored = Topics.checkForSharedSubscription("$share/" + QUEUE_ID);
        assertNotNull(asStored);
        // the premise, pinned: were the digest slash-free this would equal the forwarder's own split and
        // the test would pass without exercising anything
        assertNotEquals(MessageForwarderImpl.FORWARDER_PREFIX + FORWARDER_ID, asStored.getShareName());
        realTree.addTopic(
                "squatter",
                new Topic(asStored.getTopicFilter(), QoS.AT_LEAST_ONCE, false, true),
                SubscriptionFlag.getDefaultFlags(true, true, false),
                asStored.getShareName());
        assertFalse(
                realTree.getSharedSubscriber(asStored.getShareName(), asStored.getTopicFilter())
                        .isEmpty(),
                "fixture is vacuous: the squatter is not in the tree");

        forwarderOverRealTree.addForwarder(mqttForwarder);

        // the removal string does not depend on which split found the client: group + '/' + filter is
        // the queue ID however it was cut, and '$share/' + that is what the client's session stores
        verify(subscriptionPersistence).remove("squatter", "$share/" + QUEUE_ID);
    }

    /** An internal component's own entry is not an intruder, including a previous generation of it. */
    @Test
    @Timeout(5)
    public void test_addForwarder_leaves_internal_subscribers_alone() {
        final String ownClientId = MessageForwarderImpl.FORWARDER_PREFIX + FORWARDER_ID + "#node-1";
        when(topicTree.getSharedSubscriber(MessageForwarderImpl.FORWARDER_PREFIX + FORWARDER_ID, TOPIC))
                .thenReturn(ImmutableSet.of(new SubscriberWithQoS(ownClientId, 1, (byte) 0, null)));

        messageForwarder.addForwarder(mqttForwarder);

        verify(subscriptionPersistence, never()).remove(anyString(), anyString());
    }

    /**
     * The claim the subscribe-side check rests on: a queue held only by a reservation — which is what
     * every start, restart and node bootstrap takes before anything is registered — already reads as a
     * forwarder queue, so a client cannot slip into the group during the hand-over.
     */
    @Test
    @Timeout(5)
    public void test_isForwarderQueue_is_true_while_only_a_reservation_holds_the_queue() {
        messageForwarder.reserveQueues("bridge", Map.of(FORWARDER_ID, List.of(TOPIC)));

        assertTrue(messageForwarder.isForwarderQueue(QUEUE_ID));

        messageForwarder.releaseReservedQueues("bridge");
        assertFalse(messageForwarder.isForwarderQueue(QUEUE_ID));
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
     * EDG-882 F-01, the defect itself, at the level where it is decided.
     * <p>
     * A forwarder ID does not determine its queue set: the ID embeds a digest over the filters joined
     * with an <em>empty</em> separator, so the subscriptions {@code {"ab","c"}} and {@code {"a","bc"}}
     * — with the same destination — share an ID while owning different queues. Both are live at the
     * same time, because a bridge builds and registers one forwarder per local subscription.
     * <p>
     * Letting the second registration displace the first is what loses messages: the first
     * forwarder's queues stop reading as owned while it is still running and still filling them, and
     * the next clean-up sweep deletes their contents. So the second registration is refused and the
     * first keeps everything.
     */
    @Test
    @Timeout(5)
    public void test_a_colliding_registration_is_refused_and_the_incumbent_keeps_its_queues() {
        final MqttForwarder incumbent = forwarder(FORWARDER_ID, "ab", "c");
        final MqttForwarder colliding = forwarder(FORWARDER_ID, "a", "bc");
        messageForwarder.addForwarder(incumbent);

        assertThrows(IllegalStateException.class, () -> messageForwarder.addForwarder(colliding));

        assertTrue(
                messageForwarder.isForwarderQueue(queueId(FORWARDER_ID, "ab")),
                "the incumbent is still live; un-owning its queue lets the clean-up delete its messages");
        assertTrue(messageForwarder.isForwarderQueue(queueId(FORWARDER_ID, "c")));
        // and the refused registration claimed nothing of its own
        assertFalse(messageForwarder.isForwarderQueue(queueId(FORWARDER_ID, "a")));
        assertFalse(messageForwarder.isForwarderQueue(queueId(FORWARDER_ID, "bc")));
    }

    /** A refused registration must not have been started or subscribed — it was never registered. */
    @Test
    @Timeout(5)
    public void test_a_refused_registration_is_never_started() {
        final MqttForwarder colliding = forwarder(FORWARDER_ID, "a", "bc");
        messageForwarder.addForwarder(forwarder(FORWARDER_ID, "ab", "c"));

        assertThrows(IllegalStateException.class, () -> messageForwarder.addForwarder(colliding));

        verify(colliding, never()).start();
        verify(topicTree, never())
                .addTopic(any(), argThat(topic -> topic.getTopic().equals("bc")), anyByte(), any());
    }

    /**
     * The refusal must leave the reference counts exactly as it found them. A queue the two
     * registrations have in common is the case that breaks a naive undo: releasing the incumbent's
     * reference along with the refused one drops the count to zero for a queue that is still owned.
     */
    @Test
    @Timeout(5)
    public void test_a_refused_registration_leaves_the_reference_counts_untouched() {
        final MqttForwarder incumbent = forwarder(FORWARDER_ID, TOPIC, "ab", "c");
        messageForwarder.addForwarder(incumbent);

        // overlaps the incumbent on TOPIC and differs elsewhere
        assertThrows(
                IllegalStateException.class,
                () -> messageForwarder.addForwarder(forwarder(FORWARDER_ID, TOPIC, "a", "bc")));

        assertTrue(messageForwarder.isForwarderQueue(QUEUE_ID), "the shared queue lost its owner");

        // one removal must still fully deregister: the refusal may not have leaked or stacked a reference
        messageForwarder.removeForwarder(incumbent, false);
        assertFalse(messageForwarder.isForwarderQueue(QUEUE_ID), "the refusal leaked a reference on the shared queue");
        assertFalse(messageForwarder.isForwarderQueue(queueId(FORWARDER_ID, "ab")));
    }

    /** Refusing is not poisoning: once the incumbent is gone, the same ID registers normally. */
    @Test
    @Timeout(5)
    public void test_the_id_is_reusable_after_the_incumbent_is_removed() {
        final MqttForwarder incumbent = forwarder(FORWARDER_ID, "ab", "c");
        final MqttForwarder replacement = forwarder(FORWARDER_ID, "a", "bc");
        messageForwarder.addForwarder(incumbent);
        assertThrows(IllegalStateException.class, () -> messageForwarder.addForwarder(replacement));

        messageForwarder.removeForwarder(incumbent, false);
        messageForwarder.addForwarder(replacement);

        assertTrue(messageForwarder.isForwarderQueue(queueId(FORWARDER_ID, "a")));
        assertTrue(messageForwarder.isForwarderQueue(queueId(FORWARDER_ID, "bc")));
        assertFalse(messageForwarder.isForwarderQueue(queueId(FORWARDER_ID, "ab")));
        assertFalse(messageForwarder.isForwarderQueue(queueId(FORWARDER_ID, "c")));
    }

    /** A topic kept across a remove-then-add must be owned before and after. */
    @Test
    @Timeout(5)
    public void test_reregistration_after_removal_keeps_ownership_of_an_unchanged_topic() {
        final MqttForwarder first = forwarder(FORWARDER_ID, TOPIC, OTHER_TOPIC);
        messageForwarder.addForwarder(first);
        messageForwarder.removeForwarder(first, false);
        messageForwarder.addForwarder(forwarder(FORWARDER_ID, TOPIC));

        assertTrue(messageForwarder.isForwarderQueue(QUEUE_ID));
        assertFalse(messageForwarder.isForwarderQueue(queueId(FORWARDER_ID, OTHER_TOPIC)));
    }

    /**
     * Even the same object twice is refused. Nothing in production does this — a bridge registers each
     * forwarder once, and a restart removes before it adds — so tolerating it would only hide the case
     * the refusal exists for: an ID that is registered twice while both owners are live.
     */
    @Test
    @Timeout(5)
    public void test_registering_the_same_forwarder_object_twice_is_refused() {
        messageForwarder.addForwarder(mqttForwarder);

        assertThrows(IllegalStateException.class, () -> messageForwarder.addForwarder(mqttForwarder));

        assertTrue(messageForwarder.isForwarderQueue(QUEUE_ID));
        // a single removal must fully deregister: the refused add must not have stacked a reference
        messageForwarder.removeForwarder(mqttForwarder, false);
        assertFalse(messageForwarder.isForwarderQueue(QUEUE_ID));
    }

    /**
     * A forwarder without topics owns no queues, so its registration leaves no trace in the reference
     * counts — the ID must still be taken, or the collision it exists to catch goes unnoticed.
     */
    @Test
    @Timeout(5)
    public void test_a_topicless_registration_still_takes_the_id() {
        messageForwarder.addForwarder(forwarder(FORWARDER_ID));

        assertThrows(IllegalStateException.class, () -> messageForwarder.addForwarder(forwarder(FORWARDER_ID, TOPIC)));
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
     * A notification that lands after the forwarder is gone must not put its queue back into the poll
     * set (EDG-882 review v02, R2-06).
     * <p>
     * {@code messageAvailable} does not touch the set directly: it submits the add to the single writer,
     * so the add runs at a moment {@code removeForwarder} does not control. Pruning "after the stop" was
     * not enough, because a task submitted before the stop can still run after the prune, and the entry
     * it leaves is one nothing else removes — one string per retired subscription, for the life of the
     * node. The guard reads the ownership at the moment the task runs, and {@code removeForwarder}
     * releases ownership before it prunes, so a late add is impossible rather than merely unlikely.
     * <p>
     * The writer here runs tasks inline, which is a faithful stand-in: what the guard depends on is the
     * state when the task runs, not how long it waited.
     */
    @Test
    @Timeout(5)
    public void test_a_notification_arriving_after_removal_does_not_re_add_the_queue() {
        final MessageForwarderImpl forwarderOverInlineWriter = new MessageForwarderImpl(
                topicTree,
                new HivemqId(),
                () -> null,
                () -> subscriptionPersistence,
                inlineSingleWriter(),
                mock(ShutdownHooks.class));
        final MqttForwarder registered = forwarder(FORWARDER_ID, TOPIC);
        forwarderOverInlineWriter.addForwarder(registered);

        // the control: while it is registered, a notification does put the queue in the poll set
        forwarderOverInlineWriter.messageAvailable(QUEUE_ID);
        assertTrue(forwarderOverInlineWriter.notEmptyQueues().contains(QUEUE_ID));

        forwarderOverInlineWriter.removeForwarder(registered, false);
        forwarderOverInlineWriter.messageAvailable(QUEUE_ID); // the straggler

        assertFalse(
                forwarderOverInlineWriter.notEmptyQueues().contains(QUEUE_ID),
                "a queue nobody owns was put back into the poll set, and nothing ever takes it out again");
    }

    /**
     * The poll set is pruned against what was registered, not against what the forwarder object says its
     * topics are now (EDG-882 review v02, R2-05).
     * <p>
     * The sibling test asserts the same thing for ownership. This is the other map the two can disagree
     * about, and the entry the disagreement leaves behind is one nothing else removes: the forwarder is
     * gone, so nothing polls it and nothing ever prunes it again.
     */
    @Test
    @Timeout(5)
    public void test_removal_prunes_the_poll_set_against_the_registered_set() {
        messageForwarder.addForwarder(forwarder(FORWARDER_ID, TOPIC));
        assertTrue(messageForwarder.notEmptyQueues().contains(QUEUE_ID));

        // same id, different topics — as if the forwarder object had been rebuilt in the meantime
        messageForwarder.removeForwarder(forwarder(FORWARDER_ID, OTHER_TOPIC), false);

        assertFalse(
                messageForwarder.notEmptyQueues().contains(QUEUE_ID),
                "the registered queue stayed in the poll set, and nothing polls or prunes it again");
    }

    /** A single writer that runs what it is given, so a submitted task's ordering can be asserted. */
    private static SingleWriterService inlineSingleWriter() {
        final ProducerQueues queues = mock(ProducerQueues.class);
        when(queues.submit(anyString(), any())).thenAnswer(invocation -> {
            final SingleWriterService.Task<?> task = invocation.getArgument(1);
            task.doTask(0);
            return Futures.immediateFuture(null);
        });
        final SingleWriterService singleWriter = mock(SingleWriterService.class);
        when(singleWriter.getQueuedMessagesQueue()).thenReturn(queues);
        return singleWriter;
    }

    /**
     * Why removal is by id and not conditional on the instance (EDG-882 review v02, R2-05).
     * <p>
     * The review asked for {@code forwarders.remove(id, instance)}, so that a caller holding a stale
     * object cannot tear down a successor that has taken the same id. That defence is unnecessary and
     * not free: it would break {@link #test_removal_releases_the_registered_set_not_the_current_topics},
     * where the caller holds a rebuilt object and the removal must still release — otherwise the queue
     * is claimed for the life of the node with nothing able to free it.
     * <p>
     * It is unnecessary because a successor cannot coexist with an incumbent: {@code addForwarder}
     * refuses a second registration under a live id. This pins that, so the argument the decision rests
     * on fails here rather than in review if it ever stops being true.
     */
    @Test
    @Timeout(5)
    public void test_a_successor_cannot_register_while_the_incumbent_is_live() {
        final MqttForwarder incumbent = forwarder(FORWARDER_ID, TOPIC);
        final MqttForwarder successor = forwarder(FORWARDER_ID, TOPIC);
        messageForwarder.addForwarder(incumbent);

        assertThrows(IllegalStateException.class, () -> messageForwarder.addForwarder(successor));

        // and the incumbent is untouched by the refusal
        assertTrue(messageForwarder.isForwarderQueue(QUEUE_ID));
        verify(successor, never()).start();
    }

    /**
     * The other half of the same argument: once the incumbent has been removed, a successor may take the
     * id, and a second (stale) removal of the incumbent must not take the successor's queues with it.
     * <p>
     * It does not, because the stale removal finds nothing left of the incumbent to remove — the maps
     * were emptied by the first one and the successor re-populated them. This is the sequence the review
     * asked for; it passes with removal by id.
     */
    @Test
    @Timeout(5)
    public void test_a_stale_removal_after_a_successor_registered_leaves_the_successor_alone() {
        final MqttForwarder incumbent = forwarder(FORWARDER_ID, TOPIC);
        final MqttForwarder successor = forwarder(FORWARDER_ID, TOPIC);
        messageForwarder.addForwarder(incumbent);
        messageForwarder.removeForwarder(incumbent, false);
        messageForwarder.addForwarder(successor);

        messageForwarder.removeForwarder(incumbent, false); // the stale one

        assertFalse(
                messageForwarder.isForwarderQueue(QUEUE_ID),
                "a stale removal must not leave the successor's queue owned by nobody either");
        verify(successor).stop();
    }

    /**
     * EDG-882 F-02, the half of the hand-over that lives in this class. Ownership is what the periodic
     * clean-up reads, so it may not be dropped while the forwarder is still published and still
     * polling: a sweep landing in that window clears a queue that is being filled and drained as it
     * does so. The queue must therefore still read as owned at the moment the forwarder is stopped.
     */
    @Test
    @Timeout(5)
    public void test_ownership_outlives_the_forwarder_it_belongs_to() {
        final AtomicBoolean ownedWhenStopped = new AtomicBoolean();
        doAnswer(invocation -> {
                    ownedWhenStopped.set(messageForwarder.isForwarderQueue(QUEUE_ID));
                    return null;
                })
                .when(mqttForwarder)
                .stop();

        messageForwarder.addForwarder(mqttForwarder);
        messageForwarder.removeForwarder(mqttForwarder, false);

        assertTrue(
                ownedWhenStopped.get(),
                "the queue read as unowned while its forwarder was still running; a sweep here clears a live queue");
        assertFalse(
                messageForwarder.isForwarderQueue(QUEUE_ID), "and it must be reclaimable once the forwarder is gone");
    }

    /**
     * A reservation is ownership without a forwarder: it exists so that the periodic clean-up, which
     * deletes every forwarder queue no registered forwarder owns, leaves the queues of a bridge that
     * could not start alone until it can be corrected or removed.
     */
    @Test
    @Timeout(5)
    public void test_reserved_queues_read_as_owned_until_released() {
        assertFalse(messageForwarder.isForwarderQueue(QUEUE_ID));

        messageForwarder.reserveQueues("bridge", Map.of(FORWARDER_ID, List.of(TOPIC, OTHER_TOPIC)));
        assertTrue(messageForwarder.isForwarderQueue(QUEUE_ID));
        assertTrue(messageForwarder.isForwarderQueue(queueId(FORWARDER_ID, OTHER_TOPIC)));
        assertFalse(messageForwarder.isForwarderQueue(OTHER_QUEUE_ID), "only what was reserved is held");

        messageForwarder.releaseReservedQueues("bridge");
        assertFalse(messageForwarder.isForwarderQueue(QUEUE_ID));
        assertFalse(messageForwarder.isForwarderQueue(queueId(FORWARDER_ID, OTHER_TOPIC)));
    }

    /**
     * The hand-over a corrected configuration makes: the forwarder registers, and only then is the
     * reservation dropped. The queue is owned throughout — its count goes 1 → 2 → 1 and never reaches
     * zero, because a single sweep landing on zero is all it takes to lose the messages.
     */
    @Test
    @Timeout(5)
    public void test_a_reserved_queue_stays_owned_across_the_hand_over_to_its_forwarder() {
        messageForwarder.reserveQueues("bridge", Map.of(FORWARDER_ID, List.of(TOPIC)));

        messageForwarder.addForwarder(mqttForwarder);
        assertTrue(messageForwarder.isForwarderQueue(QUEUE_ID));
        messageForwarder.releaseReservedQueues("bridge");
        assertTrue(messageForwarder.isForwarderQueue(QUEUE_ID), "the forwarder owns it now");

        // and the reservation left no residual reference behind: one removal fully deregisters
        messageForwarder.removeForwarder(mqttForwarder, false);
        assertFalse(messageForwarder.isForwarderQueue(QUEUE_ID), "the reservation leaked a reference");
    }

    /** Re-reserving under the same id replaces the held set rather than stacking references on it. */
    @Test
    @Timeout(5)
    public void test_reserving_twice_replaces_the_held_set() {
        messageForwarder.reserveQueues("bridge", Map.of(FORWARDER_ID, List.of(TOPIC, OTHER_TOPIC)));
        messageForwarder.reserveQueues("bridge", Map.of(FORWARDER_ID, List.of(TOPIC)));

        assertTrue(messageForwarder.isForwarderQueue(QUEUE_ID));
        assertFalse(
                messageForwarder.isForwarderQueue(queueId(FORWARDER_ID, OTHER_TOPIC)),
                "a queue dropped from the configuration must stop being held");

        messageForwarder.releaseReservedQueues("bridge");
        assertFalse(messageForwarder.isForwarderQueue(QUEUE_ID), "re-reserving stacked a reference");
    }

    /** Two bridges may hold the same queue id; it is reclaimable only once both have let go. */
    @Test
    @Timeout(5)
    public void test_queues_held_by_two_reservations_survive_the_first_release() {
        messageForwarder.reserveQueues("bridge-a", Map.of(FORWARDER_ID, List.of(TOPIC)));
        messageForwarder.reserveQueues("bridge-b", Map.of(FORWARDER_ID, List.of(TOPIC)));

        messageForwarder.releaseReservedQueues("bridge-a");
        assertTrue(messageForwarder.isForwarderQueue(QUEUE_ID), "the second reservation still holds it");
        messageForwarder.releaseReservedQueues("bridge-b");
        assertFalse(messageForwarder.isForwarderQueue(QUEUE_ID));
    }

    /** Degenerate inputs must not corrupt the counts: releasing an unknown id, or reserving nothing. */
    @Test
    @Timeout(5)
    public void test_reserving_nothing_and_releasing_an_unknown_reservation_are_no_ops() {
        messageForwarder.releaseReservedQueues("never-reserved");
        messageForwarder.reserveQueues("empty", Map.of());
        messageForwarder.reserveQueues("no-topics", Map.of(FORWARDER_ID, List.of()));
        messageForwarder.releaseReservedQueues("empty");
        messageForwarder.releaseReservedQueues("no-topics");

        // the index must still work afterwards
        messageForwarder.addForwarder(mqttForwarder);
        assertTrue(messageForwarder.isForwarderQueue(QUEUE_ID));
        messageForwarder.removeForwarder(mqttForwarder, false);
        assertFalse(messageForwarder.isForwarderQueue(QUEUE_ID));
    }

    /**
     * EDG-882 F-10. A registration that throws part way must leave nothing behind. Ownership is what
     * the periodic clean-up reads, so a claim held by an object that never ran preserves a queue
     * nobody will ever drain — for the life of the node, since the ID cannot be registered again
     * either.
     */
    @Test
    @Timeout(5)
    public void test_a_registration_that_fails_in_the_topic_tree_leaves_nothing_behind() {
        final MqttForwarder failing = forwarder(FORWARDER_ID, TOPIC, OTHER_TOPIC);
        doThrow(new IllegalStateException("topic tree said no"))
                .when(topicTree)
                .addTopic(any(), argThat(topic -> topic.getTopic().equals(OTHER_TOPIC)), anyByte(), any());

        assertThrows(IllegalStateException.class, () -> messageForwarder.addForwarder(failing));

        assertFalse(messageForwarder.isForwarderQueue(QUEUE_ID), "ownership survived a failed registration");
        assertFalse(messageForwarder.isForwarderQueue(queueId(FORWARDER_ID, OTHER_TOPIC)));
        // the subscription that did get added must have been taken back out again
        verify(topicTree).removeSubscriber(any(), eq(TOPIC), any());
        verify(failing, never()).start();
    }

    /** The same, one step later: the object was published and started before it threw. */
    @Test
    @Timeout(5)
    public void test_a_registration_that_fails_on_start_leaves_nothing_behind() {
        final MqttForwarder failing = forwarder(FORWARDER_ID, TOPIC);
        doThrow(new IllegalStateException("start said no")).when(failing).start();

        assertThrows(IllegalStateException.class, () -> messageForwarder.addForwarder(failing));

        assertFalse(messageForwarder.isForwarderQueue(QUEUE_ID), "ownership survived a failed start");
        verify(failing).stop();
        verify(topicTree).removeSubscriber(any(), eq(TOPIC), any());
    }

    /** And the ID is free afterwards: a failed registration must not block the retry. */
    @Test
    @Timeout(5)
    public void test_the_id_is_registerable_again_after_a_failed_registration() {
        final MqttForwarder failing = forwarder(FORWARDER_ID, TOPIC);
        doThrow(new IllegalStateException("start said no")).when(failing).start();
        assertThrows(IllegalStateException.class, () -> messageForwarder.addForwarder(failing));

        messageForwarder.addForwarder(forwarder(FORWARDER_ID, TOPIC));

        assertTrue(messageForwarder.isForwarderQueue(QUEUE_ID));
    }

    /**
     * The rollback must undo its own claim and nothing else. Another forwarder holding the same queue
     * ID — reachable because a share name may contain '/' — must still own it afterwards, or one
     * forwarder's failure deletes another's messages.
     */
    @Test
    @Timeout(5)
    public void test_a_failed_registration_does_not_disturb_another_owner_of_the_same_queue() {
        final MqttForwarder incumbent = forwarder("a/b", "c");
        messageForwarder.addForwarder(incumbent);
        final String sharedQueue = queueId("a/b", "c");
        assertEquals(sharedQueue, queueId("a", "b/c"), "the two registrations must collide");

        final MqttForwarder failing = forwarder("a", "b/c");
        doThrow(new IllegalStateException("start said no")).when(failing).start();
        assertThrows(IllegalStateException.class, () -> messageForwarder.addForwarder(failing));

        assertTrue(
                messageForwarder.isForwarderQueue(sharedQueue),
                "the incumbent still owns this queue; clearing it would destroy its messages");
        messageForwarder.removeForwarder(incumbent, false);
        assertFalse(messageForwarder.isForwarderQueue(sharedQueue), "the failed registration leaked a reference");
    }

    /**
     * The equivalence property: for every queue ID, at every step of an arbitrary registration
     * sequence, the index must answer exactly what a full scan over the registered sets would answer.
     * The ID and topic pools are chosen so that colliding queue IDs and shared topics occur often, and
     * the sequence deliberately attempts registrations under IDs that are already taken — each of
     * those must be refused and must leave the index byte-for-byte as it was.
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
        int refusals = 0;

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
                final MqttForwarder candidate = forwarder(id, chosen.toArray(new String[0]));
                if (oracle.containsKey(id)) {
                    // the ID is taken: refused, and the index must be unchanged by the attempt
                    assertThrows(IllegalStateException.class, () -> messageForwarder.addForwarder(candidate));
                    refusals++;
                } else {
                    messageForwarder.addForwarder(candidate);
                    final Set<String> registered = new HashSet<>();
                    for (final String topic : chosen) {
                        registered.add(queueId(id, topic));
                    }
                    oracle.put(id, registered);
                }
            }

            for (final String candidate : universe) {
                assertEquals(
                        scan(oracle, candidate),
                        messageForwarder.isForwarderQueue(candidate),
                        "step " + step + ", queue " + candidate);
            }
        }

        assertTrue(refusals > 0, "the sequence never attempted a registration under a taken ID");
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

    // ---------------------------------------------------------------------------------------------
    // EDG-882 review v03, R3-04 — the teardown must reach a deliberate final state whatever throws.
    //
    // removeForwarder used to stop the forwarder before dropping its ownership, and a stop() that threw
    // took the rest of the method with it: the queue IDs stayed in the index, so the periodic clean-up
    // read them as owned for the life of the node and never reclaimed them, and addForwarder refused a
    // replacement under the same id because putIfAbsent still found the abandoned claim. One transient
    // persistence failure, and the bridge could not be restarted and its storage could not be freed.
    //
    // RemoteMqttForwarder.stop() drains its buffers through reset callbacks that wait on persistence
    // futures and rethrow an ExecutionException as a RuntimeException, so this is an ordinary
    // consequence of a persistence hiccup rather than a contrived mock.
    // ---------------------------------------------------------------------------------------------

    @Test
    @Timeout(5)
    public void test_aStopThatThrowsStillReleasesQueueOwnership() {
        messageForwarder.addForwarder(mqttForwarder);
        assertTrue(messageForwarder.isForwarderQueue(QUEUE_ID));
        doThrow(new RuntimeException("persistence future failed while draining"))
                .when(mqttForwarder)
                .stop();

        assertThrows(RuntimeException.class, () -> messageForwarder.removeForwarder(mqttForwarder, false));

        assertFalse(
                messageForwarder.isForwarderQueue(QUEUE_ID),
                "the queue is owned by a forwarder that no longer exists, so the clean-up can never reclaim it");
    }

    /**
     * And the id must be reusable afterwards. A claim left behind is not only a storage leak: {@code
     * addForwarder} refuses a second registration while one is live, so the bridge cannot be restarted.
     */
    @Test
    @Timeout(5)
    public void test_aStopThatThrowsLeavesTheForwarderIdReusable() {
        messageForwarder.addForwarder(mqttForwarder);
        doThrow(new RuntimeException("persistence future failed while draining"))
                .when(mqttForwarder)
                .stop();
        assertThrows(RuntimeException.class, () -> messageForwarder.removeForwarder(mqttForwarder, false));

        final MqttForwarder replacement = forwarder(FORWARDER_ID, TOPIC);
        messageForwarder.addForwarder(replacement);

        assertTrue(
                messageForwarder.isForwarderQueue(QUEUE_ID),
                "the replacement could not claim the id its predecessor abandoned");
    }

    /** The failure still has to reach the caller — released, but not silently. */
    @Test
    @Timeout(5)
    public void test_aStopThatThrowsIsReportedToTheCaller() {
        messageForwarder.addForwarder(mqttForwarder);
        final RuntimeException stopFailure = new RuntimeException("persistence future failed while draining");
        doThrow(stopFailure).when(mqttForwarder).stop();

        final RuntimeException thrown =
                assertThrows(RuntimeException.class, () -> messageForwarder.removeForwarder(mqttForwarder, false));

        assertTrue(
                Arrays.asList(thrown.getSuppressed()).contains(stopFailure),
                "the teardown swallowed the reason it did not complete cleanly");
    }

    /**
     * The topic-tree removal runs before the stop, and it can throw too — a forwarder left subscribed to
     * a topic it can no longer serve is the same class of half-finished teardown.
     */
    @Test
    @Timeout(5)
    public void test_aTopicTreeRemovalThatThrowsStillReleasesQueueOwnership() {
        messageForwarder.addForwarder(mqttForwarder);
        doThrow(new RuntimeException("topic tree unavailable"))
                .when(topicTree)
                .removeSubscriber(anyString(), anyString(), anyString());

        assertThrows(RuntimeException.class, () -> messageForwarder.removeForwarder(mqttForwarder, false));

        assertFalse(
                messageForwarder.isForwarderQueue(QUEUE_ID),
                "a topic-tree failure stranded the queue ownership behind it");
    }

    /**
     * A stop that throws must not take the other forwarders' teardown with it either. This is the
     * per-forwarder guarantee {@code BridgeService.internalStopBridge} relies on when it continues to the
     * next forwarder after one reports a failure.
     */
    @Test
    @Timeout(5)
    public void test_aStopThatThrowsDoesNotStrandTheOtherForwardersQueues() {
        messageForwarder.addForwarder(mqttForwarder);
        messageForwarder.addForwarder(otherMqttForwarder);
        doThrow(new RuntimeException("persistence future failed while draining"))
                .when(mqttForwarder)
                .stop();

        assertThrows(RuntimeException.class, () -> messageForwarder.removeForwarder(mqttForwarder, false));
        messageForwarder.removeForwarder(otherMqttForwarder, false);

        assertFalse(messageForwarder.isForwarderQueue(QUEUE_ID), "the failing forwarder's queue stayed claimed");
        assertFalse(messageForwarder.isForwarderQueue(OTHER_QUEUE_ID), "the second forwarder's queue stayed claimed");
    }

    /**
     * Clearing a queue is fallible too — the persistence it calls is the same one whose failures make
     * {@code stop()} throw.
     */
    @Test
    @Timeout(5)
    public void test_aQueueClearThatThrowsStillReleasesQueueOwnership() {
        final ClientQueuePersistence queuePersistence = mock(ClientQueuePersistence.class);
        // registration polls the queue as soon as the forwarder is added; an empty read is all this
        // test needs from that path, and without it the poll fails before the removal under test runs
        when(queuePersistence.readShared(anyString(), anyInt(), anyLong()))
                .thenReturn(Futures.immediateFuture(ImmutableList.of()));
        doThrow(new RuntimeException("queue persistence unavailable"))
                .when(queuePersistence)
                .clear(anyString(), anyBoolean());
        final MessageForwarderImpl forwarderWithPersistence = new MessageForwarderImpl(
                topicTree,
                new HivemqId(),
                () -> queuePersistence,
                () -> subscriptionPersistence,
                mock(SingleWriterService.class),
                mock(ShutdownHooks.class));
        final MqttForwarder toRemove = forwarder(FORWARDER_ID, TOPIC);
        forwarderWithPersistence.addForwarder(toRemove);

        assertThrows(RuntimeException.class, () -> forwarderWithPersistence.removeForwarder(toRemove, true));

        assertFalse(
                forwarderWithPersistence.isForwarderQueue(QUEUE_ID),
                "a queue-clear failure stranded the ownership behind it");
    }
}
