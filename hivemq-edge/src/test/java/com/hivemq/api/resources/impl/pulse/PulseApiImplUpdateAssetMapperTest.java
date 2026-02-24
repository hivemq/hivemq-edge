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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.api.errors.ConfigWritingDisabled;
import com.hivemq.api.errors.pulse.AssetMapperNotFoundError;
import com.hivemq.api.errors.pulse.InvalidDataIdentifierReferenceTypeForAssetMapperError;
import com.hivemq.api.errors.pulse.InvalidManagedAssetMappingIdError;
import com.hivemq.api.errors.pulse.InvalidManagedAssetSchemaError;
import com.hivemq.api.errors.pulse.InvalidManagedAssetTopicError;
import com.hivemq.combining.model.DataCombiner;
import com.hivemq.configuration.entity.pulse.PulseAssetEntity;
import com.hivemq.configuration.entity.pulse.PulseAssetMappingEntity;
import com.hivemq.configuration.entity.pulse.PulseAssetMappingStatus;
import com.hivemq.edge.api.model.Combiner;
import com.hivemq.edge.api.model.DataCombining;
import com.hivemq.edge.api.model.DataCombiningDestination;
import com.hivemq.edge.api.model.DataCombiningSources;
import com.hivemq.edge.api.model.DataIdentifierReference;
import com.hivemq.edge.api.model.EntityType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class PulseApiImplUpdateAssetMapperTest extends AbstractPulseApiImplTest {
    @Test
    public void whenConfigNotWritable_thenReturnsConfigWritingDisabledError() {
        when(systemInformation.isConfigWriteable()).thenReturn(false);
        final Combiner combiner = createCombiner(EntityType.PULSE_AGENT, DataIdentifierReference.TypeEnum.TOPIC_FILTER);
        try (final Response response = pulseApi.updateAssetMapper(combiner.getId(), combiner)) {
            assertThat(response.getStatus()).isEqualTo(403);
            assertThat(response.getEntity()).isInstanceOf(ConfigWritingDisabled.class);
        }
    }

    @Test
    public void whenAssetMapperDoesNotExist_thenReturnsAssetMapperNotFoundError() {
        final Combiner combiner = createCombiner(EntityType.PULSE_AGENT, DataIdentifierReference.TypeEnum.TOPIC_FILTER);
        when(assetMappingExtractor.getCombinerById(any())).thenReturn(Optional.empty());
        try (final Response response = pulseApi.updateAssetMapper(combiner.getId(), combiner)) {
            assertThat(response.getStatus()).isEqualTo(404);
            assertThat(response.getEntity()).isInstanceOf(AssetMapperNotFoundError.class);
        }
    }

    @Test
    public void whenTypeIsPulseAssetAndTopicMismatches_thenReturnsInvalidManagedAssetTopicError() {
        final Combiner combiner = createCombiner(EntityType.PULSE_AGENT, DataIdentifierReference.TypeEnum.TOPIC_FILTER);
        when(assetMappingExtractor.getCombinerById(any())).thenReturn(Optional.of(DataCombiner.fromModel(combiner)));
        when(pulseAssetsEntity.getPulseAssetEntities())
                .thenReturn(List.of(PulseAssetEntity.builder()
                        .id(combiner.getMappings()
                                .getItems()
                                .getFirst()
                                .getDestination()
                                .getAssetId())
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
            assertThat(response.getEntity()).isInstanceOf(InvalidManagedAssetTopicError.class);
        }
    }

    @Test
    public void whenTypeIsPulseAssetAndSchemaMismatches_thenReturnsInvalidManagedAssetSchemaError() {
        final Combiner combiner = createCombiner(EntityType.PULSE_AGENT, DataIdentifierReference.TypeEnum.TOPIC_FILTER);
        when(assetMappingExtractor.getCombinerById(any())).thenReturn(Optional.of(DataCombiner.fromModel(combiner)));
        when(pulseAssetsEntity.getPulseAssetEntities())
                .thenReturn(List.of(PulseAssetEntity.builder()
                        .id(combiner.getMappings()
                                .getItems()
                                .getFirst()
                                .getDestination()
                                .getAssetId())
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
            assertThat(response.getEntity()).isInstanceOf(InvalidManagedAssetSchemaError.class);
        }
    }

    @Test
    public void whenTypeIsPulseAssetAndMappingIdMismatches_thenReturnsInvalidManagedAssetMappingIdError() {
        final Combiner combiner = createCombiner(EntityType.PULSE_AGENT, DataIdentifierReference.TypeEnum.TOPIC_FILTER);
        when(assetMappingExtractor.getCombinerById(any())).thenReturn(Optional.of(DataCombiner.fromModel(combiner)));
        when(pulseAssetsEntity.getPulseAssetEntities())
                .thenReturn(List.of(PulseAssetEntity.builder()
                        .id(combiner.getMappings()
                                .getItems()
                                .getFirst()
                                .getDestination()
                                .getAssetId())
                        .name(combiner.getName())
                        .description(combiner.getDescription())
                        .topic(combiner.getMappings()
                                .getItems()
                                .getFirst()
                                .getDestination()
                                .getTopic())
                        .schema(combiner.getMappings()
                                .getItems()
                                .getFirst()
                                .getDestination()
                                .getSchema())
                        .mapping(PulseAssetMappingEntity.builder()
                                .id(UUID.randomUUID())
                                .status(PulseAssetMappingStatus.UNMAPPED)
                                .build())
                        .build()));
        try (final Response response = pulseApi.updateAssetMapper(combiner.getId(), combiner)) {
            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getEntity()).isInstanceOf(InvalidManagedAssetMappingIdError.class);
        }
    }

    @Test
    public void whenTypeIsPulseAssetAndAssetIdsAreSwapped_thenReturnsOK() {
        final Combiner combiner = createCombiner(EntityType.PULSE_AGENT, DataIdentifierReference.TypeEnum.TOPIC_FILTER);
        final UUID assetId = UUID.randomUUID();
        final UUID mappingId = UUID.randomUUID();
        combiner.getMappings()
                .getItems()
                .add(DataCombining.builder()
                        .id(UUID.randomUUID())
                        .sources(DataCombiningSources.builder()
                                .primary(combiner.getMappings()
                                        .getItems()
                                        .getFirst()
                                        .getSources()
                                        .getPrimary())
                                .build())
                        .destination(DataCombiningDestination.builder()
                                .assetId(UUID.randomUUID())
                                .topic("topic")
                                .schema("{}")
                                .build())
                        .instructions(
                                combiner.getMappings().getItems().getFirst().getInstructions())
                        .build());
        combiner.getMappings().getItems().getLast().setId(mappingId);
        combiner.getMappings().getItems().getLast().getDestination().setAssetId(assetId);
        when(assetMappingExtractor.getCombinerById(any())).thenReturn(Optional.of(DataCombiner.fromModel(combiner)));
        combiner.getMappings()
                .getItems()
                .getLast()
                .setId(combiner.getMappings().getItems().getFirst().getId());
        combiner.getMappings()
                .getItems()
                .getLast()
                .getDestination()
                .setAssetId(combiner.getMappings()
                        .getItems()
                        .getFirst()
                        .getDestination()
                        .getAssetId());
        combiner.getMappings().getItems().getFirst().setId(mappingId);
        combiner.getMappings().getItems().getFirst().getDestination().setAssetId(assetId);
        when(assetMappingExtractor.getAssetIdSet())
                .thenReturn(combiner.getMappings().getItems().stream()
                        .map(com.hivemq.edge.api.model.DataCombining::getDestination)
                        .map(com.hivemq.edge.api.model.DataCombiningDestination::getAssetId)
                        .map(UUID::toString)
                        .collect(Collectors.toSet()));
        when(pulseAssetsEntity.getPulseAssetEntities())
                .thenReturn(combiner.getMappings().getItems().stream()
                        .map(dataCombining -> PulseAssetEntity.builder()
                                .id(dataCombining.getDestination().getAssetId())
                                .name(combiner.getName())
                                .description(combiner.getDescription())
                                .topic(dataCombining.getDestination().getTopic())
                                .schema(dataCombining.getDestination().getSchema())
                                .mapping(PulseAssetMappingEntity.builder()
                                        .id(dataCombining.getId())
                                        .status(PulseAssetMappingStatus.UNMAPPED)
                                        .build())
                                .build())
                        .toList());
        when(assetMappingExtractor.updateDataCombiner(any())).thenReturn(true);
        try (final Response response = pulseApi.updateAssetMapper(combiner.getId(), combiner)) {
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    public void whenTypeIsPulseAssetAndAllCorrect_thenReturnsOK() {
        final Combiner combiner = createCombiner(EntityType.PULSE_AGENT, DataIdentifierReference.TypeEnum.TOPIC_FILTER);
        when(assetMappingExtractor.getCombinerById(any())).thenReturn(Optional.of(DataCombiner.fromModel(combiner)));
        when(assetMappingExtractor.updateDataCombiner(any())).thenReturn(true);
        when(pulseAssetsEntity.getPulseAssetEntities())
                .thenReturn(List.of(PulseAssetEntity.builder()
                        .id(combiner.getMappings()
                                .getItems()
                                .getFirst()
                                .getDestination()
                                .getAssetId())
                        .name(combiner.getName())
                        .description(combiner.getDescription())
                        .topic(combiner.getMappings()
                                .getItems()
                                .getFirst()
                                .getDestination()
                                .getTopic())
                        .schema(combiner.getMappings()
                                .getItems()
                                .getFirst()
                                .getDestination()
                                .getSchema())
                        .mapping(PulseAssetMappingEntity.builder()
                                .id(combiner.getMappings().getItems().getFirst().getId())
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

    @Test
    public void whenPrimaryReferenceTypeIsPulseAsset_thenReturnsInvalidDataIdentifierReferenceTypeError() {
        final Combiner combiner = createCombiner(EntityType.PULSE_AGENT, DataIdentifierReference.TypeEnum.TOPIC_FILTER);
        combiner.getMappings()
                .getItems()
                .forEach(dataCombining ->
                        dataCombining.getSources().getPrimary().setType(DataIdentifierReference.TypeEnum.PULSE_ASSET));
        when(assetMappingExtractor.getCombinerById(any())).thenReturn(Optional.of(DataCombiner.fromModel(combiner)));
        when(pulseAssetsEntity.getPulseAssetEntities())
                .thenReturn(combiner.getMappings().getItems().stream()
                        .map(dataCombining -> PulseAssetEntity.builder()
                                .id(dataCombining.getDestination().getAssetId())
                                .name(combiner.getName())
                                .description(combiner.getDescription())
                                .topic(dataCombining.getDestination().getTopic())
                                .schema(dataCombining.getDestination().getSchema())
                                .mapping(PulseAssetMappingEntity.builder()
                                        .id(dataCombining.getId())
                                        .status(PulseAssetMappingStatus.UNMAPPED)
                                        .build())
                                .build())
                        .toList());
        try (final Response response = pulseApi.updateAssetMapper(combiner.getId(), combiner)) {
            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getEntity()).isInstanceOf(InvalidDataIdentifierReferenceTypeForAssetMapperError.class);
        }
    }

    @Test
    public void whenInstructionReferenceTypeIsPulseAsset_thenReturnsInvalidDataIdentifierReferenceTypeError() {
        final Combiner combiner = createCombiner(EntityType.PULSE_AGENT, DataIdentifierReference.TypeEnum.TOPIC_FILTER);
        combiner.getMappings().getItems().forEach(dataCombining -> dataCombining
                .getInstructions()
                .forEach(instruction ->
                        instruction.getSourceRef().setType(DataIdentifierReference.TypeEnum.PULSE_ASSET)));
        when(assetMappingExtractor.getCombinerById(any())).thenReturn(Optional.of(DataCombiner.fromModel(combiner)));
        when(pulseAssetsEntity.getPulseAssetEntities())
                .thenReturn(combiner.getMappings().getItems().stream()
                        .map(dataCombining -> PulseAssetEntity.builder()
                                .id(dataCombining.getDestination().getAssetId())
                                .name(combiner.getName())
                                .description(combiner.getDescription())
                                .topic(dataCombining.getDestination().getTopic())
                                .schema(dataCombining.getDestination().getSchema())
                                .mapping(PulseAssetMappingEntity.builder()
                                        .id(dataCombining.getId())
                                        .status(PulseAssetMappingStatus.UNMAPPED)
                                        .build())
                                .build())
                        .toList());
        try (final Response response = pulseApi.updateAssetMapper(combiner.getId(), combiner)) {
            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getEntity()).isInstanceOf(InvalidDataIdentifierReferenceTypeForAssetMapperError.class);
        }
    }
}
