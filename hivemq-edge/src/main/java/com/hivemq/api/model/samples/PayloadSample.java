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
package com.hivemq.api.model.samples;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

public class PayloadSample {

    @JsonProperty("payload")
    @Schema(description = "The payload of the sample. The bytes are base64 encoded to ensure compatibility even if the payload is a arbitrary byte sequence.",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private final @NotNull String payload;

    @JsonCreator
    public PayloadSample( @JsonProperty("payload") final @NotNull String payload) {
        this.payload = payload;
    }

    public @NotNull String getPayload() {
        return payload;
    }
}
