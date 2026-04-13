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

import static com.hivemq.mqtt.message.publish.PUBLISH.MESSAGE_EXPIRY_INTERVAL_NOT_SET;
import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.combining.mapping.DataCombiningTransformationService;
import com.hivemq.combining.model.DataCombining;
import com.hivemq.combining.model.DataIdentifierReference;
import com.hivemq.configuration.HivemqId;
import com.hivemq.datapoint.DataPointWithMetadata;
import com.hivemq.edge.modules.adapters.data.TagManager;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriptionFlag;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.mappings.fieldmapping.Instruction;
import com.hivemq.protocols.northbound.SingleTagConsumer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// region `DataCombiningRuntime` manages a **data combiner mapping** (aka "datacombing") - class and instance vars
// =====================================================================================================================
// It assembles values from inputs, which can be tags or topic-filters, into a destination message.
// `start()` subscribes to all the inputs (tags and topic-filters), including the trigger (aka primary).
// When values arrive (via `accept()` or `process()`), they are stored in the concurrent hashmap `values`.
// When the trigger arrives, the `assembleAndPublish()` method is called which assembles all inputs into an object
// and calls the `dataCombiningTransformationService` to turn that into a message and publish that to the target topic.
public class DataCombiningRuntime {

    private static final Logger log = LoggerFactory.getLogger(DataCombiningRuntime.class);

    private final @NotNull DataCombining dataCombining;

    // `tagManager` manages the subscriptions for tags
    private final @NotNull TagManager tagManager;

    // `localTopicTree` enqueues published messages matching a topic filter to `clientQueuePersistence`
    // `singleWriterService` is the concurrency mechanism to serialize all access for `clientQueuePersistence`
    private final @NotNull LocalTopicTree localTopicTree;
    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull SingleWriterService singleWriterService;

    // `transformationService` does the real magic applying all the instructions and creating the proper destination
    // There exist two implementations of the transformation service:
    // VanillaDataCombiningTransformationService, Open-source/no DataHub, applies field mappings using JSONPath
    // DataCombiningTransformationServiceImpl, Commercial DataHub module, compiles and runs a JavaScript transformation
    // The `CombiningModule` decides which transformation service to use based on the license information
    // and then injects it into `DataCombingingPublishService` and `DataCombingRuntimeFactory`
    private final @NotNull DataCombiningTransformationService transformationService;
    private volatile boolean scriptAdded = false;

    // `HivemqId` generates unique ids for the messages, used when enqueueing messages
    private final @NotNull HivemqId hiveMQId;

    // subscriptions remembers all the subscriptions for tags and topic filters, so we can unsubscribe them again
    private final @NotNull List<InternalSubscription> subscriptions;

    // `values` is the hash map where we store values, it must be a concurrent map since values may arrive concurrently
    private final @NotNull ConcurrentHashMap<DataIdentifierReference, Value> values;

    // `mapper` does all the necessary JSON magic for us
    private final @NotNull ObjectMapper mapper;

    public DataCombiningRuntime(
            final @NotNull DataCombining dataCombining,
            final @NotNull LocalTopicTree localTopicTree,
            final @NotNull TagManager tagManager,
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull SingleWriterService singleWriterService,
            final @NotNull DataCombiningTransformationService transformationService,
            final @NotNull HivemqId hiveMQId) {
        this.dataCombining = dataCombining;
        this.localTopicTree = localTopicTree;
        this.tagManager = tagManager;
        this.clientQueuePersistence = clientQueuePersistence;
        this.singleWriterService = singleWriterService;
        this.transformationService = transformationService;
        this.hiveMQId = hiveMQId;

        this.subscriptions = new ArrayList<>();
        this.values = new ConcurrentHashMap<>();
        this.mapper = new ObjectMapper();
    }

    // endregion

