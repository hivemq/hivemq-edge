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
package com.hivemq.edge.compiler.lib.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public record CompiledCombinerMapping(
        @JsonProperty("trigger") @NotNull CompiledCombinerTrigger trigger,
        @JsonProperty("output") @NotNull CompiledCombinerOutput output,
        @JsonProperty("instructions") @NotNull List<CompiledInstruction> instructions) {

    @JsonCreator
    public CompiledCombinerMapping(
            @JsonProperty("trigger") final @NotNull CompiledCombinerTrigger trigger,
            @JsonProperty("output") final @NotNull CompiledCombinerOutput output,
            @JsonProperty("instructions") final @NotNull List<CompiledInstruction> instructions) {
        this.trigger = trigger;
        this.output = output;
        this.instructions = instructions;
    }
}
