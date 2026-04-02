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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.nio.file.Path;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SourceDataCombiner {

    /** Optional UUID. If absent the compiler derives one from {@code name}. */
    public @Nullable String id;

    public @Nullable String name;
    public @Nullable String description;
    public @NotNull List<SourceCombinerMapping> mappings = List.of();

    /** Set by {@code GlobalResolver} when collecting combiners from files. Used for diagnostic file references. */
    @JsonIgnore
    public @Nullable Path sourcePath;

    /** Source position — set by {@code YamlFileParser} after parsing; -1 means unknown. 0-based (LSP convention). */
    @JsonIgnore
    public int line = -1;

    @JsonIgnore
    public int character = -1;
}
