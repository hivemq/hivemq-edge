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

package com.hivemq.pulse.converters;

import com.hivemq.edge.api.model.ManagedAsset;
import com.hivemq.pulse.asset.PulseAgentAsset;
import org.jetbrains.annotations.NotNull;

public class PulseAgentAssetConverter implements PulseApiEntityConverter<ManagedAsset, PulseAgentAsset> {
    public static final PulseAgentAssetConverter INSTANCE = new PulseAgentAssetConverter();

    private PulseAgentAssetConverter() {
    }

    @Override
    public @NotNull PulseAgentAsset toInternalEntity(final @NotNull ManagedAsset asset) {
        return PulseAgentAsset.builder()
                .id(asset.getId())
                .name(asset.getName())
                .description(asset.getDescription())
                .topic(asset.getTopic())
                .schema(PulseAgentAssetSchemaConverter.INSTANCE.toInternalEntity(asset.getSchema()))
                .mapping(PulseAgentAssetMappingConverter.INSTANCE.toInternalEntity(asset.getMapping()))
                .build();
    }

    @Override
    public @NotNull ManagedAsset toRestEntity(final @NotNull PulseAgentAsset asset) {
        return ManagedAsset.builder()
                .id(asset.getId())
                .name(asset.getName())
                .description(asset.getDescription())
                .topic(asset.getTopic())
                .schema(PulseAgentAssetSchemaConverter.INSTANCE.toRestEntity(asset.getSchema()))
                .mapping(PulseAgentAssetMappingConverter.INSTANCE.toRestEntity(asset.getMapping()))
                .build();
    }
}
