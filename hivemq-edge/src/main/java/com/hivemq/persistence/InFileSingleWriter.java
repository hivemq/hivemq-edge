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
package com.hivemq.persistence;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hivemq.configuration.service.InternalConfigurationService;
import com.hivemq.configuration.service.InternalConfigurations;
import org.jetbrains.annotations.NotNull;
import com.hivemq.util.Exceptions;
import com.hivemq.util.ThreadFactoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import java.util.SplittableRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.hivemq.configuration.service.InternalConfigurations.PERSISTENCE_BUCKET_COUNT;
import static com.hivemq.configuration.service.InternalConfigurations.SINGLE_WRITER_INTERVAL_TO_CHECK_PENDING_TASKS_AND_SCHEDULE_MSEC;

/**
 * @author Lukas Brandl
 */
public class InFileSingleWriter implements SingleWriterService {

    private static final @NotNull Logger log = LoggerFactory.getLogger(InFileSingleWriter.class);

    private static final int AMOUNT_OF_PRODUCERS = 5;
    private static final int RETAINED_MESSAGE_QUEUE_INDEX = 0;
    private static final int CLIENT_SESSION_QUEUE_INDEX = 1;
    private static final int SUBSCRIPTION_QUEUE_INDEX = 2;
    private static final int QUEUED_MESSAGES_QUEUE_INDEX = 3;
    private static final int ATTRIBUTE_STORE_QUEUE_INDEX = 4;

    private final int persistenceBucketCount;
    private final int threadPoolSize;
    private final int creditsPerExecution;
    private final long shutdownGracePeriod;

    private final @NotNull AtomicLong nonemptyQueueCounter = new AtomicLong(0);
    private final @NotNull AtomicInteger runningThreadsCount = new AtomicInteger(0);
    private final @NotNull AtomicLong globalTaskCount = new AtomicLong(0);

    private final @NotNull ProducerQueuesImpl @NotNull [] producers = new ProducerQueuesImpl[AMOUNT_OF_PRODUCERS];

    @VisibleForTesting
    @NotNull
    ExecutorService singleWriterExecutor;

    @VisibleForTesting
    final @NotNull ScheduledExecutorService checkScheduler;

    @Inject
    public InFileSingleWriter(final @NotNull InternalConfigurationService internalConfigurationService) {
        persistenceBucketCount = internalConfigurationService.getInteger(PERSISTENCE_BUCKET_COUNT);
        threadPoolSize =
                internalConfigurationService.getInteger(InternalConfigurations.FILE_SINGLE_WRITER_THREAD_POOL_SIZE);
        log.info("Allocating {} threads for file single writer.", threadPoolSize);
        creditsPerExecution = InternalConfigurations.SINGLE_WRITER_CREDITS_PER_EXECUTION.get();
        shutdownGracePeriod = InternalConfigurations.PERSISTENCE_SHUTDOWN_GRACE_PERIOD_MSEC.get();

        final ThreadFactory threadFactory = ThreadFactoryUtil.create("single-writer-%d");
        singleWriterExecutor = Executors.newFixedThreadPool(threadPoolSize, threadFactory);

        final int amountOfQueues = validAmountOfQueues(threadPoolSize, persistenceBucketCount);

        for (int i = 0; i < producers.length; i++) {
            producers[i] = new ProducerQueuesImpl(this, amountOfQueues);
        }

        final ThreadFactory checkThreadFactory =
                new ThreadFactoryBuilder().setNameFormat("single-writer-scheduled-check-%d").build();
        checkScheduler = Executors.newSingleThreadScheduledExecutor(checkThreadFactory);
    }

    @PostConstruct
    public void postConstruct() {
        // Periodically check if there are pending tasks in the queues
        checkScheduler.scheduleAtFixedRate(() -> {
                    try {

                        if (runningThreadsCount.getAndIncrement() == 0 && !singleWriterExecutor.isShutdown()) {
                            singleWriterExecutor.submit(new SingleWriterTask(nonemptyQueueCounter,
                                    globalTaskCount,
                                    runningThreadsCount,
                                    producers));
                        } else {
                            runningThreadsCount.decrementAndGet();
                        }
                    } catch (final Exception e) {
                        log.error("Exception in single writer check task ", e);
                    }
                },
                SINGLE_WRITER_INTERVAL_TO_CHECK_PENDING_TASKS_AND_SCHEDULE_MSEC.get(),
                SINGLE_WRITER_INTERVAL_TO_CHECK_PENDING_TASKS_AND_SCHEDULE_MSEC.get(),
                TimeUnit.MILLISECONDS);
    }

    @VisibleForTesting
    int validAmountOfQueues(final int processorCount, final int bucketCount) {
        for (int i = processorCount; i < bucketCount; i++) {
            if (bucketCount % i == 0) {
                return i;
            }
        }
        return persistenceBucketCount;
    }

    void incrementNonemptyQueueCounter() {
        nonemptyQueueCounter.incrementAndGet();

        if (runningThreadsCount.getAndIncrement() < threadPoolSize) {
            singleWriterExecutor.submit(new SingleWriterTask(nonemptyQueueCounter,
                    globalTaskCount,
                    runningThreadsCount,
                    producers));
        } else {
            runningThreadsCount.decrementAndGet();
        }
    }

    public void decrementNonemptyQueueCounter() {
        nonemptyQueueCounter.decrementAndGet();
    }

