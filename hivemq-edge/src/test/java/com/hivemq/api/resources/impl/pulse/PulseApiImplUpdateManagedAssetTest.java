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

import com.hivemq.api.errors.ConfigWritingDisabled;
import com.hivemq.api.errors.pulse.InvalidManagedAssetMappingIdError;
import com.hivemq.api.errors.pulse.ManagedAssetNotFoundError;
import com.hivemq.configuration.entity.pulse.PulseAssetEntity;
import com.hivemq.configuration.entity.pulse.PulseAssetMappingStatus;
import com.hivemq.configuration.entity.pulse.PulseEntity;
import com.hivemq.pulse.asset.PulseAgentAsset;
import com.hivemq.pulse.asset.PulseAgentAssetMapping;
import com.hivemq.pulse.asset.PulseAgentAssetMappingStatus;
import com.hivemq.pulse.converters.PulseAgentAssetConverter;
import com.hivemq.pulse.status.Status;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PulseApiImplUpdateManagedAssetTest extends AbstractPulseApiImplTest {
    @Test
    public void whenConfigNotWritable_thenReturnsConfigWritingDisabledError() {
        when(statusProvider.getStatus()).thenReturn(new Status(Status.ActivationStatus.ACTIVATED,
                Status.ConnectionStatus.CONNECTED,
                List.of()));
        when(systemInformation.isConfigWriteable()).thenReturn(false);
        final PulseAgentAsset expectedAsset = new PulseAgentAsset.Builder().id(UUID.randomUUID())
                .name("Test Asset")
                .description("A test asset")
                .topic("test/topic")
                .schema("{}")
                .mapping(PulseAgentAssetMapping.builder().build())
                .build();
        when(pulseAssetsEntity.getPulseAssetEntities()).thenReturn(List.of());
        try (final Response response = pulseApi.updateManagedAsset(expectedAsset.getId(),
                PulseAgentAssetConverter.INSTANCE.toRestEntity(expectedAsset))) {
            assertThat(response.getStatus()).isEqualTo(403);
            assertThat(response.getEntity()).isInstanceOf(ConfigWritingDisabled.class);
        }
    }

    @Test
    public void whenMappingIdIsNull_thenReturnsInvalidManagedAssetMappingIdError() {
        when(statusProvider.getStatus()).thenReturn(new Status(Status.ActivationStatus.ACTIVATED,
                Status.ConnectionStatus.CONNECTED,
                List.of()));
        final PulseAgentAsset expectedAsset = new PulseAgentAsset.Builder().id(UUID.randomUUID())
                .name("Test Asset")
                .description("A test asset")
                .topic("test/topic")
                .schema("{}")
                .mapping(PulseAgentAssetMapping.builder().build())
                .build();
        when(pulseAssetsEntity.getPulseAssetEntities()).thenReturn(List.of());
        try (final Response response = pulseApi.updateManagedAsset(expectedAsset.getId(),
                PulseAgentAssetConverter.INSTANCE.toRestEntity(expectedAsset))) {
            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getEntity()).isInstanceOf(InvalidManagedAssetMappingIdError.class);
        }
    }

    @Test
    public void whenAssetDoesNotExist_thenReturnsManagedAssetNotFoundError() {
        when(statusProvider.getStatus()).thenReturn(new Status(Status.ActivationStatus.ACTIVATED,
                Status.ConnectionStatus.CONNECTED,
                List.of()));
        final PulseAgentAsset expectedAsset = new PulseAgentAsset.Builder().id(UUID.randomUUID())
                .name("Test Asset")
                .description("A test asset")
                .topic("test/topic")
                .schema("{}")
                .mapping(PulseAgentAssetMapping.builder().id(UUID.randomUUID()).build())
                .build();
        when(pulseAssetsEntity.getPulseAssetEntities()).thenReturn(List.of());
        try (final Response response = pulseApi.updateManagedAsset(expectedAsset.getId(),
                PulseAgentAssetConverter.INSTANCE.toRestEntity(expectedAsset))) {
            assertThat(response.getStatus()).isEqualTo(404);
            assertThat(response.getEntity()).isInstanceOf(ManagedAssetNotFoundError.class);
        }
    }

    @Test
    public void whenAssetExistsAndMappingIdMismatches_thenReturnsInvalidManagedAssetMappingIdError() {
        when(statusProvider.getStatus()).thenReturn(new Status(Status.ActivationStatus.ACTIVATED,
                Status.ConnectionStatus.CONNECTED,
                List.of()));
        final UUID id = UUID.randomUUID();
        final PulseAgentAsset expectedAsset = new PulseAgentAsset.Builder().id(id)
                .name("Test Asset")
                .description("A test asset")
                .topic("test/topic")
                .schema("{}")
                .mapping(PulseAgentAssetMapping.builder().id(UUID.randomUUID()).status(PulseAgentAssetMappingStatus.STREAMING).build())
                .build();
        when(pulseAssetsEntity.getPulseAssetEntities()).thenReturn(List.of(expectedAsset.toPersistence()));
        try (final Response response = pulseApi.updateManagedAsset(id,
                PulseAgentAssetConverter.INSTANCE.toRestEntity(expectedAsset.withName("New name")
                        .withDescription("New description")
                        .withTopic("new/topic")
                        .withSchema("{[]}")
                        .withMapping(PulseAgentAssetMapping.builder()
                                .id(UUID.randomUUID())
                                .status(PulseAgentAssetMappingStatus.REQUIRES_REMAPPING)
                                .build())))) {
            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getEntity()).isInstanceOf(InvalidManagedAssetMappingIdError.class);
        }
    }

    @Test
    public void whenAssetExistsAndMappingDoesNotExist_thenReturnsOK() {
        when(statusProvider.getStatus()).thenReturn(new Status(Status.ActivationStatus.ACTIVATED,
                Status.ConnectionStatus.CONNECTED,
                List.of()));
        final UUID id = UUID.randomUUID();
        final UUID mappingId = UUID.randomUUID();
        final PulseAgentAsset expectedAsset = new PulseAgentAsset.Builder().id(id)
                .name("Test Asset")
                .description("A test asset")
                .topic("test/topic")
                .schema("{}")
                .mapping(PulseAgentAssetMapping.builder().status(PulseAgentAssetMappingStatus.STREAMING).build())
                .build();
        when(pulseAssetsEntity.getPulseAssetEntities()).thenReturn(List.of(expectedAsset.toPersistence()));
        try (final Response response = pulseApi.updateManagedAsset(id,
                PulseAgentAssetConverter.INSTANCE.toRestEntity(expectedAsset.withName("New name")
                        .withDescription("New description")
                        .withTopic("new/topic")
                        .withSchema("{[]}")
                        .withMapping(PulseAgentAssetMapping.builder()
                                .id(mappingId)
                                .status(PulseAgentAssetMappingStatus.REQUIRES_REMAPPING)
                                .build())))) {
            assertThat(response.getStatus()).isEqualTo(200);
            final ArgumentCaptor<PulseEntity> assetsArgumentCaptor = ArgumentCaptor.forClass(PulseEntity.class);
            verify(pulseExtractor).setPulseEntity(assetsArgumentCaptor.capture());
            assertThat(assetsArgumentCaptor.getValue()).isNotNull();
            assertThat(assetsArgumentCaptor.getValue().getPulseAssetsEntity()).isNotNull();
            assertThat(assetsArgumentCaptor.getValue().getPulseAssetsEntity().getPulseAssetEntities()).hasSize(1);
            final PulseAssetEntity asset =
                    assetsArgumentCaptor.getValue().getPulseAssetsEntity().getPulseAssetEntities().get(0);
            assertThat(asset.getId()).as("ID cannot be changed.").isEqualTo(id);
            assertThat(asset.getName()).as("Name cannot be changed.").isEqualTo(expectedAsset.getName());
            assertThat(asset.getDescription()).as("Description cannot be changed.")
                    .isEqualTo(expectedAsset.getDescription());
            assertThat(asset.getTopic()).as("Topic cannot be changed.").isEqualTo(expectedAsset.getTopic());
            assertThat(asset.getSchema()).as("Schema cannot be changed.").isEqualTo(expectedAsset.getSchema());
            assertThat(asset.getMapping().getId()).isEqualTo(mappingId);
            assertThat(asset.getMapping().getStatus()).isEqualTo(PulseAssetMappingStatus.REQUIRES_REMAPPING);
        }
    }

    @Test
    public void whenAssetExistsAndMappingExists_thenReturnsOK() {
        when(statusProvider.getStatus()).thenReturn(new Status(Status.ActivationStatus.ACTIVATED,
                Status.ConnectionStatus.CONNECTED,
                List.of()));
        final UUID id = UUID.randomUUID();
        final UUID mappingId = UUID.randomUUID();
        final PulseAgentAsset expectedAsset = new PulseAgentAsset.Builder().id(id)
                .name("Test Asset")
                .description("A test asset")
                .topic("test/topic")
                .schema("{}")
                .mapping(PulseAgentAssetMapping.builder()
                        .id(mappingId)
                        .status(PulseAgentAssetMappingStatus.STREAMING)
                        .build())
                .build();
        when(pulseAssetsEntity.getPulseAssetEntities()).thenReturn(List.of(expectedAsset.toPersistence()));
        try (final Response response = pulseApi.updateManagedAsset(id,
                PulseAgentAssetConverter.INSTANCE.toRestEntity(expectedAsset.withName("New name")
                        .withDescription("New description")
                        .withTopic("new/topic")
                        .withSchema("{[]}")
                        .withMapping(PulseAgentAssetMapping.builder()
                                .id(mappingId)
                                .status(PulseAgentAssetMappingStatus.REQUIRES_REMAPPING)
                                .build())))) {
            assertThat(response.getStatus()).isEqualTo(200);
            final ArgumentCaptor<PulseEntity> assetsArgumentCaptor = ArgumentCaptor.forClass(PulseEntity.class);
            verify(pulseExtractor).setPulseEntity(assetsArgumentCaptor.capture());
            assertThat(assetsArgumentCaptor.getValue()).isNotNull();
            assertThat(assetsArgumentCaptor.getValue().getPulseAssetsEntity()).isNotNull();
            assertThat(assetsArgumentCaptor.getValue().getPulseAssetsEntity().getPulseAssetEntities()).hasSize(1);
            final PulseAssetEntity asset =
                    assetsArgumentCaptor.getValue().getPulseAssetsEntity().getPulseAssetEntities().get(0);
            assertThat(asset.getId()).as("ID cannot be changed.").isEqualTo(id);
            assertThat(asset.getName()).as("Name cannot be changed.").isEqualTo(expectedAsset.getName());
            assertThat(asset.getDescription()).as("Description cannot be changed.")
                    .isEqualTo(expectedAsset.getDescription());
            assertThat(asset.getTopic()).as("Topic cannot be changed.").isEqualTo(expectedAsset.getTopic());
            assertThat(asset.getSchema()).as("Schema cannot be changed.").isEqualTo(expectedAsset.getSchema());
            assertThat(asset.getMapping().getId()).isEqualTo(mappingId);
            assertThat(asset.getMapping().getStatus()).isEqualTo(PulseAssetMappingStatus.REQUIRES_REMAPPING);
        }
    }
}
