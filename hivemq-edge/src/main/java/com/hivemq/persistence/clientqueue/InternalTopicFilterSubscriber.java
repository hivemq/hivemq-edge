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

import static com.hivemq.configuration.service.InternalConfigurations.PUBLISH_POLL_BATCH_SIZE_BYTES;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.ImmutableIntArray;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.configuration.service.MqttConfigurationService.QueuedMessagesStrategy;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriptionFlag;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.util.FutureUtils;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// region InternalTopicFilterSubscriber -- concrete topic-tree subscriber for internal Edge components
// =====================================================================================================================
// Lets an internal Edge component (a bridge, a combiner, a sampler, ...) receive messages from the
// local topic tree without being an MQTT client.
//
// This is a CONCRETE, FINAL class -- there is nothing to subclass and nothing to override. You do not
// extend it; you obtain one from InternalTopicFilterSubscriberFactory, configure it through a fluent
// Builder, and drive it with explicit lifecycle verbs. The single piece of behaviour the component
// supplies -- what to do with each message -- is a Processor lambda passed to the builder.
//
//   // a combiner keeping the latest value of one topic in an atomic reference:
//   final AtomicReference<byte[]> latest = new AtomicReference<>();
//   subscriber = factory.builder("combiner", combinerId)
//                       .withProcessor(message -> latest.set(message.getPayload())) // SingleWriter thread
//                       .withTopicFilter("sensors/temperature")
//                       .build();
//   subscriber.start();                          // attach(); consume();
//   subscriber.addTopicFilter("sensors/humidity"); // runtime mutation, no teardown
//   subscriber.stop();                           // detach(); pause(); deallocate();
//
// Lifecycle. The lifecycle is decomposed into independent verbs. attach()/detach() control the
// SUBSCRIPTION (topics in the topic tree, i.e. whether messages are COLLECTED); consume()/pause()
// control the CALLBACK (whether collected messages are PROCESSED). They are orthogonal -- a subscriber
// can be attached-but-paused (collecting into the queue without draining) or detached-but-consuming
// (callback armed, ready for when topics are attached later). start() and stop() are just convenience
// compositions of the verbs.
//
// Threading, and why a processor may not block. Three steps, in order -- the constraint on a processor is
// a consequence rather than a rule of its own:
//
//   1. Message handling in the broker and in Edge is generally SEQUENTIAL. That is how MQTT's ordering
//      guarantees are met.
//   2. Sequential without locks, and without overhead in either code complexity or runtime cost, is
//      achieved with DEDICATED THREADS: one thread performs every operation for a given queue -- in fact
//      for a whole bucket of queues.
//   3. So a component's processor MAY NOT BLOCK that thread for any extended period. The thread is not the
//      component's; holding it holds up every queue in the bucket.
//
// A component whose work is slow, or which may block, therefore returns a future instead of doing the work
// (see withAsyncProcessor): it starts the work elsewhere and returns at once, releasing the thread.
//
// The verbs themselves are safe to call from any thread; the queue/topic-tree operations they perform are
// the same ones the previous design performed, just split apart so each is independently callable.
//
// One message in flight. Handling stays sequential in either form -- the next message is passed on only
// once the previous one's future has completed, so a component never has two in hand. For a withProcessor
// lambda that future completes when the lambda returns; for a withAsyncProcessor lambda, when the consumer
// completes it. The asynchronous form does not create that bound: it is what step 1 requires and holds
// either way. What it buys is keeping the bound WITHOUT occupying the thread for the length of the work.
//
// @see <a href="https://hivemq.github.io/hivemq-edge-lore/2-implementation/internal-topic-filter-subscriber/">Edge Lore
// -- Internal Topic Filter Subscriber</a>
//
@SuppressWarnings({"FutureReturnValueIgnored", "CheckReturnValue", "UnusedReturnValue"})
public final class InternalTopicFilterSubscriber {

    // region Instance Variables -- what a subscriber is made of
    // =================================================================================================================

    // Dependencies - logger, topic tree, queues
    private static final @NotNull Logger log = LoggerFactory.getLogger(InternalTopicFilterSubscriber.class);

    // The three Edge singletons this subscriber operates against. Supplied by the factory (which has
    // them injected), so callers never see or thread them. Final -- set once at construction.
    private final @NotNull LocalTopicTree topicTree;
    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull SingleWriterService singleWriterService;

    //   the factory that created this subscriber, and is its registry
    // The factory that built this subscriber, kept as a back-reference so that build()/deallocate() can
    // (de)register this subscriber in the factory's registry -- see isExcludedIngressClientId() below.
    private final @NotNull InternalTopicFilterSubscriberFactory factory;

    // Identity - creation, identity
    //   the reserved prefix, and this subscriber's own id
    // INTERNAL_SUBSCRIBER_PREFIX -- what allows to tell internal subscribers apart
    public static final @NotNull String INTERNAL_SUBSCRIBER_PREFIX = "$INTERNAL::";

    // clientId -- built once as the reserved-prefix triple "$INTERNAL::<componentPrefix>::<instanceId>"
    //            (see the constructor for what each segment means). PublishDistributorImpl
    //            .isReservedClientId() recognises the "$INTERNAL::" prefix and rejects any external
    //            MQTT client that tries to connect with it, so this namespace cannot collide with real
    //            clients.
    //
    //            ONE id, used as the identity in TWO different subsystems -- and deliberately so:
    //              - in the TOPIC TREE  it is the subscriber id (addTopic / removeSubscriber), i.e.
    //                "who is subscribed to this filter".
    //              - in the CLIENT-QUEUE persistence it is the queue id (the bucket key for
    //                addPublishAvailableCallback / submit / readNew / removeShared / clear), i.e.
    //                "whose queue do the matched messages land in".
    //            Using the same string for both is what wires the two together: the topic tree routes
    //            a matching message into the queue named by this id, and we drain that same-named
    //            queue. They are the same id on purpose; there is no separate topic-tree id vs queue id.
    private final @NotNull String clientId;

    // Processors - what behaviour
    // The per-message handler, in whichever of the four forms the consumer supplied. EXACTLY ONE IS NON-NULL
    // -- the builder rejects a second -- and process() picks the one that is. Kept in the form supplied rather
    // than wrapped into a common one: the wrapping cost a lambda layer per form and computed destinations for
    // subscribers that never asked to be told them.
    private final @Nullable Processor plainProcessor;
    private final @Nullable AsyncProcessor asyncProcessor;
    private final @Nullable ContextProcessor<?> contextProcessor;
    private final @Nullable AsyncContextProcessor<?> asyncContextProcessor;

    // TopicFilters - which messages
    //
    // A subscriber is one of two kinds, and which it is follows from the processor it was built with:
    // withProcessor/withAsyncProcessor make a PLAIN one, withContextProcessor a CONTEXT one. The kind decides
    // which family of filter verbs is legal (the other family throws) and which structures below are used --
    // a plain subscriber never touches the context map, and a context subscriber never uses topicFilters.
    // See hasTopicFilterContext() and the filter-verb region.

    // topicFilters -- a PLAIN subscriber's filter set. When detached this is just a remembered set (replayed by
    //                the next attach()); when attached the topic tree is kept reconciled with it. A
    //                LinkedHashSet so order is stable and duplicates are ignored.
    private final @NotNull Set<String> topicFilters = new LinkedHashSet<>();

    // topicFilterContexts -- a CONTEXT subscriber's filters, each with the contexts registered for it. Doubles
    //                as that subscriber's filter set: its keys are exactly the filters, so the two kinds do
    //                not each need one.
    //
    //                A SET per filter, because one filter may carry several contexts -- two southbound mappings
    //                may share a topic filter -- and registering the same filter twice OVERWRITES in the topic
    //                tree rather than accumulating, correctly so, since MQTT-3.8.4-3 requires a repeated filter
    //                to replace its predecessor. The fan-out cannot live in the tree, so it lives here.
    private final @NotNull Map<String, Set<TopicFilterContext>> topicFilterContexts = new LinkedHashMap<>();

    // NO per-filter subscription identifiers, deliberately. Which filters a message matched is worked out when
    // the message is POLLED, by matching its topic against this subscriber's own filters -- see resolveDestinations()
    // and
    // isMatchFilterTopic(). An identifier recorded on the message when it was ENQUEUED would be a cached answer
    // that outlives the table giving it meaning: it would have to survive detach/attach and a restart, and when
    // it did not the message arrived with no contexts and was silently dropped.

    // excludedIngressClientId -- ingress-exclusion (generalized No Local). If set, a message whose
    //            INGRESS client id (the publishing client, i.e. the distributor's `sender`) equals this
    //            value must NOT be delivered to us. Set once via the builder, immutable after build();
    //            null means "no exclusion -- accept from everyone". The check happens at distribution
    //            time (PublishDistributorImpl), BEFORE the message is queued, so an excluded message
    //            never enters our queue. See isExcludedIngressClientId().
    private final @Nullable String excludedIngressClientId;

    // Queues - how to buffer
    // The QoS every filter is registered at. One value for the whole subscriber, not per filter, and
    // deliberately: when several of a subscriber's filters match one message the topic tree folds them into
    // one entry keeping the LAST QoS in sorted order, so per-filter values would make the effective QoS of a
    // message depend on sort order rather than on anything the caller said.
    private final @NotNull QoS qos;

    // How many messages this subscriber's queue holds, and what happens when it is full. Both are read by the
    // publish distributor at delivery time, through the factory registry -- the same hook ingress-exclusion
    // uses -- because the decision is made where the message is queued, not here.
    //
    // Null means "whatever the broker is configured for": maxQueuedMessages (1000 unless changed) and the
    // configured strategy (DISCARD by default). Declaring them replaces the alternative, which is what Edge
    // does today for the sampler -- a prefix match on the client id, hardcoded into the distributor for the
    // limit and into the queue persistence for the strategy. A consumer should say this where it is defined.
    private final @Nullable Long queueLimit;

    private final @Nullable QueuedMessagesStrategy queueOverflow;

