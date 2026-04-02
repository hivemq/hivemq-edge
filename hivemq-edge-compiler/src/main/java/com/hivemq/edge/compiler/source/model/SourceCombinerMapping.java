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
package com.hivemq.edge.compiler.source.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SourceCombinerMapping {

    /** Optional UUID. If absent the compiler derives one from {@code combinerName::name}. */
    public @Nullable String id;

    /** Optional name; used for ID derivation if {@code id} is absent. */
    public @Nullable String name;

    /** Optional description — passed through but not used by the runtime. */
    public @Nullable String description;

    public @Nullable SourceTrigger trigger;
    public @Nullable SourceCombinerOutput output;
    public @NotNull List<SourceInstruction> instructions = List.of();

    /** Source position — set by {@code YamlFileParser} after parsing; -1 means unknown. 0-based (LSP convention). */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public int line = -1;

    @com.fasterxml.jackson.annotation.JsonIgnore
    public int character = -1;
}
