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
package com.hivemq.persistence.clientqueue;

import static com.hivemq.configuration.service.MqttConfigurationService.QueuedMessagesStrategy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.ImmutableIntArray;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.persistence.LocalPersistence;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * <b>Binary compatibility with the file-native module is part of this interface's contract.</b>
 * <p>
 * The file-native implementation lives in {@code hivemq-edge-mqtt-persistence}, which is loaded from
 * {@code HIVEMQ_HOME/modules} at run time. Core and module are compiled separately and meet only at
 * start-up, so an operator can pair any module jar with any core jar — including by accident, by
 * replacing the core zip and leaving {@code modules/} alone.
 * <p>
 * That makes the direction of every {@code default} here load-bearing. A default method protects
 * <em>callers</em> of the signature it replaces, and core has no callers of the old signatures left.
 * What needs protecting is <em>implementors</em>: an older module implements only the signature that
 * existed when it was built, so the signature core calls must be the one with a default, and the
 * default must delegate to the older one. Getting this backwards makes a mismatched module fail with
 * {@code AbstractMethodError} on the first queued publish — on a file-native node, the entire message
 * path, with a stack trace that names no version (EDG-882 review v02, R2-01).
 * <p>
 * So: <b>when a parameter is added here, the old signature stays abstract and the new one gets the
 * default.</b> Implementations override the new signature and implement the old one as a delegation.
 * The cost is that a module built before the addition degrades silently to the previous behaviour, so
 * {@code ClientQueueLocalPersistenceProvider} probes the loaded implementation once at start-up and
 * says plainly what a stale module gives up.
 *
 * @author Lukas Brandl
 * @since 4.0.0
 */
public interface ClientQueueLocalPersistence extends LocalPersistence {

    /**
     * Adds a PUBLISH to a client or shared subscription queue. If the size exceeds the queue limit, the given PUBLISH
     * or the oldest PUBLISH in the queue will be dropped dependent on the queued messages strategy.
     *
     * @param queueId     for which the PUBLISH will be queued
     * @param shared      is true if the queueId is actually a shared subscription false if it is a client ID
     * @param publish     to be queued
     * @param max         maximum amount of messages queued for the client
     * @param strategy    how to discard messages in case the queue is full
     * @param applyMaxToQos0 whether {@code max} also bounds QoS 0 messages. Normally false: QoS 0 queues
     *                       are held only by the node-wide QoS 0 memory budget, and imposing a count
     *                       limit on them would change long-standing behaviour for bridges and client
     *                       queues. Sampler queues pass true — they are ring buffers of the most recent
     *                       {@code SamplingService.SAMPLE_SIZE} payloads, and without this one sampled
     *                       QoS 0 topic can consume the whole shared budget and starve every other QoS 0
     *                       consumer on the node (EDG-885).
     * @param retained    true if this message was sent in response to a subscribe. Retained messages are not dropped
     *                    when the queue reached the maximum queue size.
     * @param bucketIndex provided by the single writer
     */
    default void add(
            final @NotNull String queueId,
            final boolean shared,
            final @NotNull PUBLISH publish,
            final long max,
            final @NotNull QueuedMessagesStrategy strategy,
            final boolean retained,
            final boolean applyMaxToQos0,
            final int bucketIndex) {
        // Default, not abstract, so a module built before applyMaxToQos0 existed keeps working; see the
        // contract note on this interface. It loses the QoS 0 bound, which is the previous behaviour.
        add(queueId, shared, publish, max, strategy, retained, bucketIndex);
    }

    /**
     * Equivalent to the overload above with {@code applyMaxToQos0 = false}, which is the historical
     * behaviour. Abstract, and staying abstract, because it is the signature every module ever built
     * implements.
     */
    void add(
            @NotNull String queueId,
            boolean shared,
            @NotNull PUBLISH publish,
            long max,
            @NotNull QueuedMessagesStrategy strategy,
            boolean retained,
            int bucketIndex);

