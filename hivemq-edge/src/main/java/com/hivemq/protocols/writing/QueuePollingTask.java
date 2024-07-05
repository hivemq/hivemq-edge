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
package com.hivemq.protocols.writing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.adapter.sdk.api.config.WriteContext;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingSampler;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.util.ExceptionUtils;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.hivemq.configuration.service.InternalConfigurations.PUBLISH_POLL_BATCH_SIZE_BYTES;

public class QueuePollingTask implements Runnable {

    private static final @NotNull Logger log = LoggerFactory.getLogger(QueuePollingTask.class);

    private final @NotNull WritingProtocolAdapter writingProtocolAdapter;
    private final @NotNull ScheduledExecutorService scheduledExecutorService;
    private final @NotNull EventService eventService;
    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull NanoTimeProvider nanoTimeProvider;
    private final @NotNull WriteTask writeTask;
    private final @NotNull String queueId;
    private final @NotNull SingleWriterService singleWriterService;
    private final @NotNull WriteContext writeContext;
    private final @NotNull AtomicInteger watchdogErrorCount = new AtomicInteger();
    private final @NotNull AtomicInteger applicationErrorCount = new AtomicInteger();

    private volatile long nanosOfLastPolling;
    private final @NotNull AtomicBoolean continueScheduling = new AtomicBoolean(true);


    public QueuePollingTask(
            final @NotNull WritingProtocolAdapter writingProtocolAdapter,
            final @NotNull ScheduledExecutorService scheduledExecutorService,
            final @NotNull EventService eventService,
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull NanoTimeProvider nanoTimeProvider,
            final @NotNull String queueId,
            final @NotNull SingleWriterService singleWriterService,
            final @NotNull WriteContext writeContext,
            final @NotNull ObjectMapper objectMapper) {
        this.writingProtocolAdapter = writingProtocolAdapter;
        this.scheduledExecutorService = scheduledExecutorService;
        this.eventService = eventService;
        this.clientQueuePersistence = clientQueuePersistence;
        this.nanoTimeProvider = nanoTimeProvider;
        this.writeTask =
                new WriteTask(writingProtocolAdapter, clientQueuePersistence, singleWriterService, objectMapper);
        this.queueId = queueId;
        this.singleWriterService = singleWriterService;
        this.writeContext = writeContext;
    }

    @Override
    public void run() {
        try {
            nanosOfLastPolling = nanoTimeProvider.nanoTime();
            if (!continueScheduling.get()) {
                return;
            }

            final ListenableFuture<ImmutableList<PUBLISH>> publishesFuture = pollForQueue(queueId);


            Futures.addCallback(publishesFuture, new FutureCallback<ImmutableList<PUBLISH>>() {
                @Override
                public void onSuccess(final ImmutableList<PUBLISH> publishes) {
                    if (publishes.isEmpty()) {
                        // TODO we should backoff and go to a sleeping state until another message is available
                        reschedule(0);
                    } else if (publishes.size() == 1) {
                        final CompletableFuture<Boolean> writeFuture =
                                writeTask.onMessage(publishes.get(0), queueId, writeContext);
                        writeFuture.whenComplete((success, t) -> {
                            if (t == null) {
                                reschedule(0);
                            } else {
                                if (ExceptionUtils.isInterruptedException(t)) {
                                    handleInterruptionException(t);
                                } else {
                                    handleExceptionDuringPolling(t);
                                }
                            }
                        });
                    } else {
                        log.warn("Only the consumption of one publish is currently possible");
                    }
                }

                @Override
                public void onFailure(final @NotNull Throwable t) {
                    handleInterruptionException(t);
                }
            }, scheduledExecutorService);


        } catch (Throwable t) {
            // the sampler shouldn't throw a exception, but better safe than sorry as we might to miss rescheduling the task otherwise.
            handleExceptionDuringPolling(t);
        }
    }

    public void stopScheduling() {
        continueScheduling.set(false);
    }

    private void handleInterruptionException(final @NotNull Throwable throwable) {
        int errorCountTotal = watchdogErrorCount.incrementAndGet();
        System.err.println("Interruption:");
        throwable.printStackTrace();
        // TODO log
        // TODO notifyOnError(sampler, throwable, !stopBecauseOfTooManyErrors);
        reschedule(errorCountTotal);
    }


    private void handleExceptionDuringPolling(final @NotNull Throwable throwable) {
        int errorCountTotal = applicationErrorCount.incrementAndGet();
        System.err.println("Exception:");
        throwable.printStackTrace();

        //TODO log and notify adapter
        reschedule(errorCountTotal);
    }

    private void notifyOnError(
            final @NotNull ProtocolAdapterPollingSampler sampler, final @NotNull Throwable t, boolean continuing) {
        try {
            sampler.error(t, continuing);
        } catch (Throwable samplerError) {
            if (log.isInfoEnabled()) {
                log.info("Sampler Encountered Error In Notification", samplerError);
            }
        }
    }

    private void reschedule(int errorCountTotal) {
        long pollDuration = TimeUnit.NANOSECONDS.toMillis(nanoTimeProvider.nanoTime() - nanosOfLastPolling);
        final long delayInMillis = writeContext.getWritingInterval() - pollDuration;
        // a negative delay means that the last polling attempt took longer to be processed than the specified delay between polls
        if (delayInMillis < 0) {
            log.warn(
                    "Polling for protocol adapter '{}' can not keep up with the specified '{}' interval, because the polling takes too long.",
                    writingProtocolAdapter.getId(),
                    writeContext.getWritingInterval());

            eventService.createAdapterEvent(writingProtocolAdapter.getId(),
                            writingProtocolAdapter.getProtocolAdapterInformation().getProtocolId())
                    .withMessage(String.format(
                            "Polling for protocol adapter '%s' can not keep up with the specified '%d' ms interval, because the polling takes too long.",
                            writingProtocolAdapter.getId(),
                            writeContext.getWritingInterval()))
                    .withSeverity(Event.SEVERITY.WARN)
                    .fire();
        }

        long nonNegativeDelay = Math.max(0, delayInMillis);

        if (errorCountTotal == 0) {
            schedule(nonNegativeDelay);
        } else {
            long backoff = getBackoff(errorCountTotal,
                    InternalConfigurations.ADAPTER_RUNTIME_MAX_APPLICATION_ERROR_BACKOFF.get());
            long effectiveDelay = Math.max(nonNegativeDelay, backoff);
            schedule(effectiveDelay);
        }
    }

    @VisibleForTesting
    void schedule(long nonNegativeDelay) {
        if (continueScheduling.get()) {
            try {
                scheduledExecutorService.schedule(this, nonNegativeDelay, TimeUnit.MILLISECONDS);
            } catch (RejectedExecutionException rejectedExecutionException) {
                // ignore. This is fine during shutdown.
            }
        }
    }

    private static long getBackoff(int errorCount, long max) {
        //-- This will backoff up to a max of about a day (unless the max provided is less)
        long f = (long) (Math.pow(2, Math.min(errorCount, 20)) * 100);
        f += ThreadLocalRandom.current().nextInt(0, errorCount * 100);
        f = Math.min(f, max);
        return f;
    }

    private @NotNull ListenableFuture<ImmutableList<PUBLISH>> pollForQueue(
            final @NotNull String queueId) {
        return clientQueuePersistence.readShared(queueId, 1, PUBLISH_POLL_BATCH_SIZE_BYTES);
    }
}