    // PPF-Loop - implementation
    // loopState -- what the loop IS: ACTIVE (an iteration in flight), WAITING (none, but a message will start
    //            one), PAUSED (none, and a message will not), TERMINATED (dead). A subscriber is born PAUSED.
    //            ACTIVE IS THE ONE-MESSAGE-AT-A-TIME BOUND, and the only thing that states it. Deliberately
    //            about the LOOP rather than about a message: it holds across the gap between finishing one
    //            message and the next read returning, which is exactly when a second read must not start.
    //            Written by ppfLoopCtrl() and by nothing else; see there for why it can be a plain field.
    private @NotNull PpfLoopState loopState = PpfLoopState.PAUSED;

    // fetched -- messages a read handed us that we have not processed yet.
    //
    //            A READ CAN RETURN MORE THAN ONE MESSAGE, EVEN WHEN ASKED FOR ONE. We pass a single packet id,
    //            which reads as "give me one message" and is not: that argument is a supply of packet ids to
    //            STAMP messages with, and a QoS 0 message needs no stamp. So the storage layer hands back one
    //            stamped message AND, on a queue that also holds QoS 0 messages, one of those as well -- added
    //            before the count limit is re-tested. Two messages from a read that asked for one.
    //
    //            THIS IS WHY THE EXTRAS MUST BE KEPT. Reading a QoS 0 message REMOVES it from the queue, so a
    //            returned message we drop is gone: not delayed, not redelivered, just lost, with nothing
    //            logged. An earlier version took messages.get(0) and discarded the rest, which silently lost
    //            every QoS 0 message that arrived alongside a QoS 1 or 2 one. Whatever a read returns is now
    //            drained in here and handed to the processor one at a time, in order.
    //
    //            An ArrayDeque because poll() is exactly the operation wanted -- take the head, leave the
    //            rest, answer null when empty -- in one call, with no index to keep and no node per message.
    private final @NotNull Deque<PUBLISH> fetched = new ArrayDeque<>();

    // goalState -- what the loop is being ASKED to be, as against loopState which is what it IS.
    //
    //            THE PRIORITISED QUEUE OF PENDING COMMANDS, expressed as the state they are asking for. A verb
    //            arriving while an iteration is in flight cannot be acted on then -- the iteration owns the
    //            thread, and a message may be with the consumer's processor -- so it is recorded here and
    //            acted on when that iteration reports back. goalStateFor() is the rule for what an ask does to
    //            a goal already standing, and is where the priority between them lives.
    //
    //            Written and read only by ppfLoopCtrl(), like loopState, and so needs no atomicity.
    private @NotNull PpfLoopState goalState = PpfLoopState.PAUSED;

    // Lifecycle - start, stop
    // attached -- true iff the current topicFilters are registered in the topic tree (messages are
    //            being collected into the client queue).
    private boolean attached = false;

    // consuming -- true iff this subscriber is draining its queue: the publish-available callback is
    //             registered AND the poll pipeline will act on a trigger. Read by pollIfIdle(), which
    //             is what makes pause() stop work already under way rather than only future wake-ups.
    private boolean consuming = false;

    // deallocated -- true once deallocate() has cleared the queue. TERMINAL: once deallocated the
    //               subscriber is dead and cannot be resurrected; every verb is a no-op (or, for
    //               attach/consume, rejected) from then on. This flag makes deallocate()/stop() safely
    //               idempotent and guards the other verbs against use-after-deallocate.
    private boolean deallocated = false;

    // Misc
    // SHARED_IN_FLIGHT_MARKER acts as a boolean inflight flag -- not a real wire packet ID, since messages
    // never go to an MQTT client. Reading a message above QoS 0 stamps it with the id supplied here and leaves
    // it in the queue; a later read skips anything stamped. So all this value has to be is non-zero.
    //
    // IT IS ALSO WHAT finish() DELETES BY, so the same value identifies "the stamped message" rather
    // than a particular one. That is unambiguous only because a subscriber holds exactly one message at a
    // time -- see loopActive. Two at once would both be stamped with this, and the removal would
    // delete whichever the queue happened to hold first.
    private static final @NotNull ImmutableIntArray POLL_PACKET_IDS =
            ImmutableIntArray.of(ClientQueuePersistenceImpl.SHARED_IN_FLIGHT_MARKER);

    // endregion

    // region Constructor -- package-private, only the factory's builder constructs this
    // =====================================================================================================================
    // The parameters run in the order the instance variables are declared in, and mean the same things --
    // dependencies, parent, identity, processors, filters, queue handling. Only these need more:
    //
    // factory -- the factory that built us, kept as the registry for ingress-exclusion lookups.
    // componentPrefix -- identifies the Edge component type (e.g. "tynebridge", "combiner", "sampler").
    //                   Must be unique across all components in Edge. Becomes the middle segment of
    //                   the internal client ID.
    // instanceId -- identifies this specific subscriber instance within the component. Must be
    //                   unique within the componentPrefix namespace. Typically derived from the
    //                   configuration ID of the owning instance.
    // the four processors -- exactly one is non-null, which build() enforces.
    // topicFilters / topicFilterContexts -- in whichever form this subscriber's kind uses; the other is
    //                   empty. Both may be empty, which is valid.
    //
    InternalTopicFilterSubscriber(
            final @NotNull LocalTopicTree topicTree,
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull SingleWriterService singleWriterService,
            final @NotNull InternalTopicFilterSubscriberFactory factory,
            final @NotNull String componentPrefix,
            final @NotNull String instanceId,
            final @Nullable Processor plainProcessor,
            final @Nullable AsyncProcessor asyncProcessor,
            final @Nullable ContextProcessor<?> contextProcessor,
            final @Nullable AsyncContextProcessor<?> asyncContextProcessor,
            final @NotNull Set<String> topicFilters,
            final @NotNull Map<String, Set<TopicFilterContext>> topicFilterContexts,
            final @Nullable String excludedIngressClientId,
            final @NotNull QoS qos,
            final @Nullable Long queueLimit,
            final @Nullable QueuedMessagesStrategy queueOverflow) {
        this.topicTree = topicTree;
        this.clientQueuePersistence = clientQueuePersistence;
        this.singleWriterService = singleWriterService;
        this.factory = factory;
        this.clientId = INTERNAL_SUBSCRIBER_PREFIX + componentPrefix + "::" + instanceId;
        this.plainProcessor = plainProcessor;
        this.asyncProcessor = asyncProcessor;
        this.contextProcessor = contextProcessor;
        this.asyncContextProcessor = asyncContextProcessor;
        // AFTER the processors, which hasTopicFilterContext() reads. Copied rather than kept by reference, so a builder
        // reused after build() cannot reach into a live subscriber.
        if (hasTopicFilterContext()) {
            topicFilterContexts.forEach(
                    (filter, contexts) -> this.topicFilterContexts.put(filter, new LinkedHashSet<>(contexts)));
        } else {
            this.topicFilters.addAll(topicFilters);
        }
        this.excludedIngressClientId = excludedIngressClientId;
        this.qos = qos;
        this.queueLimit = queueLimit;
        this.queueOverflow = queueOverflow;
    }

    // endregion

    // region Builder -- fluent build-time configuration, obtained from the factory
    // =================================================================================================================
    // The Processor is required; build() throws without it. Topic filters are optional -- the set
    // starts empty, and an empty set is perfectly valid (a subscriber that subscribes to nothing on
    // attach(), to which filters can be added later).
    //
    // The three topic-filter verbs are the SAME as on the subscriber (see the topic-filter region
    // there): withTopicFilter is ABSOLUTE (replace the whole set, last call wins); addTopicFilter and
    // removeTopicFilter are RELATIVE. On the builder add/remove are not strictly necessary (with could
    // express any set) but are convenient for assembling a set incrementally.
    //
    public static final class Builder {

        // dependencies
        private final @NotNull LocalTopicTree topicTree;
        private final @NotNull ClientQueuePersistence clientQueuePersistence;
        private final @NotNull SingleWriterService singleWriterService;

        // parent
        private final @NotNull InternalTopicFilterSubscriberFactory factory;

        // what is my identity
        private final @NotNull String componentPrefix;
        private final @NotNull String instanceId;

        // what am I doing with the messages -- exactly one may be set (throwIfProcessorAlreadySet enforces it)
        // and build() requires that one. Each is kept as supplied and called in that shape by process().
        private @Nullable Processor plainProcessor = null;
        private @Nullable AsyncProcessor asyncProcessor = null;
        private @Nullable ContextProcessor<?> contextProcessor = null;
        private @Nullable AsyncContextProcessor<?> asyncContextProcessor = null;

        // which messages
        private final @NotNull Set<String> topicFilters = new LinkedHashSet<>();
        private final @NotNull Map<String, Set<TopicFilterContext>> topicFilterContexts = new LinkedHashMap<>();
        private @Nullable String excludedIngressClientId = null; // optional; ingress-exclusion

        // queue handling
        private @NotNull QoS qos = QoS.AT_LEAST_ONCE; // optional; the QoS every filter is registered at
        private @Nullable Long queueLimit = null; // optional; null means the broker-wide default
        private @Nullable QueuedMessagesStrategy queueOverflow = null; // optional; null means the broker default

        // Package-private -- only the factory creates builders (it supplies the injected singletons and
        // itself, as the registry the built subscriber registers with on attach()).
        Builder(
                final @NotNull LocalTopicTree topicTree,
                final @NotNull ClientQueuePersistence clientQueuePersistence,
                final @NotNull SingleWriterService singleWriterService,
                final @NotNull InternalTopicFilterSubscriberFactory factory,
                final @NotNull String componentPrefix,
                final @NotNull String instanceId) {
            this.topicTree = topicTree;
            this.clientQueuePersistence = clientQueuePersistence;
            this.singleWriterService = singleWriterService;
            this.factory = factory;
            this.componentPrefix = componentPrefix;
            this.instanceId = instanceId;
        }

        // Required, in one of two forms: the per-message work.
        //
        // withProcessor -- the work is done ON the queue's own thread, so it must be small.
        // withAsyncProcessor -- the work is done elsewhere; this returns a future for it at once.
        //
        // Two names rather than one overloaded name, because the two cannot be told apart by the compiler: a
        // lambda like `message -> latest.set(...)` is ambiguous between them, and the ambiguity is a compile
        // error at every call site rather than a silent wrong pick. Distinct names also say which contract the
        // consumer is signing up to instead of leaving it to inference.
        public @NotNull Builder withProcessor(final @NotNull Processor processor) {
            throwIfProcessorAlreadySet();
            this.plainProcessor = processor;
            return this;
        }

