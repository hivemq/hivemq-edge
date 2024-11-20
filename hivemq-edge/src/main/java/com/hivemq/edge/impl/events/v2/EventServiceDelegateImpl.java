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
package com.hivemq.edge.impl.events.v2;

import com.google.common.base.Preconditions;
import com.hivemq.adapter.sdk.api.eventsv2.Event;
import com.hivemq.adapter.sdk.api.eventsv2.EventsService;
import com.hivemq.bootstrap.factories.EventServiceHandling;
import com.hivemq.bootstrap.factories.EventServiceHandlingProvider;
import com.hivemq.bootstrap.factories.EventServiceResult;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.services.publish.Publish;
import com.hivemq.extension.sdk.api.services.publish.PublishService;
import com.hivemq.extensions.services.builder.PublishBuilderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

/**
 * SPI delegate which wraps multiple (chained) implementations and
 * manages the event listeners
 *
 * @author Simon L Johnson
 */
@Singleton
public class EventServiceDelegateImpl implements EventsService {

    private final @NotNull InMemoryEvent eventStore;
    private final @NotNull EventServiceHandlingProvider eventServiceHandlingProvider;
    private final @NotNull PublishService publishService;
    private final @NotNull ConfigurationService configurationService;

    @Inject
    public EventServiceDelegateImpl(
            final @NotNull InMemoryEvent eventStore,
            final @NotNull EventServiceHandlingProvider eventServiceHandlingProvider,
            final @NotNull PublishService publishService,
            final @NotNull ConfigurationService configurationService) {
        this.eventServiceHandlingProvider = eventServiceHandlingProvider;
        this.publishService = publishService;
        this.configurationService = configurationService;
        Preconditions.checkNotNull(eventStore);
        this.eventStore = eventStore;
    }

    @Override
    public void publish(final @NotNull Event event) {

        final EventServiceHandling eventServiceHandling = eventServiceHandlingProvider.get();
        if (eventServiceHandling != null) {
            final EventServiceResult eventServiceResult = eventServiceHandling.apply(event);
            if (!eventServiceResult.isPreventPublish()) {
                // publish can not be null if preventPublish is false
                assert eventServiceResult.getModifiedPublish() != null;
                final Publish publish =
                        new PublishBuilderImpl(configurationService).fromPublish(eventServiceResult.getModifiedPublish())
                                .build();
                publishService.publish(publish);
            }
        }
        eventStore.storeEvent(event);
    }

    @Override
    public @NotNull List<Event> readEvents(
            final @Nullable Long sinceTimestamp, final @Nullable Integer limit) {
        return eventStore.readEvents(sinceTimestamp, limit);
    }
}
