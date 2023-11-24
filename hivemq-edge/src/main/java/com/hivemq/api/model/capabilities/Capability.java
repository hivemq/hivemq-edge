package com.hivemq.api.model.capabilities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

public class Capability {

    @JsonProperty("id")
    @Schema(description = "The clientIdentifier as known by the system")
    private final @NotNull String id;

    public Capability(@JsonProperty("id") final @NotNull String id) {
        this.id = id;
    }
}
