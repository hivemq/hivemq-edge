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
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingSampler;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.util.NanoTimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Daniel Krüger
 */
@Singleton
public class ProtocolAdapterPollingServiceImpl implements ProtocolAdapterPollingService {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ProtocolAdapterPollingServiceImpl.class);

    private final @NotNull ScheduledExecutorService scheduledExecutorService;
    private final @NotNull EventService eventService;
    private final @NotNull NanoTimeProvider nanoTimeProvider;
    private final @NotNull Map<ProtocolAdapterPollingSampler, PollingTask> samplerToTask = new ConcurrentHashMap<>();

    @Inject
    public ProtocolAdapterPollingServiceImpl(
            final @NotNull ScheduledExecutorService scheduledExecutorService,
            final @NotNull ShutdownHooks shutdownHooks,
            final @NotNull EventService eventService,
            final @NotNull NanoTimeProvider nanoTimeProvider) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.eventService = eventService;
        this.nanoTimeProvider = nanoTimeProvider;
        shutdownHooks.add(new Shutdown());
    }

    @Override
    public void schedulePolling(final @NotNull ProtocolAdapterPollingSampler sampler) {
        final PollingTask pollingTask = new PollingTask(sampler, scheduledExecutorService, eventService, nanoTimeProvider);
        scheduledExecutorService.schedule(pollingTask, sampler.getInitialDelay(), sampler.getUnit());
        samplerToTask.put(sampler, pollingTask);
    }

    @Override
    public void stopPollingForAdapterInstance(final @NotNull ProtocolAdapter adapter) {
        samplerToTask.keySet()
                .stream()
                .filter(p -> p.getAdapterId().equals(adapter.getId()))
                .forEach(this::stopPolling);
    }

    private void stopPolling(final @NotNull ProtocolAdapterPollingSampler sampler) {
        final PollingTask taskToStop = samplerToTask.remove(sampler);
        taskToStop.stopScheduling();
    }

    public void stopAllPolling() {
        samplerToTask.keySet().forEach(this::stopPolling);
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
