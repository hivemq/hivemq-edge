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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import ch.qos.logback.classic.Level;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.LoggerFactory;

/**
 * The sampling lease (EDG-885): a topic stays sampled while somebody keeps asking for it, and is
 * released once nobody has for {@link InternalConfigurations#SAMPLING_LEASE_TTL_SEC}.
 * <p>
 * Time is a counter the test advances, so every case is deterministic and the sweep is driven by
 * hand; the only thing the real scheduler adds is <i>when</i> the sweep runs, and that is pinned
 * separately in {@link #test_theSweepIsScheduledAtTheConfiguredInterval()}.
 */
@SuppressWarnings("FutureReturnValueIgnored") // submitted work reports failures through the shared holder
public class SamplingServiceLeaseTest {

    private static final @NotNull String TOPIC = "plant/line1/from-plc";
    private static final @NotNull String CLIENT_ID = SAMPLER_PREFIX + TOPIC;
    private static final int TTL_SEC = 600;
    private static final long TTL_NANOS = TimeUnit.SECONDS.toNanos(TTL_SEC);

    private static Level previousSamplingLogLevel;
    private static int previousTtl;

    private final @NotNull AtomicLong clock = new AtomicLong(TimeUnit.HOURS.toNanos(1));
    private LocalTopicTree topicTree;
    private ClientQueuePersistence persistence;
    private SamplingService samplingService;

    @BeforeAll
    public static void silenceSamplingLoggingAndPinTheTtl() {
        final ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(SamplingService.class);
        previousSamplingLogLevel = logger.getLevel();
        logger.setLevel(Level.OFF);
        previousTtl = InternalConfigurations.SAMPLING_LEASE_TTL_SEC.get();
        InternalConfigurations.SAMPLING_LEASE_TTL_SEC.set(TTL_SEC);
    }