        // Required, alternative form: for work too slow, or too likely to block, to run on the thread that
        // serves this queue's bucket. The consumer starts it elsewhere and returns a future immediately; the
        // next message is passed on when that future completes, so handling stays sequential without the
        // thread being held meanwhile.
        public @NotNull Builder withAsyncProcessor(final @NotNull AsyncProcessor processor) {
            throwIfProcessorAlreadySet();
            this.asyncProcessor = processor;
            return this;
        }

        // Required, third and fourth forms: as the two above, plus what the message matched.
        //
        // SUPPLYING EITHER DECIDES THE KIND OF SUBSCRIBER. It is what makes the ...TopicFilterContext verbs
        // the legal ones, on the builder and on the subscriber alike, and the bare-string ones illegal. There
        // is deliberately no separate flag saying so: the processor already says it, and two statements of one
        // fact could disagree -- the more easily because the filters may be added far from where the
        // subscriber is built.
        public <T extends TopicFilterContext> @NotNull Builder withContextProcessor(
                final @NotNull ContextProcessor<T> processor) {
            throwIfProcessorAlreadySet();
            this.contextProcessor = processor;
            return this;
        }

        public <T extends TopicFilterContext> @NotNull Builder withAsyncContextProcessor(
                final @NotNull AsyncContextProcessor<T> processor) {
            throwIfProcessorAlreadySet();
            this.asyncContextProcessor = processor;
            return this;
        }

        /// A subscriber takes ONE processor. Rejected at the second call rather than silently replacing the
        /// first, which would discard a consumer's handler without a word -- and, if the two were of different
        /// kinds, silently change which family of filter verbs is legal.
        private void throwIfProcessorAlreadySet() {
            if (plainProcessor != null
                    || asyncProcessor != null
                    || contextProcessor != null
                    || asyncContextProcessor != null) {
                throw new IllegalStateException("InternalTopicFilterSubscriber takes one processor, not two; "
                        + "withProcessor(...), withAsyncProcessor(...), withContextProcessor(...) and "
                        + "withAsyncContextProcessor(...) are alternatives");
            }
        }

        // The topic filters, in two families of three.
        //
        //   withTopicFilter / addTopicFilter / removeTopicFilter                      -- bare String filters
        //   withTopicFilterContext / addTopicFilterContext / removeTopicFilterContext -- TopicFilterContext
        //
        // A subscriber that treats every filter alike takes strings. One standing in for several destinations
        // takes contexts, and is handed them back through a RoutingProcessor when a message matches. The two
        // families do not mix: build() rejects a subscriber that has some of each.
        //
        // SEPARATE NAMES rather than overloads. withTopicFilter(List<String>) and
        // withTopicFilter(List<? extends TopicFilterContext>) do not compile side by side -- erasure makes both
        // withTopicFilter(List), a name clash the compiler refuses -- so the list forms could not both exist
        // under one name. The names also say at the call site which of the two a subscriber is, which the
        // argument type alone leaves to be inferred.
        //
        // WITH is absolute (replace the whole set), ADD and REMOVE are relative.
        //
        // ON THE BUILDER, THOUGH, "ABSOLUTE" HAS NOTHING TO REPLACE: the set starts empty and only these calls
        // fill it, so a `with` that finds filters already there is discarding work the caller just did -- and
        // silently. That is never what anyone means; the caller wanted `add`. So it throws here, while the same
        // verb on the LIVE subscriber keeps its replacing sense, where wanting a whole new set is ordinary.

        // Absolute: set the whole set to this filter / these filters. Throws if any are already declared.
        public @NotNull Builder withTopicFilter(final @NotNull String topicFilter) {
            return withTopicFilter(List.of(topicFilter));
        }

        public @NotNull Builder withTopicFilter(final @NotNull List<String> topicFilters) {
            throwIfFiltersAlreadyDeclared("withTopicFilter", "addTopicFilter");
            this.topicFilters.addAll(topicFilters);
            return this;
        }

        // Relative: add to the current set.
        public @NotNull Builder addTopicFilter(final @NotNull String topicFilter) {
            topicFilters.add(topicFilter);
            return this;
        }

        public @NotNull Builder addTopicFilter(final @NotNull List<String> topicFilters) {
            this.topicFilters.addAll(topicFilters);
            return this;
        }

        /// Rejects an absolute filter call once any filter has been declared.
        ///
        /// Checks BOTH families, so declaring a bare filter and then a context (or the reverse) is caught here,
        /// at the call that did it, rather than at `build()` -- which can only name the filters, not the call
        /// that added them.
        ///
        /// @param absolute the verb being called, named in the message
        /// @param relative the verb the caller almost certainly meant
        private void throwIfFiltersAlreadyDeclared(final @NotNull String absolute, final @NotNull String relative) {

            if (topicFilters.isEmpty() && topicFilterContexts.isEmpty()) {
                return;
            }
            final Set<String> declared = new LinkedHashSet<>(topicFilters);
            declared.addAll(topicFilterContexts.keySet());
            throw new IllegalStateException("InternalTopicFilterSubscriber.Builder." + absolute
                    + "(...) replaces the whole filter set, but these are already declared: "
                    + declared
                    + ". Use "
                    + relative
                    + "(...) to add to them.");
        }

        // Relative: remove from the current set.
        public @NotNull Builder removeTopicFilter(final @NotNull String topicFilter) {
            return removeTopicFilter(List.of(topicFilter));
        }

        public @NotNull Builder removeTopicFilter(final @NotNull List<String> topicFilters) {
            this.topicFilters.removeAll(topicFilters);
            return this;
        }

        // Absolute, with context: set the whole set to these, each carrying its own filter. Throws if any
        // filters are already declared -- see the note above.
        public @NotNull Builder withTopicFilterContext(final @NotNull TopicFilterContext context) {
            return withTopicFilterContext(List.of(context));
        }

        public @NotNull Builder withTopicFilterContext(final @NotNull List<? extends TopicFilterContext> contexts) {
            throwIfFiltersAlreadyDeclared("withTopicFilterContext", "addTopicFilterContext");
            return addTopicFilterContext(contexts);
        }

        // Relative, with context: add these to the current set.
        //
        // Two contexts may name the SAME filter -- two southbound mappings on one topic -- and both are kept.
        // That fan-out is precisely what the topic tree cannot hold itself, since a repeated filter replaces
        // its predecessor there (MQTT-3.8.4-3); keeping it here is what makes the case work at all.
        public @NotNull Builder addTopicFilterContext(final @NotNull TopicFilterContext context) {
            return addTopicFilterContext(List.of(context));
        }

        public @NotNull Builder addTopicFilterContext(final @NotNull List<? extends TopicFilterContext> contexts) {
            for (final TopicFilterContext context : contexts) {
                topicFilterContexts
                        .computeIfAbsent(context.topicFilter(), ignored -> new LinkedHashSet<>())
                        .add(context);
            }
            return this;
        }

        // Relative, with context: remove ONE destination, leaving its filter if other contexts still want it.
        //
        // Found by equals(), so an implementation with identity equality can only be removed by the object
        // that was registered -- said on TopicFilterContext, and worth knowing here too.
        public @NotNull Builder removeTopicFilterContext(final @NotNull TopicFilterContext context) {
            return removeTopicFilterContext(List.of(context));
        }

        public @NotNull Builder removeTopicFilterContext(final @NotNull List<? extends TopicFilterContext> contexts) {

            for (final TopicFilterContext context : contexts) {
                final String topicFilter = context.topicFilter();
                final Set<TopicFilterContext> remaining = topicFilterContexts.get(topicFilter);
                if (remaining != null) {
                    remaining.remove(context);
                    if (remaining.isEmpty()) {
                        // Nothing wants this filter any more, so the filter goes too. Keeping it would leave a
                        // subscription whose messages resolve to no destination -- accepted, then dropped.
                        topicFilterContexts.remove(topicFilter);
                    }
                }
            }
            return this;
        }

        // Optional: ingress-exclusion (generalized No Local). A message whose ingress (publishing)
        // client id equals this value will not be delivered to the built subscriber. Build-time only;
        // immutable after build(). For a bridge, this is the peer it forwards to -- so a message it sent
        // us is not sent straight back. The plain No Local case is excludedIngressClientId == our own id.
        public @NotNull Builder withExcludedIngressClientId(final @NotNull String excludedIngressClientId) {
            this.excludedIngressClientId = excludedIngressClientId;
            return this;
        }

        // Optional: the QoS every filter is registered at. Defaults to AT_LEAST_ONCE.
        //
        // A CEILING, not a floor -- the delivered QoS is min(published, this), so subscribing at EXACTLY_ONCE
        // does not upgrade a QoS 0 publish. The distinction that matters downstream is zero versus non-zero: a
        // QoS 0 message is held in a separate memory-limited list and is never acknowledged.
        public @NotNull Builder withQoS(final @NotNull QoS qos) {
            this.qos = qos;
            return this;
        }

        // Optional: the most messages to hold for this subscriber. Defaults to the broker-wide
        // maxQueuedMessages, which is 1000 unless configured otherwise -- so this narrows rather than
        // unbounds, and a consumer that has no opinion should not set it.
        //
        // What happens at the limit is withQueueOverflow below, and the two are worth reading together: a
        // small limit with the default strategy means "reject new messages beyond N", which is rarely what a
        // consumer picking a small number has in mind.
        public @NotNull Builder withQueueLimit(final long queueLimit) {
            this.queueLimit = queueLimit;
            return this;
        }

        // Optional: which message to drop when the queue is full.
        //
        // DISCARD        -- reject the INCOMING message, keeping the queue as it is. The broker-wide default.
        // DISCARD_OLDEST -- drop the oldest queued message to make room for the new one.
        //
        // The default is worth a moment: for a command queue it means a fresh command is refused in favour of
        // a backlog of stale ones, which is usually the wrong way round. A consumer whose messages supersede
        // each other -- a setpoint, a latest-value -- wants DISCARD_OLDEST and should say so.
        public @NotNull Builder withQueueOverflow(final @NotNull QueuedMessagesStrategy queueOverflow) {
            this.queueOverflow = queueOverflow;
            return this;
        }

