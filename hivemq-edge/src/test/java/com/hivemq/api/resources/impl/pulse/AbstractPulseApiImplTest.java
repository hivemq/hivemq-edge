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
package com.hivemq.api.resources.impl.pulse;

import static org.mockito.Mockito.when;

import com.hivemq.api.resources.impl.PulseApiImpl;
import com.hivemq.configuration.entity.pulse.PulseAssetsEntity;
import com.hivemq.configuration.entity.pulse.PulseEntity;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.reader.AssetMappingExtractor;
import com.hivemq.configuration.reader.PulseExtractor;
import com.hivemq.edge.api.PulseApi;
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
import com.hivemq.pulse.asset.AssetProviderRegistry;
import com.hivemq.pulse.asset.ExternalAssetProvider;
import com.hivemq.pulse.status.StatusProvider;
import com.hivemq.pulse.status.StatusProviderRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
public abstract class AbstractPulseApiImplTest {
    @Mock
    protected @NotNull SystemInformation systemInformation;

    @Mock
    protected @NotNull AssetMappingExtractor assetMappingExtractor;

    @Mock
    protected @NotNull ExternalAssetProvider assetProvider;

    @Mock
    protected @NotNull AssetProviderRegistry assetProviderRegistry;

    @Mock
    protected @NotNull StatusProvider statusProvider;

    @Mock
    protected @NotNull StatusProviderRegistry statusProviderRegistry;

    @Mock
    protected @NotNull PulseExtractor pulseExtractor;

    @Mock
    protected @NotNull PulseEntity pulseEntity;

    @Mock
    protected @NotNull PulseAssetsEntity pulseAssetsEntity;

    protected @NotNull PulseApi pulseApi;

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
                                                .primary(instructions.get(0).getSourceRef())
                                                .build())
                                        .destination(DataCombiningDestination.builder()
                                                .assetId(UUID.randomUUID())
                                                .topic("topic")
                                                .schema("{}")
                                                .build())
                                        .instructions(instructions)
                                        .build())
                                .collect(Collectors.toCollection(ArrayList::new)))
                        .build())
                .build();
    }

    @BeforeEach
    public void setUp() {
        when(systemInformation.isConfigWriteable()).thenReturn(true);
        when(pulseExtractor.getPulseEntity()).thenReturn(pulseEntity);
        when(pulseExtractor.getLock()).thenReturn(new Object());
        when(pulseEntity.getPulseAssetsEntity()).thenReturn(pulseAssetsEntity);
        when(assetProviderRegistry.getAssetProviders()).thenReturn(Set.of(assetProvider));
        when(statusProviderRegistry.getStatusProviders()).thenReturn(Set.of(statusProvider));
        pulseApi = new PulseApiImpl(
                systemInformation,
                assetMappingExtractor,
                pulseExtractor,
                assetProviderRegistry,
                statusProviderRegistry);
    }
}
