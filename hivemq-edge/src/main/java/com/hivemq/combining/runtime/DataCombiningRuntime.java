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
import com.hivemq.datapoint.DataPointWithMetadata;
import com.hivemq.edge.modules.adapters.data.TagManager;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.clientqueue.InternalTopicFilterSubscriber;
import com.hivemq.persistence.mappings.fieldmapping.Instruction;
import com.hivemq.protocols.northbound.SingleTagConsumer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    private final @NotNull ConcurrentHashMap<DataIdentifierReference, DataPoint> tagResults;
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
        final DataIdentifierReference primaryReference = combining.sources().primaryReference();
        if (primaryReference == null) {
            log.warn("Primary reference of data combining {} is null", combining.id());
        } else {
            final AtomicBoolean primaryProcessed = new AtomicBoolean();
            combining.instructions().stream()
                    .map(Instruction::dataIdentifierReference)
                    .filter(Objects::nonNull)
                    .distinct()
                    .forEach(dataIdentifierReference -> {
                        if (dataIdentifierReference.equals(primaryReference)) {
                            primaryProcessed.set(true);
                            subscribe(dataIdentifierReference, true, true);
                        } else {
                            subscribe(dataIdentifierReference, false, true);
                        }
                    });
            if (!primaryProcessed.get()) {
                subscribe(primaryReference, true, false);
            }
        }
    }

    private void subscribe(
            final @NotNull DataIdentifierReference dataIdentifierReference,
            final boolean primary,
            final boolean storeDataPoints) {
        InternalConsumer consumer = null;
        switch (dataIdentifierReference.type()) {
            case TAG -> {
                log.debug(
                        "Starting tag consumer for tag {} with scope {}",
                        dataIdentifierReference.id(),
                        dataIdentifierReference.scope());
                consumer = new InternalTagConsumer(dataIdentifierReference, primary, storeDataPoints);
            }
            case TOPIC_FILTER -> {
                log.debug("Starting mqtt consumer for filter {}", dataIdentifierReference.id());
                consumer = new InternalTopicFilterConsumer(dataIdentifierReference, primary, storeDataPoints);
            }
            default -> log.warn("Unsupported data identifier reference type: {}", dataIdentifierReference.type());
        }
        if (consumer != null) {
            consumers.add(consumer);
            consumer.start();
        }
    }

    public void stop() {
        consumers.forEach(InternalConsumer::stop);
        consumers.clear();
        dataCombiningTransformationService.removeScriptForDataCombining(combining);
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

        tagsToDataPoints.forEach((tagRef, dataPoint) -> {
            if (dataPoint instanceof final DataPointWithMetadata dpMeta) {
                rootNode.set(tagRef.toFullyQualifiedName(), dpMeta.getJsonNode());
            } else {
                try {
                    rootNode.set(
                            tagRef.toFullyQualifiedName(),
                            mapper.readTree(dataPoint.getTagValue().toString()));
                } catch (final IOException e) {
                    log.warn("Exception during json parsing of datapoint '{}'", dataPoint.getTagValue());
                    throw new RuntimeException(e);
                }
            }
        });

        dataCombiningPublishService.publish(
                combining.destination(), rootNode.toString().getBytes(StandardCharsets.UTF_8), dataCombining);
    }

    public interface InternalConsumer {
        void start();

        void stop();
    }

    public final class InternalTopicFilterConsumer extends InternalTopicFilterSubscriber implements InternalConsumer {
        private static final String COMPONENT_PREFIX = "combiner";

        private final @NotNull String topicFilter;
        private final boolean primary;
        private final boolean storeDataPoints;

        public InternalTopicFilterConsumer(
                final @NotNull DataIdentifierReference dataIdentifierReference,
                final boolean primary,
                final boolean storeDataPoints) {
            super(
                    COMPONENT_PREFIX,
                    combining.id() + "/" + dataIdentifierReference.id(),
                    dataIdentifierReference.id(),
                    localTopicTree,
                    clientQueuePersistence,
                    singleWriterService);
            this.topicFilter = dataIdentifierReference.id();
            this.primary = primary;
            this.storeDataPoints = storeDataPoints;
        }

        @Override
        public void process(final @NotNull PUBLISH publish) {
            if (storeDataPoints) {
                topicFilterToPublish.put(topicFilter, publish);
            }
            if (primary) {
                triggerPublish(combining);
            }
        }
    }

    public final class InternalTagConsumer implements InternalConsumer, SingleTagConsumer {
        private final @NotNull DataIdentifierReference dataIdentifierReference;
        private final boolean primary;
        private final boolean storeDataPoints;

        public InternalTagConsumer(
                final @NotNull DataIdentifierReference dataIdentifierReference,
                final boolean primary,
                final boolean storeDataPoints) {
            this.dataIdentifierReference = dataIdentifierReference;
            this.primary = primary;
            this.storeDataPoints = storeDataPoints;
        }

        @Override
        public @NotNull String getTagName() {
            return dataIdentifierReference.id();
        }

        @Override
        public @Nullable String getScope() {
            return dataIdentifierReference.scope();
        }

        @Override
        public void accept(final @NotNull DataPoint dataPoint) {
            // Use DataIdentifierReference as the key to include scope
            if (storeDataPoints) {
                tagResults.put(dataIdentifierReference, dataPoint);
            }
            if (primary) {
                try {
                    triggerPublish(combining);
                } catch (final Exception e) {
                    log.warn("Unable to process data point '{}'", dataPoint, e);
                }
            }
        }

        @Override
        public void start() {
            tagManager.addConsumer(this);
        }

        @Override
        public void stop() {
            tagManager.removeConsumer(this);
        }
    }
}
