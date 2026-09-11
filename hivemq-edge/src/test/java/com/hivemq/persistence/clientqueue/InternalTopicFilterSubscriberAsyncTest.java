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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.ImmutableIntArray;
import com.google.common.util.concurrent.Futures;
import com.hivemq.configuration.service.MqttConfigurationService.QueuedMessagesStrategy;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.ProducerQueues;
import com.hivemq.persistence.SingleWriterService;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

/// The one-message-in-flight bound, and what it rests on.
///
/// **These drive the real poll pipeline**, unlike the sibling test class which deliberately does not: the
/// single writer is stubbed to run submitted work on the calling thread, so a whole read-process-release cycle
/// happens synchronously and can be observed step by step.
///
/// What every test here pins is the same claim from a different angle: **the next message is read only once the
/// processor is done with the previous one**. A subscriber that reads ahead has lost the bound; a subscriber
/// that never reads again has stalled. Both failures are silent in production, which is why they are worth
/// pinning here.
class InternalTopicFilterSubscriberAsyncTest {

    private @NotNull ClientQueuePersistence clientQueuePersistence;
    private @NotNull InternalTopicFilterSubscriberFactory factory;
    private @NotNull LocalTopicTree topicTree; // REAL -- several tests assert what the tree was actually told

    /// Every message the stubbed queue has been asked for, so a test can count reads.
    private final @NotNull AtomicInteger reads = new AtomicInteger();

    /// The messages the queue will hand out, in order; empty once exhausted.
    private final @NotNull List<PUBLISH> queued = new ArrayList<>();

    @BeforeEach
    void setUp() {
        topicTree = new LocalTopicTree(new MetricsHolder(new MetricRegistry()));
        clientQueuePersistence = mock(ClientQueuePersistence.class);
        final SingleWriterService singleWriterService = mock(SingleWriterService.class);

        // The single writer runs submitted work on the calling thread, so a test drives the whole pipeline
        // synchronously. The sibling test class stubs this to do nothing, because it is testing the state
        // machine rather than the loop; here the loop is the subject.
        final ProducerQueues producerQueues = mock(ProducerQueues.class);
        when(singleWriterService.getQueuedMessagesQueue()).thenReturn(producerQueues);
        when(producerQueues.submit(anyString(), any())).thenAnswer(invocation -> {
            final SingleWriterService.Task<?> task = invocation.getArgument(1);
            return task.doTask(0);
        });

        when(clientQueuePersistence.readNew(anyString(), any(Boolean.class), any(ImmutableIntArray.class), anyLong()))
                .thenAnswer(invocation -> {
                    reads.incrementAndGet();
                    if (queued.isEmpty()) {
                        return Futures.immediateFuture(ImmutableList.of());
                    }
                    return Futures.immediateFuture(ImmutableList.of(queued.remove(0)));
                });
        when(clientQueuePersistence.remove(anyString(), anyInt())).thenReturn(Futures.immediateFuture(null));
        // Stubbed as well, so a test that pins WHICH removal is used fails on the assertion rather than on a
        // null future from an unstubbed mock.
        when(clientQueuePersistence.removeShared(anyString(), anyString())).thenReturn(Futures.immediateFuture(null));

        factory = new InternalTopicFilterSubscriberFactory(topicTree, clientQueuePersistence, singleWriterService);
    }

    private @NotNull PUBLISH message(final @NotNull String payload) {
        return new PUBLISHFactory.Mqtt5Builder()
                .withHivemqId("edge1")
                .withTopic("commands/setpoint")
                .withQoS(QoS.AT_LEAST_ONCE)
                .withOnwardQos(QoS.AT_LEAST_ONCE)
                .withPayload(payload.getBytes(StandardCharsets.UTF_8))
                .build();
    }

    @Test
    void aSynchronousProcessorBehavesExactlyAsBefore() {
        final List<String> seen = new ArrayList<>();
        queued.add(message("a"));
        queued.add(message("b"));

        factory.builder("test", "sync")
                .withProcessor(m -> seen.add(new String(m.getPayload(), StandardCharsets.UTF_8)))
                .withTopicFilter("commands/#")
                .build()
                .consume();

        assertThat(seen).containsExactly("a", "b");
    }

    @Test
    void anAsynchronousProcessorHoldsTheQueueUntilItsFutureCompletes() {
        // The claim this whole extension exists for. The processor takes the first message and does not
        // answer; nothing further may be read until it does.
        final List<CompletableFuture<Void>> outstanding = new ArrayList<>();
        final List<String> seen = new ArrayList<>();
        queued.add(message("a"));
        queued.add(message("b"));

        factory.builder("test", "async")
                .withAsyncProcessor(m -> {
                    seen.add(new String(m.getPayload(), StandardCharsets.UTF_8));
                    final CompletableFuture<Void> completion = new CompletableFuture<>();
                    outstanding.add(completion);
                    return completion;
                })
                .withTopicFilter("commands/#")
                .build()
                .consume();

        assertThat(seen)
                .as("only the first message, because the first has not been answered")
                .containsExactly("a");

        outstanding.get(0).complete(null);
        assertThat(seen)
                .as("answering releases the queue and the next message arrives")
                .containsExactly("a", "b");
    }

