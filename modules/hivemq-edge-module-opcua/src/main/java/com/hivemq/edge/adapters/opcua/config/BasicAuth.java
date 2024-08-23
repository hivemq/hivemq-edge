package com.hivemq.edge.adapters.opcua.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.Nullable;

public class BasicAuth {

    @JsonProperty("username")
    @ModuleConfigField(title = "Username", description = "Username for basic authentication")
    private final @Nullable String username;

    @JsonProperty("password")
    @ModuleConfigField(title = "Password", description = "Password for basic authentication")
    private final @Nullable String password;

    @JsonCreator
    public BasicAuth(
            @JsonProperty("username") final @Nullable String username,
            @JsonProperty("password") final @Nullable String password) {
        this.username = username;
        this.password = password;
    }

    public @Nullable String getUsername() {
        return username;
    }

    public @Nullable String getPassword() {
        return password;
    }
}
