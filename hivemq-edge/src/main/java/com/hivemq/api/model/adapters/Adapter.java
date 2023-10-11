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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.api.model.status.Status;
import com.hivemq.edge.HiveMQEdgeConstants;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

public class Adapter {

    @JsonProperty("id")
    @Schema(name = "id",
            description = "The adapter id, must be unique and only contain alpha numeric characters with spaces and hyphens.",
            format = "string",
            minLength = 1,
            required = true,
            maxLength = HiveMQEdgeConstants.MAX_ID_LEN,
            pattern = HiveMQEdgeConstants.ID_REGEX)
    private final @NotNull String id;

    @JsonProperty("type")
    @Schema(name = "type",
            description = "The adapter type associated with this instance")
    private final @NotNull String protocolAdapterType;

    @JsonProperty("config")
    @Schema(name = "config",
            description = "The adapter configuration associated with this instance")
    private final @NotNull Map<String, Object>  config;

    @JsonProperty("status")
    @Schema(name = "status",
            description = "Information associated with the runtime of this adapter")
    private final @Nullable Status status;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Adapter(
            @JsonProperty("id") final @NotNull String id,
            @JsonProperty("type") final @NotNull String protocolAdapterType,
            @JsonProperty("config") final @NotNull Map<String, Object>  config,
            @JsonProperty("status") final @Nullable Status status) {
        this.id = id;
        this.protocolAdapterType = protocolAdapterType;
        this.config = config;
        this.status = status;
    }

    public @Nullable Status getStatus() {
        return status;
    }

    public @NotNull String getId() {
        return id;
    }

    public @NotNull String getProtocolAdapterType() {
        return protocolAdapterType;
    }

    public @NotNull Map<String, Object> getConfig() {
        return config;
    }
}
