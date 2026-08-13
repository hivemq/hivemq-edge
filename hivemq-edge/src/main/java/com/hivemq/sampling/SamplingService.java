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
package com.hivemq.sampling;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriptionFlag;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SamplingService {

    private static final Logger log = LoggerFactory.getLogger(SamplingService.class);

    public static final @NotNull String SAMPLER_PREFIX = "$SAMPLER::";

    public static final int SAMPLE_SIZE = 10;
    public static final long SAMPLER_QUEUE_LIMIT = SAMPLE_SIZE;
    public static final int BYTE_LIMIT_SAMPLES = 100_000;

    private final @NotNull LocalTopicTree localTopicTree;
    private final @NotNull ClientQueuePersistence clientQueuePersistence;

    /**
     * How many callers currently want samples for each topic.
     * <p>
     * Sampling is a diagnostic collection started so the mapping editor can infer a schema, not a
     * durable subscription: several clients may ask for the same topic at once, and the subscription
     * must live exactly as long as the last of them. Counting watchers is what makes one client
     * closing its panel harmless to another that is still looking.
     */
    private final @NotNull Map<String, Integer> watchers = new ConcurrentHashMap<>(0);

    @Inject
    public SamplingService(
            final @NotNull LocalTopicTree localTopicTree,
            final @NotNull ClientQueuePersistence clientQueuePersistence) {
        this.localTopicTree = localTopicTree;
        this.clientQueuePersistence = clientQueuePersistence;
    }

    /**
     * Registers interest in samples for a topic, subscribing on the first watcher.
     * <p>
     * Every call must be paired with exactly one {@link #stopSampling(String)}, or the subscription
     * outlives its last watcher and its queue is never reclaimed.
     * <p>
     * The subscribe happens <b>inside</b> the map update rather than after it. {@code compute} is
     * atomic for a key, so a concurrent {@link #stopSampling(String)} cannot interleave between the
     * count reaching zero and the subscriber being removed. Without that, a release that had decided
     * to stop could remove the subscriber an acquire had just added, leaving the count saying
     * "watched" while the topic tree says "not subscribed" — no samples would ever arrive, the
     * clean-up would rightly reclaim the queue, and nothing would re-register until the topic changed.
     * Silent, sticky, and invisible to any single-threaded test.
     */
    public void startSampling(final @NotNull String topic) {
        watchers.compute(topic, (sampledTopic, count) -> {
            if (count == null) {
                subscribe(sampledTopic);
                return 1;
            }
            return count + 1;
        });
    }

    /**
     * Releases one watcher's interest, unsubscribing when the last one lets go.
     * <p>
     * A release for a topic nobody is watching is a no-op: the count never goes negative, so a
     * duplicate stop cannot make a later {@link #startSampling(String)} fail to subscribe.
     * <p>
     * Once the subscriber is gone the periodic clean-up reclaims the queue on its next sweep with no
     * further help — {@code ClientQueuePersistenceImpl.isOrphaned} stops finding an owner for it. If a
     * new watcher arrives after that, it starts from an empty queue and waits a moment for fresh
     * samples. That is the intended behaviour and not a race worth defending against: samples are
     * ephemeral diagnostics that regenerate in seconds, unlike a bridge queue, whose loss is
     * unbounded and unrecoverable.
     */
    public void stopSampling(final @NotNull String topic) {
        watchers.computeIfPresent(topic, (sampledTopic, count) -> {
            if (count > 1) {
                return count - 1;
            }
            unsubscribe(sampledTopic);
            return null;
        });
    }

    /**
     * Whether any caller is currently watching this topic. Exposed for tests and diagnostics; the
     * authoritative record of a live sampler remains the topic tree.
     */
    @VisibleForTesting
    public boolean isSampling(final @NotNull String topic) {
        return watchers.containsKey(topic);
    }

    private void subscribe(final @NotNull String topic) {
        log.debug("Starting sampling for topic: '{}'", topic);
        final String clientId = SAMPLER_PREFIX + topic;
        localTopicTree.addTopic(
                clientId,
                new Topic(topic, QoS.AT_LEAST_ONCE, false, true),
                SubscriptionFlag.getDefaultFlags(true, true, false),
                clientId);
    }

    private void unsubscribe(final @NotNull String topic) {
        log.debug("Stopping sampling for topic: '{}'", topic);
        final String clientId = SAMPLER_PREFIX + topic;
        // The share name must be the one subscribe() registered under. Passing null here sends
        // MatchingNodeSubscriptions.removeSubscriberFromStructures down its non-shared branch, which
        // searches a map this subscription was never in -- so the removal silently does nothing and
        // the sampler lives forever. MessageForwarderImpl.removeForwarder passes its share name for
        // the same reason.
        localTopicTree.removeSubscriber(clientId, topic, clientId);
    }

    /**
     * The queue backing a sampled topic. The share name is {@code $SAMPLER::<topic>}, so the topic
     * appears twice and, when it contains a '/', the share-name boundary is not the first slash.
     */
    public static @NotNull String createQueueId(final @NotNull String topic) {
        return SAMPLER_PREFIX + topic + "/" + topic;
    }

    /**
     * Recovers the sampled topic from a queue ID built by {@link #createQueueId(String)}, or null if
     * the ID does not have that shape. Splitting at the first '/' would yield the wrong share name
     * for any sampled topic containing a '/'.
     */
    public static @Nullable String extractSampledTopic(final @NotNull String queueId) {
        if (!queueId.startsWith(SAMPLER_PREFIX)) {
            return null;
        }
        // What follows the prefix must be the topic, a '/', then the same topic again. Both halves
        // have the same length, so the separator can only be at the exact midpoint -- which makes the
        // shape decidable by three checks and no searching.
        final int topicStart = SAMPLER_PREFIX.length();
        final int remainingLength = queueId.length() - topicStart;
        if (remainingLength % 2 == 0) {
            return null; // topic + '/' + topic always has odd length
        }
        final int topicLength = remainingLength / 2;
        final int separator = topicStart + topicLength;
        if (queueId.charAt(separator) != '/') {
            return null;
        }
        if (!queueId.regionMatches(topicStart, queueId, separator + 1, topicLength)) {
            return null; // the two halves differ, so this is not a sampler queue
        }
        return queueId.substring(topicStart, separator);
    }

    public @NotNull List<byte[]> getSamples(final @NotNull String topic) {
        final String queueId = createQueueId(topic);
        final ListenableFuture<ImmutableList<PUBLISH>> publishes =
                clientQueuePersistence.peek(queueId, true, BYTE_LIMIT_SAMPLES, SAMPLE_SIZE);
        try {
            return publishes.get().stream().map(PUBLISH::getPayload).collect(Collectors.toList());
        } catch (final @NotNull InterruptedException | ExecutionException e) {
            log.warn("Exception while retrieval of sample payloads for topic '{}'", topic);
            throw new RuntimeException(e);
        }
    }
}
