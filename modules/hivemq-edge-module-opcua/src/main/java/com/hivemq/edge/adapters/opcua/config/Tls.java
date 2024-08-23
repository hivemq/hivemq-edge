package com.hivemq.edge.adapters.opcua.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class Tls {

    @JsonProperty("enabled")
    @ModuleConfigField(title = "Enable TLS", description = "Enables TLS encrypted connection", defaultValue = "true")
    private final boolean enabled;

    @JsonProperty("keystore")
    @ModuleConfigField(title = "Keystore",
                       description = "Keystore that contains the client certificate including the chain. Required for X509 authentication.")
    private final @Nullable Keystore keystore;

    @JsonProperty("truststore")
    @ModuleConfigField(title = "Truststore",
                       description = "Truststore wich contains the trusted server certificates or trusted intermediates.")
    private final @Nullable Truststore truststore;

    @JsonCreator
    public Tls(
            @JsonProperty("enabled") final @Nullable Boolean enabled,
            @JsonProperty("keystore") final @Nullable Keystore keystore,
            @JsonProperty("truststore") final @Nullable Truststore truststore) {
        this.enabled = Objects.requireNonNullElse(enabled, true);
        this.keystore = keystore;
        this.truststore = truststore;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public @Nullable Keystore getKeystore() {
        return keystore;
    }

    public @Nullable Truststore getTruststore() {
        return truststore;
    }
}
