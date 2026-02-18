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
    private final @NotNull LocalTopicTree localTopicTree;
    private final @NotNull TagManager tagManager;
    private final @NotNull ClientQueuePersistence clientQueuePersistence; // what is the purpose of this?
    private final @NotNull SingleWriterService singleWriterService; // what is the purpose of this?
    private final @NotNull DataCombiningPublishService dataCombiningPublishService;
    private final @NotNull DataCombiningTransformationService dataCombiningTransformationService; // and this

    private final @NotNull ObjectMapper mapper;
    private final @NotNull List<InternalSubscription> subscriptions;
    // values must be a concurrent hash map, values may arrive anytime
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

    /// `starts()` subscribes to all inputs
    public void start() {
        log.debug("Starting data combining {}", dataCombining.id());

        dataCombiningTransformationService.addScriptForDataCombining(dataCombining);

        DataIdentifierReference trigger = dataCombining.sources().primaryReference();

        // subscribe to all inputs, except for the trigger; subscribe to each input only once
        dataCombining.instructions()
                .stream()
                .map(Instruction::dataIdentifierReference)
                .filter(Objects::nonNull)
                .filter(ref -> !ref.equals(trigger))
                .distinct()
                .forEach(ref -> subscribe(ref, false, true));

        // subscribe to the trigger last, gives the other subscriptions a little bit more time to receive a value
        boolean providesValue = dataCombining.instructions()
                .stream()
                .map(Instruction::dataIdentifierReference)
                .filter(Objects::nonNull)
                .anyMatch(ref -> ref.equals(trigger));
        subscribe(trigger, true, providesValue);
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

        valuesSnapshot.forEach((propertyName, propertyValue) ->
                inputValuesAsDictObject.set(propertyName, propertyValue.getJsonNode()));

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
    public final class Value {
        @Nullable RawValue value;
        @Nullable JsonNode jsonNode;

        public Value(final @NotNull RawValue value) {
            this.value = value;
            this.jsonNode = null;
        }

        public JsonNode getJsonNode() {
            if (jsonNode == null) {
                String propertyValue = value.toString();
                try {
                    jsonNode = mapper.readTree(value.toString());
                } catch (final IOException e) {
                    log.warn("Exception during json parsing of datapoint '{}'", propertyValue);
                    throw new RuntimeException(e);
                }
                value = null;
            }
            return jsonNode;
        }
    }

    public interface RawValue {
        @NotNull String toString();
    }

    public static final class RawValueTag implements RawValue {
        DataPoint dataPoint;

        RawValueTag(final DataPoint dataPoint) {
            this.dataPoint = dataPoint;
        }

        @Override
        public @NotNull String toString() {
            return dataPoint.getTagValue().toString();
        }
    }

    public static final class RawValueTopicFilter implements RawValue {
        byte[] payload;

        RawValueTopicFilter(final byte[] payload) {
            this.payload = payload;
        }

        @Override
        public @NotNull String toString() {
            return new String(payload, StandardCharsets.UTF_8);
        }
    }

    /// `subscribe` subscribes the input (tag or topic filter).
    /// It does this by creating either a `InternalSubscriptionTag` or `InternalSubscriptionTopicFilter`.
    /// It remembers all subscriptions in `subscriptions`, so that we can unsubscribe them again on `close()`.
    public void subscribe(
            final @NotNull DataIdentifierReference ref,
            final boolean isTrigger,
            final boolean providesValue) {
        log.debug("Starting {} consumer for {}", ref.type(), ref.id());
        switch (ref.type()) {
            case TAG -> {
                subscriptions.add(new InternalSubscriptionTag(ref, isTrigger, providesValue));
            }
            case TOPIC_FILTER -> {
                subscriptions.add(new InternalSubscriptionTopicFilter(ref, isTrigger, providesValue));
            }
            case PULSE_ASSET -> {
                log.error("Pulse Assets shouldn't be input to data combining in Edge {}", ref.id());
                throw new RuntimeException();
            }
        }
    }

    public interface InternalSubscription {
        void unSubscribe();
    }

    /// Tags are subscribed to the `TagManager` with a `TagConsumer` and its `accept()` method.
    public final class InternalSubscriptionTag implements InternalSubscription {
        private final TagConsumer consumer;

        public InternalSubscriptionTag(
                final @NotNull DataIdentifierReference ref,
                final boolean isTrigger,
                final boolean providesValue) {

            this.consumer = new TagConsumer() {
                @Override
                public @NotNull String getTagName() {
                    /// TODO(mschoene) this must become `sanitized(ref)`!
                    /// so that `tagManager` knows which tag is meant (if there are multiple with the same tagname)
                    /// I presume such a change has been made to tag manager in Sam's epic
                    return ref.id();
                }

                // TODO(mschoene) I'm not sure that the TagConsumer should get a scope?
                // I think it should return the ref, and then the ref has one way to deterministically serialize itself
                public String getScope() {
                    return ref.scope();
                }

                @Override
                public void accept(final @NotNull List<DataPoint> dataPoints) {
                    if (providesValue && !dataPoints.isEmpty()) {
                        values.put(sanitize(ref), new Value(new RawValueTag(dataPoints.getLast())));
                    }
                    if (isTrigger) {
                        assembleAndPublish();
                    }
                }
            };

            tagManager.addConsumer(consumer);
        }

        public void unSubscribe() {
            tagManager.removeConsumer(consumer);
        }
    }

    /// `TopicFilters` are subscribed to the `TopicTree` with a `QueueConsumer` and its `process()` method.
    public final class InternalSubscriptionTopicFilter implements InternalSubscription {
        private final @NotNull String subscriber;
        private final @NotNull String topicFilter;
        private final @NotNull String sharedName;
        private final @NotNull QueueConsumer consumer;

        InternalSubscriptionTopicFilter(
                final @NotNull DataIdentifierReference ref,
                final boolean isTrigger,
                final boolean providesValue) {
            this.subscriber = dataCombining.id().toString() + "#";
            this.topicFilter = ref.id();
            this.sharedName = dataCombining.id().toString();
            String queueId = dataCombining.id().toString() + "/" + topicFilter;

            /// TODO(mschoene) figure out, how the subscription of topic filters actually works?
            /// I'm puzzled, because the consumer is not **directly** added to the topic tree
            /// So I presume that there must be some connection via the `subscriber` and `queueId`?
            /// And then what is the `sharedName` for?
            localTopicTree.addTopic(subscriber,
                    new Topic(topicFilter, QoS.EXACTLY_ONCE, false, true),
                    SubscriptionFlag.getDefaultFlags(true, true, false),
                    sharedName);

            this.consumer = new QueueConsumer(clientQueuePersistence, queueId, singleWriterService) {
                @Override
                public void process(final @NotNull PUBLISH message) {
                    if (providesValue) {
                        values.put(sanitize(ref), new Value(new RawValueTopicFilter(message.getPayload())));
                    }
                    if (isTrigger) {
                        assembleAndPublish();
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
