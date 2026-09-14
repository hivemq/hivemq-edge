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

import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriptionFlag;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// region InternalTopicFilterSubscriberWithoutQueue -- a topic-tree subscriber with no queue behind it
// =====================================================================================================================
// Lets an internal Edge component receive messages from the local topic tree without being an MQTT client
// AND WITHOUT A CLIENT QUEUE. A matched message is handed to the component's processor on the thread that
// distributed it, at the moment it is distributed.
//
// This is the sibling of InternalTopicFilterSubscriber, not a mode of it. The two share the topic-tree half --
// register filters under a reserved client id, keep them reconciled, remove them again -- and nothing else.
//
//   // a combiner keeping the latest value of one topic in an atomic reference:
//   final AtomicReference<byte[]> latest = new AtomicReference<>();
//   subscriber = factory.builderWithoutQueue("combiner", combinerId)
//                       .withProcessor(message -> latest.set(message.getPayload())) // distributing thread
//                       .withTopicFilter("sensors/temperature")
//                       .build();
//   subscriber.start();                            // attachConsume()
//   subscriber.addTopicFilter("sensors/humidity"); // runtime mutation
//   subscriber.stop();                             // pauseDetach(); deallocate();
//
// WHICH OF THE TWO TO USE is a real decision, and this one is the narrower choice. Take it when the processor's
// whole job is cheaper than enqueueing -- the motivating case is an atomic store, for which the queued variant
// pays two SingleWriter hops and a persistence round-trip. Everything the queue provides is given up with it:
// buffering, backpressure, a queue limit, survival across a restart, and sequential delivery.
//
// THE PROCESSOR MUST NOT BLOCK AND MUST BE THREAD-SAFE. It runs on whichever thread distributed the message,
// inside the distribution path, and two messages may be distributed concurrently -- nothing here serialises
// them. Blocking holds up distribution; not being thread-safe breaks under concurrent delivery. There is no
// future-returning form, because a future would mean a message outliving its delivery and there is nowhere to
// hold it.
//
// Lifecycle. Without a queue there is no buffer between collecting and processing, so the queued class's two
// orthogonal axes -- attached (collecting) and consuming (draining) -- collapse into one question, asked at the
// instant a message is distributed: is it delivered right now?
//
//   attachConsume() -- register the filters and start delivering.
//   pauseDetach()   -- stop delivering and remove the filters. Keeps the identity; can be started again.
//   deallocate()    -- release the client id. TERMINAL.
//   start()         -- attachConsume().
//   stop()          -- pauseDetach(); deallocate().
//
// Stopping the flow and releasing the identity are separate, and pauseDetach() does only the first.
//
// @see <a href="https://hivemq.github.io/hivemq-edge-lore/2-implementation/internal-topic-filter-subscriber/">Edge Lore
// -- Internal Topic Filter Subscriber</a>
//
public final class InternalTopicFilterSubscriberWithoutQueue {

    // region Instance Variables -- what a subscriber is made of
    // =================================================================================================================
    // The same aspect order as the queued variant, so the two read alike: dependencies, parent, identity,
    // processor, filters, delivery, lifecycle. That class's queue and ppf-loop groups have no counterpart here.

    // Dependencies - logger, topic tree
    private static final @NotNull Logger log = LoggerFactory.getLogger(InternalTopicFilterSubscriberWithoutQueue.class);

    // The one Edge singleton this subscriber operates against.
    private final @NotNull LocalTopicTree topicTree;

    //   the factory that created this subscriber, and is its registry
    private final @NotNull InternalTopicFilterSubscriberFactory factory;

    // Identity - creation, identity
    // clientId -- "$INTERNAL::<componentPrefix>::<instanceId>", the same reserved prefix the queued variant
    //            uses, and the identity under which filters are registered in the topic tree.
    //
    //            KEEP THE SHARED PREFIX. ConnectHandler rejects an external MQTT client connecting with a
    //            reserved id, and there it has only the string -- no subscriber to ask. A prefix of this
    //            class's own would have to be added there, and forgetting it would let a client take an
    //            internal identity. Callers that hold the object tell the kinds apart by which of the
    //            factory's two lookups answers.
    private final @NotNull String clientId;

