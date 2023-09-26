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
package com.hivemq.api.model.status;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author Simon L Johnson
 */
public class StatusTransitionResult {

    public enum STATUS {
        PENDING,
        COMPLETE
    }

    @JsonProperty("status")
    @Schema(description = "The status to perform on the target connection.")
    private final @NotNull STATUS status;

    @JsonProperty("callbackTimeoutMillis")
    @Schema(description = "The callback timeout specifies the minimum amount of time (in milliseconds) that the API advises the client to backoff before rechecking the (runtime or connection) status of this object. This is only applicable when the status is 'PENDING'.")
    private final @NotNull Integer callbackTimeoutMillis;

    @JsonProperty("identifier")
    @Schema(description = "The identifier of the object in transition")
    private final @NotNull String identifier;

    @JsonProperty("type")
    @Schema(description = "The type of the object in transition")
    private final @NotNull String type;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public StatusTransitionResult(@JsonProperty("type") final String type,
                                  @JsonProperty("identifier") final String identifier,
                                  @JsonProperty("status") final STATUS status,
                                  @JsonProperty("callbackTimeoutMillis") final Integer callbackTimeoutMillis) {
        this.type = type;
        this.status = status;
        this.identifier = identifier;
        this.callbackTimeoutMillis = callbackTimeoutMillis;
    }

    public @NotNull String getType() {
        return type;
    }

    public @NotNull STATUS getStatus() {
        return status;
    }

    public @NotNull String getIdentifier() {
        return identifier;
    }

    public @NotNull Integer getCallbackTimeoutMillis() {
        return callbackTimeoutMillis;
    }

    public static StatusTransitionResult pending(String type, String identifier, Integer timeout){
        return new StatusTransitionResult(type, identifier, STATUS.PENDING, timeout);
    }

    public static StatusTransitionResult complete(String type, String identifier){
        return new StatusTransitionResult(type, identifier, STATUS.COMPLETE, null);
    }
}
