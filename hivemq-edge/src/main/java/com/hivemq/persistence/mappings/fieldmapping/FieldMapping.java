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
package com.hivemq.persistence.mappings.fieldmapping;

import com.hivemq.api.model.mappings.fieldmapping.FieldMappingModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class FieldMapping {

    public static @NotNull FieldMapping DEFAULT_FIELD_MAPPING =
            new FieldMapping(List.of(new Instruction("value", "value")));

    private final @NotNull List<Instruction> instructions;

    public FieldMapping(final @NotNull List<Instruction> instructions) {
        this.instructions = instructions;
    }

    public @NotNull List<Instruction> getInstructions() {
        return instructions;
    }

    public static @NotNull FieldMapping fromModel(final @NotNull FieldMappingModel model) {
        return new FieldMapping(model.getInstructions().stream().map(Instruction::from).collect(Collectors.toList()));
    }
}
