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
import static org.mockito.Mockito.when;

import com.hivemq.api.errors.InternalServerError;
import com.hivemq.api.errors.pulse.PulseAgentDeactivatedError;
import com.hivemq.api.errors.pulse.PulseAgentNotConnectedError;
import com.hivemq.edge.api.model.ManagedAsset;
import com.hivemq.edge.api.model.ManagedAssetList;
import com.hivemq.pulse.asset.PulseAgentAsset;
import com.hivemq.pulse.asset.PulseAgentAssetMapping;
import com.hivemq.pulse.asset.PulseAgentAssetMappingStatus;
import com.hivemq.pulse.converters.PulseAgentAssetMappingStatusConverter;
import com.hivemq.pulse.converters.PulseAgentAssetSchemaConverter;
import com.hivemq.pulse.status.Status;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class PulseApiImplGetManagedAssetsTest extends AbstractPulseApiImplTest {
    @Test
    public void whenActivationStatusIsDeactivated_thenReturnsPulseAgentDeactivatedError() {
        when(statusProvider.getStatus())
                .thenReturn(
                        new Status(Status.ActivationStatus.DEACTIVATED, Status.ConnectionStatus.CONNECTED, List.of()));
        try (final Response response = pulseApi.getManagedAssets()) {
            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getEntity()).isInstanceOf(PulseAgentDeactivatedError.class);
        }
    }

    @Test
    public void whenActivationStatusIsError_thenReturnsInternalServerError() {
        when(statusProvider.getStatus())
                .thenReturn(new Status(Status.ActivationStatus.ERROR, Status.ConnectionStatus.CONNECTED, List.of()));
        try (final Response response = pulseApi.getManagedAssets()) {
            assertThat(response.getStatus()).isEqualTo(500);
            assertThat(response.getEntity()).isInstanceOf(InternalServerError.class);
        }
    }

    @Test
    public void whenConnectionStatusIsDisconnected_thenReturnsPulseAgentNotConnectedError() {
        when(statusProvider.getStatus())
                .thenReturn(
                        new Status(Status.ActivationStatus.ACTIVATED, Status.ConnectionStatus.DISCONNECTED, List.of()));
        try (final Response response = pulseApi.getManagedAssets()) {
            assertThat(response.getStatus()).isEqualTo(503);
            assertThat(response.getEntity()).isInstanceOf(PulseAgentNotConnectedError.class);
        }
    }

    @Test
    public void whenConnectionStatusIsError_thenReturnsInternalServerError() {
        when(statusProvider.getStatus())
                .thenReturn(new Status(Status.ActivationStatus.ACTIVATED, Status.ConnectionStatus.ERROR, List.of()));
        try (final Response response = pulseApi.getManagedAssets()) {
            assertThat(response.getStatus()).isEqualTo(500);
            assertThat(response.getEntity()).isInstanceOf(InternalServerError.class);
        }
    }

    @Test
    public void whenNoAssets_thenReturnsNoAssets() {
        when(statusProvider.getStatus())
                .thenReturn(
                        new Status(Status.ActivationStatus.ACTIVATED, Status.ConnectionStatus.CONNECTED, List.of()));
        when(pulseAssetsEntity.getPulseAssetEntities()).thenReturn(List.of());
        try (final Response response = pulseApi.getManagedAssets()) {
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getEntity()).isInstanceOf(ManagedAssetList.class);
            final ManagedAssetList assetList = (ManagedAssetList) response.getEntity();
            assertThat(assetList).isNotNull();
            assertThat(assetList.getItems()).isEmpty();
        }
    }

    @Test
    public void whenAssetsWithoutMappings_thenReturnsAssetsWithMappingStatusUnmapped() {
        when(statusProvider.getStatus())
                .thenReturn(
                        new Status(Status.ActivationStatus.ACTIVATED, Status.ConnectionStatus.CONNECTED, List.of()));
        final PulseAgentAsset expectedAsset = new PulseAgentAsset.Builder()
                .id(UUID.randomUUID())
                .name("Test Asset")
                .description("A test asset")
                .topic("test/topic")
                .schema("{}")
                .mapping(PulseAgentAssetMapping.builder().build())
                .build();
        when(pulseAssetsEntity.getPulseAssetEntities()).thenReturn(List.of(expectedAsset.toPersistence()));
        try (final Response response = pulseApi.getManagedAssets()) {
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getEntity()).isInstanceOf(ManagedAssetList.class);
            final ManagedAssetList assetList = (ManagedAssetList) response.getEntity();
            assertThat(assetList).isNotNull();
            assertThat(assetList.getItems()).hasSize(1);
            final ManagedAsset asset = assetList.getItems().getFirst();
            assertThat(asset.getId()).isEqualTo(expectedAsset.getId());
            assertThat(asset.getName()).isEqualTo(expectedAsset.getName());
            assertThat(asset.getDescription()).isEqualTo(expectedAsset.getDescription());
            assertThat(asset.getTopic()).isEqualTo(expectedAsset.getTopic());
            assertThat(PulseAgentAssetSchemaConverter.INSTANCE.toInternalEntity(asset.getSchema()))
                    .isEqualTo(expectedAsset.getSchema());
            assertThat(asset.getMapping()).isNotNull();
            assertThat(asset.getMapping().getMappingId()).isNull();
            assertThat(asset.getMapping().getStatus())
                    .isEqualTo(PulseAgentAssetMappingStatusConverter.INSTANCE.toRestEntity(
                            PulseAgentAssetMappingStatus.UNMAPPED));
            assertThat(PulseAgentAssetMappingStatusConverter.INSTANCE.toInternalEntity(
                            asset.getMapping().getStatus()))
                    .isEqualTo(expectedAsset.getMapping().getStatus());
        }
    }

    @Test
    public void whenAssetsWithMappings_thenReturnsAssetsWithMappings() {
        when(statusProvider.getStatus())
                .thenReturn(
                        new Status(Status.ActivationStatus.ACTIVATED, Status.ConnectionStatus.CONNECTED, List.of()));
        final UUID id = UUID.randomUUID();
        final UUID mappingId = UUID.randomUUID();
        final PulseAgentAsset expectedAsset = new PulseAgentAsset.Builder()
                .id(id)
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
        try (final Response response = pulseApi.getManagedAssets()) {
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getEntity()).isInstanceOf(ManagedAssetList.class);
            final ManagedAssetList assetList = (ManagedAssetList) response.getEntity();
            assertThat(assetList).isNotNull();
            assertThat(assetList.getItems()).hasSize(1);
            final ManagedAsset asset = assetList.getItems().getFirst();
            assertThat(asset.getId()).isEqualTo(id);
            assertThat(asset.getName()).isEqualTo(expectedAsset.getName());
            assertThat(asset.getDescription()).isEqualTo(expectedAsset.getDescription());
            assertThat(asset.getTopic()).isEqualTo(expectedAsset.getTopic());
            assertThat(PulseAgentAssetSchemaConverter.INSTANCE.toInternalEntity(asset.getSchema()))
                    .isEqualTo(expectedAsset.getSchema());
            assertThat(asset.getMapping()).isNotNull();
            assertThat(asset.getMapping().getMappingId()).isEqualTo(mappingId);
            assertThat(PulseAgentAssetMappingStatusConverter.INSTANCE.toInternalEntity(
                            asset.getMapping().getStatus()))
                    .isEqualTo(expectedAsset.getMapping().getStatus());
        }
    }
}
