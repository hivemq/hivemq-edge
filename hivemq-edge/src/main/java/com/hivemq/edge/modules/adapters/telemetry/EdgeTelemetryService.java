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
package com.hivemq.edge.modules.adapters.telemetry;

import com.hivemq.combining.runtime.QueueConsumer;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriptionFlag;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class EdgeTelemetryService {

    private static final @NotNull Logger log = LoggerFactory.getLogger(EdgeTelemetryService.class);

    private static volatile @Nullable EdgeTelemetryService INSTANCE;

    private final @NotNull LocalTopicTree localTopicTree;
    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull SingleWriterService singleWriterService;

    private final @NotNull Map<String, TagEntry> entries = new ConcurrentHashMap<>();

    @Inject
    public EdgeTelemetryService(
            final @NotNull LocalTopicTree localTopicTree,
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull SingleWriterService singleWriterService) {
        this.localTopicTree = localTopicTree;
        this.clientQueuePersistence = clientQueuePersistence;
        this.singleWriterService = singleWriterService;
        INSTANCE = this;
    }

    public static @Nullable EdgeTelemetryService getInstance() {
        return INSTANCE;
    }

    public synchronized void subscribe(final @NotNull String tagKey, final @NotNull String topicFilter) {
        if (entries.containsKey(tagKey)) {
            unsubscribe(tagKey);
        }
        final String id = UUID.randomUUID().toString();
        final String subscriber = "edge-telemetry-" + id + "#";
        final String sharedName = "edge-telemetry-" + id;
        final AtomicLong counter = new AtomicLong(0);

        final QueueConsumer consumer =
                new QueueConsumer(clientQueuePersistence, sharedName + "/" + topicFilter, singleWriterService) {
                    @Override
                    public void process(final @NotNull PUBLISH publish) {
                        counter.incrementAndGet();
                    }
                };

        localTopicTree.addTopic(
                subscriber,
                new Topic(topicFilter, QoS.EXACTLY_ONCE, false, true),
                SubscriptionFlag.getDefaultFlags(true, true, false),
                sharedName);
        consumer.start();

        entries.put(tagKey, new TagEntry(subscriber, topicFilter, sharedName, consumer, counter));
        log.debug("Edge telemetry subscribed to '{}' (tagKey='{}')", topicFilter, tagKey);
    }

    public long getAndResetCount(final @NotNull String tagKey) {
        final TagEntry entry = entries.get(tagKey);
        if (entry == null) {
            return 0L;
        }
        return entry.counter().getAndSet(0L);
    }

    public synchronized void unsubscribe(final @NotNull String tagKey) {
        final TagEntry entry = entries.remove(tagKey);
        if (entry == null) {
            return;
        }
        try {
            entry.consumer().close();
            localTopicTree.removeSubscriber(entry.subscriber(), entry.topicFilter(), entry.sharedName());
            log.debug("Edge telemetry unsubscribed from '{}' (tagKey='{}')", entry.topicFilter(), tagKey);
        } catch (final Exception e) {
            log.warn("Error unsubscribing edge telemetry tag '{}'", tagKey, e);
        }
    }

    private record TagEntry(
            @NotNull String subscriber,
            @NotNull String topicFilter,
            @NotNull String sharedName,
            @NotNull QueueConsumer consumer,
            @NotNull AtomicLong counter) {}
}