    // Processors - what behaviour
    // One form only: no future-returning form (nowhere to hold a message while the consumer works) and no
    // context form (that is resolved at poll time, and there is no poll here).
    private final @NotNull Processor processor;

    // TopicFilters - which messages
    // topicFilters -- while not delivering this is a remembered set, replayed by the next attachConsume();
    //                while delivering the topic tree is kept reconciled with it. LinkedHashSet for stable
    //                order and no duplicates.
    private final @NotNull Set<String> topicFilters = new LinkedHashSet<>();

    // excludedIngressClientId -- ingress-exclusion (generalized No Local): if set, a message published by this
    //            client id is not delivered to us. Checked by the publish path at distribution time. Null
    //            means accept from everyone.
    private final @Nullable String excludedIngressClientId;

    // Delivery - how messages reach the processor
    // qos -- the QoS every filter is registered at. One value for the whole subscriber: the topic tree folds
    //            several matching filters into one entry keeping the last QoS in sorted order, so per-filter
    //            values would make the effective QoS depend on sort order.
    private final @NotNull QoS qos;

    // Lifecycle - start, stop
    // delivering -- whether a matched message reaches the processor right now. The one state variable of this
    //            class, where the queued variant needs four.
    //
    //            MUST STAY VOLATILE: written by the lifecycle verbs on the owning component's thread, read by
    //            deliver() on a distributing thread. Without it a pauseDetach() can go unseen indefinitely.
    //            It is not a lock -- a message already inside the processor is not recalled.
    private volatile boolean delivering = false;

    // deallocated -- true once deallocate() has released the identity. Terminal: every verb then throws,
    //            except deallocate() and stop(), which are idempotent no-ops. Volatile for the same reason.
    private volatile boolean deallocated = false;

    // endregion

    // region Constructor -- package-private, only the factory's builder constructs this
    // =================================================================================================================
    // The parameters run in the order the instance variables are declared in, and mean the same things --
    // dependency, parent, identity, processor, filters, delivery. Only these need more:
    //
    // factory -- the factory that built us, kept as the registry for the distributor's lookups.
    // componentPrefix -- identifies the Edge component type (e.g. "combiner"). Must be unique across all
    //                   components in Edge. Becomes the middle segment of the internal client id.
    // instanceId -- identifies this specific subscriber instance within the component. Must be unique within
    //                   the componentPrefix namespace. Typically derived from the configuration id of the
    //                   owning instance.
    // topicFilters -- may be empty, which is valid: a subscriber that subscribes to nothing until filters are
    //                   added later.
    //
    InternalTopicFilterSubscriberWithoutQueue(
            final @NotNull LocalTopicTree topicTree,
            final @NotNull InternalTopicFilterSubscriberFactory factory,
            final @NotNull String componentPrefix,
            final @NotNull String instanceId,
            final @NotNull Processor processor,
            final @NotNull Set<String> topicFilters,
            final @Nullable String excludedIngressClientId,
            final @NotNull QoS qos) {
        this.topicTree = topicTree;
        this.factory = factory;
        this.clientId = InternalTopicFilterSubscriber.INTERNAL_SUBSCRIBER_PREFIX + componentPrefix + "::" + instanceId;
        this.processor = processor;
        // Copied rather than kept by reference, so a builder reused after build() cannot reach into a live
        // subscriber.
        this.topicFilters.addAll(topicFilters);
        this.excludedIngressClientId = excludedIngressClientId;
        this.qos = qos;
    }

    // endregion

    // region Builder -- fluent build-time configuration, obtained from the factory
    // =================================================================================================================
    // The Processor is required; build() throws without it. Topic filters are optional, and an empty set is
    // valid -- a subscriber that subscribes to nothing until filters are added later.
    //
    // The three topic-filter verbs are the same as on the subscriber: withTopicFilter is ABSOLUTE (replace the
    // whole set, last call wins); addTopicFilter and removeTopicFilter are RELATIVE.
    //
    public static final class Builder {

        // dependencies
        private final @NotNull LocalTopicTree topicTree;

        // parent
        private final @NotNull InternalTopicFilterSubscriberFactory factory;

