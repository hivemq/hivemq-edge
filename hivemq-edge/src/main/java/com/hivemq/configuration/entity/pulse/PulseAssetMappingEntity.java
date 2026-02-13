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
package com.hivemq.configuration.entity.pulse;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.configuration.entity.EntityValidatable;
import com.hivemq.configuration.entity.UUIDAdapter;
import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "mapping")
public class PulseAssetMappingEntity implements EntityValidatable {

    @JsonProperty(value = "id")
    @XmlAttribute(name = "id")
    @XmlJavaTypeAdapter(UUIDAdapter.class)
    private @Nullable UUID id;

    @JsonProperty(value = "status", required = true)
    @XmlAttribute(name = "status", required = true)
    private @NotNull PulseAssetMappingStatus status;

    public PulseAssetMappingEntity() {
        this(null, PulseAssetMappingStatus.UNMAPPED);
    }

    @JsonCreator
    public PulseAssetMappingEntity(
            final @JsonProperty("id") @Nullable UUID id,
            final @JsonProperty("status") @NotNull PulseAssetMappingStatus status) {
        this.id = id;
        this.status = status;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (!(o instanceof final PulseAssetMappingEntity that)) {
            return false;
        }
        return Objects.equals(id, that.id) && Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, status);
    }

    public @NotNull PulseAssetMappingStatus getStatus() {
        return status;
    }

    public void setStatus(@NotNull final PulseAssetMappingStatus status) {
        this.status = status;
    }

    public @Nullable UUID getId() {
        return id;
    }

    public void setId(@Nullable final UUID id) {
        this.id = id;
    }

    public @NotNull PulseAssetMappingEntity withId(final @NotNull UUID id) {
        if (Objects.equals(this.id, id)) {
            return this;
        }
        return new PulseAssetMappingEntity(id, status);
    }

    public @NotNull PulseAssetMappingEntity withStatus(final @NotNull PulseAssetMappingStatus status) {
        if (Objects.equals(this.status, status)) {
            return this;
        }
        return new PulseAssetMappingEntity(id, status);
    }

    @Override
    public void validate(final @NotNull List<ValidationEvent> validationEvents) {
        EntityValidatable.notNull(validationEvents, status, "status");
    }

    @Override
    public @NotNull String toString() {
        return "PulseAssetMappingEntity{" + "id=" + id + ", status=" + status + '}';
    }

    public static class Builder {
        private @Nullable UUID id;
        private @NotNull PulseAssetMappingStatus status;

        public Builder() {
            id = null;
            status = PulseAssetMappingStatus.UNMAPPED;
        }

        public @NotNull Builder status(final @Nullable PulseAssetMappingStatus status) {
            this.status = Objects.requireNonNullElse(status, PulseAssetMappingStatus.UNMAPPED);
            return this;
        }

        public @NotNull Builder id(final @Nullable UUID id) {
            this.id = id;
            return this;
        }

        public @NotNull PulseAssetMappingEntity build() {
            return new PulseAssetMappingEntity(id, status);
        }
    }
}
