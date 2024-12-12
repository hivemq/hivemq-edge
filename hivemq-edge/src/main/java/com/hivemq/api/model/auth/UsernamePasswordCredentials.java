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
package com.hivemq.api.model.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author Simon L Johnson
 */
public class UsernamePasswordCredentials {

    @JsonProperty(value = "userName", required = true)
    @Schema(description = "The userName associated with the user",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = 1000)
    private final @NotNull String userName;

    @JsonProperty(value = "password", required = true)
    @Schema(description = "The password associated with the user",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = 1000)
    private final @NotNull String password;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public UsernamePasswordCredentials(
            @JsonProperty(value = "userName", required = true) final @NotNull String userName,
            @JsonProperty(value = "password", required = true) final @NotNull String password) {

        this.userName = userName;
        this.password = password;
    }

    public @NotNull String getUserName() {
        return userName;
    }

    public @NotNull String getPassword() {
        return password;
    }
}