        // Produce the live subscriber. The subscriber is constructed in the IDLE state -- detached and
        // paused -- i.e. no messages flow until start() (or attach()/consume()) is called. It is, however,
        // already REGISTERED with the factory: the subscriber owns its clientId ("$INTERNAL::<prefix>::
        // <instanceId>") from build() until deallocate(). register() throws if that identity is already in
        // use, so a duplicate componentPrefix::instanceId is rejected here rather than silently colliding.
        public @NotNull InternalTopicFilterSubscriber build() {
            if (plainProcessor == null
                    && asyncProcessor == null
                    && contextProcessor == null
                    && asyncContextProcessor == null) {
                throw new IllegalStateException("InternalTopicFilterSubscriber requires a processor; call one of "
                        + "withProcessor(...), withAsyncProcessor(...), withContextProcessor(...) or "
                        + "withAsyncContextProcessor(...) before build()");
            }
            final boolean hasTopicFilterContext = contextProcessor != null || asyncContextProcessor != null;
            // The processor decides which family of filter verbs applies, so filters given in the other family
            // are a mistake -- and one worth naming here, since the two calls may be far apart.
            if (hasTopicFilterContext && !topicFilters.isEmpty()) {
                throw new IllegalStateException("InternalTopicFilterSubscriber was built with a context processor, "
                        + "so its filters must be given as contexts; these were given as bare strings: "
                        + topicFilters);
            }
            if (!hasTopicFilterContext && !topicFilterContexts.isEmpty()) {
                throw new IllegalStateException("InternalTopicFilterSubscriber was given topic filter contexts but "
                        + "no context processor to deliver them to; call withContextProcessor(...) or "
                        + "withAsyncContextProcessor(...)");
            }

            final InternalTopicFilterSubscriber subscriber = new InternalTopicFilterSubscriber(
                    topicTree,
                    clientQueuePersistence,
                    singleWriterService,
                    factory,
                    componentPrefix,
                    instanceId,
                    plainProcessor,
                    asyncProcessor,
                    contextProcessor,
                    asyncContextProcessor,
                    topicFilters,
                    topicFilterContexts,
                    excludedIngressClientId,
                    qos,
                    queueLimit,
                    queueOverflow);
            factory.register(subscriber); // throws if clientId already in use -- deregistered by deallocate()
            return subscriber;
        }
    }

    // endregion

    // region Subscriber Identity -- what others ask this subscriber, and the declarations they read
    // =================================================================================================================
    // All four are immutable after build(), so all four are lock-free reads, safe to call from the publish
    // path concurrently with the lifecycle verbs.
    //
    // -- what is my identity ---------------------------------------------------------------------------
    //
    // clientId() -- this subscriber's reserved-prefix id, used by the factory as the registry key.
    public @NotNull String clientId() {
        return clientId;
    }

    // -- which messages --------------------------------------------------------------------------------
    //
    // isExcludedIngressClientId(senderClientId) -- the ingress-exclusion decision, called by the publish
    //   path at distribution time (BEFORE queueing) with the ingress/publishing client id (its
    //   `sender`). Returns true iff an excluded id is configured and equals senderClientId -- in which
    //   case the message must NOT be queued to us. With no excluded id (the common case) it always
    //   returns false.
    public boolean isExcludedIngressClientId(final @Nullable String senderClientId) {
        return excludedIngressClientId != null && excludedIngressClientId.equals(senderClientId);
    }

    // -- queue handling --------------------------------------------------------------------------------

    /// @return the most messages to hold for this subscriber, or null to use the broker-wide
    ///     `maxQueuedMessages`. Read by the publish distributor when it queues a message.
    public @Nullable Long queueLimit() {
        return queueLimit;
    }

    /// @return what to drop when this subscriber's queue is full, or null to use the broker-wide strategy.
    ///     Read by the queue persistence when it adds a message.
    public @Nullable QueuedMessagesStrategy queueOverflow() {
        return queueOverflow;
    }

    // endregion

    // region Subscriber Processors -- the per-message handler forms a consumer may supply
    // =================================================================================================================
    // TWO INDEPENDENT CHOICES, so four interfaces. How a consumer says it is finished -- by returning, or by
    // completing a future -- and what it is told -- the message alone, or the message plus which of its
    // destinations matched -- are unrelated questions:
    //
    //                      told only the message      told what it matched
    //   done on return     Processor                  ContextProcessor
    //   done on a future   AsyncProcessor             AsyncContextProcessor
    //
    // A processor runs on the SingleWriter thread that serves this queue's whole bucket, so the top row is for
    // work small enough to do there. Anything slower, and anything that may block, wants the bottom row: the
    // work moves off the thread while the ordering stays. A future rather than a callback, because a callback
    // can be forgotten (stalling this queue silently) or called twice (destroying the one-at-a-time bound),
    // and because "completes" means SETTLED -- a failure finishes a message exactly as a success does.
    //
    // The left column is for a subscriber handling every message the same way; the right for one standing in
    // for several destinations. SUPPLYING A RIGHT-HAND FORM IS WHAT MAKES A SUBSCRIBER A CONTEXT ONE -- there
    // is no separate flag, since a second statement of that could disagree with the first. It follows that
    // only the ...TopicFilterContext verbs are legal on such a subscriber, and only the bare-string ones on
    // any other.
    //
    // A TopicFilterContext is a topic filter that knows what it is for: it carries the filter AND whatever the
    // consumer wants back when that filter matches, so the two travel together. The implementation is the
    // consumer's, and two obligations come with that -- EQUALITY, since removeTopicFilterContext finds one by
    // equals(), and IMMUTABILITY of the filter, since it is read both when registering and when removing. A
    // record gets both for free.
    //
    // What comes back is a SET of the consumer's own contexts, exactly as registered: several filters may
    // match one message, and two filters may carry the same context -- one destination reachable by two
    // patterns -- which appears once, because doing the work twice for one message is the failure to avoid.
    //
    // EACH FORM IS KEPT AND CALLED AS SUPPLIED. The subscriber holds four nullable fields, exactly one set,
    // and process() calls whichever it is -- making the already-complete future for a returning form, and
    // computing the destinations (destinationsFor(), beside process() in the ppf-loop) only for a context one. An
    // earlier version wrapped every form into a single common shape, which cost a lambda layer per form and
    // computed destinations for subscribers that had not asked to be told them.
    //
    @FunctionalInterface
    public interface TopicFilterContext {

        /// @return the topic filter to subscribe to. Must not change for the life of this object.
        @NotNull
        String topicFilter();
    }

    @FunctionalInterface
    public interface Processor {
        void process(final @NotNull PUBLISH message);
    }

    @FunctionalInterface
    public interface AsyncProcessor {

        /// @param message the message
        /// @return the completion of this consumer's work. Never null; return an already-complete future for
        ///     work that finished synchronously.
        @NotNull
        CompletableFuture<Void> process(final @NotNull PUBLISH message);
    }

    @FunctionalInterface
    public interface ContextProcessor<T extends TopicFilterContext> {

        /// @param message the message
        /// @param destinations the contexts of the filters this message matched. Empty only if every filter
        ///     it matched has been unsubscribed since the message was queued
        void process(final @NotNull PUBLISH message, final @NotNull Set<T> destinations);
    }

    @FunctionalInterface
    public interface AsyncContextProcessor<T extends TopicFilterContext> {

        /// @param message the message
        /// @param destinations the contexts of the filters this message matched. Empty only if every filter
        ///     it matched has been unsubscribed since the message was queued
        /// @return the completion of this consumer's work, as [AsyncProcessor#process]
        @NotNull
        CompletableFuture<Void> process(final @NotNull PUBLISH message, final @NotNull Set<T> destinations);
    }

    // endregion

    // region Subscriber TopicFilter Verbs -- the same two families on builder and subscriber
    // =================================================================================================================
    // with... -- ABSOLUTE: replace the whole set. add... / remove... -- RELATIVE. Each is overloaded for one or
    // a list. When DETACHED these only update the remembered set (replayed by the next attach()); when ATTACHED
    // the topic tree is reconciled immediately -- only the genuine additions and removals are pushed, so a
    // with...(wholeNewSet) becomes exactly the diff against what is currently registered.
    //
    // TWO FAMILIES, and a subscriber may use only its own: the bare-string one for a plain subscriber, the
    // ...TopicFilterContext one for a context subscriber. Which it is was settled by the processor it was built
    // with, so the wrong family is a mistake this can name rather than a state it has to represent.
    //
    // Separate names rather than overloads, and not by taste: withTopicFilter(List<String>) and
    // withTopicFilter(List<? extends TopicFilterContext>) erase to the same signature and do not compile side by
    // side. The names also say at the call site which kind of subscriber this is.
    //
    public synchronized @NotNull InternalTopicFilterSubscriber withTopicFilter(final @NotNull String topicFilter) {
        return withTopicFilter(List.of(topicFilter));
    }

    public synchronized @NotNull InternalTopicFilterSubscriber withTopicFilter(final @NotNull List<String> newFilters) {
        throwIfDeallocated();
        throwIfContextual("withTopicFilter");
        reconcileTo(new LinkedHashSet<>(newFilters));
        topicFilters.clear();
        topicFilters.addAll(newFilters);
        return this;
    }

    public synchronized @NotNull InternalTopicFilterSubscriber addTopicFilter(final @NotNull String topicFilter) {
        return addTopicFilter(List.of(topicFilter));
    }

    public synchronized @NotNull InternalTopicFilterSubscriber addTopicFilter(
            final @NotNull List<String> topicFiltersToAdd) {
        throwIfDeallocated();
        throwIfContextual("addTopicFilter");
        for (final String topicFilter : topicFiltersToAdd) {
            if (topicFilters.add(topicFilter) && attached) {
                subscribeFilterInTopicTree(topicFilter);
            }
        }
        return this;
    }

