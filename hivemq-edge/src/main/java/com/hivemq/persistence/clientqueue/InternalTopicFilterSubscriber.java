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
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriptionFlag;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.util.FutureUtils;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// region InternalTopicFilterSubscriber — concrete topic-tree subscriber for internal Edge components
// =====================================================================================================================
// Lets an internal Edge component (a bridge, a combiner, a sampler, ...) receive messages from the
// local topic tree without being an MQTT client.
//
// This is a CONCRETE, FINAL class — there is nothing to subclass and nothing to override. You do not
// extend it; you obtain one from InternalTopicFilterSubscriberFactory, configure it through a fluent
// Builder, and drive it with explicit lifecycle verbs. The single piece of behaviour the component
// supplies — what to do with each message — is a Processor lambda passed to the builder.
//
//   subscriber = factory.create("tynebridge", bridgeId)
//                       .withProcessor(message -> { ... })   // runs on the SingleWriter thread
//                       .withTopicFilter("sensors/#")
//                       .build();
//   subscriber.start();                       // attach(); consume();
//   subscriber.addTopicFilter("actuators/#"); // runtime mutation, no teardown
//   subscriber.stop();                        // detach(); pause(); deallocate();
//
// Lifecycle. The lifecycle is decomposed into independent verbs. attach()/detach() control the
// SUBSCRIPTION (topics in the topic tree, i.e. whether messages are COLLECTED); consume()/pause()
// control the CALLBACK (whether collected messages are PROCESSED). They are orthogonal — a subscriber
// can be attached-but-paused (collecting into the queue without draining) or detached-but-consuming
// (callback armed, ready for when topics are attached later). start() and stop() are just convenience
// compositions of the verbs.
//
// Threading. The whole message pipeline runs on the SingleWriter thread keyed by clientId. The verbs
// themselves are safe to call from any thread; the queue/topic-tree operations they perform are the
// same ones the previous design performed, just split apart so each is independently callable.
//
// @see <a href="https://hivemq.github.io/hivemq-edge-lore/2-implementation/internal-topic-filter-subscriber/">Edge Lore
// — Internal Topic Filter Subscriber</a>
//
@SuppressWarnings({"FutureReturnValueIgnored", "CheckReturnValue", "UnusedReturnValue"})
public final class InternalTopicFilterSubscriber {

    private static final @NotNull Logger log = LoggerFactory.getLogger(InternalTopicFilterSubscriber.class);

    public static final @NotNull String INTERNAL_SUBSCRIBER_PREFIX = "$INTERNAL::";

    // SHARED_IN_FLIGHT_MARKER acts as a boolean inflight flag — not a real wire packet ID, since
    // messages never go to an MQTT client. removeShared() uses uniqueId, not the packet ID, so
    // the value here does not matter.
    private static final @NotNull ImmutableIntArray POLL_PACKET_IDS =
            ImmutableIntArray.of(ClientQueuePersistenceImpl.SHARED_IN_FLIGHT_MARKER);

    // clientId — built once as the reserved-prefix triple "$INTERNAL::<componentPrefix>::<instanceId>"
    //            (see the constructor for what each segment means). PublishDistributorImpl
    //            .isReservedClientId() recognises the "$INTERNAL::" prefix and rejects any external
    //            MQTT client that tries to connect with it, so this namespace cannot collide with real
    //            clients.
    //
    //            ONE id, used as the identity in TWO different subsystems — and deliberately so:
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
    // them injected), so callers never see or thread them. Final — set once at construction.
    private final @NotNull LocalTopicTree topicTree;
    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull SingleWriterService singleWriterService;

    // The per-message handler. Set once at construction (Builder requires it).
    private final @NotNull Processor processor;

    // ── mutable lifecycle state, all touched only via the verbs below ────────────────────────────
    // topicFilters — the CURRENT desired filter set. When detached this is just a remembered set
    //                (replayed by the next attach()); when attached the topic tree is kept reconciled
    //                with it. A LinkedHashSet so order is stable and duplicates are ignored.
    private final @NotNull Set<String> topicFilters = new LinkedHashSet<>();

    // attached — true iff the current topicFilters are registered in the topic tree (messages are
    //            being collected into the client queue).
    private boolean attached = false;

    // consuming — true iff the publish-available callback is registered (collected messages are being
    //             drained and handed to the processor).
    private boolean consuming = false;

    // endregion

