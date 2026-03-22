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
package com.hivemq.topicbuffer;

import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.events.model.Payload;
import com.hivemq.adapter.sdk.api.events.model.TypeIdentifier;
import com.hivemq.api.model.core.PayloadImpl;
import com.hivemq.combining.runtime.QueueConsumer;
import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.reader.TopicBufferExtractor;
import com.hivemq.edge.model.TypeIdentifierImpl;
import com.hivemq.edge.modules.api.events.model.EventImpl;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriptionFlag;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.topicbuffer.model.BufferedMqttMessage;
import com.hivemq.topicbuffer.model.TopicBufferSubscription;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TopicBufferService {

    private static final Logger log = LoggerFactory.getLogger(TopicBufferService.class);

    private final @NotNull LocalTopicTree localTopicTree;
    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull SingleWriterService singleWriterService;
    private final @NotNull TopicBufferExtractor extractor;

    private final @NotNull Map<String, LinkedList<BufferedMqttMessage>> buffers = new ConcurrentHashMap<>();
    private final @NotNull Map<String, Integer> capacities = new ConcurrentHashMap<>();
    private final @NotNull List<ActiveSubscription> activeSubscriptions = new ArrayList<>();

    @Inject
    public TopicBufferService(
            final @NotNull LocalTopicTree localTopicTree,
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull SingleWriterService singleWriterService,
            final @NotNull TopicBufferExtractor extractor,
            final @NotNull ShutdownHooks shutdownHooks) {
        this.localTopicTree = localTopicTree;
        this.clientQueuePersistence = clientQueuePersistence;
        this.singleWriterService = singleWriterService;
        this.extractor = extractor;
        shutdownHooks.add(new HiveMQShutdownHook() {
            @Override
            public @NotNull String name() {
                return "Topic Buffer Service Shutdown";
            }

            @Override
            public void run() {
                stopAllSubscriptions();
            }
        });
    }

    public void start() {
        log.info("Starting topic buffer service");
        extractor.registerConsumer(this::refresh);
    }

    private synchronized void refresh(final @NotNull List<TopicBufferSubscription> subscriptions) {
        log.info("Refreshing topic buffer subscriptions (count: {})", subscriptions.size());
        stopAllSubscriptions();
        buffers.clear();
        capacities.clear();
        for (final TopicBufferSubscription sub : subscriptions) {
            buffers.put(sub.topicFilter(), new LinkedList<>());
            capacities.put(sub.topicFilter(), sub.maxMessages());
            subscribe(sub);
        }
    }

    private void subscribe(final @NotNull TopicBufferSubscription sub) {
        final String id = UUID.randomUUID().toString();
        final String subscriber = "topic-buffer-" + id + "#";
        final String sharedName = "topic-buffer-" + id;
        final String topicFilter = sub.topicFilter();

        final QueueConsumer consumer =
                new QueueConsumer(clientQueuePersistence, sharedName + "/" + topicFilter, singleWriterService) {
                    @Override
                    public void process(final @NotNull PUBLISH publish) {
                        addToBuffer(topicFilter, publish);
                    }
                };

        localTopicTree.addTopic(
                subscriber,
                new Topic(topicFilter, QoS.EXACTLY_ONCE, false, true),
                SubscriptionFlag.getDefaultFlags(true, true, false),
                sharedName);
        consumer.start();

        activeSubscriptions.add(new ActiveSubscription(subscriber, topicFilter, sharedName, consumer));
        log.debug("Subscribed to topic filter '{}' (max {} messages)", topicFilter, sub.maxMessages());
    }

    private void addToBuffer(final @NotNull String topicFilter, final @NotNull PUBLISH publish) {
        final LinkedList<BufferedMqttMessage> buffer = buffers.get(topicFilter);
        if (buffer == null) {
            return;
        }
        final BufferedMqttMessage message = new BufferedMqttMessage(
                publish.getTopic(),
                publish.getPayload(),
                publish.getQoS().getQosNumber(),
                publish.isRetain(),
                Instant.now());
        synchronized (buffer) {
            buffer.addLast(message);
            final int capacity = capacities.getOrDefault(topicFilter, 100);
            while (buffer.size() > capacity) {
                buffer.removeFirst();
            }
        }
    }

    private synchronized void stopAllSubscriptions() {
        for (final ActiveSubscription sub : activeSubscriptions) {
            try {
                sub.consumer().close();
                localTopicTree.removeSubscriber(sub.subscriber(), sub.topicFilter(), sub.sharedName());
            } catch (final Exception e) {
                log.warn("Error stopping topic buffer subscription for filter '{}'", sub.topicFilter(), e);
            }
        }
        activeSubscriptions.clear();
    }

    /**
     * Returns all buffered messages across all topic filters as events, optionally filtered by a since-timestamp.
     */
    public @NotNull List<Event> getAllAsEvents(final @Nullable Long since) {
        final List<Event> events = new ArrayList<>();
        for (final Map.Entry<String, LinkedList<BufferedMqttMessage>> entry : buffers.entrySet()) {
            final String topicFilter = entry.getKey();
            final List<BufferedMqttMessage> snapshot;
            synchronized (entry.getValue()) {
                snapshot = new ArrayList<>(entry.getValue());
            }
            for (final BufferedMqttMessage msg : snapshot) {
                if (since != null && msg.timestamp().toEpochMilli() <= since) {
                    continue;
                }
                events.add(toEvent(topicFilter, msg));
            }
        }
        return events;
    }

    private @NotNull Event toEvent(final @NotNull String topicFilter, final @NotNull BufferedMqttMessage msg) {
        final String payloadStr = msg.payload() != null ? new String(msg.payload(), StandardCharsets.UTF_8) : "";
        return new EventImpl(
                TypeIdentifierImpl.generate(TypeIdentifier.Type.EVENT),
                Event.SEVERITY.INFO,
                msg.topic(),
                PayloadImpl.from(Payload.ContentType.PLAIN_TEXT, payloadStr),
                msg.timestamp().toEpochMilli(),
                null,
                TypeIdentifierImpl.create(TypeIdentifier.Type.EDGE, topicFilter));
    }

    private record ActiveSubscription(
            @NotNull String subscriber,
            @NotNull String topicFilter,
            @NotNull String sharedName,
            @NotNull QueueConsumer consumer) {}
}
