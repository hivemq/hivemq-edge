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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.topic.SubscriberWithIdentifiers;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.SingleWriterService;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/// What a subscriber without a queue does, and what it deliberately does not.
///
/// The queue persistence and the single writer are mocked and asserted UNTOUCHED: a change that quietly routed
/// a message through the queue would still deliver it and pass every behavioural assertion here, so
/// [#noMessageEverReachesTheQueueOrTheSingleWriter] pins the absence directly. The topic tree is real, because
/// several tests assert what it was actually told.
class InternalTopicFilterSubscriberWithoutQueueTest {

    private @NotNull ClientQueuePersistence clientQueuePersistence;
    private @NotNull SingleWriterService singleWriterService;
    private @NotNull InternalTopicFilterSubscriberFactory factory;
    private @NotNull LocalTopicTree topicTree;

    @BeforeEach
    void setUp() {
        topicTree = new LocalTopicTree(new MetricsHolder(new MetricRegistry()));
        // Deliberately NOT stubbed. A queueless subscriber must never call either of these, and an unstubbed
        // mock makes an accidental call loud -- verifyNoInteractions below states it as an assertion too.
        clientQueuePersistence = mock(ClientQueuePersistence.class);
        singleWriterService = mock(SingleWriterService.class);
        factory = new InternalTopicFilterSubscriberFactory(topicTree, clientQueuePersistence, singleWriterService);
    }

    private @NotNull PUBLISH message(final @NotNull String payload) {
        return message("commands/setpoint", payload);
    }

    private @NotNull PUBLISH message(final @NotNull String topic, final @NotNull String payload) {
        return new PUBLISHFactory.Mqtt5Builder()
                .withHivemqId("edge1")
                .withTopic(topic)
                .withQoS(QoS.AT_LEAST_ONCE)
                .withOnwardQos(QoS.AT_LEAST_ONCE)
                .withPayload(payload.getBytes(StandardCharsets.UTF_8))
                .build();
    }

    private @NotNull String payloadOf(final @NotNull PUBLISH message) {
        return new String(message.getPayload(), StandardCharsets.UTF_8);
    }

    // -- delivery ------------------------------------------------------------------------------------------

    @Test
    void aDeliveredMessageReachesTheProcessorOnTheCallingThread() {
        final List<String> seen = new ArrayList<>();
        final Thread caller = Thread.currentThread();
        final List<Thread> processorThreads = new ArrayList<>();

        final InternalTopicFilterSubscriberWithoutQueue subscriber = factory.builderWithoutQueue("test", "direct")
                .withProcessor(m -> {
                    seen.add(payloadOf(m));
                    processorThreads.add(Thread.currentThread());
                })
                .withTopicFilter("commands/#")
                .build()
                .start();

        assertThat(subscriber.deliver(message("a"))).isTrue();

        assertThat(seen).containsExactly("a");
        assertThat(processorThreads)
                .as("no hand-off to another thread: the processor runs where the distributor was")
                .containsExactly(caller);
    }

    @Test
    void noMessageEverReachesTheQueueOrTheSingleWriter() {
        // The reason this class exists. Both collaborators are the queued variant's, and a queueless subscriber
        // that touched either would be paying the cost it was built to avoid.
        final InternalTopicFilterSubscriberWithoutQueue subscriber = factory.builderWithoutQueue("test", "noqueue")
                .withProcessor(m -> {})
                .withTopicFilter("commands/#")
                .build()
                .start();

        subscriber.deliver(message("a"));
        subscriber.deliver(message("b"));
        subscriber.stop();

        verifyNoInteractions(clientQueuePersistence);
        verifyNoInteractions(singleWriterService);
    }

    @Test
    void everyDeliveredMessageIsPassedOnWithNoBoundBetweenThem() {
        // The queued variant holds one message at a time; this one holds none, so several deliveries in flight
        // are all passed straight through. Stated as a test because it is the contract difference a consumer
        // must know about -- the processor here can be re-entered.
        final List<String> seen = new ArrayList<>();

        final InternalTopicFilterSubscriberWithoutQueue subscriber = factory.builderWithoutQueue("test", "nobound")
                .withProcessor(m -> seen.add(payloadOf(m)))
                .withTopicFilter("commands/#")
                .build()
                .start();

        subscriber.deliver(message("a"));
        subscriber.deliver(message("b"));
        subscriber.deliver(message("c"));

        assertThat(seen).containsExactly("a", "b", "c");
    }

