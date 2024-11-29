package com.hivemq.persistence.mappings.fieldmapping;

import com.hivemq.api.model.mappings.fieldmapping.InstructionModel;
import org.jetbrains.annotations.NotNull;

public class Instruction {
    private final @NotNull String destinationFieldName;

    private final @NotNull String sourceFieldName;

    public Instruction(
            @NotNull final String destinationFieldName,
            @NotNull final String sourceFieldName) {
        this.destinationFieldName = destinationFieldName;
        this.sourceFieldName = sourceFieldName;
    }

    public @NotNull String getDestinationFieldName() {
        return destinationFieldName;
    }

    public @NotNull String getSourceFieldName() {
        return sourceFieldName;
    }

    public static Instruction from(InstructionModel model) {
        return new Instruction(
                model.getDestinationFieldName(),
                model.getDestinationFieldName());
    }
}
