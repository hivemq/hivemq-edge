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
     * The topics currently sampled. A set, not a count of watchers.
     * <p>
     * Counting was the wrong shape while nothing releases: {@link #startSampling(String)} is reached
     * from an HTTP POST that carries no watcher identity, so retries, a remounted panel and a second
     * browser tab are indistinguishable and each one incremented a count that nothing ever decremented.
     * A count like that cannot be balanced by any release added later — it would have no way to know
     * which acquire it was balancing — and it read as a lifecycle that was being managed when it was
     * not (EDG-882 F-06).
     * <p>
     * <b>What this does not fix:</b> nothing in production stops sampling, so a sampled topic keeps its
     * subscription, and its ten-message queue, for the life of the node. Making that finite needs
     * either a release the caller can be identified by — a DELETE endpoint with a watcher token — or a
     * lease that expires unless refreshed, and the second changes what an idle-but-open editor panel
     * sees. Both are decisions about the product's API surface rather than repairs to this class, and
     * they belong to EDG-885.
     */
    private final @NotNull Map<String, Boolean> sampledTopics = new ConcurrentHashMap<>(0);

    @Inject
    public SamplingService(
            final @NotNull LocalTopicTree localTopicTree,
            final @NotNull ClientQueuePersistence clientQueuePersistence) {
        this.localTopicTree = localTopicTree;
        this.clientQueuePersistence = clientQueuePersistence;
    }

    /**
     * Starts sampling a topic, subscribing the first time it is asked for.
     * <p>
     * Idempotent: asking again while the topic is already sampled changes nothing. That is the honest
     * shape for a call reached from a POST with no watcher identity — retries, a remounted panel and a
     * second tab all mean the same thing here, "somebody wants samples of this topic".
     * <p>
     * The subscribe happens <b>inside</b> the map update rather than after it. {@code computeIfAbsent}
     * is atomic for a key, so a concurrent {@link #stopSampling(String)} cannot interleave between the
     * entry appearing and the subscriber being added. Without that, a stop could remove the subscriber
     * a start had just added, leaving this map saying "sampled" while the topic tree says "not
     * subscribed" — no samples would ever arrive, the clean-up would rightly reclaim the queue, and
     * nothing would re-register until the topic changed. Silent, sticky, and invisible to any
     * single-threaded test.
     */
    public void startSampling(final @NotNull String topic) {
        sampledTopics.computeIfAbsent(topic, sampledTopic -> {
            subscribe(sampledTopic);
            return Boolean.TRUE;
        });
    }

    /**
     * Stops sampling a topic and unsubscribes.
     * <p>
     * <b>Nothing in production calls this yet</b>; it is the entry point a release path will use, and
     * what the tests drive. Stopping a topic nobody is sampling is a no-op, so a duplicate stop cannot
     * make a later {@link #startSampling(String)} fail to subscribe.
     * <p>
     * Once the subscriber is gone the periodic clean-up reclaims the queue on its next sweep with no
     * further help — {@code ClientQueuePersistenceImpl.isOrphaned} stops finding an owner for it. If
     * sampling starts again after that, it starts from an empty queue and waits a moment for fresh
     * samples. That is the intended behaviour and not a race worth defending against: samples are
     * ephemeral diagnostics that regenerate in seconds, unlike a bridge queue, whose loss is unbounded
     * and unrecoverable.
     */
    public void stopSampling(final @NotNull String topic) {
        sampledTopics.computeIfPresent(topic, (sampledTopic, sampling) -> {
            unsubscribe(sampledTopic);
            return null;
        });
    }

    /**
     * Whether this topic is currently sampled. Exposed for tests and diagnostics; the authoritative
     * record of a live sampler remains the topic tree.
     */
    @VisibleForTesting
    public boolean isSampling(final @NotNull String topic) {
        return sampledTopics.containsKey(topic);
    }

    /**
     * Whether this queue belongs to a sampling subscription this service created.
     * <p>
     * Asked by the publish path to decide whether the queue is a sample ring — a policy that discards
     * the oldest messages — so the answer has to be about what Edge owns, not about how the ID is
     * spelled. Two things have to hold: the ID must have the shape {@link #createQueueId(String)}
     * produces, and the topic it decodes to must actually be sampled right now. A client subscribing
     * to {@code $share/$SAMPLER::customer/alerts} fails the first (the two halves differ) and one that
     * contrives the doubled shape fails the second, so neither has an eviction policy applied to its
     * messages that it did not ask for (EDG-882 F-05).
     * <p>
     * The sampled-topics map, not the topic tree: it is the record of what was asked for, and it is
     * the same map {@link #startSampling(String)} maintains, so this cannot disagree with whether a
     * subscription exists.
     */
    public boolean isSamplerQueue(final @NotNull String queueId) {
        final String sampledTopic = extractSampledTopic(queueId);
        return sampledTopic != null && sampledTopics.containsKey(sampledTopic);
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
