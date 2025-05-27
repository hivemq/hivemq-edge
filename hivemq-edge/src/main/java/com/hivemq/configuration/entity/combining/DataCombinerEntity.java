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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


public class DataCombinerEntity {
    @JsonProperty(value = "id", required = true)
    @XmlElement(name = "id", required = true)
    private @NotNull UUID id;

    @JsonProperty(value = "name", required = true)
    @XmlElement(name = "name", required = true)
    private @NotNull String name;

    @JsonProperty(value = "description", required = true)
    @XmlElement(name = "description", required = true)
    private @NotNull String description;

    @JsonProperty(value = "entityReferences", required = true)
    @XmlElementWrapper(name = "entity-references", required = true)
    @XmlElement(name = "entity-reference")
    private @NotNull List<EntityReferenceEntity> entityReferenceEntities = new ArrayList<>();;

    @JsonProperty(value = "dataCombinings", required = true)
    @XmlElementWrapper(name = "data-combinings", required = true)
    @XmlElement(name = "data-combining")
    private @NotNull List<DataCombiningEntity> dataCombiningEntities = new ArrayList<>();;

    // no-arg for jaxb
    public DataCombinerEntity() {
    }

    public DataCombinerEntity(
            final @NotNull UUID id,
            final @NotNull String name,
            final @NotNull String description,
            final @NotNull List<EntityReferenceEntity> entityReferenceEntities,
            final @NotNull List<DataCombiningEntity> dataCombiningEntities) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.entityReferenceEntities = entityReferenceEntities;
        this.dataCombiningEntities = dataCombiningEntities;
    }

    public void validate(final @NotNull List<ValidationEvent> validationErrors) {
        //TODO
    }

    @Override
    public @NotNull String toString() {
        return "DataCombinerEntity{" +
                "dataCombiningEntities=" +
                dataCombiningEntities +
                ", id=" +
                id +
                ", name='" +
                name +
                '\'' +
                ", description='" +
                description +
                '\'' +
                ", entityReferenceEntities=" +
                entityReferenceEntities +
                '}';
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final DataCombinerEntity that = (DataCombinerEntity) o;
        return id.equals(that.id) &&
                name.equals(that.name) &&
                description.equals(that.description) &&
                entityReferenceEntities.equals(that.entityReferenceEntities) &&
                dataCombiningEntities.equals(that.dataCombiningEntities);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + entityReferenceEntities.hashCode();
        result = 31 * result + dataCombiningEntities.hashCode();
        return result;
    }

    public @NotNull List<DataCombiningEntity> getDataCombiningEntities() {
        return dataCombiningEntities;
    }

    public @NotNull String getDescription() {
        return description;
    }

    public @NotNull List<EntityReferenceEntity> getEntityReferenceEntities() {
        return entityReferenceEntities;
    }

    public @NotNull UUID getId() {
        return id;
    }

    public @NotNull String getName() {
        return name;
    }
}
