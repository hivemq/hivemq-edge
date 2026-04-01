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
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public record CompiledAdapterConfig(
        @JsonProperty("adapterId") @NotNull String adapterId,
        @JsonProperty("protocolId") @NotNull String protocolId,
        @JsonProperty("config") @NotNull Map<String, Object> config,
        @JsonProperty("tags") @NotNull List<CompiledTag> tags,
        @JsonProperty("northboundMappings") @NotNull List<CompiledNorthboundMapping> northboundMappings,
        @JsonProperty("southboundMappings") @NotNull List<Object> southboundMappings) {

    @JsonCreator
    public CompiledAdapterConfig(
            @JsonProperty("adapterId") final @NotNull String adapterId,
            @JsonProperty("protocolId") final @NotNull String protocolId,
            @JsonProperty("config") final @NotNull Map<String, Object> config,
            @JsonProperty("tags") final @NotNull List<CompiledTag> tags,
            @JsonProperty("northboundMappings") final @NotNull List<CompiledNorthboundMapping> northboundMappings,
            @JsonProperty("southboundMappings") final @NotNull List<Object> southboundMappings) {
        this.adapterId = adapterId;
        this.protocolId = protocolId;
        this.config = config;
        this.tags = tags;
        this.northboundMappings = northboundMappings;
        this.southboundMappings = southboundMappings;
    }
}
