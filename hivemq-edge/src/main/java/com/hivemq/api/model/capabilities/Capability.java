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
package com.hivemq.api.model.capabilities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

public class Capability {

    @JsonProperty("id")
    @Schema(description = "The identifier of this capability")
    private final @NotNull String id;

    @JsonProperty("displayName")
    @Schema(description = "A human readable name, intended to be used to display at front end.")
    private final @NotNull String displayName;

    @JsonProperty("description")
    @Schema(description = "A description for the capability")
    private final @NotNull String description;

    public Capability(@JsonProperty("id") final @NotNull String id,
                      @JsonProperty("displayName") final @NotNull String displayName,
                      @JsonProperty("description") final @NotNull String description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }

    public @NotNull String getId() {
        return id;
    }

    public @NotNull String getDisplayName() {
        return displayName;
    }

    public @NotNull String getDescription() {
        return description;
    }
}

