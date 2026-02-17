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
package com.hivemq.pulse.converters;

import com.hivemq.configuration.entity.EntityConverter;
import com.hivemq.edge.api.model.ManagedAssetList;
import com.hivemq.pulse.asset.PulseAgentAssets;
import org.jetbrains.annotations.NotNull;

public class PulseAgentAssetsConverter implements EntityConverter<ManagedAssetList, PulseAgentAssets> {
    public static final PulseAgentAssetsConverter INSTANCE = new PulseAgentAssetsConverter();

    private PulseAgentAssetsConverter() {}

    @Override
    public @NotNull PulseAgentAssets toInternalEntity(final @NotNull ManagedAssetList assets) {
        final PulseAgentAssets pulseAgentAssets = new PulseAgentAssets();
        assets.getItems().stream()
                .map(PulseAgentAssetConverter.INSTANCE::toInternalEntity)
                .forEach(pulseAgentAssets::add);
        return pulseAgentAssets;
    }

    @Override
    public @NotNull ManagedAssetList toRestEntity(final @NotNull PulseAgentAssets assets) {
        return ManagedAssetList.builder()
                .items(assets.stream()
                        .map(PulseAgentAssetConverter.INSTANCE::toRestEntity)
                        .toList())
                .build();
    }
}
