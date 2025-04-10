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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.combining.mapping.DataCombiningTransformationService;
import com.hivemq.combining.model.DataCombining;
import com.hivemq.combining.model.DataIdentifierReference;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.hivemq.combining.model.DataIdentifierReference.Type.TAG;
import static com.hivemq.combining.model.DataIdentifierReference.Type.TOPIC_FILTER;
import static com.hivemq.combining.runtime.SourceSanitizer.sanitize;

public class DataCombiningRuntime {

    private static final Logger log = LoggerFactory.getLogger(DataCombiningRuntime.class);

    private final @NotNull DataCombining combining;
    private final @NotNull LocalTopicTree localTopicTree;
    private final @NotNull TagManager tagManager;
    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull SingleWriterService singleWriterService;
    private final @NotNull DataCombiningPublishService dataCombiningPublishService;
    private final @NotNull DataCombiningTransformationService dataCombiningTransformationService;
    private final @NotNull ObjectMapper mapper;
    private final @NotNull List<InternalTagConsumer> consumers;
    private final @NotNull List<InternalSubscription> internalSubscriptions;
    private final @NotNull ConcurrentHashMap<String, List<DataPoint>> tagResults;
    private final @NotNull ConcurrentHashMap<String, PUBLISH> topicFilterToPublish;

    public DataCombiningRuntime(
            final @NotNull DataCombining combining,
            final @NotNull LocalTopicTree localTopicTree,
            final @NotNull TagManager tagManager,
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull SingleWriterService singleWriterService,
            final @NotNull DataCombiningPublishService dataCombiningPublishService,
            final @NotNull DataCombiningTransformationService dataCombiningTransformationService) {
        this.combining = combining;
        this.localTopicTree = localTopicTree;
        this.tagManager = tagManager;
        this.clientQueuePersistence = clientQueuePersistence;
        this.singleWriterService = singleWriterService;
        this.dataCombiningPublishService = dataCombiningPublishService;
        this.dataCombiningTransformationService = dataCombiningTransformationService;
        this.mapper = new ObjectMapper();
        this.consumers = new ArrayList<>();
        this.internalSubscriptions = new ArrayList<>();
        this.tagResults = new ConcurrentHashMap<>();
        this.topicFilterToPublish = new ConcurrentHashMap<>();
    }

    public void start() {
        log.debug("Starting data combining {}", combining.id());
        // prepare the script for the data combining
        dataCombiningTransformationService.addScriptForDataCombining(combining);

        combining.sources().tags().stream().map(tag -> {
            log.debug("Starting tag consumer for tag {}", tag);
            return new InternalTagConsumer(tag,
                    combining,
                    TAG.equals(combining.sources().primaryReference().type()) &&
                            tag.equals(combining.sources().primaryReference().id()));
        }).forEach(consumer -> {
            tagManager.addConsumer(consumer);
            consumers.add(consumer);
        });

        combining.sources().topicFilters().forEach(topicFilter -> {
            log.debug("Starting mqtt consumer for filter {}", topicFilter);
            internalSubscriptions.add(subscribeTopicFilter(combining,
                    topicFilter,
                    TOPIC_FILTER.equals(combining.sources().primaryReference().type()) &&
                            topicFilter.equals(combining.sources().primaryReference().id())));
        });

        internalSubscriptions.forEach(internalSubscription -> internalSubscription.queueConsumer().start());
    }

    public void stop() {
        consumers.forEach(tagManager::removeConsumer);
        internalSubscriptions.forEach(sub -> {
            sub.queueConsumer().close();
            localTopicTree.removeSubscriber(sub.subscriber(),
                    sub.topic(),
                    sub.sharedName()); //I guess we should keep the subscription?
        });

        dataCombiningTransformationService.removeScriptForDataCombining(combining);
    }

    public @NotNull InternalSubscription subscribeTopicFilter(
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
        final ObjectNode rootNode = mapper.createObjectNode();
        topicFilterResults.forEach((topicFilter, publish) -> {
            try {
                rootNode.set(sanitize(new DataIdentifierReference(topicFilter, TOPIC_FILTER)),
                        mapper.readTree(publish.getPayload()));
            } catch (final IOException e) {
                log.warn("Exception during json parsing of payload '{}'", publish.getPayload());
                throw new RuntimeException(e);
            }
        });

        tagsToDataPoints.forEach((tagName, dataPoints) -> dataPoints.forEach(dataPoint -> {
            try {
                rootNode.set(sanitize(new DataIdentifierReference(tagName, TAG)),
                        mapper.readTree(dataPoint.getTagValue().toString()));
            } catch (final IOException e) {
                log.warn("Exception during json parsing of datapoint '{}'", dataPoint.getTagValue());
                throw new RuntimeException(e);
            }
        }));

        dataCombiningPublishService.publish(combining.destination(),
                rootNode.toString().getBytes(StandardCharsets.UTF_8),
                dataCombining);
    }

    public record InternalSubscription(@NotNull String subscriber, @NotNull String topic, @NotNull String sharedName,
                                       @NotNull QueueConsumer queueConsumer) {
        public @NotNull String getQueueId() {
            return sharedName() + '/' + topic();
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
        public void accept(final @NotNull List<DataPoint> dataPoints) {
            tagResults.put(tagName, dataPoints);
            if (isPrimary) {
                try {
                    triggerPublish(dataCombining);
                } catch (final Exception e) {
                    log.warn("Unable to process data points '{}'", dataPoints, e);
                }
            }
        }
    }
}
