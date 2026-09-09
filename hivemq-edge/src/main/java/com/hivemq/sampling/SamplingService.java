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
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriptionFlag;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.ioc.annotation.Persistence;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SamplingService {

    private static final Logger log = LoggerFactory.getLogger(SamplingService.class);

    public static final @NotNull String SAMPLER_PREFIX = "$SAMPLER::";

    /**
     * How many samples {@link #getSamples} asks the persistence for.
     * <p>
     * <b>The ring is bounded per QoS class, not per queue</b> (EDG-882 review v02, R2-12). The limit is
     * applied on the QoS 0 path and on the QoS 1/2 path independently, so a topic whose publishers use
     * both can hold up to {@code 2 × SAMPLE_SIZE} messages while this peek takes {@code SAMPLE_SIZE} of
     * them — and which ones is the persistence's read order, not recency. Acceptable for a diagnostic
     * that exists to show an operator what a topic looks like, and written down because EDG-885's "at
     * most ten samples" is true per class rather than per topic. Counting across both classes would mean
     * one shared counter on a path where the two are stored separately, which is a bigger change than
     * the imprecision costs.
     */
    public static final int SAMPLE_SIZE = 10;

    public static final long SAMPLER_QUEUE_LIMIT = SAMPLE_SIZE;
    public static final int BYTE_LIMIT_SAMPLES = 100_000;

    private final @NotNull LocalTopicTree localTopicTree;
    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @Nullable ListeningScheduledExecutorService scheduledExecutorService;
    private final @NotNull LongSupplier nanoClock;

    /**
     * The topics currently sampled, each with the {@link #nanoClock} reading of the last time somebody
     * asked for it — a start or a read of its samples. That reading is the lease (EDG-885).
     * <p>
     * A set of leases, not a count of watchers.
     * <p>
     * Counting was the wrong shape while nothing releases: {@link #startSampling(String)} is reached
     * from an HTTP POST that carries no watcher identity, so retries, a remounted panel and a second
     * browser tab are indistinguishable and each one incremented a count that nothing ever decremented.
     * A count like that cannot be balanced by any release added later — it would have no way to know
     * which acquire it was balancing — and it read as a lifecycle that was being managed when it was
     * not (EDG-882 F-06).
     * <p>
     * <b>What makes it finite</b> is the lease. A release the caller could be identified by — a DELETE
     * with a watcher token — would need a new endpoint and a UI that remembers to call it on every way
     * a panel can go away, and a tab that crashes never would. The lease needs neither: it is renewed
     * by the calls the UI already makes, and {@link #expireLeases()} releases whatever has not been
     * asked about for {@link InternalConfigurations#SAMPLING_LEASE_TTL_SEC}. What a panel left open
     * past the lease sees is covered in {@link #getSamples(String)}.
     */
    private final @NotNull Map<String, Long> sampledTopics = new ConcurrentHashMap<>(0);

    @Inject
    public SamplingService(
            final @NotNull LocalTopicTree localTopicTree,
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull @Persistence ListeningScheduledExecutorService scheduledExecutorService) {
        this(localTopicTree, clientQueuePersistence, scheduledExecutorService, System::nanoTime);
    }

    /** No sweep is scheduled: tests drive {@link #expireLeases()} themselves. */
    @VisibleForTesting
    SamplingService(
            final @NotNull LocalTopicTree localTopicTree,
            final @NotNull ClientQueuePersistence clientQueuePersistence) {
        this(localTopicTree, clientQueuePersistence, null, System::nanoTime);
    }

    @VisibleForTesting
    SamplingService(
            final @NotNull LocalTopicTree localTopicTree,
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @Nullable ListeningScheduledExecutorService scheduledExecutorService,
            final @NotNull LongSupplier nanoClock) {
        this.localTopicTree = localTopicTree;
        this.clientQueuePersistence = clientQueuePersistence;
        this.scheduledExecutorService = scheduledExecutorService;
        this.nanoClock = nanoClock;
    }

    /**
     * Schedules the lease sweep. Method injection: Dagger calls this once, right after construction.
     * <p>
     * The persistence scheduler is used rather than a thread of this class's own because the sweep is
     * a sibling of the periodic clean-up that reclaims the released queues, and because that executor
     * already has a shutdown hook; the sweep dies with it. Nothing is scheduled on an executor that is
     * already shut down, which is where a late construction during shutdown would otherwise throw.
     */
    @Inject
    public void postConstruct() {
        if (scheduledExecutorService == null || scheduledExecutorService.isShutdown()) {
            return;
        }
        final int interval = InternalConfigurations.SAMPLING_LEASE_SWEEP_INTERVAL_SEC.get();
        // The future is not kept: the sweep runs until the executor is shut down, and there is no
        // earlier point at which anything would cancel it.
        final var unused = scheduledExecutorService.scheduleWithFixedDelay(
                this::expireLeases, interval, interval, TimeUnit.SECONDS);
    }

    /**
     * Starts sampling a topic, subscribing the first time it is asked for, and renews its lease.
     * <p>
     * Idempotent on the subscription: asking again while the topic is already sampled subscribes
     * nothing. That is the honest shape for a call reached from a POST with no watcher identity —
     * retries, a remounted panel and a second tab all mean the same thing here, "somebody wants
     * samples of this topic". What a repeated start does change is the lease: it is what keeps a
     * topic the UI keeps coming back to from expiring under it.
     * <p>
     * The subscribe happens <b>inside</b> the map update rather than after it. {@code compute} is
     * atomic for a key, so a concurrent {@link #stopSampling(String)} or {@link #expireLeases()}
     * cannot interleave between the entry appearing and the subscriber being added. Without that, a
     * stop could remove the subscriber a start had just added, leaving this map saying "sampled" while
     * the topic tree says "not subscribed" — no samples would ever arrive, the clean-up would rightly
     * reclaim the queue, and nothing would re-register until the topic changed. Silent, sticky, and
     * invisible to any single-threaded test.
     */
    public void startSampling(final @NotNull String topic) {
        sampledTopics.compute(topic, (sampledTopic, lease) -> {
            if (lease == null) {
                subscribe(sampledTopic);
            }
            return nanoClock.getAsLong();
        });
    }

    /**
     * Stops sampling a topic and unsubscribes.
     * <p>
     * In production this is reached through {@link #expireLeases()}; nothing calls it for a named
     * topic, because nothing can name a watcher. Stopping a topic nobody is sampling is a no-op, so a
     * duplicate stop cannot make a later {@link #startSampling(String)} fail to subscribe.
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
     * Releases every topic whose lease has run out: nobody started or read it for
     * {@link InternalConfigurations#SAMPLING_LEASE_TTL_SEC}. Run periodically from
     * {@link #postConstruct()}; public so a test can drive it against its own clock.
     * <p>
     * The expiry check runs <b>inside</b> the per-key update, so a renewal that lands while the sweep
     * is looking at that key is seen — the entry is either renewed or removed, and the subscriber
     * follows the entry either way. A snapshot-then-remove would let a renewal slip between the two
     * and release a topic that had just been asked for.
     * <p>
     * A failure on one topic is logged and the sweep goes on; a periodic task that throws would
     * silently never run again, which would put the leak back.
     */
    @VisibleForTesting
    public void expireLeases() {
        final long now = nanoClock.getAsLong();
        final long ttlNanos = TimeUnit.SECONDS.toNanos(InternalConfigurations.SAMPLING_LEASE_TTL_SEC.get());
        for (final String topic : sampledTopics.keySet()) {
            try {
                sampledTopics.computeIfPresent(topic, (sampledTopic, lease) -> {
                    if (now - lease < ttlNanos) {
                        return lease;
                    }
                    log.debug(
                            "Releasing sampling for topic '{}': nobody asked for it for {} s",
                            sampledTopic,
                            InternalConfigurations.SAMPLING_LEASE_TTL_SEC.get());
                    unsubscribe(sampledTopic);
                    return null;
                });
            } catch (final RuntimeException e) {
                log.warn(
                        "Failed to release sampling for topic '{}'; it will be tried again on the next sweep",
                        topic,
                        e);
            }
        }
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

    /**
     * The samples collected for a topic, most recent last.
     * <p>
     * Reading renews the lease, and a read of a topic that is no longer sampled starts it again. That
     * is what a panel left open past the lease sees: its next refresh comes back empty — the released
     * queue was reclaimed — and sampling is running again by the time it returns, so the refresh after
     * that has samples. The same thing the panel saw when it first opened, and the same thing it
     * would see after a node restart; nothing to handle that the UI does not handle already. Making
     * the read revive rather than only renew is what keeps a GET from ever answering "no samples"
     * for a topic that was sampled a moment ago and stays that way.
     */
    public @NotNull List<byte[]> getSamples(final @NotNull String topic) {
        startSampling(topic);
        final String queueId = createQueueId(topic);
        final ListenableFuture<ImmutableList<PUBLISH>> publishes =
                clientQueuePersistence.peek(queueId, true, BYTE_LIMIT_SAMPLES, SAMPLE_SIZE);
        try {
            return publishes.get().stream().map(PUBLISH::getPayload).collect(Collectors.toList());
        } catch (final InterruptedException | ExecutionException e) {
            // The flag is restored before the throw: this runs on the REST request thread, which the
            // container reuses, and swallowing the interrupt leaves the next request on that thread
            // unable to see that it was asked to stop.
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Exception while retrieval of sample payloads for topic '{}'", topic, e);
            throw new RuntimeException(e);
        }
    }
}
