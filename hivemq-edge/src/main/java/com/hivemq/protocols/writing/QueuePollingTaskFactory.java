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
import com.hivemq.adapter.sdk.api.config.WriteContext;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ScheduledExecutorService;

@Singleton
public class QueuePollingTaskFactory {

    private final @NotNull ScheduledExecutorService scheduledExecutorService;
    private final @NotNull EventService eventService;
    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull NanoTimeProvider nanoTimeProvider;
    private final @NotNull SingleWriterService singleWriterService;
    private final @NotNull ObjectMapper objectMapper;


    @Inject
    public QueuePollingTaskFactory(
            final @NotNull ScheduledExecutorService scheduledExecutorService,
            final @NotNull EventService eventService,
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull NanoTimeProvider nanoTimeProvider,
            final @NotNull SingleWriterService singleWriterService,
            final @NotNull ObjectMapper objectMapper) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.eventService = eventService;
        this.clientQueuePersistence = clientQueuePersistence;
        this.nanoTimeProvider = nanoTimeProvider;
        this.singleWriterService = singleWriterService;
        this.objectMapper = objectMapper;
    }


    public @NotNull QueuePollingTask create(
            @NotNull WritingProtocolAdapter adapter, @NotNull String queueId, @NotNull WriteContext writeContext) {
        return new QueuePollingTask(adapter,
                scheduledExecutorService,
                eventService,
                clientQueuePersistence,
                nanoTimeProvider,
                queueId,
                singleWriterService,
                writeContext,
                objectMapper);
    }

}
