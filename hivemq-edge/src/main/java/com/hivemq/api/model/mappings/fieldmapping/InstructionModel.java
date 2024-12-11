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
import com.hivemq.persistence.mappings.fieldmapping.Instruction;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Schema(name = "Instruction")
public class InstructionModel {

    @JsonProperty(value = "destination", required = true)
    @Schema(name = "destination",
            description = "The field in the output object where the data will be written to",
            minLength = 1,
            maxLength = 2048)
    private final @NotNull String destinationFieldName;

    @JsonProperty(value = "source", required = true)
    @Schema(name = "source", description = "The field in the input object where the data will be read from",
            minLength = 1,
            maxLength = 2048)
    private final @NotNull String sourceFieldName;

    @JsonProperty(value = "transformation")
    @Schema(name = "transformation", description = "The kind of transformation that is performed on the data.")
    private final @Nullable TransformationModel transformationModel;


    @JsonCreator
    public InstructionModel(
            @JsonProperty(value = "source", required = true) final @NotNull String sourceFieldName,
            @JsonProperty(value = "destination", required = true) final @NotNull String destinationFieldName,
            @JsonProperty(value = "transformation") final @Nullable TransformationModel transformationModel) {
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

    public static @NotNull InstructionModel from(final @NotNull Instruction instruction) {
        return new InstructionModel(instruction.getSourceFieldName(), instruction.getDestinationFieldName(), null);
    }
}
