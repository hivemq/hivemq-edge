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
package com.hivemq.persistence.local.memory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.ImmutableIntArray;
import com.hivemq.annotations.ExecuteInSingleWriter;
import com.hivemq.configuration.service.InternalConfigurationService;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.configuration.service.MqttConfigurationService.QueuedMessagesStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.hivemq.metrics.HiveMQMetrics;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.persistence.clientqueue.ClientQueueLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.MemoryEstimator;
import com.hivemq.util.Strings;
import com.hivemq.util.ThreadPreConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.configuration.service.InternalConfigurations.QOS_0_MEMORY_HARD_LIMIT_DIVISOR;
import static com.hivemq.util.ThreadPreConditions.SINGLE_WRITER_THREAD_PREFIX;

/**
 * @author Florian Limpöck
 * @author Silvio Giebl
 */
@Singleton
public class ClientQueueMemoryLocalPersistence implements ClientQueueLocalPersistence {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ClientQueueMemoryLocalPersistence.class);

    private static final int NO_PACKET_ID = 0;

    private final @NotNull Map<String, Messages> @NotNull [] buckets;
    private final @NotNull Map<String, Messages> @NotNull [] sharedBuckets;
    private final @NotNull InternalConfigurationService internalConfigurationService;

    private static class Messages {
        final @NotNull LinkedList<MessageWithID> qos1Or2Messages = new LinkedList<>();
        final @NotNull LinkedList<PublishWithRetained> qos0Messages = new LinkedList<>();
        int retainedQos1Or2Messages = 0;
        long qos0Memory = 0;
    }

    private final @NotNull PublishPayloadPersistence payloadPersistence;
    private final @NotNull MessageDroppedService messageDroppedService;

    private final long qos0MemoryLimit;
    private final int qos0ClientMemoryLimit;
    private final int retainedMessageMax;

    private final @NotNull AtomicLong qos0MessagesMemory;
    private final @NotNull AtomicLong totalMemorySize;

    @Inject
    ClientQueueMemoryLocalPersistence(
            final @NotNull PublishPayloadPersistence payloadPersistence,
            final @NotNull MessageDroppedService messageDroppedService,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull InternalConfigurationService internalConfigurationService) {
        this.internalConfigurationService = internalConfigurationService;

        final int bucketCount =
                internalConfigurationService.getInteger(InternalConfigurations.PERSISTENCE_BUCKET_COUNT);
        //noinspection unchecked
        buckets = new HashMap[bucketCount];
        //noinspection unchecked
        sharedBuckets = new HashMap[bucketCount];
        for (int i = 0; i < bucketCount; i++) {
            buckets[i] = new HashMap<>();
            sharedBuckets[i] = new HashMap<>();
        }

        this.payloadPersistence = payloadPersistence;
        this.messageDroppedService = messageDroppedService;

        qos0MemoryLimit = getQos0MemoryLimit();
        qos0ClientMemoryLimit = InternalConfigurations.QOS_0_MEMORY_LIMIT_PER_CLIENT_BYTES.get();
        retainedMessageMax = InternalConfigurations.RETAINED_MESSAGE_QUEUE_SIZE.get();

        qos0MessagesMemory = new AtomicLong();
        totalMemorySize = new AtomicLong();

        metricRegistry.register(HiveMQMetrics.QUEUED_MESSAGES_MEMORY_PERSISTENCE_TOTAL_SIZE.name(),
                (Gauge<Long>) totalMemorySize::get);

    }

    private long getQos0MemoryLimit() {
        final long maxHeap = Runtime.getRuntime().maxMemory();
        final long maxHardLimit;

        final int hardLimitDivisor = QOS_0_MEMORY_HARD_LIMIT_DIVISOR.get();

        if (hardLimitDivisor < 1) {
            //fallback to default if config failed
            maxHardLimit = maxHeap / 4;
        } else {
            maxHardLimit = maxHeap / hardLimitDivisor;
        }
        log.debug("{} allocated for qos 0 inflight messages", Strings.toHumanReadableFormat(maxHardLimit));
        return maxHardLimit;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ExecuteInSingleWriter
    public void add(
            final @NotNull String queueId,
            final boolean shared,
            final @NotNull PUBLISH publish,
            final long max,
            final @NotNull QueuedMessagesStrategy strategy,
            final boolean retained,
            final int bucketIndex) {

        checkNotNull(queueId, "Queue ID must not be null");
        checkNotNull(publish, "Publish must not be null");
        checkNotNull(strategy, "Strategy must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        add(queueId, shared, List.of(publish), max, strategy, retained, bucketIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ExecuteInSingleWriter
    public void add(
            final @NotNull String queueId,
            final boolean shared,
            final @NotNull List<PUBLISH> publishes,
            final long max,
            final @NotNull QueuedMessagesStrategy strategy,
            final boolean retained,
            final int bucketIndex) {

        checkNotNull(queueId, "Queue ID must not be null");
        checkNotNull(publishes, "Publishes must not be null");
        checkNotNull(strategy, "Strategy must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Map<String, Messages> bucket = shared ? sharedBuckets[bucketIndex] : buckets[bucketIndex];
        final Messages messages = bucket.computeIfAbsent(queueId, s -> new Messages());

        for (final PUBLISH publish : publishes) {
            final PublishWithRetained publishWithRetained = new PublishWithRetained(publish, retained);
            if (publish.getQoS() == QoS.AT_MOST_ONCE) {
                addQos0Publish(queueId, shared, messages, publishWithRetained);
            } else {
                final int qos1And2QueueSize = messages.qos1Or2Messages.size() - messages.retainedQos1Or2Messages;
                if ((qos1And2QueueSize >= max) && !retained) {
                    if (strategy == QueuedMessagesStrategy.DISCARD) {
                        logAndDecrementPayloadReference(publish, shared, queueId);
                        continue;
                    } else {
                        final boolean discarded = discardOldest(queueId, shared, messages, false);
                        if (!discarded) {
                            //discard this message if no old could be discarded
                            logAndDecrementPayloadReference(publish, shared, queueId);
                            continue;
                        }
                    }
                } else if ((messages.retainedQos1Or2Messages >= retainedMessageMax) && retained) {
                    if (strategy == QueuedMessagesStrategy.DISCARD) {
                        logAndDecrementPayloadReference(publish, shared, queueId);
                        continue;
                    } else {
                        final boolean discarded = discardOldest(queueId, shared, messages, true);
                        if (!discarded) {
                            //discard this message if no old could be discarded
                            logAndDecrementPayloadReference(publish, shared, queueId);
                            continue;
                        }
                    }
                } else {
                    if (retained) {
                        messages.retainedQos1Or2Messages++;
                    }
                }

                publishWithRetained.setPacketIdentifier(NO_PACKET_ID);
                messages.qos1Or2Messages.add(publishWithRetained);
                increaseMessagesMemory(publishWithRetained.getEstimatedSize());
            }
        }
    }

    private void addQos0Publish(
            final @NotNull String queueId,
            final boolean shared,
            final @NotNull Messages messages,
            final @NotNull PublishWithRetained publishWithRetained) {

        final long currentQos0MessagesMemory = qos0MessagesMemory.get();
        if (currentQos0MessagesMemory >= qos0MemoryLimit) {
            if (shared) {
                messageDroppedService.qos0MemoryExceededShared(queueId,
                        publishWithRetained.getTopic(),
                        0,
                        currentQos0MessagesMemory,
                        qos0MemoryLimit);
            } else {
                messageDroppedService.qos0MemoryExceeded(queueId,
                        publishWithRetained.getTopic(),
                        0,
                        currentQos0MessagesMemory,
                        qos0MemoryLimit);
            }
            return;
        }

        if (!shared) {
            if (messages.qos0Memory >= qos0ClientMemoryLimit) {
                messageDroppedService.qos0MemoryExceeded(queueId,
                        publishWithRetained.getTopic(),
                        0,
                        messages.qos0Memory,
                        qos0ClientMemoryLimit);
                return;
            }
        }

        messages.qos0Messages.add(publishWithRetained);
        increaseQos0MessagesMemory(publishWithRetained.getEstimatedSize());
        increaseClientQos0MessagesMemory(messages, publishWithRetained.getEstimatedSize());
        increaseMessagesMemory(publishWithRetained.getEstimatedSize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ExecuteInSingleWriter
    public @NotNull ImmutableList<PUBLISH> readNew(
            final @NotNull String queueId,
            final boolean shared,
            final @NotNull ImmutableIntArray packetIds,
            final long bytesLimit,
            final int bucketIndex) {

        checkNotNull(queueId, "Queue ID must not be null");
        checkNotNull(packetIds, "Packet IDs must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Map<String, Messages> bucket = shared ? sharedBuckets[bucketIndex] : buckets[bucketIndex];
        final Messages messages = bucket.get(queueId);
        if (messages == null) {
            return ImmutableList.of();
        }

        // In case there are only qos 0 messages
        if (messages.qos1Or2Messages.size() == 0) {
            return getQos0Publishes(messages, packetIds, bytesLimit);
        }

        final int countLimit = packetIds.length();
        int messageCount = 0;
        int packetIdIndex = 0;
        int bytes = 0;
        final ImmutableList.Builder<PUBLISH> publishes = ImmutableList.builder();

        final Iterator<MessageWithID> iterator = messages.qos1Or2Messages.iterator();
        while (iterator.hasNext()) {
            final MessageWithID messageWithID = iterator.next();
            if (!(messageWithID instanceof PublishWithRetained)) {
                continue;
            }
            final PublishWithRetained publishWithRetained = (PublishWithRetained) messageWithID;
            if (publishWithRetained.getPacketIdentifier() != NO_PACKET_ID) {
                //already inflight
                continue;
            }

            if (publishWithRetained.isExpired()) {
                iterator.remove();
                // the payloads for QoS-0 messages are not extracted and their reference count is not incremented.
                // therefor it must not be decremented
                if (publishWithRetained.getQoS() != QoS.AT_MOST_ONCE) {
                    payloadPersistence.decrementReferenceCounter(publishWithRetained.getPublishId());
                }
                if (publishWithRetained.retained) {
                    messages.retainedQos1Or2Messages--;
                }
                increaseMessagesMemory(-publishWithRetained.getEstimatedSize());
                //do not return here, because we could have a QoS 0 message left
            } else {

                final int packetId = packetIds.get(packetIdIndex);
                publishWithRetained.setPacketIdentifier(packetId);
                publishes.add(publishWithRetained);
                packetIdIndex++;
                messageCount++;
                bytes += publishWithRetained.getEstimatedSizeInMemory();
                if ((messageCount == countLimit) || (bytes > bytesLimit)) {
                    break;
                }
            }

            // poll a qos 0 message
            final PUBLISH qos0Publish = pollQos0Message(messages);
            if ((qos0Publish != null) && !qos0Publish.isExpired()) {
                publishes.add(qos0Publish);
                messageCount++;
                bytes += qos0Publish.getEstimatedSizeInMemory();
            }
            if ((messageCount == countLimit) || (bytes > bytesLimit)) {
                break;
            }
        }
        return publishes.build();
    }

    @Override
    public @NotNull ImmutableList<PUBLISH> peek(
            final @NotNull String queueId,
            final boolean shared,
            final long bytesLimit,
            final int maxMessages,
            final int bucketIndex) {
        checkNotNull(queueId, "Queue ID must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);
        final Map<String, Messages> bucket = shared ? sharedBuckets[bucketIndex] : buckets[bucketIndex];
        final Messages messages = bucket.get(queueId);
        if (messages == null) {
            return ImmutableList.of();
        }

        int messageCount = 0;
        int bytes = 0;
        final ImmutableList.Builder<PUBLISH> publishes = ImmutableList.builder();

        // Qos 1 and 2 has prio
        for (int index = 0; index < messages.qos1Or2Messages.size() && messageCount < maxMessages; index++) {
            final MessageWithID messageWithID = messages.qos1Or2Messages.get(index);
            if (!(messageWithID instanceof PublishWithRetained)) {
                continue;
            }
            final PublishWithRetained publishWithRetained = (PublishWithRetained) messageWithID;
            if (publishWithRetained.getPacketIdentifier() != NO_PACKET_ID) {
                //already inflight
                continue;
            }

            // check for expiration, but do not modify the queue
            if (!publishWithRetained.isExpired()) {
                bytes += publishWithRetained.getEstimatedSizeInMemory();
                // check if adding the message would exceed the byte limit
                if ((bytes > bytesLimit)) {
                    break;
                } else {
                    publishes.add(publishWithRetained);
                    messageCount++;
                }
            }
        }

        // read as many qos0 as possible
        for (int index = 0; messageCount < maxMessages && index < messages.qos0Messages.size(); index++) {
            final PublishWithRetained publishWithRetained = messages.qos0Messages.get(index);
            final int estimatedSize = publishWithRetained.getEstimatedSize();
            bytes += estimatedSize;
            // check if we would exceed the byte limit.
            if (bytes <= bytesLimit) {
                publishes.add(publishWithRetained);
                messageCount++;
            } else {
                break;
            }
        }
        return publishes.build();
    }

    private @NotNull ImmutableList<PUBLISH> getQos0Publishes(
            final @NotNull Messages messages, final @NotNull ImmutableIntArray packetIds, final long bytesLimit) {

        final ImmutableList.Builder<PUBLISH> publishes = ImmutableList.builder();
        int qos0MessagesFound = 0;
        int qos0Bytes = 0;
        while (qos0MessagesFound < packetIds.length() && bytesLimit > qos0Bytes) {
            final PUBLISH qos0Publish = pollQos0Message(messages);
            if (qos0Publish == null) {
                break;
            }
            if (!qos0Publish.isExpired()) {
                publishes.add(qos0Publish);
                qos0MessagesFound++;
                qos0Bytes += qos0Publish.getEstimatedSizeInMemory();
            }
        }

        return publishes.build();
    }

    private @Nullable PUBLISH pollQos0Message(final @NotNull Messages messages) {
        final PublishWithRetained publishWithRetained = messages.qos0Messages.poll();
        if (publishWithRetained == null) {
            return null;
        }
        final int estimatedSize = publishWithRetained.getEstimatedSize();
        increaseQos0MessagesMemory(-estimatedSize);
        increaseClientQos0MessagesMemory(messages, -estimatedSize);
        increaseMessagesMemory(-estimatedSize);
        // the payloads for QoS-0 messages are not extracted and their reference count is not incremented.
        // therefor it must not be decremented
        if (publishWithRetained.getQoS() != QoS.AT_MOST_ONCE) {
            payloadPersistence.decrementReferenceCounter(publishWithRetained.getPublishId());
        }
        return publishWithRetained;
    }

    @Override
    @ExecuteInSingleWriter
    public @NotNull ImmutableList<MessageWithID> readInflight(
            final @NotNull String queueId,
            final boolean shared,
            final int batchSize,
            final long bytesLimit,
            final int bucketIndex) {

        checkNotNull(queueId, "client id must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Map<String, Messages> bucket = shared ? sharedBuckets[bucketIndex] : buckets[bucketIndex];
        final Messages messages = bucket.get(queueId);
        if (messages == null) {
            return ImmutableList.of();
        }

        int messageCount = 0;
        int bytes = 0;
        final ImmutableList.Builder<MessageWithID> publishes = ImmutableList.builder();

        for (final MessageWithID messageWithID : messages.qos1Or2Messages) {
            // Stop at first non inflight message
            // This works because in-flight messages are always first in the queue
            if (messageWithID.getPacketIdentifier() == NO_PACKET_ID) {
                break;
            }
            publishes.add(messageWithID);
            messageCount++;

            if (messageWithID instanceof PublishWithRetained) {
                final PublishWithRetained publishWithRetained = (PublishWithRetained) messageWithID;
                bytes += publishWithRetained.getEstimatedSizeInMemory();
                publishWithRetained.setDuplicateDelivery(true);
            }

            if ((messageCount == batchSize) || (bytes > bytesLimit)) {
                break;
            }
        }
        return publishes.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ExecuteInSingleWriter
    public @Nullable String replace(
            final @NotNull String queueId, final @NotNull PUBREL pubrel, final int bucketIndex) {

        checkNotNull(queueId, "client id must not be null");
        checkNotNull(pubrel, "pubrel must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Map<String, Messages> bucket = buckets[bucketIndex];
        final Messages messages = bucket.get(queueId);
        if (messages == null) {
            return null;
        }

        boolean packetIdFound = false;
        String replacedId = null;
        boolean retained = false;

        int messageIndexInQueue = -1;

        for (final MessageWithID messageWithID : messages.qos1Or2Messages) {
            messageIndexInQueue++;
            final int packetId = messageWithID.getPacketIdentifier();
            if (packetId == NO_PACKET_ID) {
                break;
            }
            if (packetId == pubrel.getPacketIdentifier()) {
                packetIdFound = true;
                if (messageWithID instanceof PublishWithRetained) {
                    final PublishWithRetained publish = (PublishWithRetained) messageWithID;
                    retained = publish.retained;
                    // the payloads for QoS-0 messages are not extracted and their reference count is not incremented.
                    // therefor it must not be decremented
                    if (publish.getQoS() != QoS.AT_MOST_ONCE) {
                        payloadPersistence.decrementReferenceCounter(publish.getPublishId());
                    }
                    increaseMessagesMemory(-publish.getEstimatedSize());
                    pubrel.setMessageExpiryInterval(publish.getMessageExpiryInterval());
                    pubrel.setPublishTimestamp(publish.getTimestamp());
                    replacedId = publish.getUniqueId();
                } else if (messageWithID instanceof PubrelWithRetained) {
                    final PubrelWithRetained pubrelWithRetained = (PubrelWithRetained) messageWithID;
                    pubrel.setMessageExpiryInterval(pubrelWithRetained.getMessageExpiryInterval());
                    pubrel.setPublishTimestamp(pubrelWithRetained.getPublishTimestamp());
                    retained = pubrelWithRetained.retained;
                }
                break;
            }
        }
        final PubrelWithRetained pubrelWithRetained = new PubrelWithRetained(pubrel, retained);
        if (packetIdFound) {
            messages.qos1Or2Messages.set(messageIndexInQueue, pubrelWithRetained);
        } else {
            // Ensure unknown PUBRELs are always first in queue
            messages.qos1Or2Messages.addFirst(pubrelWithRetained);
        }
        increaseMessagesMemory(pubrelWithRetained.getEstimatedSize());
        return replacedId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ExecuteInSingleWriter
    public @Nullable String remove(final @NotNull String queueId, final int packetId, final int bucketIndex) {
        return remove(queueId, packetId, null, bucketIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ExecuteInSingleWriter
    public @Nullable String remove(
            final @NotNull String queueId, final int packetId, final @Nullable String uniqueId, final int bucketIndex) {

        checkNotNull(queueId, "client id must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Map<String, Messages> bucket = buckets[bucketIndex];
        final Messages messages = bucket.get(queueId);
        if (messages == null) {
            return null;
        }

        final Iterator<MessageWithID> iterator = messages.qos1Or2Messages.iterator();
        while (iterator.hasNext()) {
            final MessageWithID messageWithID = iterator.next();
            if (messageWithID.getPacketIdentifier() == packetId) {
                String removedId = null;
                if (messageWithID instanceof PublishWithRetained) {
                    final PublishWithRetained publish = (PublishWithRetained) messageWithID;
                    if (uniqueId != null && !uniqueId.equals(publish.getUniqueId())) {
                        break;
                    }
                    // the payloads for QoS-0 messages are not extracted and their reference count is not incremented.
                    // therefor it must not be decremented
                    if (publish.getQoS() != QoS.AT_MOST_ONCE) {
                        payloadPersistence.decrementReferenceCounter(publish.getPublishId());
                    }
                    removedId = publish.getUniqueId();
                }
                if (isRetained(messageWithID)) {
                    messages.retainedQos1Or2Messages--;
                }
                increaseMessagesMemory(-getMessageSize(messageWithID));
                iterator.remove();
                return removedId;
            }
        }
        return null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @ExecuteInSingleWriter
    public int size(final @NotNull String queueId, final boolean shared, final int bucketIndex) {
        checkNotNull(queueId, "Queue ID must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX); // QueueSizes are not thread save

        final Map<String, Messages> bucket = shared ? sharedBuckets[bucketIndex] : buckets[bucketIndex];
        final Messages messages = bucket.get(queueId);
        return (messages == null) ? 0 : (messages.qos1Or2Messages.size() + messages.qos0Messages.size());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ExecuteInSingleWriter
    public void clear(final @NotNull String queueId, final boolean shared, final int bucketIndex) {
        checkNotNull(queueId, "Queue ID must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Map<String, Messages> bucket = shared ? sharedBuckets[bucketIndex] : buckets[bucketIndex];
        final Messages messages = bucket.remove(queueId);
        if (messages == null) {
            return;
        }

        for (final MessageWithID messageWithID : messages.qos1Or2Messages) {
            if (messageWithID instanceof PublishWithRetained) {
                final PublishWithRetained publish = (PublishWithRetained) messageWithID;
                // the payloads for QoS-0 messages are not extracted and their reference count is not incremented.
                // therefor it must not be decremented
                if (publish.getQoS() != QoS.AT_MOST_ONCE) {
                    payloadPersistence.decrementReferenceCounter(publish.getPublishId());
                }
            }
            increaseMessagesMemory(-getMessageSize(messageWithID));
        }

        for (final PublishWithRetained qos0Message : messages.qos0Messages) {
            final int estimatedSize = qos0Message.getEstimatedSize();
            increaseQos0MessagesMemory(-estimatedSize);
            // increaseClientQos0MessagesMemory not necessary as messages are removed completely
            increaseMessagesMemory(-estimatedSize);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ExecuteInSingleWriter
    public void removeAllQos0Messages(final @NotNull String queueId, final boolean shared, final int bucketIndex) {
        checkNotNull(queueId, "Queue id must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Map<String, Messages> bucket = shared ? sharedBuckets[bucketIndex] : buckets[bucketIndex];
        final Messages messages = bucket.get(queueId);
        if (messages == null) {
            return;
        }

        for (final PublishWithRetained publishWithRetained : messages.qos0Messages) {
            // the payloads for QoS-0 messages are not extracted and their reference count is not incremented.
            // therefor it must not be decremented
            if (publishWithRetained.getQoS() != QoS.AT_MOST_ONCE) {
                payloadPersistence.decrementReferenceCounter(publishWithRetained.getPublishId());
            }
            increaseQos0MessagesMemory(-publishWithRetained.getEstimatedSize());
            // increaseClientQos0MessagesMemory not necessary as messages.qos0Memory = 0 below
            increaseMessagesMemory(-publishWithRetained.getEstimatedSize());
        }
        messages.qos0Messages.clear();
        messages.qos0Memory = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ExecuteInSingleWriter
    public @NotNull ImmutableSet<String> cleanUp(final int bucketIndex) {
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Map<String, Messages> bucket = buckets[bucketIndex];
        final Map<String, Messages> sharedBucket = sharedBuckets[bucketIndex];

        bucket.forEach((queueId, messages) -> cleanExpiredMessages(messages));
        sharedBucket.forEach((queueId, messages) -> cleanExpiredMessages(messages));

        return ImmutableSet.copyOf(sharedBucket.keySet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ExecuteInSingleWriter
    public void removeShared(
            final @NotNull String sharedSubscription, final @NotNull String uniqueId, final int bucketIndex) {

        checkNotNull(sharedSubscription, "Shared subscription must not be null");
        checkNotNull(uniqueId, "Unique id must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Map<String, Messages> bucket = sharedBuckets[bucketIndex];
        final Messages messages = bucket.get(sharedSubscription);
        if (messages == null) {
            return;
        }

        final Iterator<MessageWithID> iterator = messages.qos1Or2Messages.iterator();
        while (iterator.hasNext()) {
            final MessageWithID messageWithID = iterator.next();
            if (messageWithID instanceof PublishWithRetained) {
                final PublishWithRetained publish = (PublishWithRetained) messageWithID;
                if (!uniqueId.equals(publish.getUniqueId())) {
                    continue;
                }
                // the payloads for QoS-0 messages are not extracted and their reference count is not incremented.
                // therefor it must not be decremented
                if (publish.getQoS() != QoS.AT_MOST_ONCE) {
                    payloadPersistence.decrementReferenceCounter(publish.getPublishId());
                }
                if (publish.retained) {
                    messages.retainedQos1Or2Messages--;
                }
                increaseMessagesMemory(-publish.getEstimatedSize());
                iterator.remove();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ExecuteInSingleWriter
    public void removeInFlightMarker(
            final @NotNull String queueId, final @NotNull String uniqueId, final int bucketIndex) {

        checkNotNull(queueId, "QueueId must not be null");
        checkNotNull(uniqueId, "Unique id must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Map<String, Messages> bucket = sharedBuckets[bucketIndex];
        final Messages messages = bucket.get(queueId);
        if (messages == null) {
            return;
        }

        for (final MessageWithID messageWithID : messages.qos1Or2Messages) {
            if (messageWithID instanceof PublishWithRetained) {
                final PublishWithRetained publish = (PublishWithRetained) messageWithID;
                if (!uniqueId.equals(publish.getUniqueId())) {
                    continue;
                }
                publish.setPacketIdentifier(NO_PACKET_ID);
                break;
            }
        }
    }

    @Override
    @ExecuteInSingleWriter
    public void closeDB(final int bucketIndex) {
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);
        buckets[bucketIndex].clear();
        sharedBuckets[bucketIndex].clear();
        totalMemorySize.set(0L);
        qos0MessagesMemory.set(0L);
    }

    private int getMessageSize(final @NotNull MessageWithID messageWithID) {
        if (messageWithID instanceof PublishWithRetained) {
            return ((PublishWithRetained) messageWithID).getEstimatedSize();
        }
        if (messageWithID instanceof PubrelWithRetained) {
            return ((PubrelWithRetained) messageWithID).getEstimatedSize();
        }
        return 0;
    }

    private boolean isRetained(final @NotNull MessageWithID messageWithID) {
        if (messageWithID instanceof PublishWithRetained) {
            return ((PublishWithRetained) messageWithID).retained;
        }
        if (messageWithID instanceof PubrelWithRetained) {
            return ((PubrelWithRetained) messageWithID).retained;
        }
        return false;
    }

    private void logMessageDropped(
            final @NotNull PUBLISH publish, final boolean shared, final @NotNull String queueId) {

        if (shared) {
            messageDroppedService.queueFullShared(queueId, publish.getTopic(), publish.getQoS().getQosNumber());
        } else {
            messageDroppedService.queueFull(queueId, publish.getTopic(), publish.getQoS().getQosNumber());
        }
    }

    /**
     * @param size the amount of bytes the currently used qos 0 memory will be increased by. May be negative.
     */
    private void increaseQos0MessagesMemory(final int size) {
        if (size < 0) {
            qos0MessagesMemory.addAndGet(size - MemoryEstimator.LINKED_LIST_NODE_OVERHEAD);
        } else {
            qos0MessagesMemory.addAndGet(size + MemoryEstimator.LINKED_LIST_NODE_OVERHEAD);
        }
    }

    /**
     * @param size the amount of bytes the currently used memory will be increased by. May be negative.
     */
    private void increaseMessagesMemory(final int size) {
        if (size < 0) {
            totalMemorySize.addAndGet(size - MemoryEstimator.LINKED_LIST_NODE_OVERHEAD);
        } else {
            totalMemorySize.addAndGet(size + MemoryEstimator.LINKED_LIST_NODE_OVERHEAD);
        }
    }

    /**
     * @param size the amount of bytes the currently used qos 0 memory will be increased by. May be negative.
     */
    private void increaseClientQos0MessagesMemory(final @NotNull Messages messages, final int size) {
        if (size < 0) {
            messages.qos0Memory += size - MemoryEstimator.LINKED_LIST_NODE_OVERHEAD;
        } else {
            messages.qos0Memory += size + MemoryEstimator.LINKED_LIST_NODE_OVERHEAD;
        }
        if (messages.qos0Memory < 0) {
            messages.qos0Memory = 0;
        }
    }

    /**
     * @return true if a message was discarded, else false
     */
    private boolean discardOldest(
            final @NotNull String queueId,
            final boolean shared,
            final @NotNull Messages messages,
            final boolean retainedOnly) {

        final Iterator<MessageWithID> iterator = messages.qos1Or2Messages.iterator();
        while (iterator.hasNext()) {
            final MessageWithID messageWithID = iterator.next();
            if (!(messageWithID instanceof PublishWithRetained)) {
                continue;
            }
            final PublishWithRetained publish = (PublishWithRetained) messageWithID;
            // we must no discard inflight messages
            if (publish.getPacketIdentifier() != NO_PACKET_ID) {
                continue;
            }
            // Messages that are queued as retained messages are not discarded,
            // otherwise a client could only receive a limited amount of retained messages per subscription.
            if ((retainedOnly && !publish.retained) || (!retainedOnly && publish.retained)) {
                continue;
            }
            logAndDecrementPayloadReference(publish, shared, queueId);
            iterator.remove();
            return true;
        }
        return false;

    }

    private void logAndDecrementPayloadReference(
            final @NotNull PUBLISH publish, final boolean shared, final @NotNull String queueId) {

        logMessageDropped(publish, shared, queueId);
        // the payloads for QoS-0 messages are not extracted and their reference count is not incremented.
        // therefor it must not be decremented
        if (publish.getQoS() != QoS.AT_MOST_ONCE) {
            payloadPersistence.decrementReferenceCounter(publish.getPublishId());
        }
    }

    private void cleanExpiredMessages(final @NotNull Messages messages) {

        final Iterator<PublishWithRetained> iterator = messages.qos0Messages.iterator();
        while (iterator.hasNext()) {
            final PublishWithRetained publishWithRetained = iterator.next();
            if (publishWithRetained.isExpired()) {
                increaseQos0MessagesMemory(-publishWithRetained.getEstimatedSize());
                increaseClientQos0MessagesMemory(messages, -publishWithRetained.getEstimatedSize());
                increaseMessagesMemory(-publishWithRetained.getEstimatedSize());
                iterator.remove();
            }
        }

        final Iterator<MessageWithID> qos12iterator = messages.qos1Or2Messages.iterator();
        while (qos12iterator.hasNext()) {
            final MessageWithID messageWithID = qos12iterator.next();
            if (messageWithID instanceof PubrelWithRetained) {
                final PubrelWithRetained pubrel = (PubrelWithRetained) messageWithID;
                if (!InternalConfigurations.EXPIRE_INFLIGHT_PUBRELS_ENABLED) {
                    continue;
                }
                if (pubrel.getMessageExpiryInterval() == null || pubrel.getPublishTimestamp() == null) {
                    continue;
                }
                if (!pubrel.hasExpired()) {
                    continue;
                }
                if (pubrel.retained) {
                    messages.retainedQos1Or2Messages--;
                }
                increaseMessagesMemory(-pubrel.getEstimatedSize());
                qos12iterator.remove();

            } else if (messageWithID instanceof PublishWithRetained) {
                final PublishWithRetained publish = (PublishWithRetained) messageWithID;
                final boolean expireInflight = InternalConfigurations.EXPIRE_INFLIGHT_MESSAGES_ENABLED;
                final boolean isInflight = publish.getQoS() == QoS.EXACTLY_ONCE && publish.getPacketIdentifier() > 0;
                final boolean drop = publish.isExpired() && (!isInflight || expireInflight);
                if (drop) {
                    payloadPersistence.decrementReferenceCounter(publish.getPublishId());
                    if (publish.retained) {
                        messages.retainedQos1Or2Messages--;
                    }
                    increaseMessagesMemory(-publish.getEstimatedSize());
                    qos12iterator.remove();
                }
            }
        }
    }

    @VisibleForTesting
    static class PublishWithRetained extends PUBLISH {

        private final boolean retained;

        PublishWithRetained(final @NotNull PUBLISH publish, final boolean retained) {
            super(publish, publish.getPersistence());
            this.retained = retained;
        }

        int getEstimatedSize() {
            return getEstimatedSizeInMemory()  // publish
                    + MemoryEstimator.OBJECT_SHELL_SIZE // the object itself
                    + MemoryEstimator.BOOLEAN_SIZE; // retain flag
        }
    }

    private static class PubrelWithRetained extends PUBREL {

        private final boolean retained;

        private PubrelWithRetained(final @NotNull PUBREL pubrel, final boolean retained) {
            super(pubrel.getPacketIdentifier(),
                    pubrel.getReasonCode(),
                    pubrel.getReasonString(),
                    pubrel.getUserProperties(),
                    pubrel.getPublishTimestamp(),
                    pubrel.getMessageExpiryInterval());
            this.retained = retained;
        }

        private int getEstimatedSize() {
            return getEstimatedSizeInMemory()  // publish
                    + MemoryEstimator.OBJECT_SHELL_SIZE // the object itself
                    + MemoryEstimator.BOOLEAN_SIZE; // retain flag
        }
    }
}
