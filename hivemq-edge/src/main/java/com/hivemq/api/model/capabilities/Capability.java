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
}

