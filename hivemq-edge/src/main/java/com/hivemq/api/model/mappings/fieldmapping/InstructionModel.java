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
package com.hivemq.api.model.mappings.fieldmapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.mappings.fields.Instruction;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;

public class InstructionModel {

    @JsonProperty(value = "destinationFieldName", required = true)
    @Schema(description = "The field in the output object where the data will be written to")
    private final @NotNull String destinationFieldName;

    @JsonProperty(value = "sourceFieldName", required = true)
    @Schema(description = "The field in the input object where the data will be read from")
    private final @NotNull String sourceFieldName;

    @JsonProperty(value = "transformation", required = true)
    @Schema(description = "The transformation to be applied between mapping the fields")
    private final @NotNull TransformationModel transformationModel;

    @JsonCreator
    public InstructionModel(
            @JsonProperty(value = "destinationFieldName", required = true) @NotNull final String destinationFieldName,
            @JsonProperty(value = "sourceFieldName", required = true) @NotNull final String sourceFieldName,
            @JsonProperty(value = "transformation", required = true) @NotNull final TransformationModel transformationModel) {
        this.destinationFieldName = destinationFieldName;
        this.sourceFieldName = sourceFieldName;
        this.transformationModel = transformationModel;
    }

    public @NotNull String getDestinationFieldName() {
        return destinationFieldName;
    }

    public @NotNull String getSourceFieldName() {
        return sourceFieldName;
    }

    public @NotNull TransformationModel getTransformationModel() {
        return transformationModel;
    }

    public static InstructionModel from(final @NotNull Instruction instructionModel) {
        return new InstructionModel(
                instructionModel.getDestinationFieldName(),
                instructionModel.getSourceFieldName(),
                TransformationModel.from(instructionModel.getTransformation()));
    }
}
