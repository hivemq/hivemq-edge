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

package com.hivemq.pulse.asset;

import com.hivemq.configuration.entity.pulse.PulseAssetsEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PulseAgentAssets extends ArrayList<PulseAgentAsset> {
    public static @NotNull PulseAgentAssets fromPersistence(final @NotNull PulseAssetsEntity assetsEntity) {
        final PulseAgentAssets assets = new PulseAgentAssets();
        assetsEntity.getPulseAssetEntities().stream().map(PulseAgentAsset::fromPersistence).forEach(assets::add);
        return assets;
    }

    public @NotNull Map<String, PulseAgentAsset> getAssetMap() {
        return stream().collect(Collectors.toMap(asset -> asset.getId().toString(),
                Function.identity(),
                (a1, a2) -> a1,
                LinkedHashMap::new));
    }

    public @NotNull PulseAssetsEntity toPersistence() {
        final PulseAssetsEntity assetsEntity = new PulseAssetsEntity();
        stream().map(PulseAgentAsset::toPersistence).forEach(assetsEntity.getPulseAssetEntities()::add);
        return assetsEntity;
    }

    public @NotNull Set<String> getMappingIdSet() {
        return stream().map(asset -> asset.getMapping().getId())
                .filter(Objects::nonNull)
                .map(UUID::toString)
                .collect(Collectors.toSet());
    }
}
