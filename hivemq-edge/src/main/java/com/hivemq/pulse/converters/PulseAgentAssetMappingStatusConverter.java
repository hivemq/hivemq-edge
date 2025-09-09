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
import com.hivemq.pulse.asset.PulseAgentAssetMappingStatus;
import org.jetbrains.annotations.NotNull;

public class PulseAgentAssetMappingStatusConverter
        implements PulseApiEntityConverter<AssetMapping.StatusEnum, PulseAgentAssetMappingStatus> {
    public static final PulseAgentAssetMappingStatusConverter INSTANCE = new PulseAgentAssetMappingStatusConverter();

    private PulseAgentAssetMappingStatusConverter() {
    }

    @Override
    public @NotNull PulseAgentAssetMappingStatus toInternalEntity(final @NotNull AssetMapping.StatusEnum status) {
        return switch (status) {
            case DRAFT -> PulseAgentAssetMappingStatus.DRAFT;
            case MISSING -> PulseAgentAssetMappingStatus.MISSING;
            case REQUIRES_REMAPPING -> PulseAgentAssetMappingStatus.REQUIRES_REMAPPING;
            case STREAMING -> PulseAgentAssetMappingStatus.STREAMING;
            case UNMAPPED -> PulseAgentAssetMappingStatus.UNMAPPED;
            default -> throw new IllegalArgumentException("Unknown pulse asset mapping status " + status);
        };
    }

    @Override
    public @NotNull AssetMapping.StatusEnum toRestEntity(final @NotNull PulseAgentAssetMappingStatus status) {
        return switch (status) {
            case DRAFT -> AssetMapping.StatusEnum.DRAFT;
            case MISSING -> AssetMapping.StatusEnum.MISSING;
            case REQUIRES_REMAPPING -> AssetMapping.StatusEnum.REQUIRES_REMAPPING;
            case STREAMING -> AssetMapping.StatusEnum.STREAMING;
            case UNMAPPED -> AssetMapping.StatusEnum.UNMAPPED;
            default -> throw new IllegalArgumentException("Unknown pulse asset mapping status " + status);
        };
    }
}
