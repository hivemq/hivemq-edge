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

import com.fasterxml.jackson.databind.JsonNode;
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
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.hivemq.combining.runtime.SourceSanitizer.sanitize;

/// `DataCombiningRuntime` manages a **combiner mapping** (aka "datacombing"),
/// that assembles inputs, which can be tags or topic-filters, into a destination message.
/// `start()` subscribes to all the inputs (tags and topic-filters), including the trigger (aka primary).
/// When values arrive (via `accept()` or `process()`), they are stored in the concurrent hashmap `values`.
/// When the trigger arrives, the `assembleAndPublish()` method is called which assembles all inputs into an object
/// and calls the `dataCombiningPublishingService` to turn that into a message and publish that to the target topic.
public class DataCombiningRuntime {

    private static final Logger log = LoggerFactory.getLogger(DataCombiningRuntime.class);

    private final @NotNull DataCombining dataCombining;

    /// `tagManager` manages the subscriptions for tags
    private final @NotNull TagManager tagManager;

    /// `localTopicTree` enqueues published messages matching a topic filter to `clientQueuePersistence`
    /// `singleWriterService` is the concurrency mechanism to serialize all access for `clientQueuePersistence`
    private final @NotNull LocalTopicTree localTopicTree;
    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull SingleWriterService singleWriterService;

    /// add some description for these two (why are there two?)
    private final @NotNull DataCombiningPublishService dataCombiningPublishService;
    private final @NotNull DataCombiningTransformationService dataCombiningTransformationService;
    private final @NotNull ObjectMapper mapper;

    /// subscriptions remembers all the subscriptions for tags and topic filters, so we can unsubscribe them again
    private final @NotNull List<InternalSubscription> subscriptions;

    /// `values` is the hash map where we store values, it must be a concurrent map since values may arrive concurrent
    private final @NotNull ConcurrentHashMap<String, Value> values;

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

    /// `starts()` collects all the inputs and subscribes to them all
    public void start() {
        log.debug("Starting data combining {}", dataCombining.id());

        dataCombiningTransformationService.addScriptForDataCombining(dataCombining);

        DataIdentifierReference trigger = dataCombining.sources().primaryReference();

        /// collect subscriptions for all the distinct inputs, except for the trigger
        subscriptions.addAll(dataCombining.instructions()
                .stream()
                .map(Instruction::dataIdentifierReference)
                .filter(Objects::nonNull)
                .filter(ref -> !ref.equals(trigger))
                .distinct()
                .map(ref -> internalSubscription(ref, false, true))
                .toList());

        /// collect a subscription for the trigger, check whether it is used in an instructions and we need its value
        boolean isValueUsed = dataCombining.instructions()
                .stream()
                .map(Instruction::dataIdentifierReference)
                .filter(Objects::nonNull)
                .anyMatch(ref -> ref.equals(trigger));
        subscriptions.add(internalSubscription(trigger, true, isValueUsed));

        /// now subscribe to them all
        subscriptions.forEach(InternalSubscription::subscribe);
    }

    /// `stop()` unsubscribes all `subscriptions` again
    public void stop() {
        log.debug("Stoping data combining {}", dataCombining.id());

        subscriptions.forEach(InternalSubscription::unSubscribe);
        subscriptions.clear();

        dataCombiningTransformationService.removeScriptForDataCombining(dataCombining);
    }

    /// `assembleAndPublish` is what it really is all about, assembling values and publishing a new, combined message
    public void assembleAndPublish() {
        log.debug("Triggering data combining {}", dataCombining.id());
        final ObjectNode inputValuesAsDictObject = mapper.createObjectNode();
        final var valuesSnapshot = Map.copyOf(values);

        valuesSnapshot.forEach((propertyName, propertyValue) -> inputValuesAsDictObject.set(propertyName,
                propertyValue.getTagValue4Combiner()));

        dataCombiningPublishService.publish(dataCombining.destination(),
                inputValuesAsDictObject.toString().getBytes(StandardCharsets.UTF_8),
                dataCombining);
    }

