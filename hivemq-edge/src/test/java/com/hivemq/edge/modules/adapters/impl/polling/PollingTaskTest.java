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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.edge.modules.api.events.model.EventBuilderImpl;
import com.hivemq.protocols.AbstractSubscriptionSampler;
import com.hivemq.util.NanoTimeProvider;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        final PollingTask pollingTask = new PollingTask(sampler, mockedExecutor, eventService, nanoTimeProvider, 1, 1);

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
                new PollingTask(sampler, mockedExecutor, eventService, nanoTimeProvider, 60_000, 1);

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
        final PollingTask pollingTask = new PollingTask(sampler, mockedExecutor, eventService, nanoTimeProvider, 1, 10);

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
                new PollingTask(sampler, mockedExecutor, eventService, nanoTimeProvider, 60_000, 10);

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
                new PollingTask(sampler, mockedExecutor, eventService, nanoTimeProvider, 60_000, 10);

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
        final PollingTask pollingTask = new PollingTask(sampler, mockedExecutor, eventService, nanoTimeProvider, 1, 1);

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
        final PollingTask pollingTask = new PollingTask(sampler, mockedExecutor, eventService, nanoTimeProvider, 1, 10);

        pollingTask.run();

        await().untilAsserted(() ->
                verify(mockedExecutor, times(1)).schedule(same(pollingTask), anyLong(), eq(TimeUnit.MILLISECONDS)));
        assertThat(samplerFuture).isNotDone();
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