    /**
     * Adds a list of PUBLISHes to a client or shared subscription queue. If the size exceeds the queue limit, the given
     * PUBLISH
     * or the oldest PUBLISH in the queue will be dropped dependent on the queued messages strategy.
     *
     * @param queueId     for which the PUBLISH will be queued
     * @param shared      is true if the queueId is actually a shared subscription false if it is a client ID
     * @param publishes   to be queued
     * @param max         maximum amount of messages queued for the client
     * @param strategy    how to discard messages in case the queue is full
     * @param applyMaxToQos0 whether {@code max} also bounds QoS 0 messages. Normally false: QoS 0 queues
     *                       are held only by the node-wide QoS 0 memory budget, and imposing a count
     *                       limit on them would change long-standing behaviour for bridges and client
     *                       queues. Sampler queues pass true — they are ring buffers of the most recent
     *                       {@code SamplingService.SAMPLE_SIZE} payloads, and without this one sampled
     *                       QoS 0 topic can consume the whole shared budget and starve every other QoS 0
     *                       consumer on the node (EDG-885).
     * @param retained    true if this messages are sent in response to a subscribe. Retained messages are not dropped
     *                    when the queue reached the maximum queue size. It is not necessarily the same as the retain
     *                    flag of the publish.
     * @param bucketIndex provided by the single writer
     */
    default void add(
            final @NotNull String queueId,
            final boolean shared,
            final @NotNull List<PUBLISH> publishes,
            final long max,
            final @NotNull QueuedMessagesStrategy strategy,
            final boolean retained,
            final boolean applyMaxToQos0,
            final int bucketIndex) {
        // Default, not abstract, so a module built before applyMaxToQos0 existed keeps working; see the
        // contract note on this interface. It loses the QoS 0 bound, which is the previous behaviour.
        add(queueId, shared, publishes, max, strategy, retained, bucketIndex);
    }

    /**
     * Equivalent to the overload above with {@code applyMaxToQos0 = false}, which is the historical
     * behaviour. Abstract, and staying abstract, because it is the signature every module ever built
     * implements.
     */
    void add(
            @NotNull String queueId,
            boolean shared,
            @NotNull List<PUBLISH> publishes,
            long max,
            @NotNull QueuedMessagesStrategy strategy,
            boolean retained,
            int bucketIndex);

    /**
     * Returns a batch of PUBLISHes and marks them by setting packet identifiers. The size of the batch is limited by 2
     * factors:
     * <li>
     * <ul>The count of PUBLISHes will be less than or equal to the size of the given packet id list</ul>
     * <ul>The estimated memory usage will be approximately less than or equal to the given bytes limit but never less
     * than one publish</ul>
     * </li>
     * <p>
     * IMPORTANT: qos 0 messages are removed after reading.
     *
     * @param queueId     for which to read the PUBLISHes
     * @param shared      is true if the queueId is actually a shared subscription false if it is a client ID
     * @param packetIds   to be set for the PUBLISHes in the batch
     * @param bytesLimit  the estimated memory limit of the batch
     * @param bucketIndex provided by the single writer
     * @return a list of queued messages with the provided ID's
     */
    @NotNull
    ImmutableList<PUBLISH> readNew(
            @NotNull String queueId,
            boolean shared,
            @NotNull ImmutableIntArray packetIds,
            long bytesLimit,
            int bucketIndex);

    /**
     * Returns a batch of PUBLISHes and marks them by setting packet identifiers. The size of the batch is limited by 2
     * factors:
     * <li>
     * <ul>The count of PUBLISHes will be less than or equal to the size of the given packet id list</ul>
     * <ul>The estimated memory usage will be approximately less than or equal to the given bytes limit but never less
     * than one publish</ul>
     * </li>
     * <p>
     * IMPORTANT: No messages are altered in the process
     *
     * @param queueId     for which to read the PUBLISHes
     * @param shared      is true if the queueId is actually a shared subscription false if it is a client ID
     * @param bytesLimit  the estimated memory limit of the batch
     * @param bucketIndex provided by the single writer
     * @param maxMessages maximal amount of publishes that will be fetched.
     * @return a list of queued messages with the provided ID's
     */
    @NotNull
    ImmutableList<PUBLISH> peek(
            @NotNull String queueId, boolean shared, long bytesLimit, final int maxMessages, int bucketIndex);

