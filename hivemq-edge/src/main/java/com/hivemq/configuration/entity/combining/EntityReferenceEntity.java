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
package com.hivemq.configuration.entity.combining;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.combining.model.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jakarta.xml.bind.annotation.XmlElement;
import java.util.Objects;

public class EntityReferenceEntity {

    @JsonProperty(value = "type", required = true)
    @XmlElement(name = "type", required = true)
    private @NotNull EntityType type;

    @JsonProperty(value = "id", required = true)
    @XmlElement(name = "id", required = true)
    private @NotNull String id;

    // no-arg for jaxb
    public EntityReferenceEntity() {
    }

    public EntityReferenceEntity(@NotNull final EntityType type, @NotNull final String id) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(id, "id must not be null");
        this.type = type;
        this.id = id;
    }

    public @NotNull String getId() {
        return id;
    }

    public @NotNull EntityType getType() {
        return type;
    }

    @Override
    public @NotNull String toString() {
        return "EntityReferenceEntity{" + "id='" + id + '\'' + ", type=" + type + '}';
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final EntityReferenceEntity that = (EntityReferenceEntity) o;
        return type == that.type && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + id.hashCode();
        return result;
    }



}
