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

import com.codahale.metrics.MetricRegistry;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.combining.model.DataCombiner;
import com.hivemq.combining.model.DataCombining;
import com.hivemq.combining.model.DataCombiningSources;
import com.hivemq.combining.model.EntityReference;
import com.hivemq.combining.model.EntityType;
import com.hivemq.combining.model.PrimaryType;
import com.hivemq.combining.runtime.DataCombinerManager;
import com.hivemq.combining.runtime.DataCombiningPublishService;
import com.hivemq.edge.modules.adapters.data.TagManager;
import com.hivemq.edge.modules.api.events.model.EventBuilderImpl;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.protocols.ConfigPersistence;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataCombinerManagerTest {

    private final @NotNull UUID generatedUuid = UUID.randomUUID();

    private final @NotNull ConfigPersistence configPersistence = mock();
    private final @NotNull EventService eventService = mock();
    private final @NotNull LocalTopicTree localTopicTree = mock();
    private final @NotNull TagManager tagManager = mock();
    private final @NotNull ClientQueuePersistence clientQueuePersistence = mock();
    private final @NotNull DataCombiningPublishService dataCombiningPublishService = mock();
    private final @NotNull SingleWriterService singleWriterService= mock();
    private final @NotNull MetricRegistry metricRegistry = new MetricRegistry();

    private final @NotNull DataCombinerManager dataCombinerManager =
            new DataCombinerManager(
                    configPersistence,
                    eventService,
                    metricRegistry,
                    localTopicTree,
                    tagManager,
                    clientQueuePersistence,
                    singleWriterService,
                    dataCombiningPublishService);

    private final @NotNull DataCombiner defaultCombinerInstance = new DataCombiner(generatedUuid,
            "name",
            null,
            List.of(new EntityReference(EntityType.EDGE_BROKER, UUID.randomUUID().toString())),
            List.of(new DataCombining(UUID.randomUUID(),
                    new DataCombiningSources("#", PrimaryType.TOPIC_FILTER, List.of(), List.of("#")),
                    "dest",
                    List.of())));


    @BeforeEach
    void setUp() {
        when(eventService.createDataCombiningEvent(any())).thenReturn(new EventBuilderImpl(event -> {}));
    }

    @Test
    void test_addDataCombiner_whenNotPresent_thenAdd() {
        dataCombinerManager.addDataCombiner(defaultCombinerInstance);

        final List<DataCombiner> allCombiners = dataCombinerManager.getAllCombiners();
        assertEquals(1, allCombiners.size());
        assertEquals(defaultCombinerInstance, allCombiners.get(0));
    }


    @Test
    void test_update_whenPresent_thenUpdated() {
        final DataCombiner updatedDataCombiner = new DataCombiner(generatedUuid,
                "update",
                null,
                List.of(new EntityReference(EntityType.EDGE_BROKER, UUID.randomUUID().toString())),
                List.of(new DataCombining(UUID.randomUUID(),
                        new DataCombiningSources("#", PrimaryType.TOPIC_FILTER, List.of(), List.of("#")),
                        "dest",
                        List.of())));

        dataCombinerManager.addDataCombiner(defaultCombinerInstance);
        assertTrue(dataCombinerManager.updateDataCombiner(updatedDataCombiner));

        final List<DataCombiner> allCombiners = dataCombinerManager.getAllCombiners();
        assertEquals(1, allCombiners.size());
        assertEquals(updatedDataCombiner, allCombiners.get(0));
    }

    @Test
    void test_update_whenNotPresent_thenReturnFalse() {
        final DataCombiner updatedDataCombiner = new DataCombiner(generatedUuid,
                "update",
                null,
                List.of(new EntityReference(EntityType.EDGE_BROKER, UUID.randomUUID().toString())),
                List.of(new DataCombining(UUID.randomUUID(),
                        new DataCombiningSources("#", PrimaryType.TOPIC_FILTER, List.of(), List.of("#")),
                        "dest",
                        List.of())));

        assertFalse(dataCombinerManager.updateDataCombiner(updatedDataCombiner));
    }

    @Test
    void test_delete_whenPresent_thenStopAndDeleteIT() {
        dataCombinerManager.addDataCombiner(defaultCombinerInstance);
        final List<DataCombiner> allCombiners = dataCombinerManager.getAllCombiners();
        assertEquals(1, allCombiners.size());

        dataCombinerManager.deleteDataCombiner(defaultCombinerInstance.id());
        final List<DataCombiner> allCombiners2 = dataCombinerManager.getAllCombiners();
        assertEquals(0, allCombiners2.size());

    }
}
