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
package com.hivemq.protocols;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.edge.modules.adapters.impl.polling.PollingTask;
import com.hivemq.edge.modules.api.events.model.EventBuilderImpl;
import com.hivemq.util.NanoTimeProvider;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/// The future a sampler hands to its polling task completes before the sampler logs and records the poll's outcome,
/// so a slow logger cannot turn a polling error the driver already reported into a watchdog hit.
class SamplerReportingOrderTest {

    /// The reporter sees the returned future already complete, even while it blocks.
    @Test
    void completeBeforeReporting_whenTheReporterBlocks_thenTheReturnedFutureIsAlreadyComplete() throws Exception {
        final CompletableFuture<Object> poll = new CompletableFuture<>();
        final CountDownLatch reporterEntered = new CountDownLatch(1);
        final CountDownLatch releaseReporter = new CountDownLatch(1);
        final AtomicBoolean doneWhenReporting = new AtomicBoolean();
        final AtomicReference<CompletableFuture<Void>> watched = new AtomicReference<>();
        watched.set(AbstractSubscriptionSampler.completeBeforeReporting(poll, (_, _) -> {
            doneWhenReporting.set(watched.get().isDone());
            reporterEntered.countDown();
            awaitQuietly(releaseReporter);
        }));

        final Thread driver = Thread.ofVirtual()
                .name("test-driver-poll-failure")
                .start(() -> poll.completeExceptionally(new TimeoutException()));
        try {
            assertThat(reporterEntered.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(doneWhenReporting.get()).isTrue();
            assertThat(watched.get()).isCompletedExceptionally();
        } finally {
            releaseReporter.countDown();
            driver.join(5_000);
        }
    }

    /// A successful poll completes the returned future normally, and the reporter sees no failure.
    @Test
    void completeBeforeReporting_whenThePollSucceeds_thenTheReturnedFutureSucceeds() {
        final AtomicReference<Throwable> reported = new AtomicReference<>(new AssertionError("not reported"));

        final CompletableFuture<Void> watched = AbstractSubscriptionSampler.completeBeforeReporting(
                CompletableFuture.completedFuture("ok"), (_, throwable) -> reported.set(throwable));

        assertThat(watched).isCompletedWithValue(null);
        assertThat(reported.get()).isNull();
    }

    /// A reporter that throws does not change the poll's outcome as the polling task sees it.
    @Test
    void completeBeforeReporting_whenTheReporterThrows_thenThePollOutcomeStands() {
        final CompletableFuture<Void> watched =
                AbstractSubscriptionSampler.completeBeforeReporting(CompletableFuture.completedFuture("ok"), (_, _) -> {
                    throw new IllegalStateException("appender failed");
                });

        assertThat(watched).isCompletedWithValue(null);
    }

    /// The poll's own exception reaches the polling task unchanged, so its classification stays as before.
    @Test
    void completeBeforeReporting_whenThePollFailsWrapped_thenTheSameExceptionIsRelayed() {
        final CompletionException failure = new CompletionException(new TimeoutException());

        final CompletableFuture<Void> watched = AbstractSubscriptionSampler.completeBeforeReporting(
                CompletableFuture.failedFuture(failure), (_, _) -> {});

        assertThatThrownBy(watched::join).isSameAs(failure);
    }

    /// The driver's own timeout fails the poll while the sampler's error logging blocks. The polling task counts a
    /// polling error (limit `maxErrorsBeforeRemoval`, 5 here) and keeps polling; it must not count a watchdog hit,
    /// whose limit of 0 here would stop the adapter at the first failure. Before the fix the polling task saw
    /// nothing until the logging returned.
    @Test
    void pollingTask_whenTheSamplersLoggingBlocks_thenAReportedFailureCountsAsAPollingErrorNotAWatchdogHit()
            throws Exception {
        final CompletableFuture<Object> poll = new CompletableFuture<>();
        final CountDownLatch releaseReporter = new CountDownLatch(1);
        final AbstractSubscriptionSampler sampler = mock();
        when(sampler.getMaxErrorsBeforeRemoval()).thenReturn(5);
        when(sampler.getAdapterId()).thenReturn("adapter-1");
        when(sampler.getProtocolId()).thenReturn("protocol-1");
        when(sampler.getUnit()).thenReturn(TimeUnit.MILLISECONDS);
        when(sampler.execute())
                .thenAnswer(_ -> AbstractSubscriptionSampler.completeBeforeReporting(
                        poll, (_, _) -> awaitQuietly(releaseReporter)));
        final EventService eventService = mock();
        when(eventService.createAdapterEvent(any(), any())).thenReturn(new EventBuilderImpl(_ -> {}));
        final ScheduledExecutorService scheduler = mock();
        final PollingTask task = new PollingTask(
                sampler,
                scheduler,
                eventService,
                mock(NanoTimeProvider.class),
                new PollingTask.Settings(
                        PollingTask.Settings.DEFAULTS.watchdogTimeoutMillis(),
                        0,
                        PollingTask.Settings.DEFAULTS.maxErrorBackoffMillis()));

        task.run();
        final Thread driver = Thread.ofVirtual()
                .name("test-driver-timeout")
                .start(() -> poll.completeExceptionally(new CompletionException(new TimeoutException("driver"))));
        try {
            verify(sampler, timeout(5_000)).error(any(), eq(true));
            verify(sampler, never()).error(any(), eq(false));
        } finally {
            releaseReporter.countDown();
            driver.join(5_000);
        }
    }

    private static void awaitQuietly(final @NotNull CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("reporter was never released");
            }
        } catch (final InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }
}