    /// `Value` represents values to be used in the combining (`assempleAndPublish`) as JsonNodes.
    /// It is created holding the `RawValue` (either tag value or topic-filter payload).
    /// It converts the raw value to a `JsonNode` when requested (lazily) and caches the result of the conversion.
    /// That way we only convert raw values when needed,
    /// so we don't convert raw values that are overwritten by other raw values before a trigger arrives.
    /// And we also don't convert a raw value multiple times,
    /// so we convert raw values that don't change while multiple triggers arrive only once.
    /// There are two concrete implementations for `RawValue`, for tags and for topic-filters.
    public static final class Value {
        @Nullable RawValue value;
        @Nullable JsonNode jsonNode;

        public Value(final @NotNull RawValue value) {
            this.value = value;
            this.jsonNode = null;
        }

        public JsonNode getTagValue4Combiner() {
            if (value != null) {
                jsonNode = value.getTagValue4Combiner();
                value = null;
            }
            return jsonNode;
        }
    }

    public interface RawValue {
        @NotNull JsonNode getTagValue4Combiner();
    }

    public final class RawValueTag implements RawValue {
        DataPoint dataPoint;

        RawValueTag(final DataPoint dataPoint) {
            this.dataPoint = dataPoint;
        }

        @Override
        public @NotNull JsonNode getTagValue4Combiner() {
            try {
                return mapper.readTree(dataPoint.getTagValue().toString());
            } catch (final IOException e) {
                log.warn("Exception during json parsing of datapoint '{}'", dataPoint.getTagValue().toString());
                throw new RuntimeException(e);
            }
        }
    }

    public final class RawValueTopicFilter implements RawValue {
        byte[] payload;

        RawValueTopicFilter(final byte[] payload) {
            this.payload = payload;
        }

        @Override
        public @NotNull JsonNode getTagValue4Combiner() {
            try {
                return mapper.readTree(new String(payload, StandardCharsets.UTF_8));
            } catch (final IOException e) {
                log.warn("Exception during json parsing of message '{}'", new String(payload, StandardCharsets.UTF_8));
                throw new RuntimeException(e);
            }
        }
    }

    /// `internalSubscription` returns an, initially inactive, subscription for the input (tag or topic filter),
    /// by creating either an `InternalSubscriptionTag` or an `InternalSubscriptionTopicFilter`
    public InternalSubscription internalSubscription(
            final @NotNull DataIdentifierReference ref,
            final boolean isTrigger,
            final boolean isValueUsed) {
        log.debug("Starting {} consumer for {}", ref.type(), ref);
        switch (ref.type()) {
            case TAG -> {
                return new InternalSubscriptionTag(ref, isTrigger, isValueUsed);
            }
            case TOPIC_FILTER -> {
                return new InternalSubscriptionTopicFilter(ref, isTrigger, isValueUsed);
            }
            case PULSE_ASSET -> {
                log.error("Pulse Assets shouldn't be input to data combining in Edge {}", ref.id());
                throw new RuntimeException("Pulse Assets shouldn't be input to data combining in Edge");
            }
        }
        return null;
    }

    public interface InternalSubscription {

        void subscribe();

        void unSubscribe();
    }

    /// Tags are subscribed to the `TagManager` with a `TagConsumer` and its `accept()` method.
    public final class InternalSubscriptionTag implements InternalSubscription {
        private final TagConsumer consumer;

