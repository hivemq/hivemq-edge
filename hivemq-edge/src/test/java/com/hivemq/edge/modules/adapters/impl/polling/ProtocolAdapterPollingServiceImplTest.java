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

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.configuration.service.impl.InternalConfigurationServiceImpl;
import com.hivemq.edge.modules.api.events.model.EventBuilderImpl;
import com.hivemq.protocols.AbstractSubscriptionSampler;
import com.hivemq.util.NanoTimeProvider;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProtocolAdapterPollingServiceImplTest {

    private final @NotNull AbstractSubscriptionSampler sampler = mock();
    private final @NotNull ScheduledExecutorService scheduledExecutorService = mock();
    private final @NotNull EventService eventService = mock();
    private final @NotNull NanoTimeProvider nanoTimeProvider = mock();
    private final @NotNull InternalConfigurationServiceImpl internalConfigurationService =
            new InternalConfigurationServiceImpl();

    @BeforeEach
    void setUp() {
        when(sampler.getInitialDelay()).thenReturn(0L);
        when(sampler.getUnit()).thenReturn(TimeUnit.MILLISECONDS);
        when(sampler.getMaxErrorsBeforeRemoval()).thenReturn(5);
        when(sampler.getAdapterId()).thenReturn("test-adapter-1");
        when(sampler.getProtocolId()).thenReturn("test-protocol");
        when(sampler.execute()).thenAnswer(invocation -> new CompletableFuture<>());
        when(eventService.createAdapterEvent(any(), any())).thenReturn(new EventBuilderImpl(event -> {}));
        when(nanoTimeProvider.nanoTime()).thenReturn(0L);
    }

    @Test
    void schedulePolling_whenWatchdogOptionsAreSet_thenTaskUsesThem() {
        internalConfigurationService.set(InternalConfigurations.ADAPTER_POLLING_WATCHDOG_TIMEOUT_MILLIS, "1");
        internalConfigurationService.set(InternalConfigurations.ADAPTER_POLLING_WATCHDOG_ERRORS_BEFORE_STOP, "0");

        scheduledTask().run();

        // a 1 ms watchdog fires on the never-completing poll, and a limit of 0 stops at the first interruption
        await().untilAsserted(() -> verify(sampler).error(any(), eq(false)));
    }

    @Test
    void schedulePolling_whenWatchdogLimitIsBelowMinimum_thenTaskUsesDefault() {
        internalConfigurationService.set(InternalConfigurations.ADAPTER_POLLING_WATCHDOG_TIMEOUT_MILLIS, "1");
        internalConfigurationService.set(InternalConfigurations.ADAPTER_POLLING_WATCHDOG_ERRORS_BEFORE_STOP, "-1");

        scheduledTask().run();

        // with the default limit of 10 the first interruption continues polling; -1 would have stopped it
        await().untilAsserted(() -> verify(sampler).error(any(), eq(true)));
    }

    @Test
    void schedulePolling_whenWatchdogOptionsAreNotSet_thenTaskStopsAfterDefaultLimit() {
        assertStopsAfterDefaultWatchdogLimit();
    }

    @Test
    void schedulePolling_whenWatchdogOptionIsNotANumber_thenTaskStopsAfterDefaultLimit() {
        internalConfigurationService.set(InternalConfigurations.ADAPTER_POLLING_WATCHDOG_ERRORS_BEFORE_STOP, "ten");
        assertStopsAfterDefaultWatchdogLimit();
    }

    @Test
    void schedulePolling_whenWatchdogOptionHasSurroundingWhitespace_thenTaskUsesIt() {
        internalConfigurationService.set(InternalConfigurations.ADAPTER_POLLING_WATCHDOG_ERRORS_BEFORE_STOP, " 0 ");
        when(sampler.execute()).thenReturn(CompletableFuture.failedFuture(new InterruptedException()));

        scheduledTask().run();

        verify(sampler).error(any(), eq(false));
    }

    private void assertStopsAfterDefaultWatchdogLimit() {
        // an interrupted poll is a watchdog hit that completes synchronously
        when(sampler.execute()).thenReturn(CompletableFuture.failedFuture(new InterruptedException()));
        final PollingTask task = scheduledTask();

        for (int i = 0; i <= InternalConfigurations.ADAPTER_POLLING_WATCHDOG_ERRORS_BEFORE_STOP_DEFAULT; i++) {
            task.run();
        }

        // the initial schedule plus one reschedule for each of the 10 hits the limit allows; the 11th stops
        verify(scheduledExecutorService, times(11)).schedule(same(task), anyLong(), eq(TimeUnit.MILLISECONDS));
        verify(sampler, times(10)).error(any(), eq(true));
        verify(sampler, times(1)).error(any(), eq(false));
    }

    private @NotNull PollingTask scheduledTask() {
        final ProtocolAdapterPollingServiceImpl pollingService = new ProtocolAdapterPollingServiceImpl(
                scheduledExecutorService,
                mock(ShutdownHooks.class),
                eventService,
                nanoTimeProvider,
                internalConfigurationService);
        pollingService.schedulePolling(sampler);
        final ArgumentCaptor<PollingTask> task = ArgumentCaptor.forClass(PollingTask.class);
        verify(scheduledExecutorService).schedule(task.capture(), anyLong(), eq(TimeUnit.MILLISECONDS));
        return task.getValue();
    }
}