    /** Restores both so this class cannot affect others sharing the JVM. */
    @AfterAll
    public static void restoreSamplingLoggingAndTtl() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(SamplingService.class))
                .setLevel(previousSamplingLogLevel);
        InternalConfigurations.SAMPLING_LEASE_TTL_SEC.set(previousTtl);
    }

    @BeforeEach
    public void setUp() {
        topicTree = mock(LocalTopicTree.class);
        persistence = mock(ClientQueuePersistence.class);
        when(persistence.peek(anyString(), eq(true), anyLong(), anyInt()))
                .thenReturn(Futures.immediateFuture(ImmutableList.of()));
        samplingService = new SamplingService(topicTree, persistence, null, clock::get);
    }

    @AfterEach
    public void theTtlWasNotChangedByTheTest() {
        assertEquals(TTL_SEC, InternalConfigurations.SAMPLING_LEASE_TTL_SEC.get());
    }

    private void advance(final long nanos) {
        clock.addAndGet(nanos);
    }

    @Test
    public void test_aTopicNobodyAsksAboutIsReleasedOnceItsLeaseRunsOut() {
        samplingService.startSampling(TOPIC);

        advance(TTL_NANOS - 1);
        samplingService.expireLeases();
        assertTrue(samplingService.isSampling(TOPIC), "the lease has not run out yet");
        verify(topicTree, never()).removeSubscriber(any(), any(), any());

        advance(1);
        samplingService.expireLeases();
        assertFalse(samplingService.isSampling(TOPIC), "the lease ran out and nobody renewed it");
        verify(topicTree, times(1)).removeSubscriber(eq(CLIENT_ID), eq(TOPIC), eq(CLIENT_ID));
    }

    @Test
    public void test_readingTheSamplesRenewsTheLease() {
        samplingService.startSampling(TOPIC);

        // ask again just before every expiry: the topic must never be released
        for (int i = 0; i < 5; i++) {
            advance(TTL_NANOS - 1);
            samplingService.getSamples(TOPIC);
            samplingService.expireLeases();
            assertTrue(samplingService.isSampling(TOPIC), "renewed on read " + i);
        }
        verify(topicTree, times(1)).addTopic(eq(CLIENT_ID), any(), anyByte(), eq(CLIENT_ID));
        verify(topicTree, never()).removeSubscriber(any(), any(), any());

        // and once the reads stop, so does the sampling
        advance(TTL_NANOS);
        samplingService.expireLeases();
        assertFalse(samplingService.isSampling(TOPIC));
        verify(topicTree, times(1)).removeSubscriber(eq(CLIENT_ID), eq(TOPIC), eq(CLIENT_ID));
    }

    @Test
    public void test_startingAgainRenewsTheLeaseWithoutSubscribingAgain() {
        samplingService.startSampling(TOPIC);
        advance(TTL_NANOS - 1);
        samplingService.startSampling(TOPIC);
        advance(TTL_NANOS - 1);

        samplingService.expireLeases();

        assertTrue(samplingService.isSampling(TOPIC), "the second start moved the lease forward");
        verify(topicTree, times(1)).addTopic(eq(CLIENT_ID), any(), anyByte(), eq(CLIENT_ID));
        verify(topicTree, never()).removeSubscriber(any(), any(), any());
    }

    /**
     * A read must not start sampling: the start is admin-only, the reads are open to every role, and a
     * read that subscribed would hand the one to the other. A released topic stays released until it is
     * started again.
     */
    @Test
    public void test_readingAReleasedTopicDoesNotStartItAgain() {
        samplingService.startSampling(TOPIC);
        advance(TTL_NANOS);
        samplingService.expireLeases();
        assertFalse(samplingService.isSampling(TOPIC));

        samplingService.getSamples(TOPIC);

        assertFalse(samplingService.isSampling(TOPIC), "a read must not revive a released topic");
        verify(topicTree, times(1)).addTopic(eq(CLIENT_ID), any(), anyByte(), eq(CLIENT_ID));
        verify(topicTree, times(1)).removeSubscriber(eq(CLIENT_ID), eq(TOPIC), eq(CLIENT_ID));
    }

    @Test
    public void test_readingATopicThatWasNeverStartedDoesNotStartIt() {
        samplingService.getSamples(TOPIC);

        assertFalse(samplingService.isSampling(TOPIC));
        verify(topicTree, never()).addTopic(any(), any(), anyByte(), any());
    }

    @Test
    public void test_leasesAreIndependentPerTopic() {
        final String other = "plant/line2/from-plc";
        final String otherClientId = SAMPLER_PREFIX + other;
        samplingService.startSampling(TOPIC);
        samplingService.startSampling(other);

        advance(TTL_NANOS / 2);
        samplingService.getSamples(other);
        advance(TTL_NANOS / 2);
        samplingService.expireLeases();

        assertFalse(samplingService.isSampling(TOPIC), "not asked about for a whole TTL");
        assertTrue(samplingService.isSampling(other), "read half a TTL ago");
        verify(topicTree, times(1)).removeSubscriber(eq(CLIENT_ID), eq(TOPIC), eq(CLIENT_ID));
        verify(topicTree, never()).removeSubscriber(eq(otherClientId), eq(other), eq(otherClientId));
    }

    @Test
    public void test_aSweepWithNothingSampledDoesNothing() {
        samplingService.expireLeases();
        advance(TTL_NANOS * 10);
        samplingService.expireLeases();

        verify(topicTree, never()).removeSubscriber(any(), any(), any());
        verify(topicTree, never()).addTopic(any(), any(), anyByte(), any());
    }

    @Test
    public void test_theSweepHonoursTheConfiguredTtl() {
        InternalConfigurations.SAMPLING_LEASE_TTL_SEC.set(5);
        try {
            samplingService.startSampling(TOPIC);
            advance(TimeUnit.SECONDS.toNanos(4));
            samplingService.expireLeases();
            assertTrue(samplingService.isSampling(TOPIC));
            advance(TimeUnit.SECONDS.toNanos(1));
            samplingService.expireLeases();
            assertFalse(samplingService.isSampling(TOPIC));
        } finally {
            InternalConfigurations.SAMPLING_LEASE_TTL_SEC.set(TTL_SEC);
        }
    }

    /**
     * A periodic task that throws is silently never run again by a scheduled executor, which would
     * bring the leak back through the back door. One topic's failure must not take the sweep down.
     */
    @Test
    public void test_aFailureToReleaseOneTopicDoesNotStopTheSweep() {
        final String broken = "plant/broken";
        final String brokenClientId = SAMPLER_PREFIX + broken;
        doThrow(new IllegalStateException("simulated topic tree failure"))
                .when(topicTree)
                .removeSubscriber(eq(brokenClientId), eq(broken), eq(brokenClientId));
        samplingService.startSampling(broken);
        samplingService.startSampling(TOPIC);
        advance(TTL_NANOS);

        samplingService.expireLeases(); // must not throw

        assertFalse(samplingService.isSampling(TOPIC), "the healthy topic was released");
        assertTrue(samplingService.isSampling(broken), "the broken one is kept, to be retried");
        verify(topicTree, times(1)).removeSubscriber(eq(CLIENT_ID), eq(TOPIC), eq(CLIENT_ID));
    }

    /**
     * The one thing the real scheduler adds: the sweep runs by itself. A real executor, because the
     * Guava one is annotated {@code @DoNotMock}; the interval is pinned to a second so the wait is
     * short, and the clock is advanced by hand so the TTL, not wall time, decides expiry.
     */
    @Test
    @Timeout(30)
    public void test_theSweepRunsOnTheSchedulerWithoutBeingAskedTo() throws Exception {
        final int previousInterval = InternalConfigurations.SAMPLING_LEASE_SWEEP_INTERVAL_SEC.get();
        InternalConfigurations.SAMPLING_LEASE_SWEEP_INTERVAL_SEC.set(1);
        final ListeningScheduledExecutorService scheduler =
                MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());
        try {
            final SamplingService service = new SamplingService(topicTree, persistence, scheduler, clock::get);
            service.postConstruct();
            service.startSampling(TOPIC);
            advance(TTL_NANOS);

            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
            while (service.isSampling(TOPIC) && System.nanoTime() < deadline) {
                Thread.sleep(50);
            }
            assertFalse(service.isSampling(TOPIC), "the scheduled sweep released the expired topic");
            verify(topicTree, times(1)).removeSubscriber(eq(CLIENT_ID), eq(TOPIC), eq(CLIENT_ID));
        } finally {
            scheduler.shutdownNow();
            InternalConfigurations.SAMPLING_LEASE_SWEEP_INTERVAL_SEC.set(previousInterval);
        }
    }

    /**
     * A scheduled executor that is already shut down rejects new work with an exception. Construction
     * during shutdown must not turn that into a failed injection.
     */
    @Test
    public void test_nothingIsScheduledOnAnExecutorThatIsAlreadyShutDown() {
        final ListeningScheduledExecutorService scheduler =
                MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());
        scheduler.shutdownNow();
        final SamplingService service = new SamplingService(topicTree, persistence, scheduler, clock::get);

        service.postConstruct(); // must not throw RejectedExecutionException
    }

    /**
     * Renewals racing the sweep. The clock is frozen at "everything has expired", so every sweep
     * wants to release the topic while other threads keep reading it. Whatever interleaving happens,
     * this service and the topic tree must agree at the end, and the service must still work.
     */
    @Test
    @Timeout(120)
    public void test_renewalsRacingTheSweepLeaveTheTwoRecordsAgreeing() throws Exception {
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
        final ClientQueuePersistence stubPersistence =
                mock(ClientQueuePersistence.class, withSettings().stubOnly());
        when(stubPersistence.peek(anyString(), eq(true), anyLong(), anyInt()))
                .thenReturn(Futures.immediateFuture(ImmutableList.of()));
        // a clock that always reads "one TTL later than the last renewal": every sweep sees expiry
        final AtomicLong ticks = new AtomicLong();
        final SamplingService service =
                new SamplingService(statefulTree, stubPersistence, null, () -> ticks.addAndGet(TTL_NANOS));

        final int readers = 4;
        final int iterations = 20_000;
        // readers renew, one starter keeps re-starting what the sweep releases, one sweeper releases
        final ExecutorService executor = Executors.newFixedThreadPool(readers + 2);
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(readers + 2);
        try {
            for (int t = 0; t < readers; t++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < iterations; i++) {
                            service.getSamples(TOPIC);
                        }
                    } catch (final Throwable e) {
                        failure.compareAndSet(null, e);
                    } finally {
                        done.countDown();
                    }
                });
            }
            executor.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < iterations; i++) {
                        service.startSampling(TOPIC);
                    }
                } catch (final Throwable e) {
                    failure.compareAndSet(null, e);
                } finally {
                    done.countDown();
                }
            });
            executor.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < iterations; i++) {
                        service.expireLeases();
                    }
                } catch (final Throwable e) {
                    failure.compareAndSet(null, e);
                } finally {
                    done.countDown();
                }
            });
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
                service.isSampling(TOPIC),
                subscribed.contains(CLIENT_ID),
                "this service and the topic tree disagree about whether the topic is sampled");

        // and the service is still usable afterwards: the storm left no wedged state
        service.startSampling(TOPIC);
        assertTrue(service.isSampling(TOPIC));
        assertTrue(subscribed.contains(CLIENT_ID));
        service.expireLeases();
        assertFalse(service.isSampling(TOPIC));
        assertFalse(subscribed.contains(CLIENT_ID));
    }
}
