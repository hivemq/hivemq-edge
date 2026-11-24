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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.edge.modules.api.events.model.EventBuilderImpl;
import com.hivemq.protocols.AbstractSubscriptionSampler;
import com.hivemq.util.NanoTimeProvider;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class PollingTaskTest {

    private final @NotNull AbstractSubscriptionSampler sampler = mock();
    private final @NotNull ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    private final @NotNull EventService eventService = mock();
    private final @NotNull NanoTimeProvider nanoTimeProvider = mock();

    @BeforeEach
    void setUp() {
        when(sampler.getInitialDelay()).thenReturn(0L);
        when(sampler.getPeriod()).thenReturn(0L);
        when(sampler.execute()).thenReturn(CompletableFuture.completedFuture(null));
        when(sampler.getMaxErrorsBeforeRemoval()).thenReturn(-1);
        when(sampler.getAdapterId()).thenReturn("test-adapter-1");
        when(sampler.getProtocolId()).thenReturn("test-protocol");
        when(eventService.createAdapterEvent(any(), any())).thenReturn(new EventBuilderImpl((event) -> {}));
        when(nanoTimeProvider.nanoTime()).thenReturn(0L, 1000L);
    }

    @AfterEach
    void tearDown() {
        scheduledExecutorService.shutdown();
    }

    @Test
    void run_whenSampleExecutionThrowsErrorMoreThanLimitedTimes_thenTaskIsRescheduledMaxErrorTimes() {
        final ScheduledExecutorService mockedExecutor = mock();
        when(sampler.getMaxErrorsBeforeRemoval()).thenReturn(3);

        when(sampler.execute()).thenThrow(new RuntimeException());
        final PollingTask pollingTask = new PollingTask(sampler, mockedExecutor, eventService, nanoTimeProvider);

        pollingTask.run();
        verify(mockedExecutor, times(1)).schedule(same(pollingTask), anyLong(), eq(TimeUnit.MILLISECONDS));
        pollingTask.run();
        verify(mockedExecutor, times(2)).schedule(same(pollingTask), anyLong(), eq(TimeUnit.MILLISECONDS));
        pollingTask.run();
        verify(mockedExecutor, times(3)).schedule(same(pollingTask), anyLong(), eq(TimeUnit.MILLISECONDS));
        pollingTask.run();
        verify(mockedExecutor, times(3)).schedule(same(pollingTask), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void run_whenSampleExecutionTakesTooLong_thenTaskIsRescheduledMaxErrorTimes() throws InterruptedException {
        final ScheduledExecutorService mockedExecutor = mock();
        when(sampler.getMaxErrorsBeforeRemoval()).thenReturn(1);
        when(sampler.execute()).thenReturn(new CompletableFuture<>());
        final PollingTask pollingTask =
                new PollingTask(sampler, mockedExecutor, eventService, nanoTimeProvider, watchdog(1, 1));

        pollingTask.run();
        await().until(() -> {
            try {
                verify(mockedExecutor, times(1)).schedule(same(pollingTask), anyLong(), eq(TimeUnit.MILLISECONDS));
            } catch (Exception e) {
                return false;
            }
            return true;
        });

        pollingTask.run();
        Thread.sleep(500);
        verify(mockedExecutor, times(1)).schedule(same(pollingTask), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void run_whenSampleExecutionGetsInterrupted_thenTaskIsRescheduledMaxErrorTimes() {
        final ScheduledExecutorService mockedExecutor = mock();
        when(sampler.getMaxErrorsBeforeRemoval()).thenReturn(1);
        when(sampler.execute()).thenReturn(CompletableFuture.failedFuture(new InterruptedException()));
        final PollingTask pollingTask =
                new PollingTask(sampler, mockedExecutor, eventService, nanoTimeProvider, watchdog(60_000, 1));

        pollingTask.run();
        verify(mockedExecutor, times(1)).schedule(same(pollingTask), anyLong(), eq(TimeUnit.MILLISECONDS));

        pollingTask.run();
        verify(mockedExecutor, times(1)).schedule(same(pollingTask), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void run_whenSampleExecutionThrowsError_thenTaskIsRescheduled() {
        final ScheduledExecutorService mockedExecutor = mock();
        when(sampler.execute()).thenThrow(new RuntimeException());
        final PollingTask pollingTask = new PollingTask(sampler, mockedExecutor, eventService, nanoTimeProvider);

        pollingTask.run();

        verify(mockedExecutor, times(1)).schedule(same(pollingTask), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void run_whenSampleExecutionReturnsExceptionalFuture_thenTaskIsRescheduled() {
        final ScheduledExecutorService mockedExecutor = mock();
        when(sampler.execute()).thenReturn(CompletableFuture.failedFuture(new RuntimeException()));
        final PollingTask pollingTask = new PollingTask(sampler, mockedExecutor, eventService, nanoTimeProvider);

        pollingTask.run();

        verify(mockedExecutor, times(1)).schedule(same(pollingTask), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void run_whenSampleExecutionExceedsTime_thenTaskIsRescheduled() {
        final ScheduledExecutorService mockedExecutor = mock();
        when(sampler.execute()).thenReturn(new CompletableFuture<>());
        final PollingTask pollingTask =
                new PollingTask(sampler, mockedExecutor, eventService, nanoTimeProvider, watchdog(1, 10));

        pollingTask.run();
        await().until(() -> {
            try {
                verify(mockedExecutor, times(1)).schedule(same(pollingTask), anyLong(), eq(TimeUnit.MILLISECONDS));
            } catch (Exception e) {
                return false;
            }
            return true;
        });
    }

    @Test
    void run_whenSamplerTimesOutMoreThanWatchdogLimit_andRetriesAreUnlimited_thenTaskKeepsRescheduling() {
        final ScheduledExecutorService mockedExecutor = mock();
        when(sampler.getMaxErrorsBeforeRemoval()).thenReturn(-1);
        when(sampler.execute()).thenReturn(CompletableFuture.failedFuture(new TimeoutException()));
        final PollingTask pollingTask =
                new PollingTask(sampler, mockedExecutor, eventService, nanoTimeProvider, watchdog(60_000, 10));

        for (int i = 1; i <= 12; i++) {
            pollingTask.run();
            verify(mockedExecutor, times(i)).schedule(same(pollingTask), anyLong(), eq(TimeUnit.MILLISECONDS));
        }
        verify(sampler, never()).error(any(), eq(false));
    }

    @Test
    void run_whenSamplerTimesOut_thenTimeoutCountsAgainstMaxErrorsBeforeRemoval() {
        final ScheduledExecutorService mockedExecutor = mock();
        when(sampler.getMaxErrorsBeforeRemoval()).thenReturn(2);
        when(sampler.execute()).thenReturn(CompletableFuture.failedFuture(new TimeoutException()));
        final PollingTask pollingTask =
                new PollingTask(sampler, mockedExecutor, eventService, nanoTimeProvider, watchdog(60_000, 10));

        pollingTask.run();
        pollingTask.run();
        pollingTask.run();

        verify(mockedExecutor, times(2)).schedule(same(pollingTask), anyLong(), eq(TimeUnit.MILLISECONDS));
        verify(sampler, times(1)).error(any(), eq(false));
    }

    @Test
    void run_whenWatchdogFiresMoreThanLimit_andRetriesAreUnlimited_thenTaskKeepsRescheduling() {
        final ScheduledExecutorService mockedExecutor = mock();
        when(sampler.getMaxErrorsBeforeRemoval()).thenReturn(-1);
        when(sampler.execute()).thenAnswer(invocation -> new CompletableFuture<>());
        final PollingTask pollingTask =
                new PollingTask(sampler, mockedExecutor, eventService, nanoTimeProvider, watchdog(1, 1));

        for (int i = 1; i <= 3; i++) {
            pollingTask.run();
            final int expectedSchedules = i;
            await().untilAsserted(() -> verify(mockedExecutor, times(expectedSchedules))
                    .schedule(same(pollingTask), anyLong(), eq(TimeUnit.MILLISECONDS)));
        }
        verify(sampler, never()).error(any(), eq(false));
    }

    @Test
    void run_whenWatchdogFires_thenSamplerFutureIsNotCompletedByTheWatchdog() {
        final ScheduledExecutorService mockedExecutor = mock();
        final CompletableFuture<Object> samplerFuture = new CompletableFuture<>();
        when(sampler.execute()).thenAnswer(invocation -> samplerFuture);
        final PollingTask pollingTask =
                new PollingTask(sampler, mockedExecutor, eventService, nanoTimeProvider, watchdog(1, 10));

        pollingTask.run();

        await().untilAsserted(() ->
                verify(mockedExecutor, times(1)).schedule(same(pollingTask), anyLong(), eq(TimeUnit.MILLISECONDS)));
        assertThat(samplerFuture).isNotDone();
    }

    /// A poll a built-in Modbus or PLC4X adapter did not carry out (it ended the poll with a cancellation, wrapped as
    /// the sampler's future passes it on) leaves the error count as it is: at a limit of 1, a failed poll, a poll
    /// not carried out and another failed poll stop polling, as two failed polls in a row would.
    @ParameterizedTest
    @ValueSource(strings = {"modbus", "s7", "ads"})
    void run_whenAPollIsNotCarriedOutBetweenTwoFailures_thenTheErrorCountIsKeptAndPollingStops(
            final @NotNull String protocolId) {
        final ScheduledExecutorService mockedExecutor = mock();
        when(sampler.getProtocolId()).thenReturn(protocolId);
        when(sampler.getMaxErrorsBeforeRemoval()).thenReturn(1);
        final Iterator<CompletableFuture<?>> outcomes =
                List.<CompletableFuture<?>>of(busy(), notCarriedOut(), busy()).iterator();
        when(sampler.execute()).thenAnswer(_ -> outcomes.next());
        final PollingTask pollingTask = new PollingTask(sampler, mockedExecutor, eventService, nanoTimeProvider);

        pollingTask.run();
        pollingTask.run();
        pollingTask.run();

        verify(mockedExecutor, times(2)).schedule(same(pollingTask), anyLong(), eq(TimeUnit.MILLISECONDS));
        verify(sampler, times(1)).error(any(), eq(true));
        verify(sampler, times(1)).error(any(), eq(false));
    }

    /// A poll not carried out after a failure does not stop polling at a limit of 1, and a successful poll after it
    /// resets the count: one busy answer recovers.
    @ParameterizedTest
    @ValueSource(strings = {"modbus", "s7", "ads"})
    void run_whenAPollIsNotCarriedOutAfterAFailure_thenTheNextSuccessfulPollResetsTheCount(
            final @NotNull String protocolId) {
        final ScheduledExecutorService mockedExecutor = mock();
        when(sampler.getProtocolId()).thenReturn(protocolId);
        when(sampler.getMaxErrorsBeforeRemoval()).thenReturn(1);
        final Iterator<CompletableFuture<?>> outcomes = List.<CompletableFuture<?>>of(
                        busy(), notCarriedOut(), CompletableFuture.completedFuture(null), busy())
                .iterator();
        when(sampler.execute()).thenAnswer(_ -> outcomes.next());
        final PollingTask pollingTask = new PollingTask(sampler, mockedExecutor, eventService, nanoTimeProvider);

        pollingTask.run();
        pollingTask.run();
        pollingTask.run();
        pollingTask.run();

        verify(mockedExecutor, times(4)).schedule(same(pollingTask), anyLong(), eq(TimeUnit.MILLISECONDS));
        verify(sampler, never()).error(any(), eq(false));
        verify(sampler, times(2)).error(any(), eq(true));
    }

    /// A cancelled request from any other adapter, HTTP or a third party, is an ordinary polling error: with a limit
    /// of 0, polling stops at the first cancellation, as it did before polls not carried out existed.
    @ParameterizedTest
    @ValueSource(strings = {"http", "test-protocol"})
    void run_whenAnotherAdaptersRequestIsCancelled_thenItCountsAndALimitOfZeroStopsPolling(
            final @NotNull String protocolId) {
        final ScheduledExecutorService mockedExecutor = mock();
        when(sampler.getProtocolId()).thenReturn(protocolId);
        when(sampler.getMaxErrorsBeforeRemoval()).thenReturn(0);
        when(sampler.execute()).thenAnswer(_ -> notCarriedOut());
        final PollingTask pollingTask = new PollingTask(sampler, mockedExecutor, eventService, nanoTimeProvider);

        pollingTask.run();

        verify(mockedExecutor, never()).schedule(any(Runnable.class), anyLong(), any());
        verify(sampler, times(1)).error(any(), eq(false));
    }

    private static @NotNull CompletableFuture<Object> busy() {
        return CompletableFuture.failedFuture(new RuntimeException("busy"));
    }

    private static @NotNull CompletableFuture<Object> notCarriedOut() {
        return CompletableFuture.failedFuture(
                new CompletionException(new CancellationException("Reconnected too late to read within this poll")));
    }

    /// A poll whose failure arrives after `stopScheduling()` is dropped: the sampler, whose wrapper a restarted run of
    /// the same adapter may use, is not notified, and nothing is rescheduled.
    @Test
    void run_whenPollFailsAfterStopScheduling_thenSamplerIsNotNotifiedAndNothingIsRescheduled() {
        final ScheduledExecutorService mockedExecutor = mock();
        final CompletableFuture<Object> samplerFuture = new CompletableFuture<>();
        when(sampler.getMaxErrorsBeforeRemoval()).thenReturn(0);
        when(sampler.execute()).thenAnswer(_ -> samplerFuture);
        final PollingTask pollingTask = new PollingTask(sampler, mockedExecutor, eventService, nanoTimeProvider);
        pollingTask.run();

        pollingTask.stopScheduling();
        samplerFuture.completeExceptionally(new RuntimeException("late failure"));

        verify(sampler, never()).error(any(), anyBoolean());
        verify(mockedExecutor, never()).schedule(any(Runnable.class), anyLong(), any());
    }

    /// A poll the watchdog gives up on after `stopScheduling()` is dropped: the sampler is not notified and nothing is
    /// rescheduled.
    @Test
    void run_whenWatchdogFiresAfterStopScheduling_thenSamplerIsNotNotifiedAndNothingIsRescheduled() {
        final ScheduledExecutorService mockedExecutor = mock();
        when(sampler.getMaxErrorsBeforeRemoval()).thenReturn(0);
        when(sampler.execute()).thenAnswer(_ -> new CompletableFuture<>());
        final PollingTask pollingTask =
                new PollingTask(sampler, mockedExecutor, eventService, nanoTimeProvider, watchdog(100, 0));
        pollingTask.run();

        pollingTask.stopScheduling();

        await().during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            verify(sampler, never()).error(any(), anyBoolean());
            verify(mockedExecutor, never()).schedule(any(Runnable.class), anyLong(), any());
        });
    }

    /// A poll that succeeds after `stopScheduling()` is dropped: it neither reschedules nor reports that it cannot keep
    /// up with the period.
    @Test
    void run_whenPollSucceedsAfterStopScheduling_thenNothingIsRescheduledOrReported() {
        final ScheduledExecutorService mockedExecutor = mock();
        final CompletableFuture<Object> samplerFuture = new CompletableFuture<>();
        // the poll takes 4 ms against a period of 1 ms, so a task that was not stopped reports it
        when(sampler.getPeriod()).thenReturn(1L);
        when(nanoTimeProvider.nanoTime()).thenReturn(0L, TimeUnit.MILLISECONDS.toNanos(4));
        when(sampler.execute()).thenAnswer(_ -> samplerFuture);
        final PollingTask pollingTask = new PollingTask(sampler, mockedExecutor, eventService, nanoTimeProvider);
        pollingTask.run();

        pollingTask.stopScheduling();
        samplerFuture.complete(null);

        verify(mockedExecutor, never()).schedule(any(Runnable.class), anyLong(), any());
        verify(eventService, never()).createAdapterEvent(any(), any());
    }

    /// A sampler that throws after `stopScheduling()` is dropped as well: the synchronous failure path does not
    /// notify the sampler or reschedule.
    @Test
    void run_whenSamplerThrowsAfterStopScheduling_thenSamplerIsNotNotifiedAndNothingIsRescheduled() {
        final ScheduledExecutorService mockedExecutor = mock();
        when(sampler.getMaxErrorsBeforeRemoval()).thenReturn(0);
        final PollingTask pollingTask = new PollingTask(sampler, mockedExecutor, eventService, nanoTimeProvider);
        when(sampler.execute()).thenAnswer(_ -> {
            pollingTask.stopScheduling();
            throw new RuntimeException("failure while stopping");
        });

        pollingTask.run();

        verify(sampler, never()).error(any(), anyBoolean());
        verify(mockedExecutor, never()).schedule(any(Runnable.class), anyLong(), any());
    }

    @Test
    void run_whenPollsKeepFailing_thenDelayGrowsToDefaultBackoffMax() {
        assertThat(delaysAfterFailedPolls(PollingTask.Settings.DEFAULTS, 15))
                .last()
                .isEqualTo((long) InternalConfigurations.ADAPTER_POLLING_ERROR_BACKOFF_MAX_MILLIS_DEFAULT);
    }

    @Test
    void run_whenPollsKeepFailing_thenDelayStopsAtConfiguredBackoffMax() {
        final List<Long> delays = delaysAfterFailedPolls(new PollingTask.Settings(60_000, 10, 50), 15);

        assertThat(delays).allMatch(delay -> delay <= 50L);
        assertThat(delays).last().isEqualTo(50L);
    }

    private @NotNull List<Long> delaysAfterFailedPolls(final @NotNull PollingTask.Settings settings, final int polls) {
        final ScheduledExecutorService mockedExecutor = mock();
        when(sampler.execute()).thenReturn(CompletableFuture.failedFuture(new RuntimeException()));
        final PollingTask pollingTask =
                new PollingTask(sampler, mockedExecutor, eventService, nanoTimeProvider, settings);

        for (int i = 0; i < polls; i++) {
            pollingTask.run();
        }

        final ArgumentCaptor<Long> delays = ArgumentCaptor.forClass(Long.class);
        verify(mockedExecutor, times(polls)).schedule(same(pollingTask), delays.capture(), eq(TimeUnit.MILLISECONDS));
        return delays.getAllValues();
    }

    private static @NotNull PollingTask.Settings watchdog(final int timeoutMillis, final int errorsBeforeStop) {
        return new PollingTask.Settings(
                timeoutMillis,
                errorsBeforeStop,
                InternalConfigurations.ADAPTER_POLLING_ERROR_BACKOFF_MAX_MILLIS_DEFAULT);
    }

    @Test
    void schedule_whenTaskShouldBeScheduled_thenTaskGetsGetsScheduled() {
        ScheduledExecutorService mockedExecutor = mock();
        final PollingTask pollingTask = new PollingTask(sampler, mockedExecutor, eventService, nanoTimeProvider);

        pollingTask.schedule(1);

        verify(mockedExecutor, times(1)).schedule(same(pollingTask), eq(1L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void schedule_whenTaskShouldNotBeScheduled_thenTaskGetsGetsScheduled() {
        ScheduledExecutorService mockedExecutor = mock();
        final PollingTask pollingTask = new PollingTask(sampler, mockedExecutor, eventService, nanoTimeProvider);
        pollingTask.stopScheduling();

        pollingTask.schedule(1);

        verify(mockedExecutor, never()).schedule(same(pollingTask), eq(1L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void backoffMillis_doublesFromTheBaseWithJitterBelowCountTimesTheBase() {
        assertThat(PollingTask.backoffMillis(1, Long.MAX_VALUE)).isBetween(200L, 299L);
        assertThat(PollingTask.backoffMillis(3, Long.MAX_VALUE)).isBetween(800L, 1099L);
    }

    @Test
    void backoffMillis_whenDoublingsReachTheCeiling_thenItIsTheCeiling() {
        assertThat(PollingTask.backoffMillis(20, PollingTask.BACKOFF_CEILING_MILLIS))
                .isEqualTo(PollingTask.BACKOFF_CEILING_MILLIS);
        assertThat(PollingTask.backoffMillis(1000, PollingTask.BACKOFF_CEILING_MILLIS))
                .isEqualTo(PollingTask.BACKOFF_CEILING_MILLIS);
    }

    @Test
    void backoffMillis_whenTheJitterBoundWouldOverflowAnInt_thenTheMaxIsReturned() {
        // an adapter that retries without limit, with the minimum 1 s backoff, reaches 21 474 837 failures in a row
        // after about 250 days; an int jitter bound of count * 100 overflowed there, threw, and stopped polling
        for (final int errorCount : new int[] {Integer.MAX_VALUE / 100 + 1, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
            assertThat(PollingTask.backoffMillis(errorCount, 1000)).isEqualTo(1000L);
        }
    }

    @Test
    void run_whenFutureCompletes_thenTaskGetsExactlyScheduled() {
        ScheduledExecutorService mockedExecutor = mock();
        // period is exactly 2s
        when(sampler.getPeriod()).thenReturn(2000L);
        // polling takes exactly 4ms
        when(nanoTimeProvider.nanoTime()).thenReturn(0L, TimeUnit.MILLISECONDS.toNanos(4));
        // expected delay is 2s-4ms = 1996ms
        long expectedDelay = 1996L;
        final PollingTask pollingTask = new PollingTask(sampler, mockedExecutor, eventService, nanoTimeProvider);

        pollingTask.run();

        verify(mockedExecutor, times(1)).schedule(same(pollingTask), eq(expectedDelay), eq(TimeUnit.MILLISECONDS));
    }
}
