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
package com.hivemq.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import ch.qos.logback.classic.Level;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.HivemqId;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.SingleWriterService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.LoggerFactory;

/**
 * Concurrency coverage for the reference-counted queue-ownership index (EDG-882).
 * <p>
 * Every test here is deliberately <b>one-sided</b>: it asserts a safety property that a correct
 * implementation can never violate, so it cannot fail spuriously on green code. What it cannot do is
 * guarantee detection — a race window a few instructions wide may simply not be hit on a given run.
 * The quiescent-state assertions (after all threads have joined) are fully deterministic; the
 * assertions made by the hammering reader threads are probabilistic detectors. Each test says which
 * of the two it is.
 * <p>
 * The property under test throughout is the one that matters to the customer: {@code
 * isForwarderQueue} must never answer {@code false} for a queue that a live forwarder owns, because
 * the periodic clean-up deletes what it believes to be unowned, irreversibly and without logging.
 * Answering {@code true} for a queue that has just become unowned is harmless — it is reclaimed on
 * the next sweep.
 */
@SuppressWarnings("FutureReturnValueIgnored") // submitted work reports failures through the shared list
public class MessageForwarderQueueOwnershipConcurrencyTest {

    private static final int WRITER_ITERATIONS = 20_000;
    private static final int CHURN_THREADS = 4;

    private MessageForwarderImpl messageForwarder;
    private ExecutorService executor;
    private static Level previousForwarderLogLevel;

    /**
     * Silences {@link MessageForwarderImpl}'s own logging for the duration of this class.
     * <p>
     * Not cosmetic. {@code addForwarder} and {@code removeForwarder} each log at INFO or WARN on every
     * call, and these tests make hundreds of thousands of calls. Gradle captures that output into the
     * JUnit XML, which produced a <b>155 MB result file</b> whose CDATA section exceeded libxml2's size
     * limit — CI's test-result reporter could not parse it and failed the build even though every test
     * passed. Leave this in place, or restore it if the logging here ever changes.
     */
    @BeforeAll
    public static void silenceForwarderLogging() {
        final ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(MessageForwarderImpl.class);
        previousForwarderLogLevel = logger.getLevel();
        logger.setLevel(Level.OFF);
    }