    public synchronized @NotNull InternalTopicFilterSubscriber removeTopicFilter(final @NotNull String topicFilter) {
        return removeTopicFilter(List.of(topicFilter));
    }

    public synchronized @NotNull InternalTopicFilterSubscriber removeTopicFilter(
            final @NotNull List<String> topicFiltersToRemove) {
        throwIfDeallocated();
        throwIfContextual("removeTopicFilter");
        for (final String topicFilter : topicFiltersToRemove) {
            if (topicFilters.remove(topicFilter) && attached) {
                unsubscribeFilterFromTopicTree(topicFilter);
            }
        }
        return this;
    }

    // -- the same three, for a subscriber built with a context processor ---------------------------------------
    //
    // A context carries its own filter, so these say WHAT to subscribe to and WHY in one object. Several
    // contexts may name one filter -- two destinations on one topic -- and the filter is subscribed once, with
    // the fan-out kept here.

    public synchronized @NotNull InternalTopicFilterSubscriber withTopicFilterContext(
            final @NotNull TopicFilterContext context) {
        return withTopicFilterContext(List.of(context));
    }

    public synchronized @NotNull InternalTopicFilterSubscriber withTopicFilterContext(
            final @NotNull List<? extends TopicFilterContext> contexts) {
        throwIfDeallocated();
        throwIfNotContextual("withTopicFilterContext");
        final Map<String, Set<TopicFilterContext>> target = new LinkedHashMap<>();
        for (final TopicFilterContext context : contexts) {
            target.computeIfAbsent(context.topicFilter(), ignored -> new LinkedHashSet<>())
                    .add(context);
        }
        // The tree is reconciled against the FILTERS; the contexts behind them are this object's own business,
        // so a filter kept by both sets is left alone in the tree even if its contexts changed entirely.
        reconcileTo(target.keySet());
        topicFilterContexts.clear();
        topicFilterContexts.putAll(target);
        return this;
    }

    public synchronized @NotNull InternalTopicFilterSubscriber addTopicFilterContext(
            final @NotNull TopicFilterContext context) {
        return addTopicFilterContext(List.of(context));
    }

    public synchronized @NotNull InternalTopicFilterSubscriber addTopicFilterContext(
            final @NotNull List<? extends TopicFilterContext> contexts) {
        throwIfDeallocated();
        throwIfNotContextual("addTopicFilterContext");
        for (final TopicFilterContext context : contexts) {
            final String topicFilter = context.topicFilter();
            final boolean isNewFilter = !topicFilterContexts.containsKey(topicFilter);
            topicFilterContexts
                    .computeIfAbsent(topicFilter, ignored -> new LinkedHashSet<>())
                    .add(context);
            if (isNewFilter && attached) {
                subscribeFilterInTopicTree(topicFilter);
            }
        }
        return this;
    }

    public synchronized @NotNull InternalTopicFilterSubscriber removeTopicFilterContext(
            final @NotNull TopicFilterContext context) {
        return removeTopicFilterContext(List.of(context));
    }

    public synchronized @NotNull InternalTopicFilterSubscriber removeTopicFilterContext(
            final @NotNull List<? extends TopicFilterContext> contexts) {
        throwIfDeallocated();
        throwIfNotContextual("removeTopicFilterContext");
        for (final TopicFilterContext context : contexts) {
            final String topicFilter = context.topicFilter();
            final Set<TopicFilterContext> remaining = topicFilterContexts.get(topicFilter);
            if (remaining == null) {
                continue;
            }
            remaining.remove(context);
            if (remaining.isEmpty()) {
                // Nothing wants this filter any more, so the filter goes too. Keeping it would leave a
                // subscription whose messages resolve to no destination -- accepted into the queue, then dropped.
                topicFilterContexts.remove(topicFilter);
                if (attached) {
                    unsubscribeFilterFromTopicTree(topicFilter);
                }
            }
        }
        return this;
    }

    /// Reconciles the topic tree from the currently subscribed filters to the given target.
    ///
    /// Does nothing while detached -- there is nothing registered to reconcile, and the caller's new filter set
    /// is replayed in full by the next `attach()`.
    private void reconcileTo(final @NotNull Set<String> target) {
        if (!attached) {
            return;
        }
        final Set<String> current = hasTopicFilterContext() ? topicFilterContexts.keySet() : topicFilters;
        final Set<String> gone = new LinkedHashSet<>();
        for (final String existing : current) {
            if (!target.contains(existing)) {
                gone.add(existing);
            }
        }
        gone.forEach(this::unsubscribeFilterFromTopicTree);
        for (final String wanted : target) {
            if (!current.contains(wanted)) {
                subscribeFilterInTopicTree(wanted);
            }
        }
    }

    /// Whether this is a CONTEXT subscriber: one told which of its destinations each message is for, and so
    /// one whose filter verbs are the ...TopicFilterContext family.
    ///
    /// **Derived, never stored.** Supplying a context processor is exactly the consumer saying it wants this,
    /// so a flag beside it would be a second statement of one fact, able to disagree with the first.
    private boolean hasTopicFilterContext() {
        return contextProcessor != null || asyncContextProcessor != null;
    }

    /// The filters this subscriber is subscribed to, whichever kind it is.
    private @NotNull Set<String> currentFilters() {
        return hasTopicFilterContext() ? topicFilterContexts.keySet() : topicFilters;
    }

    private void throwIfContextual(final @NotNull String verb) {
        if (hasTopicFilterContext()) {
            throw new IllegalStateException("InternalTopicFilterSubscriber '" + clientId
                    + "' was built with a context processor, so "
                    + verb
                    + " does not apply to it; use "
                    + verb
                    + "Context instead");
        }
    }

    private void throwIfNotContextual(final @NotNull String verb) {
        if (!hasTopicFilterContext()) {
            throw new IllegalStateException("InternalTopicFilterSubscriber '" + clientId
                    + "' was not built with a context processor, so "
                    + verb
                    + " does not apply to it; use "
                    + verb.replace("Context", "")
                    + " instead, or build it with a context processor");
        }
    }

    // endregion

    // region Subscriber TopicTree Wiring -- every call this subscriber makes to the topic tree
    // =================================================================================================================
    // THE TOPIC TREE IS REACHED FROM HERE AND NOWHERE ELSE. Everything above and below calls these two, so
    // "what does this subscriber ask of the tree" is answered by reading one short region.
    //
    // subscribeFilterInTopicTree() / unsubscribeFilterFromTopicTree() -- the two operations, so the lifecycle
    //                     verbs and the reconciliation logic share one definition of each under this clientId.
    //
    // The trailing `null` in both calls is the topic tree's `sharedName` -- and it is null ON PURPOSE.
    // The topic tree distinguishes shared from non-shared subscriptions: a non-null sharedName makes
    // the subscription part of a shared group (load-balanced across the group's members). Some other
    // internal subscribers ARE shared and pass one (the bridge forwarder's sharedName is
    // "forwarder#{id}"; the combiner's is its uuid, with the subscriber id differing by a trailing
    // "#"). An InternalTopicFilterSubscriber is deliberately NON-shared: that is the whole point of
    // EDG-504 -- a non-shared internal subscriber so the topic tree deduplicates by client id and a
    // message matching several of our filters is delivered to our queue exactly once. So sharedName
    // stays null, and our `clientId` is the sole identity (no separate shared-group name).
    /// Registers one topic filter in the topic tree under this subscriber's client id, so that matching
    /// messages start landing in its queue.
    ///
    /// **No subscription identifier is recorded**, for either kind of subscriber. A subscriber told which of
    /// its destinations a message is for works that out when it HANDLES the message, from the message's own
    /// topic -- see [#destinationsFor] -- so nothing need be stamped onto the message when it is queued. That
    /// also means this method has no precondition a caller can forget: every registration path is
    /// interchangeable, and a new one needs only to call this.
    private void subscribeFilterInTopicTree(final @NotNull String topicFilter) {
        topicTree.addTopic(
                clientId,
                new Topic(topicFilter, qos, false, true, Topic.DEFAULT_RETAIN_HANDLING, null),
                SubscriptionFlag.getDefaultFlags(false, true, false),
                null); // sharedName -- null: non-shared (see note above)
    }

    /// Removes one topic filter from the topic tree, so that matching messages stop landing in this
    /// subscriber's queue. Messages already queued are unaffected.
    private void unsubscribeFilterFromTopicTree(final @NotNull String topicFilter) {
        topicTree.removeSubscriber(clientId, topicFilter, null); // sharedName -- null: non-shared
    }

    // endregion

    // region Subscriber Queue Wiring -- every call this subscriber makes to the client-queue persistence
    // =================================================================================================================
    // THE QUEUE PERSISTENCE IS REACHED FROM HERE AND NOWHERE ELSE, for the same reason as the topic tree
    // above: one region answers "what does this subscriber ask of its queue", and the verbs and the ppf-loop
    // read as what they decide rather than as which persistence call they make.
    //
    // All five take this subscriber's clientId as the QUEUE id -- the same string the topic tree knows it by,
    // which is what wires the two together (see clientId).

    /// Asks to be told when a message arrives, and starts a ppf-loop each time one does.
    private void callMeWhenAMessageArrives() {
        clientQueuePersistence.addPublishAvailableCallback(
                id -> sendPpfLoopCommand(PpfLoopCommand.WAKE, false), clientId);
    }

    /// Stops those notifications. Messages keep arriving in the queue; nothing wakes up to drain them.
    private void stopCallingMeWhenAMessageArrives() {
        clientQueuePersistence.removePublishAvailableCallback(clientId);
    }

    /// Reads the next message(s), answering a future. See [#fetched] for why this may return more than one
    /// even though a single packet id is passed.
    private @NotNull ListenableFuture<ImmutableList<PUBLISH>> readNextMessagesFromQueue() {
        return clientQueuePersistence.readNew(clientId, false, POLL_PACKET_IDS, PUBLISH_POLL_BATCH_SIZE_BYTES);
    }

    /// Deletes the message currently stamped in flight -- this subscriber's stand-in for the acknowledgement
    /// an MQTT client would send. See [#finish] for why it is keyed the way it is, and why QoS 0 needs none.
    private void deleteStampedMessageFromQueue() {
        FutureUtils.addExceptionLogger(
                clientQueuePersistence.remove(clientId, ClientQueuePersistenceImpl.SHARED_IN_FLIGHT_MARKER));
    }

