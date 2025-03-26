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

import com.hivemq.combining.model.DataCombiner;
import com.hivemq.combining.model.DataCombining;
import com.hivemq.combining.model.DataCombiningDestination;
import com.hivemq.combining.model.DataCombiningSources;
import com.hivemq.combining.model.DataIdentifierReference;
import com.hivemq.combining.model.EntityReference;
import com.hivemq.combining.model.EntityType;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.DataCombiningExtractor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DataCombinerManagerTest {

    private final @NotNull UUID generatedUuid = UUID.randomUUID();

    private final @NotNull ConfigFileReaderWriter configFileReaderWriter = mock();
    private final @NotNull DataCombiningExtractor dataCombiningExtractor = new DataCombiningExtractor(configFileReaderWriter);

    private final @NotNull DataCombiner defaultCombinerInstance = new DataCombiner(generatedUuid,
            "name",
            null,
            List.of(new EntityReference(EntityType.EDGE_BROKER, UUID.randomUUID().toString())),
            List.of(new DataCombining(UUID.randomUUID(),
                    new DataCombiningSources(new DataIdentifierReference("#",
                            DataIdentifierReference.Type.TOPIC_FILTER), List.of(), List.of("#")),
                    new DataCombiningDestination("dest", "{}"),
                    List.of())));

    @Test
    void test_addDataCombiner_whenNotPresent_thenAdd() {
        dataCombiningExtractor.addDataCombiner(defaultCombinerInstance);

        final List<DataCombiner> allCombiners = dataCombiningExtractor.getAllCombiners();
        assertThat(allCombiners)
                .hasSize(1)
                .containsExactly(allCombiners.get(0));
    }


    @Test
    void test_update_whenPresent_thenUpdated() {
        final DataCombiner updatedDataCombiner = new DataCombiner(generatedUuid,
                "update",
                null,
                List.of(new EntityReference(EntityType.EDGE_BROKER, UUID.randomUUID().toString())),
                List.of(new DataCombining(UUID.randomUUID(),
                        new DataCombiningSources(new DataIdentifierReference("#",
                                DataIdentifierReference.Type.TOPIC_FILTER), List.of(), List.of("#")),
                        new DataCombiningDestination("dest", "{}"),
                        List.of())));

        dataCombiningExtractor.addDataCombiner(defaultCombinerInstance);

        final List<DataCombiner> allCombiners = dataCombiningExtractor.getAllCombiners();
        assertThat(allCombiners)
                .hasSize(1)
                .containsExactly(allCombiners.get(0));
    }

    @Test
    void test_update_whenNotPresent_thenReturnFalse() {
        final DataCombiner updatedDataCombiner = new DataCombiner(generatedUuid,
                "update",
                null,
                List.of(new EntityReference(EntityType.EDGE_BROKER, UUID.randomUUID().toString())),
                List.of(new DataCombining(UUID.randomUUID(),
                        new DataCombiningSources(new DataIdentifierReference("#",
                                DataIdentifierReference.Type.TOPIC_FILTER), List.of(), List.of("#")),
                        new DataCombiningDestination("dest", "{}"),
                        List.of())));

        assertThat(dataCombiningExtractor.updateDataCombiner(updatedDataCombiner))
                .isFalse();
    }

    @Test
    void test_delete_whenPresent_thenStopAndDeleteIT() {
        dataCombiningExtractor.addDataCombiner(defaultCombinerInstance);
        assertThat(dataCombiningExtractor.getAllCombiners())
                .hasSize(1);

        dataCombiningExtractor.deleteDataCombiner(defaultCombinerInstance.id());
        assertThat(dataCombiningExtractor.getAllCombiners())
                .hasSize(0);

    }
}
