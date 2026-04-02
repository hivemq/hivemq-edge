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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Source for a combiner instruction — either a tag reference or a topic filter, plus the JSONPath expression
 * selecting the field in the incoming document. Exactly one of {@code tag} or {@code topic} must be non-null.
 */
public record CompiledInstructionSource(
        @JsonProperty("tag") @Nullable String tag,
        @JsonProperty("topic") @Nullable String topic,
        @JsonProperty("field") @NotNull String field) {

    @JsonCreator
    public CompiledInstructionSource(
            @JsonProperty("tag") final @Nullable String tag,
            @JsonProperty("topic") final @Nullable String topic,
            @JsonProperty("field") final @NotNull String field) {
        this.tag = tag;
        this.topic = topic;
        this.field = field;
    }
}