    /// Destroys the queue and everything still in it. Terminal -- see [#deallocate].
    private void destroyQueue() {
        clientQueuePersistence.clear(clientId, false); // false == no tombstone
    }

    // endregion

    // region Subscriber PPF-Loop -- poll a message, process it, finish it, go round again
    // =================================================================================================================
    // Draining this subscriber's queue is one loop of three steps -- POLL a message, PROCESS it, FINISH it --
    // which then goes round again. "ppf" throughout this class is always those three. The loop is asynchronous
    // rather than a Java loop: each step chains to the next from a callback, because polling and processing
    // both answer with a future.
    //
    // AT MOST ONE PPF-LOOP EXISTS AT ANY MOMENT. That is the whole bound this pipeline has to keep: one
    // message handled at a time, in order. Two mechanisms enforce it, and they are separate things:
    //
    //   internalSubmit (the SingleWriter's, not ours) guarantees ppfLoopCtrl never runs concurrently with
    //       itself. Tasks submitted for one queue run strictly one after another -- whether inline under a
    //       work-in-progress counter (in memory) or on a dedicated writer thread (file-backed) -- so no two
    //       invocations of ppfLoopCtrl ever overlap, whichever thread submitted them.
    //   ppfLoopCtrl guarantees at most one ppf-loop, via loopActive, which NOTHING ELSE EVER WRITES.
    //
    // The five methods:
    //
    // sendPpfLoopCommand(PpfLoopCommand.START) -- the ONE entry point for every trigger that says "a message may be
    // available now": the
    //                     message-available callback and consume(). Submits ppfLoopCtrl(START).
    // ppfLoopCtrl(case) -- owns loopActive and is the only caller of poll(). Every transition of the loop
    //                     goes through it; see the four cases on the method itself.
    // poll() -- produces the next message and hands it to process(). Serves anything left over from the
    //                     previous read first, and only reads the queue again once those are gone -- a read
    //                     can return more than one message even when asked for one; see the fetched field.
    //                     An empty read or a failure ends the loop by calling back into ppfLoopCtrl.
    // process() -- hands the message to the processor and chains finish() off its completion.
    // finish() -- deletes the message from the queue, then goes round again via ppfLoopCtrl(CONTINUE).
    //
    // And two helpers of process(), for the context forms, which are told which destinations a message is for:
    //
    // destinationsFor() -- the contexts behind every filter of this subscriber that the message's topic
    //                     matches, deduplicated.
    // isMatchFilterTopic() -- whether one topic filter matches one concrete topic; standard MQTT matching.
    //
    // A TRIGGER THAT FINDS A LOOP RUNNING DOES NOTHING, AND LOSES NOTHING. It only says a message may be
    // available; the running loop will come round and read it. Likewise a paused subscriber: resuming
    // submits again. Both cases re-ask the question rather than stranding whatever arrived.
    //
    // WHY loopActive HAD TO BE ADDED. When a processor did its work before returning, one-message-at-a-time
    // was free: the whole chain ran as one task, and a trigger arriving meanwhile could only queue another
    // task behind it. A processor that answers a future returns at once, so the thread goes free while the
    // work continues elsewhere -- and a trigger then polls immediately, handing out a second message.
    //
    // WHY THE FLAG IS ABOUT THE LOOP AND NOT ABOUT A MESSAGE. Every name that described a message -- in
    // flight, handling, holding -- was false for part of the time the flag is true: after a processor's
    // future completes, and before the next read returns a value, no message is being handled at all. But
    // that stretch is precisely when a second read must not start. A loop is either running or it is not,
    // with no such gap, so the loop is what the flag is about.
    //
    // WHY consuming IS READ HERE. It was set and cleared by consume()/pause() and read by nothing on this
    // path, so pausing removed the message-available callback but did not stop a loop already running.
    // Deallocation is covered by the same clause rather than needing its own: deallocate() requires being
    // paused, so a dead subscriber is one whose consuming flag is already false -- which matters because
    // deallocation FREES THE CLIENT ID, and a replacement may already own the queue this loop would read.
    //
    // A PLAIN FIELD, NOT AN ATOMIC. ppfLoopCtrl() is reached either from sendPpfLoopCommand(PpfLoopCommand.START),
    // which puts it in the
    // SingleWriter's serialisation for this queue, or from a step of a loop that is already inside it -- so
    // no two invocations ever overlap, whichever thread submitted them, and the field needs no atomicity.

    /// What the ppf-loop IS, and -- as [#goalState] -- what it is being asked to be.
    ///
    /// **Named as adjectives**, where the commands of [PpfLoopCommand] are named as verbs. That is only a
    /// device for keeping the two vocabularies apart on sight: a state is something the loop is, a command is
    /// something asked of it, and confusing the two is how one word came to mean two things here before.
    private enum PpfLoopState {
        /// An iteration is in flight right now.
        ACTIVE,
        /// None is, but a message arriving will start one.
        WAITING,
        /// None is, and a message arriving will NOT start one. The state a subscriber is born in.
        PAUSED,
        /// Dead, and not revivable.
        TERMINATED
    }

    /// What one call asks of the ppf-loop. Each command names the state it asks for, and [#goalStateFor]
    /// decides whether that ask takes. Named as verbs, against the adjectives of [PpfLoopState].
    private enum PpfLoopCommand {
        /// A message is available; run an iteration. Sent by the message-available callback.
        WAKE,
        /// That iteration finished a message; run another. Sent by [#finish].
        CONTINUE,
        /// That iteration failed; run another. Until there is anything cleverer -- a backoff, an attempt
        /// limit -- a failure is simply retried, so this asks for the same thing CONTINUE does.
        RESTART,
        /// Be draining. Sent by [#consume].
        CONSUME,
        /// That iteration found nothing to read; settle. Sent by [#poll].
        IDLE,
        /// Stop draining. Sent by [#pause].
        PAUSE,
        /// Be destroyed. Sent by [#deallocate].
        TEARDOWN
    }

    /// Sends one command to the ppf-loop, from any thread.
    ///
    /// **Submitted rather than called, and that is what makes the loop safe.** The submit puts the call inside
    /// the SingleWriter's serialisation for this queue, which is what guarantees [#ppfLoopCtrl] never runs
    /// concurrently with itself -- and so what lets [#loopState] and [#goalState] be plain fields.
    ///
    /// @param fromActiveLoop true iff the sender IS the iteration currently in flight, reporting back. Only
    ///     such a command may pass the one-loop guard; everything else is recorded in the goal and waits.
    private void sendPpfLoopCommand(final @NotNull PpfLoopCommand command, final boolean fromActiveLoop) {
        singleWriterService.getQueuedMessagesQueue().submit(clientId, bucketIndex -> {
            ppfLoopCtrl(command, fromActiveLoop);
            return null;
        });
    }

    /// The goal a command asks for, given the goal already standing. **The first of the loop's two matrices.**
    ///
    /// Each command names a state it wants; this decides whether the ask takes. Two rules, and only two:
    ///
    ///   - **TERMINATED absorbs.** Once destruction is the goal, nothing lowers it.
    ///   - **PAUSED is not overridden by the loop's own progress.** A pause asked for mid-iteration must
    ///     survive the CONTINUE that ends that iteration, or it is lost. So CONTINUE, RESTART, WAKE and IDLE
    ///     leave a PAUSED goal alone; only CONSUME lifts it.
    private static @NotNull PpfLoopState goalStateFor(
            final @NotNull PpfLoopState goal, final @NotNull PpfLoopCommand command) {
        if (goal == PpfLoopState.TERMINATED) {
            return PpfLoopState.TERMINATED;
        }
        return switch (command) {
            case TEARDOWN -> PpfLoopState.TERMINATED;
            case PAUSE -> PpfLoopState.PAUSED;
            case CONSUME -> PpfLoopState.ACTIVE;
            // The loop's own reports. They say what just happened, so they may move a goal that is already
            // about draining -- but they must not resurrect a paused subscriber.
            case WAKE, CONTINUE, RESTART -> goal == PpfLoopState.PAUSED ? PpfLoopState.PAUSED : PpfLoopState.ACTIVE;
            case IDLE -> goal == PpfLoopState.PAUSED ? PpfLoopState.PAUSED : PpfLoopState.WAITING;
        };
    }

    /// Drives the ppf-loop: records what was asked, refuses to act while an iteration is in flight, then moves
    /// [#loopState] towards [#goalState]. **The only place either is written.**
    ///
    /// **Record, then guard, then act** -- and the order is the whole design. A command may arrive while an
    /// iteration is in flight, with a message in the consumer's processor, so it is recorded in the goal first
    /// and acted on later, when the iteration that was running comes round. Acting before the guard -- which
    /// an earlier version did for TEARDOWN -- destroys the queue out from under an iteration still reading it.
    private void ppfLoopCtrl(final @NotNull PpfLoopCommand command, final boolean fromActiveLoop) {

        // 1. RECORD -- the whole of the first matrix, in one line.
        goalState = goalStateFor(goalState, command);

        // 2. GUARD -- there is only ever one iteration in flight. Pass only if none is, or if this IS that
        // iteration reporting back. Anything turned away is already recorded in the goal above, and the
        // iteration in flight will act on it when it reports.
        //
        // fromActiveLoop is the CALLER's word, not something derived from the command, because only the sender
        // knows whether it is the running iteration speaking.
        if (loopState == PpfLoopState.ACTIVE && !fromActiveLoop) {
            return;
        }

        // 3. ACT -- the second matrix: move loopState towards goalState.
        switch (goalState) {
            // Destroy. Deregistering the clientId from the factory is tearDown()'s last step, so there can
            // never be two subscribers for one clientId.
            case TERMINATED -> {
                tearDown();
                loopState = PpfLoopState.TERMINATED;
            }
            // Stop draining. A message arriving later sends WAKE, which will not lift a PAUSED goal.
            case PAUSED -> loopState = PpfLoopState.PAUSED;
            // Settle: no iteration runs, but a message arriving will start one.
            case WAITING -> loopState = PpfLoopState.WAITING;
            // Run an iteration. poll() reports back with CONTINUE, RESTART or IDLE, and that report is what
            // decides the next goal.
            case ACTIVE -> {
                loopState = PpfLoopState.ACTIVE;
                poll();
            }
        }
    }