        // what is my identity
        private final @NotNull String componentPrefix;
        private final @NotNull String instanceId;

        // what am I doing with the messages -- required; build() throws without it
        private @Nullable Processor processor = null;

        // which messages
        private final @NotNull Set<String> topicFilters = new LinkedHashSet<>();
        private @Nullable String excludedIngressClientId = null; // optional; ingress-exclusion

        // delivery
        private @NotNull QoS qos = QoS.AT_LEAST_ONCE; // optional; the QoS every filter is registered at

        // Package-private -- only the factory creates builders (it supplies the injected singleton and itself,
        // as the registry the built subscriber registers with).
        Builder(
                final @NotNull LocalTopicTree topicTree,
                final @NotNull InternalTopicFilterSubscriberFactory factory,
                final @NotNull String componentPrefix,
                final @NotNull String instanceId) {
            this.topicTree = topicTree;
            this.factory = factory;
            this.componentPrefix = componentPrefix;
            this.instanceId = instanceId;
        }

        /// Required: the per-message work, done on the distributing thread.
        ///
        /// **Must not block, and must be thread-safe.** See the note at the top of the class: this runs inside
        /// message distribution, and concurrently with itself. A consumer that cannot meet both wants the
        /// queued [InternalTopicFilterSubscriber] instead.
        public @NotNull Builder withProcessor(final @NotNull Processor processor) {
            if (this.processor != null) {
                throw new IllegalStateException(
                        "InternalTopicFilterSubscriberWithoutQueue takes one processor, not two");
            }
            this.processor = processor;
            return this;
        }

        public @NotNull Builder withTopicFilter(final @NotNull String topicFilter) {
            return withTopicFilter(List.of(topicFilter));
        }

        public @NotNull Builder withTopicFilter(final @NotNull List<String> newFilters) {
            topicFilters.clear();
            topicFilters.addAll(newFilters);
            return this;
        }

        public @NotNull Builder addTopicFilter(final @NotNull String topicFilter) {
            return addTopicFilter(List.of(topicFilter));
        }

        public @NotNull Builder addTopicFilter(final @NotNull List<String> topicFiltersToAdd) {
            topicFilters.addAll(topicFiltersToAdd);
            return this;
        }

        public @NotNull Builder removeTopicFilter(final @NotNull String topicFilter) {
            return removeTopicFilter(List.of(topicFilter));
        }

        public @NotNull Builder removeTopicFilter(final @NotNull List<String> topicFiltersToRemove) {
            topicFilters.removeAll(topicFiltersToRemove);
            return this;
        }

        /// Optional: do not deliver a message published by this client id (generalized No Local).
        public @NotNull Builder withExcludedIngressClientId(final @NotNull String excludedIngressClientId) {
            this.excludedIngressClientId = excludedIngressClientId;
            return this;
        }

        /// Optional: the QoS every filter is registered at. Defaults to AT_LEAST_ONCE.
        public @NotNull Builder withQoS(final @NotNull QoS qos) {
            this.qos = qos;
            return this;
        }

        // Produce the live subscriber. It is constructed NOT DELIVERING and detached -- no message reaches the
        // processor until start() (or attachConsume()) is called. It is, however, already REGISTERED with the
        // factory: the subscriber owns its clientId from build() until deallocate(). register() throws if that
        // identity is already in use, so a duplicate componentPrefix::instanceId is rejected here rather than
        // silently colliding. The two kinds are kept in separate maps but share ONE client-id namespace, so a
        // queued subscriber cannot take an id this one holds either.
        public @NotNull InternalTopicFilterSubscriberWithoutQueue build() {
            if (processor == null) {
                throw new IllegalStateException("InternalTopicFilterSubscriberWithoutQueue requires a processor;"
                        + " call withProcessor(...) before build()");
            }
            final InternalTopicFilterSubscriberWithoutQueue subscriber = new InternalTopicFilterSubscriberWithoutQueue(
                    topicTree,
                    factory,
                    componentPrefix,
                    instanceId,
                    processor,
                    topicFilters,
                    excludedIngressClientId,
                    qos);
            factory.register(subscriber); // throws if clientId already in use -- deregistered by deallocate()
            return subscriber;
        }
    }

    // endregion