        public InternalSubscriptionTag(
                final @NotNull DataIdentifierReference ref,
                final boolean isTrigger,
                final boolean isValueUsed) {

            this.consumer = new TagConsumer() {
                @Override
                public @NotNull String getTagName() {
                    /// TODO(mschoene) this must become `sanitized(ref)`!
                    /// so that `tagManager` knows which tag is meant (if there are multiple with the same tagname)
                    /// I presume such a change has been made to tag manager in Sam's epic
                    return ref.id();
                }

                /// TODO(mschoene) I'm not sure that the TagConsumer should get a scope?
                /// I think it should return the ref, and then the ref has one way to deterministically serialize itself
                public String getScope() {
                    return ref.scope();
                }

                @Override
                public void accept(final @NotNull List<DataPoint> dataPoints) {
                    if (isValueUsed && !dataPoints.isEmpty()) {
                        values.put(sanitize(ref), new Value(new RawValueTag(dataPoints.getLast())));
                    }
                    if (isTrigger) {
                        assembleAndPublish();
                    }
                }
            };

        }

        public void subscribe() {
            tagManager.addConsumer(consumer);
        }

        public void unSubscribe() {
            tagManager.removeConsumer(consumer);
        }
    }

    /// `TopicFilters` are subscribed to the `TopicTree` with a `QueueConsumer` and its `process()` method.
    public final class InternalSubscriptionTopicFilter implements InternalSubscription {
        private final @NotNull String consumerId;
        private final @NotNull String subscriber;
        private final @NotNull String topicFilter;
        private final @NotNull QueueConsumer consumer;

        InternalSubscriptionTopicFilter(
                final @NotNull DataIdentifierReference ref,
                final boolean isTrigger,
                final boolean isValueUsed) {

            /// Note that the steps here and in `subscribe()` must be done exactly in this order and with these values.
            /// It is a problem that `DataCombiningRuntime` needs to know so much about how to subscribe a topic filter.
            /// The whole sequence should have been combined and encapsulated somewhere central.

            /// `consumerId` must be an id for the subscription to the topic tree,
            /// with the intent that publish can distinguish different subscribers to the same topic filter;
            /// the only requirement for this id is uniqueness, so we simply use the combiner mapping UUID.
            this.consumerId = dataCombining.id().toString();

            /// `subscriber` must be an id for the subscription to the topic tree,
            /// with the intent that we can unsubscribe this exact subscription again later (in `close`);
            /// the only requirement for this id is also uniqueness, so we use the combiner mapping UUID again
            /// and append `#` to avoid any risk of clashes with `consumerId` should they ever be used in the same hash.
            this.subscriber = consumerId + "#";

            /// the topic filter that we subscribe to
            this.topicFilter = ref.id();

            /// Create the queue consumer with a callback for the same slot (initially inactive and not on the queue).
            /// This must be the same slot that `addTopic` enqueues to, so `queueId` must be `consumerId/topicFilter`.
            String queueId = consumerId + "/" + topicFilter;
            this.consumer = new QueueConsumer(clientQueuePersistence, queueId, singleWriterService) {
                @Override
                public void process(final @NotNull PUBLISH message) {
                    if (isValueUsed) {
                        values.put(sanitize(ref), new Value(new RawValueTopicFilter(message.getPayload())));
                    }
                    if (isTrigger) {
                        assembleAndPublish();
                    }
                }
            };

        }

        public void subscribe() {

            /// add the consumer to the queue, start listening for messages enqueued to this slot
            consumer.start();

            /// Add the topic filter to the `localTopicTree`, so that when a message is published to `topicFilter`
            /// it will be enqueued to `clientPersistenceQueue` in the slot `consumerId/topicFilter`.
            /// First adding the consumer and then the topic filter to the topic tree means that a consumer already
            /// exists when the messages are being enqueued, though there is an initial `submitPoll` to also handle it.
            localTopicTree.addTopic(subscriber,
                    new Topic(topicFilter, QoS.EXACTLY_ONCE, false, true),
                    SubscriptionFlag.getDefaultFlags(true, true, false),
                    consumerId);

        }

        public void unSubscribe() {
            localTopicTree.removeSubscriber(subscriber, topicFilter, consumerId);
            consumer.close();
        }
    }
}
