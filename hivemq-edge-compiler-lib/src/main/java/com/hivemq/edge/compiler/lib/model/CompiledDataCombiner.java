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

public record CompiledDataCombiner(
        @JsonProperty("name") @NotNull String name,
        @JsonProperty("mappings") @NotNull List<CompiledCombinerMapping> mappings) {

    @JsonCreator
    public CompiledDataCombiner(
            @JsonProperty("name") final @NotNull String name,
            @JsonProperty("mappings") final @NotNull List<CompiledCombinerMapping> mappings) {
        this.name = name;
        this.mappings = mappings;
    }
}
