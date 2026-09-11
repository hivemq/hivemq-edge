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

import ch.qos.logback.classic.Level;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

/**
 * EDG-885 / EDG-882 F-06: what sampling's lifecycle actually is, rather than what it looked like.
 * <p>
 * It was written as a watcher count — subscribe on the first, unsubscribe on the last — but
 * {@code startSampling} is reached from a POST that carries no watcher identity, so retries, a
 * remounted panel and a second browser tab are indistinguishable, and each one incremented a count
 * that nothing ever decremented. No release added later could have balanced it, because it would have
 * no way to know which acquire it was balancing. What is left is the truthful shape: starting is
 * idempotent, stopping unsubscribes, and neither pretends to be reference-counted.
 * <p>
 * <b>Still open, deliberately:</b> nothing in production stops sampling, so a sampled topic keeps its
 * subscription and its ten-message queue for the life of the node. Closing that needs a release the
 * caller can be identified by, or a lease that expires — both decisions about the product's API
 * surface, both EDG-885's.
 */
@SuppressWarnings("FutureReturnValueIgnored") // submitted work reports failures through the shared holder
public class SamplingServiceLifecycleTest {

    private static final @NotNull String TOPIC = "plant/line1/from-plc";
    private static final @NotNull String CLIENT_ID = SAMPLER_PREFIX + TOPIC;

    private LocalTopicTree topicTree;
    private SamplingService samplingService;

    private static Level previousSamplingLogLevel;

    /**
     * Silences {@link SamplingService}'s own logging for the duration of this class.
     * <p>
     * Not cosmetic, and the second time this exact defect has bitten on this branch — see the same
     * guard in {@code MessageForwarderQueueOwnershipConcurrencyTest}. {@code startSampling} and
     * {@code stopSampling} log at DEBUG on every call, and the concurrency tests below make hundreds
     * of thousands of calls across a thread pool. Gradle captures that output into the JUnit XML,
     * which produced a <b>27 MB result file</b> whose CDATA section exceeds libxml2's limit: CI's
     * test-result reporter fails with {@code XMLSyntaxError: CData section too big} and reds the check
     * while every test passes. Leave this in place, or restore it if the logging here ever changes.
     */
    @BeforeAll
    public static void silenceSamplingLogging() {
        final ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(SamplingService.class);
        previousSamplingLogLevel = logger.getLevel();
        logger.setLevel(Level.OFF);
    }