    // region `starts()` collects all the inputs and subscribes to them all, `stop()` unsubscribes again
    // =================================================================================================================
    // note that the mappings `sources.tag` and `sources.topics` are not used, we fetch everything from the instructions
    public void start() {
        log.debug("Starting data combining {}", dataCombining.id());

        DataIdentifierReference trigger = dataCombining.sources().primaryReference();
        if (trigger == null) {
            log.warn("Primary reference of data combining {} is null", dataCombining.id());
            return;
        }

        transformationService.addScriptForDataCombining(dataCombining);
        scriptAdded = true;

        // collect subscriptions for all the distinct inputs, except for the trigger
        subscriptions.addAll(dataCombining.instructions().stream()
                .map(Instruction::dataIdentifierReference)
                .filter(Objects::nonNull)
                .filter(ref -> !ref.equals(trigger))
                .distinct()
                .map(ref -> internalSubscription(ref, false, true))
                .toList());

        // collect a subscription for the trigger, check whether it is used in an instructions and so we need its value
        boolean isValueUsed = dataCombining.instructions().stream()
                .map(Instruction::dataIdentifierReference)
                .filter(Objects::nonNull)
                .anyMatch(ref -> ref.equals(trigger));
        subscriptions.add(internalSubscription(trigger, true, isValueUsed));

        // now subscribe to them all
        subscriptions.stream().filter(Objects::nonNull).forEach(InternalSubscription::subscribe);
    }

    // `stop()` unsubscribes all `subscriptions` again
    public void stop() {
        log.debug("Stoping data combining {}", dataCombining.id());

        subscriptions.stream().filter(Objects::nonNull).forEach(InternalSubscription::unSubscribe);
        subscriptions.clear();
        // deliberately not clearing `values` so that a stop/restart finds the last known values

        if (scriptAdded) {
            transformationService.removeScriptForDataCombining(dataCombining);
            scriptAdded = false;
        }
    }

    // endregion

    // region `assembleAndPublish()` assembles the values and publishes a new, combined message
    // =================================================================================================================
    // this is what it really is all about, assembling values and publishing a new, combined message
    // There is an awful lot of copying and JSON marshaling and un-marshaling going on:
    // 1. it makes a copy of all the values to get a snapshot
    // 2. it reads the values (getTagValue4Combiner), combining the DataPoint value into a JSON object
    // 3. it stuffs all the values into a JSON dictionary object
    // 4. publishing service creates a full MQTT5 message with that JSON dictionary object as payload
    // 5. transformation then goes over that message and creates the proper destination message
    public void assembleAndPublish() {
        try {
            assembleAndPublishInternal();
        } catch (final Exception e) {
            log.warn("Unable to process data combining '{}'", dataCombining.id(), e);
        }
    }

    private void assembleAndPublishInternal() {
        log.debug("Triggering data combining {}", dataCombining.id());
        final ObjectNode inputValuesAsDictObject = mapper.createObjectNode();
        final var valuesSnapshot = Map.copyOf(values);

        valuesSnapshot.forEach((dir, propertyValue) ->
                inputValuesAsDictObject.set(dir.toFullyQualifiedName(), propertyValue.getTagValue4Combiner()));

        PUBLISH inputValuesAsMQTTMessage = new PUBLISHFactory.Mqtt5Builder()
                .withHivemqId(hiveMQId.get())
                .withQoS(QoS.AT_LEAST_ONCE)
                .withOnwardQos(QoS.AT_LEAST_ONCE)
                .withRetain(false) // this message is not retained
                .withTopic(dataCombining.destination().topic())
                .withMessageExpiryInterval(MESSAGE_EXPIRY_INTERVAL_NOT_SET)
                .withResponseTopic(null)
                .withCorrelationData(null)
                .withPayload(inputValuesAsDictObject.toString().getBytes(StandardCharsets.UTF_8))
                .withContentType(null)
                .withPayloadFormatIndicator(null)
                .withUserProperties(Mqtt5UserProperties.of())
                .build();

        transformationService
                .applyMappings(inputValuesAsMQTTMessage, dataCombining)
                .exceptionally(e -> {
                    log.warn("Unable to apply mappings for data combining '{}'", dataCombining.id(), e);
                    return null;
                });
    }

    // endregion