    @Test
    void aThrowingProcessorIsReportedAsAFailedDeliveryAndDoesNotStopTheNextOne() {
        final AtomicInteger calls = new AtomicInteger();

        final InternalTopicFilterSubscriberWithoutQueue subscriber = factory.builderWithoutQueue("test", "throws")
                .withProcessor(m -> {
                    if (calls.incrementAndGet() == 1) {
                        throw new IllegalStateException("no");
                    }
                })
                .withTopicFilter("commands/#")
                .build()
                .start();

        assertThat(subscriber.deliver(message("a")))
                .as("a throwing processor is a failed delivery, not a silent success")
                .isFalse();
        assertThat(subscriber.deliver(message("b")))
                .as("and it does not poison the subscriber")
                .isTrue();
        assertThat(calls).hasValue(2);
    }

    @Test
    void aMessageIsRefusedBeforeStartAndAfterStop() {
        final List<String> seen = new ArrayList<>();

        final InternalTopicFilterSubscriberWithoutQueue subscriber = factory.builderWithoutQueue("test", "gate")
                .withProcessor(m -> seen.add(payloadOf(m)))
                .withTopicFilter("commands/#")
                .build();

        assertThat(subscriber.deliver(message("before")))
                .as("built but not started: the gate is shut")
                .isFalse();

        subscriber.start();
        assertThat(subscriber.deliver(message("during"))).isTrue();

        subscriber.pauseDetach();
        assertThat(subscriber.deliver(message("after")))
                .as("pauseDetach shuts the gate")
                .isFalse();

        assertThat(seen).containsExactly("during");
    }

    @Test
    void anExcludedIngressClientIdIsNotAskedToDeliver() {
        // The decision itself lives in the publish path, which consults this method before delivering; what is
        // pinned here is that a queueless subscriber answers it the same way a queued one does.
        final InternalTopicFilterSubscriberWithoutQueue subscriber = factory.builderWithoutQueue("test", "exclude")
                .withProcessor(m -> {})
                .withTopicFilter("commands/#")
                .withExcludedIngressClientId("the-writer")
                .build();

        assertThat(subscriber.isExcludedIngressClientId("the-writer")).isTrue();
        assertThat(subscriber.isExcludedIngressClientId("someone-else")).isFalse();
        assertThat(subscriber.isExcludedIngressClientId(null)).isFalse();
    }

    // -- the topic tree ------------------------------------------------------------------------------------

    @Test
    void startRegistersTheFiltersAndStopRemovesThem() {
        final InternalTopicFilterSubscriberWithoutQueue subscriber = factory.builderWithoutQueue("test", "tree")
                .withProcessor(m -> {})
                .withTopicFilter(List.of("commands/setpoint", "commands/mode"))
                .build();

        assertThat(subscribersOf("commands/setpoint"))
                .as("built, not started: nothing is in the tree yet")
                .isEmpty();

        subscriber.start();
        assertThat(subscribersOf("commands/setpoint")).containsExactly(subscriber.clientId());
        assertThat(subscribersOf("commands/mode")).containsExactly(subscriber.clientId());

        subscriber.pauseDetach();
        assertThat(subscribersOf("commands/setpoint")).isEmpty();
        assertThat(subscribersOf("commands/mode")).isEmpty();
    }

    @Test
    void aFilterAddedWhileRunningTakesEffectAtOnce() {
        final InternalTopicFilterSubscriberWithoutQueue subscriber = factory.builderWithoutQueue("test", "addlive")
                .withProcessor(m -> {})
                .withTopicFilter("commands/setpoint")
                .build()
                .start();

        subscriber.addTopicFilter("commands/mode");

        assertThat(subscribersOf("commands/mode")).containsExactly(subscriber.clientId());
    }

    @Test
    void aFilterAddedWhileStoppedIsReplayedByTheNextStart() {
        final InternalTopicFilterSubscriberWithoutQueue subscriber = factory.builderWithoutQueue("test", "replay")
                .withProcessor(m -> {})
                .withTopicFilter("commands/setpoint")
                .build();

        subscriber.addTopicFilter("commands/mode");
        assertThat(subscribersOf("commands/mode"))
                .as("not running, so nothing is registered yet")
                .isEmpty();

        subscriber.start();
        assertThat(subscribersOf("commands/mode"))
                .as("the remembered set is replayed in full")
                .containsExactly(subscriber.clientId());
    }

