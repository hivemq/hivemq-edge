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
import com.hivemq.combining.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.XmlElement;

public class EntityReferenceEntity {

    @JsonProperty("type")
    @XmlElement(name = "type")
    private final @NotNull EntityType type;

    @JsonProperty("id")
    @XmlElement(name = "id")
    private final @NotNull String id;

    // no-arg for jaxb
    public EntityReferenceEntity() {
        id = "";
        type = EntityType.EDGE_BROKER;
    }

    public EntityReferenceEntity(@NotNull final EntityType type, @NotNull final String id) {
        this.type = type;
        this.id = id;
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

    public @NotNull String getId() {
        return id;
    }

    public @NotNull EntityType getType() {
        return type;
    }



}
