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
// create(...).
//
//   @Inject MyComponent(final InternalTopicFilterSubscriberFactory subscriberFactory) { ... }
//   ...
//   subscriber = subscriberFactory.create("tynebridge", bridgeId)
//                                 .withProcessor(message -> { ... })
//                                 .withTopicFilter("sensors/#")
//                                 .build();
//
@Singleton
public class InternalTopicFilterSubscriberFactory {

    private final @NotNull LocalTopicTree topicTree;
    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull SingleWriterService singleWriterService;

    // Registry of currently-attached subscribers, keyed by their reserved-prefix clientId. Populated
    // by attach(), cleared by detach(). The PublishDistributor consults it (getSubscriber) to ask a
    // subscriber's isExcludedIngressClientId(...) before queueing a publish — i.e. ingress-exclusion.
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

    // ── registry — called by the subscriber's attach()/detach(), read by the distributor ────────────

    void register(final @NotNull InternalTopicFilterSubscriber subscriber) {
        registry.put(subscriber.clientId(), subscriber);
    }

    void deregister(final @NotNull InternalTopicFilterSubscriber subscriber) {
        registry.remove(subscriber.clientId(), subscriber);
    }

    /**
     * The attached subscriber with this reserved-prefix clientId, or null if none is attached. Used by
     * the PublishDistributor to ask isExcludedIngressClientId(...) before queueing a publish.
     */
    public @Nullable InternalTopicFilterSubscriber getSubscriber(final @NotNull String clientId) {
        return registry.get(clientId);
    }

    // Begin building a subscriber for the given component identity.
    //
    // componentPrefix — the Edge component type, unique across Edge (e.g. "tynebridge"). Becomes the
    //                   middle segment of the reserved internal client ID "$INTERNAL::<prefix>::<id>".
    // instanceId      — unique within the componentPrefix namespace; typically the config ID of the
    //                   owning instance.
    //
    public @NotNull InternalTopicFilterSubscriber.Builder create(
            final @NotNull String componentPrefix, final @NotNull String instanceId) {
        return new InternalTopicFilterSubscriber.Builder(
                componentPrefix, instanceId, this, topicTree, clientQueuePersistence, singleWriterService);
    }
}

// endregion
