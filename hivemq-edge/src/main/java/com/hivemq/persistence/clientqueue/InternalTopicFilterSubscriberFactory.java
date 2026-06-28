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
import org.jetbrains.annotations.NotNull;

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

    @Inject
    InternalTopicFilterSubscriberFactory(
            final @NotNull LocalTopicTree topicTree,
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull SingleWriterService singleWriterService) {
        this.topicTree = topicTree;
        this.clientQueuePersistence = clientQueuePersistence;
        this.singleWriterService = singleWriterService;
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
                componentPrefix, instanceId, topicTree, clientQueuePersistence, singleWriterService);
    }
}

// endregion
