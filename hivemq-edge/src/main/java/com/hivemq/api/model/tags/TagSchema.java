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
package com.hivemq.api.model.tags;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.jetbrains.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

public class TagSchema {
    @JsonProperty("protocolId")
    @Schema(description = "The id assigned to the protocol adapter type")
    private final @NotNull String protocolId;

    @JsonProperty("configSchema")
    @Schema(description = "JSONSchema in the \'https://json-schema.org/draft/2020-12/schema\' format, which describes the configuration requirements for the adapter.")
    private final @NotNull JsonNode configSchema;

    public TagSchema(
            @JsonProperty("protocolId") final @NotNull String protocolId,
            @JsonProperty("configSchema") final @NotNull JsonNode configSchema) {
        this.protocolId = protocolId;
        this.configSchema = configSchema;
    }

    public @NotNull String getProtocolId() {
        return protocolId;
    }

    public @NotNull JsonNode getConfigSchema() {
        return configSchema;
    }
}
