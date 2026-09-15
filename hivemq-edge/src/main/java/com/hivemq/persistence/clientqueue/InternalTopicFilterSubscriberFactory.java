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

import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.SingleWriterService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// region InternalTopicFilterSubscriberFactory — the injected entry point for building subscribers
// =====================================================================================================================
// This factory is the ONE thing a component injects in order to obtain an InternalTopicFilterSubscriber.
// It holds the three Edge singletons a subscriber operates against — LocalTopicTree, the client-queue
// persistence, and the SingleWriterService — so that callers never have to know about, hold, or thread
// those singletons through their own constructors. A component just injects this factory and calls
// builder(...).
//
//   @Inject MyComponent(final InternalTopicFilterSubscriberFactory subscriberFactory) { ... }
//   ...
//   // a combiner that keeps the latest value of one topic in an atomic reference:
//   final AtomicReference<byte[]> latest = new AtomicReference<>();
//   subscriber = subscriberFactory.builder("combiner", combinerId)
//                                 .withProcessor(message -> latest.set(message.getPayload()))
//                                 .withTopicFilter("sensors/temperature")
//                                 .build();
//
@Singleton
public class InternalTopicFilterSubscriberFactory {

    private final @NotNull LocalTopicTree topicTree;
    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull SingleWriterService singleWriterService;

    // Registry of currently-allocated subscribers, keyed by their reserved-prefix clientId. Populated
    // by build(), cleared by deallocate() — a subscriber owns its identity for its whole live span, not
    // just while attached. The PublishDistributor consults it (getSubscriber) to ask a subscriber's
    // isExcludedIngressClientId(...) before queueing a publish — i.e. ingress-exclusion. It is also the
    // uniqueness guard: register() rejects a duplicate componentPrefix::instanceId.
    // Concurrent because the publish path reads it while lifecycle verbs (any thread) mutate it.
    private final @NotNull ConcurrentHashMap<String, InternalTopicFilterSubscriber> registry =
            new ConcurrentHashMap<>();

    @Inject
    InternalTopicFilterSubscriberFactory(
            final @NotNull LocalTopicTree topicTree,
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull SingleWriterService singleWriterService) {
        this.topicTree = topicTree;
        this.clientQueuePersistence = clientQueuePersistence;
        this.singleWriterService = singleWriterService;
    }

    // ── registry — called by the subscriber's build()/deallocate(), read by the distributor ──────────

    void register(final @NotNull InternalTopicFilterSubscriber subscriber) {
        final InternalTopicFilterSubscriber existing = registry.putIfAbsent(subscriber.clientId(), subscriber);
        if (existing != null) {
            throw new IllegalStateException("InternalTopicFilterSubscriber '" + subscriber.clientId()
                    + "' is already in use; componentPrefix::instanceId must be unique within Edge"
                    + " (the existing subscriber must deallocate() before this identity can be reused)");
        }
    }

    void deregister(final @NotNull InternalTopicFilterSubscriber subscriber) {
        registry.remove(subscriber.clientId(), subscriber);
    }

    /**
     * The allocated subscriber with this reserved-prefix clientId, or null if none is allocated. Used by
     * the PublishDistributor to ask isExcludedIngressClientId(...) before queueing a publish.
     */
    public @Nullable InternalTopicFilterSubscriber getSubscriber(final @NotNull String clientId) {
        return registry.get(clientId);
    }

    // A builder for a subscriber with the given component identity. Named for what it returns (a Builder),
    // not for its effect — it has none until build() is called on the returned builder.
    //
    // componentPrefix — the Edge component type, unique across Edge (e.g. "combiner"). Becomes the middle
    //                   segment of the reserved internal client ID "$INTERNAL::<prefix>::<id>".
    // instanceId      — unique within the componentPrefix namespace; typically the config ID of the
    //                   owning instance.
    //
    public @NotNull InternalTopicFilterSubscriber.Builder builder(
            final @NotNull String componentPrefix, final @NotNull String instanceId) {
        return new InternalTopicFilterSubscriber.Builder(
                componentPrefix, instanceId, this, topicTree, clientQueuePersistence, singleWriterService);
    }
}

// endregion
