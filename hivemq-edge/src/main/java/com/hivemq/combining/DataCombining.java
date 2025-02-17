package com.hivemq.combining;

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
                com.hivemq.combining.DataCombiningSources.fromModel(model.getSources()),
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