    // region `Value` represents values to be used in the combining (`assempleAndPublish`) as JsonNodes.
    // =================================================================================================================
    // It is created holding the `RawValue` (either tag value or topic-filter payload).
    // It converts the raw value to a `JsonNode` when requested (lazily) and caches the result of the conversion.
    // That way we only convert raw values when needed,
    // so we don't convert raw values that are overwritten by other raw values before a trigger arrives.
    // And we also don't convert a raw value multiple times,
    // so we convert raw values that don't change while multiple triggers arrive only once.
    // There are two concrete implementations for `RawValue`: one for tags and one for topic-filters.
    public static final class Value {
        volatile @Nullable RawValue value;
        volatile @Nullable JsonNode jsonNode;

        public Value(final @NotNull RawValue value) {
            this.value = value;
            this.jsonNode = null;
        }

        public @NotNull JsonNode getTagValue4Combiner() {
            final RawValue rawValue = value;
            if (rawValue != null) {
                jsonNode = rawValue.getTagValue4Combiner();
                value = null;
            }
            requireNonNull(jsonNode);
            return jsonNode;
        }
    }

    public interface RawValue {
        @NotNull
        JsonNode getTagValue4Combiner();
    }

    public final class RawValueTag implements RawValue {
        DataPoint dataPoint;

        RawValueTag(final DataPoint dataPoint) {
            this.dataPoint = dataPoint;
        }