    @Test
    void aWakeUpWhileAMessageIsInFlightDoesNotHandOutASecondOne() {
        // The bound used to rest on the completion chain alone: the release polls again, so the NEXT message
        // waits. But that is only one of the ways a poll starts. A message arriving fires the publish-available
        // callback, which polls independently -- and with the first future still pending, the processor was
        // handed a second message. Reported by Sam on #1752.
        //
        // Reproduced here by firing the callback, which is exactly what the queue persistence does when a
        // message is queued for a client that is already consuming.
        final List<CompletableFuture<Void>> outstanding = new ArrayList<>();
        final List<String> seen = new ArrayList<>();
        queued.add(message("a"));

        final ArgumentCaptor<ClientQueuePersistence.PublishAvailableCallback> callback =
                ArgumentCaptor.forClass(ClientQueuePersistence.PublishAvailableCallback.class);

        final InternalTopicFilterSubscriber subscriber = factory.builder("test", "wake-up-while-in-flight")
                .withAsyncProcessor(m -> {
                    seen.add(new String(m.getPayload(), StandardCharsets.UTF_8));
                    final CompletableFuture<Void> completion = new CompletableFuture<>();
                    outstanding.add(completion);
                    return completion;
                })
                .withTopicFilter("commands/#")
                .build();
        subscriber.consume();

        verify(clientQueuePersistence).addPublishAvailableCallback(callback.capture(), anyString());
        assertThat(seen).as("the first message is being processed").containsExactly("a");

        // A second message arrives while the first is still in flight, and wakes the poller.
        queued.add(message("b"));
        callback.getValue().onPublishAvailable(subscriber.clientId());

        assertThat(seen)
                .as("the wake-up must not hand out a second message while the first is unfinished")
                .containsExactly("a");

        outstanding.get(0).complete(null);
        assertThat(seen)
                .as("and the second arrives once the first has finished")
                .containsExactly("a", "b");
    }

    @Test
    void anEmptyPollLeavesTheSubscriberAbleToPollAgain() {
        // The in-flight claim is taken BEFORE the queue is read, because the read runs as a task of its own and
        // claiming afterwards would leave a gap for a second poll. The price is that an empty read has to
        // release the claim itself: no message means no completion, so nothing else ever would, and the
        // subscriber would go permanently deaf -- accepting messages into its queue and never reading them.
        final List<String> seen = new ArrayList<>();

        final ArgumentCaptor<ClientQueuePersistence.PublishAvailableCallback> callback =
                ArgumentCaptor.forClass(ClientQueuePersistence.PublishAvailableCallback.class);

        // Nothing queued, so the drain that consume() starts reads an empty queue.
        final InternalTopicFilterSubscriber subscriber = factory.builder("test", "empty-then-message")
                .withProcessor(m -> seen.add(new String(m.getPayload(), StandardCharsets.UTF_8)))
                .withTopicFilter("commands/#")
                .build();
        subscriber.consume();

        assertThat(seen).as("nothing to read yet").isEmpty();

        verify(clientQueuePersistence).addPublishAvailableCallback(callback.capture(), anyString());
        queued.add(message("a"));
        callback.getValue().onPublishAvailable(subscriber.clientId());

        assertThat(seen)
                .as("the empty read released its claim, so this wake-up is acted on")
                .containsExactly("a");
    }

    @Test
    void aCompletionArrivingAfterStopDoesNotReadFromTheQueue() {
        // stop() FREES THE CLIENT ID, so a replacement subscriber may already own the queue under that name.
        // An outstanding future completing afterwards used to acknowledge and poll regardless -- the discarded
        // object reading a command meant for its successor, which then never sees it. Reported by Sam on #1752.
        //
        // Covered by the same clause as everything else rather than a check of its own: stop() pauses before
        // deallocating, so a dead subscriber is one that is no longer consuming, and pollUnlessBusy() declines.
        final List<CompletableFuture<Void>> outstanding = new ArrayList<>();
        final List<String> seen = new ArrayList<>();
        queued.add(message("a"));

        final InternalTopicFilterSubscriber subscriber = factory.builder("test", "completion-after-stop")
                .withAsyncProcessor(m -> {
                    seen.add(new String(m.getPayload(), StandardCharsets.UTF_8));
                    final CompletableFuture<Void> completion = new CompletableFuture<>();
                    outstanding.add(completion);
                    return completion;
                })
                .withTopicFilter("commands/#")
                .build();
        subscriber.consume();

        assertThat(seen).as("the first message is in flight").containsExactly("a");

        subscriber.stop();

        // A message queued under that client id after the identity was freed -- as a replacement's would be.
        queued.add(message("b"));
        final int readsBeforeCompletion = reads.get();
        outstanding.get(0).complete(null);

        assertThat(seen).as("the dead subscriber processes nothing further").containsExactly("a");
        assertThat(reads.get())
                .as("and does not read the queue at all, which a replacement may now own")
                .isEqualTo(readsBeforeCompletion);
    }

    @Test
    void aProcessedQoS1MessageIsRemovedFromTheNonSharedQueue() {
        // Nothing acknowledges on an internal subscriber's behalf, so this one call is all that ever removes a
        // message -- and reading does not, above QoS 0: the store leaves the message stamped in flight.
        //
        // From EDG-504 until now it called removeShared, which searches the SHARED-subscription map. This
        // subscriber's queue is in the client map, so that found nothing and returned silently: every QoS 1 and
        // 2 message stayed stamped for ever, skipped by later reads, until the queue filled. Reported by Sam
        // on #1752 as pre-existing, and confirmed on the first commit of the class.
        queued.add(message("a"));

        final InternalTopicFilterSubscriber subscriber = factory.builder("test", "qos1-removal")
                .withProcessor(m -> {})
                .withTopicFilter("commands/#")
                .build();
        subscriber.consume();

        verify(clientQueuePersistence)
                .remove(subscriber.clientId(), ClientQueuePersistenceImpl.SHARED_IN_FLIGHT_MARKER);
        verify(clientQueuePersistence, never()).removeShared(anyString(), anyString());
    }

