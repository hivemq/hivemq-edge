package com.hivemq.api.model.adapters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.edge.HiveMQEdgeConstants;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A category is a unique entity and represents a curated grouping of a protocol adapter. A protocol adapter
 * maybe in 1 category.
 *
 * @author Simon L Johnson
 */
public class ProtocolAdapterCategory {

    @JsonProperty("name")
    @Schema(name = "name",
            description = "The unique name of the category to be used in API communication.",
            format = "string",
            minLength = 1,
            required = true,
            maxLength = HiveMQEdgeConstants.MAX_NAME_LEN,
            pattern = HiveMQEdgeConstants.NAME_REGEX)
    private final @NotNull String name;

    @JsonProperty("displayName")
    @Schema(name = "displayName",
            description = "The display name of the category to be used in HCIs.",
            format = "string",
            minLength = 1,
            required = true)
    private final @NotNull String displayName;

    @JsonProperty("description")
    @Schema(name = "description",
            description = "The description associated with the category.",
            format = "string")
    private final @NotNull String description;

    @JsonProperty("image")
    @Schema(name = "image",
            description = "The image associated with the category.",
            format = "string")
    private final @NotNull String image;

    public ProtocolAdapterCategory(
            @JsonProperty("name") final @NotNull String name,
            @JsonProperty("displayName") final @NotNull String displayName,
            @JsonProperty("description") final @Nullable String description,
            @JsonProperty("image") final @Nullable String image) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.image = image;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getImage() {
        return image;
    }
}
