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
package com.hivemq.pulse.asset;

import com.hivemq.configuration.entity.pulse.PulseAssetMappingStatus;
import org.jetbrains.annotations.NotNull;

public enum PulseAgentAssetMappingStatus {
    DRAFT,
    MISSING,
    REQUIRES_REMAPPING,
    STREAMING,
    UNMAPPED,
    ;

    public static @NotNull PulseAgentAssetMappingStatus fromPersistence(@NotNull final PulseAssetMappingStatus status) {
        return switch (status) {
            case DRAFT -> DRAFT;
            case MISSING -> MISSING;
            case REQUIRES_REMAPPING -> REQUIRES_REMAPPING;
            case STREAMING -> STREAMING;
            case UNMAPPED -> UNMAPPED;
        };
    }

    public @NotNull PulseAssetMappingStatus toPersistence() {
        return switch (this) {
            case DRAFT -> PulseAssetMappingStatus.DRAFT;
            case MISSING -> PulseAssetMappingStatus.MISSING;
            case REQUIRES_REMAPPING -> PulseAssetMappingStatus.REQUIRES_REMAPPING;
            case STREAMING -> PulseAssetMappingStatus.STREAMING;
            case UNMAPPED -> PulseAssetMappingStatus.UNMAPPED;
        };
    }
}
