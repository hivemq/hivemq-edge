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
import com.hivemq.configuration.entity.pulse.PulseAssetsEntity;
import com.hivemq.configuration.entity.pulse.PulseEntity;
import com.hivemq.configuration.reader.PulseExtractor;
import com.hivemq.pulse.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PulseAgentAssetDiffUtils {
    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(PulseAgentAssetDiffUtils.class);

    private PulseAgentAssetDiffUtils() {
    }

    public static void resolve(final @NotNull PulseExtractor pulseExtractor, final @NotNull List<Asset> remoteAssets) {
        final PulseEntity oldPulseEntity = pulseExtractor.getPulseEntity();
        // TODO Update data combiners.
        synchronized (oldPulseEntity.getLock()) {
            final List<PulseAssetEntity> localAssets = oldPulseEntity.getPulseAssetsEntity().getPulseAssetEntities();
            final List<PulseAssetEntity> newLocalAssets = new ArrayList<>();
            final Map<String, Asset> remoteAssetMap = remoteAssets.stream()
                    .collect(Collectors.toMap(Asset::id,
                            Function.identity(),
                            (asset1, asset2) -> asset1,
                            LinkedHashMap::new));
            // Process local assets.
            localAssets.forEach(localAsset -> {
                final String id = localAsset.getId().toString();
                final Asset remoteAsset = remoteAssetMap.remove(id);
                if (remoteAsset == null) {
                    // Asset is removed remotely.
                    if (localAsset.getMapping().getId() != null) {
                        newLocalAssets.add(localAsset.withMapping(switch (localAsset.getMapping().getStatus()) {
                            case DRAFT, REQUIRES_REMAPPING, STREAMING ->
                                    localAsset.getMapping().withStatus(PulseAssetMappingStatus.MISSING);
                            default -> localAsset.getMapping();
                        }));
                    }
                } else {
                    // Asset is found remotely. We check if it has changed.
                    if (localAsset.equals(remoteAsset)) {
                        newLocalAssets.add(localAsset.withMapping(PulseAssetMappingEntity.builder()
                                .id(localAsset.getId())
                                .status(switch (localAsset.getMapping().getStatus()) {
                                    case MISSING -> PulseAssetMappingStatus.UNMAPPED;
                                    default -> localAsset.getMapping().getStatus();
                                })
                                .build()));
                    } else {
                        // Asset has changed. We update it and mark its mapping as REQUIRES_REMAPPING if it was STREAMING.
                        newLocalAssets.add(PulseAssetEntity.builder()
                                .id(localAsset.getId())
                                .name(remoteAsset.name())
                                .description(localAsset.getDescription())
                                .schema(remoteAsset.jsonSchema())
                                .topic(remoteAsset.topic())
                                .mapping(PulseAssetMappingEntity.builder()
                                        .id(localAsset.getId())
                                        .status(switch (localAsset.getMapping().getStatus()) {
                                            case STREAMING -> PulseAssetMappingStatus.REQUIRES_REMAPPING;
                                            case MISSING -> PulseAssetMappingStatus.UNMAPPED;
                                            default -> localAsset.getMapping().getStatus();
                                        })
                                        .build())
                                .build());
                    }
                }
            });
            // Process the remaining remote assets because they are new.
            remoteAssetMap.values().forEach(remoteAsset -> {
                final String id = remoteAsset.id();
                newLocalAssets.add(PulseAssetEntity.builder()
                        .id(UUID.fromString(id))
                        .name(remoteAsset.name())
                        .description(null)
                        .schema(remoteAsset.jsonSchema())
                        .topic(remoteAsset.topic())
                        .mapping(PulseAssetMappingEntity.builder()
                                .id(null)
                                .status(PulseAssetMappingStatus.UNMAPPED)
                                .build())
                        .build());
            });
            final PulseEntity newPulseEntity = new PulseEntity(new PulseAssetsEntity(newLocalAssets));
            pulseExtractor.setPulseEntity(newPulseEntity);
        }
    }
}