    // region Subscriber Identity -- what others ask this subscriber, and the declarations they read
    // =================================================================================================================
    // Both read fields that are immutable after build(), so both are lock-free and safe to call from the
    // publish path concurrently with the lifecycle verbs.

    // clientId() -- this subscriber's reserved-prefix id, and the factory's registry key.
    public @NotNull String clientId() {
        return clientId;
    }

    // isExcludedIngressClientId(senderClientId) -- the ingress-exclusion decision, asked by the publish path at
    //   distribution time with the publishing client's id. True means: do not deliver this message to us.
    public boolean isExcludedIngressClientId(final @Nullable String senderClientId) {
        return excludedIngressClientId != null && excludedIngressClientId.equals(senderClientId);
    }

    // endregion

    // region Subscriber Processors -- the per-message handler form a consumer may supply
    // =================================================================================================================
    // One form, where the queued variant offers four. Neither axis that gives it four survives without a queue:
    // a future has nowhere to land, because the message exists only for the duration of the call; and a context
    // has nothing to be computed from, because contexts are resolved at poll time and there is no poll. A
    // consumer wanting per-filter context builds one subscriber per filter.
    //
    @FunctionalInterface
    public interface Processor {

        /// Handle one message, on the thread that distributed it.
        ///
        /// **Must not block, and must be thread-safe.** Blocking holds up distribution; concurrency is possible
        /// because nothing here serialises deliveries.
        void process(final @NotNull PUBLISH message);
    }

    // endregion

    // region Subscriber TopicFilter Verbs -- the same family on builder and subscriber
    // =================================================================================================================
    // with... -- ABSOLUTE: replace the whole set. add... / remove... -- RELATIVE. Each is overloaded for one or a
    // list. When detached these only update the remembered set (replayed by the next attachConsume()); when
    // attached the topic tree is reconciled immediately -- only the genuine additions and removals are pushed,
    // so a with...(wholeNewSet) becomes exactly the diff against what is currently registered.
    //
    // Synchronized, and on the same monitor as the lifecycle verbs, so a filter change and a state transition
    // never interleave. The processor is never called with this monitor held -- see deliver().
    //
    public synchronized @NotNull InternalTopicFilterSubscriberWithoutQueue withTopicFilter(
            final @NotNull String topicFilter) {
        return withTopicFilter(List.of(topicFilter));
    }

    public synchronized @NotNull InternalTopicFilterSubscriberWithoutQueue withTopicFilter(
            final @NotNull List<String> newFilters) {
        throwIfDeallocated();
        reconcileTo(new LinkedHashSet<>(newFilters));
        topicFilters.clear();
        topicFilters.addAll(newFilters);
        return this;
    }

    public synchronized @NotNull InternalTopicFilterSubscriberWithoutQueue addTopicFilter(
            final @NotNull String topicFilter) {
        return addTopicFilter(List.of(topicFilter));
    }

    public synchronized @NotNull InternalTopicFilterSubscriberWithoutQueue addTopicFilter(
            final @NotNull List<String> topicFiltersToAdd) {
        throwIfDeallocated();
        for (final String topicFilter : topicFiltersToAdd) {
            if (topicFilters.add(topicFilter) && delivering) {
                subscribeFilterInTopicTree(topicFilter);
            }
        }
        return this;
    }

    public synchronized @NotNull InternalTopicFilterSubscriberWithoutQueue removeTopicFilter(
            final @NotNull String topicFilter) {
        return removeTopicFilter(List.of(topicFilter));
    }

    public synchronized @NotNull InternalTopicFilterSubscriberWithoutQueue removeTopicFilter(
            final @NotNull List<String> topicFiltersToRemove) {
        throwIfDeallocated();
        for (final String topicFilter : topicFiltersToRemove) {
            if (topicFilters.remove(topicFilter) && delivering) {
                unsubscribeFilterFromTopicTree(topicFilter);
            }
        }
        return this;
    }

