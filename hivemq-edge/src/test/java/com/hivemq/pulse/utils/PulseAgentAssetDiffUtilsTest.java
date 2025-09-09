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
import com.hivemq.pulse.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class PulseAgentAssetDiffUtilsTest {
    private static final List<PulseAssetMappingStatus> STATUSES = List.of(PulseAssetMappingStatus.values());
    private @NotNull List<PulseAssetEntity> localAssets;
    private @NotNull List<Asset> remoteAssets;

    @BeforeEach
    public void setUp() {
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
                "{ \"name\": \"Name " + index + "\" }");
    }

    @Test
    public void whenConfigWithoutRemoteAssetsAndWithoutMappings_thenLocalAssetsShouldBeCleared() {
        // There are no local assets.
        final PulseEntity pulseEntityWithoutAssets = new PulseEntity();
        final PulseEntity newPulseEntityWithoutAssets =
                PulseAgentAssetDiffUtils.resolve(pulseEntityWithoutAssets, List.of());
        assertThat(newPulseEntityWithoutAssets.getPulseAssetsEntity().getPulseAssetEntities()).hasSize(0);
        // There are local assets.
        final PulseEntity pulseEntityWithAssets = new PulseEntity();
        pulseEntityWithAssets.getPulseAssetsEntity().setPulseAssetEntities(localAssets);
        final PulseEntity newPulseEntityWithAssets = PulseAgentAssetDiffUtils.resolve(pulseEntityWithAssets, List.of());
        assertThat(newPulseEntityWithAssets.getPulseAssetsEntity().getPulseAssetEntities()).hasSize(0);
    }

    @Test
    public void whenConfigWithoutLocalAssetsAndWithMappings_thenMappingsShouldBeCleared() {
        // There are no local asset mappings.
        final PulseEntity pulseEntityWithoutAssetMappings = new PulseEntity();
        final PulseEntity newPulseEntityWithoutAssetMappings =
                PulseAgentAssetDiffUtils.resolve(pulseEntityWithoutAssetMappings, remoteAssets);
        assertThat(newPulseEntityWithoutAssetMappings.getPulseAssetsEntity().getPulseAssetEntities()).isEqualTo(
                localAssets.stream().map(a -> a.withDescription(null)).toList());
        // There are local asset mappings.
        final PulseEntity pulseConfigWithAssetMappings = new PulseEntity();
        pulseConfigWithAssetMappings.getPulseAssetsEntity()
                .getPulseAssetEntities()
                .forEach(e -> e.getMapping().setId(UUID.randomUUID()));
        final PulseEntity newPulseConfigWithAssetMappings =
                PulseAgentAssetDiffUtils.resolve(pulseConfigWithAssetMappings, remoteAssets);
        assertThat(newPulseConfigWithAssetMappings.getPulseAssetsEntity()
                .getPulseAssetEntities()).isEqualTo(localAssets.stream().map(a -> a.withDescription(null)).toList());
    }

    @Test
    public void whenLocalMappingStatusIsStreamingAndRemoteAssetUpdated_thenStatusShouldBeRequiresRemapping() {
        final PulseEntity pulseEntity = new PulseEntity();
        pulseEntity.getPulseAssetsEntity()
                .getPulseAssetEntities()
                .add(localAssets.get(0)
                        .withMapping(PulseAssetMappingEntity.builder()
                                .id(localAssets.get(0).getId())
                                .status(PulseAssetMappingStatus.STREAMING)
                                .build()));
        final PulseEntity newPulseEntity = PulseAgentAssetDiffUtils.resolve(pulseEntity,
                List.of(remoteAssets.get(0)
                        .withName("Updated Name")
                        .withTopic("updated/topic")
                        .withJsonSchema("{ \"name\": \"Updated Name\" }")));
        assertThat(newPulseEntity.getPulseAssetsEntity().getPulseAssetEntities()).hasSize(1);
        final PulseAssetEntity asset = newPulseEntity.getPulseAssetsEntity().getPulseAssetEntities().get(0);
        assertThat(asset.getId()).isEqualTo(localAssets.get(0).getId());
        assertThat(asset.getName()).isEqualTo("Updated Name");
        assertThat(asset.getTopic()).isEqualTo("updated/topic");
        assertThat(asset.getSchema()).isEqualTo("{ \"name\": \"Updated Name\" }");
        assertThat(asset.getMapping().getId()).isEqualTo(localAssets.get(0).getId());
        assertThat(asset.getMapping().getStatus()).isEqualTo(PulseAssetMappingStatus.REQUIRES_REMAPPING);
    }

    @Test
    public void whenLocalMappingExistsAndRemoteAssetIsRemoved_thenStatusShouldBeUpdatedCorrectly() {
        STATUSES.forEach(status -> {
            final PulseEntity pulseEntity = new PulseEntity();
            final PulseAssetEntity expectedAsset = localAssets.get(0)
                    .withMapping(PulseAssetMappingEntity.builder()
                            .id(localAssets.get(0).getId())
                            .status(status)
                            .build());
            pulseEntity.getPulseAssetsEntity().getPulseAssetEntities().add(expectedAsset);
            final PulseEntity newPulseEntity = PulseAgentAssetDiffUtils.resolve(pulseEntity, List.of());
            assertThat(newPulseEntity.getPulseAssetsEntity().getPulseAssetEntities()).hasSize(1);
            final PulseAssetEntity asset = newPulseEntity.getPulseAssetsEntity().getPulseAssetEntities().get(0);
            assertThat(asset.getId()).isEqualTo(expectedAsset.getId());
            assertThat(asset.getName()).isEqualTo(expectedAsset.getName());
            assertThat(asset.getDescription()).isEqualTo(expectedAsset.getDescription());
            assertThat(asset.getTopic()).isEqualTo(expectedAsset.getTopic());
            assertThat(asset.getSchema()).isEqualTo(expectedAsset.getSchema());
            assertThat(asset.getMapping().getId()).isEqualTo(expectedAsset.getId());
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
        STATUSES.forEach(status -> {
            final PulseEntity pulseEntity = new PulseEntity();
            final PulseAssetEntity expectedAsset = localAssets.get(0)
                    .withMapping(PulseAssetMappingEntity.builder()
                            .id(localAssets.get(0).getId())
                            .status(status)
                            .build());
            pulseEntity.getPulseAssetsEntity().getPulseAssetEntities().add(expectedAsset);
            final PulseEntity newPulseEntity =
                    PulseAgentAssetDiffUtils.resolve(pulseEntity, List.of(remoteAssets.get(0)));
            assertThat(newPulseEntity.getPulseAssetsEntity().getPulseAssetEntities()).hasSize(1);
            final PulseAssetEntity asset = newPulseEntity.getPulseAssetsEntity().getPulseAssetEntities().get(0);
            assertThat(asset.getId()).isEqualTo(expectedAsset.getId());
            assertThat(asset.getName()).isEqualTo(expectedAsset.getName());
            assertThat(asset.getDescription()).isEqualTo(expectedAsset.getDescription());
            assertThat(asset.getTopic()).isEqualTo(expectedAsset.getTopic());
            assertThat(asset.getSchema()).isEqualTo(expectedAsset.getSchema());
            assertThat(asset.getMapping().getId()).isEqualTo(expectedAsset.getId());
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
        STATUSES.forEach(status -> {
            final PulseEntity pulseEntity = new PulseEntity();
            final PulseAssetEntity expectedAsset = localAssets.get(0)
                    .withMapping(PulseAssetMappingEntity.builder()
                            .id(localAssets.get(0).getId())
                            .status(status)
                            .build());
            pulseEntity.getPulseAssetsEntity().getPulseAssetEntities().add(expectedAsset);
            final PulseEntity newPulseEntity = PulseAgentAssetDiffUtils.resolve(pulseEntity,
                    List.of(remoteAssets.get(0)
                            .withName("Updated Name")
                            .withTopic("updated/topic")
                            .withJsonSchema("{ \"name\": \"Updated Name\" }")));
            assertThat(newPulseEntity.getPulseAssetsEntity().getPulseAssetEntities()).hasSize(1);
            final PulseAssetEntity asset = newPulseEntity.getPulseAssetsEntity().getPulseAssetEntities().get(0);
            assertThat(asset.getId()).isEqualTo(expectedAsset.getId());
            assertThat(asset.getName()).isEqualTo("Updated Name");
            assertThat(asset.getTopic()).isEqualTo("updated/topic");
            assertThat(asset.getSchema()).isEqualTo("{ \"name\": \"Updated Name\" }");
            assertThat(asset.getMapping().getId()).isEqualTo(expectedAsset.getId());
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
