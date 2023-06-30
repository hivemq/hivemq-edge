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
package com.hivemq.api.model.adapters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

public class ProtocolAdapter {

    @JsonProperty("id")
    private final @NotNull String id;

    @JsonProperty("protocol")
    @Schema(description = "The supported protocol")
    private final @NotNull String protocol;

    @JsonProperty("name")
    @Schema(description = "The name of the adapter")
    private final @NotNull String name;

    @JsonProperty("description")
    @Schema(description = "The description")
    private final @NotNull String description;

    @JsonProperty("url")
    @Schema(description = "The url of the adapter")
    private final @NotNull String url;

    @JsonProperty("version")
    @Schema(description = "The installed version of the adapter")
    private final @NotNull String version;

    @JsonProperty("logoUrl")
    @Schema(description = "The logo of the adapter")
    private final @NotNull String logoUrl;

    @JsonProperty("author")
    @Schema(description = "The author of the adapter")
    private final @NotNull String author;

    @JsonProperty("configSchema")
    @Schema(description = "JSONSchema in the \'https://json-schema.org/draft/2020-12/schema\' format, which describes the configuration requirements for the adapter.")
    private final @NotNull JsonNode configSchema;

    public ProtocolAdapter(
            @JsonProperty("id") final @NotNull String id,
            @JsonProperty("protocol") final @NotNull String protocol,
            @JsonProperty("name") final @NotNull String name,
            @JsonProperty("description") final @NotNull String description,
            @JsonProperty("url") final @NotNull String url,
            @JsonProperty("version") final @NotNull String version,
            @JsonProperty("logoUrl") final @NotNull String logoUrl,
            @JsonProperty("author") final @NotNull String author,
            @JsonProperty("configSchema") final @NotNull JsonNode configSchema) {
        this.id = id;
        this.protocol = protocol;
        this.name = name;
        this.description = description;
        this.url = url;
        this.version = version;
        this.logoUrl = logoUrl;
        this.author = author;
        this.configSchema = configSchema;
    }

    public @NotNull String getId() {
        return id;
    }

    public @NotNull String getProtocol() {
        return protocol;
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull String getDescription() {
        return description;
    }

    public @NotNull String getUrl() {
        return url;
    }

    public @NotNull String getVersion() {
        return version;
    }

    public @NotNull String getLogoUrl() {
        return logoUrl;
    }

    public @NotNull String getAuthor() {
        return author;
    }

    public @NotNull JsonNode getConfigSchema() {
        return configSchema;
    }
}
