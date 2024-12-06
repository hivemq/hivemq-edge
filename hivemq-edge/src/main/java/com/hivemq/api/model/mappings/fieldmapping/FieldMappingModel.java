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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.persistence.mappings.fieldmapping.FieldMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

@Schema(name = "FieldMapping")
public class FieldMappingModel {

    @JsonProperty(value = "instructions", required = true)
    @Schema(description = "List of instructions to be applied to incoming data")
    private final @NotNull List<InstructionModel> instructions;

    public FieldMappingModel(
            @JsonProperty(value = "instructions", required = true) final @NotNull List<InstructionModel> instructions) {
        this.instructions = instructions;
    }

    public @NotNull List<InstructionModel> getInstructions() {
        return instructions;
    }

    public static @NotNull FieldMappingModel from(final @NotNull FieldMapping fieldMapping) {
        return new FieldMappingModel(fieldMapping.getInstructions()
                .stream()
                .map(InstructionModel::from)
                .collect(Collectors.toList()));
    }
}