        @Override
        public @NotNull JsonNode getTagValue4Combiner() {
            // DataPointWithMetadata wraps the tag value in {"value": <actual value>}, matching what instructions
            // expect (source "$.value") and what the DataHub commercial module expects for tag data.
            if (dataPoint instanceof DataPointWithMetadata dpMeta) {
                final ObjectNode wrapper = mapper.createObjectNode();
                wrapper.set("value", dpMeta.getTagValue());
                return wrapper;
            }
            final Object tagValue = dataPoint.getTagValue();
            if (tagValue == null) {
                log.warn("Tag value of datapoint '{}' is null", dataPoint.getTagName());
                throw new RuntimeException("Tag value of datapoint '" + dataPoint.getTagName() + "' is null");
            }
            try {
                return mapper.readTree(tagValue.toString());
            } catch (final IOException e) {
                log.warn(
                        "Exception during json parsing of datapoint '{}' for tag '{}'",
                        tagValue,
                        dataPoint.getTagName());
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

    // endregion

    // region `internalSubscription`, return a `InternalSubscription` for the input (tag or topic filter)
    // =================================================================================================================
    // `internalSubscription` returns an initially inactive subscription for the input (tag or topic filter),
    // by creating either an `InternalSubscriptionTag` or an `InternalSubscriptionTopicFilter`
    public @Nullable InternalSubscription internalSubscription(
            final @NotNull DataIdentifierReference ref, final boolean isTrigger, final boolean isValueUsed) {
        log.debug("Creating {} subscription for {}", ref.type(), ref);
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

    // endregion

    // region `InternalSubscriptionTag`, subscription for a tag to `TagManager`
    // =================================================================================================================
    // Tags are subscribed to the `TagManager` with a `SingleTagConsumer` and its `accept()` method.
    public final class InternalSubscriptionTag implements InternalSubscription {
        private final SingleTagConsumer consumer;

        public InternalSubscriptionTag(
                final @NotNull DataIdentifierReference ref, final boolean isTrigger, final boolean isValueUsed) {

            this.consumer = new SingleTagConsumer() {
                @Override
                public @NotNull String getTagName() {
                    return ref.id();
                }

                @Override
                public @Nullable String getScope() {
                    return ref.scope();
                }

                @Override
                public void accept(final @NotNull DataPoint dataPoint) {
                    if (isValueUsed) {
                        values.put(ref, new Value(new RawValueTag(dataPoint)));
                    }
                    if (isTrigger) {
                        assembleAndPublish();
                    }
                }
            };
        }

        @Override
        public void subscribe() {
            tagManager.addConsumer(consumer);
        }

        @Override
        public void unSubscribe() {
            tagManager.removeConsumer(consumer);
        }
    }

    // endregion

    // region `InternalSubscriptionTopicFilter` subscription for a topicfilter with a `QueueConsumer`
    // =================================================================================================================
    // `TopicFilters` are subscribed to the `TopicTree` with a `QueueConsumer` and its `process()` method.
    public final class InternalSubscriptionTopicFilter implements InternalSubscription {
        private final @NotNull String topicFilter;
        private final @NotNull String sharedName;
        private final @NotNull String clientId;
        private final @NotNull QueueConsumer consumer;

        InternalSubscriptionTopicFilter(
                final @NotNull DataIdentifierReference ref, final boolean isTrigger, final boolean isValueUsed) {

            // Note is that the steps here and in `subscribe() must be done exactly in this order and with these values.
            // It is a problem that `DataCombiningRuntime` needs to know so much about how to subscribe a topic filter.
            // The whole sequence should have been combined and encapsulated somewhere central (project Sihltal).

            // the topic filter that we subscribe to
            this.topicFilter = ref.id();

            // The first thing to note is that the subscription must be done using a shared subscription,
            // because non-shared subscriptions are supposed to be "real" MQTT client subscriptions
            // requiring a session and a netty network connection. That is of course totally stupid!
            // Logically this subscription is of course NOT shared, there can never be multiple clients for it.
            // But this is why there is a `sharedName` attribute for this subscription.

            // What is even more disturbing is that this only works because of a bug:
            // Assume that this data combiner mapping had two topic filter inputs
            // with overlapping filters (e.g. `a/+/c` and `a/b/+`)
            // and thus two `InternalSubscriptionTopicFilter` objects with two `process` callback,
            // then a message to `a/b/c` should actually be deduplicated and
            // provided only once to the (single) client of this shared subscription
            // and so only one `process` callback should be called.
            // This deduplication does work for non-shared subscriptions,
            // but it does not work for shared subscriptions.
            // All of this shall be clean up in project Sihltal.

            // `sharedName` must be an id for the (shared) subscription to the topic tree,
            // with the intent that publish can distinguish different subscribers to the same topic filter;
            // the only requirement for this id is uniqueness, so we simply use the combiner mapping UUID.
            this.sharedName = dataCombining.id().toString();

            // `clientId` must be an id for the client of the subscription to the topic tree,
            // with the intent that we can unsubscribe this exact subscription again later (in `close`);
            // the only requirement for this id is also uniqueness, so we use the combiner mapping UUID again
            // and append `#` to avoid any risk of clashes with `sharedName` should they ever be used in the same hash.
            // As mentioned above this is the only client in this shared subscription.
            this.clientId = sharedName + "#";

            // The name of the queue to which messages matching `topicFilter` are enqueued for the shared subscription
            // `queueId` is `sharedName/topicFilter` (determined by the inner workings of the topic tree).
            String queueId = sharedName + "/" + topicFilter;

            // Create the queue consumer with a callback for the same slot (initially inactive and not on the queue).
            this.consumer = new QueueConsumer(clientQueuePersistence, queueId, singleWriterService) {
                @Override
                public void process(final @NotNull PUBLISH message) {
                    if (isValueUsed && message.getPayload() != null) {
                        values.put(ref, new Value(new RawValueTopicFilter(message.getPayload())));
                    }
                    if (isTrigger) {
                        assembleAndPublish();
                    }
                }
            };
        }

        @Override
        public void subscribe() {

            // add the consumer to the queue, start listening for messages enqueued to this slot
            consumer.start();

            // Add the topic filter to the `localTopicTree`, so that when a message is published to `topicFilter`
            // it will be enqueued to `clientPersistenceQueue` in the slot `sharedName/topicFilter`.
            // First adding the consumer and then the topic filter to the topic tree means that a consumer already
            // exists when the messages are being enqueued, though there is an initial `submitPoll` to also handle it.
            localTopicTree.addTopic(
                    clientId,
                    new Topic(topicFilter, QoS.EXACTLY_ONCE, false, true),
                    SubscriptionFlag.getDefaultFlags(true, true, false),
                    sharedName);
        }

        @Override
        public void unSubscribe() {
            localTopicTree.removeSubscriber(clientId, topicFilter, sharedName);
            consumer.close();
        }
    }

    // endregion
}
