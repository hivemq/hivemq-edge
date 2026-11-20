/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua.config;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// A directory whose readable entries, or a glob pattern whose matches, belong to one piece of TLS
/// material.
///
/// Two elements rather than a bare path, because whether emptiness is acceptable is a question only
/// the operator can answer: a directory a deployment populates later is legitimately empty today,
/// while an empty directory where material was expected means Edge checks less than the
/// configuration says.
public record FileLocation(
        @JsonProperty(value = "location")
        @ModuleConfigField(
                title = "Location",
                description = "Path to the directory, or the glob pattern the files match.",
                required = true)
        @Nullable
        String location,

        /// Whether this may name no file at all. Defaults to false, so the strict reading is what an
        /// operator gets without writing anything and permitting emptiness is a visible choice.
        @JsonProperty(value = "may-be-empty")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "May be empty",
                description = "Whether naming no file is acceptable. Defaults to false, which refuses a "
                        + "configuration whose directory or pattern finds nothing.",
                defaultValue = "false")
        @Nullable
        Boolean mayBeEmpty) {

    @JsonCreator
    public FileLocation {}

    /// One that must name at least one file, which is what an operator gets by writing no flag.
    public FileLocation(final @Nullable String location) {
        this(location, null);
    }

    /// Whether emptiness was permitted, with the default applied.
    public boolean isMayBeEmpty() {
        return Boolean.TRUE.equals(mayBeEmpty);
    }

    @Override
    public @NotNull String toString() {
        return "FileLocation{location=" + location + ", mayBeEmpty=" + isMayBeEmpty() + "}";
    }
}
