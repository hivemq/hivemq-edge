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
package com.hivemq.topicbuffer;

import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.edge.impl.events.InMemoryEventImpl;
import com.hivemq.edge.modules.api.events.EventStore;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Composite {@link EventStore} that merges regular adapter events (from {@link InMemoryEventImpl})
 * with buffered MQTT messages (from {@link TopicBufferService}) at read time.
 * Writes are always forwarded to the in-memory store only.
 */
@Singleton
public class TopicBufferAwareEventStore implements EventStore {

    private final @NotNull InMemoryEventImpl delegate;
    private final @NotNull TopicBufferService topicBufferService;

    @Inject
    public TopicBufferAwareEventStore(
            final @NotNull InMemoryEventImpl delegate, final @NotNull TopicBufferService topicBufferService) {
        this.delegate = delegate;
        this.topicBufferService = topicBufferService;
    }

    @Override
    public void storeEvent(final @NotNull Event event) {
        delegate.storeEvent(event);
    }

    @Override
    public @NotNull List<Event> readEvents(final @Nullable Long since, final @Nullable Integer limit) {
        final List<Event> regular = delegate.readEvents(since, null);
        final List<Event> buffered = topicBufferService.getAllAsEvents(since);

        final List<Event> merged = new ArrayList<>(regular.size() + buffered.size());
        merged.addAll(regular);
        merged.addAll(buffered);

        Stream<Event> stream =
                merged.stream().sorted(Comparator.comparing(Event::getTimestamp).reversed());
        if (limit != null) {
            stream = stream.limit(limit);
        }
        return stream.toList();
    }
}
