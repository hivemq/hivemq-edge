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

public class MtConnectAdapterTagDefinition implements TagDefinition {
    @JsonProperty(value = "httpHeaders")
    @ModuleConfigField(title = "HTTP Headers", description = "HTTP headers to be added to your requests")
    private final @NotNull List<MtConnectAdapterHttpHeader> httpHeaders;

    @JsonProperty(value = "url", required = true)
    @ModuleConfigField(title = "URL",
                       description = "The url of the HTTP request you would like to make",
                       format = ModuleConfigField.FieldType.URI,
                       required = true)
    private final @NotNull String url;

    @JsonCreator
    public MtConnectAdapterTagDefinition(
            @JsonProperty(value = "url", required = true) final @NotNull String url,
            @JsonProperty(value = "httpHeaders") final @Nullable List<MtConnectAdapterHttpHeader> httpHeaders) {
        this.url = url;
        this.httpHeaders = Objects.requireNonNullElseGet(httpHeaders, List::of);
    }

    public @NotNull List<MtConnectAdapterHttpHeader> getHttpHeaders() {
        return httpHeaders;
    }

    public @NotNull String getUrl() {
        return url;
    }

}
