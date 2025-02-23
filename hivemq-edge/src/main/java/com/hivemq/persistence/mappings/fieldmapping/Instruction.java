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

import com.hivemq.api.model.mappings.fieldmapping.InstructionModel;
import org.jetbrains.annotations.NotNull;

public class Instruction {

    private final @NotNull String destinationFieldName;
    private final @NotNull String sourceFieldName;

    public Instruction(
            final @NotNull String sourceFieldName, final @NotNull String destinationFieldName) {
        this.sourceFieldName = sourceFieldName;
        this.destinationFieldName = destinationFieldName;
    }

    public @NotNull String getDestinationFieldName() {
        return destinationFieldName;
    }

    public @NotNull String getSourceFieldName() {
        return sourceFieldName;
    }

    public static Instruction from(final @NotNull com.hivemq.edge.api.model.Instruction model) {
        return new Instruction(model.getSource(), model.getDestination());
    }

    public @NotNull com.hivemq.edge.api.model.Instruction toModel() {
        return new com.hivemq.edge.api.model.Instruction().source(sourceFieldName).destination(destinationFieldName);
    }

}
