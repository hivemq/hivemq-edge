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
package com.hivemq.combining;

import com.codahale.metrics.NoopMetricRegistry;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.events.model.TypeIdentifier;
import com.hivemq.combining.model.DataCombiner;
import com.hivemq.combining.runtime.DataCombinerManager;
import com.hivemq.combining.runtime.DataCombiningRuntimeFactory;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.reader.DataCombiningExtractor;
import com.hivemq.edge.impl.events.EventServiceDelegateImpl;
import com.hivemq.edge.impl.events.InMemoryEventImpl;
import com.hivemq.edge.model.TypeIdentifierImpl;
import com.hivemq.edge.modules.api.events.EventStore;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

public class DataCombinerManagerTest {

    @Test
    public void test_add() {
        var now = System.currentTimeMillis();
        var eventService = new EventServiceDelegateImpl(createEventStore());

        var dataCombiningRuntimeFactory = mock(DataCombiningRuntimeFactory.class);
        var dataCombiningExtractor = mock(DataCombiningExtractor.class);
        var shutdownHooks = mock(ShutdownHooks.class);
        var dataCombinerManager = new DataCombinerManager(eventService, new NoopMetricRegistry(), dataCombiningRuntimeFactory, shutdownHooks, dataCombiningExtractor);
        dataCombinerManager.start();
        var combiner = new DataCombiner(UUID.randomUUID(), "namey", "description", List.of(), List.of());
        dataCombinerManager.refresh(List.of(combiner));

        await().until(() -> getFitleredEvents(eventService, now).size() == 1);

        assertThat(getFitleredEvents(eventService, now))
                .extracting(Event::getSource, Event::getMessage)
                .containsExactlyInAnyOrder(
                        tuple(TypeIdentifierImpl.create(TypeIdentifier.Type.COMBINER, combiner.id().toString()), "Combiner 'namey' was successfully created.")
                );
    }

    @Test
    public void test_addThenDelete() {
        var now = System.currentTimeMillis();
        var eventService = new EventServiceDelegateImpl(createEventStore());

        var dataCombiningRuntimeFactory = mock(DataCombiningRuntimeFactory.class);
        var dataCombiningExtractor = mock(DataCombiningExtractor.class);
        var shutdownHooks = mock(ShutdownHooks.class);
        var dataCombinerManager = new DataCombinerManager(eventService, new NoopMetricRegistry(), dataCombiningRuntimeFactory, shutdownHooks, dataCombiningExtractor);
        dataCombinerManager.start();
        var combiner = new DataCombiner(UUID.randomUUID(), "namey", "description", List.of(), List.of());
        dataCombinerManager.refresh(List.of(combiner));
        dataCombinerManager.refresh(List.of());

        await().until(() -> getFitleredEvents(eventService, now).size() == 2);

        assertThat(getFitleredEvents(eventService, now))
                .extracting(Event::getSource, Event::getMessage)
                .containsExactlyInAnyOrder(
                        tuple(TypeIdentifierImpl.create(TypeIdentifier.Type.COMBINER, combiner.id().toString()), "Combiner 'namey' was permanently deleted."),
                        tuple(TypeIdentifierImpl.create(TypeIdentifier.Type.COMBINER, combiner.id().toString()), "Combiner 'namey' was successfully created.")
                );
    }

    @Test
    public void test_addAndDeleteInOneGo() {
        var now = System.currentTimeMillis();
        var eventService = new EventServiceDelegateImpl(createEventStore());

        var dataCombiningRuntimeFactory = mock(DataCombiningRuntimeFactory.class);
        var dataCombiningExtractor = mock(DataCombiningExtractor.class);
        var shutdownHooks = mock(ShutdownHooks.class);
        var dataCombinerManager = new DataCombinerManager(eventService, new NoopMetricRegistry(), dataCombiningRuntimeFactory, shutdownHooks, dataCombiningExtractor);
        dataCombinerManager.start();
        var combiner = new DataCombiner(UUID.randomUUID(), "namey", "description", List.of(), List.of());
        var combiner2 = new DataCombiner(UUID.randomUUID(), "namey2", "description", List.of(), List.of());
        dataCombinerManager.refresh(List.of(combiner));
        dataCombinerManager.refresh(List.of(combiner2));

        await().until(() -> getFitleredEvents(eventService, now).size() == 3);

        assertThat(getFitleredEvents(eventService, now))
                .extracting(Event::getSource, Event::getMessage)
                .containsExactlyInAnyOrder(
                        tuple(TypeIdentifierImpl.create(TypeIdentifier.Type.COMBINER, combiner.id().toString()), "Combiner 'namey' was successfully created."),
                        tuple(TypeIdentifierImpl.create(TypeIdentifier.Type.COMBINER, combiner.id().toString()), "Combiner 'namey' was permanently deleted."),
                        tuple(TypeIdentifierImpl.create(TypeIdentifier.Type.COMBINER, combiner2.id().toString()), "Combiner 'namey2' was successfully created.")
                );
    }

    @Test
    public void test_addThenUpdate() {
        var now = System.currentTimeMillis();
        var eventService = new EventServiceDelegateImpl(createEventStore());

        var dataCombiningRuntimeFactory = mock(DataCombiningRuntimeFactory.class);
        var dataCombiningExtractor = mock(DataCombiningExtractor.class);
        var shutdownHooks = mock(ShutdownHooks.class);
        var dataCombinerManager = new DataCombinerManager(eventService, new NoopMetricRegistry(), dataCombiningRuntimeFactory, shutdownHooks, dataCombiningExtractor);
        dataCombinerManager.start();
        var combiner = new DataCombiner(UUID.randomUUID(), "namey", "description", List.of(), List.of());
        var updatedCombiner = new DataCombiner(combiner.id(), "namey2", "description", List.of(), List.of());
        dataCombinerManager.refresh(List.of(combiner));
        dataCombinerManager.refresh(List.of(updatedCombiner));

        assertThat(getFitleredEvents(eventService, now))
                .extracting(Event::getSource, Event::getMessage)
                .containsExactlyInAnyOrder(
                        tuple(TypeIdentifierImpl.create(TypeIdentifier.Type.COMBINER, combiner.id().toString()), "Combiner 'namey2' was successfully updated."),
                        tuple(TypeIdentifierImpl.create(TypeIdentifier.Type.COMBINER, combiner.id().toString()), "Combiner 'namey' was successfully created.")
                );
    }

    private static @NotNull List<Event> getFitleredEvents(EventServiceDelegateImpl eventService, long now) {
        return eventService.readEvents(now, 100).stream().filter(event -> !event.getMessage().equals("Configuration has been successfully updated")).toList();
    }

    private static @NotNull EventStore createEventStore() {
        return new EventStore() {

            private final @NotNull List<Event> events = new ArrayList<>();

            @Override
            public void storeEvent(@NotNull Event event) {
                events.add(event);
            }

            @Override
            public @NotNull List<Event> readEvents(@NotNull Long since, @NotNull Integer limit) {
                return events;
            }
        };
    }
}
