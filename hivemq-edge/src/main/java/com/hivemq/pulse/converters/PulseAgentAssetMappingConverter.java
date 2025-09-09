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

import com.hivemq.edge.api.model.AssetMapping;
import com.hivemq.pulse.asset.PulseAgentAssetMapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PulseAgentAssetMappingConverter implements PulseApiEntityConverter<AssetMapping, PulseAgentAssetMapping> {
    public static final PulseAgentAssetMappingConverter INSTANCE = new PulseAgentAssetMappingConverter();

    private PulseAgentAssetMappingConverter() {
    }

    @Override
    public @NotNull PulseAgentAssetMapping toInternalEntity(final @NotNull AssetMapping assetMapping) {
        return PulseAgentAssetMapping.builder()
                .id(assetMapping.getMappingId())
                .status(PulseAgentAssetMappingStatusConverter.INSTANCE.toInternalEntity(assetMapping.getStatus()))
                .build();
    }

    @Override
    public @NotNull AssetMapping toRestEntity(final @Nullable PulseAgentAssetMapping assetMapping) {
        return AssetMapping.builder()
                .mappingId(assetMapping == null ? null : assetMapping.getId())
                .status(assetMapping == null ?
                        AssetMapping.StatusEnum.UNMAPPED :
                        PulseAgentAssetMappingStatusConverter.INSTANCE.toRestEntity(assetMapping.getStatus()))
                .build();
    }
}
