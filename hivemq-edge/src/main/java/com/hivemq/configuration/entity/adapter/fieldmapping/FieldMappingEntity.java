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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.mappings.fieldmapping.FieldMapping;
import com.hivemq.persistence.mappings.fieldmapping.Instruction;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class FieldMappingEntity {

    @XmlElementWrapper(name = "instructions")
    @XmlElement(name = "instruction")
    private final @NotNull List<InstructionEntity> instructions;

    @XmlElement(name = "metadata")
    private final @NotNull MetadataEntity metaData;

    //no arg constructor for JaxB
    public FieldMappingEntity() {;
        instructions = new ArrayList<>();
        metaData = new MetadataEntity();
    }

    public FieldMappingEntity(
            final @NotNull List<InstructionEntity> instructions,
            final @NotNull MetadataEntity metaData) {
        this.instructions = instructions;
        this.metaData = metaData;
    }

    public @NotNull List<InstructionEntity> getInstructions() {
        return instructions;
    }

    public @NotNull MetadataEntity getMetaData() {
        return metaData;
    }

    public static @NotNull FieldMappingEntity from(final @NotNull FieldMapping model) {
        if (model == null) {
            return null;
        }
        final List<InstructionEntity> fieldMappingEntityList =
                model.getInstructions().stream().map(InstructionEntity::from).collect(Collectors.toList());
        return new FieldMappingEntity(fieldMappingEntityList,
                MetadataEntity.from(model.getMetaData()));
    }

    public @NotNull FieldMapping to(final @NotNull ObjectMapper mapper) {
        final List<Instruction> instructions =
                getInstructions().stream().map(InstructionEntity::to).collect(Collectors.toList());
        return new FieldMapping(
                instructions,
                getMetaData().to(mapper));
    }
}
