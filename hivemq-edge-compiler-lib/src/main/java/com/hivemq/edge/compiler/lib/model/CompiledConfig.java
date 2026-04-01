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

/** Top-level compiled configuration artifact. */
public record CompiledConfig(
        @JsonProperty("_notice") @NotNull String notice,
        @JsonProperty("_signature") @NotNull String signature,
        @JsonProperty("_formatVersion") @NotNull String formatVersion,
        @JsonProperty("_edgeVersion") @NotNull String edgeVersion,
        @JsonProperty("protocolAdapters") @NotNull List<CompiledAdapterConfig> protocolAdapters,
        @JsonProperty("dataCombiners") @NotNull List<CompiledDataCombiner> dataCombiners) {

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
            @JsonProperty("dataCombiners") final @NotNull List<CompiledDataCombiner> dataCombiners) {
        this.notice = notice;
        this.signature = signature;
        this.formatVersion = formatVersion;
        this.edgeVersion = edgeVersion;
        this.protocolAdapters = protocolAdapters;
        this.dataCombiners = dataCombiners;
    }
}
