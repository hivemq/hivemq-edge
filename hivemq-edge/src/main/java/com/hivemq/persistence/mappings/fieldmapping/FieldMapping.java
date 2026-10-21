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

import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FieldMapping {

    public static @NotNull FieldMapping DEFAULT_FIELD_MAPPING =
            new FieldMapping(List.of(new Instruction("value", "value", null)));

    private final @NotNull List<Instruction> instructions;

    public FieldMapping(final @NotNull List<Instruction> instructions) {
        this.instructions = instructions;
    }

    public @NotNull List<Instruction> getInstructions() {
        return instructions;
    }

    public static @NotNull FieldMapping fromModel(final @NotNull com.hivemq.edge.api.model.FieldMapping model) {
        return new FieldMapping(
                model.getInstructions().stream().map(Instruction::from).collect(Collectors.toList()));
    }

    public @NotNull com.hivemq.edge.api.model.FieldMapping toModel() {
        final List<com.hivemq.edge.api.model.Instruction> instructions =
                this.instructions.stream().map(Instruction::toModel).collect(Collectors.toList());
        return new com.hivemq.edge.api.model.FieldMapping().instructions(instructions);
    }

    /**
     * Compares the instructions, so two field mappings describing the same transform are equal.
     * <p>
     * <b>Why this was missing and what it cost.</b> Without it, equality fell back to identity -- and a
     * configuration is parsed afresh on every change, so no two instances were ever equal.
     * {@code SouthboundMapping.equals} compares its field mapping, and {@code ProtocolAdapterConfig.equals}
     * compares its southbound mappings, so **every adapter with a southbound mapping compared as changed on
     * every configuration edit** and was restarted. {@code ProtocolAdapterManager.refreshUpdatedAdapters} has a
     * guard that skips an unchanged adapter; the guard was correct and could never fire.
     * <p>
     * Measured by Miguel on PR 1765 (M2): one mapping update on one adapter restarted four others, about 23 s
     * for an S7-1500. For a method call the cost is worse than the delay -- a call in flight completed on the
     * device and its answer was dropped, because the adapter it belonged to had been torn down.
     * <p>
     * {@code Instruction} and {@code DataIdentifierReference} are records, so this compares by value all the way
     * down.
     */
    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof final FieldMapping that && instructions.equals(that.instructions);
    }

    @Override
    public int hashCode() {
        return instructions.hashCode();
    }

    @Override
    public @NotNull String toString() {
        return "FieldMapping{instructions=" + instructions + '}';
    }
}
