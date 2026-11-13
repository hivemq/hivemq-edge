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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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
    void pollingSettings_whenOptionsAreNotSet_thenDefaultsAreUsed() {
        assertThat(pollingService().pollingSettings()).isEqualTo(PollingTask.Settings.DEFAULTS);
    }

    @Test
    void pollingSettings_defaultsLieInsideTheirRanges() {
        assertThat(InternalConfigurations.ADAPTER_POLLING_WATCHDOG_TIMEOUT_MILLIS_DEFAULT)
                .isBetween(
                        InternalConfigurations.ADAPTER_POLLING_WATCHDOG_TIMEOUT_MILLIS_MIN,
                        InternalConfigurations.ADAPTER_POLLING_WATCHDOG_TIMEOUT_MILLIS_MAX);
        assertThat(InternalConfigurations.ADAPTER_POLLING_WATCHDOG_ERRORS_BEFORE_STOP_DEFAULT)
                .isBetween(
                        InternalConfigurations.ADAPTER_POLLING_WATCHDOG_ERRORS_BEFORE_STOP_MIN,
                        InternalConfigurations.ADAPTER_POLLING_WATCHDOG_ERRORS_BEFORE_STOP_MAX);
        assertThat(InternalConfigurations.ADAPTER_POLLING_ERROR_BACKOFF_MAX_MILLIS_DEFAULT)
                .isBetween(
                        InternalConfigurations.ADAPTER_POLLING_ERROR_BACKOFF_MAX_MILLIS_MIN,
                        InternalConfigurations.ADAPTER_POLLING_ERROR_BACKOFF_MAX_MILLIS_MAX);
    }

    @Test
    void pollingSettings_watchdogTimeoutMinimumLiesAboveTheSamplerPollTimeout() {
        assertThat(InternalConfigurations.ADAPTER_POLLING_WATCHDOG_TIMEOUT_MILLIS_MIN)
                .isGreaterThan(AbstractSubscriptionSampler.POLL_OUTPUT_TIMEOUT_MILLIS);
    }

    @Test
    void pollingSettings_whenOptionsAreAtTheirMinimum_thenTheyAreUsed() {
        internalConfigurationService.set(InternalConfigurations.ADAPTER_POLLING_WATCHDOG_TIMEOUT_MILLIS, "11000");
        internalConfigurationService.set(InternalConfigurations.ADAPTER_POLLING_WATCHDOG_ERRORS_BEFORE_STOP, "0");
        internalConfigurationService.set(InternalConfigurations.ADAPTER_POLLING_ERROR_BACKOFF_MAX_MILLIS, "1000");

        assertThat(pollingService().pollingSettings()).isEqualTo(new PollingTask.Settings(11_000, 0, 1000));
    }

    @Test
    void pollingSettings_whenOptionsAreAtTheirMaximum_thenTheyAreUsed() {
        internalConfigurationService.set(InternalConfigurations.ADAPTER_POLLING_WATCHDOG_TIMEOUT_MILLIS, "3600000");
        internalConfigurationService.set(InternalConfigurations.ADAPTER_POLLING_WATCHDOG_ERRORS_BEFORE_STOP, "10000");
        internalConfigurationService.set(InternalConfigurations.ADAPTER_POLLING_ERROR_BACKOFF_MAX_MILLIS, "104857600");

        assertThat(pollingService().pollingSettings())
                .isEqualTo(new PollingTask.Settings(3_600_000, 10_000, 104_857_600));
    }

    @ParameterizedTest
    @CsvSource({
        "adapter.polling.watchdog.timeout.millis, 10999",
        "adapter.polling.watchdog.timeout.millis, 3600001",
        "adapter.polling.watchdog.timeout.millis, 1",
        "adapter.polling.watchdog.timeout.millis, -1",
        "adapter.polling.watchdog.errors.before.stop, -1",
        "adapter.polling.watchdog.errors.before.stop, 10001",
        "adapter.polling.error.backoff.max.millis, 999",
        "adapter.polling.error.backoff.max.millis, 104857601",
        "adapter.polling.error.backoff.max.millis, 0",
    })
    void pollingSettings_whenAnOptionIsOutsideItsRange_thenItsDefaultIsUsed(
            final @NotNull String key, final @NotNull String value) {
        internalConfigurationService.set(key, value);

        assertThat(pollingService().pollingSettings()).isEqualTo(PollingTask.Settings.DEFAULTS);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                InternalConfigurations.ADAPTER_POLLING_WATCHDOG_TIMEOUT_MILLIS,
                InternalConfigurations.ADAPTER_POLLING_WATCHDOG_ERRORS_BEFORE_STOP,
                InternalConfigurations.ADAPTER_POLLING_ERROR_BACKOFF_MAX_MILLIS
            })
    void pollingSettings_whenAnOptionIsNotANumber_thenItsDefaultIsUsed(final @NotNull String key) {
        internalConfigurationService.set(key, "ten");

        assertThat(pollingService().pollingSettings()).isEqualTo(PollingTask.Settings.DEFAULTS);
    }

    @Test
    void pollingSettings_whenAnOptionHasSurroundingWhitespace_thenItIsUsed() {
        internalConfigurationService.set(InternalConfigurations.ADAPTER_POLLING_WATCHDOG_TIMEOUT_MILLIS, " 20000 ");

        assertThat(pollingService().pollingSettings().watchdogTimeoutMillis()).isEqualTo(20_000);
    }

    @Test
    void schedulePolling_whenWatchdogLimitIsSet_thenTaskStopsAtIt() {
        internalConfigurationService.set(InternalConfigurations.ADAPTER_POLLING_WATCHDOG_ERRORS_BEFORE_STOP, "0");
        when(sampler.execute()).thenReturn(CompletableFuture.failedFuture(new InterruptedException()));

        scheduledTask().run();

        // an interrupted poll is a watchdog hit, and a limit of 0 stops at the first one
        verify(sampler).error(any(), eq(false));
    }

    @Test
    void schedulePolling_whenWatchdogLimitIsBelowMinimum_thenTaskUsesDefault() {
        internalConfigurationService.set(InternalConfigurations.ADAPTER_POLLING_WATCHDOG_ERRORS_BEFORE_STOP, "-1");
        when(sampler.execute()).thenReturn(CompletableFuture.failedFuture(new InterruptedException()));

        scheduledTask().run();

        // with the default limit of 10 the first watchdog hit continues polling; -1 would have stopped it
        verify(sampler).error(any(), eq(true));
    }

    @Test
    void schedulePolling_whenWatchdogTimeoutIsShorterThanTheSamplerPollTimeout_thenItIsNotApplied() {
        internalConfigurationService.set(InternalConfigurations.ADAPTER_POLLING_WATCHDOG_TIMEOUT_MILLIS, "1000");

        // a watchdog shorter than the samplers' poll timeout would fire first and count a hung poll as a watchdog hit
        assertThat(scheduledTask().settings().watchdogTimeoutMillis())
                .isEqualTo(InternalConfigurations.ADAPTER_POLLING_WATCHDOG_TIMEOUT_MILLIS_DEFAULT);
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
    void schedulePolling_whenBackoffMaxIsSet_thenTaskUsesIt() {
        internalConfigurationService.set(InternalConfigurations.ADAPTER_POLLING_ERROR_BACKOFF_MAX_MILLIS, "2000");

        // without the option the last delays would be 10 minutes
        assertThat(delaysAfterFailedPolls())
                .allMatch(delay -> delay <= 2000L)
                .last()
                .isEqualTo(2000L);
    }

    @Test
    void schedulePolling_whenBackoffMaxIsBelowMinimum_thenTaskUsesDefault() {
        internalConfigurationService.set(InternalConfigurations.ADAPTER_POLLING_ERROR_BACKOFF_MAX_MILLIS, "0");

        assertThat(delaysAfterFailedPolls()).last().isEqualTo((long)
                InternalConfigurations.ADAPTER_POLLING_ERROR_BACKOFF_MAX_MILLIS_DEFAULT);
    }

    private @NotNull List<Long> delaysAfterFailedPolls() {
        when(sampler.getMaxErrorsBeforeRemoval()).thenReturn(-1);
        when(sampler.execute()).thenReturn(CompletableFuture.failedFuture(new RuntimeException()));
        final PollingTask task = scheduledTask();

        for (int i = 0; i < 15; i++) {
            task.run();
        }

        // the initial schedule plus 15 reschedules
        final ArgumentCaptor<Long> delays = ArgumentCaptor.forClass(Long.class);
        verify(scheduledExecutorService, times(16)).schedule(same(task), delays.capture(), eq(TimeUnit.MILLISECONDS));
        return delays.getAllValues();
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

    private @NotNull ProtocolAdapterPollingServiceImpl pollingService() {
        return new ProtocolAdapterPollingServiceImpl(
                scheduledExecutorService,
                mock(ShutdownHooks.class),
                eventService,
                nanoTimeProvider,
                internalConfigurationService);
    }

    private @NotNull PollingTask scheduledTask() {
        pollingService().schedulePolling(sampler);
        final ArgumentCaptor<PollingTask> task = ArgumentCaptor.forClass(PollingTask.class);
        verify(scheduledExecutorService).schedule(task.capture(), anyLong(), eq(TimeUnit.MILLISECONDS));
        return task.getValue();
    }
}