    // region constructor — package-private, only the factory/builder construct this
    // =================================================================================================================
    // componentPrefix — identifies the Edge component type (e.g. "tynebridge", "combiner", "sampler").
    //                   Must be unique across all components in Edge. Becomes the middle segment of
    //                   the internal client ID.
    // instanceId      — identifies this specific subscriber instance within the component. Must be
    //                   unique within the componentPrefix namespace. Typically derived from the
    //                   configuration ID of the owning instance.
    // processor       — the per-message handler (runs on the SingleWriter thread).
    // initialFilters  — the topic filter set assembled in the builder (may be empty; that is valid).
    //
    InternalTopicFilterSubscriber(
            final @NotNull String componentPrefix,
            final @NotNull String instanceId,
            final @NotNull Processor processor,
            final @NotNull Set<String> initialFilters,
            final @NotNull LocalTopicTree topicTree,
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull SingleWriterService singleWriterService) {
        this.clientId = INTERNAL_SUBSCRIBER_PREFIX + componentPrefix + "::" + instanceId;
        this.processor = processor;
        this.topicFilters.addAll(initialFilters);
        this.topicTree = topicTree;
        this.clientQueuePersistence = clientQueuePersistence;
        this.singleWriterService = singleWriterService;
    }

    // endregion

    // region Processor — the per-message handler supplied by the user (a lambda)
    // =================================================================================================================
    // Called once per message, on the SingleWriter thread. Implementations must not block. Any
    // exception thrown is caught, logged, and the message is dropped.
    //
    // Deliberately NOT named MessageProcessor / MessageTransformer — those names belong to the
    // MessageFabric design, the long-term home. This is the interim ITFS-local type; its signature is
    // exactly the old abstract process(PUBLISH), so an old override body ports into a lambda verbatim.
    //
    @FunctionalInterface
    public interface Processor {
        void process(final @NotNull PUBLISH message);
    }

    // endregion

    // region Builder — fluent build-time configuration, obtained from the factory
    // =================================================================================================================
    // The Processor is required; build() throws without it. Topic filters are optional — the set
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
        private final @NotNull LocalTopicTree topicTree;
        private final @NotNull ClientQueuePersistence clientQueuePersistence;
        private final @NotNull SingleWriterService singleWriterService;

        private @Nullable Processor processor = null; // required; checked in build()
        private final @NotNull Set<String> topicFilters = new LinkedHashSet<>();

        // Package-private — only the factory creates builders (it supplies the injected singletons).
        Builder(
                final @NotNull String componentPrefix,
                final @NotNull String instanceId,
                final @NotNull LocalTopicTree topicTree,
                final @NotNull ClientQueuePersistence clientQueuePersistence,
                final @NotNull SingleWriterService singleWriterService) {
            this.componentPrefix = componentPrefix;
            this.instanceId = instanceId;
            this.topicTree = topicTree;
            this.clientQueuePersistence = clientQueuePersistence;
            this.singleWriterService = singleWriterService;
        }

        // Required: the per-message work.
        public @NotNull Builder withProcessor(final @NotNull Processor processor) {
            this.processor = processor;
            return this;
        }

        // Absolute: replace the whole set with this filter / these filters. Last call wins.
        public @NotNull Builder withTopicFilter(final @NotNull String topicFilter) {
            topicFilters.clear();
            topicFilters.add(topicFilter);
            return this;
        }

        public @NotNull Builder withTopicFilter(final @NotNull List<String> topicFilters) {
            this.topicFilters.clear();
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

        // Relative: remove from the current set.
        public @NotNull Builder removeTopicFilter(final @NotNull String topicFilter) {
            topicFilters.remove(topicFilter);
            return this;
        }

        public @NotNull Builder removeTopicFilter(final @NotNull List<String> topicFilters) {
            this.topicFilters.removeAll(topicFilters);
            return this;
        }

        // Produce the live subscriber. The subscriber is constructed in the IDLE state — detached and
        // paused — i.e. nothing happens until start() (or attach()/consume()) is called.
        public @NotNull InternalTopicFilterSubscriber build() {
            if (processor == null) {
                throw new IllegalStateException(
                        "InternalTopicFilterSubscriber requires a processor; call withProcessor(...) before build()");
            }
            return new InternalTopicFilterSubscriber(
                    componentPrefix,
                    instanceId,
                    processor,
                    topicFilters,
                    topicTree,
                    clientQueuePersistence,
                    singleWriterService);
        }
    }

    // endregion

