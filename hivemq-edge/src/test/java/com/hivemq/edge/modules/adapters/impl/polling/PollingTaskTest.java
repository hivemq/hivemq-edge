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

import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.protocols.AbstractSubscriptionSampler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PollingTaskTest {


    private final @NotNull AbstractSubscriptionSampler sampler = mock();
    private final @NotNull ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    @BeforeEach
    void setUp() {
        when(sampler.getInitialDelay()).thenReturn(0L);
        when(sampler.getPeriod()).thenReturn(0L);
        when(sampler.execute()).thenReturn(CompletableFuture.completedFuture(null));
        when(sampler.getMaxErrorsBeforeRemoval()).thenReturn(-1);
        when(sampler.getAdapterId()).thenReturn("test-adapter-1");
    }

    @AfterEach
    void tearDown() {
        scheduledExecutorService.shutdown();
    }

    @Test
    void when_executorServiceIsShutdown_thenNoException() throws InterruptedException {
        final PollingTask pollingTask = new PollingTask(sampler, scheduledExecutorService);
        pollingTask.run();
        scheduledExecutorService.shutdown();
        Thread.sleep(10_000);
    }

    @Test
    void run_whenSampleExecutionThrowsErrorMoreThanLimitedTimes_thenTaskIsRescheduledMaxErrorTimes() {
        final ScheduledExecutorService mockedExecutor = mock();
        when(sampler.getMaxErrorsBeforeRemoval()).thenReturn(3);

        when(sampler.execute()).thenThrow(new RuntimeException());
        final PollingTask pollingTask = new PollingTask(sampler, mockedExecutor);

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
    void run_whenSampleExecutionThrowsError_thenTaskIsRescheduled() {
        final ScheduledExecutorService mockedExecutor = mock();
        when(sampler.execute()).thenThrow(new RuntimeException());
        final PollingTask pollingTask = new PollingTask(sampler, mockedExecutor);

        pollingTask.run();

        verify(mockedExecutor, times(1)).schedule(same(pollingTask), anyLong(), eq(TimeUnit.MILLISECONDS));
    }


    @Test
    void run_whenSampleExecutionReturnsExceptionalFuture_thenTaskIsRescheduled() {
        final ScheduledExecutorService mockedExecutor = mock();
        when(sampler.execute()).thenReturn(CompletableFuture.failedFuture(new RuntimeException()));
        final PollingTask pollingTask = new PollingTask(sampler, mockedExecutor);

        pollingTask.run();

        verify(mockedExecutor, times(1)).schedule(same(pollingTask), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void run_whenSampleExecutionExceedsTime_thenTaskIsRescheduled() throws InterruptedException {
        InternalConfigurations.ADAPTER_RUNTIME_JOB_EXECUTION_TIMEOUT_MILLIS.set(1);
        final ScheduledExecutorService mockedExecutor = mock();
        when(sampler.execute()).thenReturn(new CompletableFuture<>());
        final PollingTask pollingTask = new PollingTask(sampler, mockedExecutor);

        pollingTask.run();
        Thread.sleep(2_000);
        verify(mockedExecutor, times(1)).schedule(same(pollingTask), anyLong(), eq(TimeUnit.MILLISECONDS));
    }


    @Test
    void schedule_whenTaskShouldBeScheduled_thenTaskGetsGetsScheduled() {
        ScheduledExecutorService mockedExecutor = mock();
        final PollingTask pollingTask = new PollingTask(sampler, mockedExecutor);

        pollingTask.schedule(1);

        verify(mockedExecutor, times(1)).schedule(same(pollingTask), eq(1L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void schedule_whenExecutorServiceIsShutdown_thenExceptionIsHandled() {
        scheduledExecutorService.shutdownNow();
        final PollingTask pollingTask = new PollingTask(sampler, scheduledExecutorService);

        pollingTask.schedule(1);
    }

    @Test
    void schedule_whenTaskShouldNotBeScheduled_thenTaskGetsGetsScheduled() {
        ScheduledExecutorService mockedExecutor = mock();
        final PollingTask pollingTask = new PollingTask(sampler, mockedExecutor);
        pollingTask.stopScheduling();

        pollingTask.schedule(1);

        verify(mockedExecutor, never()).schedule(same(pollingTask), eq(1L), eq(TimeUnit.MILLISECONDS));
    }


}