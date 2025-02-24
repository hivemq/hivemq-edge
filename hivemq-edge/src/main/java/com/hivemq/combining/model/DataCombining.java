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
package com.hivemq.combining.model;

import com.hivemq.configuration.entity.adapter.fieldmapping.InstructionEntity;
import com.hivemq.configuration.entity.combining.DataCombiningEntity;
import com.hivemq.persistence.mappings.fieldmapping.Instruction;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public record DataCombining(UUID id, DataCombiningSources sources, String destination,
                            List<com.hivemq.persistence.mappings.fieldmapping.Instruction> instructions) {

    public static @NotNull DataCombining fromModel(final @NotNull com.hivemq.edge.api.model.DataCombining model) {
        final List<Instruction> instructions = model.getInstructions().stream().map(Instruction::from).toList();
        return new DataCombining(model.getId(),
                DataCombiningSources.fromModel(model.getSources()),
                model.getDestination(),
                instructions);
    }

    public @NotNull com.hivemq.edge.api.model.DataCombining toModel() {
        final List<com.hivemq.edge.api.model.Instruction> instructionsAsModelClass =
                instructions.stream().map(Instruction::toModel).toList();
        return new com.hivemq.edge.api.model.DataCombining().id(id)
                .sources(sources.toModel())
                .destination(destination)
                .instructions(instructionsAsModelClass);
    }


    public static @NotNull DataCombining fromPersistence(final @NotNull DataCombiningEntity model) {
        final List<Instruction> instructions = model.getInstructions().stream().map(InstructionEntity::to).toList();
        return new DataCombining(model.getId(),
                DataCombiningSources.fromPersistence(model.getSources()),
                model.getDestination(),
                instructions);
    }

    public @NotNull DataCombiningEntity toPersistence() {
        final List<InstructionEntity> instructions = this.instructions().stream().map(InstructionEntity::from).toList();
        return new DataCombiningEntity(this.id(), this.sources.toPersistence(), this.destination, instructions);
    }
}
