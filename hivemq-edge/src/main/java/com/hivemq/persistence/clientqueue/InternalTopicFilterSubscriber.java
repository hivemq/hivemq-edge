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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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

    private static final @NotNull Logger log = LoggerFactory.getLogger(InternalTopicFilterSubscriber.class);

    public static final @NotNull String INTERNAL_SUBSCRIBER_PREFIX = "$INTERNAL::";

    // SHARED_IN_FLIGHT_MARKER acts as a boolean inflight flag -- not a real wire packet ID, since
    // messages never go to an MQTT client. removeShared() uses uniqueId, not the packet ID, so
    // the value here does not matter.
    private static final @NotNull ImmutableIntArray POLL_PACKET_IDS =
            ImmutableIntArray.of(ClientQueuePersistenceImpl.SHARED_IN_FLIGHT_MARKER);

    // The largest MQTT subscription identifier: a variable byte integer, so 2^28 - 1. Topic's constructor
    // asserts the range, and 0 is reserved -- so the usable space is [1, MAX_SUBSCRIPTION_ID].
    private static final int MAX_SUBSCRIPTION_ID = 268_435_455;

    // At most half the space may be subscribed at once, and that bound is what makes allocation terminate
    // rather than merely usually terminate: with under half the identifiers taken, no run of consecutive taken
    // values can span the whole space, so the search below always finds a free one. Raising this towards the
    // full space would take that guarantee away, which is why it is stated here rather than tuned.
    private static final int MAX_TOPIC_FILTERS = MAX_SUBSCRIPTION_ID / 2;

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

    // The three Edge singletons this subscriber operates against. Supplied by the factory (which has
    // them injected), so callers never see or thread them. Final -- set once at construction.
    private final @NotNull LocalTopicTree topicTree;
    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull SingleWriterService singleWriterService;

    // The per-message handler. Set once at construction (Builder requires one of the two forms). Always the
    // async form internally: a synchronous Processor is wrapped by the builder, so the pipeline has one shape.
    private final @NotNull AsyncProcessor processor;

    // The factory that built this subscriber, kept as a back-reference so that build()/deallocate() can
    // (de)register this subscriber in the factory's registry -- see isExcludedIngressClientId() below.
    private final @NotNull InternalTopicFilterSubscriberFactory factory;

    // excludedIngressClientId -- ingress-exclusion (generalized No Local). If set, a message whose
    //            INGRESS client id (the publishing client, i.e. the distributor's `sender`) equals this
    //            value must NOT be delivered to us. Set once via the builder, immutable after build();
    //            null means "no exclusion -- accept from everyone". The check happens at distribution
    //            time (PublishDistributorImpl), BEFORE the message is queued, so an excluded message
    //            never enters our queue. See isExcludedIngressClientId().
    private final @Nullable String excludedIngressClientId;

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

    // Which kind of subscriber this is, and so which family of filter verbs is legal. NOT a separate
    // declaration: it follows from the processor supplied, since withContextProcessor is exactly the consumer
    // saying it wants to be told what a message matched. A flag beside it could disagree with it.
    private final boolean contextual;

    // -- mutable lifecycle state, all touched only via the verbs below ----------------------------
    //
    // A subscriber is one of two kinds, and which it is follows from the processor it was built with:
    // withProcessor/withAsyncProcessor make a PLAIN one, withContextProcessor a CONTEXT one. The kind decides
    // which family of filter verbs is legal (the other family throws) and which structures below are used --
    // a plain subscriber never touches the three context maps, and a context subscriber never uses topicFilters.
    // See contextual() and the filter-verb region.

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

    // The two directions between a filter and its MQTT subscription identifier. Inverses of each other, and
    // maintained only by allocate/free below, which is what keeps them so.
    //
    //   subscriptionIdByTopicFilter -- used when registering a filter in the tree, and when removing one.
    //   topicFilterBySubscriptionId -- used on the DELIVERY path: a message carries the identifiers it matched,
    //                                  and this is what turns them back into filters and so into contexts.
    //
    // An identifier is assigned once per filter and kept for as long as that filter is subscribed, so it
    // survives every detach()/attach(). Re-allocating on re-attach would stay internally consistent and break
    // only when a message queued BEFORE the detach is read after it, carrying an identifier this subscriber no
    // longer knows -- and the identifier travels with the queued message, so that window is real.
    private final @NotNull Map<String, Integer> subscriptionIdByTopicFilter = new HashMap<>();

    private final @NotNull Map<Integer, String> topicFilterBySubscriptionId = new HashMap<>();

    // Where the next allocation starts looking. Identifiers are per subscriber, not global: two subscribers may
    // both use 1 without interfering.
    private int nextSubscriptionId = 0;

    // attached -- true iff the current topicFilters are registered in the topic tree (messages are
    //            being collected into the client queue).
    private boolean attached = false;

    // consuming -- true iff this subscriber is draining its queue: the publish-available callback is
    //             registered AND the poll pipeline will act on a trigger. Read by pollUnlessBusy(), which
    //             is what makes pause() stop work already under way rather than only future wake-ups.
    private boolean consuming = false;

    // deallocated -- true once deallocate() has cleared the queue. TERMINAL: once deallocated the
    //               subscriber is dead and cannot be resurrected; every verb is a no-op (or, for
    //               attach/consume, rejected) from then on. This flag makes deallocate()/stop() safely
    //               idempotent and guards the other verbs against use-after-deallocate.
    private boolean deallocated = false;

    // inFlight -- true from the moment a message is claimed for processing until its future settles.
    //            THE ONE-MESSAGE-AT-A-TIME BOUND, and the only thing that states it: see the poll
    //            pipeline's region note for why a processor answering a future made it necessary, and
    //            why this is a plain field rather than an atomic.
    private boolean inFlight = false;

    // endregion

    // region constructor -- package-private, only the factory/builder construct this
    // =================================================================================================================
    // componentPrefix -- identifies the Edge component type (e.g. "tynebridge", "combiner", "sampler").
    //                   Must be unique across all components in Edge. Becomes the middle segment of
    //                   the internal client ID.
    // instanceId -- identifies this specific subscriber instance within the component. Must be
    //                   unique within the componentPrefix namespace. Typically derived from the
    //                   configuration ID of the owning instance.
    // processor -- the per-message handler (runs on the SingleWriter thread).
    // initialFilters -- the topic filter set assembled in the builder (may be empty; that is valid).
    // excludedIngressClientId -- ingress-exclusion id, or null for none (see the field above).
    // factory -- the factory that built us, used as the registry for ingress-exclusion lookups.
    //
    InternalTopicFilterSubscriber(
            final @NotNull String componentPrefix,
            final @NotNull String instanceId,
            final @Nullable AsyncProcessor processor,
            final @Nullable AsyncContextProcessor<?> contextProcessor,
            final @NotNull Set<String> initialFilters,
            final @NotNull Map<String, Set<TopicFilterContext>> initialContexts,
            final @NotNull QoS qos,
            final @Nullable Long queueLimit,
            final @Nullable QueuedMessagesStrategy queueOverflow,
            final @Nullable String excludedIngressClientId,
            final @NotNull InternalTopicFilterSubscriberFactory factory,
            final @NotNull LocalTopicTree topicTree,
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull SingleWriterService singleWriterService) {
        this.clientId = INTERNAL_SUBSCRIBER_PREFIX + componentPrefix + "::" + instanceId;
        this.qos = qos;
        this.queueLimit = queueLimit;
        this.queueOverflow = queueOverflow;
        // One shape internally: a context processor becomes an async one by resolving a message's identifiers
        // back to contexts. The consumer never sees an integer.
        //
        // Exactly one of the two is non-null -- build() rejects both and neither -- but the constructor cannot
        // see that, so it is checked here rather than assumed. This is the only construction path.
        if (processor != null) {
            this.processor = processor;
            this.contextual = false;
        } else if (contextProcessor != null) {
            this.processor = asAsync(contextProcessor);
            this.contextual = true;
        } else {
            throw new IllegalArgumentException(
                    "InternalTopicFilterSubscriber needs a processor or a context processor, not neither");
        }
        // The filters, in whichever form this subscriber's kind uses. Each context filter is assigned its
        // identifier here, so it survives every later detach()/attach().
        if (contextual) {
            initialContexts.forEach((filter, contexts) -> {
                topicFilterContexts.put(filter, new LinkedHashSet<>(contexts));
                subscriptionIdFor(filter);
            });
        } else {
            this.topicFilters.addAll(initialFilters);
        }
        this.excludedIngressClientId = excludedIngressClientId;
        this.factory = factory;
        this.topicTree = topicTree;
        this.clientQueuePersistence = clientQueuePersistence;
        this.singleWriterService = singleWriterService;
    }

    // endregion

    // region Processor -- the per-message handler supplied by the user (a lambda)
    // =================================================================================================================
    // Called once per message, on the SingleWriter thread that performs every operation for this queue's
    // whole bucket -- see the threading note on the class. So this is for work small enough to do there:
    // storing a value, handing the message to another thread's queue. Anything slower, and anything that
    // may block, wants AsyncProcessor instead. Any exception thrown is caught, logged, message dropped.
    //
    // Deliberately NOT named MessageProcessor / MessageTransformer -- those names belong to the
    // MessageFabric design, the long-term home. This is the interim ITFS-local type; its signature is
    // exactly the old abstract process(PUBLISH), so an old override body ports into a lambda verbatim.
    //
    @FunctionalInterface
    public interface Processor {
        void process(final @NotNull PUBLISH message);
    }

    // endregion

    // region AsyncProcessor -- for work too slow, or too blocking, for the queue's own thread
    // =================================================================================================================
    // Same job as Processor, but the work does not happen ON the calling thread. The consumer starts it
    // elsewhere and returns a future AT ONCE; the next message is passed on when that future completes.
    //
    // WHY, in the order the reasons actually run (see the threading note on the class): message handling is
    // sequential because MQTT's ordering guarantees require it; that is achieved cheaply by giving each
    // bucket of queues one dedicated thread; and it follows that a processor must not block that thread for
    // any extended period, since every queue in the bucket waits behind it.
    //
    // A Processor has nowhere to put slow work: it says "finished" by returning, so the only way to stay
    // sequential would be to do the work first, on that thread. This interface separates the two -- the work
    // moves off the thread, the ordering stays.
    //
    // The bound itself is NOT what this buys: handling is sequential in both forms, one message in flight
    // per component. This is what lets a consumer keep that bound without holding the thread meanwhile.
    //
    // Returning rather than being handed a callback, deliberately. A callback can be forgotten -- which
    // compiles, and then stalls this queue permanently and silently -- and can be called twice, which
    // acknowledges twice and reads two next messages, destroying the very bound this is for. A future
    // completes once by construction, and a method that must return something cannot silently return nothing.
    //
    // "Completes" means SETTLED, either way. A failure releases the queue exactly as a success does; whether
    // the work succeeded is the consumer's business, and both outcomes mean "this one is no longer in flight".
    //
    // A Processor is the degenerate case of this one -- a future already complete when it returns -- and is
    // wrapped into exactly that by the builder. The pipeline below knows only this interface.
    //
    @FunctionalInterface
    public interface AsyncProcessor {

        /**
         * @param message the message
         * @return the completion of this consumer's work. Never null; return an already-complete future for
         *         work that finished synchronously.
         */
        @NotNull
        CompletableFuture<Void> process(final @NotNull PUBLISH message);
    }

    // endregion

    // region TopicFilterContext -- a topic filter that knows what it is for
    // =================================================================================================================
    // A subscriber standing in for several destinations needs to be told which of them a message is for. It
    // subscribes with these rather than with bare strings: each carries its filter AND whatever the consumer
    // needs back when that filter matches, so the two travel together instead of being paired up by the caller.
    //
    // The implementation is entirely the consumer's -- this interface asks for the filter and nothing else. Two
    // obligations come with that, and both are the implementor's to meet:
    //
    //   - EQUALITY, if the consumer means to remove one later. removeTopicFilterContext finds it by equals(),
    //     so an implementation with identity equality can only be removed by the very object registered. A
    //     record gets this for free and is the obvious choice.
    //   - IMMUTABILITY of the filter. It is read when the subscription is registered and again when it is
    //     removed; a filter that changed in between would leave a subscription nothing can take away.
    //
    @FunctionalInterface
    public interface TopicFilterContext {

        /**
         * @return the topic filter to subscribe to. Must not change for the life of this object.
         */
        @NotNull
        String topicFilter();
    }

    // endregion

    // region ContextProcessor / AsyncContextProcessor -- told WHICH of its filters a message matched
    // =================================================================================================================
    // TWO INDEPENDENT CHOICES, so four interfaces rather than three. How a consumer says it is finished --
    // by returning or by completing a future -- and what it is told about the message -- the message alone or
    // the message plus what it matched -- are unrelated questions, and either answer to one goes with either
    // answer to the other:
    //
    //                      told only the message      told what it matched
    //   done on return     Processor                  ContextProcessor
    //   done on a future   AsyncProcessor             AsyncContextProcessor
    //
    // A subscriber holding one filter, or several handled the same way, wants the left column; one standing in
    // for several destinations wants the right. Slow or blocking work wants the bottom row (see AsyncProcessor
    // for why); work small enough for the queue's own thread wants the top.
    //
    // SUPPLYING EITHER RIGHT-HAND FORM IS WHAT MAKES A SUBSCRIBER A CONTEXT ONE. There is no separate flag
    // saying so: the processor a consumer supplies already says which kind it wants, and a second statement of
    // that could disagree with the first. It follows that only the ...TopicFilterContext verbs are legal on
    // such a subscriber, and only the bare-string ones on any other.
    //
    // What comes back are the consumer's OWN contexts, exactly as registered. The MQTT subscription identifiers
    // that carry them never surface: this subscriber allocated them, so this subscriber is the only thing that
    // can translate them, and an integer would be meaningless to a consumer without the maps held here.
    //
    // A SET, because several filters may match one message and the tree returns all their identifiers. Two
    // filters may also carry the SAME context -- one destination reachable by two patterns -- and it appears
    // once, because doing the work twice for one message is the failure this is meant to avoid.
    //
    @FunctionalInterface
    public interface ContextProcessor<T extends TopicFilterContext> {

        /**
         * @param message the message
         * @param matched the contexts of the filters this message matched. Empty only if every filter it
         *         matched has been unsubscribed since the message was queued
         */
        void process(final @NotNull PUBLISH message, final @NotNull Set<T> matched);
    }

    @FunctionalInterface
    public interface AsyncContextProcessor<T extends TopicFilterContext> {

        /**
         * @param message the message
         * @param matched the contexts of the filters this message matched. Empty only if every filter it
         *         matched has been unsubscribed since the message was queued
         * @return the completion of this consumer's work, as {@link AsyncProcessor#process}
         */
        @NotNull
        CompletableFuture<Void> process(final @NotNull PUBLISH message, final @NotNull Set<T> matched);
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

        private final @NotNull String componentPrefix;
        private final @NotNull String instanceId;
        private final @NotNull InternalTopicFilterSubscriberFactory factory;
        private final @NotNull LocalTopicTree topicTree;
        private final @NotNull ClientQueuePersistence clientQueuePersistence;
        private final @NotNull SingleWriterService singleWriterService;

        private @Nullable AsyncProcessor processor = null; // required; checked in build()
        // Always the async form internally, as with `processor` above: a synchronous ContextProcessor is
        // wrapped by withContextProcessor, so the pipeline has one shape.
        private @Nullable AsyncContextProcessor<?> contextProcessor = null;
        private final @NotNull Set<String> topicFilters = new LinkedHashSet<>();
        private final @NotNull Map<String, Set<TopicFilterContext>> topicFilterContexts = new LinkedHashMap<>();
        private @NotNull QoS qos = QoS.AT_LEAST_ONCE; // optional; the QoS every filter is registered at
        private @Nullable Long queueLimit = null; // optional; null means the broker-wide default
        private @Nullable QueuedMessagesStrategy queueOverflow = null; // optional; null means the broker default
        private @Nullable String excludedIngressClientId = null; // optional; ingress-exclusion

        // Package-private -- only the factory creates builders (it supplies the injected singletons and
        // itself, as the registry the built subscriber registers with on attach()).
        Builder(
                final @NotNull String componentPrefix,
                final @NotNull String instanceId,
                final @NotNull InternalTopicFilterSubscriberFactory factory,
                final @NotNull LocalTopicTree topicTree,
                final @NotNull ClientQueuePersistence clientQueuePersistence,
                final @NotNull SingleWriterService singleWriterService) {
            this.componentPrefix = componentPrefix;
            this.instanceId = instanceId;
            this.factory = factory;
            this.topicTree = topicTree;
            this.clientQueuePersistence = clientQueuePersistence;
            this.singleWriterService = singleWriterService;
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
            // Wrapped into the async form so the pipeline has one shape. The catch is not decoration: a
            // consumer may throw before any future exists, and an escaping exception would skip the
            // acknowledge-and-poll and stall this queue for good. The old pipeline's try/catch did exactly
            // this, and turning the throw into a failed future preserves it -- both settle the future, and a
            // settled future is what releases the queue.
            this.processor = message -> {
                try {
                    processor.process(message);
                    return CompletableFuture.completedFuture(null);
                } catch (final Exception e) {
                    return CompletableFuture.failedFuture(e);
                }
            };
            return this;
        }

        // Required, alternative form: for work too slow, or too likely to block, to run on the thread that
        // serves this queue's bucket. The consumer starts it elsewhere and returns a future immediately; the
        // next message is passed on when that future completes, so handling stays sequential without the
        // thread being held meanwhile.
        public @NotNull Builder withAsyncProcessor(final @NotNull AsyncProcessor processor) {
            this.processor = processor;
            return this;
        }

        // Required, third and fourth forms: as the two above, plus what the message matched.
        //
        // SUPPLYING EITHER DECIDES THE KIND OF SUBSCRIBER. It is what makes the ...TopicFilterContext verbs
        // the legal ones, on the builder and on the subscriber alike, and the bare-string ones illegal. There
        // is deliberately no separate flag saying so: the processor already says it, and two statements of one
        // fact could disagree -- the more easily because the filters may be added far from where the
        // subscriber is built.
        //
        // Whether to be told what a message matched, and whether the work needs a future, are independent
        // choices -- hence four forms rather than three.
        @SuppressWarnings("unchecked") // the set holds exactly the contexts this consumer registered
        public <T extends TopicFilterContext> @NotNull Builder withContextProcessor(
                final @NotNull ContextProcessor<T> processor) {

            // Wrapped into the async form for the same reason withProcessor is, and with the same catch: a
            // consumer may throw before any future exists, and an escaping exception would stall this queue.
            this.contextProcessor = (AsyncContextProcessor<T>) (message, matched) -> {
                try {
                    processor.process(message, matched);
                    return CompletableFuture.completedFuture(null);
                } catch (final Exception e) {
                    return CompletableFuture.failedFuture(e);
                }
            };
            return this;
        }

        public <T extends TopicFilterContext> @NotNull Builder withAsyncContextProcessor(
                final @NotNull AsyncContextProcessor<T> processor) {

            this.contextProcessor = processor;
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

        // Optional: ingress-exclusion (generalized No Local). A message whose ingress (publishing)
        // client id equals this value will not be delivered to the built subscriber. Build-time only;
        // immutable after build(). For a bridge, this is the peer it forwards to -- so a message it sent
        // us is not sent straight back. The plain No Local case is excludedIngressClientId == our own id.
        public @NotNull Builder withExcludedIngressClientId(final @NotNull String excludedIngressClientId) {
            this.excludedIngressClientId = excludedIngressClientId;
            return this;
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

        /**
         * Rejects an absolute filter call once any filter has been declared.
         * <p>
         * Checks BOTH families, so declaring a bare filter and then a context (or the reverse) is caught here,
         * at the call that did it, rather than at {@code build()} -- which can only name the filters, not the
         * call that added them.
         *
         * @param absolute the verb being called, named in the message
         * @param relative the verb the caller almost certainly meant
         */
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

        // Produce the live subscriber. The subscriber is constructed in the IDLE state -- detached and
        // paused -- i.e. no messages flow until start() (or attach()/consume()) is called. It is, however,
        // already REGISTERED with the factory: the subscriber owns its clientId ("$INTERNAL::<prefix>::
        // <instanceId>") from build() until deallocate(). register() throws if that identity is already in
        // use, so a duplicate componentPrefix::instanceId is rejected here rather than silently colliding.
        public @NotNull InternalTopicFilterSubscriber build() {
            if (processor == null && contextProcessor == null) {
                throw new IllegalStateException("InternalTopicFilterSubscriber requires a processor; call one of "
                        + "withProcessor(...), withAsyncProcessor(...), withContextProcessor(...) or "
                        + "withAsyncContextProcessor(...) before build()");
            }
            if (processor != null && contextProcessor != null) {
                throw new IllegalStateException("InternalTopicFilterSubscriber takes one processor, not two; a "
                        + "context processor cannot be combined with withProcessor(...) or withAsyncProcessor(...)");
            }
            // The processor decides which family of filter verbs applies, so filters given in the other family
            // are a mistake -- and one worth naming here, since the two calls may be far apart.
            if (contextProcessor != null && !topicFilters.isEmpty()) {
                throw new IllegalStateException("InternalTopicFilterSubscriber was built with a context processor, "
                        + "so its filters must be given as contexts; these were given as bare strings: "
                        + topicFilters);
            }
            if (contextProcessor == null && !topicFilterContexts.isEmpty()) {
                throw new IllegalStateException("InternalTopicFilterSubscriber was given topic filter contexts but "
                        + "no context processor to deliver them to; call withContextProcessor(...) or "
                        + "withAsyncContextProcessor(...)");
            }

            final InternalTopicFilterSubscriber subscriber = new InternalTopicFilterSubscriber(
                    componentPrefix,
                    instanceId,
                    processor,
                    contextProcessor,
                    topicFilters,
                    topicFilterContexts,
                    qos,
                    queueLimit,
                    queueOverflow,
                    excludedIngressClientId,
                    factory,
                    topicTree,
                    clientQueuePersistence,
                    singleWriterService);
            factory.register(subscriber); // throws if clientId already in use -- deregistered by deallocate()
            return subscriber;
        }
    }

    // endregion

    // region topic-filter verbs -- the same two families on builder and subscriber
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
                addToTree(topicFilter);
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
                removeFromTree(topicFilter);
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
        final Set<String> goneFilters = reconcileTo(target.keySet());
        goneFilters.forEach(this::freeSubscriptionId);
        topicFilterContexts.clear();
        topicFilterContexts.putAll(target);
        // After the tree work, so a filter that survives keeps the identifier it was registered under.
        target.keySet().forEach(this::subscriptionIdFor);
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
            if (isNewFilter) {
                // The identifier first: addToTree reads it, and a filter registered without one would deliver
                // messages that resolve to no context at all.
                subscriptionIdFor(topicFilter);
                if (attached) {
                    addToTree(topicFilter);
                }
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
                    removeFromTree(topicFilter);
                }
                freeSubscriptionId(topicFilter);
            }
        }
        return this;
    }

    /**
     * Reconciles the topic tree from the currently subscribed filters to the given target, and answers which
     * filters are no longer wanted.
     * <p>
     * Does nothing to the tree while detached -- there is nothing registered to reconcile -- but still answers
     * the difference, since the caller frees identifiers either way.
     *
     * @return the filters that were subscribed and are not in the target
     */
    private @NotNull Set<String> reconcileTo(final @NotNull Set<String> target) {
        final Set<String> current = contextual ? topicFilterContexts.keySet() : topicFilters;
        final Set<String> gone = new LinkedHashSet<>();
        for (final String existing : current) {
            if (!target.contains(existing)) {
                gone.add(existing);
            }
        }
        if (attached) {
            gone.forEach(this::removeFromTree);
            for (final String wanted : target) {
                if (!current.contains(wanted)) {
                    // For a context subscriber the identifier must exist before the filter is registered, and
                    // the caller has not written the new set yet -- so allocate here rather than after.
                    if (contextual) {
                        subscriptionIdFor(wanted);
                    }
                    addToTree(wanted);
                }
            }
        }
        return gone;
    }

    /** The filters this subscriber is subscribed to, whichever kind it is. */
    private @NotNull Set<String> currentFilters() {
        return contextual ? topicFilterContexts.keySet() : topicFilters;
    }

    private void throwIfContextual(final @NotNull String verb) {
        if (contextual) {
            throw new IllegalStateException("InternalTopicFilterSubscriber '" + clientId
                    + "' was built with a context processor, so "
                    + verb
                    + " does not apply to it; use "
                    + verb
                    + "Context instead");
        }
    }

    private void throwIfNotContextual(final @NotNull String verb) {
        if (!contextual) {
            throw new IllegalStateException("InternalTopicFilterSubscriber '" + clientId
                    + "' was not built with a context processor, so "
                    + verb
                    + " does not apply to it; use "
                    + verb.replace("Context", "")
                    + " instead, or build it with a context processor");
        }
    }

    // endregion

    // region lifecycle verbs -- attach / consume / pause / detach / deallocate
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
    // deallocate() -- clear the client queue entry from persistence. Precondition: detached AND paused
    //               (throws otherwise) -- clearing the queue while still collecting or draining would
    //               race the SingleWriter.
    //
    // All return `this` (except deallocate) so they chain. All are synchronized with the topic-filter
    // verbs so state transitions and tree reconciliation never interleave.
    //
    // NO PROCESSOR IS EVER CALLED WITH THIS MONITOR HELD, and consume() below is where that is earned.
    // Submitting a poll can run the whole read-process-release cycle INLINE on the calling thread -- the
    // in-memory single writer executes a submitted task immediately when nothing else holds its bucket --
    // so a submit made inside a synchronized verb would run the component's processor inside this monitor.
    // Two things would follow, both bad: a processor that blocks waiting on a thread which wants this
    // monitor deadlocks, and a processor calling back into a verb would observe half-finished state.
    // Hence the split below -- state under the monitor, poll after it.
    //
    public synchronized @NotNull InternalTopicFilterSubscriber attach() {
        throwIfDeallocated();
        if (attached) {
            return this;
        }
        for (final String topicFilter : currentFilters()) {
            addToTree(topicFilter);
        }
        attached = true;
        return this;
    }

    public @NotNull InternalTopicFilterSubscriber consume() {
        if (armConsuming()) {
            // OUTSIDE the monitor, deliberately -- see the note above. By here `consuming` is already true,
            // so a processor that calls pause() during this drain is honoured rather than silently undone.
            submitPoll();
        }
        return this;
    }

    /**
     * The state half of {@link #consume()}: arms the callback and marks this subscriber as consuming.
     *
     * @return true if a drain is owed, i.e. this call is the one that started consuming
     */
    private synchronized boolean armConsuming() {
        throwIfDeallocated();
        if (consuming) {
            return false;
        }
        // Register the callback first so any message arriving from now on wakes the poller; the drain the
        // caller then submits picks up messages already in the queue (e.g. from a previous run, or collected
        // while attached-but-paused).
        clientQueuePersistence.addPublishAvailableCallback(id -> submitPoll(), clientId);
        // BEFORE the drain, not after: the drain may run inline, and a processor calling pause() while this
        // was still false would hit the "not consuming" guard and have its pause silently discarded.
        consuming = true;
        return true;
    }

    public synchronized @NotNull InternalTopicFilterSubscriber pause() {
        throwIfDeallocated();
        if (!consuming) {
            return this;
        }
        clientQueuePersistence.removePublishAvailableCallback(clientId);
        consuming = false;
        return this;
    }

    public synchronized @NotNull InternalTopicFilterSubscriber detach() {
        throwIfDeallocated();
        if (!attached) {
            return this;
        }
        for (final String topicFilter : currentFilters()) {
            removeFromTree(topicFilter);
        }
        attached = false;
        return this;
    }

    // deallocate() is the one terminal verb -- and the one exception to throwIfDeallocated(): calling it
    // on an already-dead subscriber is a no-op (idempotent), not an error.
    public synchronized void deallocate() {
        if (deallocated) {
            return; // idempotent -- already dead, nothing to clear
        }
        if (attached || consuming) {
            throw new IllegalStateException("InternalTopicFilterSubscriber '" + clientId
                    + "' must be detached and paused before deallocate() (attached="
                    + attached + ", consuming="
                    + consuming + ")");
        }
        // Remove the queue entry from persistence entirely. false == do not keep a tombstone. This
        // also DROPS any messages that were collected but not yet processed -- which is exactly why a
        // long-lived subscriber must eventually stop()/deallocate() rather than just detach(), or the
        // queue (and its memory) would linger.
        clientQueuePersistence.clear(clientId, false);
        // Free the clientId in the factory's registry -- symmetric with build(). The queue is now
        // destroyed, so the componentPrefix::instanceId identity becomes available for reuse.
        factory.deregister(this);
        deallocated = true;
    }

    // Guard for every verb except deallocate(): a deallocated subscriber is dead and cannot be
    // resurrected or operated on. Throws on any use-after-deallocate.
    private void throwIfDeallocated() {
        if (deallocated) {
            throw new IllegalStateException(
                    "InternalTopicFilterSubscriber '" + clientId + "' has been deallocated and cannot be used");
        }
    }

    // endregion

    // region start() / stop() -- convenience compositions of the verbs
    // =================================================================================================================
    // start() = attach(); consume(); -- order does not matter operationally; whichever runs second
    //           activates the flow.
    // stop()  = detach(); pause(); deallocate(); -- order DOES matter: detach first (stop the inflow),
    //           pause second (stop the processing), deallocate last (clear the now-quiet queue).
    //
    // They exist only to spare callers the common two-/three-call sequences; a caller that wants finer
    // control (e.g. attach without consuming, or pause without deallocating) calls the verbs directly.
    //
    // start() throws on a deallocated subscriber (you cannot resurrect a dead one -- that goes through
    // attach()). stop() is idempotent: it is the only composition you can safely call on a dead
    // subscriber, returning quietly (it just deallocates, which is itself a no-op when already dead).
    //
    // NOT synchronized, and that is the point: it composes attach() and consume(), each of which takes the
    // monitor for its own state work. Holding it across both would put the drain consume() submits back
    // inside the monitor -- exactly what consume() was split apart to avoid.
    public @NotNull InternalTopicFilterSubscriber start() {
        attach();
        consume();
        return this;
    }

    public synchronized void stop() {
        if (deallocated) {
            return; // already dead -- stop() is idempotent
        }
        detach();
        pause();
        deallocate();
    }

    // endregion

    // region ingress-exclusion -- identity and the accept decision
    // =================================================================================================================
    // clientId() -- this subscriber's reserved-prefix id, used by the factory as the registry key.
    //
    // isExcludedIngressClientId(senderClientId) -- the ingress-exclusion decision, called by the publish
    //   path at distribution time (BEFORE queueing) with the ingress/publishing client id (its
    //   `sender`). Returns true iff an excluded id is configured and equals senderClientId -- in which
    //   case the message must NOT be queued to us. With no excluded id (the common case) it always
    //   returns false. The excluded id is immutable after build(), so this is a lock-free read and safe
    //   to call from the publish path concurrently with the lifecycle verbs.
    //
    public @NotNull String clientId() {
        return clientId;
    }

    public boolean isExcludedIngressClientId(final @Nullable String senderClientId) {
        return excludedIngressClientId != null && excludedIngressClientId.equals(senderClientId);
    }

    /**
     * @return the most messages to hold for this subscriber, or null to use the broker-wide
     *         {@code maxQueuedMessages}. Read by the publish distributor when it queues a message.
     */
    public @Nullable Long queueLimit() {
        return queueLimit;
    }

    /**
     * @return what to drop when this subscriber's queue is full, or null to use the broker-wide strategy. Read
     *         by the queue persistence when it adds a message.
     */
    public @Nullable QueuedMessagesStrategy queueOverflow() {
        return queueOverflow;
    }

    // endregion

    // region internal wiring -- topic-tree add/remove and the SingleWriter poll pipeline
    // =================================================================================================================
    // addToTree() / removeFromTree() -- the two topic-tree operations, factored out so the verbs and
    //                     the reconciliation logic share one definition of "register a filter" and
    //                     "deregister a filter" under this clientId.
    //
    // The methods after them form the poll pipeline, which runs entirely on the SingleWriter thread:
    //
    // submitPoll() -- schedules pollUnlessBusy() on the SingleWriter queue. Called by every trigger
    //                     that means "a message may be available now": the publish-available callback,
    //                     consume(), and the error path.
    // submitPollAfterInFlight() -- the same, for the ONE trigger that also means "the message I was
    //                     working on is finished": the completion of a processor's future.
    // pollUnlessBusy() -- THE ONE PLACE THE CONDITION LIVES. Polls only when this subscriber is consuming
    //                     and holds no message. Otherwise gives up -- see below.
    // pollAfterInFlight() -- clears the in-flight state, then goes through pollUnlessBusy() like anything
    //                     else, so a subscriber paused meanwhile stops rather than reading one more.
    // poll() -- claims the in-flight state and reads one message. On an empty read it releases the
    //                     claim, since nothing will arrive to release it later.
    // processMessage() -- hands the message to the processor and chains the release off its completion.
    // release() -- acknowledges the message and triggers the next poll.
    // removeMessage() -- acknowledges the message to the queue persistence. QoS 0 messages are
    //                     not persisted and need no acknowledgement.
    //
    // TWO CONDITIONS, ONE PLACE, AND GIVING UP IS NEVER LOSING A TRIGGER. A trigger only says "a message may
    // be available"; whether to act is pollUnlessBusy()'s alone. It declines while a message is in flight,
    // because that message's completion polls again; and while paused, because resuming polls again. Both
    // paths therefore re-ask the question rather than stranding whatever arrived.
    //
    // WHY inFlight HAD TO BE ADDED. When a processor did its work before returning, one-message-at-a-time was
    // free: the whole chain ran as one task, and a trigger arriving meanwhile could only queue another task
    // behind it. A processor that answers a future returns at once, so the thread goes free while the work
    // continues elsewhere -- and a trigger then polls immediately, handing out a second message.
    //
    // WHY consuming HAD TO BE READ HERE. It was set and cleared by consume()/pause() and read by nothing on
    // this path, so pausing removed the wake-up callback but did not stop a poll already under way, nor the
    // completion of an outstanding message from reading the next one. Deallocation is covered by the same
    // clause rather than needing its own: deallocate() requires being paused, so a dead subscriber is one
    // whose consuming flag is already false -- which matters because deallocation FREES THE CLIENT ID, and a
    // replacement may already own the queue this one would otherwise read from.
    //
    // A PLAIN FIELD, NOT AN ATOMIC. Every one of these methods runs inside a task submitted for this
    // subscriber's clientId, and the SingleWriter runs the tasks of one bucket strictly one at a time --
    // so no two of them are ever in flight together, whichever thread did the submitting. That is also
    // why the completion submits a task rather than clearing the state itself: doing it inline would put
    // the write on the consumer's thread, outside the serialisation, for no gain.
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
    /// Registers one topic filter in the topic tree under this subscriber's client id.
    ///
    /// **On a CONTEXT subscriber the filter must already have a subscription identifier when this is called.**
    /// This method only reads one; it does not allocate. Every current caller satisfies that -- the
    /// constructor, `addTopicFilterContext` and the reconciliation in `withTopicFilterContext` each allocate
    /// before registering, and `attach()` only ever replays filters one of those three put there -- so there is
    /// no defect today. It is written down because nothing enforces it.
    ///
    /// **What goes wrong if a future caller forgets is silent.** A missing identifier reads as `null` from the
    /// map, and [Topic] accepts `null` as "no subscription identifier" rather than rejecting it. So the filter
    /// registers, messages match it, and they reach the consumer with an EMPTY set of matched contexts: a
    /// component that finds nothing to do with them drops them, and nothing anywhere logs or throws. That is
    /// precisely the failure the identifier mechanism exists to prevent, arriving through the one method that
    /// is supposed to deliver it.
    ///
    /// If a fifth registration path is ever added, either allocate first as the others do, or move the
    /// allocation in here -- [#subscriptionIdFor] is idempotent, so doing so would change no existing path.
    private void addToTree(final @NotNull String topicFilter) {
        topicTree.addTopic(
                clientId,
                // The subscription identifier is what makes a matched message say WHICH filter matched: the
                // broker echoes it back on every message that subscription matched, and several come back when
                // several filters match.
                //
                // NULL IS LEGITIMATE ONLY FOR A PLAIN SUBSCRIBER, which has no contexts to resolve and
                // delivers exactly as it did before identifiers existed. On a context subscriber a null here
                // is the silent failure described above -- see this method's documentation.
                new Topic(
                        topicFilter,
                        qos,
                        false,
                        true,
                        Topic.DEFAULT_RETAIN_HANDLING,
                        subscriptionIdByTopicFilter.get(topicFilter)),
                SubscriptionFlag.getDefaultFlags(false, true, false),
                null); // sharedName -- null: non-shared (see note above)
    }

    /**
     * Turns a routing processor into the async one the pipeline uses, by resolving each message's subscription
     * identifiers back to what the consumer asked to be told.
     * <p>
     * <b>A union, and over the CONTEXTS rather than the identifiers.</b> One context may sit behind two
     * filters -- one destination reachable by two patterns -- and both identifiers come back when both match.
     * Deduplicating at the end rather than the start is what stops the consumer doing the work twice for one
     * message, which for a device write means sending the same command twice.
     * <p>
     * The identifiers are in practice distinct, since a filter holds exactly one and the tree merges rather
     * than repeats. But that is a consequence of two other invariants rather than anything declared, and a set
     * costs nothing.
     */
    @SuppressWarnings("unchecked")
    private @NotNull AsyncProcessor asAsync(final @NotNull AsyncContextProcessor<?> contextProcessor) {
        final AsyncContextProcessor<TopicFilterContext> routing =
                (AsyncContextProcessor<TopicFilterContext>) contextProcessor;
        return message -> {
            final ImmutableIntArray matchedIdentifiers = message.getSubscriptionIdentifiers();
            final Set<TopicFilterContext> matched = new LinkedHashSet<>();
            if (matchedIdentifiers != null) {
                synchronized (this) {
                    for (int i = 0; i < matchedIdentifiers.length(); i++) {
                        // identifier -> filter -> contexts. A filter removed since this message was queued
                        // resolves to nothing, and contributes nothing to the set.
                        final String topicFilter = topicFilterBySubscriptionId.get(matchedIdentifiers.get(i));
                        if (topicFilter != null) {
                            matched.addAll(topicFilterContexts.getOrDefault(topicFilter, Set.of()));
                        }
                    }
                }
            }
            return routing.process(message, matched);
        };
    }

    /**
     * The identifier this subscriber uses for a filter, assigned on first subscription and stable thereafter.
     * <p>
     * <b>Stable is the whole point.</b> A filter re-registered after a {@code detach()}/{@code attach()} keeps
     * the identifier it had, because a message queued before the detach carries that identifier and is read
     * after it. Re-allocating would look correct -- the identifiers would stay internally consistent -- and
     * would fail only on that one path.
     */
    private int subscriptionIdFor(final @NotNull String topicFilter) {
        final Integer existing = subscriptionIdByTopicFilter.get(topicFilter);
        if (existing != null) {
            return existing;
        }
        final int allocated = allocateSubscriptionId();
        subscriptionIdByTopicFilter.put(topicFilter, allocated);
        topicFilterBySubscriptionId.put(allocated, topicFilter);
        return allocated;
    }

    /**
     * Frees the identifier a filter held, so the space does not grow with churn.
     * <p>
     * Called only when a filter is genuinely unsubscribed -- not on {@code detach()}, which keeps the filters
     * in order to replay them.
     */
    private void freeSubscriptionId(final @NotNull String topicFilter) {
        final Integer released = subscriptionIdByTopicFilter.remove(topicFilter);
        if (released != null) {
            topicFilterBySubscriptionId.remove(released);
        }
    }

    /**
     * An identifier no filter currently holds, walking forward from the last one handed out and wrapping.
     * <p>
     * <b>Identifiers are not reused while held, and are reused only after wrapping.</b> Walking forward means a
     * freed identifier is handed out again only once the counter has been all the way round -- after
     * {@link #MAX_SUBSCRIPTION_ID} allocations, each one a filter subscribed for the first time -- rather than
     * immediately. That matters because a message queued under a filter's identifier may be read after that
     * filter is gone: until the wrap it resolves to nothing and the message is dropped, which is right; after a
     * wrap it could resolve to whichever filter has since taken the number, which is not. No real deployment
     * subscribes that many filters on one subscriber while a single message sits unread, and the trade buys a
     * bounded space and a plain int counter.
     *
     * @throws IllegalStateException if this subscriber already holds {@link #MAX_TOPIC_FILTERS} filters
     */
    private int allocateSubscriptionId() {
        if (topicFilterBySubscriptionId.size() >= MAX_TOPIC_FILTERS) {
            throw new IllegalStateException("InternalTopicFilterSubscriber '" + clientId
                    + "' has too many topic filters: at most "
                    + MAX_TOPIC_FILTERS
                    + " may be subscribed at once");
        }
        do {
            // Wraps within [1, MAX_SUBSCRIPTION_ID]; never yields 0, which MQTT reserves. Terminates because
            // fewer than half the identifiers are taken -- see MAX_TOPIC_FILTERS.
            nextSubscriptionId = (nextSubscriptionId % MAX_SUBSCRIPTION_ID) + 1;
        } while (topicFilterBySubscriptionId.containsKey(nextSubscriptionId));
        return nextSubscriptionId;
    }

    private void removeFromTree(final @NotNull String topicFilter) {
        topicTree.removeSubscriber(clientId, topicFilter, null); // sharedName -- null: non-shared
    }

    /**
     * Says "a message may be available now". Used by every trigger except the completion of a processor's
     * future, which uses {@link #submitPollAfterInFlight()} because it means one thing more.
     */
    private void submitPoll() {
        singleWriterService.getQueuedMessagesQueue().submit(clientId, bucketIndex -> {
            pollUnlessBusy();
            return null;
        });
    }

    /**
     * Says "the message I was working on is finished, and another may be available now".
     * <p>
     * <b>A submitted task rather than clearing the state inline</b>, because this is called from the
     * completion of a processor's future -- on whichever thread completed it. Going through the queue puts
     * the write back inside the single-writer serialisation, which is what lets {@link #inFlight} be a
     * plain field.
     */
    private void submitPollAfterInFlight() {
        singleWriterService.getQueuedMessagesQueue().submit(clientId, bucketIndex -> {
            pollAfterInFlight();
            return null;
        });
    }

    /**
     * Polls, unless this subscriber should not be reading right now.
     * <p>
     * <b>Giving up is not dropping the trigger.</b> A message in flight will finish and its completion polls
     * again; a paused subscriber polls again when it resumes. So a trigger that arrives at the wrong moment is
     * redundant rather than lost.
     */
    private void pollUnlessBusy() {
        if (inFlight || !consuming) {
            return;
        }
        poll();
    }

    /**
     * Clears the in-flight state a completed message held, then polls for the next.
     * <p>
     * <b>The poll goes through the same condition as every other trigger</b>, so a subscriber paused while this
     * message was being processed stops here rather than reading one more.
     */
    private void pollAfterInFlight() {
        if (!inFlight) {
            // Nothing sets inFlight except poll(), and nothing clears it except this -- so reaching here
            // with it already clear means a message was released twice, which no path should allow.
            log.error("Internal subscriber '{}' completed a message it was not processing", clientId);
        }
        inFlight = false;
        pollUnlessBusy();
    }

    /**
     * Claims the in-flight state and reads one message.
     * <p>
     * <b>The claim happens here, before the read, and that placement is the whole point.</b> Reading submits
     * a task of its own, so this one ends before the message arrives; claiming afterwards would leave a gap
     * in which another trigger could poll and hand out a second message.
     * <p>
     * <b>An empty read therefore has to release the claim</b>: no message means no completion, and nothing
     * else would ever clear it -- the subscriber would stop for good.
     */
    private void poll() {
        inFlight = true;
        try {
            final ListenableFuture<ImmutableList<PUBLISH>> future =
                    clientQueuePersistence.readNew(clientId, false, POLL_PACKET_IDS, PUBLISH_POLL_BATCH_SIZE_BYTES);
            Futures.transform(
                    future,
                    publishes -> {
                        if (publishes == null || publishes.isEmpty()) {
                            inFlight = false;
                            return null;
                        }
                        processMessage(publishes.get(0));
                        return null;
                    },
                    MoreExecutors.directExecutor());
        } catch (final Throwable t) {
            log.error("Failed to poll internal subscriber '{}': {}", clientId, t.getMessage());
            inFlight = false;
            submitPoll();
        }
    }

    /**
     * Hands one message to the processor, and releases the queue once the processor is done with it.
     * <p>
     * <b>The release is chained off the processor's completion</b>, so the queue moves on when the consumer is
     * done rather than when it returns. What keeps a second message from being handed out meanwhile is
     * {@link #inFlight}, claimed in {@link #poll()} -- chaining alone does not, since a trigger arriving from
     * anywhere else would poll regardless.
     * <p>
     * Success and failure release alike: whether the work succeeded is the consumer's business, and both
     * outcomes mean "this one is no longer in flight". A message is dropped either way -- this is a subscriber,
     * not a retry mechanism.
     */
    private void processMessage(final @NotNull PUBLISH message) {
        final CompletableFuture<Void> completion;
        try {
            completion = processor.process(message);
        } catch (final Exception e) {
            // The contract is that a processor reports through its future rather than by throwing, and the
            // synchronous form is wrapped to guarantee it. An async processor that throws anyway would
            // otherwise leave nothing to attach the release to, and this queue would never move again.
            log.error(
                    "Processor for internal subscriber '{}' threw instead of returning a future; message will be "
                            + "dropped: {}",
                    clientId,
                    e.getMessage());
            release(message);
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
            release(message);
        });
    }

    /**
     * Acknowledges a message and reads the next.
     * <p>
     * <b>Wrapped so that neither half can be skipped.</b> A throw from the acknowledgement would otherwise
     * swallow the poll, and this subscriber would stop for good -- the same silent stall the whole design is
     * built to prevent, arriving through the one path that is supposed to prevent it.
     */
    private void release(final @NotNull PUBLISH message) {
        try {
            removeMessage(message);
        } catch (final Exception e) {
            log.error("Failed to acknowledge message for internal subscriber '{}': {}", clientId, e.getMessage());
        } finally {
            // The one trigger that also clears the in-flight state, since this message is now finished.
            submitPollAfterInFlight();
        }
    }

    private void removeMessage(final @NotNull PUBLISH message) {
        if (message.getQoS() != QoS.AT_MOST_ONCE) {
            FutureUtils.addExceptionLogger(clientQueuePersistence.removeShared(clientId, message.getUniqueId()));
        }
    }

    // endregion

}
