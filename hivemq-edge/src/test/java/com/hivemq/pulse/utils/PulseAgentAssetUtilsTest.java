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

package com.hivemq.pulse.utils;

import com.hivemq.configuration.entity.pulse.PulseAssetEntity;
import com.hivemq.configuration.entity.pulse.PulseAssetMappingEntity;
import com.hivemq.configuration.entity.pulse.PulseAssetMappingStatus;
import com.hivemq.configuration.entity.pulse.PulseEntity;
import com.hivemq.configuration.reader.PulseExtractor;
import com.hivemq.pulse.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PulseAgentAssetUtilsTest {
    private static final List<PulseAssetMappingStatus> STATUSES = List.of(PulseAssetMappingStatus.values());

    @Mock
    private @NotNull PulseExtractor pulseExtractor;

    private @NotNull ArgumentCaptor<PulseEntity> pulseEntityArgumentCaptor;

    private @NotNull List<PulseAssetEntity> localAssets;
    private @NotNull List<Asset> remoteAssets;

    @BeforeEach
    public void setUp() {
        pulseEntityArgumentCaptor = ArgumentCaptor.forClass(PulseEntity.class);
        localAssets = IntStream.range(0, STATUSES.size()).mapToObj(this::createLocalAsset).toList();
        remoteAssets = IntStream.range(0, STATUSES.size()).mapToObj(this::createRemoteAsset).toList();
    }

    private PulseAssetEntity createLocalAsset(final int index) {
        final UUID uuid =
                UUID.fromString("00000000-0000-0000-0000-" + "0".repeat(12 - String.valueOf(index).length()) + index);
        return new PulseAssetEntity(uuid,
                "Name " + index,
                "Description " + index,
                "topic/asset/" + index,
                "{ \"name\": \"Name " + index + "\" }",
                new PulseAssetMappingEntity(null, PulseAssetMappingStatus.UNMAPPED));
    }

    private Asset createRemoteAsset(final int index) {
        return new Asset("00000000-0000-0000-0000-" + "0".repeat(12 - String.valueOf(index).length()) + index,
                "topic/asset/" + index,
                "Name " + index,
                "Description " + index,
                "{ \"name\": \"Name " + index + "\" }");
    }

    @Test
    public void whenConfigWithoutLocalAssetsWithoutRemoteAssetsAndWithoutMappings_thenLocalAssetsShouldBeCleared() {
        final PulseEntity pulseEntity = new PulseEntity();
        when(pulseExtractor.getPulseEntity()).thenReturn(pulseEntity);
        PulseAgentAssetUtils.resolveDiff(pulseExtractor, List.of());
        verify(pulseExtractor).setPulseEntity(pulseEntityArgumentCaptor.capture());
        final PulseEntity newPulseEntity = pulseEntityArgumentCaptor.getValue();
        assertThat(newPulseEntity.getPulseAssetsEntity().getPulseAssetEntities()).hasSize(0);
    }

    @Test
    public void whenConfigWithLocalAssetsWithoutRemoteAssetsAndWithoutMappings_thenLocalAssetsShouldBeCleared() {
        final PulseEntity pulseEntity = new PulseEntity();
        pulseEntity.getPulseAssetsEntity().setPulseAssetEntities(localAssets);
        when(pulseExtractor.getPulseEntity()).thenReturn(pulseEntity);
        PulseAgentAssetUtils.resolveDiff(pulseExtractor, List.of());
        verify(pulseExtractor).setPulseEntity(pulseEntityArgumentCaptor.capture());
        final PulseEntity newPulseEntity = pulseEntityArgumentCaptor.getValue();
        assertThat(newPulseEntity.getPulseAssetsEntity().getPulseAssetEntities()).hasSize(0);
    }

    @Test
    public void whenConfigWithoutLocalAssetsAndWithMappings_thenMappingsShouldBeCleared() {
        final PulseEntity pulseEntity = new PulseEntity();
        when(pulseExtractor.getPulseEntity()).thenReturn(pulseEntity);
        PulseAgentAssetUtils.resolveDiff(pulseExtractor, remoteAssets);
        verify(pulseExtractor).setPulseEntity(pulseEntityArgumentCaptor.capture());
        final PulseEntity newPulseEntity = pulseEntityArgumentCaptor.getValue();
        assertThat(newPulseEntity.getPulseAssetsEntity().getPulseAssetEntities()).isEqualTo(localAssets);
    }

    @Test
    public void whenConfigWithLocalAssetsWithoutLocalAssetsAndWithMappings_thenMappingsShouldBeCleared() {
        final PulseEntity pulseEntity = new PulseEntity();
        pulseEntity.getPulseAssetsEntity()
                .getPulseAssetEntities()
                .forEach(e -> e.getMapping().setId(UUID.randomUUID()));
        when(pulseExtractor.getPulseEntity()).thenReturn(pulseEntity);
        PulseAgentAssetUtils.resolveDiff(pulseExtractor, remoteAssets);
        verify(pulseExtractor).setPulseEntity(pulseEntityArgumentCaptor.capture());
        final PulseEntity newPulseEntity = pulseEntityArgumentCaptor.getValue();
        assertThat(newPulseEntity.getPulseAssetsEntity().getPulseAssetEntities()).isEqualTo(localAssets);
    }

    @Test
    public void whenLocalMappingStatusIsStreamingAndRemoteAssetUpdated_thenStatusShouldBeRequiresRemapping() {
        final PulseEntity pulseEntity = new PulseEntity();
        final UUID mappingId = UUID.randomUUID();
        pulseEntity.getPulseAssetsEntity()
                .getPulseAssetEntities()
                .add(localAssets.get(0)
                        .withMapping(PulseAssetMappingEntity.builder()
                                .id(mappingId)
                                .status(PulseAssetMappingStatus.STREAMING)
                                .build()));
        when(pulseExtractor.getPulseEntity()).thenReturn(pulseEntity);
        PulseAgentAssetUtils.resolveDiff(pulseExtractor,
                List.of(remoteAssets.get(0)
                        .withName("Updated Name")
                        .withDescription("Updated Description")
                        .withTopic("updated/topic")
                        .withJsonSchema("{ \"name\": \"Updated Name\" }")));
        verify(pulseExtractor).setPulseEntity(pulseEntityArgumentCaptor.capture());
        final PulseEntity newPulseEntity = pulseEntityArgumentCaptor.getValue();
        assertThat(newPulseEntity.getPulseAssetsEntity().getPulseAssetEntities()).hasSize(1);
        final PulseAssetEntity asset = newPulseEntity.getPulseAssetsEntity().getPulseAssetEntities().get(0);
        assertThat(asset.getId()).isEqualTo(localAssets.get(0).getId());
        assertThat(asset.getName()).isEqualTo("Updated Name");
        assertThat(asset.getDescription()).isEqualTo("Updated Description");
        assertThat(asset.getTopic()).isEqualTo("updated/topic");
        assertThat(asset.getSchema()).isEqualTo("{ \"name\": \"Updated Name\" }");
        assertThat(asset.getMapping().getId()).isEqualTo(mappingId);
        assertThat(asset.getMapping().getStatus()).isEqualTo(PulseAssetMappingStatus.REQUIRES_REMAPPING);
    }

    @Test
    public void whenLocalMappingExistsAndRemoteAssetIsRemoved_thenStatusShouldBeUpdatedCorrectly() {
        IntStream.range(0, STATUSES.size()).forEach(i -> {
            final PulseAssetMappingStatus status = STATUSES.get(i);
            final PulseEntity pulseEntity = new PulseEntity();
            final UUID mappingId = UUID.randomUUID();
            final PulseAssetEntity expectedAsset = localAssets.get(0)
                    .withMapping(PulseAssetMappingEntity.builder()
                            .id(mappingId)
                            .status(status)
                            .build());
            pulseEntity.getPulseAssetsEntity().getPulseAssetEntities().add(expectedAsset);
            when(pulseExtractor.getPulseEntity()).thenReturn(pulseEntity);
            PulseAgentAssetUtils.resolveDiff(pulseExtractor, List.of());
            verify(pulseExtractor, times(i + 1)).setPulseEntity(pulseEntityArgumentCaptor.capture());
            final PulseEntity newPulseEntity = pulseEntityArgumentCaptor.getValue();
            assertThat(newPulseEntity.getPulseAssetsEntity().getPulseAssetEntities()).hasSize(1);
            final PulseAssetEntity asset = newPulseEntity.getPulseAssetsEntity().getPulseAssetEntities().get(0);
            assertThat(asset.getId()).isEqualTo(expectedAsset.getId());
            assertThat(asset.getName()).isEqualTo(expectedAsset.getName());
            assertThat(asset.getDescription()).isEqualTo(expectedAsset.getDescription());
            assertThat(asset.getTopic()).isEqualTo(expectedAsset.getTopic());
            assertThat(asset.getSchema()).isEqualTo(expectedAsset.getSchema());
            assertThat(asset.getMapping().getId()).isEqualTo(mappingId);
            switch (status) {
                case DRAFT, STREAMING, REQUIRES_REMAPPING, MISSING ->
                        assertThat(asset.getMapping().getStatus()).isEqualTo(PulseAssetMappingStatus.MISSING);
                case UNMAPPED -> assertThat(asset.getMapping().getStatus()).isEqualTo(PulseAssetMappingStatus.UNMAPPED);
                default -> fail("Unhandled status: " + status);
            }
        });
    }

    @Test
    public void whenLocalMappingExistsAndRemoteAssetIsNotUpdated_thenStatusShouldBeUpdatedCorrectly() {
        IntStream.range(0, STATUSES.size()).forEach(i -> {
            final PulseAssetMappingStatus status = STATUSES.get(i);
            final PulseEntity pulseEntity = new PulseEntity();
            final UUID mappingId = UUID.randomUUID();
            final PulseAssetEntity expectedAsset = localAssets.get(0)
                    .withMapping(PulseAssetMappingEntity.builder()
                            .id(mappingId)
                            .status(status)
                            .build());
            pulseEntity.getPulseAssetsEntity().getPulseAssetEntities().add(expectedAsset);
            when(pulseExtractor.getPulseEntity()).thenReturn(pulseEntity);
            PulseAgentAssetUtils.resolveDiff(pulseExtractor, List.of(remoteAssets.get(0)));
            verify(pulseExtractor, times(i + 1)).setPulseEntity(pulseEntityArgumentCaptor.capture());
            final PulseEntity newPulseEntity = pulseEntityArgumentCaptor.getValue();
            assertThat(newPulseEntity.getPulseAssetsEntity().getPulseAssetEntities()).hasSize(1);
            final PulseAssetEntity asset = newPulseEntity.getPulseAssetsEntity().getPulseAssetEntities().get(0);
            assertThat(asset.getId()).isEqualTo(expectedAsset.getId());
            assertThat(asset.getName()).isEqualTo(expectedAsset.getName());
            assertThat(asset.getDescription()).isEqualTo(expectedAsset.getDescription());
            assertThat(asset.getTopic()).isEqualTo(expectedAsset.getTopic());
            assertThat(asset.getSchema()).isEqualTo(expectedAsset.getSchema());
            assertThat(asset.getMapping().getId()).isEqualTo(mappingId);
            switch (status) {
                case DRAFT, STREAMING, REQUIRES_REMAPPING, UNMAPPED ->
                        assertThat(asset.getMapping().getStatus()).isEqualTo(status);
                case MISSING -> assertThat(asset.getMapping().getStatus()).isEqualTo(PulseAssetMappingStatus.UNMAPPED);
                default -> fail("Unhandled status: " + status);
            }
        });
    }

    @Test
    public void whenLocalMappingExistsAndRemoteAssetIsUpdated_thenStatusShouldBeUpdatedCorrectly() {
        IntStream.range(0, STATUSES.size()).forEach(i -> {
            final PulseAssetMappingStatus status = STATUSES.get(i);
            final PulseEntity pulseEntity = new PulseEntity();
            final PulseAssetEntity expectedAsset = localAssets.get(0)
                    .withMapping(PulseAssetMappingEntity.builder().id(UUID.randomUUID()).status(status).build());
            pulseEntity.getPulseAssetsEntity().getPulseAssetEntities().add(expectedAsset);
            when(pulseExtractor.getPulseEntity()).thenReturn(pulseEntity);
            PulseAgentAssetUtils.resolveDiff(pulseExtractor,
                    List.of(remoteAssets.get(0)
                            .withName("Updated Name")
                            .withTopic("updated/topic")
                            .withJsonSchema("{ \"name\": \"Updated Name\" }")));
            verify(pulseExtractor, times(i + 1)).setPulseEntity(pulseEntityArgumentCaptor.capture());
            final PulseEntity newPulseEntity = pulseEntityArgumentCaptor.getValue();
            assertThat(newPulseEntity.getPulseAssetsEntity().getPulseAssetEntities()).hasSize(1);
            final PulseAssetEntity asset = newPulseEntity.getPulseAssetsEntity().getPulseAssetEntities().get(0);
            assertThat(asset.getId()).isEqualTo(expectedAsset.getId());
            assertThat(asset.getName()).isEqualTo("Updated Name");
            assertThat(asset.getTopic()).isEqualTo("updated/topic");
            assertThat(asset.getSchema()).isEqualTo("{ \"name\": \"Updated Name\" }");
            assertThat(asset.getMapping().getId()).isEqualTo(expectedAsset.getMapping().getId());
            switch (status) {
                case DRAFT, REQUIRES_REMAPPING, UNMAPPED ->
                        assertThat(asset.getMapping().getStatus()).isEqualTo(status);
                case STREAMING -> assertThat(asset.getMapping()
                        .getStatus()).isEqualTo(PulseAssetMappingStatus.REQUIRES_REMAPPING);
                case MISSING -> assertThat(asset.getMapping().getStatus()).isEqualTo(PulseAssetMappingStatus.UNMAPPED);
                default -> fail("Unhandled status: " + status);
            }
        });
    }
}
