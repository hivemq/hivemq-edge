package com.hivemq.api.model.bridge;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

public class WebsocketConfiguration {

    @JsonProperty("enabled")
    @Schema(description = "If Websockets are used")
    private final boolean enabled;

    @JsonProperty(value = "serverPath", defaultValue = "/mqtt")
    @Schema(description = "The server path from the config")
    private final @NotNull String serverPath;

    @JsonProperty(value = "subProtocol", defaultValue = "mqtt")
    @Schema(description = "The subProtocol from the config")
    private final @NotNull String subProtocol;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public WebsocketConfiguration(@JsonProperty("enabled") final boolean enabled,
                                  @JsonProperty("serverPath") final @NotNull String serverPath,
                                  @JsonProperty("subProtocol") final @NotNull String subProtocol) {
        this.enabled = enabled;
        this.serverPath = serverPath;
        this.subProtocol = subProtocol;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public @NotNull String getServerPath() {
        return serverPath;
    }

    public @NotNull String getSubProtocol() {
        return subProtocol;
    }
}