    /**
     * Returns a batch of PUBLISHes that already have a packet identifier. The size of the batch is limited by 2
     * factors:
     * <li>
     * <ul>The count of PUBLISHes will be less then or equal to the size of the given packet id list</ul>
     * <ul>The estimated memory usage will be approximately less than or equal to the given bytes limit</ul>
     * </li>
     *
     * @param client      for which to read the PUBLISHes
     * @param shared      is true if the queueId is actually a shared subscription false if it is a client ID
     * @param batchSize   the limit of messages for the batch
     * @param bytesLimit  the estimated memory limit of the batch
     * @param bucketIndex provided by the single writer
     * @return a list of queued messages with the provided ID's
     */
    @NotNull
    ImmutableList<MessageWithID> readInflight(
            @NotNull String client, boolean shared, int batchSize, long bytesLimit, int bucketIndex);

    /**
     * Replaces the PUBLISH with the PUBREL with the same packet id.
     * <p>
     * This method is not used for shared subscriptions. Because PUBRELs are not stored for shared subscriptions.
     *
     * @param client      for which the PUBREL will replace a PUBLISH
     * @param pubrel      to be put
     * @param bucketIndex provided by the single writer
     * @return the id of the replace publish or null if no message was replaced
     */
    @Nullable
    String replace(@NotNull String client, @NotNull PUBREL pubrel, int bucketIndex);

    /**
     * Removes the PUBLISH or PUBREL with the given packet id.
     * <p>
     * This method is not used for shared subscriptions. Because the shared subscription queue doesn't have packet IDs.
     *
     * @param client      for which the message will be removed
     * @param packetId    for which the message will be removed
     * @param bucketIndex provided by the single writer
     * @return the unique id of the removed publish or null if no publish was removed
     */
    @Nullable
    String remove(@NotNull String client, int packetId, int bucketIndex);

    /**
     * Removes the PUBLISH or PUBREL with the given packet id if the unique publish id matches.
     *
     * @param client      for which the message will be removed
     * @param packetId    for which the message will be removed
     * @param bucketIndex provided by the single writer
     * @param uniqueId    of the PUBLISH to remove
     * @return the unique id of the removed publish or null if no publish was removed
     */
    @Nullable
    String remove(@NotNull String client, int packetId, @Nullable String uniqueId, int bucketIndex);

    /**
     * Returns the amount of queued messages for the given client or shared subscription.
     *
     * @param queueId     for which to read the queue size
     * @param shared      is true if the queueId is actually a shared subscription false if it is a client ID
     * @param bucketIndex provided by the single writer
     * @return the amount of queued messages
     */
    int size(@NotNull String queueId, boolean shared, int bucketIndex);

    /**
     * Removes the queue for the given client or shared subscription.
     *
     * @param queueId     for which to remove the queue
     * @param shared      is true if the queueId is actually a shared subscription false if it is a client ID
     * @param bucketIndex provided by the single writer
     */
    void clear(@NotNull String queueId, boolean shared, int bucketIndex);

    /**
     * Removes all qos 0 messages from a queue
     *
     * @param queueId     for which to remove the messages
     * @param shared      is true if the queueId is actually a shared subscription false if it is a client ID
     * @param bucketIndex provided by the single writer
     */
    void removeAllQos0Messages(@NotNull String queueId, boolean shared, int bucketIndex);

    /**
     * Remove expired messages.
     *
     * @param bucketIndex of the bucket to clean up
     * @return queue ids of all shared queues
     */
    @NotNull
    ImmutableSet<String> cleanUp(int bucketIndex);

    /**
     * Remove a PUBLISH with a given unique ID. Messages with QoS 0 are not checked.
     *
     * @param sharedSubscription for which the message is removed
     * @param uniqueId           of the message to remove
     * @param bucketIndex        provided by the single writer
     */
    void removeShared(@NotNull String sharedSubscription, @NotNull String uniqueId, int bucketIndex);

    /**
     * Remove the in-flight marker of a PUBLISH with a given unique ID.
     *
     * @param queueId     for which the marker is removed
     * @param uniqueId    of the affected message
     * @param bucketIndex provided by the single writer
     */
    void removeInFlightMarker(@NotNull String queueId, @NotNull String uniqueId, int bucketIndex);

    /**
     * Remove all in-flight markers for a queue.
     * This is called when a bridge reconnects to reset the state and allow messages to be re-delivered.
     *
     * @param queueId     for which all markers should be removed
     * @param bucketIndex provided by the single writer
     */
    void removeAllInFlightMarkers(@NotNull String queueId, int bucketIndex);
}