    /// Tears this subscriber down, on the SingleWriter thread and between iterations of the ppf-loop.
    ///
    /// **Everything here runs where the loop runs**, which is the whole point. Clearing [#fetched] needs no
    /// lock because nothing else touches it from another thread, and the client id is handed back to the
    /// factory only once this subscriber has stopped reading from and writing to its queue -- so no operation
    /// of this lifetime can land on the queue of whatever takes the id next.
    ///
    /// Idempotent: a second teardown finds this subscriber already dead and does nothing.
    private void tearDown() {
        if (deallocated) {
            return;
        }
        destroyQueue();
        // Messages a read had already handed over go with the queue: they left it when they were read, so
        // this is where they end.
        fetched.clear();
        // Symmetric with build(): the queue is destroyed, so the identity becomes available for reuse. LAST,
        // so nothing of this lifetime can still touch a queue the next owner of the id is using.
        factory.deregister(this);
        deallocated = true;
    }

    /// Produces the next message and hands it to [#process], the first P of the ppf-loop.
    ///
    /// **Called only by [#ppfLoopCtrl]**, so arriving here means a loop is running and this is its next step.
    ///
    /// **A read is issued only when nothing is left over from the last one.** A read can return more than one
    /// message even though we ask for a single packet id -- see [#fetched] for why -- and every returned
    /// message must be processed, because a returned QoS 0 message has already been removed from the queue
    /// and exists nowhere else. So the leftovers are served first, and the queue is read again only once they
    /// are gone.
    ///
    /// **Both outcomes of the read are handled**, which is why this uses a callback with an explicit failure
    /// method rather than a success-only transform. A read whose future completes exceptionally would
    /// otherwise run nothing at all, leaving [#loopActive] set for ever: the subscriber would accept messages
    /// into its queue and never read them again, silently, and pausing and resuming would not help. The
    /// enclosing `catch` does not cover that -- a future completing exceptionally is not a throw from the
    /// `try` block; it only covers a throw from ISSUING the read.
    private void poll() {
        final PUBLISH leftover = fetched.poll();
        if (leftover != null) {
            process(leftover);
            return;
        }
        try {
            Futures.addCallback(
                    readNextMessagesFromQueue(),
                    new FutureCallback<>() {
                        @Override
                        public void onSuccess(final @Nullable ImmutableList<PUBLISH> messages) {
                            if (messages == null || messages.isEmpty()) {
                                sendPpfLoopCommand(PpfLoopCommand.IDLE, true);
                                return;
                            }
                            // EVERYTHING the read returned, not just the first: see [#fetched].
                            fetched.addAll(messages);
                            process(fetched.poll());
                        }

                        @Override
                        public void onFailure(final @NotNull Throwable t) {
                            log.error(
                                    "Failed to read a message for internal subscriber '{}': {}",
                                    clientId,
                                    t.getMessage());
                            sendPpfLoopCommand(PpfLoopCommand.RESTART, true);
                        }
                    },
                    MoreExecutors.directExecutor());
        } catch (final Throwable t) {
            log.error("Failed to poll internal subscriber '{}': {}", clientId, t.getMessage());
            sendPpfLoopCommand(PpfLoopCommand.RESTART, true);
        }
    }

