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
package com.hivemq.combining.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.combining.model.DataCombining;
import com.hivemq.edge.modules.adapters.data.TagManager;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriptionFlag;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.protocols.northbound.TagConsumer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.hivemq.combining.model.DataIdentifierReference.Type.TAG;
import static com.hivemq.combining.model.DataIdentifierReference.Type.TOPIC_FILTER;

public class DataCombiningRuntime {

    private static final Logger log = LoggerFactory.getLogger(DataCombiningRuntime.class);

    private final @NotNull DataCombining combining;
    private final @NotNull LocalTopicTree localTopicTree;
    private final @NotNull TagManager tagManager;
    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull SingleWriterService singleWriterService;
    private final @NotNull DataCombiningPublishService dataCombiningPublishService;

    private final @NotNull ObjectMapper mapper;

    private final List<InternalTagConsumer> consumers = new ArrayList<>();
    private final List<InternalSubscription> internalSubscriptions = new ArrayList<>();

    private final ConcurrentHashMap<String, List<DataPoint>> tagResults = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PUBLISH> topicFilterToPublish = new ConcurrentHashMap<>();

    public DataCombiningRuntime(
            final @NotNull DataCombining combining,
            final @NotNull LocalTopicTree localTopicTree,
            final @NotNull TagManager tagManager,
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull SingleWriterService singleWriterService,
            final @NotNull DataCombiningPublishService dataCombiningPublishService) {
        this.combining = combining;
        this.localTopicTree = localTopicTree;
        this.tagManager = tagManager;
        this.clientQueuePersistence = clientQueuePersistence;
        this.singleWriterService = singleWriterService;
        this.dataCombiningPublishService = dataCombiningPublishService;
        this.mapper = new ObjectMapper();
    }

    public void start() {
        combining.sources()
                .tags()
                .stream()
                .map(tag -> new InternalTagConsumer(tag,
                        combining,
                        TAG.equals(combining.sources().primaryReference().type()) &&
                                tag.equals(combining.sources().primaryReference().id())))
                .forEach(consumer -> {
                    tagManager.addConsumer(consumer);
                    consumers.add(consumer);
                });

        combining.sources().topicFilters().forEach(topicFilter -> {
            internalSubscriptions.add(subscribeTopicFilter(combining,
                    topicFilter,
                    TOPIC_FILTER.equals(combining.sources().primaryReference().type()) &&
                            topicFilter.equals(combining.sources().primaryReference().id())));
        });

        internalSubscriptions.forEach(internalSubscription -> {
            internalSubscription.queueConsumer().start();
        });
    }

    public void stop() {
        consumers.forEach(tagManager::removeConsumer);
        internalSubscriptions.forEach(sub -> {
            sub.queueConsumer().close();
            localTopicTree.removeSubscriber(sub.subscriber(),
                    sub.topic(),
                    sub.sharedName()); //I guess we should keep the subscription?
        });
    }

    public InternalSubscription subscribeTopicFilter(
            final @NotNull DataCombining dataCombining,
            final @NotNull String topicFilter,
            final boolean isPrimary) {
        final String clientId = dataCombining.id() + "#";
        final QoS qos = QoS.EXACTLY_ONCE;

        final var subscription = new InternalSubscription(clientId,
                topicFilter,
                dataCombining.id().toString(),
                new QueueConsumer(clientQueuePersistence, dataCombining.id() + "/" + topicFilter, singleWriterService) {
                    @Override
                    public void process(final @NotNull PUBLISH publish) {
                        topicFilterToPublish.put(topicFilter, publish);
                        if (isPrimary) {
                            triggerPublish(dataCombining);
                        }
                    }
                });

        localTopicTree.addTopic(subscription.subscriber(),
                new Topic(subscription.topic(), qos, false, true),
                SubscriptionFlag.getDefaultFlags(true, true, false),
                subscription.sharedName());

        return subscription;
    }

    public void triggerPublish(final @NotNull DataCombining dataCombining) {
        final var tagsToDataPoints = Map.copyOf(tagResults);
        final var topicFilterResults = Map.copyOf(topicFilterToPublish);

        final Map<String, Object> outgoing = new HashMap<>();

        topicFilterResults.forEach((topicFilter, publish) -> {
            outgoing.put(topicFilter, new String(publish.getPayload()));
        });

        tagsToDataPoints.forEach((tagName, dataPoints) -> {
            dataPoints.forEach(dataPoint -> {
                outgoing.put(dataPoint.getTagName(), dataPoint.getTagValue().toString());
            });
        });

        try {
            dataCombiningPublishService.publish(combining.destination(),
                    mapper.writeValueAsBytes(outgoing), dataCombining);
        } catch (final JsonProcessingException e) {
            log.error("Can't produce JSON", e);
            throw new RuntimeException(e);
        }

    }

    public final class InternalTagConsumer implements TagConsumer {
        private final @NotNull String tagName;
        private final boolean isPrimary;
        private final @NotNull DataCombining dataCombining;

        public InternalTagConsumer(
                final @NotNull String tagName,
                final @NotNull DataCombining dataCombining,
                final boolean isPrimary) {
            this.tagName = tagName;
            this.dataCombining = dataCombining;
            this.isPrimary = isPrimary;
        }

        @Override
        public @NotNull String getTagName() {
            return tagName;
        }

        @Override
        public void accept(final List<DataPoint> dataPoints) {
            tagResults.put(tagName, dataPoints);
            if (isPrimary) {
                triggerPublish(dataCombining);
            }
        }
    }

    public record InternalSubscription(@NotNull String subscriber, @NotNull String topic, @NotNull String sharedName,
                                       @NotNull QueueConsumer queueConsumer) {
        public String getQueueId() {
            return sharedName() + "/" + topic();
        }
    }


}
