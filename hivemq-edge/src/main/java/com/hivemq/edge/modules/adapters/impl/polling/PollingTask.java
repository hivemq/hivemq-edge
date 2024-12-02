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
package com.hivemq.edge.modules.adapters.impl.polling;

import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingSampler;
import org.jetbrains.annotations.NotNull;
import com.hivemq.util.ExceptionUtils;
import com.hivemq.util.NanoTimeProvider;
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

public class PollingTask implements Runnable {

    private static final @NotNull Logger log = LoggerFactory.getLogger(PollingTask.class);

    private final @NotNull ProtocolAdapterPollingSampler sampler;
    private final @NotNull ScheduledExecutorService scheduledExecutorService;
    private final @NotNull EventService eventService;
    private final @NotNull NanoTimeProvider nanoTimeProvider;
    private final @NotNull AtomicInteger watchdogErrorCount = new AtomicInteger();
    private final @NotNull AtomicInteger applicationErrorCount = new AtomicInteger();

    private volatile long nanosOfLastPolling;
    private final @NotNull AtomicBoolean continueScheduling = new AtomicBoolean(true);


    public PollingTask(
            final @NotNull ProtocolAdapterPollingSampler sampler,
            final @NotNull ScheduledExecutorService scheduledExecutorService,
            final @NotNull EventService eventService,
            final @NotNull NanoTimeProvider nanoTimeProvider) {
        this.sampler = sampler;
        this.scheduledExecutorService = scheduledExecutorService;
        this.eventService = eventService;
        this.nanoTimeProvider = nanoTimeProvider;
    }

    @Override
    public void run() {
        try {
            nanosOfLastPolling = nanoTimeProvider.nanoTime();
            if (!continueScheduling.get()) {
                return;
            }
            final CompletableFuture<?> localExecutionFuture = sampler.execute()
                    .orTimeout(InternalConfigurations.ADAPTER_RUNTIME_JOB_EXECUTION_TIMEOUT_MILLIS.get(),
                            TimeUnit.MILLISECONDS);
            localExecutionFuture.whenComplete((aVoid, throwable) -> {
                if (throwable == null) {
                    resetErrorStats();
                    reschedule(0);
                } else {
                    if (ExceptionUtils.isInterruptedException(throwable)) {
                        handleInterruptionException(throwable);
                    } else {
                        handleExceptionDuringPolling(throwable);
                    }
                }
            });
        } catch (Throwable t) {
            // the sampler shouldn't throw a exception, but better safe than sorry as we might to miss rescheduling the task otherwise.
            handleExceptionDuringPolling(t);
        }
    }

    public void stopScheduling() {
        continueScheduling.set(false);
    }

    private void handleInterruptionException(final @NotNull Throwable throwable) {
        //-- Job was killed by the framework as it took too long
        //-- Do not call back to the job here (notify) since it will
        //-- Not respond and we dont want to block other polls
        int errorCountTotal = watchdogErrorCount.incrementAndGet();
        boolean stopBecauseOfTooManyErrors =
                errorCountTotal > InternalConfigurations.ADAPTER_RUNTIME_WATCHDOG_TIMEOUT_ERRORS_BEFORE_INTERRUPT.get();
        final long milliSecondsSinceLastPoll = TimeUnit.NANOSECONDS.toMillis(nanoTimeProvider.nanoTime() - nanosOfLastPolling);
        if (stopBecauseOfTooManyErrors) {
            log.warn(
                    "Detected bad system process {} in sampler {} - terminating process to maintain health ({}ms runtime)",
                    errorCountTotal,
                    sampler.getAdapterId(),
                    milliSecondsSinceLastPoll);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(
                        "Detected bad system process {} in sampler {} - interrupted process to maintain health ({}ms runtime)",
                        errorCountTotal,
                        sampler.getAdapterId(),
                        milliSecondsSinceLastPoll);
            }
        }
        notifyOnError(sampler, throwable, !stopBecauseOfTooManyErrors);
        if (!stopBecauseOfTooManyErrors) {
            reschedule(errorCountTotal);
        }
    }


    private void handleExceptionDuringPolling(final @NotNull Throwable throwable) {
        int errorCountTotal = applicationErrorCount.incrementAndGet();
        final int maxErrorsBeforeRemoval = sampler.getMaxErrorsBeforeRemoval();
        // case 1: Unlimited retry (maxErrorsBeforeRemoval < 0) or less errors than the limit
        if (maxErrorsBeforeRemoval < 0 || errorCountTotal <= maxErrorsBeforeRemoval) {
            if (log.isDebugEnabled()) {
                log.debug("Application Error {} in sampler {} -> {}",
                        errorCountTotal,
                        sampler.getAdapterId(),
                        throwable.getMessage(),
                        throwable);
            }
            reschedule(errorCountTotal);
            notifyOnError(sampler, throwable, true);
        } else {
            log.info(
                    "Detected '{}' recent errors when sampling. This exceeds the configured limit of '{}'. Sampling for adapter with id '{}' gets stopped.",
                    errorCountTotal,
                    maxErrorsBeforeRemoval,
                    sampler.getAdapterId());
            notifyOnError(sampler, throwable, false);
            // no rescheduling
        }

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
        final long delayInMillis = sampler.getPeriod() - pollDuration;
        // a negative delay means that the last polling attempt took longer to be processed than the specified delay between polls
        if (delayInMillis < 0) {
            log.warn(
                    "Polling for protocol adapter '{}' can not keep up with the specified '{}' interval, because the polling takes too long.",
                    sampler.getAdapterId(),
                    sampler.getPeriod());
            eventService.createAdapterEvent(sampler.getAdapterId(), sampler.getProtocolId())
                    .withMessage(String.format(
                            "Polling for protocol adapter '%s' can not keep up with the specified '%d' ms interval, because the polling takes too long.",
                            sampler.getAdapterId(),
                            sampler.getPeriod()))
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

    private void resetErrorStats() {
        applicationErrorCount.set(0);
        watchdogErrorCount.set(0);
    }

    private static long getBackoff(int errorCount, long max) {
        //-- This will backoff up to a max of about a day (unless the max provided is less)
        long f = (long) (Math.pow(2, Math.min(errorCount, 20)) * 100);
        f += ThreadLocalRandom.current().nextInt(0, errorCount * 100);
        f = Math.min(f, max);
        return f;
    }
}
