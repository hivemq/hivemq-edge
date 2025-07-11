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
package com.hivemq.persistence.local.memory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.hivemq.annotations.ExecuteInSingleWriter;
import com.hivemq.configuration.service.InternalConfigurationService;
import com.hivemq.configuration.service.InternalConfigurations;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.hivemq.extensions.iteration.BucketChunkResult;
import com.hivemq.metrics.HiveMQMetrics;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.persistence.IterablePersistenceEntry;
import com.hivemq.persistence.local.ClientSessionSubscriptionLocalPersistence;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import com.hivemq.util.MemoryEstimator;
import com.hivemq.util.ThreadPreConditions;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.hivemq.util.ThreadPreConditions.SINGLE_WRITER_THREAD_PREFIX;

/**
 * A memory based ClientSessionSubscriptionLocalPersistence.
 *
 * @author Florian Limpöck
 */
@Singleton
public class ClientSessionSubscriptionMemoryLocalPersistence implements ClientSessionSubscriptionLocalPersistence {


    private final @NotNull Map<String, IterablePersistenceEntry<ImmutableSet<Topic>>> @NotNull [] buckets;
    private final int bucketCount;

    @VisibleForTesting
    final @NotNull AtomicLong currentMemorySize = new AtomicLong();

    @Inject
    ClientSessionSubscriptionMemoryLocalPersistence(
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull InternalConfigurationService internalConfigurationService) {

        bucketCount = internalConfigurationService.getInteger(InternalConfigurations.PERSISTENCE_BUCKET_COUNT);

        //noinspection unchecked
        buckets = new HashMap[bucketCount];
        for (int i = 0; i < bucketCount; i++) {
            buckets[i] = new HashMap<>();
        }

        metricRegistry.remove(HiveMQMetrics.CLIENT_SESSION_SUBSCRIPTIONS_MEMORY_PERSISTENCE_TOTAL_SIZE.name());
        metricRegistry.register(HiveMQMetrics.CLIENT_SESSION_SUBSCRIPTIONS_MEMORY_PERSISTENCE_TOTAL_SIZE.name(),
                (Gauge<Long>) currentMemorySize::get);

    }

    @Override
    @ExecuteInSingleWriter
    public void addSubscription(
            final @NotNull String client, final @NotNull Topic topic, final long timestamp, final int bucketIndex) {
        addSubscriptions(client, ImmutableSet.of(topic), timestamp, bucketIndex);
    }

    @Override
    @ExecuteInSingleWriter
    public void addSubscriptions(
            final @NotNull String client,
            final @NotNull ImmutableSet<Topic> topics,
            final long timestamp,
            final int bucketIndex) {
        checkNotNull(client, "Client id must not be null");
        checkNotNull(topics, "Topics must not be null");
        checkState(timestamp > 0, "Timestamp must not be 0");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        buckets[bucketIndex].compute(client, (ignore, oldEntry) -> {
            if (oldEntry == null) {
                final IterablePersistenceEntry<ImmutableSet<Topic>> newEntry =
                        new IterablePersistenceEntry<>(topics, timestamp);
                currentMemorySize.addAndGet(newEntry.getEstimatedSize());
                currentMemorySize.addAndGet(MemoryEstimator.stringSize(client));
                return newEntry;
            }
            currentMemorySize.addAndGet(-oldEntry.getEstimatedSize());
            final IterablePersistenceEntry<ImmutableSet<Topic>> mergedEntry =
                    new IterablePersistenceEntry<>(Sets.union(topics, oldEntry.getObject()).immutableCopy(), timestamp);
            currentMemorySize.addAndGet(mergedEntry.getEstimatedSize());
            return mergedEntry;
        });
    }

    @Override
    @ExecuteInSingleWriter
    public void remove(
            final @NotNull String client, final @NotNull String topic, final long timestamp, final int bucketIndex) {
        checkNotNull(client, "Clientid must not be null");
        checkNotNull(topic, "Topic must not be null");
        checkState(timestamp > 0, "Timestamp must not be 0");

        removeSubscriptions(client, ImmutableSet.of(topic), timestamp, bucketIndex);
    }

    @Override
    @ExecuteInSingleWriter
    public void removeSubscriptions(
            final @NotNull String client,
            final @NotNull ImmutableSet<String> topics,
            final long timestamp,
            final int bucketIndex) {
        checkNotNull(client, "Client id must not be null");
        checkNotNull(topics, "Topics must not be null");
        checkState(timestamp > 0, "Timestamp must not be 0");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        buckets[bucketIndex].computeIfPresent(client, (ignore, entry) -> {
            final ImmutableSet.Builder<Topic> remainingTopicsBuilder = new ImmutableSet.Builder<>();
            boolean remaining = false;
            for (final Topic topic : entry.getObject()) {
                if (topics.contains(topic.getTopic())) {
                    currentMemorySize.addAndGet(-(topic.getEstimatedSize() + MemoryEstimator.OBJECT_REF_SIZE));
                    continue;
                }
                remaining = true;
                remainingTopicsBuilder.add(topic);
            }
            if (!remaining) {
                currentMemorySize.addAndGet(-IterablePersistenceEntry.getFixedSize());
                currentMemorySize.addAndGet(-MemoryEstimator.stringSize(client));
                return null;
            }
            return new IterablePersistenceEntry<>(remainingTopicsBuilder.build(), timestamp);
        });
    }

    @Override
    @ExecuteInSingleWriter
    public void removeAll(final @NotNull String client, final long timestamp, final int bucketIndex) {
        checkNotNull(client, "Clientid must not be null");
        checkState(timestamp > 0, "Timestamp must not be 0");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final IterablePersistenceEntry<ImmutableSet<Topic>> remove = buckets[bucketIndex].remove(client);
        if (remove == null) {
            return;
        }
        currentMemorySize.addAndGet(-remove.getEstimatedSize());
        currentMemorySize.addAndGet(-MemoryEstimator.stringSize(client));
    }

    @Override
    @NotNull
    public ImmutableSet<Topic> getSubscriptions(final @NotNull String client) {
        checkNotNull(client, "Clientid must not be null");

        final IterablePersistenceEntry<ImmutableSet<Topic>> entry =
                buckets[BucketUtils.getBucket(client, bucketCount)].get(client);
        if (entry == null) {
            return ImmutableSet.of();
        } else {
            return entry.getObject();
        }

    }

    @Override
    @NotNull
    public BucketChunkResult<Map<String, ImmutableSet<Topic>>> getAllSubscribersChunk(
            final int bucketIndex, final @Nullable String lastClientIdIgnored, final int maxResultsIgnored) {

        //as all subscriptions are already in memory, we can ignore any pagination here and return the whole bucket.
        final Map<String, ImmutableSet<Topic>> result = buckets[bucketIndex].entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getObject()));

        return new BucketChunkResult<>(result, true, lastClientIdIgnored, bucketIndex);

    }

    @Override
    public void cleanUp(final int bucket) {
        //noop because we have no duplicates in memory
    }

    @Override
    public void closeDB(final int bucketIndex) {
        buckets[bucketIndex].clear();
        currentMemorySize.set(0);
    }
}