    public @NotNull ProducerQueues getRetainedMessageQueue() {
        return producers[RETAINED_MESSAGE_QUEUE_INDEX];
    }

    public @NotNull ProducerQueues getClientSessionQueue() {
        return producers[CLIENT_SESSION_QUEUE_INDEX];
    }

    public @NotNull ProducerQueues getSubscriptionQueue() {
        return producers[SUBSCRIPTION_QUEUE_INDEX];
    }

    public @NotNull ProducerQueues getQueuedMessagesQueue() {
        return producers[QUEUED_MESSAGES_QUEUE_INDEX];
    }

    public @NotNull ProducerQueues getAttributeStoreQueue() {
        return producers[ATTRIBUTE_STORE_QUEUE_INDEX];
    }

    public int getPersistenceBucketCount() {
        return persistenceBucketCount;
    }

    public int getCreditsPerExecution() {
        return creditsPerExecution;
    }

    public long getShutdownGracePeriod() {
        return shutdownGracePeriod;
    }

    public @NotNull AtomicLong getGlobalTaskCount() {
        return globalTaskCount;
    }

    public @NotNull AtomicLong getNonemptyQueueCounter() {
        return nonemptyQueueCounter;
    }

    public @NotNull AtomicInteger getRunningThreadsCount() {
        return runningThreadsCount;
    }

    public void stop() {
        final long start = System.currentTimeMillis();
        if (log.isTraceEnabled()) {
            log.trace("Shutting down single writer");
        }

        singleWriterExecutor.shutdown();

        try {
            singleWriterExecutor.awaitTermination(shutdownGracePeriod, TimeUnit.SECONDS);
            if (log.isTraceEnabled()) {
                log.trace("Finished single writer shutdown in {} ms", (System.currentTimeMillis() - start));
            }
        } catch (final InterruptedException e) {
            //ignore
        }
        singleWriterExecutor.shutdownNow();
        checkScheduler.shutdownNow();
    }

    private static class SingleWriterTask implements Runnable {

        private final @NotNull AtomicLong nonemptyQueueCounter;
        private final @NotNull AtomicLong globalTaskCount;
        private final @NotNull AtomicInteger runningThreadsCount;
        private final ProducerQueuesImpl @NotNull [] producers;
        final int @NotNull [] probabilities;

        private static final int MIN_PROBABILITY_IN_PERCENT = 5;

        private static final @NotNull SplittableRandom RANDOM = new SplittableRandom();

        SingleWriterTask(
                final @NotNull AtomicLong nonemptyQueueCounter,
                final @NotNull AtomicLong globalTaskCount,
                final @NotNull AtomicInteger runningThreadsCount,
                final ProducerQueuesImpl @NotNull [] producers) {

            this.nonemptyQueueCounter = nonemptyQueueCounter;
            this.globalTaskCount = globalTaskCount;
            this.runningThreadsCount = runningThreadsCount;
            this.producers = producers;
            probabilities = new int[producers.length];
        }

        public void run() {
            try {
                final SplittableRandom random = RANDOM.split();

                // It is possible that all tasks stop running while there are still non-empty queues.
                // We have yet to determine if there is a lock free way to avoid this.
                outerLoop:
                while (nonemptyQueueCounter.get() >= runningThreadsCount.getAndDecrement()) {
                    runningThreadsCount.incrementAndGet();
                    final long countSnapShot = globalTaskCount.get();
                    if (countSnapShot == 0) {
                        continue;
                    }

                    // Calculate the percentage portion of total tasks per persistence.
                    for (int i = 0; i < producers.length; i++) {
                        probabilities[i] = (int) ((producers[i].getTaskCount().get() * 100) / countSnapShot);
                    }

                    int sumWithoutMins = 0;
                    // Set to min probability if necessary
                    for (int i = 0; i < probabilities.length; i++) {
                        if (probabilities[i] < MIN_PROBABILITY_IN_PERCENT) {
                            probabilities[i] = MIN_PROBABILITY_IN_PERCENT;
                        } else {
                            sumWithoutMins += probabilities[i];
                        }
                    }

                    int surplus = 0;
                    for (int i = 0; i < probabilities.length; i++) {
                        surplus += probabilities[i];
                    }
                    surplus -= 100;

                    if (surplus > 0) { // Normalize to a 100% sum
                        // We reduce the probability of all persistences that are not at the minimum, be a portion of the overhead.
                        // The portion is based on there portion of the sum of all probabilities, ignoring those with minimum probability.
                        for (int i = 0; i < probabilities.length; i++) {
                            if (probabilities[i] > MIN_PROBABILITY_IN_PERCENT) {
                                probabilities[i] -= surplus / (sumWithoutMins / probabilities[i]);
                            }
                        }
                    }

                    final int randomInt = random.nextInt(100);
                    int offset = 0;

                    for (int i = 0; i < probabilities.length; i++) {
                        if (randomInt <= probabilities[i] + offset) {
                            producers[i].execute(random);
                            continue outerLoop;
                        }
                        offset += probabilities[i];
                    }
                }

            } catch (final Throwable t) {
                // Exceptions in the executed tasks, are passed to there result future.
                // So we only end up here if there is an exception in the probability calculation.
                // We decrement the running thread count so that a new thread will start running, as soon as a new task is added to any queue.
                runningThreadsCount.decrementAndGet();
                Exceptions.rethrowError("Exception in single writer executor. ", t);
            }
        }
    }
}
