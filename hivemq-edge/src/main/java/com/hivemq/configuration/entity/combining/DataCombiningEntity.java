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
import com.hivemq.configuration.entity.adapter.fieldmapping.InstructionEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.List;
import java.util.UUID;


// JAXB can not handle records ... :-(
public class DataCombiningEntity {

    @JsonProperty("id")
    @XmlElement(name = "id")
    private final @NotNull UUID id;

    @JsonProperty("sources")
    @XmlElement(name = "sources")
    private final @NotNull DataCombiningSourcesEntity sources;

    @JsonProperty("destination")
    @XmlElement(name = "destination")
    private final @NotNull String destination;

    @JsonProperty("instructions")
    @XmlElementWrapper(name = "instructions")
    private final @NotNull List<InstructionEntity> instructions;

    // no-arg for jaxb
    public DataCombiningEntity() {
        this.destination = "";
        this.id = UUID.randomUUID();
        this.sources = new DataCombiningSourcesEntity();
        this.instructions = List.of();
    }

    public DataCombiningEntity(
            final @NotNull UUID id,
            final @NotNull DataCombiningSourcesEntity sources,
            final @NotNull String destination,
            final @NotNull List<InstructionEntity> instructions) {
        this.id = id;
        this.sources = sources;
        this.destination = destination;
        this.instructions = instructions;
    }

    @Override
    public @NotNull String toString() {
        return "DataCombiningEntity{" +
                "destination='" +
                destination +
                '\'' +
                ", id=" +
                id +
                ", sources=" +
                sources +
                ", instructions=" +
                instructions +
                '}';
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final DataCombiningEntity that = (DataCombiningEntity) o;
        return id.equals(that.id) &&
                sources.equals(that.sources) &&
                destination.equals(that.destination) &&
                instructions.equals(that.instructions);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + sources.hashCode();
        result = 31 * result + destination.hashCode();
        result = 31 * result + instructions.hashCode();
        return result;
    }

    public @NotNull String getDestination() {
        return destination;
    }

    public @NotNull UUID getId() {
        return id;
    }

    public @NotNull List<InstructionEntity> getInstructions() {
        return instructions;
    }

    public @NotNull DataCombiningSourcesEntity getSources() {
        return sources;
    }
}