    // region topic-filter verbs — the same three on builder and subscriber
    // =================================================================================================================
    // withTopicFilter — ABSOLUTE: replace the whole set. addTopicFilter / removeTopicFilter — RELATIVE.
    // Each is overloaded for one filter or a list. When DETACHED these only update the remembered set
    // (replayed by the next attach()); when ATTACHED the topic tree is reconciled immediately — only
    // the genuine additions/removals are pushed to the tree, so a withTopicFilter(wholeNewSet) becomes
    // exactly the diff against what is currently registered.
    //
    public synchronized @NotNull InternalTopicFilterSubscriber withTopicFilter(final @NotNull String topicFilter) {
        return withTopicFilter(List.of(topicFilter));
    }

    public synchronized @NotNull InternalTopicFilterSubscriber withTopicFilter(final @NotNull List<String> newFilters) {
        final Set<String> target = new LinkedHashSet<>(newFilters);
        if (attached) {
            // Reconcile the tree with the new target: remove what is no longer wanted, add what is new.
            for (final String existing : new ArrayList<>(topicFilters)) {
                if (!target.contains(existing)) {
                    removeFromTree(existing);
                }
            }
            for (final String wanted : target) {
                if (!topicFilters.contains(wanted)) {
                    addToTree(wanted);
                }
            }
        }
        topicFilters.clear();
        topicFilters.addAll(target);
        return this;
    }

    public synchronized @NotNull InternalTopicFilterSubscriber addTopicFilter(final @NotNull String topicFilter) {
        return addTopicFilter(List.of(topicFilter));
    }

    public synchronized @NotNull InternalTopicFilterSubscriber addTopicFilter(
            final @NotNull List<String> topicFiltersToAdd) {
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
        for (final String topicFilter : topicFiltersToRemove) {
            if (topicFilters.remove(topicFilter) && attached) {
                removeFromTree(topicFilter);
            }
        }
        return this;
    }

    // endregion

    // region lifecycle verbs — attach / consume / pause / detach / deallocate
    // =================================================================================================================
    // attach()    — register the current topic filters in the topic tree. Messages now COLLECTED into
    //               the client queue. Idempotent.
    // consume()   — register the publish-available callback and drain anything already queued. Messages
    //               now PROCESSED (if attached). Legal while detached — the callback is simply armed for
    //               when topics attach later. Idempotent.
    // pause()     — deregister the callback. Messages keep being COLLECTED (if attached) but are no
    //               longer PROCESSED. Idempotent.
    // detach()    — remove the current topic filters from the topic tree. Messages no longer COLLECTED.
    //               The remembered filter set survives, so a later attach() restores the same
    //               subscription. Idempotent.
    // deallocate()— clear the client queue entry from persistence. Precondition: detached AND paused
    //               (throws otherwise) — clearing the queue while still collecting or draining would
    //               race the SingleWriter.
    //
    // All return `this` (except deallocate) so they chain. All are synchronized with the topic-filter
    // verbs so state transitions and tree reconciliation never interleave.
    //
    public synchronized @NotNull InternalTopicFilterSubscriber attach() {
        if (attached) {
            return this;
        }
        for (final String topicFilter : topicFilters) {
            addToTree(topicFilter);
        }
        attached = true;
        return this;
    }

    public synchronized @NotNull InternalTopicFilterSubscriber consume() {
        if (consuming) {
            return this;
        }
        // Register the callback first so any message arriving from now on wakes the poller; the
        // explicit submitPoll() then drains messages already in the queue (e.g. from a previous run,
        // or collected while attached-but-paused).
        clientQueuePersistence.addPublishAvailableCallback(id -> submitPoll(), clientId);
        submitPoll();
        consuming = true;
        return this;
    }

    public synchronized @NotNull InternalTopicFilterSubscriber pause() {
        if (!consuming) {
            return this;
        }
        clientQueuePersistence.removePublishAvailableCallback(clientId);
        consuming = false;
        return this;
    }

    public synchronized @NotNull InternalTopicFilterSubscriber detach() {
        if (!attached) {
            return this;
        }
        for (final String topicFilter : topicFilters) {
            removeFromTree(topicFilter);
        }
        attached = false;
        return this;
    }

    public synchronized void deallocate() {
        if (attached || consuming) {
            throw new IllegalStateException("InternalTopicFilterSubscriber '" + clientId
                    + "' must be detached and paused before deallocate() (attached="
                    + attached + ", consuming="
                    + consuming + ")");
        }
        // Remove the queue entry from persistence entirely. false == do not keep a tombstone.
        clientQueuePersistence.clear(clientId, false);
    }

    // endregion

