package com.hivemq.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author Simon L Johnson
 */
public class TypeIdentifier {

    enum TYPE {
        BRIDGE, ADAPTER, ADAPTER_TYPE, EVENT, USER
    }

    @JsonProperty("type")
    @Schema(description = "The type of the associated object/entity",
            required = true)
    private final @NotNull TYPE type;

    @JsonProperty("identifier")
    @Schema(description = "The identifier associated with the object, a combination of type and identifier is used to uniquely identify an object in the system")
    private final @Nullable String identifier;

    public TypeIdentifier(@JsonProperty("type") final TYPE type, @JsonProperty("identifier") final String identifier) {
        this.type = type;
        this.identifier = identifier;
    }

    public TYPE getType() {
        return type;
    }

    public String getIdentifier() {
        return identifier;
    }
}
