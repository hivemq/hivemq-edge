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

package com.hivemq.api.pulse;

import com.hivemq.configuration.entity.pulse.PulseEntity;
import com.hivemq.edge.api.model.AssetMapping;
import com.hivemq.edge.api.model.ManagedAsset;
import com.hivemq.pulse.asset.PulseAgentAsset;
import com.hivemq.pulse.asset.PulseAgentAssetMapping;
import com.hivemq.pulse.asset.PulseAgentAssetMappingStatus;
import com.hivemq.pulse.converters.PulseAgentAssetConverter;
import com.hivemq.pulse.converters.PulseAgentAssetMappingConverter;
import com.hivemq.pulse.converters.PulseAgentAssetMappingStatusConverter;
import com.hivemq.pulse.converters.PulseAgentAssetSchemaConverter;
import com.hivemq.pulse.status.Status;
import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PulseApiImplAddManagedAssetTest extends AbstractPulseApiImplTest {
    @Test
    public void whenAssetDoesNotExist_thenReturnsManagedAssetNotFoundError() {
        when(statusProvider.getStatus()).thenReturn(new Status(Status.ActivationStatus.ACTIVATED,
                Status.ConnectionStatus.CONNECTED,
                List.of()));
        when(pulseAssetsEntity.getPulseAssetEntities()).thenReturn(List.of());
        try (final Response response = pulseApi.addManagedAsset(ManagedAsset.builder()
                .id(UUID.randomUUID())
                .name("Test Asset")
                .description("A test asset")
                .topic("test/topic")
                .schema("{}")
                .mapping(AssetMapping.builder().status(AssetMapping.StatusEnum.DRAFT).build())
                .build())) {
            assertThat(response.getStatus()).isEqualTo(404);
        }
    }

    @Test
    public void whenAssetExistsAndMappingExists_thenReturnsManagedAssetAlreadyExistsError() {
        when(statusProvider.getStatus()).thenReturn(new Status(Status.ActivationStatus.ACTIVATED,
                Status.ConnectionStatus.CONNECTED,
                List.of()));
        final UUID id = UUID.randomUUID();
        final PulseAgentAsset expectedAsset = new PulseAgentAsset.Builder().id(id)
                .name("Test Asset")
                .description("A test asset")
                .topic("test/topic")
                .schema("{}")
                .mapping(PulseAgentAssetMapping.builder().id(id).status(PulseAgentAssetMappingStatus.STREAMING).build())
                .build();
        when(pulseAssetsEntity.getPulseAssetEntities()).thenReturn(List.of(expectedAsset.toPersistence()));
        try (final Response response = pulseApi.addManagedAsset(PulseAgentAssetConverter.INSTANCE.toRestEntity(
                expectedAsset))) {
            assertThat(response.getStatus()).isEqualTo(409);
        }
    }

    @ParameterizedTest
    @EnumSource(PulseAgentAssetMappingStatus.class)
    public void whenAssetExistsAndMappingDoesNotExist_thenReturnsOK(final @NotNull PulseAgentAssetMappingStatus expectedStatus) {
        when(statusProvider.getStatus()).thenReturn(new Status(Status.ActivationStatus.ACTIVATED,
                Status.ConnectionStatus.CONNECTED,
                List.of()));
        final UUID id = UUID.randomUUID();
        final PulseAgentAsset expectedAsset = new PulseAgentAsset.Builder().id(id)
                .name("Test Asset")
                .description("A test asset")
                .topic("test/topic")
                .schema("{}")
                .mapping(PulseAgentAssetMapping.builder().status(PulseAgentAssetMappingStatus.UNMAPPED).build())
                .build();
        when(pulseAssetsEntity.getPulseAssetEntities()).thenReturn(List.of(expectedAsset.toPersistence()));
        try (final Response response = pulseApi.addManagedAsset(PulseAgentAssetConverter.INSTANCE.toRestEntity(
                        expectedAsset)
                .name("New name")
                .description("New description")
                .topic("new/topic")
                .schema("{[]}")
                .mapping(PulseAgentAssetMappingConverter.INSTANCE.toRestEntity(PulseAgentAssetMapping.builder()
                        .id(id)
                        .status(expectedStatus)
                        .build())))) {
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getEntity()).isInstanceOf(ManagedAsset.class);
            final ManagedAsset asset = (ManagedAsset) response.getEntity();
            assertThat(asset.getId()).as("ID cannot be changed.").isEqualTo(id);
            assertThat(asset.getName()).as("Name cannot be changed.").isEqualTo(expectedAsset.getName());
            assertThat(asset.getDescription()).as("Description can be changed.").isEqualTo("New description");
            assertThat(asset.getTopic()).as("Topic cannot be changed.").isEqualTo(expectedAsset.getTopic());
            assertThat(PulseAgentAssetSchemaConverter.INSTANCE.toInternalEntity(asset.getSchema())).as(
                    "Schema cannot be changed.").isEqualTo(expectedAsset.getSchema());
            assertThat(asset.getMapping()).isNotNull();
            assertThat(asset.getMapping().getMappingId()).isEqualTo(id);
            assertThat(PulseAgentAssetMappingStatusConverter.INSTANCE.toInternalEntity(asset.getMapping()
                    .getStatus())).isEqualTo(expectedStatus);
            final ArgumentCaptor<PulseEntity> assetsArgumentCaptor = ArgumentCaptor.forClass(PulseEntity.class);
            verify(pulseExtractor).setPulseEntity(assetsArgumentCaptor.capture());
            assertThat(assetsArgumentCaptor.getValue()).isNotNull();
            assertThat(assetsArgumentCaptor.getValue().getPulseAssetsEntity()).isNotNull();
            assertThat(assetsArgumentCaptor.getValue().getPulseAssetsEntity().getPulseAssetEntities()).hasSize(1);
        }
    }
}
