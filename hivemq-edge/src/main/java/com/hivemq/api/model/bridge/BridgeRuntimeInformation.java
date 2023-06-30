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
package com.hivemq.api.model.bridge;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.api.model.connection.ConnectionStatus;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author Simon L Johnson
 */
public class BridgeRuntimeInformation {

    @JsonProperty("connectionStatus")
    @Schema(description = "The current status of the connection")
    private final @NotNull ConnectionStatus connectionStatus;

    @JsonProperty("errorMessage")
    @Schema(description = "An error message associated with the connection")
    private final @Nullable String errorMessage;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public BridgeRuntimeInformation(
            @JsonProperty("connectionStatus") final @NotNull ConnectionStatus connectionStatus,
            @JsonProperty("errorMessage") final @NotNull String errorMessage) {
        this.connectionStatus = connectionStatus;
        this.errorMessage = errorMessage;
    }

    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
