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
import com.hivemq.combining.mapping.DataCombiningTransformationService;
import com.hivemq.combining.model.DataCombiner;
import com.hivemq.combining.model.DataCombining;
import com.hivemq.combining.model.DataCombiningDestination;
import com.hivemq.combining.model.DataCombiningSources;
import com.hivemq.combining.model.DataIdentifierReference;
import com.hivemq.combining.model.EntityReference;
import com.hivemq.combining.model.EntityType;
import com.hivemq.combining.runtime.DataCombiningPublishService;
import com.hivemq.combining.runtime.DataCombiningRuntimeFactory;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.entity.pulse.PulseAssetEntity;
import com.hivemq.configuration.entity.pulse.PulseAssetMappingEntity;
import com.hivemq.configuration.entity.pulse.PulseAssetMappingStatus;
import com.hivemq.configuration.entity.pulse.PulseEntity;
import com.hivemq.configuration.reader.AssetMappingExtractor;
import com.hivemq.configuration.reader.PulseExtractor;
import com.hivemq.edge.impl.events.EventServiceDelegateImpl;
import com.hivemq.edge.modules.adapters.data.TagManager;
import com.hivemq.edge.modules.api.events.EventStore;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.mappings.fieldmapping.Instruction;
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
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AssetMapperManagerTest {
    @Mock
    private @NotNull AssetMappingExtractor assetMappingExtractor;
    @Mock
    private @NotNull PulseExtractor pulseExtractor;
    @Mock
    private @NotNull ShutdownHooks shutdownHooks;
    @Mock
    private @NotNull LocalTopicTree localTopicTree;
    @Mock
    private @NotNull ClientQueuePersistence clientQueuePersistence;
    @Mock
    private @NotNull SingleWriterService singleWriterService;
    @Mock
    private @NotNull DataCombiningPublishService dataCombiningPublishService;
    @Mock
    private @NotNull TagManager tagManager;
    @Mock
    private @NotNull DataCombiningTransformationService dataCombiningTransformationService;

    private @NotNull DataCombiningRuntimeFactory dataCombiningRuntimeFactory;
    private @NotNull AssetMapperManager assetMapperManager;
    private @NotNull PulseEntity pulseEntity;
    private @NotNull MockEventStore eventStore;
    private @NotNull EventService eventService;

    private static @NotNull DataCombiner createDataCombiner() {
        return createDataCombiner(List.of(EntityType.PULSE_AGENT),
                List.of(List.of(DataIdentifierReference.Type.TAG)));
    }

    private static @NotNull DataCombiner createDataCombiner(
            final @NotNull List<EntityType> entityTypes,
            final @NotNull List<List<DataIdentifierReference.Type>> typeLists) {
        final List<EntityReference> entityReferences = entityTypes.stream()
                .map(entityType -> new EntityReference(entityType, UUID.randomUUID().toString()))
                .collect(Collectors.toCollection(ArrayList::new));
        final List<DataCombining> dataCombinings = IntStream.range(0, typeLists.size()).mapToObj(i -> {
            final List<Instruction> instructions = typeLists.get(i)
                    .stream()
                    .map(type -> new Instruction("source" + i + type.name(),
                            "destination" + i + type.name(),
                            new DataIdentifierReference(UUID.randomUUID().toString(), type)))
                    .collect(Collectors.toCollection(ArrayList::new));
            return new DataCombining(UUID.randomUUID(),
                    new DataCombiningSources(instructions.getFirst().dataIdentifierReference(), List.of(), List.of()),
                    new DataCombiningDestination(UUID.randomUUID().toString(), "topic/" + i, "{}"),
                    instructions);
        }).collect(Collectors.toCollection(ArrayList::new));
        return new DataCombiner(UUID.randomUUID(), "name", "description", entityReferences, dataCombinings);
    }

    @BeforeEach
    public void setUp() {
        eventStore = new MockEventStore();
        eventService = new EventServiceDelegateImpl(eventStore);
        dataCombiningRuntimeFactory = new DataCombiningRuntimeFactory(localTopicTree,
                clientQueuePersistence,
                singleWriterService,
                dataCombiningPublishService,
                tagManager,
                dataCombiningTransformationService,
                new HivemqId());
        assetMapperManager = new AssetMapperManager(eventService,
                new NoopMetricRegistry(),
                dataCombiningRuntimeFactory,
                shutdownHooks,
                assetMappingExtractor,
                pulseExtractor);
        pulseEntity = new PulseEntity();
        when(pulseExtractor.getLock()).thenReturn(new Object());
        when(pulseExtractor.getPulseEntity()).thenReturn(pulseEntity);
    }

    @Test
    public void whenNotAssetMapper_thenNoEvent() {
        assetMapperManager.start();
        assetMapperManager.refresh(List.of());
        assertEvents(eventStore.getEvents());
    }

    @Test
    public void whenAddNonStreamingAsset_thenNotAdded() {
        assetMapperManager.start();
        assetMapperManager.refresh(List.of(createDataCombiner(List.of(EntityType.values()),
                List.of(List.of(DataIdentifierReference.Type.values())))));
        assertEvents(eventStore.getEvents());
    }

    @Test
    public void whenAddAsset_thenAdded() {
        final DataCombiner dataCombiner = createDataCombiner();
        pulseEntity.getPulseAssetsEntity()
                .getPulseAssetEntities()
                .add(PulseAssetEntity.builder()
                        .id(UUID.fromString(dataCombiner.dataCombinings().getFirst().destination().assetId()))
                        .name("name")
                        .description("description")
                        .topic(dataCombiner.dataCombinings().getFirst().destination().topic())
                        .schema(dataCombiner.dataCombinings().getFirst().destination().schema())
                        .mapping(PulseAssetMappingEntity.builder()
                                .id(dataCombiner.dataCombinings().getFirst().id())
                                .status(PulseAssetMappingStatus.STREAMING)
                                .build())
                        .build());
        assetMapperManager.start();
        assetMapperManager.refresh(List.of(dataCombiner));
        assertEvents(eventStore.getEvents(), "Asset mapper 'name' was successfully created.");
    }

    @Test
    public void whenDeleteAsset_thenDeleted() {
        whenAddAsset_thenAdded();
        eventStore.getEvents().clear();
        assetMapperManager.refresh(List.of());
        assertEvents(eventStore.getEvents(), "Asset mapper 'name' was successfully deleted.");
    }

    @Test
    public void whenUpdateAsset_thenUpdated() {
        final DataCombiner dataCombinerForCreation = createDataCombiner().withName("name for creation");
        final DataCombiner dataCombinerForDeletion = createDataCombiner().withName("name for deletion");
        final DataCombiner dataCombinerForUpdate = createDataCombiner().withName("name for update");
        Stream.of(dataCombinerForCreation, dataCombinerForDeletion, dataCombinerForUpdate)
                .map(dataCombiner -> PulseAssetEntity.builder()
                        .id(UUID.fromString(dataCombiner.dataCombinings().getFirst().destination().assetId()))
                        .name(dataCombiner.name())
                        .description(dataCombiner.description())
                        .topic(dataCombiner.dataCombinings().getFirst().destination().topic())
                        .schema(dataCombiner.dataCombinings().getFirst().destination().schema())
                        .mapping(PulseAssetMappingEntity.builder()
                                .id(dataCombiner.dataCombinings().getFirst().id())
                                .status(PulseAssetMappingStatus.STREAMING)
                                .build())
                        .build())
                .forEach(pulseEntity.getPulseAssetsEntity().getPulseAssetEntities()::add);
        assetMapperManager.start();
        // Let's add asset mappers for deletion and update.
        assetMapperManager.refresh(List.of(dataCombinerForDeletion, dataCombinerForUpdate));
        assertEvents(eventStore.getEvents(),
                "Asset mapper 'name for update' was successfully created.",
                "Asset mapper 'name for deletion' was successfully created.");
        eventStore.getEvents().clear();
        // Update asset mapper for update.
        pulseEntity.getPulseAssetsEntity().getPulseAssetEntities().get(2).setTopic("topic/updated");
        assetMapperManager.refresh(List.of(dataCombinerForCreation, dataCombinerForUpdate));
        assertEvents(eventStore.getEvents(),
                "Asset mapper 'name for deletion' was successfully deleted.",
                "Asset mapper 'name for creation' was successfully created.",
                "Asset mapper 'name for update' was successfully updated.");
    }

    private void assertEvents(final @NotNull List<Event> events, final String... expectedMessages) {
        assertThat(events).isNotNull();
        if (events.size() != expectedMessages.length + 1) {
            System.out.println("====================================================================");
            events.stream().map(Event::getMessage).forEach(System.out::println);
            System.out.println("====================================================================");
        }
        assertThat(events.size()).isEqualTo(expectedMessages.length + 1);
        final List<String> messages = events.stream().map(Event::getMessage).toList();
        Stream.of(expectedMessages).forEach(expectedMessage -> assertThat(messages).contains(expectedMessage));
        assertThat(messages.getLast()).isEqualTo("Configuration has been successfully updated");
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

        public @NotNull List<Event> getEvents() {
            return events;
        }

        @Override
        public @NotNull List<Event> readEvents(@NotNull final Long since, @NotNull final Integer limit) {
            return events;
        }
    }
}
