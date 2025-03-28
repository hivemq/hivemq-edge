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
package com.hivemq.configuration.entity.adapter.fieldmapping;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.combining.model.DataIdentifierReference;
import com.hivemq.configuration.entity.combining.DataIdentifierReferenceEntity;
import com.hivemq.persistence.mappings.fieldmapping.Instruction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.XmlElement;
import java.util.Objects;

public class InstructionEntity {

    @JsonProperty("source")
    @XmlElement(name = "source")
    private @NotNull String sourceFieldName;

    @JsonProperty("destination")
    @XmlElement(name = "destination")
    private @NotNull String destinationFieldName;

    @JsonProperty("origin")
    @XmlElement(name = "origin")
    private @Nullable DataIdentifierReferenceEntity origin;

    // no- arg for JaxB
    public InstructionEntity() {
    }

    public InstructionEntity(
            final @NotNull String sourceFieldName,
            final @NotNull String destinationFieldName,
            final @Nullable DataIdentifierReferenceEntity origin) {
        this.sourceFieldName = sourceFieldName;
        this.destinationFieldName = destinationFieldName;
        this.origin = origin;
    }

    public @NotNull String getDestinationFieldName() {
        return destinationFieldName;
    }

    public @NotNull String getSourceFieldName() {
        return sourceFieldName;
    }

    public @Nullable DataIdentifierReferenceEntity getOrigin() {
        return origin;
    }

    public static @NotNull InstructionEntity from(final @NotNull Instruction model) {
        return new InstructionEntity(model.sourceFieldName(),
                model.destinationFieldName(),
                model.dataIdentifierReference() != null ? model.dataIdentifierReference().toPersistence() : null);
    }

    public static @NotNull InstructionEntity from(final @NotNull com.hivemq.edge.api.model.Instruction model) {
        return new InstructionEntity(model.getSource(),
                model.getDestination(),
                model.getSourceRef() != null ? DataIdentifierReferenceEntity.from(model.getSourceRef()) : null);
    }

    public @NotNull Instruction to() {
        return new Instruction(getSourceFieldName(),
                getDestinationFieldName(),
                getOrigin() != null ? DataIdentifierReference.fromPersistence(getOrigin()) : null);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final InstructionEntity that = (InstructionEntity) o;
        return Objects.equals(getSourceFieldName(), that.getSourceFieldName()) &&
                Objects.equals(getDestinationFieldName(), that.getDestinationFieldName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSourceFieldName(), getDestinationFieldName());
    }
}
