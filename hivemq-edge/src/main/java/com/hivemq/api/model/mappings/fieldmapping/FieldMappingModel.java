package com.hivemq.api.model.mappings.fieldmapping;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.mappings.fields.FieldMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class FieldMappingModel {

    @JsonProperty(value = "instructions", required = true)
    @Schema(description = "List of instructions to be applied to incoming data")
    private final @NotNull List<InstructionModel> instructions;

    @JsonProperty(value = "metadata", required = true)
    @Schema(description = "Metadata for the whole mapping")
    private final @NotNull MetadataModel metadata;

    public FieldMappingModel(
            @JsonProperty(value = "instructions", required = true) @NotNull final List<InstructionModel> instructions,
            @JsonProperty(value = "metadata", required = true) @NotNull final MetadataModel metadata) {
        this.instructions = instructions;
        this.metadata = metadata;
    }

    public @NotNull List<InstructionModel> getInstructions() {
        return instructions;
    }

    public @NotNull MetadataModel getMetadata() {
        return metadata;
    }

    public static FieldMappingModel fromFieldMapping(final @NotNull FieldMapping fieldMapping) {
        return new FieldMappingModel(
                fieldMapping.getInstructions().stream().map(InstructionModel::from)
                        .collect(Collectors.toList()),
                MetadataModel.from(fieldMapping.getMetaData()));
    }
}
