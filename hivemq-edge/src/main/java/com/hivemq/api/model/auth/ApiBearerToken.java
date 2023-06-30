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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author Simon L Johnson
 */
public class ApiBearerToken {

    @JsonProperty("token")
    @Schema(description = "The token associated a set of authenticated credentials")
    private final @NotNull String token;


    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ApiBearerToken(
            @JsonProperty("token") final @NotNull String token) {

        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