    @Test
    void withTopicFilterWhileRunningPushesOnlyTheDiff() {
        final InternalTopicFilterSubscriberWithoutQueue subscriber = factory.builderWithoutQueue("test", "diff")
                .withProcessor(m -> {})
                .withTopicFilter(List.of("a/1", "a/2"))
                .build()
                .start();

        subscriber.withTopicFilter(List.of("a/2", "a/3"));

        assertThat(subscribersOf("a/1")).as("dropped").isEmpty();
        assertThat(subscribersOf("a/2")).as("kept").containsExactly(subscriber.clientId());
        assertThat(subscribersOf("a/3")).as("added").containsExactly(subscriber.clientId());
    }

    @Test
    void aRemovedFilterStopsBeingSubscribed() {
        final InternalTopicFilterSubscriberWithoutQueue subscriber = factory.builderWithoutQueue("test", "remove")
                .withProcessor(m -> {})
                .withTopicFilter(List.of("a/1", "a/2"))
                .build()
                .start();

        subscriber.removeTopicFilter("a/1");

        assertThat(subscribersOf("a/1")).isEmpty();
        assertThat(subscribersOf("a/2")).containsExactly(subscriber.clientId());
    }

    // -- identity ------------------------------------------------------------------------------------------

    @Test
    void theClientIdCarriesTheSharedInternalPrefix() {
        // The same prefix as the queued variant, deliberately: it is what makes the connect-time reserved-id
        // check cover this class without being edited.
        final InternalTopicFilterSubscriberWithoutQueue subscriber = factory.builderWithoutQueue("combiner", "c-1")
                .withProcessor(m -> {})
                .build();

        assertThat(subscriber.clientId()).isEqualTo("$INTERNAL::combiner::c-1");
        assertThat(subscriber.clientId()).startsWith(InternalTopicFilterSubscriber.INTERNAL_SUBSCRIBER_PREFIX);
    }

    @Test
    void pauseDetachDoesNotFreeTheIdentityButDeallocateDoes() {
        // THE DISTINCTION MOST EASILY GOT WRONG. Stopping the flow and releasing the identity are different
        // things, and only the second makes the id reusable.
        final InternalTopicFilterSubscriberWithoutQueue subscriber = factory.builderWithoutQueue("test", "id")
                .withProcessor(m -> {})
                .withTopicFilter("commands/#")
                .build()
                .start();

        subscriber.pauseDetach();

        assertThat(factory.getSubscriberWithoutQueue(subscriber.clientId()))
                .as("still registered: pauseDetach stops delivery, it does not release the id")
                .isSameAs(subscriber);
        assertThatThrownBy(() -> factory.builderWithoutQueue("test", "id")
                        .withProcessor(m -> {})
                        .build())
                .as("so the id cannot be taken by another subscriber")
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already in use");

        subscriber.deallocate();

        assertThat(factory.getSubscriberWithoutQueue(subscriber.clientId()))
                .as("deallocate is what frees it")
                .isNull();
        final InternalTopicFilterSubscriberWithoutQueue reused =
                factory.builderWithoutQueue("test", "id").withProcessor(m -> {}).build();
        assertThat(reused.clientId()).isEqualTo(subscriber.clientId());
    }

    @Test
    void aPausedAndDetachedSubscriberCanBeStartedAgainUnderItsOwnId() {
        final List<String> seen = new ArrayList<>();
        final InternalTopicFilterSubscriberWithoutQueue subscriber = factory.builderWithoutQueue("test", "restart")
                .withProcessor(m -> seen.add(payloadOf(m)))
                .withTopicFilter("commands/#")
                .build()
                .start();

        subscriber.pauseDetach();
        subscriber.start();

        assertThat(subscribersOf("commands/setpoint")).containsExactly(subscriber.clientId());
        assertThat(subscriber.deliver(message("again"))).isTrue();
        assertThat(seen).containsExactly("again");
    }

    @Test
    void everyVerbThrowsAfterDeallocateExceptTheIdempotentOnes() {
        final InternalTopicFilterSubscriberWithoutQueue subscriber = factory.builderWithoutQueue("test", "dead")
                .withProcessor(m -> {})
                .withTopicFilter("commands/#")
                .build();
        subscriber.deallocate();

        assertThatThrownBy(subscriber::attachConsume).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(subscriber::pauseDetach).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> subscriber.addTopicFilter("x")).isInstanceOf(IllegalStateException.class);

        // The two exceptions, both idempotent no-ops rather than errors.
        subscriber.deallocate();
        subscriber.stop();

