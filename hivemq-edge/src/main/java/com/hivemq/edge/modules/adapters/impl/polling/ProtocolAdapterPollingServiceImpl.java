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

import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.InternalConfigurationService;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingSampler;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingService;
import com.hivemq.util.NanoTimeProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Krüger
 */
@Singleton
@SuppressWarnings("FutureReturnValueIgnored")
public class ProtocolAdapterPollingServiceImpl implements ProtocolAdapterPollingService {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ProtocolAdapterPollingServiceImpl.class);

    private final @NotNull ScheduledExecutorService scheduledExecutorService;
    private final @NotNull EventService eventService;
    private final @NotNull NanoTimeProvider nanoTimeProvider;
    private final @NotNull InternalConfigurationService internalConfigurationService;
    private final @NotNull Map<ProtocolAdapterPollingSampler, PollingTask> samplerToTask = new ConcurrentHashMap<>();
    // -- One adapter start schedules a sampler per tag; its samplers share the settings read for its first one, so
    // -- each option is read and warned about once per start, and a stop drops them so the next start reads again
    private final @NotNull Map<String, PollingTask.Settings> settingsByAdapterId = new ConcurrentHashMap<>();

    @Inject
    public ProtocolAdapterPollingServiceImpl(
            final @NotNull ScheduledExecutorService scheduledExecutorService,
            final @NotNull ShutdownHooks shutdownHooks,
            final @NotNull EventService eventService,
            final @NotNull NanoTimeProvider nanoTimeProvider,
            final @NotNull InternalConfigurationService internalConfigurationService) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.eventService = eventService;
        this.nanoTimeProvider = nanoTimeProvider;
        this.internalConfigurationService = internalConfigurationService;
        shutdownHooks.add(new Shutdown());
    }

    @Override
    public void schedulePolling(final @NotNull ProtocolAdapterPollingSampler sampler) {
        // -- Read outside the map: a WARN inside computeIfAbsent would hold the map's lock for other adapters while an
        // -- appender blocks. One start schedules its samplers from one thread, so it reads once
        PollingTask.Settings settings = settingsByAdapterId.get(sampler.getAdapterId());
        if (settings == null) {
            final PollingTask.Settings read = pollingSettings();
            final PollingTask.Settings earlier = settingsByAdapterId.putIfAbsent(sampler.getAdapterId(), read);
            settings = earlier != null ? earlier : read;
        }
        final PollingTask pollingTask =
                new PollingTask(sampler, scheduledExecutorService, eventService, nanoTimeProvider, settings);
        scheduledExecutorService.schedule(pollingTask, sampler.getInitialDelay(), sampler.getUnit());
        samplerToTask.put(sampler, pollingTask);
    }

    /**
     * The polling settings from the {@code <internal>} options, read once per adapter start (the first sampler it
     * schedules) so a changed option applies to the next adapter start. Each option has a range; a value that is not
     * an integer in its range falls back to the option's default with one WARN naming the range.
     */
    @VisibleForTesting
    @NotNull
    PollingTask.Settings pollingSettings() {
        return new PollingTask.Settings(
                internalOption(
                        InternalConfigurations.ADAPTER_POLLING_WATCHDOG_TIMEOUT_MILLIS,
                        InternalConfigurations.ADAPTER_POLLING_WATCHDOG_TIMEOUT_MILLIS_MIN,
                        InternalConfigurations.ADAPTER_POLLING_WATCHDOG_TIMEOUT_MILLIS_MAX,
                        InternalConfigurations.ADAPTER_POLLING_WATCHDOG_TIMEOUT_MILLIS_DEFAULT),
                internalOption(
                        InternalConfigurations.ADAPTER_POLLING_WATCHDOG_ERRORS_BEFORE_STOP,
                        InternalConfigurations.ADAPTER_POLLING_WATCHDOG_ERRORS_BEFORE_STOP_MIN,
                        InternalConfigurations.ADAPTER_POLLING_WATCHDOG_ERRORS_BEFORE_STOP_MAX,
                        InternalConfigurations.ADAPTER_POLLING_WATCHDOG_ERRORS_BEFORE_STOP_DEFAULT),
                internalOption(
                        InternalConfigurations.ADAPTER_POLLING_ERROR_BACKOFF_MAX_MILLIS,
                        InternalConfigurations.ADAPTER_POLLING_ERROR_BACKOFF_MAX_MILLIS_MIN,
                        InternalConfigurations.ADAPTER_POLLING_ERROR_BACKOFF_MAX_MILLIS_MAX,
                        InternalConfigurations.ADAPTER_POLLING_ERROR_BACKOFF_MAX_MILLIS_DEFAULT));
    }

    private int internalOption(
            final @NotNull String key, final int minimum, final int maximum, final int defaultValue) {
        final String configured = internalConfigurationService.get(key);
        if (configured == null) {
            return defaultValue;
        }
        // -- A value too large for an int fails to parse like a non-number; both are answered as "not an integer in
        // -- the range", with the bounds, as is a parsed value outside them
        Integer value;
        try {
            value = Integer.valueOf(configured.trim());
        } catch (final NumberFormatException e) {
            value = null;
        }
        if (value == null || value < minimum || value > maximum) {
            log.warn(
                    "Internal option expects an integer in its range, using the default [key={}, value='{}', min={}, max={}, default={}]",
                    key,
                    configured,
                    minimum,
                    maximum,
                    defaultValue);
            return defaultValue;
        }
        return value;
    }

    @Override
    public void stopPollingForAdapterInstance(final @NotNull ProtocolAdapter adapter) {
        settingsByAdapterId.remove(adapter.getId());
        samplerToTask.keySet().stream()
                .filter(p -> p.getAdapterId().equals(adapter.getId()))
                .forEach(this::stopPolling);
    }

    private void stopPolling(final @NotNull ProtocolAdapterPollingSampler sampler) {
        final PollingTask taskToStop = samplerToTask.remove(sampler);
        if (taskToStop != null) {
            taskToStop.stopScheduling();
        }
    }

    public void stopAllPolling() {
        samplerToTask.keySet().forEach(this::stopPolling);
        settingsByAdapterId.clear();
    }

    private class Shutdown implements HiveMQShutdownHook {
        @Override
        public @NotNull String name() {
            return "Protocol Adapter Polling Service ShutDown";
        }

        @Override
        public void run() {
            stopAllPolling();
            if (!scheduledExecutorService.isShutdown()) {
                try {
                    scheduledExecutorService.shutdown();
                    if (!scheduledExecutorService.awaitTermination(10, TimeUnit.SECONDS)) {
                        scheduledExecutorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    log.warn("Error Encountered Attempting to Shutdown Adapter Polling Service", e);
                }
            }
        }
    }
}
