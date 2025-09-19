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

package com.hivemq.api.resources.impl.pulse;

import com.hivemq.combining.model.DataCombiner;
import com.hivemq.configuration.entity.pulse.PulseAssetEntity;
import com.hivemq.configuration.entity.pulse.PulseAssetMappingEntity;
import com.hivemq.configuration.entity.pulse.PulseAssetMappingStatus;
import com.hivemq.edge.api.model.Combiner;
import com.hivemq.edge.api.model.DataIdentifierReference;
import com.hivemq.edge.api.model.EntityType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PulseApiImplUpdateAssetMapperTest extends AbstractPulseApiImplTest {
    @Test
    public void whenConfigNotWritable_thenReturnsConfigWritingDisabledError() {
        when(systemInformation.isConfigWriteable()).thenReturn(false);
        final Combiner combiner = createCombiner(EntityType.PULSE_AGENT, DataIdentifierReference.TypeEnum.PULSE_ASSET);
        try (final Response response = pulseApi.updateAssetMapper(combiner.getId(), combiner)) {
            assertThat(response.getStatus()).isEqualTo(403);
        }
    }

    @Test
    public void whenCombinerDoesNotExist_thenReturnsDataCombinerNotFoundError() {
        final Combiner combiner = createCombiner(EntityType.PULSE_AGENT, DataIdentifierReference.TypeEnum.PULSE_ASSET);
        when(assetMappingExtractor.getCombinerById(any())).thenReturn(Optional.empty());
        try (final Response response = pulseApi.updateAssetMapper(combiner.getId(), combiner)) {
            assertThat(response.getStatus()).isEqualTo(404);
        }
    }

    @Test
    public void whenTypeIsPulseAssetAndTopicMismatches_thenReturnsInvalidManagedAssetTopicError() {
        final Combiner combiner = createCombiner(EntityType.PULSE_AGENT, DataIdentifierReference.TypeEnum.PULSE_ASSET);
        when(assetMappingExtractor.getCombinerById(any())).thenReturn(Optional.of(DataCombiner.fromModel(combiner)));
        when(pulseAssetsEntity.getPulseAssetEntities()).thenReturn(List.of(PulseAssetEntity.builder()
                .id(combiner.getMappings().getItems().get(0).getDestination().getAssetId())
                .name(combiner.getName())
                .description(combiner.getDescription())
                .topic("new topic")
                .schema("{}")
                .mapping(PulseAssetMappingEntity.builder()
                        .id(UUID.randomUUID())
                        .status(PulseAssetMappingStatus.UNMAPPED)
                        .build())
                .build()));
        try (final Response response = pulseApi.updateAssetMapper(combiner.getId(), combiner)) {
            assertThat(response.getStatus()).isEqualTo(400);
        }
    }

    @Test
    public void whenTypeIsPulseAssetAndSchemaMismatches_thenReturnsInvalidManagedAssetTopicError() {
        final Combiner combiner = createCombiner(EntityType.PULSE_AGENT, DataIdentifierReference.TypeEnum.PULSE_ASSET);
        when(assetMappingExtractor.getCombinerById(any())).thenReturn(Optional.of(DataCombiner.fromModel(combiner)));
        when(pulseAssetsEntity.getPulseAssetEntities()).thenReturn(List.of(PulseAssetEntity.builder()
                .id(combiner.getMappings().getItems().get(0).getDestination().getAssetId())
                .name(combiner.getName())
                .description(combiner.getDescription())
                .topic("topic")
                .schema("{ \"type\": \"object\" }")
                .mapping(PulseAssetMappingEntity.builder()
                        .id(UUID.randomUUID())
                        .status(PulseAssetMappingStatus.UNMAPPED)
                        .build())
                .build()));
        try (final Response response = pulseApi.updateAssetMapper(combiner.getId(), combiner)) {
            assertThat(response.getStatus()).isEqualTo(400);
        }
    }

    @Test
    public void whenTypeIsPulseAssetAndMappingIdMismatches_thenReturnsInvalidManagedAssetMappingIdError() {
        final Combiner combiner = createCombiner(EntityType.PULSE_AGENT, DataIdentifierReference.TypeEnum.PULSE_ASSET);
        when(assetMappingExtractor.getCombinerById(any())).thenReturn(Optional.of(DataCombiner.fromModel(combiner)));
        when(pulseAssetsEntity.getPulseAssetEntities()).thenReturn(List.of(PulseAssetEntity.builder()
                .id(combiner.getMappings().getItems().get(0).getDestination().getAssetId())
                .name(combiner.getName())
                .description(combiner.getDescription())
                .topic("topic")
                .schema("{}")
                .mapping(PulseAssetMappingEntity.builder()
                        .id(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                        .status(PulseAssetMappingStatus.UNMAPPED)
                        .build())
                .build()));
        try (final Response response = pulseApi.updateAssetMapper(combiner.getId(), combiner)) {
            assertThat(response.getStatus()).isEqualTo(400);
        }
    }

    @Test
    public void whenTypeIsPulseAssetAndAllCorrect_thenReturnsOK() {
        final Combiner combiner = createCombiner(EntityType.PULSE_AGENT, DataIdentifierReference.TypeEnum.PULSE_ASSET);
        when(assetMappingExtractor.getCombinerById(any())).thenReturn(Optional.of(DataCombiner.fromModel(combiner)));
        when(assetMappingExtractor.updateDataCombiner(any())).thenReturn(true);
        when(pulseAssetsEntity.getPulseAssetEntities()).thenReturn(List.of(PulseAssetEntity.builder()
                .id(combiner.getMappings().getItems().get(0).getDestination().getAssetId())
                .name(combiner.getName())
                .description(combiner.getDescription())
                .topic("topic")
                .schema("{}")
                .mapping(PulseAssetMappingEntity.builder()
                        .id(combiner.getMappings().getItems().get(0).getId())
                        .status(PulseAssetMappingStatus.UNMAPPED)
                        .build())
                .build()));
        try (final Response response = pulseApi.updateAssetMapper(combiner.getId(), combiner)) {
            assertThat(response.getStatus()).isEqualTo(200);
            final ArgumentCaptor<DataCombiner> dataCombinerArgumentCaptor = ArgumentCaptor.forClass(DataCombiner.class);
            verify(assetMappingExtractor).updateDataCombiner(dataCombinerArgumentCaptor.capture());
            final DataCombiner newDataCombiner = dataCombinerArgumentCaptor.getValue();
            assertThat(newDataCombiner.toModel()).isEqualTo(combiner);
        }
    }
}
