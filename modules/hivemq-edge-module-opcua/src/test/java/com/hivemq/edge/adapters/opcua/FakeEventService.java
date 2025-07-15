/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua;

import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.events.model.EventBuilder;
import com.hivemq.adapter.sdk.api.events.model.TypeIdentifier;
import com.hivemq.edge.model.TypeIdentifierImpl;
import com.hivemq.edge.modules.api.events.model.EventBuilderImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FakeEventService implements EventService {
    private final @NotNull List<Event> eventStore;

    public FakeEventService() {
        this.eventStore = new ArrayList<>();
    }

    public void fireEvent(final @NotNull Event event) {
        eventStore.add(event);
    }

    public @NotNull EventBuilder createAdapterEvent(final @NotNull String adapterId, final @NotNull String protocolId) {
        return new EventBuilderImpl(this::fireEvent).withTimestamp(System.currentTimeMillis())
                .withSource(TypeIdentifierImpl.create(TypeIdentifier.Type.ADAPTER, adapterId))
                .withAssociatedObject(TypeIdentifierImpl.create(TypeIdentifier.Type.ADAPTER_TYPE, protocolId));
    }

    @Override
    public @NotNull EventBuilder createDataCombiningEvent(@NotNull final UUID uuid) {
        return new EventBuilderImpl(this::fireEvent).withTimestamp(System.currentTimeMillis())
                .withSource(TypeIdentifierImpl.create(TypeIdentifier.Type.DATA_COMBINING, uuid.toString()));
    }

    @Override
    public @NotNull EventBuilder createCombinerEvent(@NotNull final UUID uuid) {
        return new EventBuilderImpl(this::fireEvent).withTimestamp(System.currentTimeMillis())
                .withSource(TypeIdentifierImpl.create(TypeIdentifier.Type.COMBINER, uuid.toString()));
    }

    @Override
    public @NotNull EventBuilder bridgeEvent() {
        return new EventBuilderImpl(this::fireEvent).withTimestamp(System.currentTimeMillis());
    }

    @Override
    public @NotNull EventBuilder configurationEvent() {
        return new EventBuilderImpl(this::fireEvent).withTimestamp(System.currentTimeMillis());
    }

    @Override
    public List<Event> readEvents(final @Nullable Long sinceTimestamp, final @Nullable Integer limit) {
        return List.copyOf(eventStore);
    }
}
