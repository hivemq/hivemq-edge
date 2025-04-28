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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;

public class DataCombinerManagerTest {

    @Test
    public void test_add() {
        var now = System.currentTimeMillis();
        var eventService = new EventServiceDelegateImpl(new InMemoryEventImpl());

        var dataCombiningRuntimeFactory = mock(DataCombiningRuntimeFactory.class);
        var dataCombiningExtractor = mock(DataCombiningExtractor.class);
        var shutdownHooks = mock(ShutdownHooks.class);
        var dataCombinerManager = new DataCombinerManager(eventService, new NoopMetricRegistry(), dataCombiningRuntimeFactory, shutdownHooks, dataCombiningExtractor);
        dataCombinerManager.start();
        var combiner = new DataCombiner(UUID.randomUUID(), "namey", "description", List.of(), List.of());
        dataCombinerManager.refresh(List.of(combiner));

        assertThat(getFitleredEvents(eventService, now))
                .extracting(Event::getSource, Event::getMessage)
                .containsExactlyInAnyOrder(
                        tuple(TypeIdentifierImpl.create(TypeIdentifier.Type.COMBINER, combiner.id().toString()), "Combiner 'namey' was successfully created.")
                );
    }

    @Test
    public void test_addThenDelete() {
        var now = System.currentTimeMillis();
        var eventService = new EventServiceDelegateImpl(new InMemoryEventImpl());

        var dataCombiningRuntimeFactory = mock(DataCombiningRuntimeFactory.class);
        var dataCombiningExtractor = mock(DataCombiningExtractor.class);
        var shutdownHooks = mock(ShutdownHooks.class);
        var dataCombinerManager = new DataCombinerManager(eventService, new NoopMetricRegistry(), dataCombiningRuntimeFactory, shutdownHooks, dataCombiningExtractor);
        dataCombinerManager.start();
        var combiner = new DataCombiner(UUID.randomUUID(), "namey", "description", List.of(), List.of());
        dataCombinerManager.refresh(List.of(combiner));
        dataCombinerManager.refresh(List.of());

        assertThat(getFitleredEvents(eventService, now))
                .extracting(Event::getSource, Event::getMessage)
                .containsExactlyInAnyOrder(
                        tuple(TypeIdentifierImpl.create(TypeIdentifier.Type.COMBINER, combiner.id().toString()), "Combiner 'namey' was successfully created."),
                        tuple(TypeIdentifierImpl.create(TypeIdentifier.Type.COMBINER, combiner.id().toString()), "Combiner 'namey' was permanently deleted.")
                );
    }

    @Test
    public void test_addAndDeleteInOneGo() {
        var now = System.currentTimeMillis();
        var eventService = new EventServiceDelegateImpl(new InMemoryEventImpl());

        var dataCombiningRuntimeFactory = mock(DataCombiningRuntimeFactory.class);
        var dataCombiningExtractor = mock(DataCombiningExtractor.class);
        var shutdownHooks = mock(ShutdownHooks.class);
        var dataCombinerManager = new DataCombinerManager(eventService, new NoopMetricRegistry(), dataCombiningRuntimeFactory, shutdownHooks, dataCombiningExtractor);
        dataCombinerManager.start();
        var combiner = new DataCombiner(UUID.randomUUID(), "namey", "description", List.of(), List.of());
        var combiner2 = new DataCombiner(UUID.randomUUID(), "namey2", "description", List.of(), List.of());
        dataCombinerManager.refresh(List.of(combiner));
        dataCombinerManager.refresh(List.of(combiner2));

        assertThat(getFitleredEvents(eventService, now))
                .extracting(Event::getSource, Event::getMessage)
                .containsExactlyInAnyOrder(
                        tuple(TypeIdentifierImpl.create(TypeIdentifier.Type.COMBINER, combiner2.id().toString()), "Combiner 'namey2' was successfully created."),
                        tuple(TypeIdentifierImpl.create(TypeIdentifier.Type.COMBINER, combiner.id().toString()), "Combiner 'namey' was permanently deleted."),
                        tuple(TypeIdentifierImpl.create(TypeIdentifier.Type.COMBINER, combiner.id().toString()), "Combiner 'namey' was successfully created.")
                );
    }

    @Test
    public void test_addThenUpdate() {
        var now = System.currentTimeMillis();
        var eventService = new EventServiceDelegateImpl(new InMemoryEventImpl());

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
                        tuple(TypeIdentifierImpl.create(TypeIdentifier.Type.COMBINER, combiner.id().toString()), "Combiner 'namey' was successfully created."),
                        tuple(TypeIdentifierImpl.create(TypeIdentifier.Type.COMBINER, combiner.id().toString()), "Combiner 'namey2' was successfully updated.")
                );
    }

    private static @NotNull Stream<Event> getFitleredEvents(EventServiceDelegateImpl eventService, long now) {
        return eventService.readEvents(now, 100).stream().filter(event -> !event.getMessage().equals("Configuration has been successfully updated"));
    }
}
