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

import static com.hivemq.sampling.SamplingService.SAMPLER_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * EDG-885: sampling is started by the mapping editor so it can infer a schema, and until this work it
 * was never stopped — {@code stopSampling} had no production caller, and could not have worked if it
 * had one. The subscription therefore outlived every watcher and its queue was never reclaimed.
 * <p>
 * These tests cover the watcher lifecycle: the subscription must appear on the first watcher, survive
 * every release but the last, and disappear on the last — with the transition atomic enough that a
 * release cannot tear down a subscription an overlapping acquire has just created.
 */
@SuppressWarnings("FutureReturnValueIgnored") // submitted work reports failures through the shared holder
public class SamplingServiceLifecycleTest {

    private static final @NotNull String TOPIC = "plant/line1/from-plc";
    private static final @NotNull String CLIENT_ID = SAMPLER_PREFIX + TOPIC;

    private LocalTopicTree topicTree;
    private SamplingService samplingService;

    @BeforeEach
    public void setUp() {
        topicTree = mock(LocalTopicTree.class);
        samplingService = new SamplingService(topicTree, mock(ClientQueuePersistence.class));
    }

    @Test
    public void test_firstWatcherSubscribes_andLastWatcherUnsubscribes() {
        assertFalse(samplingService.isSampling(TOPIC));

        samplingService.startSampling(TOPIC);
        assertTrue(samplingService.isSampling(TOPIC));
        verify(topicTree, times(1)).addTopic(eq(CLIENT_ID), any(), anyByte(), eq(CLIENT_ID));

        samplingService.stopSampling(TOPIC);
        assertFalse(samplingService.isSampling(TOPIC));
        verify(topicTree, times(1)).removeSubscriber(eq(CLIENT_ID), eq(TOPIC), eq(CLIENT_ID));
    }

    /**
     * The reason for counting at all: one client closing its panel must not blind another that is
     * still watching the same topic.
     */
    @Test
    public void test_aSecondWatcherKeepsTheSubscriptionAliveWhenTheFirstReleases() {
        samplingService.startSampling(TOPIC);
        samplingService.startSampling(TOPIC);
        verify(topicTree, times(1)).addTopic(eq(CLIENT_ID), any(), anyByte(), eq(CLIENT_ID));

        samplingService.stopSampling(TOPIC);
        assertTrue(samplingService.isSampling(TOPIC), "one watcher remains");
        verify(topicTree, never()).removeSubscriber(any(), any(), any());

        samplingService.stopSampling(TOPIC);
        assertFalse(samplingService.isSampling(TOPIC));
        verify(topicTree, times(1)).removeSubscriber(eq(CLIENT_ID), eq(TOPIC), eq(CLIENT_ID));
    }

    /**
     * The regression test for the defect that made the whole lifecycle unbuildable: {@code subscribe}
     * registers a <em>shared</em> subscription under a share name, and the removal must name it.
     * Passing {@code null} sends {@code MatchingNodeSubscriptions.removeSubscriberFromStructures} down
     * its non-shared branch, which searches a map this subscription was never in — so the removal
     * silently does nothing and the sampler lives forever.
     */
    @Test
    public void test_unsubscribeNamesTheShareNameItSubscribedUnder() {
        samplingService.startSampling(TOPIC);
        samplingService.stopSampling(TOPIC);

        verify(topicTree).removeSubscriber(CLIENT_ID, TOPIC, CLIENT_ID);
        verify(topicTree, never()).removeSubscriber(CLIENT_ID, TOPIC, null);
    }

    @Test
    public void test_releasingATopicNobodyWatchesIsANoOp() {
        samplingService.stopSampling(TOPIC);

        assertFalse(samplingService.isSampling(TOPIC));
        verify(topicTree, never()).removeSubscriber(any(), any(), any());

        // and the count must not have gone negative: a later acquire still subscribes
        samplingService.startSampling(TOPIC);
        assertTrue(samplingService.isSampling(TOPIC));
        verify(topicTree, times(1)).addTopic(eq(CLIENT_ID), any(), anyByte(), eq(CLIENT_ID));
    }

    @Test
    public void test_releasingTwiceAfterOneAcquireIsANoOpTheSecondTime() {
        samplingService.startSampling(TOPIC);
        samplingService.stopSampling(TOPIC);
        samplingService.stopSampling(TOPIC);

        assertFalse(samplingService.isSampling(TOPIC));
        verify(topicTree, times(1)).removeSubscriber(eq(CLIENT_ID), eq(TOPIC), eq(CLIENT_ID));
    }

    @Test
    public void test_aTopicCanBeSampledAgainAfterItsLastWatcherLeft() {
        samplingService.startSampling(TOPIC);
        samplingService.stopSampling(TOPIC);
        samplingService.startSampling(TOPIC);

        assertTrue(samplingService.isSampling(TOPIC));
        verify(topicTree, times(2)).addTopic(eq(CLIENT_ID), any(), anyByte(), eq(CLIENT_ID));
        verify(topicTree, times(1)).removeSubscriber(eq(CLIENT_ID), eq(TOPIC), eq(CLIENT_ID));
    }

    @Test
    public void test_topicsAreCountedIndependently() {
        final String other = "plant/line2/from-plc";
        samplingService.startSampling(TOPIC);
        samplingService.startSampling(other);

        samplingService.stopSampling(TOPIC);

        assertFalse(samplingService.isSampling(TOPIC));
        assertTrue(samplingService.isSampling(other), "releasing one topic must not affect another");
        verify(topicTree, times(1)).removeSubscriber(eq(CLIENT_ID), eq(TOPIC), eq(CLIENT_ID));
    }

