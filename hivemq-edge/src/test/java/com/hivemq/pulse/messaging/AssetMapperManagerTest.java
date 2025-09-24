/*
 *  Copyright 2019-present HiveMQ GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hivemq.pulse.messaging;

import com.codahale.metrics.NoopMetricRegistry;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.combining.model.DataCombiner;
import com.hivemq.combining.model.DataCombining;
import com.hivemq.combining.model.DataIdentifierReference;
import com.hivemq.combining.model.EntityReference;
import com.hivemq.combining.model.EntityType;
import com.hivemq.combining.runtime.DataCombiningRuntimeFactory;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.entity.pulse.PulseEntity;
import com.hivemq.configuration.reader.AssetMappingExtractor;
import com.hivemq.configuration.reader.PulseExtractor;
import com.hivemq.edge.impl.events.EventServiceDelegateImpl;
import com.hivemq.edge.modules.api.events.EventStore;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AssetMapperManagerTest {
    @Mock
    private @NotNull DataCombiningRuntimeFactory dataCombiningRuntimeFactory;
    @Mock
    private @NotNull AssetMappingExtractor assetMappingExtractor;
    @Mock
    private @NotNull PulseExtractor pulseExtractor;
    @Mock
    private @NotNull ShutdownHooks shutdownHooks;
    private @NotNull AssetMapperManager assetMapperManager;
    private @NotNull PulseEntity pulseEntity;

    private @NotNull MockEventStore eventStore;
    private @NotNull EventService eventService;

//    private static @NotNull DataCombiner createDataCombiner(
//            final @NotNull List<EntityType> entityTypes,
//            final @NotNull List<DataIdentifierReference.Type> types) {
//        final List<EntityReference> entityReferences = entityTypes.stream()
//                .map(entityType -> new EntityReference(entityType, UUID.randomUUID().toString()))
//                .collect(Collectors.toCollection(ArrayList::new));
//        final List<DataCombining> dataCombinings = entityTypes.stream()
//                .flatMap(entityType -> types.stream())
//        return new DataCombiner(UUID.randomUUID(), "name", "description", entityReferences, dataCombinings);
//    }

    @BeforeEach
    public void setUp() {
        eventStore = new MockEventStore();
        eventService = new EventServiceDelegateImpl(eventStore);
        assetMapperManager = new AssetMapperManager(eventService,
                new NoopMetricRegistry(),
                dataCombiningRuntimeFactory,
                shutdownHooks,
                assetMappingExtractor,
                pulseExtractor);
        pulseEntity = new PulseEntity();
        when(pulseExtractor.getPulseEntity()).thenReturn(pulseEntity);
    }

    @Test
    public void whenNotAssetMapper_thenNoEvent() {
        assetMapperManager.start();
        assetMapperManager.refresh(List.of());
        final List<Event> events = eventStore.readEvents(0L, 0);
        assertThat(events).hasSize(1);
        assertEventConfigUpdate(events);
    }

    @Test
    public void whenAddNonStreamingAsset_thenNotAdded() {
        // TODO
        assetMapperManager.start();
        assetMapperManager.refresh(List.of());
        final List<Event> events = eventStore.readEvents(0L, 0);
        assertThat(events).hasSize(1);
        assertEventConfigUpdate(events);
    }

    private void assertEventConfigUpdate(final @NotNull List<Event> events) {
        assertThat(events).isNotNull();
        assertThat(events.size()).isGreaterThan(0);
        assertThat(events.getLast().getMessage()).isEqualTo("Configuration has been successfully updated");
    }

    private static class MockEventStore implements EventStore {
        private final @NotNull List<Event> events;

        public MockEventStore() {
            events = new ArrayList<>();
        }

        @Override
        public void storeEvent(@NotNull final Event event) {
            events.add(event);
        }

        @Override
        public @NotNull List<Event> readEvents(@NotNull final Long since, @NotNull final Integer limit) {
            return events;
        }
    }
}