    @Test
    void aProcessedQoS0MessageIsNotRemovedTwice() {
        // Reading a QoS 0 message takes it out of the store, so there is nothing left to delete. Asking anyway
        // would be harmless but would say something false about where the message lives.
        queued.add(new PUBLISHFactory.Mqtt5Builder()
                .withHivemqId("edge1")
                .withTopic("commands/setpoint")
                .withQoS(QoS.AT_MOST_ONCE)
                .withOnwardQos(QoS.AT_MOST_ONCE)
                .withPayload("a".getBytes(StandardCharsets.UTF_8))
                .build());

        final List<String> seen = new ArrayList<>();
        factory.builder("test", "qos0-removal")
                .withProcessor(m -> seen.add(new String(m.getPayload(), StandardCharsets.UTF_8)))
                .withTopicFilter("commands/#")
                .build()
                .consume();

        assertThat(seen).as("the message was delivered").containsExactly("a");
        verify(clientQueuePersistence, never()).remove(anyString(), anyInt());
    }

    @Test
    void aFailedFutureReleasesTheQueueExactlyAsASuccessDoes() {
        // Success and failure are alike in the one respect that matters: both mean "no longer in flight".
        // A subscriber that released only on success would stop for good on the first failure.
        final List<String> seen = new ArrayList<>();
        queued.add(message("a"));
        queued.add(message("b"));

        factory.builder("test", "failing")
                .withAsyncProcessor(m -> {
                    seen.add(new String(m.getPayload(), StandardCharsets.UTF_8));
                    return CompletableFuture.failedFuture(new IllegalStateException("device said no"));
                })
                .withTopicFilter("commands/#")
                .build()
                .consume();

        assertThat(seen).containsExactly("a", "b");
    }

    @Test
    void aSynchronousProcessorThatThrowsStillReleasesTheQueue() {
        // The wrapper's catch. Without it the exception escapes before any future exists, nothing is
        // attached to release the queue, and this subscriber stops permanently and silently.
        final List<String> seen = new ArrayList<>();
        queued.add(message("a"));
        queued.add(message("b"));

        factory.builder("test", "throwing")
                .withProcessor(m -> {
                    seen.add(new String(m.getPayload(), StandardCharsets.UTF_8));
                    throw new IllegalStateException("consumer blew up");
                })
                .withTopicFilter("commands/#")
                .build()
                .consume();

        assertThat(seen).containsExactly("a", "b");
    }

    @Test
    void anAsynchronousProcessorThatThrowsInsteadOfReturningIsContained() {
        // The contract is to report through the future. A throw would otherwise leave nothing to chain the
        // release off, so it is caught and treated as a failed message.
        final List<String> seen = new ArrayList<>();
        queued.add(message("a"));
        queued.add(message("b"));

        factory.builder("test", "badasync")
                .withAsyncProcessor(m -> {
                    seen.add(new String(m.getPayload(), StandardCharsets.UTF_8));
                    throw new IllegalStateException("threw instead of returning");
                })
                .withTopicFilter("commands/#")
                .build()
                .consume();

        assertThat(seen).containsExactly("a", "b");
    }

    // -- routing --------------------------------------------------------------------------------------

    /// A consumer's own context: a filter and what it means to that consumer.
    ///
    /// A record, so equality is component-wise, which is what the deduplication and the removal both turn on.
    /// The subscriber asks only that a context *have* an identity, never how it is defined -- a real consumer
    /// whose contexts carry a key of their own is free to make that key the whole of it, as the southbound
    /// write path does.
    private record Destination(
            @NotNull String topicFilter, @NotNull String name)
            implements InternalTopicFilterSubscriber.TopicFilterContext {}

    private static @NotNull Destination to(final @NotNull String topicFilter, final @NotNull String name) {
        return new Destination(topicFilter, name);
    }

    /// A message on the default topic. Which filters it matched is NOT stated here: the subscriber works that
    /// out when it polls, by matching this topic against its own filters, so a message carries only its topic.
    private @NotNull PUBLISH messageMatching(final @NotNull String payload) {
        return messageOn("commands/setpoint", payload);
    }

    /// A message on a given topic, for pinning which of several filters it does and does not match.
    private @NotNull PUBLISH messageOn(final @NotNull String topic, final @NotNull String payload) {
        return new PUBLISHFactory.Mqtt5Builder()
                .withHivemqId("edge1")
                .withTopic(topic)
                .withQoS(QoS.AT_LEAST_ONCE)
                .withOnwardQos(QoS.AT_LEAST_ONCE)
                .withPayload(payload.getBytes(StandardCharsets.UTF_8))
                .build();
    }