    // region start() / stop() — convenience compositions of the verbs
    // =================================================================================================================
    // start() = attach(); consume();  — order does not matter operationally; whichever runs second
    //           activates the flow.
    // stop()  = detach(); pause(); deallocate();  — order DOES matter: detach first (stop the inflow),
    //           pause second (stop the processing), deallocate last (clear the now-quiet queue).
    //
    // They exist only to spare callers the common two-/three-call sequences; a caller that wants finer
    // control (e.g. attach without consuming, or pause without deallocating) calls the verbs directly.
    //
    public synchronized @NotNull InternalTopicFilterSubscriber start() {
        attach();
        consume();
        return this;
    }

    public synchronized void stop() {
        detach();
        pause();
        deallocate();
    }

    // endregion

    // region internal wiring — topic-tree add/remove and the SingleWriter poll pipeline
    // =================================================================================================================
    // addToTree() / removeFromTree() — the two topic-tree operations, factored out so the verbs and
    //                     the reconciliation logic share one definition of "register a filter" and
    //                     "deregister a filter" under this clientId.
    //
    // The four methods after them form a simple pipeline that runs entirely on the SingleWriter thread:
    //
    // submitPoll()      — schedules pollAndForward() on the SingleWriter queue. Called from the
    //                     publish-available callback (new message arrived), from consume() (drain
    //                     pre-existing messages), and at the end of each processed message (to
    //                     continue draining).
    // pollAndForward()  — reads one message from the client queue. On success hands it to
    //                     processPublish(); on failure logs and reschedules via submitPoll().
    // processPublish()  — calls the processor (the user-supplied handler), then removeMessage(), then
    //                     submitPoll() to pick up the next message. Errors are caught, logged, and
    //                     the message is dropped — processing continues regardless.
    // removeMessage()   — acknowledges the message to the queue persistence. QoS 0 messages are
    //                     not persisted and need no acknowledgement.
    //
    // The trailing `null` in both calls is the topic tree's `sharedName` — and it is null ON PURPOSE.
    // The topic tree distinguishes shared from non-shared subscriptions: a non-null sharedName makes
    // the subscription part of a shared group (load-balanced across the group's members). Some other
    // internal subscribers ARE shared and pass one (the bridge forwarder's sharedName is
    // "forwarder#{id}"; the combiner's is its uuid, with the subscriber id differing by a trailing
    // "#"). An InternalTopicFilterSubscriber is deliberately NON-shared: that is the whole point of
    // EDG-504 — a non-shared internal subscriber so the topic tree deduplicates by client id and a
    // message matching several of our filters is delivered to our queue exactly once. So sharedName
    // stays null, and our `clientId` is the sole identity (no separate shared-group name).
    private void addToTree(final @NotNull String topicFilter) {
        topicTree.addTopic(
                clientId,
                new Topic(topicFilter, QoS.AT_LEAST_ONCE, false, true),
                SubscriptionFlag.getDefaultFlags(false, true, false),
                null); // sharedName — null: non-shared (see note above)
    }

    private void removeFromTree(final @NotNull String topicFilter) {
        topicTree.removeSubscriber(clientId, topicFilter, null); // sharedName — null: non-shared
    }

    private void submitPoll() {
        singleWriterService.getQueuedMessagesQueue().submit(clientId, bucketIndex -> {
            pollAndForward();
            return null;
        });
    }

    private void pollAndForward() {
        try {
            final ListenableFuture<ImmutableList<PUBLISH>> future =
                    clientQueuePersistence.readNew(clientId, false, POLL_PACKET_IDS, PUBLISH_POLL_BATCH_SIZE_BYTES);
            Futures.transform(
                    future,
                    publishes -> {
                        if (publishes == null || publishes.isEmpty()) {
                            return null;
                        }
                        processPublish(publishes.get(0));
                        return null;
                    },
                    MoreExecutors.directExecutor());
        } catch (final Throwable t) {
            log.error("Failed to poll internal subscriber '{}': {}", clientId, t.getMessage());
            submitPoll();
        }
    }

    private void processPublish(final @NotNull PUBLISH message) {
        try {
            processor.process(message);
            removeMessage(message);
            submitPoll();
        } catch (final Exception e) {
            log.error(
                    "Failed to process message for internal subscriber '{}', message will be dropped: {}",
                    clientId,
                    e.getMessage());
            removeMessage(message);
            submitPoll();
        }
    }

    private void removeMessage(final @NotNull PUBLISH message) {
        if (message.getQoS() != QoS.AT_MOST_ONCE) {
            FutureUtils.addExceptionLogger(clientQueuePersistence.removeShared(clientId, message.getUniqueId()));
        }
    }

    // endregion

}
