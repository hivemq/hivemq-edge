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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.edge.HiveMQEdgeConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNullElse;

/**
 * A category is a unique entity and represents a curated grouping of a protocol adapter. A protocol adapter
 * maybe in 1 category.
 */
public class ProtocolAdapterCategory {

    @JsonProperty("name")
    @Schema(name = "name",
            description = "The unique name of the category to be used in API communication.",
            format = "string",
            minLength = 1,
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = HiveMQEdgeConstants.MAX_NAME_LEN,
            pattern = HiveMQEdgeConstants.NAME_REGEX)
    private final @NotNull String name;

    @JsonProperty("displayName")
    @Schema(name = "displayName",
            description = "The display name of the category to be used in HCIs.",
            format = "string",
            minLength = 1,
            requiredMode = Schema.RequiredMode.REQUIRED)
    private final @NotNull String displayName;

    @JsonProperty("description")
    @Schema(name = "description", description = "The description associated with the category.", format = "string")
    private final @NotNull String description;

    @JsonProperty("image")
    @Schema(name = "image", description = "The image associated with the category.", format = "string")
    private final @NotNull String image;

    public ProtocolAdapterCategory(
            @JsonProperty("name") final @NotNull String name,
            @JsonProperty("displayName") final @NotNull String displayName,
            @JsonProperty("description") final @Nullable String description,
            @JsonProperty("image") final @Nullable String image) {
        this.name = name;
        this.displayName = displayName;
        this.description = requireNonNullElse(description, "");
        this.image = requireNonNullElse(image, "");
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull String getDisplayName() {
        return displayName;
    }

    public @NotNull String getDescription() {
        return description;
    }

    public @NotNull String getImage() {
        return image;
    }
}