    @Test
    void twoDestinationsSharingOneFilterBothReceiveTheMessage() {
        // The case the topic tree cannot express itself: registering one filter twice REPLACES the first
        // subscription, so both destinations would be reachable through a single identifier or not at all.
        // The fan-out therefore lives in the subscriber, and this is what pins it.
        //
        // Southbound, these two are one adapter::tag with TWO southbound mappings on one topic -- alike in
        // everything the mapping holds, and told apart only by a key the consumer minted. Both must arrive:
        // two mappings mean two writes, whatever they have in common. Hence the deduplication below is over
        // the WHOLE context, so a consumer that needs two of them kept apart can say so by putting something
        // distinguishing in it.
        final List<Destination> matched = new ArrayList<>();
        queued.add(messageMatching("a"));

        factory.builder("test", "shared-filter")
                .addTopicFilterContext(to("commands/#", "mapping-A"))
                .addTopicFilterContext(to("commands/#", "mapping-B"))
                .<Destination>withAsyncContextProcessor((m, destinations) -> {
                    matched.addAll(destinations);
                    return CompletableFuture.completedFuture(null);
                })
                .build()
                .consume();

        assertThat(matched).containsExactlyInAnyOrder(to("commands/#", "mapping-A"), to("commands/#", "mapping-B"));
    }

    @Test
    void oneContextRegisteredTwiceIsDeliveredOnce() {
        // The union is over the CONTEXTS, not the identifiers. Two identifiers come back when both filters
        // match, and doing the same destination's work twice for one message would, for a device write, send
        // the same command twice. Equal contexts therefore collapse -- which is what makes the record's
        // component-wise equality part of the contract rather than a convenience.
        //
        // ONE identifier here, not two: both registrations name the same filter, so both sit behind the one
        // identifier that filter was assigned. The deduplication is within that list.
        final List<Destination> matched = new ArrayList<>();
        queued.add(messageMatching("a"));

        factory.builder("test", "two-filters")
                .addTopicFilterContext(to("commands/#", "the-same-mapping"))
                .addTopicFilterContext(to("commands/#", "the-same-mapping"))
                .<Destination>withAsyncContextProcessor((m, destinations) -> {
                    matched.addAll(destinations);
                    return CompletableFuture.completedFuture(null);
                })
                .build()
                .consume();

        assertThat(matched).containsExactly(to("commands/#", "the-same-mapping"));
    }

    @Test
    void severalMatchingFiltersYieldTheirSeveralDestinations() {
        // "other/#" is excluded because it does not MATCH the message's topic -- the subscriber works that out
        // from the topic itself, rather than being told which filters matched.
        final List<Destination> matched = new ArrayList<>();
        queued.add(messageMatching("a"));

        factory.builder("test", "several")
                .addTopicFilterContext(to("commands/#", "first"))
                .addTopicFilterContext(to("other/#", "second"))
                .addTopicFilterContext(to("commands/setpoint", "third"))
                .<Destination>withAsyncContextProcessor((m, destinations) -> {
                    matched.addAll(destinations);
                    return CompletableFuture.completedFuture(null);
                })
                .build()
                .consume();

        assertThat(matched).containsExactlyInAnyOrder(to("commands/#", "first"), to("commands/setpoint", "third"));
    }

    @Test
    void aListOfContextsRegistersEveryOne() {
        // The list form is what a consumer with a configured set of mappings actually calls, and it exists
        // under its own name because withTopicFilter(List<String>) and a List of contexts have the same
        // erasure -- they could not be two overloads of one name.
        final List<Destination> matched = new ArrayList<>();
        queued.add(messageMatching("a"));

        factory.builder("test", "list-form")
                .withTopicFilterContext(List.of(to("commands/#", "first"), to("commands/setpoint", "second")))
                .<Destination>withAsyncContextProcessor((m, destinations) -> {
                    matched.addAll(destinations);
                    return CompletableFuture.completedFuture(null);
                })
                .build()
                .consume();

        assertThat(matched).containsExactlyInAnyOrder(to("commands/#", "first"), to("commands/setpoint", "second"));
    }

    @Test
    void removingOneContextLeavesTheFilterForTheOthers() {
        // Removal is by EQUALITY, and it takes away one destination rather than the filter: the other
        // destination on that filter must go on receiving. removeTopicFilter, by contrast, drops the filter
        // and everything behind it.
        final List<Destination> matched = new ArrayList<>();
        queued.add(messageMatching("a"));

        factory.builder("test", "remove-one")
                .addTopicFilterContext(to("commands/#", "keep"))
                .addTopicFilterContext(to("commands/#", "drop"))
                // A DIFFERENT but equal object, so this can only work by equals() rather than by identity.
                .removeTopicFilterContext(to("commands/#", "drop"))
                .<Destination>withAsyncContextProcessor((m, destinations) -> {
                    matched.addAll(destinations);
                    return CompletableFuture.completedFuture(null);
                })
                .build()
                .consume();

        assertThat(matched).containsExactly(to("commands/#", "keep"));
    }

