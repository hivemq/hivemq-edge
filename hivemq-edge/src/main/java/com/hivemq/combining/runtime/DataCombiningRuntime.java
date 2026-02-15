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
import com.hivemq.persistence.mappings.fieldmapping.Instruction;
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

    private final @NotNull DataCombining dataCombining;
    private final @NotNull LocalTopicTree localTopicTree;
    private final @NotNull TagManager tagManager;
    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull SingleWriterService singleWriterService;
    private final @NotNull DataCombiningPublishService dataCombiningPublishService;
    private final @NotNull DataCombiningTransformationService dataCombiningTransformationService;

    private final @NotNull ObjectMapper mapper;
    private final @NotNull List<InternalSubscription> subscriptions;
    private final @NotNull ConcurrentHashMap<String, String> values;

    public DataCombiningRuntime(
            final @NotNull DataCombining dataCombining,
            final @NotNull LocalTopicTree localTopicTree,
            final @NotNull TagManager tagManager,
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull SingleWriterService singleWriterService,
            final @NotNull DataCombiningPublishService dataCombiningPublishService,
            final @NotNull DataCombiningTransformationService dataCombiningTransformationService) {
        this.dataCombining = dataCombining;
        this.localTopicTree = localTopicTree;
        this.tagManager = tagManager;
        this.clientQueuePersistence = clientQueuePersistence;
        this.singleWriterService = singleWriterService;
        this.dataCombiningPublishService = dataCombiningPublishService;
        this.dataCombiningTransformationService = dataCombiningTransformationService;

        this.mapper = new ObjectMapper();
        this.subscriptions = new ArrayList<>();
        this.values = new ConcurrentHashMap<>();
    }

    public void start() {
        log.debug("Starting data combining {}", dataCombining.id());

        dataCombiningTransformationService.addScriptForDataCombining(dataCombining);

        DataIdentifierReference trigger = dataCombining.sources().primaryReference();

        dataCombining.instructions()
                .stream()
                .map(Instruction::dataIdentifierReference)
                .filter(ref -> !ref.equals(trigger))
                .distinct()
                .forEach(ref -> subscribe(ref, false, true));

        // subscribe to the trigger last, gives the other subscriptions a little bit more time to receive a value
        boolean providesValue = dataCombining.instructions()
                .stream()
                .map(Instruction::dataIdentifierReference)
                .anyMatch(ref -> ref.equals(trigger));
        subscribe(trigger, true, providesValue);
    }

    public void stop() {
        log.debug("Stoping data combining {}", dataCombining.id());

        subscriptions.forEach(InternalSubscription::unSubscribe);

        dataCombiningTransformationService.removeScriptForDataCombining(dataCombining);
    }

    public void triggerPublish() {
        log.debug("Triggering data combining {}", dataCombining.id());
        final ObjectNode rootNode = mapper.createObjectNode();
        final var valuesSnapshot = Map.copyOf(values);

        valuesSnapshot.forEach((propertyName, propertyValue) -> {
            try {
                rootNode.set(propertyName, mapper.readTree(propertyValue));
            } catch (final IOException e) {
                log.warn("Exception during json parsing of datapoint {} : '{}'", propertyName, propertyValue);
                throw new RuntimeException(e);
            }
        });

        dataCombiningPublishService.publish(dataCombining.destination(),
                rootNode.toString().getBytes(StandardCharsets.UTF_8),
                dataCombining);
    }

    public void subscribe(
            final @NotNull DataIdentifierReference ref,
            final boolean isTrigger,
            final boolean providesValue) {
        log.debug("Starting {} consumer for {}", ref.type(), ref.id());
        switch (ref.type()) {
            case TAG:
                subscriptions.add(new InternalTagSubscription(ref.id(), isTrigger, providesValue));
                break;
            case TOPIC_FILTER:
                subscriptions.add(new InternalTopicFilterSubscription(ref.id(), isTrigger, providesValue));
                break;
            default:
                // what should happen with PULSE_ASSET???
        }
    }

    public interface InternalSubscription {
        void unSubscribe();
    }

    public final class InternalTagSubscription implements InternalSubscription {
        private final TagConsumer consumer;

        public InternalTagSubscription(
                final @NotNull String tagName,
                final boolean isTrigger,
                final boolean providesValue) {

            this.consumer = new TagConsumer() {
                @Override
                public @NotNull String getTagName() {
                    return tagName;
                }

                @Override
                public void accept(final @NotNull List<DataPoint> dataPoints) {
                    if (providesValue && !dataPoints.isEmpty()) {
                        String propertyName = sanitize(new DataIdentifierReference(tagName, TAG));
                        String propertyValue = dataPoints.getLast().getTagValue().toString();
                        values.put(propertyName, propertyValue);
                    }
                    if (isTrigger) {
                        triggerPublish();
                    }
                }
            };

            tagManager.addConsumer(consumer);
        }

        public void unSubscribe() {
            tagManager.removeConsumer(consumer);
        }
    }

    public final class InternalTopicFilterSubscription implements InternalSubscription {
        private final @NotNull String subscriber;
        private final @NotNull String topicFilter;
        private final @NotNull String sharedName;
        private final @NotNull QueueConsumer consumer;

        InternalTopicFilterSubscription(
                final @NotNull String topicFilter,
                final boolean isTrigger,
                final boolean providesValue) {
            this.subscriber = dataCombining.id().toString() + "#";
            this.topicFilter = topicFilter;
            this.sharedName = dataCombining.id().toString();
            String queueId = dataCombining.id().toString() + "/" + topicFilter;

            localTopicTree.addTopic(subscriber,
                    new Topic(topicFilter, QoS.EXACTLY_ONCE, false, true),
                    SubscriptionFlag.getDefaultFlags(true, true, false),
                    sharedName);

            this.consumer = new QueueConsumer(clientQueuePersistence, queueId, singleWriterService) {
                @Override
                public void process(final @NotNull PUBLISH message) {
                    if (providesValue) {
                        String propertyName = sanitize(new DataIdentifierReference(topicFilter, TOPIC_FILTER));
                        String propertyValue = new String(message.getPayload(), StandardCharsets.UTF_8);
                        values.put(propertyName, propertyValue);
                    }
                    if (isTrigger) {
                        triggerPublish();
                    }
                }
            };

            consumer.start();
        }

        public void unSubscribe() {
            consumer.close();
            localTopicTree.removeSubscriber(subscriber, topicFilter, sharedName);
        }
    }
}
