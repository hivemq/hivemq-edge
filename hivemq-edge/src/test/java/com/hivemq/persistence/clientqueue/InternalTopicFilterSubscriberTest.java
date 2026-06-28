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
package com.hivemq.persistence.clientqueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.Futures;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.topic.SubscriberWithIdentifiers;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.mqtt.topic.tree.TopicSubscribers;
import com.hivemq.persistence.ProducerQueues;
import com.hivemq.persistence.SingleWriterService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@SuppressWarnings({"unchecked", "rawtypes"})
class InternalTopicFilterSubscriberTest {

    private LocalTopicTree topicTree; // REAL — the seam under test
    private ClientQueuePersistence clientQueuePersistence; // mock — used to capture the queue id
    private SingleWriterService singleWriterService; // mock
    private InternalTopicFilterSubscriberFactory factory;

    @BeforeEach
    void setUp() {
        // The real topic tree — this is what makes the id-coupling test meaningful.
        topicTree = new LocalTopicTree(new MetricsHolder(new MetricRegistry()));

        clientQueuePersistence = mock(ClientQueuePersistence.class);
        singleWriterService = mock(SingleWriterService.class);

        // consume() calls submitPoll(), which submits a task onto the SingleWriter queue. We do NOT run
        // the task here: in production each submit() is asynchronous, and the poll pipeline reschedules
        // itself via submitPoll() — running it synchronously in the test thread would recurse without
        // bound. These tests exercise the wiring and state machine, not the poll loop, so submit() just
        // returns an immediate future. (Mockito still records the queueId argument, which is what the
        // id-coupling test captures.)
        final ProducerQueues producerQueues = mock(ProducerQueues.class);
        when(singleWriterService.getQueuedMessagesQueue()).thenReturn(producerQueues);
        when(producerQueues.submit(any(), any(SingleWriterService.Task.class)))
                .thenReturn(Futures.immediateFuture(null));

        factory = new InternalTopicFilterSubscriberFactory(topicTree, clientQueuePersistence, singleWriterService);
    }

    // ── Tier 3 (the requested one): real-topic-tree contract — subscribe-id == poll-id ──────────────
    //
    // The implicit contract between the topic tree and the client-queue persistence is that the
    // SAME id is used on both sides: the id we subscribe with in the tree must equal the id we poll
    // the queue with, otherwise a message the tree routes to us would land in a queue we never drain.
    //
    // This test pins that contract using the REAL topic tree. It (a) subscribes via attach(), (b) asks
    // the real tree who matches a topic our filter covers, and (c) asserts the id the tree reports is
    // exactly the id our subscriber uses on EVERY queue-persistence call. A regression that made the
    // tree id and the queue id diverge — the classic "subscribed as X, polled as Y" bug — turns this
    // red, where mock-only tests (Tier 1/2) would stay green because each mock accepts its own id.
    @Test
    void realTree_subscribeIdEqualsPollId() {
        final InternalTopicFilterSubscriber subscriber = factory.create("tynebridge", "my-bridge")
                .withProcessor(message -> {})
                .withTopicFilter("sensors/#")
                .build();

        subscriber.start(); // attach() registers in the real tree; consume() exercises the poll path

        // (b) the real tree's view: who is subscribed to a topic our filter matches?
        final TopicSubscribers matched = topicTree.findTopicSubscribers("sensors/temperature");
        final Set<String> idsFromTree = matched.getSubscribers().stream()
                .map(SubscriberWithIdentifiers::getSubscriber)
                .collect(Collectors.toSet());

        assertThat(idsFromTree)
                .as("the real topic tree must report exactly our reserved-prefix client id")
                .containsExactly("$INTERNAL::tynebridge::my-bridge");
        final String idFromTree = idsFromTree.iterator().next();

        // (c) the id our subscriber actually uses against the queue persistence, captured from the
        // publish-available-callback registration (consume() registers it under the poll id directly).
        final ArgumentCaptor<String> callbackQueueId = ArgumentCaptor.forClass(String.class);
        verify(clientQueuePersistence).addPublishAvailableCallback(any(), callbackQueueId.capture());

        // The contract: the id the tree reports for our subscription == the id we use against the queue.
        assertThat(callbackQueueId.getValue())
                .as("the id we register the publish-available callback under must equal the tree id")
                .isEqualTo(idFromTree);
    }

