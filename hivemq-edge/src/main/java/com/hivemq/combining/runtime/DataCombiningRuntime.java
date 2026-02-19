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

import static com.hivemq.combining.model.DataIdentifierReference.Type.TOPIC_FILTER;

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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final @NotNull List<InternalConsumer> consumers;
    private final @NotNull ConcurrentHashMap<DataIdentifierReference, List<DataPoint>> tagResults;
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
        this.tagResults = new ConcurrentHashMap<>();
        this.topicFilterToPublish = new ConcurrentHashMap<>();
    }

    public void start() {
        log.debug("Starting data combining {}", combining.id());
        // prepare the script for the data combining
        dataCombiningTransformationService.addScriptForDataCombining(combining);
        final DataIdentifierReference primaryRef = combining.sources().primaryReference();
        combining.instructions().stream()
                .map(Instruction::dataIdentifierReference)
                .filter(Objects::nonNull)
                .filter(ref -> !ref.equals(primaryRef))
                .distinct()
                .forEach(this::subscribeNonPrimary);
        subscribePrimary(primaryRef);
    }

    private void subscribeNonPrimary(final @NotNull DataIdentifierReference ref) {
        subscribe(ref, false);
    }

    private void subscribePrimary(final @NotNull DataIdentifierReference ref) {
        subscribe(ref, true);
    }

    private void subscribe(final @NotNull DataIdentifierReference ref, final boolean isPrimary) {
        InternalConsumer consumer = null;
        switch (ref.type()) {
            case TAG -> {
                log.debug("Starting tag consumer for tag {} with scope {}", ref.id(), ref.scope());
                consumer = new InternalTagConsumer(ref, isPrimary);
            }
            case TOPIC_FILTER -> {
                log.debug("Starting mqtt consumer for filter {}", ref.id());
                consumer = new InternalTopicFilterConsumer(ref, isPrimary);
            }
            default -> log.warn("Unsupported data identifier reference type: {}", ref.type());
        }
        if (consumer != null) {
            consumers.add(consumer);
            consumer.start();
        }
    }

    public void stop() {
        consumers.forEach(InternalConsumer::close);
        dataCombiningTransformationService.removeScriptForDataCombining(combining);
        consumers.clear();
    }

    public void triggerPublish(final @NotNull DataCombining dataCombining) {
        log.debug("Triggering data combining {}", dataCombining.id());
        final var tagsToDataPoints = Map.copyOf(tagResults);
        final var topicFilterResults = Map.copyOf(topicFilterToPublish);
        final ObjectNode rootNode = mapper.createObjectNode();
        topicFilterResults.forEach((topicFilter, publish) -> {
            try {
                rootNode.set(
                        new DataIdentifierReference(topicFilter, TOPIC_FILTER).toFullyQualifiedName(),
                        mapper.readTree(publish.getPayload()));
            } catch (final IOException e) {
                log.warn("Exception during json parsing of payload '{}'", publish.getPayload());
                throw new RuntimeException(e);
            }
        });

        tagsToDataPoints.forEach((tagRef, dataPoints) -> dataPoints.forEach(dataPoint -> {
            try {
                rootNode.set(
                        tagRef.toFullyQualifiedName(),
                        mapper.readTree(dataPoint.getTagValue().toString()));
            } catch (final IOException e) {
                log.warn("Exception during json parsing of datapoint '{}'", dataPoint.getTagValue());
                throw new RuntimeException(e);
            }
        }));

        dataCombiningPublishService.publish(
                combining.destination(), rootNode.toString().getBytes(StandardCharsets.UTF_8), dataCombining);
    }

    public abstract class InternalConsumer {
        protected final @NotNull DataIdentifierReference dataIdentifierReference;
        protected final boolean primary;

        public InternalConsumer(final @NotNull DataIdentifierReference dataIdentifierReference, final boolean primary) {
            this.dataIdentifierReference = dataIdentifierReference;
            this.primary = primary;
        }

        public abstract void close();

        public abstract void start();
    }

    public final class InternalTopicFilterConsumer extends InternalConsumer {
        private static final byte DEFAULT_FLAGS = SubscriptionFlag.getDefaultFlags(true, true, false);
        private final @NotNull QueueConsumer queueConsumer;
        private final @NotNull String queueId;
        private final @NotNull String subscriber;
        private final @NotNull String topicFilter;

        public InternalTopicFilterConsumer(
                final @NotNull DataIdentifierReference dataIdentifierReference, final boolean primary) {
            super(dataIdentifierReference, primary);
            this.queueId = combining.id() + "/" + dataIdentifierReference.id();
            this.subscriber = combining.id() + "#";
            this.topicFilter = dataIdentifierReference.id();
            this.queueConsumer =
                    new QueueConsumer(clientQueuePersistence, combining.id() + "/" + topicFilter, singleWriterService) {
                        @Override
                        public void process(final @NotNull PUBLISH publish) {
                            topicFilterToPublish.put(topicFilter, publish);
                            if (primary) {
                                triggerPublish(combining);
                            }
                        }
                    };
        }

        public @NotNull String getQueueId() {
            return queueId;
        }

        @Override
        public void close() {
            queueConsumer.close();
            localTopicTree.removeSubscriber(
                    subscriber, topicFilter, combining.id().toString()); // I guess we should keep the subscription?
        }

        @Override
        public void start() {
            localTopicTree.addTopic(
                    subscriber,
                    new Topic(topicFilter, QoS.EXACTLY_ONCE, false, true),
                    DEFAULT_FLAGS,
                    combining.id().toString());
            queueConsumer.start();
        }
    }

    public final class InternalTagConsumer extends InternalConsumer implements TagConsumer {
        public InternalTagConsumer(
                final @NotNull DataIdentifierReference dataIdentifierReference, final boolean primary) {
            super(dataIdentifierReference, primary);
        }

        @Override
        public @NotNull String getTagName() {
            return dataIdentifierReference.id();
        }

        @Override
        public @NotNull String getScope() {
            return dataIdentifierReference.scope();
        }

        @Override
        public void accept(final @NotNull List<DataPoint> dataPoints) {
            // Use DataIdentifierReference as the key to include scope
            tagResults.put(dataIdentifierReference, dataPoints);
            if (primary) {
                try {
                    triggerPublish(combining);
                } catch (final Exception e) {
                    log.warn("Unable to process data points '{}'", dataPoints, e);
                }
            }
        }

        @Override
        public void start() {
            tagManager.addConsumer(this);
        }

        @Override
        public void close() {
            tagManager.removeConsumer(this);
        }
    }
}
