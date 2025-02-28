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
package com.hivemq.edge.adapters.mtconnect.config.tag;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.tag.TagDefinition;
import com.hivemq.edge.adapters.mtconnect.config.MtConnectAdapterHttpHeader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class MtConnectAdapterTagDefinition implements TagDefinition {
    private static final int DEFAULT_HTTP_CONNECT_TIMEOUT_SECONDS = 5;
    private static final int MIN_HTTP_CONNECT_TIMEOUT_SECONDS = 1;
    private static final int MAX_HTTP_CONNECT_TIMEOUT_SECONDS = 60;

    @JsonProperty(value = "httpHeaders")
    @ModuleConfigField(title = "HTTP Headers", description = "HTTP headers to be added to your requests")
    private final @NotNull List<MtConnectAdapterHttpHeader> httpHeaders;

    @JsonProperty(value = "url", required = true)
    @ModuleConfigField(title = "URL",
                       description = "The url of the HTTP request you would like to make",
                       format = ModuleConfigField.FieldType.URI,
                       required = true)
    private final @NotNull String url;

    @JsonProperty("httpConnectTimeoutSeconds")
    @ModuleConfigField(title = "HTTP Connection Timeout",
                       description = "Timeout (in seconds) to allow the underlying HTTP connection to be established",
                       defaultValue = DEFAULT_HTTP_CONNECT_TIMEOUT_SECONDS + "",
                       numberMin = MIN_HTTP_CONNECT_TIMEOUT_SECONDS,
                       numberMax = MAX_HTTP_CONNECT_TIMEOUT_SECONDS)
    private final int httpConnectTimeoutSeconds;
   
    @JsonProperty(value = "schemaFile", required = true)
    @ModuleConfigField(title = "Schema File",
                       description = "The file that contains the MTConnect schema",
                       format = ModuleConfigField.FieldType.UNSPECIFIED,
                       required = true)
    private final @NotNull String schemaFile;

    @JsonCreator
    public MtConnectAdapterTagDefinition(
            @JsonProperty(value = "url", required = true) final @NotNull String url,
            @JsonProperty(value = "schemaFile", required = true) final @NotNull String schemaFile,
            @JsonProperty(value = "httpConnectTimeoutSeconds") final @Nullable Integer httpConnectTimeoutSeconds,
            @JsonProperty(value = "httpHeaders") final @Nullable List<MtConnectAdapterHttpHeader> httpHeaders) {
        this.url = url;
        this.schemaFile = schemaFile;
        this.httpHeaders = Objects.requireNonNullElseGet(httpHeaders, List::of);
        this.httpConnectTimeoutSeconds = Optional.ofNullable(httpConnectTimeoutSeconds)
                .map(s -> Math.min(s, MAX_HTTP_CONNECT_TIMEOUT_SECONDS))
                .map(s -> Math.max(s, MIN_HTTP_CONNECT_TIMEOUT_SECONDS))
                .orElse(DEFAULT_HTTP_CONNECT_TIMEOUT_SECONDS);
    }

    public @NotNull String getSchemaFile() {
        return schemaFile;
    }

    public @NotNull List<MtConnectAdapterHttpHeader> getHttpHeaders() {
        return httpHeaders;
    }

    public @NotNull String getUrl() {
        return url;
    }

    public int getHttpConnectTimeoutSeconds() {
        return httpConnectTimeoutSeconds;
    }
}