        assertThat(subscriber.deliver(message("a")))
                .as("and a dead subscriber delivers nothing")
                .isFalse();
    }

    @Test
    void deallocateRefusesToRunWhileStillDelivering() {
        // Not repaired silently: deallocating a live subscriber would leave its filters in the tree under an id
        // that no longer belongs to anyone. stop() is the composition that orders the two correctly.
        final InternalTopicFilterSubscriberWithoutQueue subscriber = factory.builderWithoutQueue("test", "live")
                .withProcessor(m -> {})
                .withTopicFilter("commands/#")
                .build()
                .start();

        assertThatThrownBy(subscriber::deallocate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("still delivering");

        subscriber.stop();
        assertThat(factory.getSubscriberWithoutQueue(subscriber.clientId())).isNull();
        assertThat(subscribersOf("commands/setpoint")).isEmpty();
    }

    @Test
    void theTwoKindsShareOneClientIdNamespaceAndCannotCollide() {
        // The two kinds live in separate maps, but in ONE client-id namespace: an identity taken by either is
        // unavailable to the other, and a lookup for the wrong kind answers null rather than mis-typing what it
        // found. The uniqueness guard is held apart from both maps for exactly this.
        final InternalTopicFilterSubscriberWithoutQueue queueless = factory.builderWithoutQueue("test", "shared")
                .withProcessor(m -> {})
                .build();

        assertThatThrownBy(() ->
                        factory.builder("test", "shared").withProcessor(m -> {}).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already in use");

        assertThat(factory.getSubscriberWithoutQueue(queueless.clientId())).isSameAs(queueless);
        assertThat(factory.getSubscriber(queueless.clientId()))
                .as("asked for the queued kind, this id holds the other one")
                .isNull();
    }

    @Test
    void anIdentityFreedByOneKindCanBeTakenByTheOther() {
        // The two kinds share one client-id namespace, so releasing an id must release it for both -- the
        // uniqueness guard lives apart from the per-kind maps precisely so this holds.
        final InternalTopicFilterSubscriberWithoutQueue queueless = factory.builderWithoutQueue("test", "handover")
                .withProcessor(m -> {})
                .build();
        queueless.deallocate();

        final InternalTopicFilterSubscriber queued =
                factory.builder("test", "handover").withProcessor(m -> {}).build();

        assertThat(queued.clientId()).isEqualTo(queueless.clientId());
        assertThat(factory.getSubscriber(queued.clientId())).isSameAs(queued);
        assertThat(factory.getSubscriberWithoutQueue(queued.clientId()))
                .as("and the freed queueless entry did not linger")
                .isNull();
    }

    // -- the builder ---------------------------------------------------------------------------------------

    @Test
    void buildWithoutAProcessorIsRejected() {
        assertThatThrownBy(() -> factory.builderWithoutQueue("test", "noproc").build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requires a processor");
    }

    @Test
    void buildWithTwoProcessorsIsRejected() {
        assertThatThrownBy(() -> factory.builderWithoutQueue("test", "twoproc")
                        .withProcessor(m -> {})
                        .withProcessor(m -> {}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("one processor, not two");
    }

    @Test
    void noFiltersAtAllIsValid() {
        final InternalTopicFilterSubscriberWithoutQueue subscriber = factory.builderWithoutQueue("test", "empty")
                .withProcessor(m -> {})
                .build()
                .start();

        assertThat(subscriber.deliver(message("a")))
                .as("nothing matches it, but delivering one by hand still works")
                .isTrue();
    }

    @Test
    void aBuilderReusedAfterBuildCannotReachIntoTheLiveSubscriber() {
        final InternalTopicFilterSubscriberWithoutQueue.Builder builder = factory.builderWithoutQueue("test", "copy")
                .withProcessor(m -> {})
                .withTopicFilter("a/1");
        final InternalTopicFilterSubscriberWithoutQueue subscriber = builder.build();

        builder.addTopicFilter("a/2");
        subscriber.start();

        assertThat(subscribersOf("a/1")).containsExactly(subscriber.clientId());
        assertThat(subscribersOf("a/2"))
                .as("the filter set was copied at build(), not shared")
                .isEmpty();
    }

    // -- threading -----------------------------------------------------------------------------------------

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void aProcessorMayCallALifecycleVerbOnItsOwnSubscriberWithoutDeadlocking() {
        // A processor calling straight back into a synchronized verb on its own subscriber. This much does NOT
        // prove that deliver() holds no monitor -- Java monitors are reentrant, so the same thread re-acquiring
        // one succeeds either way. It pins the behaviour (the verb takes effect), and
        // aDeliveryDoesNotWaitOnAVerbHeldByAnotherThread is what pins the monitor itself.
        final List<String> seen = new ArrayList<>();
        final InternalTopicFilterSubscriberWithoutQueue[] holder = new InternalTopicFilterSubscriberWithoutQueue[1];

        holder[0] = factory.builderWithoutQueue("test", "reentrant")
                .withProcessor(m -> {
                    seen.add(payloadOf(m));
                    holder[0].pauseDetach(); // straight back into a synchronized verb, from inside deliver()
                })
                .withTopicFilter("commands/#")
                .build()
                .start();

        assertThat(holder[0].deliver(message("a"))).isTrue();
        assertThat(seen).containsExactly("a");
        assertThat(holder[0].deliver(message("b")))
                .as("the processor's own pauseDetach took effect")
                .isFalse();
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void aDeliveryDoesNotWaitOnAVerbHeldByAnotherThread() throws Exception {
        // THIS is what pins "deliver() does not take the monitor", and it takes a second thread to do it: a
        // same-thread re-entry proves nothing, because Java monitors are reentrant.
        //
        // One thread sits inside a synchronized verb (addTopicFilter, held open by a processor-free route: the
        // verb itself is fast, so the monitor is instead held by a plain synchronized block on the subscriber,
        // which is the same monitor the verbs use). While it is held, another thread delivers. If deliver() were
        // synchronized it would block until the monitor was released, and the delivery would not complete.
        final CountDownLatch monitorHeld = new CountDownLatch(1);
        final CountDownLatch releaseMonitor = new CountDownLatch(1);
        final List<String> seen = java.util.Collections.synchronizedList(new ArrayList<>());

        final InternalTopicFilterSubscriberWithoutQueue subscriber = factory.builderWithoutQueue("test", "monitor")
                .withProcessor(m -> seen.add(payloadOf(m)))
                .withTopicFilter("commands/#")
                .build()
                .start();

        final Thread holder = new Thread(() -> {
            synchronized (subscriber) { // the very monitor the lifecycle and filter verbs synchronize on
                monitorHeld.countDown();
                try {
                    releaseMonitor.await(5, TimeUnit.SECONDS);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        holder.start();
        assertThat(monitorHeld.await(5, TimeUnit.SECONDS)).isTrue();

        final Thread deliverer = new Thread(() -> subscriber.deliver(message("a")));
        deliverer.start();
        deliverer.join(3_000);

        final boolean deliveredWhileMonitorHeld = seen.contains("a");
        releaseMonitor.countDown();
        holder.join();
        deliverer.join();

        assertThat(deliveredWhileMonitorHeld)
                .as("delivery completed while another thread held the subscriber's monitor,"
                        + " so deliver() does not take it")
                .isTrue();
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void twoThreadsMayDeliverConcurrently() throws Exception {
        // The contract this class puts on a processor -- thread-safe, not merely non-blocking. Both threads are
        // inside the processor at once, which is only observable if nothing serialises them.
        //
        // THE RENDEZVOUS IS THE ASSERTION, not the count afterwards. Each thread reports its own await, and both
        // must succeed: if delivery were serialised the first would time out waiting for a second that cannot
        // enter, and `arrivals` would not hold two trues. An earlier version asserted only the latch count at
        // the end, which a serialised implementation ALSO satisfies -- both threads do eventually run and count
        // down -- so it passed against a synchronized deliver() and merely took the timeout to do it.
        final CountDownLatch bothInside = new CountDownLatch(2);
        final List<Boolean> arrivals = java.util.Collections.synchronizedList(new ArrayList<>());
        final InternalTopicFilterSubscriberWithoutQueue subscriber = factory.builderWithoutQueue("test", "concurrent")
                .withProcessor(m -> {
                    bothInside.countDown();
                    try {
                        // Each waits for the other, so both succeeding proves the two ran at the same time.
                        arrivals.add(bothInside.await(2, TimeUnit.SECONDS));
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                })
                .withTopicFilter("commands/#")
                .build()
                .start();

        final Thread one = new Thread(() -> subscriber.deliver(message("a")));
        final Thread two = new Thread(() -> subscriber.deliver(message("b")));
        one.start();
        two.start();
        one.join();
        two.join();

        assertThat(arrivals)
                .as("both processors were inside at the same time -- nothing here serialises delivery")
                .containsExactly(true, true);
    }

    /// The client ids the real topic tree reports as subscribed to a concrete topic.
    private @NotNull Set<String> subscribersOf(final @NotNull String topic) {
        return topicTree.findTopicSubscribers(topic).getSubscribers().stream()
                .map(SubscriberWithIdentifiers::getSubscriber)
                .collect(Collectors.toSet());
    }
}
