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
package com.hivemq.api.resources.impl.combiners;

import static org.mockito.Mockito.when;

import com.hivemq.api.resources.impl.CombinersResourceImpl;
import com.hivemq.combining.model.DataCombiner;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.reader.DataCombiningExtractor;
import com.hivemq.configuration.reader.ProtocolAdapterExtractor;
import com.hivemq.edge.api.CombinersApi;
import com.hivemq.edge.api.model.Combiner;
import com.hivemq.edge.api.model.DataCombining;
import com.hivemq.edge.api.model.DataCombiningDestination;
import com.hivemq.edge.api.model.DataCombiningList;
import com.hivemq.edge.api.model.DataCombiningSources;
import com.hivemq.edge.api.model.DataIdentifierReference;
import com.hivemq.edge.api.model.EntityReference;
import com.hivemq.edge.api.model.EntityReferenceList;
import com.hivemq.edge.api.model.EntityType;
import com.hivemq.edge.api.model.Instruction;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public abstract class AbstractCombinersResourceImplTest {

    @Mock
    protected @NotNull SystemInformation systemInformation;

    @Mock
    protected @NotNull DataCombiningExtractor dataCombiningExtractor;

    @Mock
    protected @NotNull ProtocolAdapterExtractor protocolAdapterExtractor;

    protected @NotNull CombinersApi combinersApi;

    protected static @NotNull Combiner createCombiner(
            final @NotNull EntityType entityType, final @NotNull DataIdentifierReference.TypeEnum type) {
        final List<Instruction> instructions = Stream.of(
                        Instruction.builder()
                                .source("$.a")
                                .destination("dest.a")
                                .sourceRef(DataIdentifierReference.builder()
                                        .id(UUID.randomUUID().toString())
                                        .type(type)
                                        .build())
                                .build(),
                        Instruction.builder()
                                .source("$.b")
                                .destination("dest.b")
                                .sourceRef(DataIdentifierReference.builder()
                                        .id(UUID.randomUUID().toString())
                                        .type(type)
                                        .build())
                                .build())
                .collect(Collectors.toCollection(ArrayList::new));

        final UUID dataCombiningId = UUID.randomUUID();

        return Combiner.builder()
                .id(UUID.randomUUID())
                .name("name")
                .description("description")
                .sources(EntityReferenceList.builder()
                        .items(Stream.of(EntityReference.builder()
                                        .id(dataCombiningId.toString())
                                        .type(entityType)
                                        .build())
                                .collect(Collectors.toCollection(ArrayList::new)))
                        .build())
                .mappings(DataCombiningList.builder()
                        .items(Stream.of(DataCombining.builder()
                                        .id(dataCombiningId)
                                        .sources(DataCombiningSources.builder()
                                                .primary(instructions.getFirst().getSourceRef())
                                                .build())
                                        .destination(DataCombiningDestination.builder()
                                                .topic("topic")
                                                .schema("{}")
                                                .build())
                                        .instructions(instructions)
                                        .build())
                                .collect(Collectors.toCollection(ArrayList::new)))
                        .build())
                .build();
    }

    protected static @NotNull DataCombiner toDataCombiner(final @NotNull Combiner combiner) {
        return DataCombiner.fromModel(combiner);
    }

    @BeforeEach
    public void setUp() {
        when(systemInformation.isConfigWriteable()).thenReturn(true);
        combinersApi = new CombinersResourceImpl(systemInformation, dataCombiningExtractor, protocolAdapterExtractor);
    }
}
