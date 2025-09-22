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
import com.hivemq.api.errors.pulse.AssetMapperReferencedError;
import com.hivemq.api.errors.pulse.ManagedAssetNotFoundError;
import com.hivemq.configuration.entity.pulse.PulseEntity;
import com.hivemq.pulse.asset.PulseAgentAsset;
import com.hivemq.pulse.asset.PulseAgentAssetMapping;
import com.hivemq.pulse.asset.PulseAgentAssetMappingStatus;
import com.hivemq.pulse.status.Status;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PulseApiImplDeleteManagedAssetTest extends AbstractPulseApiImplTest {
    @Test
    public void whenConfigNotWritable_thenReturnsConfigWritingDisabledError() {
        when(statusProvider.getStatus()).thenReturn(new Status(Status.ActivationStatus.ACTIVATED,
                Status.ConnectionStatus.CONNECTED,
                List.of()));
        when(systemInformation.isConfigWriteable()).thenReturn(false);
        when(pulseAssetsEntity.getPulseAssetEntities()).thenReturn(List.of());
        try (final Response response = pulseApi.deleteManagedAsset(UUID.randomUUID())) {
            assertThat(response.getStatus()).isEqualTo(403);
            assertThat(response.getEntity()).isInstanceOf(ConfigWritingDisabled.class);
        }
    }

    @Test
    public void whenAssetDoesNotExist_thenReturnsManagedAssetNotFoundError() {
        when(statusProvider.getStatus()).thenReturn(new Status(Status.ActivationStatus.ACTIVATED,
                Status.ConnectionStatus.CONNECTED,
                List.of()));
        when(pulseAssetsEntity.getPulseAssetEntities()).thenReturn(List.of());
        try (final Response response = pulseApi.deleteManagedAsset(UUID.randomUUID())) {
            assertThat(response.getStatus()).isEqualTo(404);
            assertThat(response.getEntity()).isInstanceOf(ManagedAssetNotFoundError.class);
        }
    }

    @Test
    public void whenAssetIsReferenced_thenReturnsAssetMapperReferencedError() {
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
        when(assetMappingExtractor.getPulseAssetMappingIdSet()).thenReturn(Set.of(mappingId.toString()));
        try (final Response response = pulseApi.deleteManagedAsset(id)) {
            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getEntity()).isInstanceOf(AssetMapperReferencedError.class);
        }
    }

    @Test
    public void whenAssetExistsButMappingDoesNotExist_thenReturnsOK() {
        when(statusProvider.getStatus()).thenReturn(new Status(Status.ActivationStatus.ACTIVATED,
                Status.ConnectionStatus.CONNECTED,
                List.of()));
        final UUID id = UUID.randomUUID();
        final PulseAgentAsset expectedAsset = new PulseAgentAsset.Builder().id(id)
                .name("Test Asset")
                .description("A test asset")
                .topic("test/topic")
                .schema("{}")
                .mapping(PulseAgentAssetMapping.builder().build())
                .build();
        when(pulseAssetsEntity.getPulseAssetEntities()).thenReturn(List.of(expectedAsset.toPersistence()));
        try (final Response response = pulseApi.deleteManagedAsset(expectedAsset.getId())) {
            assertThat(response.getStatus()).isEqualTo(200);
            final ArgumentCaptor<PulseEntity> assetsArgumentCaptor = ArgumentCaptor.forClass(PulseEntity.class);
            verify(pulseExtractor).setPulseEntity(assetsArgumentCaptor.capture());
            assertThat(assetsArgumentCaptor.getValue()).isNotNull();
            assertThat(assetsArgumentCaptor.getValue().getPulseAssetsEntity()).isNotNull();
            assertThat(assetsArgumentCaptor.getValue().getPulseAssetsEntity().getPulseAssetEntities()).isEmpty();
        }
    }

    @Test
    public void whenAssetExistsAndMappingExists_thenReturnsOK() {
        when(statusProvider.getStatus()).thenReturn(new Status(Status.ActivationStatus.ACTIVATED,
                Status.ConnectionStatus.CONNECTED,
                List.of()));
        final UUID id = UUID.randomUUID();
        final PulseAgentAsset expectedAsset = new PulseAgentAsset.Builder().id(id)
                .name("Test Asset")
                .description("A test asset")
                .topic("test/topic")
                .schema("{}")
                .mapping(PulseAgentAssetMapping.builder()
                        .id(UUID.randomUUID())
                        .status(PulseAgentAssetMappingStatus.STREAMING)
                        .build())
                .build();
        when(pulseAssetsEntity.getPulseAssetEntities()).thenReturn(List.of(expectedAsset.toPersistence()));
        try (final Response response = pulseApi.deleteManagedAsset(id)) {
            assertThat(response.getStatus()).isEqualTo(200);
            final ArgumentCaptor<PulseEntity> assetsArgumentCaptor = ArgumentCaptor.forClass(PulseEntity.class);
            verify(pulseExtractor).setPulseEntity(assetsArgumentCaptor.capture());
            assertThat(assetsArgumentCaptor.getValue()).isNotNull();
            assertThat(assetsArgumentCaptor.getValue().getPulseAssetsEntity()).isNotNull();
            assertThat(assetsArgumentCaptor.getValue().getPulseAssetsEntity().getPulseAssetEntities()).isEmpty();
        }
    }
}