    /// Hands one message to the processor, the second P of the ppf-loop.
    ///
    /// **The four forms are called here, in the shape the consumer supplied.** Exactly one field is non-null.
    /// A returning form has its already-complete future made here; only a context form has its destinations
    /// computed, so a consumer that did not ask to be told them pays nothing for them.
    ///
    /// **Success and failure finish alike**: whether the work succeeded is the consumer's business, and both
    /// outcomes mean this message is done with. A failed message is dropped -- this is a subscriber, not a
    /// retry mechanism.
    @SuppressWarnings("unchecked") // the set holds exactly the contexts this consumer registered
    private void process(final @NotNull PUBLISH message) {
        final CompletableFuture<Void> completion;
        try {
            if (plainProcessor != null) {
                plainProcessor.process(message);
                completion = CompletableFuture.completedFuture(null);
            } else if (asyncProcessor != null) {
                completion = asyncProcessor.process(message);
            } else if (contextProcessor != null) {
                ((ContextProcessor<TopicFilterContext>) contextProcessor).process(message, destinationsFor(message));
                completion = CompletableFuture.completedFuture(null);
            } else if (asyncContextProcessor != null) {
                completion = ((AsyncContextProcessor<TopicFilterContext>) asyncContextProcessor)
                        .process(message, destinationsFor(message));
            } else {
                // Unreachable -- build() rejects a subscriber with no processor. Stated so the compiler can
                // see that one of the four is always taken; the catch below logs it and the loop goes on.
                throw new IllegalStateException("Internal subscriber '" + clientId + "' has no processor");
            }
        } catch (final Exception e) {
            // Covers a returning processor throwing, and a future-returning one throwing before it produced a
            // future. Either way there is nothing to attach the finish to, so it happens here instead.
            log.error(
                    "Processor for internal subscriber '{}' threw; message will be dropped: {}",
                    clientId,
                    e.getMessage());
            finish(message);
            return;
        }
        // The terminal stage; the stage returned by whenComplete is intentionally dropped.
        final var unused = completion.whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                log.error(
                        "Failed to process message for internal subscriber '{}', message will be dropped: {}",
                        clientId,
                        throwable.getMessage());
            }
            finish(message);
        });
    }

    /// Acknowledges a finished message and goes round again, the F of the ppf-loop.
    ///
    /// **Acknowledging means deleting.** Nothing ever comes back from an internal subscriber, so this is the
    /// only thing that removes a message: reading one does not, for anything above QoS 0. The store leaves it
    /// in place stamped in flight, precisely so it survives until something confirms it was handled. QoS 0
    /// needs no call at all, because reading such a message removed it.
    ///
    /// The deletion is keyed by [ClientQueuePersistenceImpl#SHARED_IN_FLIGHT_MARKER] rather than a real wire
    /// packet id, so it deletes "the stamped message" -- sound only because at most one ppf-loop exists, and
    /// so at most one message is ever stamped. It is the NON-shared removal: storage keeps a separate map for
    /// shared subscriptions, and this subscriber is non-shared on every other side.
    ///
    /// **The acknowledgement and going round again cannot be separated.** A throw from the deletion would
    /// otherwise end the loop without ending it -- [#loopActive] left set and nothing to clear it -- which is
    /// the same silent stall the design exists to prevent, arriving through the one path meant to prevent it.
    /// Hence the `finally`.
    private void finish(final @NotNull PUBLISH message) {
        try {
            if (message.getQoS() != QoS.AT_MOST_ONCE) {
                deleteStampedMessageFromQueue();
            }
        } catch (final Exception e) {
            log.error("Failed to acknowledge message for internal subscriber '{}': {}", clientId, e.getMessage());
        } finally {
            sendPpfLoopCommand(PpfLoopCommand.CONTINUE, true);
        }
    }

    /// The destinations a message is for: the contexts behind every one of this subscriber's filters that its
    /// topic matches.
    ///
    /// **Matched when the message is HANDLED, not when it was queued.** The message carries its topic and this
    /// subscriber holds its filters, so the answer is computed from what is true NOW and nothing stored can go
    /// stale: a filter removed while the message waited does not match, one added while it waited does. The
    /// alternative -- stamping a per-filter subscription identifier on each queued message -- cached the answer
    /// at the wrong moment, needed those identifiers to survive detach/attach and a restart, and failed
    /// silently when they did not: the message arrived with no destinations and was dropped.
    ///
    /// **A union over the DESTINATIONS, not the filters**, since one destination may be reachable by two
    /// patterns and both may match. Deduplicating is what stops a device write being sent twice.
    private synchronized @NotNull Set<TopicFilterContext> destinationsFor(final @NotNull PUBLISH message) {
        return topicFilterContexts.entrySet().stream()
                .filter(entry -> isMatchFilterTopic(entry.getKey(), message.getTopic()))
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toSet());
    }

    /// Whether an MQTT topic filter matches a concrete topic.
    ///
    /// Standard MQTT matching: `+` matches exactly one level, `#` matches the remaining levels and also the
    /// parent itself (so `a/b/#` matches `a/b`), and both wildcards match an EMPTY level (so `a/+` matches
    /// `a/`).
    ///
    /// **Assumes both arguments are well formed** -- in the filter `+` and `#` each occupy a whole level and
    /// `#` is last; in the topic neither character appears. Both hold here: filters come from configuration
    /// that Edge validates, and topics come from messages the broker accepted.
    ///
    /// A single left-to-right walk over the two strings with no allocation and no splitting into levels. Two
    /// details carry the wildcards. The trailing `/#` is stripped up front and remembered, so the loop never
    /// sees it and a filter that ran out while the topic sits on a `/` still matches. And the `+` test comes
    /// BEFORE the character comparison, with the loop advancing on the FILTER alone -- that is what lets `+`
    /// match an empty level, where there is no character in the topic to compare against.
    ///
    /// The final read needs no bounds check: `||` evaluates its right side only when the topic cursor is not
    /// at the end, and the cursor never passes the end, so it is always in range.
    private static boolean isMatchFilterTopic(final @NotNull String filter, final @NotNull String topic) {
        boolean hasHashWildcard = false;
        int lenF = filter.length();
        if (1 == lenF && filter.charAt(0) == '#') {
            return true;
        }
        if (2 <= lenF && filter.charAt(lenF - 2) == '/' && filter.charAt(lenF - 1) == '#') {
            lenF = lenF - 2;
            hasHashWildcard = true;
        }

        final int lenT = topic.length();
        int iT = 0;

        for (int iF = 0; iF < lenF; iF++) {
            if (filter.charAt(iF) == '+') {
                while (iT < lenT && topic.charAt(iT) != '/') {
                    iT++;
                }
            } else if (iT < lenT && filter.charAt(iF) == topic.charAt(iT)) {
                iT++;
            } else {
                return false;
            }
        }
        return iT == lenT || (topic.charAt(iT) == '/' && hasHashWildcard);
    }

    // endregion

    // region Subscriber Lifecycle Verbs -- attach / consume / pause / detach / deallocate, start / stop
    // =================================================================================================================
    // attach() -- register the current topic filters in the topic tree. Messages now COLLECTED into
    //               the client queue. Idempotent.
    // consume() -- register the publish-available callback and drain anything already queued. Messages
    //               now PROCESSED (if attached). Legal while detached -- the callback is simply armed for
    //               when topics attach later. Idempotent.
    // pause() -- deregister the callback. Messages keep being COLLECTED (if attached) but are no
    //               longer PROCESSED. Idempotent.
    // detach() -- remove the current topic filters from the topic tree. Messages no longer COLLECTED.
    //               The remembered filter set survives, so a later attach() restores the same
    //               subscription. Idempotent.
    // deallocate() -- detach, pause, then ask the ppf-loop to clear the client queue entry from persistence
    //               and free the client id. TERMINAL: every verb throws afterwards, except deallocate()
    //               and stop(), which are idempotent no-ops.
    //
    // And two compositions of those five, which exist only to spare callers the common sequences:
    //
    // start() -- attach(); consume(). Order does not matter operationally; whichever runs second
    //               activates the flow. Throws on a dead subscriber, since it cannot be resurrected.
    // stop() -- detach(); pause(); deallocate(). Order DOES matter: detach first (stop the inflow),
    //               pause second (stop the processing), deallocate last (clear the now-quiet queue).
    //               The one composition that is safe on a dead subscriber; it returns quietly.
    //
    // All return `this` (except deallocate and stop) so they chain.
    //
    // THE FIVE VERBS ARE SYNCHRONIZED, THE TWO COMPOSITIONS ARE NOT. A verb holds state, so it takes the
    // monitor with the topic-filter verbs and state transitions never interleave with tree reconciliation.
    // A composition holds none of its own -- it is a sequence of verbs, each taking the monitor for its own
    // work -- so there is nothing for it to guard. consume() is the one verb split in two, for a reason
    // given at the method itself.
    //
    // ===========================================================================================
    // NO VERB EVER BLOCKS, AND NONE WAITS FOR THE PPF-LOOP TO ACT ON WHAT IT ASKED.
    // ===========================================================================================
    //
    // consume(), pause() and deallocate() each SEND a command and return. When the loop acts on it is not
    // specified, and none of them waits to find out. So a verb returning means the request was made, NOT that
    // it has taken effect -- pause() in particular does not promise that no further message reaches the
    // processor; see the method for the exact wording.
    //
    // WHY NOT WAIT. The obvious improvement -- wait on the future the submit hands back -- deadlocks, and not
    // rarely. The in-memory SingleWriter has no thread of its own: whichever thread submits first DRAINS the
    // queue inline, and a work-in-progress counter stops anyone else draining meanwhile. So:
    //
    //   1. A message arrives. Thread T submits, sees the counter at zero, and becomes the drainer.
    //   2. Draining runs the loop, which reads a message and hands it to the component's processor -- still
    //      on T, still inside the drain loop.
    //   3. The processor calls pause() on its own subscriber. That submits a command; the submit finds the
    //      counter NON-ZERO (T's own drain loop holds it) and so does not drain. It returns an uncompleted
    //      future.
    //   4. pause() waits on that future.
    //
    // T is now waiting for a task only T can run. No cycle between threads, no monitors involved -- one
    // thread blocked on work it is itself responsible for executing. It hangs every time, not sometimes.
    //
    // NOTHING DETECTS THIS AT RUNTIME. Java's deadlock detection sees monitor cycles; this is neither. A
    // timeout would convert the hang into a pause that silently did not happen. A check for "am I the
    // drainer" is not implementable: the writer records THAT someone is draining, not WHO.
    //
    // SO IT IS PREVENTED STRUCTURALLY, by three rules each checkable by reading one method:
    //
    //   1. No verb here blocks.
    //   2. Nothing submitted to the SingleWriter waits on another submit.
    //   3. Consumer code is called only from inside a submitted task, never with a monitor held.
    //
    // From which: no thread ever waits for the writer, so no thread can wait for work it might itself run,
    // so the cycle cannot form. A future change that adds a wait breaks rule 1, and nothing will complain --
    // which is why the rules are written here rather than left to be re-derived.
    //
    public synchronized @NotNull InternalTopicFilterSubscriber attach() {
        throwIfDeallocated();
        if (attached) {
            return this;
        }
        for (final String topicFilter : currentFilters()) {
            subscribeFilterInTopicTree(topicFilter);
        }
        attached = true;
        return this;
    }

    /// Starts draining the queue: registers the message-available callback, marks this subscriber as
    /// consuming, and starts the first ppf-loop. Idempotent -- a second call while already consuming does
    /// nothing.
    ///
    /// The callback covers every message arriving from NOW on. Messages already in the queue -- from a
    /// previous run, or collected while attached but paused -- are not covered by it, which is what that
    /// first loop is for.
    ///
    /// **This verb is split in two, and it is the only one that is.** [#consumeDontStartPpfLoop] does the
    /// state change under this object's monitor and answers whether this call is the one that started
    /// consuming; the loop is started out here, after that method has returned and the monitor is released.
    ///
    /// The reason is that starting a ppf-loop can run the consumer's own processor INLINE on the calling
    /// thread: the in-memory single writer executes a submitted task immediately when nothing else holds its
    /// bucket, so a whole poll-process-finish cycle can happen before [#startPpfLoop] returns. Doing that
    /// with the monitor held would mean a processor that blocks waiting on a thread which wants the monitor
    /// deadlocks, and a processor calling back into a verb observes half-finished state. **No processor is
    /// ever called with this monitor held**, and this split is where that is earned.
    ///
    /// Splitting also fixes the order. `consuming` is already true by the time the loop starts, so a processor
    /// that calls `pause()` during that first inline loop is honoured rather than silently discarded -- with
    /// the flag still false it would hit the "not consuming" guard in [#pause] and do nothing.
    public @NotNull InternalTopicFilterSubscriber consume() {
        if (consumeDontStartPpfLoop()) {
            sendPpfLoopCommand(PpfLoopCommand.CONSUME, false);
        }
        return this;
    }

    private synchronized boolean consumeDontStartPpfLoop() {
        throwIfDeallocated();
        if (consuming) {
            return false;
        }
        callMeWhenAMessageArrives();
        consuming = true;
        return true;
    }

    /// Stops draining the queue. Messages keep being collected if attached; nothing processes them.
    ///
    /// **WHAT THIS PROMISES.** The message-available callback is deregistered before this returns, so nothing
    /// further will wake the subscriber. And a PAUSE command is sent, which the ppf-loop acts on at its next
    /// decision point: from then on it hands no message to the processor.
    ///
    /// **WHAT IT DOES NOT PROMISE.** That the loop has seen it yet. The command is submitted, not executed,
    /// so a message may still be handed over in the window between this returning and the loop acting. Nor
    /// does it promise that a message already WITH the processor has finished -- a component that needs to
    /// know the last one is done must learn that from its own processor, not from this call returning.
    ///
    /// **Why it does not wait for either.** Waiting deadlocks, by a route worth reading before ever adding
    /// one -- see the rules and the worked example at the top of this region.
    ///
    /// **Why a promise about ordering is statable at all.** This call and the processor run on different
    /// threads, and two events on different threads have NO intrinsic order -- wall-clock "before" means
    /// nothing between them. An order exists only where Java's happens-before relation puts one, so a claim
    /// about what has stopped is only as good as the edge that establishes it. Here that edge is the PAUSE
    /// command sent below, which travels through the SingleWriter's queue for this subscriber: the loop sees
    /// it in order with its own work.
    public synchronized @NotNull InternalTopicFilterSubscriber pause() {
        throwIfDeallocated();
        if (!consuming) {
            return this;
        }
        stopCallingMeWhenAMessageArrives();
        consuming = false;
        // So a pause takes effect on an iteration already under way, rather than only on the next one. The
        // flag above is the standing answer to "may I poll at all"; this is the prompt to stop now.
        sendPpfLoopCommand(PpfLoopCommand.PAUSE, false);
        return this;
    }

    public synchronized @NotNull InternalTopicFilterSubscriber detach() {
        throwIfDeallocated();
        if (!attached) {
            return this;
        }
        for (final String topicFilter : currentFilters()) {
            unsubscribeFilterFromTopicTree(topicFilter);
        }
        attached = false;
        return this;
    }

    /// The one terminal verb, and the one exception to [#throwIfDeallocated]: calling it on an already-dead
    /// subscriber is a no-op rather than an error.
    ///
    /// **Destroys the queue, and everything in it.** Messages collected but not yet processed are dropped --
    /// which is why a long-lived subscriber must eventually stop rather than merely detach, or its queue and
    /// the memory behind it linger for ever.
    ///
    /// **ASYNCHRONOUS: this asks, the ppf-loop does.** It returns once the request is submitted, not once the
    /// subscriber is dead. The work itself happens on the SingleWriter thread at a point between iterations
    /// of the loop -- a message already with the consumer's processor is allowed to finish first -- which is
    /// what stops a teardown racing a loop that is reading from or acknowledging to the very queue it
    /// destroys. See [#tearDown].
    ///
    /// **No precondition.** It used to demand being detached and paused, because it could not safely tear
    /// down a live subscriber. Now it detaches and pauses on the caller's behalf and lets the loop finish.
    public void deallocate() {
        if (deallocated) {
            return;
        }
        detach();
        pause();
        sendPpfLoopCommand(PpfLoopCommand.TEARDOWN, false);
    }

    private void throwIfDeallocated() {
        if (deallocated) {
            throw new IllegalStateException(
                    "InternalTopicFilterSubscriber '" + clientId + "' has been deallocated and cannot be used");
        }
    }

    public @NotNull InternalTopicFilterSubscriber start() {
        attach();
        consume();
        return this;
    }

    public void stop() {
        deallocate(); // which detaches and pauses first, then asks the ppf-loop to tear down
    }

    // endregion

}