    @Test
    void mixingContextsAndBareFiltersIsRejected() {
        // Half a routing table hands the consumer an empty set for the bare filters -- "this matched
        // something you did not name" -- which is a trap rather than a feature.
        assertThatThrownBy(() -> factory.builder("test", "mixed")
                        .addTopicFilterContext(to("commands/#", "routed"))
                        .addTopicFilter("other/#")
                        .withAsyncContextProcessor((m, d) -> CompletableFuture.completedFuture(null))
                        .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("other/#");
    }

    @Test
    void contextsWithoutAContextProcessorAreRejected() {
        assertThatThrownBy(() -> factory.builder("test", "no-routing-processor")
                        .addTopicFilterContext(to("commands/#", "routed"))
                        .withProcessor(m -> {})
                        .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("withAsyncContextProcessor");
    }

    @Test
    void twoProcessorsAreRejected() {
        assertThatThrownBy(() -> factory.builder("test", "two-processors")
                        .withProcessor(m -> {})
                        .withAsyncContextProcessor((m, d) -> CompletableFuture.completedFuture(null))
                        .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("one processor");
    }

    @Test
    void aFilterStillDeliversItsContextsAcrossDetachAndAttach() {
        // A message queued BEFORE a detach and read after it must still reach its destination. Under the old
        // scheme this was the one path that broke: the message carried a subscription identifier stamped on it
        // at enqueue time, and a filter re-registered with a different number stranded it. Matching at POLL
        // time removes the failure mode rather than working around it -- there is no number to go stale.
        final List<Destination> matched = new ArrayList<>();
        final InternalTopicFilterSubscriber subscriber = factory.builder("test", "detach-reattach")
                .addTopicFilterContext(to("commands/#", "mapping-A"))
                .<Destination>withAsyncContextProcessor((m, destinations) -> {
                    matched.addAll(destinations);
                    return CompletableFuture.completedFuture(null);
                })
                .build();

        subscriber.attach();
        // Queued while attached the FIRST time, and not read until after the re-attach.
        queued.add(messageMatching("a"));

        subscriber.detach();
        subscriber.attach();
        subscriber.consume();

        assertThat(matched)
                .as("a message queued before the detach still reaches its destination after the re-attach")
                .containsExactly(to("commands/#", "mapping-A"));
    }

    /// Whether the real topic tree has this subscriber registered for a topic matching the default filter.
    private boolean isRegisteredInTree() {
        return !topicTree
                .findTopicSubscribers("commands/setpoint")
                .getSubscribers()
                .isEmpty();
    }

    // -- the context filter verbs at RUNTIME ------------------------------------------------------------
    //
    // These were build-time only, so a live subscriber could not take on a new destination at all. Each test
    // here pins one half of why the runtime family exists.

    @Test
    void aContextAddedAtRuntimeReceivesItsMessages() {
        // The whole point of the runtime family: a destination added to a LIVE subscriber is reachable.
        final List<Destination> matched = new ArrayList<>();

        final InternalTopicFilterSubscriber subscriber = factory.builder("test", "runtime-add")
                .<Destination>withAsyncContextProcessor((m, destinations) -> {
                    matched.addAll(destinations);
                    return CompletableFuture.completedFuture(null);
                })
                .build()
                .attach();

        subscriber.addTopicFilterContext(to("commands/#", "added-later"));
        assertThat(isRegisteredInTree())
                .as("a context added at runtime subscribes its filter")
                .isTrue();

        queued.add(messageMatching("a"));
        subscriber.consume();

        assertThat(matched).containsExactly(to("commands/#", "added-later"));
    }

    @Test
    void removingTheLastContextOfAFilterUnsubscribesIt() {
        // Two destinations on one filter: removing one leaves the filter, removing the other takes it away.
        // A filter kept with no contexts behind it would accept messages into the queue and then drop them.
        final InternalTopicFilterSubscriber subscriber = factory.builder("test", "runtime-remove")
                .addTopicFilterContext(to("commands/#", "first"))
                .addTopicFilterContext(to("commands/#", "second"))
                .withAsyncContextProcessor((m, destinations) -> CompletableFuture.completedFuture(null))
                .build()
                .attach();

        subscriber.removeTopicFilterContext(to("commands/#", "first"));
        assertThat(isRegisteredInTree())
                .as("the filter stays while another context still wants it")
                .isTrue();

        subscriber.removeTopicFilterContext(to("commands/#", "second"));
        assertThat(isRegisteredInTree())
                .as("and goes when the last one is removed")
                .isFalse();
    }

    @Test
    void aRuntimeAddedFilterStillDeliversAcrossDetachAndAttach() {
        // Same guarantee as for a build-time filter, and it has to hold for these too.
        final List<Destination> matched = new ArrayList<>();
        final InternalTopicFilterSubscriber subscriber = factory.builder("test", "runtime-stability")
                .<Destination>withAsyncContextProcessor((m, destinations) -> {
                    matched.addAll(destinations);
                    return CompletableFuture.completedFuture(null);
                })
                .build()
                .attach();

        subscriber.addTopicFilterContext(to("commands/#", "added-later"));
        queued.add(messageMatching("a"));

        subscriber.detach();
        subscriber.attach();
        subscriber.consume();

        assertThat(matched)
                .as("a runtime-added filter goes on delivering after a detach/attach")
                .containsExactly(to("commands/#", "added-later"));
    }

    @Test
    void aMessageQueuedUnderARemovedFilterResolvesToNothing() {
        // A message may sit in the queue while the filter that admitted it is removed and a DIFFERENT filter
        // is added. It must not be delivered to the new filter's destination -- that would be a write to the
        // wrong device. Because matching happens at poll time against the filters that exist THEN, the
        // message simply matches nothing that wants it.
        final List<Destination> matched = new ArrayList<>();
        final InternalTopicFilterSubscriber subscriber = factory.builder("test", "no-misdelivery")
                .<Destination>withAsyncContextProcessor((m, destinations) -> {
                    matched.addAll(destinations);
                    return CompletableFuture.completedFuture(null);
                })
                .build()
                .attach();

        subscriber.addTopicFilterContext(to("commands/#", "first"));
        queued.add(messageMatching("a"));

        subscriber.removeTopicFilterContext(to("commands/#", "first"));
        subscriber.addTopicFilterContext(to("telemetry/#", "second"));
        subscriber.consume();

        assertThat(matched)
                .as("the queued message does not reach the destination that replaced the one it matched")
                .isEmpty();
    }

    @Test
    void aSynchronousContextProcessorIsToldWhatMatchedAndNeedsNoFuture() {
        // The fourth cell of the grid: being told what a message matched and needing a future are independent
        // choices, so a consumer whose per-destination work is small says so by returning nothing at all.
        final List<Destination> matched = new ArrayList<>();
        queued.add(messageMatching("a"));
        queued.add(messageMatching("b"));

        factory.builder("test", "sync-context")
                .addTopicFilterContext(to("commands/#", "mapping-A"))
                .<Destination>withContextProcessor((m, destinations) -> matched.addAll(destinations))
                .build()
                .consume();

        assertThat(matched)
                .as("both messages are handled, so returning released the queue just as a future would")
                .containsExactly(to("commands/#", "mapping-A"), to("commands/#", "mapping-A"));
    }

    @Test
    void aThrowingSynchronousContextProcessorStillReleasesTheQueue() {
        // Same guarantee the plain synchronous form has: a throwing consumer does not stall the queue.
        //
        // Two layers deliver that, and this pins the OUTCOME rather than either layer: the builder wraps the
        // throw into a failed future, and processPublish catches anything that escapes a processor anyway.
        // Removing the wrapper's catch alone leaves this test green -- verified -- because the outer catch
        // still releases. What differs is the log line, which this does not assert on.
        final List<Destination> matched = new ArrayList<>();
        queued.add(messageMatching("a"));
        queued.add(messageMatching("b"));

        factory.builder("test", "sync-context-throwing")
                .addTopicFilterContext(to("commands/#", "mapping-A"))
                .<Destination>withContextProcessor((m, destinations) -> {
                    matched.addAll(destinations);
                    throw new RuntimeException("boom");
                })
                .build()
                .consume();

        assertThat(matched)
                .as("the second message arrives, so the first did not stall the queue")
                .hasSize(2);
    }

    // -- a processor is never called with the subscriber's monitor held --------------------------------
    //
    // Submitting a poll can run the whole cycle INLINE on the calling thread, so a submit made inside a
    // synchronized verb would call the component's processor inside the monitor. These two pin the split
    // that prevents it, each from the side that would have failed.

    @Test
    void aProcessorThatPausesDuringTheFirstDrainActuallyPauses() {
        // consume() used to set `consuming` AFTER submitting the drain. Running inline -- which the in-memory
        // single writer does -- the processor saw consuming == false, so pause() hit its "not consuming"
        // guard and returned a no-op, and consume() then set the flag to true. The component believed it had
        // paused and had not: the callback stayed armed, so every later message woke the poller.
        //
        // Two things had to be true for a pause from inside a processor to mean anything, and neither was:
        // the flag had to be set before the drain (above), and the poll pipeline had to READ it. It did not --
        // `consuming` was written by consume()/pause() and consulted by nothing on this path, so a pause
        // removed the wake-up callback and left the drain running to the end of the queue.
        final List<String> seen = new ArrayList<>();
        queued.add(message("a"));
        queued.add(message("b"));

        // The processor needs its own subscriber, which does not exist until build() returns.
        final AtomicReference<InternalTopicFilterSubscriber> self = new AtomicReference<>();
        final InternalTopicFilterSubscriber subscriber = factory.builder("test", "pause-from-processor")
                .withProcessor(m -> {
                    seen.add(new String(m.getPayload(), StandardCharsets.UTF_8));
                    self.get().pause();
                })
                .withTopicFilter("commands/#")
                .build();
        self.set(subscriber);

        subscriber.consume();

        assertThat(seen)
                .as("pausing stops the drain in progress, not merely future wake-ups")
                .containsExactly("a");
        // And the pause took effect on the callback too, which is what it always did.
        verify(clientQueuePersistence).removePublishAvailableCallback(subscriber.clientId());
    }

    @Test
    void aProcessorMayMutateItsOwnSubscriberDuringTheFirstDrain() {
        // The monitor is not held while a processor runs, so a filter verb called from inside one acquires
        // it rather than re-entering it -- and, decisively, a processor that blocks cannot deadlock against
        // a thread holding it. Which messages such a mutation affects is deliberately undefined; that it
        // completes at all is not.
        final List<String> seen = new ArrayList<>();
        queued.add(message("a"));
        queued.add(message("b"));

        final AtomicReference<InternalTopicFilterSubscriber> self = new AtomicReference<>();
        final InternalTopicFilterSubscriber subscriber = factory.builder("test", "mutate-from-processor")
                .withProcessor(m -> {
                    seen.add(new String(m.getPayload(), StandardCharsets.UTF_8));
                    // Both families of change, from inside the processor, on the subscriber delivering it.
                    self.get().addTopicFilter("commands/extra");
                    self.get().removeTopicFilter("commands/extra");
                })
                .withTopicFilter("commands/#")
                .build();
        self.set(subscriber);

        subscriber.attach();
        subscriber.consume();

        assertThat(seen)
                .as("both messages are delivered; the mutations neither blocked nor deadlocked")
                .containsExactly("a", "b");
    }

    @Test
    void theWrongFamilyOfFilterVerbsIsRejected() {
        // The processor decides which family applies, so the other one is a mistake that can be named rather
        // than a state the subscriber has to represent. Both directions, since either is easy to reach when
        // the subscriber is built far from where its filters are set.
        final InternalTopicFilterSubscriber contextual = factory.builder("test", "wrong-family-context")
                .withAsyncContextProcessor((m, destinations) -> CompletableFuture.completedFuture(null))
                .build();

        assertThatThrownBy(() -> contextual.addTopicFilter("commands/#"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("addTopicFilterContext");

        final InternalTopicFilterSubscriber plain = factory.builder("test", "wrong-family-plain")
                .withProcessor(message -> {})
                .build();

        assertThatThrownBy(() -> plain.addTopicFilterContext(to("commands/#", "nope")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("addTopicFilter");
    }

    @Test
    void anAbsoluteFilterCallOnTheBuilderIsRejectedOnceFiltersAreDeclared() {
        // On the BUILDER the set starts empty and only these calls fill it, so a `with` that finds filters
        // already there is silently discarding what the caller just declared -- which nobody means. The same
        // verb on the live subscriber keeps its replacing sense, where a whole new set is an ordinary wish.
        assertThatThrownBy(() -> factory.builder("test", "double-with-bare")
                        .withProcessor(m -> {})
                        .withTopicFilter("commands/#")
                        .withTopicFilter("commands/setpoint"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("addTopicFilter")
                .hasMessageContaining("commands/#");

        assertThatThrownBy(() -> factory.builder("test", "double-with-context")
                        .withAsyncContextProcessor((m, d) -> CompletableFuture.completedFuture(null))
                        .withTopicFilterContext(to("commands/#", "first"))
                        .withTopicFilterContext(to("other/#", "second")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("addTopicFilterContext");

        // Both families are checked, so mixing them is caught at the call that did it rather than at build().
        assertThatThrownBy(() -> factory.builder("test", "mixed-families")
                        .withAsyncContextProcessor((m, d) -> CompletableFuture.completedFuture(null))
                        .addTopicFilter("commands/#")
                        .withTopicFilterContext(to("other/#", "second")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("commands/#");
    }

    @Test
    void repeatedRelativeFilterCallsOnTheBuilderAccumulate() {
        // The counterpart: `add` is what a caller declaring filters one at a time actually wants, and it
        // stays legal however many times it is called. Pins that the guard above did not over-reach.
        final List<Destination> matched = new ArrayList<>();
        queued.add(messageMatching("a"));

        factory.builder("test", "repeated-add")
                .addTopicFilterContext(to("commands/#", "first"))
                .addTopicFilterContext(to("commands/#", "second"))
                .<Destination>withAsyncContextProcessor((m, destinations) -> {
                    matched.addAll(destinations);
                    return CompletableFuture.completedFuture(null);
                })
                .build()
                .consume();

        assertThat(matched).containsExactlyInAnyOrder(to("commands/#", "first"), to("commands/#", "second"));
    }

    @Test
    void filtersGivenInTheWrongFamilyAtBuildTimeAreRejected() {
        assertThatThrownBy(() -> factory.builder("test", "bare-with-context-processor")
                        .addTopicFilter("commands/#")
                        .withAsyncContextProcessor((m, destinations) -> CompletableFuture.completedFuture(null))
                        .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("commands/#");
    }

    @Test
    void withTopicFilterContextReplacesTheWholeSet() {
        // The absolute form, at runtime: what is no longer wanted leaves the tree, what is new enters it.
        final InternalTopicFilterSubscriber subscriber = factory.builder("test", "runtime-with")
                .addTopicFilterContext(to("commands/#", "old"))
                .withAsyncContextProcessor((m, destinations) -> CompletableFuture.completedFuture(null))
                .build()
                .attach();

        assertThat(isRegisteredInTree()).isTrue();

        subscriber.withTopicFilterContext(to("other/#", "new"));

        assertThat(isRegisteredInTree())
                .as("the replaced filter is gone from the tree")
                .isFalse();
        assertThat(topicTree.findTopicSubscribers("other/thing").getSubscribers())
                .as("and the new one is in it")
                .isNotEmpty();
    }

    @Test
    void withTopicFilterContextWhileDetachedTakesEffectOnTheNextAttach() {
        // The detached counterpart of the test above, and worth its own case because the two states take
        // different routes: when ATTACHED the tree is reconciled filter by filter, when DETACHED there is
        // nothing registered to reconcile and the whole set is replayed by attach() instead.
        //
        // What must hold either way: after attach() the subscriber delivers to the destination declared while
        // detached, and not to the one it replaced.
        final List<Destination> matched = new ArrayList<>();
        final InternalTopicFilterSubscriber subscriber = factory.builder("test", "detached-with")
                .addTopicFilterContext(to("commands/#", "old"))
                .<Destination>withAsyncContextProcessor((m, destinations) -> {
                    matched.addAll(destinations);
                    return CompletableFuture.completedFuture(null);
                })
                .build();

        // Never attached, so the tree has nothing and reconciliation has nothing to do.
        subscriber.withTopicFilterContext(to("commands/setpoint", "new"));
        assertThat(isRegisteredInTree())
                .as("nothing is registered while detached")
                .isFalse();

        subscriber.attach();
        assertThat(isRegisteredInTree())
                .as("the replacement filter reaches the tree on attach")
                .isTrue();

        queued.add(messageMatching("a"));
        subscriber.consume();

        assertThat(matched)
                .as("and resolves to the destination declared while detached, not the one it replaced")
                .containsExactly(to("commands/setpoint", "new"));
    }

    // -- topic filter matching -------------------------------------------------------------------------
    //
    // Which filters a message matched is worked out from the message's TOPIC when it is polled, so the matching
    // rules are now part of delivery and are pinned here. Driven through the public path rather than against the
    // private matcher, so these test the code that actually runs.
    //
    // The cases are chosen for the places hand-written MQTT matchers go wrong, not for coverage of the happy
    // path: a multi-level wildcard matching its own PARENT, either wildcard matching an EMPTY level, a
    // single-level wildcard refusing to cross a separator, and a filter that is a strict prefix of the topic.

    @ParameterizedTest(name = "{0} matches {1}")
    @CsvSource({
        // exact
        "commands/setpoint, commands/setpoint",
        // single-level wildcard
        "commands/+, commands/setpoint",
        "+/setpoint, commands/setpoint",
        "+/+, commands/setpoint",
        // a single-level wildcard matches an EMPTY level
        "commands/+, commands/",
        // multi-level wildcard, including its own parent
        "commands/#, commands/setpoint",
        "commands/#, commands/setpoint/deep",
        "commands/#, commands",
        "#, commands/setpoint",
        "#, ''",
        "+/#, commands",
        // wildcards combined
        "commands/+/deep, commands/x/deep",
        "+/setpoint/#, commands/setpoint/deep",
    })
    void theseFiltersMatch(final @NotNull String filter, final @NotNull String topic) {
        assertThat(destinationsFor(filter, topic))
                .as("%s should match %s", filter, topic)
                .containsExactly(to(filter, "d"));
    }

    @ParameterizedTest(name = "{0} does not match {1}")
    @CsvSource({
        // different literal
        "commands/setpoint, commands/other",
        "commands/setpoint, other/setpoint",
        // a single-level wildcard covers exactly ONE level, never more
        "commands/+, commands/setpoint/deep",
        "+, commands/setpoint",
        // a filter that is a strict PREFIX of the topic does not match
        "commands, commands/setpoint",
        "commands/set, commands/setpoint",
        // nor the other way round
        "commands/setpoint/deep, commands/setpoint",
        // a multi-level wildcard matches its parent, but not a SHORTER topic than that
        "commands/deep/#, commands",
        // partial-segment comparisons must not match
        "commands/+/deep, commands/x/deeper",
    })
    void theseFiltersDoNotMatch(final @NotNull String filter, final @NotNull String topic) {
        assertThat(destinationsFor(filter, topic))
                .as("%s should NOT match %s", filter, topic)
                .isEmpty();
    }

    /// Puts one message on `topic` through a subscriber holding exactly one context on `filter`, and answers
    /// what the processor was told it matched. Empty means the filter did not match.
    private @NotNull List<Destination> destinationsFor(final @NotNull String filter, final @NotNull String topic) {
        final List<Destination> matched = new ArrayList<>();
        queued.add(messageOn(topic, "a"));

        factory.builder("test", "match-" + filter + "-" + topic)
                .addTopicFilterContext(to(filter, "d"))
                .<Destination>withAsyncContextProcessor((m, destinations) -> {
                    matched.addAll(destinations);
                    return CompletableFuture.completedFuture(null);
                })
                .build()
                .consume();

        return matched;
    }

    // -- queue limit and overflow ----------------------------------------------------------------------

    @Test
    void aSubscriberThatDeclaresNothingGetsTheBrokerDefaults() {
        // Null on both, which is what the distributor and the persistence read as "use the configured
        // broker-wide values". Declaring nothing must not mean an unbounded queue.
        final InternalTopicFilterSubscriber subscriber = factory.builder("test", "no-opinion")
                .withProcessor(m -> {})
                .withTopicFilter("commands/#")
                .build();

        assertThat(subscriber.queueLimit()).isNull();
        assertThat(subscriber.queueOverflow()).isNull();
    }

    @Test
    void aSubscriberCarriesTheLimitAndOverflowItDeclared() {
        // These are read at delivery time, by the distributor and the queue persistence, through the factory
        // registry -- the same hook ingress-exclusion uses. What this pins is that the subscriber carries them
        // at all, which is what makes the prefix special-cases in those two classes deletable.
        final InternalTopicFilterSubscriber subscriber = factory.builder("test", "opinionated")
                .withProcessor(m -> {})
                .withTopicFilter("commands/#")
                .withQueueLimit(10)
                .withQueueOverflow(QueuedMessagesStrategy.DISCARD_OLDEST)
                .build();

        assertThat(subscriber.queueLimit()).isEqualTo(10L);
        assertThat(subscriber.queueOverflow()).isEqualTo(QueuedMessagesStrategy.DISCARD_OLDEST);
    }

    @Test
    void aSubscriberIsReachableByItsQueueIdFromTheRegistry() {
        // The lookup both consumption points make: they hold a queue id and need the subscriber that owns it.
        // A subscriber whose declared values cannot be found this way has declared them to nobody.
        final InternalTopicFilterSubscriber subscriber = factory.builder("test", "findable")
                .withProcessor(m -> {})
                .withTopicFilter("commands/#")
                .withQueueLimit(7)
                .build();

        assertThat(factory.getSubscriber(subscriber.clientId())).isSameAs(subscriber);
        assertThat(factory.getSubscriber(subscriber.clientId()).queueLimit()).isEqualTo(7L);
    }

    @Test
    void aProcessorThatNeverAnswersStallsOnlyItself() {
        // The honest consequence of the bound, stated rather than hidden: a consumer that never completes
        // its future stops its own subscriber and nothing else. That is why a firing timeout deserves a
        // metric -- it always means a bug in the consumer.
        queued.add(message("a"));
        queued.add(message("b"));

        factory.builder("test", "silent")
                .withAsyncProcessor(m -> new CompletableFuture<>())
                .withTopicFilter("commands/#")
                .build()
                .consume();

        assertThat(reads.get())
                .as("one read, and no further read while the first is unanswered")
                .isEqualTo(1);
    }
}