    /// Reconciles the topic tree from the currently subscribed filters to the given target.
    ///
    /// Does nothing while not delivering -- there is nothing registered to reconcile, and the caller's new
    /// filter set is replayed in full by the next `attachConsume()`.
    private void reconcileTo(final @NotNull Set<String> target) {
        if (!delivering) {
            return;
        }
        final Set<String> gone = new LinkedHashSet<>();
        for (final String existing : topicFilters) {
            if (!target.contains(existing)) {
                gone.add(existing);
            }
        }
        gone.forEach(this::unsubscribeFilterFromTopicTree);
        for (final String wanted : target) {
            if (!topicFilters.contains(wanted)) {
                subscribeFilterInTopicTree(wanted);
            }
        }
    }

    private void throwIfDeallocated() {
        if (deallocated) {
            throw new IllegalStateException("InternalTopicFilterSubscriberWithoutQueue '" + clientId
                    + "' has been deallocated and cannot be used");
        }
    }

    // endregion

    // region Subscriber TopicTree Wiring -- every call this subscriber makes to the topic tree
    // =================================================================================================================
    // THE TOPIC TREE IS REACHED FROM HERE AND NOWHERE ELSE, so what this subscriber asks of the tree is
    // answered by reading one short region. It is also the only wiring region: there is no queue to wire to.
    //
    // The trailing `null` in both calls is the topic tree's `sharedName`, and it is null on purpose: this
    // subscriber is non-shared, so the tree deduplicates by client id and a message matching several of our
    // filters is delivered to us exactly once.
    /// Registers one topic filter in the topic tree under this subscriber's client id, so that matching messages
    /// start being distributed to it.
    private void subscribeFilterInTopicTree(final @NotNull String topicFilter) {
        topicTree.addTopic(
                clientId,
                new Topic(topicFilter, qos, false, true, Topic.DEFAULT_RETAIN_HANDLING, null),
                SubscriptionFlag.getDefaultFlags(false, true, false),
                null); // sharedName -- null: non-shared (see note above)
    }

    /// Removes one topic filter from the topic tree, so that matching messages stop being distributed to this
    /// subscriber.
    private void unsubscribeFilterFromTopicTree(final @NotNull String topicFilter) {
        topicTree.removeSubscriber(clientId, topicFilter, null); // sharedName -- null: non-shared
    }

    // endregion

    // region Subscriber Delivery -- one message, straight to the processor
    // =================================================================================================================
    // This region stands where the queued variant has its poll/process/finish loop and the state machine that
    // runs it. Here it is one method, called from outside, which calls the processor.
    //
    // NOTHING SEQUENCES DELIVERIES, and nothing can: the queued variant's ordering comes from the queue and the
    // single thread that drains it, and both are gone. What remains is whatever concurrency the distributor has.

    /// Delivers one message to the consumer's processor, on the calling thread. Called by the publish
    /// distributor in place of queueing.
    ///
    /// @return true if the processor was called and returned normally; false if this subscriber is not
    ///     delivering, is dead, or the processor threw -- which the distributor reports as a failed delivery.
    ///
    /// **MUST NOT TAKE THIS OBJECT'S MONITOR.** The processor is consumer code and may call back into a
    /// lifecycle verb, which would deadlock against a verb running on another thread. Reading the volatile
    /// flag needs no monitor, which is what makes that possible.
    ///
    /// The flag is read, not locked: a message already inside the processor when `pauseDetach()` flips it is
    /// not recalled. See [#pauseDetach].
    public boolean deliver(final @NotNull PUBLISH message) {
        if (deallocated || !delivering) {
            return false;
        }
        try {
            processor.process(message);
            return true;
        } catch (final Exception e) {
            // No queue to leave the message in and no retry to make: log it and report a failed delivery.
            log.error(
                    "Processor for internal subscriber '{}' threw; message will be dropped: {}",
                    clientId,
                    e.getMessage());
            return false;
        }
    }

    // endregion