    /**
     * Topics containing '/' are the EDG-882 shape — the sampler queue ID repeats the topic around a
     * separator, so the share name itself contains slashes. Nothing in the lifecycle may treat them
     * specially. The empty topic is degenerate but reachable through the same REST path.
     */
    @ParameterizedTest
    @ValueSource(strings = {"a/b", "a/b/c", "/", "//", "", "sport/tennis/+/#", "äöü/中文", "$SAMPLER::x"})
    public void test_lifecycleIsIndifferentToTheShapeOfTheTopic(final String topic) {
        final String clientId = SAMPLER_PREFIX + topic;

        samplingService.startSampling(topic);
        assertTrue(samplingService.isSampling(topic));
        verify(topicTree, times(1)).addTopic(eq(clientId), any(), anyByte(), eq(clientId));

        samplingService.stopSampling(topic);
        assertFalse(samplingService.isSampling(topic));
        verify(topicTree, times(1)).removeSubscriber(eq(clientId), eq(topic), eq(clientId));
    }

    @Test
    public void test_manyWatchersNeedAsManyReleases() {
        final int watchers = 25;
        for (int i = 0; i < watchers; i++) {
            samplingService.startSampling(TOPIC);
        }
        for (int i = 0; i < watchers - 1; i++) {
            samplingService.stopSampling(TOPIC);
            assertTrue(samplingService.isSampling(TOPIC), "release " + i + " must not unsubscribe");
        }
        verify(topicTree, never()).removeSubscriber(any(), any(), any());

        samplingService.stopSampling(TOPIC);
        assertFalse(samplingService.isSampling(TOPIC));
        verify(topicTree, times(1)).removeSubscriber(eq(CLIENT_ID), eq(TOPIC), eq(CLIENT_ID));
    }

    /**
     * The race the {@code compute} exists for, and the only one that matters.
     * <p>
     * A release that has decided to unsubscribe must not remove a subscriber that an overlapping
     * acquire has just added. The failure is silent and sticky: the count would say "watched" while
     * the topic tree says "not subscribed", so no samples would ever arrive and the clean-up would
     * rightly reclaim the queue.
     * <p>
     * Asserted against a topic tree that actually tracks state, so the invariant checked is the real
     * one — <em>if anybody is watching, a subscriber exists</em> — rather than a count of mock calls.
     * One-sided: it can only fail on a broken implementation, never spuriously on a correct one.
     */
    @Test
    @Timeout(120)
    public void test_overlappingAcquireAndReleaseNeverLeaveAWatchedTopicUnsubscribed() throws Exception {
        final Set<String> subscribed = ConcurrentHashMap.newKeySet();
        final LocalTopicTree statefulTree =
                mock(LocalTopicTree.class, withSettings().stubOnly());
        doAnswer(invocation -> subscribed.add(invocation.getArgument(0)))
                .when(statefulTree)
                .addTopic(any(), any(), anyByte(), any());
        doAnswer(invocation -> {
                    subscribed.remove(invocation.<String>getArgument(0));
                    return null;
                })
                .when(statefulTree)
                .removeSubscriber(any(), any(), any());

        final SamplingService service = new SamplingService(
                statefulTree, mock(ClientQueuePersistence.class, withSettings().stubOnly()));

        final int threads = 6;
        final int iterations = 20_000;
        final ExecutorService executor = Executors.newFixedThreadPool(threads);
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(threads);
        try {
            for (int t = 0; t < threads; t++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < iterations; i++) {
                            service.startSampling(TOPIC);
                            // while this thread holds a watch, the subscription must exist
                            if (!subscribed.contains(CLIENT_ID)) {
                                throw new AssertionError(
                                        "topic is watched but has no subscriber: samples would never arrive "
                                                + "and the clean-up would reclaim the queue");
                            }
                            service.stopSampling(TOPIC);
                        }
                    } catch (final Throwable e) {
                        failure.compareAndSet(null, e);
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(120, TimeUnit.SECONDS), "threads did not finish");
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }

        if (failure.get() != null) {
            throw new AssertionError("concurrent failure", failure.get());
        }
        // every acquire was paired, so nothing may remain watched or subscribed
        assertFalse(service.isSampling(TOPIC), "watchers leaked");
        assertTrue(subscribed.isEmpty(), "subscription leaked: this queue would never be reclaimed");
    }

    /**
     * Balanced concurrent acquire/release across several topics must leave nothing behind — a leaked
     * watcher is a sampler queue that grows forever, which is the defect EDG-885 exists to close.
     */
    @Test
    @Timeout(120)
    public void test_concurrentLifecyclesAcrossTopicsLeaveNothingBehind() throws Exception {
        final List<String> topics = List.of("a/b", "c", "plant/line1/+", "", "x/y/z");
        final ExecutorService executor = Executors.newFixedThreadPool(topics.size());
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(topics.size());
        final List<Throwable> failures = new ArrayList<>();
        try {
            for (final String topic : topics) {
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < 10_000; i++) {
                            samplingService.startSampling(topic);
                            samplingService.startSampling(topic);
                            samplingService.stopSampling(topic);
                            samplingService.stopSampling(topic);
                        }
                    } catch (final Throwable e) {
                        synchronized (failures) {
                            failures.add(e);
                        }
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(120, TimeUnit.SECONDS), "threads did not finish");
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }

        assertEquals(List.of(), failures);
        for (final String topic : topics) {
            assertFalse(samplingService.isSampling(topic), "watcher leaked for '" + topic + "'");
        }
    }
}