    /** Restores the level so this class cannot affect others sharing the JVM. */
    @AfterAll
    public static void restoreSamplingLogging() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(SamplingService.class))
                .setLevel(previousSamplingLogLevel);
    }

    @BeforeEach
    public void setUp() {
        topicTree = mock(LocalTopicTree.class);
        samplingService = new SamplingService(topicTree, mock(ClientQueuePersistence.class));
    }

    @Test
    public void test_startingSubscribes_andStoppingUnsubscribes() {
        assertFalse(samplingService.isSampling(TOPIC));

        samplingService.startSampling(TOPIC);
        assertTrue(samplingService.isSampling(TOPIC));
        verify(topicTree, times(1)).addTopic(eq(CLIENT_ID), any(), anyByte(), eq(CLIENT_ID));

        samplingService.stopSampling(TOPIC);
        assertFalse(samplingService.isSampling(TOPIC));
        verify(topicTree, times(1)).removeSubscriber(eq(CLIENT_ID), eq(TOPIC), eq(CLIENT_ID));
    }

    /**
     * Starting is idempotent. A POST carries no watcher identity, so a retry, a remounted panel and a
     * second tab are the same request as far as this service can tell — and they must not each leave
     * something behind that has to be undone separately.
     */
    @Test
    public void test_startingAgainWhileSamplingChangesNothing() {
        samplingService.startSampling(TOPIC);
        samplingService.startSampling(TOPIC);
        samplingService.startSampling(TOPIC);

        assertTrue(samplingService.isSampling(TOPIC));
        verify(topicTree, times(1)).addTopic(eq(CLIENT_ID), any(), anyByte(), eq(CLIENT_ID));

        // and one stop is enough: repeated starts did not stack anything that survives it
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
    public void test_stoppingATopicNobodyIsSamplingIsANoOp() {
        samplingService.stopSampling(TOPIC);

        assertFalse(samplingService.isSampling(TOPIC));
        verify(topicTree, never()).removeSubscriber(any(), any(), any());

        // and nothing was corrupted by it: a later start still subscribes
        samplingService.startSampling(TOPIC);
        assertTrue(samplingService.isSampling(TOPIC));
        verify(topicTree, times(1)).addTopic(eq(CLIENT_ID), any(), anyByte(), eq(CLIENT_ID));
    }

    @Test
    public void test_stoppingTwiceIsANoOpTheSecondTime() {
        samplingService.startSampling(TOPIC);
        samplingService.stopSampling(TOPIC);
        samplingService.stopSampling(TOPIC);

        assertFalse(samplingService.isSampling(TOPIC));
        verify(topicTree, times(1)).removeSubscriber(eq(CLIENT_ID), eq(TOPIC), eq(CLIENT_ID));
    }

    @Test
    public void test_aTopicCanBeSampledAgainAfterItWasStopped() {
        samplingService.startSampling(TOPIC);
        samplingService.stopSampling(TOPIC);
        samplingService.startSampling(TOPIC);

        assertTrue(samplingService.isSampling(TOPIC));
        verify(topicTree, times(2)).addTopic(eq(CLIENT_ID), any(), anyByte(), eq(CLIENT_ID));
        verify(topicTree, times(1)).removeSubscriber(eq(CLIENT_ID), eq(TOPIC), eq(CLIENT_ID));
    }

    @Test
    public void test_topicsAreTrackedIndependently() {
        final String other = "plant/line2/from-plc";
        samplingService.startSampling(TOPIC);
        samplingService.startSampling(other);

        samplingService.stopSampling(TOPIC);

        assertFalse(samplingService.isSampling(TOPIC));
        assertTrue(samplingService.isSampling(other), "stopping one topic must not affect another");
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

    /**
     * Many starts, one stop. This is the case the counting got wrong in the direction that matters: a
     * browser that retried a POST twenty-five times would have needed twenty-five releases, and there
     * is no caller anywhere that could have issued them.
     */
    @Test
    public void test_manyStartsStillNeedOnlyOneStop() {
        for (int i = 0; i < 25; i++) {
            samplingService.startSampling(TOPIC);
        }
        verify(topicTree, times(1)).addTopic(eq(CLIENT_ID), any(), anyByte(), eq(CLIENT_ID));

        samplingService.stopSampling(TOPIC);

        assertFalse(samplingService.isSampling(TOPIC));
        verify(topicTree, times(1)).removeSubscriber(eq(CLIENT_ID), eq(TOPIC), eq(CLIENT_ID));
    }

    /**
     * The race the atomic {@code compute} exists for.
     * <p>
     * A stop must not tear down a subscription a start has just created in a way that leaves the two
     * records disagreeing: this service's map saying "sampled" while the topic tree says "not
     * subscribed" is silent and sticky — no samples would ever arrive, the clean-up would rightly
     * reclaim the queue, and nothing would re-register until the topic changed.
     * <p>
     * The invariant is asserted at quiescence, against a topic tree that actually tracks state, and it
     * is the strongest one that holds without watcher identity: <em>the two records agree</em>. During
     * the storm they cannot be compared, because a stop from another thread may legitimately end
     * sampling between any two instructions of this one — which is not a race but the honest
     * consequence of a POST that says "sample this" without saying who is asking. A release path that
     * lets one caller's stop not affect another needs a token to identify it, and that is EDG-885's
     * decision to make.
     */
    @Test
    @Timeout(120)
    public void test_overlappingStartAndStopLeaveTheTwoRecordsAgreeing() throws Exception {
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

        assertEquals(
                samplingServiceSaysSampled(service),
                subscribed.contains(CLIENT_ID),
                "this service and the topic tree disagree about whether the topic is sampled; samples "
                        + "would never arrive and nothing would re-register until the topic changed");

        // and the service is still usable afterwards: the storm left no wedged state
        service.startSampling(TOPIC);
        assertTrue(service.isSampling(TOPIC));
        assertTrue(subscribed.contains(CLIENT_ID));
        service.stopSampling(TOPIC);
        assertFalse(service.isSampling(TOPIC));
        assertFalse(subscribed.contains(CLIENT_ID));
    }

    private static boolean samplingServiceSaysSampled(final @NotNull SamplingService service) {
        return service.isSampling(TOPIC);
    }
}
