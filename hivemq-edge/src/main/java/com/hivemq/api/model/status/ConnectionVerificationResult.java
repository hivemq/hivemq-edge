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
import com.hivemq.extension.sdk.api.annotations.Nullable;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author Simon L Johnson
 */
public class ConnectionVerificationResult {

    enum RESULT {
        CONTINUE_SUCCESS, CONTINUE_WARNING, ERROR
    }

    @JsonProperty("connectionDetails")
    @Schema(description = "The original connection details.")
    private final @NotNull ConnectionDetails connectionDetails;

    @JsonProperty("message")
    @Schema(description = "The message.", nullable = true)
    private final @Nullable String message;

    @JsonProperty("result")
    @Schema(description = "The result.")
    private final @NotNull RESULT result;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ConnectionVerificationResult(
            @NotNull @JsonProperty("connectionDetails") final ConnectionDetails connectionDetails,
            @Nullable @JsonProperty("message") final String message,
            @NotNull @JsonProperty("result") final RESULT result) {
        this.connectionDetails = connectionDetails;
        this.message = message;
        this.result = result;
    }

    public ConnectionDetails getConnectionDetails() {
        return connectionDetails;
    }

    public String getMessage() {
        return message;
    }

    public RESULT getResult() {
        return result;
    }

    public static ConnectionVerificationResult success(@NotNull final ConnectionDetails details){
        return new ConnectionVerificationResult(details, "Socket established", RESULT.CONTINUE_SUCCESS);
    }

    public static ConnectionVerificationResult warning(@NotNull final ConnectionDetails details, String message){
        return new ConnectionVerificationResult(details, message, RESULT.CONTINUE_WARNING);
    }

    public static ConnectionVerificationResult error(@NotNull final ConnectionDetails details, String message){
        return new ConnectionVerificationResult(details, message, RESULT.ERROR);
    }

}
