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
package com.hivemq.protocols.v2.southbound;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.mqtt.message.publish.PUBLISH;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * A multi-queue scripted stand-in for the durable client queue, mirroring the shared-subscription semantics the
 * southbound backlogs rely on: {@code readShared} leases the queue's first unleased message (marks it in-flight),
 * {@code removeShared} deletes by unique id, and the publish-available callback is registered per queue id.
 * Futures complete immediately on the calling thread.
 */
final class RecordingClientQueue extends UnsupportedClientQueuePersistence {

    private final @NotNull Map<String, Deque<PUBLISH>> queues = new HashMap<>();
    private final @NotNull Map<String, Set<String>> leased = new HashMap<>();
    private final @NotNull Map<String, PublishAvailableCallback> callbacks = new HashMap<>();
    final @NotNull List<String> removed = new ArrayList<>();

    void enqueue(final @NotNull String queueId, final @NotNull PUBLISH publish) {
        queues.computeIfAbsent(queueId, ignored -> new ArrayDeque<>()).addLast(publish);
    }

    void firePublishAvailable(final @NotNull String queueId) {
        final PublishAvailableCallback callback = callbacks.get(queueId);
        if (callback != null) {
            callback.onPublishAvailable(queueId);
        }
    }

    int pending(final @NotNull String queueId) {
        final Deque<PUBLISH> queue = queues.get(queueId);
        return queue == null ? 0 : queue.size();
    }

    @NotNull
    Set<String> callbackQueueIds() {
        return Set.copyOf(callbacks.keySet());
    }

    @Override
    public @NotNull ListenableFuture<ImmutableList<PUBLISH>> readShared(
            final @NotNull String sharedSubscription, final int messageLimit, final long byteLimit) {
        final Deque<PUBLISH> queue = queues.get(sharedSubscription);
        if (queue != null) {
            final Set<String> leasedIds = leased.computeIfAbsent(sharedSubscription, ignored -> new HashSet<>());
            for (final PUBLISH publish : queue) {
                if (leasedIds.add(publish.getUniqueId())) {
                    return Futures.immediateFuture(ImmutableList.of(publish));
                }
            }
        }
        return Futures.immediateFuture(ImmutableList.of());
    }

    @Override
    public @NotNull ListenableFuture<Void> removeShared(
            final @NotNull String sharedSubscription, final @NotNull String uniqueId) {
        final Deque<PUBLISH> queue = queues.get(sharedSubscription);
        if (queue != null) {
            queue.removeIf(publish -> publish.getUniqueId().equals(uniqueId));
        }
        final Set<String> leasedIds = leased.get(sharedSubscription);
        if (leasedIds != null) {
            leasedIds.remove(uniqueId);
        }
        removed.add(uniqueId);
        return Futures.immediateFuture(null);
    }

    @Override
    public void addPublishAvailableCallback(
            final @NotNull PublishAvailableCallback callback, final @NotNull String queueId) {
        callbacks.put(queueId, callback);
    }

    @Override
    public void removePublishAvailableCallback(final @NotNull String queueId) {
        callbacks.remove(queueId);
    }
}
