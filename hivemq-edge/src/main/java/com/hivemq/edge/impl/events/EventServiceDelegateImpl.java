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
package com.hivemq.edge.impl.events;

import com.google.common.base.Preconditions;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.events.model.EventBuilder;
import com.hivemq.adapter.sdk.api.events.model.TypeIdentifier;
import com.hivemq.edge.model.TypeIdentifierImpl;
import com.hivemq.edge.modules.api.events.EventListener;
import com.hivemq.edge.modules.api.events.EventStore;
import com.hivemq.edge.modules.api.events.model.EventBuilderImpl;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * SPI delegate which wraps multiple (chained) implementations and
 * manages the event listeners
 *
 * @author Simon L Johnson
 */
@Singleton
public class EventServiceDelegateImpl implements EventService {

    private final @NotNull Set<EventListener> eventListeners;
    private final @NotNull EventStore eventStore;
    private final @NotNull ExecutorService executorService;

    @Inject
    public EventServiceDelegateImpl(
            final @NotNull EventStore eventStore,
            final @NotNull Set<EventListener> eventListeners,
            final @NotNull ExecutorService executorService) {
        Preconditions.checkNotNull(eventStore);
        Preconditions.checkNotNull(executorService);
        this.eventStore = eventStore;
        this.eventListeners = eventListeners;
        this.executorService = executorService;
    }

    public void fireEvent(final @NotNull Event event) {
        try {
            eventStore.storeEvent(event);
        } finally {
            notifyEventListeners(event);
        }
    }

    public @NotNull EventBuilder adapterEvent(final @NotNull String adapterId, final @NotNull String protocolId) {
        return new EventBuilderImpl(this::fireEvent).withTimestamp(System.currentTimeMillis())
                .withSource(TypeIdentifierImpl.create(TypeIdentifier.Type.ADAPTER, adapterId))
                .withAssociatedObject(TypeIdentifierImpl.create(TypeIdentifier.Type.ADAPTER_TYPE, protocolId));
    }

    @Override
    public @NotNull EventBuilder bridgeEvent() {
        return new EventBuilderImpl(this::fireEvent).withTimestamp(System.currentTimeMillis());
    }

    @Override
    public List<Event> readEvents(final @Nullable Long sinceTimestamp, final @Nullable Integer limit) {
        return eventStore.readEvents(sinceTimestamp, limit);
    }

    private void notifyEventListeners(final @NotNull Event event) {
        Preconditions.checkNotNull(event);
        if (!eventListeners.isEmpty()) {
            eventListeners.forEach(l -> executorService.submit(() -> l.eventFired(event)));
        }
    }
}
