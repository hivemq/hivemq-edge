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

/**
 * Parsed representation of {@code edge-project.yaml}.
 *
 * <pre>{@code
 * edgeVersion: "2.5"
 * sources:
 *   - instances/
 * output: build/
 * preprocessing: []
 * }</pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EdgeProjectDescriptor {

    public @Nullable String edgeVersion;
    public @NotNull List<String> sources = List.of(".");
    public @NotNull String output = "build";
    public @NotNull List<Object> preprocessing = List.of();
}
