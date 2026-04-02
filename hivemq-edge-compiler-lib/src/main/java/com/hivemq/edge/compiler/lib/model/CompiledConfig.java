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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Top-level compiled configuration artifact. */
public record CompiledConfig(
        @JsonProperty("_notice") @NotNull String notice,
        @JsonProperty("_signature") @NotNull String signature,
        @JsonProperty("_formatVersion") @NotNull String formatVersion,
        @JsonProperty("_edgeVersion") @NotNull String edgeVersion,
        @JsonProperty("protocolAdapters") @NotNull List<CompiledAdapterConfig> protocolAdapters,
        @JsonProperty("dataCombiners") @NotNull List<CompiledDataCombiner> dataCombiners,

        @JsonProperty("_workspace") @JsonInclude(JsonInclude.Include.NON_NULL) @Nullable
        JsonNode workspace) {

    public static final String NOTICE = "COMPILED CONFIGURATION. DO NOT EDIT.";
    public static final String SIGNATURE_UNSIGNED = "UNSIGNED-POC-BUILD";
    public static final String FORMAT_VERSION = "1.0";

    @JsonCreator
    public CompiledConfig(
            @JsonProperty("_notice") final @NotNull String notice,
            @JsonProperty("_signature") final @NotNull String signature,
            @JsonProperty("_formatVersion") final @NotNull String formatVersion,
            @JsonProperty("_edgeVersion") final @NotNull String edgeVersion,
            @JsonProperty("protocolAdapters") final @NotNull List<CompiledAdapterConfig> protocolAdapters,
            @JsonProperty("dataCombiners") final @NotNull List<CompiledDataCombiner> dataCombiners,
            @JsonProperty("_workspace") final @Nullable JsonNode workspace) {
        this.notice = notice;
        this.signature = signature;
        this.formatVersion = formatVersion;
        this.edgeVersion = edgeVersion;
        this.protocolAdapters = protocolAdapters;
        this.dataCombiners = dataCombiners;
        this.workspace = workspace;
    }

    /** Convenience constructor for configs without a workspace layout. */
    public CompiledConfig(
            final @NotNull String notice,
            final @NotNull String signature,
            final @NotNull String formatVersion,
            final @NotNull String edgeVersion,
            final @NotNull List<CompiledAdapterConfig> protocolAdapters,
            final @NotNull List<CompiledDataCombiner> dataCombiners) {
        this(notice, signature, formatVersion, edgeVersion, protocolAdapters, dataCombiners, null);
    }
}
