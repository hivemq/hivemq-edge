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

import com.hivemq.api.errors.AlreadyExistsError;
import com.hivemq.api.errors.ConfigWritingDisabled;
import com.hivemq.api.errors.pulse.EmptyMappingsForAssetMapperError;
import com.hivemq.api.errors.pulse.EmptySourcesForAssetMapperError;
import com.hivemq.api.errors.pulse.InvalidManagedAssetMappingIdError;
import com.hivemq.api.errors.pulse.InvalidManagedAssetSchemaError;
import com.hivemq.api.errors.pulse.InvalidManagedAssetTopicError;
import com.hivemq.api.errors.pulse.ManagedAssetNotFoundError;
import com.hivemq.api.errors.pulse.MissingEntityTypePulseAgentForAssetMapperError;
import com.hivemq.combining.model.DataCombiner;
import com.hivemq.configuration.entity.pulse.PulseAssetEntity;
import com.hivemq.configuration.entity.pulse.PulseAssetMappingEntity;
import com.hivemq.configuration.entity.pulse.PulseAssetMappingStatus;
import com.hivemq.edge.api.model.Combiner;
import com.hivemq.edge.api.model.DataCombining;
import com.hivemq.edge.api.model.DataCombiningList;
import com.hivemq.edge.api.model.DataIdentifierReference;
import com.hivemq.edge.api.model.EntityReferenceList;
import com.hivemq.edge.api.model.EntityType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class PulseApiImplAddAssetMapperTest extends AbstractPulseApiImplTest {
    @Test
    public void whenConfigNotWritable_thenReturnsConfigWritingDisabledError() {
        final Combiner combiner = createCombiner(EntityType.PULSE_AGENT, DataIdentifierReference.TypeEnum.PULSE_ASSET);
        when(systemInformation.isConfigWriteable()).thenReturn(false);
        try (final Response response = pulseApi.addAssetMapper(combiner)) {
            assertThat(response.getStatus()).isEqualTo(403);
            assertThat(response.getEntity()).isInstanceOf(ConfigWritingDisabled.class);
        }
    }

    @Test
    public void whenCombinerExists_thenReturnsAlreadyExistsError() {
        final Combiner combiner = createCombiner(EntityType.PULSE_AGENT, DataIdentifierReference.TypeEnum.PULSE_ASSET);
        when(assetMappingExtractor.getCombinerById(any())).thenReturn(Optional.of(DataCombiner.fromModel(combiner)));
        try (final Response response = pulseApi.addAssetMapper(combiner)) {
            assertThat(response.getStatus()).isEqualTo(409);
            assertThat(response.getEntity()).isInstanceOf(AlreadyExistsError.class);
        }
    }

    @Test
    public void whenTypeIsPulseAssetAndAssetNotFound_thenReturnsManagedAssetNotFoundError() {
        final Combiner combiner = createCombiner(EntityType.PULSE_AGENT, DataIdentifierReference.TypeEnum.PULSE_ASSET);
        when(assetMappingExtractor.getCombinerById(any())).thenReturn(Optional.empty());
        try (final Response response = pulseApi.addAssetMapper(combiner)) {
            assertThat(response.getStatus()).isEqualTo(404);
            assertThat(response.getEntity()).isInstanceOf(ManagedAssetNotFoundError.class);
        }
    }

    @Test
    public void whenSourcesAreEmpty_thenReturnsEmptySourcesForAssetMapperError() {
        final Combiner combiner =
                createCombiner(EntityType.PULSE_AGENT, DataIdentifierReference.TypeEnum.PULSE_ASSET).sources(
                        EntityReferenceList.builder().build()).mappings(DataCombiningList.builder().build());
        try (final Response response = pulseApi.addAssetMapper(combiner)) {
            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getEntity()).isInstanceOf(EmptySourcesForAssetMapperError.class);
        }
    }

    @Test
    public void whenMappingsAreEmpty_thenReturnsEmptyMappingsForAssetMapperError() {
        final Combiner combiner =
                createCombiner(EntityType.PULSE_AGENT, DataIdentifierReference.TypeEnum.PULSE_ASSET).mappings(
                        DataCombiningList.builder().build());
        try (final Response response = pulseApi.addAssetMapper(combiner)) {
            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getEntity()).isInstanceOf(EmptyMappingsForAssetMapperError.class);
        }
    }

    @Test
    public void whenTypeIsPulseAssetAndTopicMismatches_thenReturnsInvalidManagedAssetTopicError() {
        final Combiner combiner = createCombiner(EntityType.PULSE_AGENT, DataIdentifierReference.TypeEnum.PULSE_ASSET);
        when(assetMappingExtractor.getCombinerById(any())).thenReturn(Optional.empty());
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
        try (final Response response = pulseApi.addAssetMapper(combiner)) {
            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getEntity()).isInstanceOf(InvalidManagedAssetTopicError.class);
        }
    }

    @Test
    public void whenTypeIsPulseAssetAndSchemaMismatches_thenReturnsInvalidManagedAssetSchemaError() {
        final Combiner combiner = createCombiner(EntityType.PULSE_AGENT, DataIdentifierReference.TypeEnum.PULSE_ASSET);
        when(assetMappingExtractor.getCombinerById(any())).thenReturn(Optional.empty());
        when(pulseAssetsEntity.getPulseAssetEntities()).thenReturn(List.of(PulseAssetEntity.builder()
                .id(combiner.getMappings().getItems().get(0).getDestination().getAssetId())
                .name(combiner.getName())
                .description(combiner.getDescription())
                .topic(combiner.getMappings().getItems().get(0).getDestination().getTopic())
                .schema("{ \"type\": \"object\" }")
                .mapping(PulseAssetMappingEntity.builder()
                        .id(UUID.randomUUID())
                        .status(PulseAssetMappingStatus.UNMAPPED)
                        .build())
                .build()));
        try (final Response response = pulseApi.addAssetMapper(combiner)) {
            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getEntity()).isInstanceOf(InvalidManagedAssetSchemaError.class);
        }
    }

    @Test
    public void whenTypeIsPulseAssetAndMappingIdIsNull_thenReturnsInvalidManagedAssetMappingIdError() {
        final Combiner combiner = createCombiner(EntityType.PULSE_AGENT, DataIdentifierReference.TypeEnum.PULSE_ASSET);
        when(assetMappingExtractor.getCombinerById(any())).thenReturn(Optional.empty());
        when(pulseAssetsEntity.getPulseAssetEntities()).thenReturn(List.of(PulseAssetEntity.builder()
                .id(combiner.getMappings().getItems().get(0).getDestination().getAssetId())
                .name(combiner.getName())
                .description(combiner.getDescription())
                .topic(combiner.getMappings().getItems().get(0).getDestination().getTopic())
                .schema(combiner.getMappings().getItems().get(0).getDestination().getSchema())
                .mapping(PulseAssetMappingEntity.builder().id(null).status(PulseAssetMappingStatus.UNMAPPED).build())
                .build()));
        try (final Response response = pulseApi.addAssetMapper(combiner)) {
            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getEntity()).isInstanceOf(InvalidManagedAssetMappingIdError.class);
        }
    }

    @Test
    public void whenTypeIsPulseAssetAndMappingIdMismatches_thenReturnsInvalidManagedAssetMappingIdError() {
        final Combiner combiner = createCombiner(EntityType.PULSE_AGENT, DataIdentifierReference.TypeEnum.PULSE_ASSET);
        when(assetMappingExtractor.getCombinerById(any())).thenReturn(Optional.empty());
        when(pulseAssetsEntity.getPulseAssetEntities()).thenReturn(List.of(PulseAssetEntity.builder()
                .id(combiner.getMappings().getItems().get(0).getDestination().getAssetId())
                .name(combiner.getName())
                .description(combiner.getDescription())
                .topic(combiner.getMappings().getItems().get(0).getDestination().getTopic())
                .schema(combiner.getMappings().getItems().get(0).getDestination().getSchema())
                .mapping(PulseAssetMappingEntity.builder()
                        .id(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                        .status(PulseAssetMappingStatus.UNMAPPED)
                        .build())
                .build()));
        try (final Response response = pulseApi.addAssetMapper(combiner)) {
            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getEntity()).isInstanceOf(InvalidManagedAssetMappingIdError.class);
        }
    }

    @Test
    public void whenEntityTypeIsNotPulseAgentAndTypeIsNotPulseAsset_thenReturnsMissingEntityTypePulseAgentForAssetMapperError() {
        Stream.of(EntityType.values())
                .filter(entityType -> entityType != EntityType.PULSE_AGENT)
                .forEach(entityType -> Stream.of(DataIdentifierReference.TypeEnum.values())
                        .filter(type -> type != DataIdentifierReference.TypeEnum.PULSE_ASSET)
                        .forEach(type -> {
                            final Combiner combiner = createCombiner(entityType, type);
                            when(assetMappingExtractor.getCombinerById(any())).thenReturn(Optional.empty());
                            when(pulseAssetsEntity.getPulseAssetEntities()).thenReturn(List.of());
                            try (final Response response = pulseApi.addAssetMapper(combiner)) {
                                assertThat(response.getStatus()).isEqualTo(400);
                                assertThat(response.getEntity()).isInstanceOf(
                                        MissingEntityTypePulseAgentForAssetMapperError.class);
                            }
                        }));
    }

    @Test
    public void whenAllCorrect_thenReturnsOK() {
        final Combiner combiner = createCombiner(EntityType.PULSE_AGENT, DataIdentifierReference.TypeEnum.PULSE_ASSET);
        when(assetMappingExtractor.getCombinerById(any())).thenReturn(Optional.empty());
        when(pulseAssetsEntity.getPulseAssetEntities()).thenReturn(combiner.getMappings()
                .getItems()
                .stream()
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
        try (final Response response = pulseApi.addAssetMapper(combiner)) {
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }
}
