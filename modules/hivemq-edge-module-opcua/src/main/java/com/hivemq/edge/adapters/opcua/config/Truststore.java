package com.hivemq.edge.adapters.opcua.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.NotNull;

public class Truststore {

    @JsonProperty("path")
    @ModuleConfigField(title = "Truststore path", description = "Path on the local file system to the truststore.")
    private final @NotNull String path;

    @JsonProperty("password")
    @ModuleConfigField(title = "Truststore password", description = "Password to open the truststore.")
    private final @NotNull String password;

    public Truststore(
            @JsonProperty(value = "path", required = true) final @NotNull String path,
            @JsonProperty(value = "password", required = true) final @NotNull String password) {
        this.path = path;
        this.password = password;
    }

    public @NotNull String getPath() {
        return path;
    }

    public @NotNull String getPassword() {
        return password;
    }
}