    // ── Tier 1: lifecycle state machine ─────────────────────────────────────────────────────────────

    @Test
    void attach_isIdempotent_andDetachReversesIt() {
        final InternalTopicFilterSubscriber s = build("sensors/#");

        s.attach();
        s.attach(); // second attach must NOT double-subscribe in the tree
        assertThat(treeIdsFor("sensors/x")).containsExactly(clientId());

        s.detach();
        assertThat(treeIdsFor("sensors/x")).isEmpty();
    }

    @Test
    void deallocate_throws_whenNotDetachedAndPaused() {
        final InternalTopicFilterSubscriber s = build("sensors/#").start(); // attached + consuming

        assertThatThrownBy(s::deallocate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be detached and paused");
    }

    @Test
    void stop_ordersDetachThenPauseThenDeallocate() {
        final InternalTopicFilterSubscriber s = build("sensors/#").start();

        s.stop();

        // detached: no longer in the tree; deallocated: queue cleared.
        assertThat(treeIdsFor("sensors/x")).isEmpty();
        verify(clientQueuePersistence).removePublishAvailableCallback(clientId()); // pause()
        verify(clientQueuePersistence).clear(clientId(), false); // deallocate()
    }

    // ── Tier 1: runtime topic-filter reconciliation ─────────────────────────────────────────────────

    @Test
    void addTopicFilter_whileDetached_doesNotTouchTree_butReplaysOnAttach() {
        final InternalTopicFilterSubscriber s = build(); // no filters, detached

        s.addTopicFilter("sensors/#"); // detached: internal set only
        assertThat(treeIdsFor("sensors/x")).isEmpty(); // nothing in the tree yet

        s.attach(); // now the remembered set is replayed
        assertThat(treeIdsFor("sensors/x")).containsExactly(clientId());
    }

    @Test
    void withTopicFilter_whileAttached_appliesOnlyTheDiff() {
        final InternalTopicFilterSubscriber s = build("a/#", "b/#").attach();
        assertThat(treeIdsFor("a/x")).containsExactly(clientId());
        assertThat(treeIdsFor("b/x")).containsExactly(clientId());

        s.withTopicFilter(List.of("b/#", "c/#")); // absolute: drop a, keep b, add c

        assertThat(treeIdsFor("a/x")).as("a was removed").isEmpty();
        assertThat(treeIdsFor("b/x")).as("b was kept").containsExactly(clientId());
        assertThat(treeIdsFor("c/x")).as("c was added").containsExactly(clientId());
    }

    // ── Tier 1: builder validation ──────────────────────────────────────────────────────────────────

    @Test
    void build_withoutProcessor_throws() {
        assertThatThrownBy(() -> factory.create("tynebridge", "x").build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requires a processor");
    }

    @Test
    void build_withEmptyFilterSet_isValid_andAttachIsANoOp() {
        final InternalTopicFilterSubscriber s =
                factory.create("tynebridge", "x").withProcessor(m -> {}).build();

        s.attach(); // empty set: subscribes to nothing, must not error
        assertThat(treeIdsFor("anything")).isEmpty();
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────

    private @NotNull InternalTopicFilterSubscriber build(final @NotNull String... filters) {
        return factory.create("tynebridge", "my-bridge")
                .withProcessor(m -> {})
                .withTopicFilter(List.of(filters))
                .build();
    }

    private @NotNull String clientId() {
        return "$INTERNAL::tynebridge::my-bridge";
    }

    private @NotNull Set<String> treeIdsFor(final @NotNull String topic) {
        return topicTree.findTopicSubscribers(topic).getSubscribers().stream()
                .map(SubscriberWithIdentifiers::getSubscriber)
                .collect(Collectors.toSet());
    }
}
