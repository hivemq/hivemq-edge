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

public record DataCombining(UUID id, DataCombiningSources sources, DataCombiningDestination destination,
                            List<com.hivemq.persistence.mappings.fieldmapping.Instruction> instructions) {

    public static @NotNull DataCombining fromModel(final @NotNull com.hivemq.edge.api.model.DataCombining model) {
        return new DataCombining(model.getId(),
                DataCombiningSources.fromModel(model.getSources()),
                DataCombiningDestination.from(model.getDestination()),
                model.getInstructions().stream().map(Instruction::from).toList());
    }

    public static @NotNull DataCombining fromPersistence(final @NotNull DataCombiningEntity model) {
        return new DataCombining(model.getId(),
                DataCombiningSources.fromPersistence(model.getSources()),
                DataCombiningDestination.fromPersistence(model.getDestination()),
                model.getInstructions().stream().map(InstructionEntity::to).toList());
    }

    public @NotNull com.hivemq.edge.api.model.DataCombining toModel() {
        return new com.hivemq.edge.api.model.DataCombining().id(id())
                .sources(sources().toModel())
                .destination(destination().toModel())
                .instructions(instructions().stream().map(Instruction::toModel).toList());
    }

    public @NotNull DataCombiningEntity toPersistence() {
        return new DataCombiningEntity(id(),
                sources().toPersistence(),
                destination().toPersistence(),
                instructions().stream().map(InstructionEntity::from).toList());
    }
}
