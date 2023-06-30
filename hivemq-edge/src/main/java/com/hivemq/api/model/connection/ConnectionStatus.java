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
package com.hivemq.api.model.connection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author Simon L Johnson
 */
public class ConnectionStatus {

    public enum STATUS {
        CONNECTED,
        DISCONNECTED,
        CONNECTING,
        DISCONNECTING
    }

    @JsonProperty("status")
    @Schema(description = "A mandatory status field.")
    private final @NotNull STATUS status;

    @JsonProperty("id")
    @Schema(description = "The identifier of the object")
    private final @NotNull String id;

    @JsonProperty("type")
    @Schema(description = "The type of the object")
    private final @NotNull String type;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ConnectionStatus(@NotNull @JsonProperty("status") final STATUS status,
                            @NotNull @JsonProperty("id") final String id,
                            @NotNull @JsonProperty("type") final String type) {
        this.status = status;
        this.id = id;
        this.type = type;
    }

    public STATUS getStatus() {
        return status;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }
}
