package com.hivemq.edge.adapters.opcua.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.NotNull;

public class Keystore {

    @JsonProperty("path")
    @ModuleConfigField(title = "Keystore path", description = "Path on the local file system to the keystore.")
    private final @NotNull String path;

    @JsonProperty("password")
    @ModuleConfigField(title = "Keystore password", description = "Password to open the keystore.")
    private final @NotNull String password;

    @JsonProperty("private-key-password")
    @ModuleConfigField(title = "Private key password", description = "Password to access the private key.")
    private final @NotNull String privateKeyPassword;

    public Keystore(
            @JsonProperty(value = "path", required = true) final @NotNull String path,
            @JsonProperty(value = "password", required = true) final @NotNull String password,
            @JsonProperty(value = "private-key-password", required = true) final @NotNull String privateKeyPassword) {
        this.path = path;
        this.password = password;
        this.privateKeyPassword = privateKeyPassword;
    }

    public @NotNull String getPath() {
        return path;
    }

    public @NotNull String getPassword() {
        return password;
    }

    public @NotNull String getPrivateKeyPassword() {
        return privateKeyPassword;
    }
}