    /** Restores the level so this class cannot affect others sharing the JVM. */
    @AfterAll
    public static void restoreForwarderLogging() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(MessageForwarderImpl.class))
                .setLevel(previousForwarderLogLevel);
    }

    @BeforeEach
    public void setUp() {
        messageForwarder = new MessageForwarderImpl(
                mock(LocalTopicTree.class, withSettings().stubOnly()),
                new HivemqId(),
                () -> null,
                mock(SingleWriterService.class, withSettings().stubOnly()),
                mock(ShutdownHooks.class, withSettings().stubOnly()));
        executor = Executors.newCachedThreadPool();
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }

    /**
     * Stub-only so that hammering a mock several hundred thousand times does not accumulate recorded
     * invocations (which would exhaust the heap) and does not contend on Mockito's invocation
     * container across threads.
     */
    private static MqttForwarder forwarder(final @NotNull String id, final String... topics) {
        final MqttForwarder forwarder = mock(MqttForwarder.class, withSettings().stubOnly());
        when(forwarder.getId()).thenReturn(id);
        when(forwarder.getTopics()).thenReturn(List.of(topics));
        return forwarder;
    }

    private static String queueId(final @NotNull String forwarderId, final @NotNull String topic) {
        return MessageForwarderImpl.FORWARDER_PREFIX + forwarderId + "/" + topic;
    }

    /**
     * Runs {@code reader} continuously on its own thread while {@code writers} run, then joins
     * everything and rethrows the first failure any thread saw.
     */
    private void runConcurrently(final @NotNull Runnable reader, final @NotNull List<Runnable> writers)
            throws Exception {
        final AtomicBoolean writersDone = new AtomicBoolean(false);
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch finished = new CountDownLatch(writers.size() + 1);
        final List<Throwable> failures = java.util.Collections.synchronizedList(new ArrayList<>());

        executor.submit(() -> {
            try {
                start.await();
                while (!writersDone.get()) {
                    reader.run();
                }
                reader.run(); // one last look after the writers have stopped
            } catch (final Throwable t) {
                failures.add(t);
            } finally {
                finished.countDown();
            }
        });

        for (final Runnable writer : writers) {
            executor.submit(() -> {
                try {
                    start.await();
                    writer.run();
                } catch (final Throwable t) {
                    failures.add(t);
                } finally {
                    finished.countDown();
                }
            });
        }

        start.countDown();
        // writers finish on their own; flip the flag once all but the reader have counted down
        while (finished.getCount() > 1) {
            Thread.onSpinWait();
        }
        writersDone.set(true);
        assertTrue(finished.await(120, TimeUnit.SECONDS), "threads did not finish in time");

        if (!failures.isEmpty()) {
            final Throwable first = failures.get(0);
            if (first instanceof Error error) {
                throw error;
            }
            throw new AssertionError("concurrent failure: " + first, first);
        }
    }

    /**
     * O2 under contention, and the single most valuable test in this class.
     * <p>
     * A forwarder is re-registered over and over with two topic sets that <b>overlap</b> in one topic.
     * The overlapping queue is continuously owned throughout — its reference count goes 1 → 2 → 1 and
     * must never reach zero. Reversing {@code retain(new)} and {@code release(superseded)} makes it dip
     * to zero for an instant, and a clean-up sweep landing in that instant would clear a live queue.
     * <p>
     * Probabilistic detector for the reader assertion; the post-join assertion is deterministic.
     */
    @Test
    @Timeout(120)
    public void test_reregistration_never_exposes_a_topic_present_in_both_sets() throws Exception {
        final String id = "bridge-Bt80p78iNo/w7W1W7bGwcg==";
        final String keptTopic = "plant/line1/from-plc";
        final String queue = queueId(id, keptTopic);

        final MqttForwarder registrationA = forwarder(id, keptTopic, "plant/line1/alpha");
        final MqttForwarder registrationB = forwarder(id, keptTopic, "plant/line1/beta");
        messageForwarder.addForwarder(registrationA);

        final AtomicLong reads = new AtomicLong();
        final Runnable reader = () -> {
            reads.incrementAndGet();
            assertTrue(
                    messageForwarder.isForwarderQueue(queue),
                    "a topic kept across a re-registration read as unowned; a sweep here deletes live messages");
        };
        final Runnable writer = () -> {
            for (int i = 0; i < WRITER_ITERATIONS; i++) {
                messageForwarder.addForwarder(i % 2 == 0 ? registrationB : registrationA);
            }
        };

        runConcurrently(reader, List.of(writer));

        assertTrue(reads.get() > 0, "the reader never ran");
        assertTrue(messageForwarder.isForwarderQueue(queue));
    }

    /**
     * A queue owned by two <b>different</b> forwarders — reachable because a share name may contain
     * '/' — must stay owned while either of them is registered, no matter how hard the other churns.
     * <p>
     * Probabilistic detector for the reader assertion; the post-join assertion is deterministic.
     */
    @Test
    @Timeout(120)
    public void test_a_colliding_queue_stays_owned_while_one_owner_churns() throws Exception {
        final MqttForwarder stable = forwarder("a/b", "c");
        final MqttForwarder churning = forwarder("a", "b/c");
        final String collidingQueue = queueId("a", "b/c");
        assertEquals(collidingQueue, queueId("a/b", "c"), "the two registrations must collide");

        messageForwarder.addForwarder(stable);

        final Runnable reader = () -> assertTrue(
                messageForwarder.isForwarderQueue(collidingQueue),
                "a queue still owned by the stable forwarder read as unowned");
        final Runnable writer = () -> {
            for (int i = 0; i < WRITER_ITERATIONS; i++) {
                messageForwarder.addForwarder(churning);
                messageForwarder.removeForwarder(churning, false);
            }
        };

        runConcurrently(reader, List.of(writer));

        assertTrue(messageForwarder.isForwarderQueue(collidingQueue));
        messageForwarder.removeForwarder(stable, false);
        assertFalse(messageForwarder.isForwarderQueue(collidingQueue));
    }

    /**
     * A forwarder that is registered once and never removed must never read as unowned, whatever else
     * is being registered and unregistered around it.
     * <p>
     * Probabilistic detector for the reader assertion; the post-join assertion is deterministic.
     */
    @Test
    @Timeout(120)
    public void test_an_untouched_forwarder_is_unaffected_by_churn_on_others() throws Exception {
        final MqttForwarder stable = forwarder("stable-AbCd/EfGh==", "stable/topic");
        final String stableQueue = queueId("stable-AbCd/EfGh==", "stable/topic");
        messageForwarder.addForwarder(stable);

        final Runnable reader = () -> assertTrue(
                messageForwarder.isForwarderQueue(stableQueue), "a forwarder that was never removed read as unowned");

        final List<Runnable> writers = new ArrayList<>();
        for (int t = 0; t < CHURN_THREADS; t++) {
            final MqttForwarder churn = forwarder("churn-" + t + "-Xy/Zw==", "churn/" + t);
            writers.add(() -> {
                for (int i = 0; i < WRITER_ITERATIONS; i++) {
                    messageForwarder.addForwarder(churn);
                    messageForwarder.removeForwarder(churn, false);
                }
            });
        }

        runConcurrently(reader, writers);

        assertTrue(messageForwarder.isForwarderQueue(stableQueue));
    }

    /**
     * Reference-count pairing under contention: many threads register the <b>same</b> forwarder
     * concurrently, so many {@code retain}s race a single winning {@code put}. Each must still be
     * paired with exactly one {@code release}, or a single removal leaves the queue permanently
     * claimed and it is never reclaimed.
     * <p>
     * Fully deterministic — the assertion is made after every thread has joined.
     */
    @Test
    @Timeout(120)
    public void test_concurrent_registrations_of_one_forwarder_leave_no_residual_references() throws Exception {
        final String id = "bridge-Zz99/Yy88==";
        final String topic = "shared/topic";
        final String queue = queueId(id, topic);
        final MqttForwarder subject = forwarder(id, topic);

        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(CHURN_THREADS);
        for (int t = 0; t < CHURN_THREADS; t++) {
            executor.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < WRITER_ITERATIONS; i++) {
                        messageForwarder.addForwarder(subject);
                    }
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(120, TimeUnit.SECONDS), "registration threads did not finish");

        assertTrue(messageForwarder.isForwarderQueue(queue), "the forwarder is registered");

        // one removal must fully deregister, however many concurrent adds preceded it
        messageForwarder.removeForwarder(subject, false);
        assertFalse(
                messageForwarder.isForwarderQueue(queue),
                "references leaked: the queue can now never be reclaimed by the clean-up");
    }

    /**
     * The whole index must return to empty once every forwarder is unregistered, after arbitrary
     * concurrent add/remove interleavings — no leaked references (a queue that can never be reclaimed)
     * and no lost ones (re-registration must still work afterwards).
     * <p>
     * Fully deterministic — asserted at quiescence.
     */
    @Test
    @Timeout(120)
    public void test_index_returns_to_empty_after_concurrent_add_remove_storms() throws Exception {
        final List<String> ids = List.of("a", "a/b", "bridge-Ab/cd==", "bridge-Abcd==");
        final List<String> topics = List.of("b/c", "c", "t");

        final List<MqttForwarder> forwarders = new ArrayList<>();
        final List<String> universe = new ArrayList<>();
        for (final String id : ids) {
            forwarders.add(forwarder(id, topics.toArray(new String[0])));
            for (final String topic : topics) {
                universe.add(queueId(id, topic));
            }
        }

        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(forwarders.size());
        for (final MqttForwarder subject : forwarders) {
            executor.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < WRITER_ITERATIONS / 2; i++) {
                        messageForwarder.addForwarder(subject);
                        messageForwarder.removeForwarder(subject, false);
                    }
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(120, TimeUnit.SECONDS), "storm threads did not finish");

        // every forwarder ended on a removal, so nothing may remain claimed
        for (final String queue : universe) {
            assertFalse(
                    messageForwarder.isForwarderQueue(queue),
                    "reference leaked for " + queue + "; this queue would never be reclaimed");
        }

        // and the index must still be usable: counts must not have gone negative or been corrupted
        for (final MqttForwarder subject : forwarders) {
            messageForwarder.addForwarder(subject);
        }
        for (final String queue : universe) {
            assertTrue(messageForwarder.isForwarderQueue(queue), "re-registration failed for " + queue);
        }
    }
}