    // region Subscriber Lifecycle Verbs -- attachConsume / pauseDetach / deallocate, start / stop
    // =================================================================================================================
    // attachConsume() -- register the filters and start delivering. Idempotent.
    // pauseDetach() -- stop delivering and remove the filters. The remembered filter set survives, so a later
    //               attachConsume() restores the same subscription. Keeps the identity. Idempotent.
    // deallocate() -- release the client id. TERMINAL: every verb throws afterwards, except deallocate() and
    //               stop(), which are idempotent no-ops.
    // start() -- attachConsume().
    // stop() -- pauseDetach(); deallocate(). Stop the flow, then release the identity.
    //
    // Two verbs where the queued variant has four, because there the queue sits between collecting and
    // processing and lets one happen without the other. Without it both are the same question.
    //
    // NO VERB BLOCKS. None of them waits for a message already inside the processor -- see pauseDetach().
    //
    public synchronized @NotNull InternalTopicFilterSubscriberWithoutQueue attachConsume() {
        throwIfDeallocated();
        if (delivering) {
            return this;
        }
        // SET THE FLAG BEFORE REGISTERING. The tree distributes to us the moment a filter is registered, on
        // another thread, and deliver() turns a message away while the flag is false -- so registering first
        // drops anything arriving in between. (No test covers this: the window needs a real concurrent
        // distributor, so swapping these two statements leaves the suite green.)
        delivering = true;
        for (final String topicFilter : topicFilters) {
            subscribeFilterInTopicTree(topicFilter);
        }
        return this;
    }

    /// Stops delivering and removes this subscriber's filters from the topic tree. The remembered filter set
    /// survives; a later [#attachConsume] restores the same subscription.
    ///
    /// **Does NOT release the identity** -- the subscriber keeps its client id and can be started again. Only
    /// [#deallocate] releases it.
    ///
    /// **Promises**, once it returns: the filters are out of the tree, so nothing further is distributed here;
    /// and a distribution already in flight is turned away when it reaches [#deliver]. The happens-before edge
    /// that makes the second statable is the volatile write below, which [#deliver]'s volatile read sees.
    ///
    /// **Does not promise** that a message already inside the processor has finished. It runs on another
    /// thread, and this call neither waits for it nor can recall it -- waiting would mean waiting for consumer
    /// code from inside a verb that same code may have called. A component needing to know the last message is
    /// done must learn that from its own processor.
    public synchronized @NotNull InternalTopicFilterSubscriberWithoutQueue pauseDetach() {
        throwIfDeallocated();
        if (!delivering) {
            return this;
        }
        // Clear the flag before deregistering, mirroring attachConsume().
        delivering = false;
        for (final String topicFilter : topicFilters) {
            unsubscribeFilterFromTopicTree(topicFilter);
        }
        return this;
    }

    /// Releases this subscriber's client id for reuse. Terminal; calling it on an already-dead subscriber is a
    /// no-op rather than an error.
    ///
    /// **Refuses a subscriber that is still delivering**, rather than stopping it first: that would leave
    /// filters in the topic tree under an id the next owner may take. Use [#stop], which orders the two.
    public synchronized void deallocate() {
        if (deallocated) {
            return;
        }
        if (delivering) {
            throw new IllegalStateException("InternalTopicFilterSubscriberWithoutQueue '" + clientId
                    + "' is still delivering; call pauseDetach() first, or stop() which does both");
        }
        factory.deregister(this);
        deallocated = true;
    }

    /// **Synchronized like the verbs it composes**, so that two threads calling it cannot interleave. The
    /// queued sibling needs no such thing: its compositions delegate to ONE verb, so there is no gap between
    /// two calls to slip into. Here there are two, and holding the monitor across them is safe because
    /// nothing in these verbs runs consumer code -- they touch only the topic tree and the factory.
    /// [#deliver] deliberately takes no monitor, so a processor calling a verb waits rather than deadlocks.
    public synchronized @NotNull InternalTopicFilterSubscriberWithoutQueue start() {
        return attachConsume();
    }

    /// The one composition that is safe on a dead subscriber; it returns quietly.
    ///
    /// **Synchronized, and that is what makes the promise above true.** Unsynchronized, two threads could both
    /// pass the deallocated check, one complete the whole shutdown, and the other then enter [#pauseDetach] --
    /// which refuses a dead subscriber and throws. That broke the documented idempotence, and it threw during
    /// shutdown cleanup, where an unexpected exception is most likely to abandon the rest of it half-done.
    /// Raised in review, 2026-09-14.
    public synchronized void stop() {
        if (deallocated) {
            return;
        }
        pauseDetach();
        deallocate();
    }

    // endregion

}

// endregion
