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

import com.hivemq.configuration.entity.pulse.PulseAssetMappingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class PulseAgentAssetMapping {

    private @Nullable UUID id;

    private @NotNull PulseAgentAssetMappingStatus status;

    public PulseAgentAssetMapping() {
        this(null, PulseAgentAssetMappingStatus.UNMAPPED);
    }

    public PulseAgentAssetMapping(final @Nullable UUID id, final @NotNull PulseAgentAssetMappingStatus status) {
        this.id = id;
        this.status = status;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static @NotNull PulseAgentAssetMapping fromPersistence(@NotNull final PulseAssetMappingEntity mapping) {
        return new PulseAgentAssetMapping(mapping.getId(),
                PulseAgentAssetMappingStatus.fromPersistence(mapping.getStatus()));
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (!(o instanceof final PulseAgentAssetMapping that)) {
            return false;
        }
        return Objects.equals(id, that.id) && Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, status);
    }

    public @NotNull PulseAgentAssetMappingStatus getStatus() {
        return status;
    }

    public void setStatus(@NotNull final PulseAgentAssetMappingStatus status) {
        this.status = status;
    }

    public @Nullable UUID getId() {
        return id;
    }

    public void setId(@Nullable final UUID id) {
        this.id = id;
    }

    public @NotNull PulseAgentAssetMapping withId(final @Nullable UUID id) {
        if (Objects.equals(this.id, id)) {
            return this;
        }
        return new PulseAgentAssetMapping(id, status);
    }

    public @NotNull PulseAgentAssetMapping withStatus(final @NotNull PulseAgentAssetMappingStatus status) {
        if (Objects.equals(this.status, status)) {
            return this;
        }
        return new PulseAgentAssetMapping(id, status);
    }

    public @NotNull PulseAssetMappingEntity toPersistence() {
        return new PulseAssetMappingEntity(id, status.toPersistence());
    }

    public static class Builder {
        private @Nullable UUID id;
        private @NotNull PulseAgentAssetMappingStatus status;

        public Builder() {
            id = null;
            status = PulseAgentAssetMappingStatus.UNMAPPED;
        }

        public @NotNull Builder status(final @Nullable PulseAgentAssetMappingStatus status) {
            this.status = Objects.requireNonNullElse(status, PulseAgentAssetMappingStatus.UNMAPPED);
            return this;
        }

        public @NotNull Builder id(final @Nullable UUID id) {
            this.id = id;
            return this;
        }

        public @NotNull PulseAgentAssetMapping build() {
            return new PulseAgentAssetMapping(id, status);
        }
    }
}
