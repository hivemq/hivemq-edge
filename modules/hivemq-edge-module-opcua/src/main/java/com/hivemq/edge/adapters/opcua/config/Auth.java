package com.hivemq.edge.adapters.opcua.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.Nullable;

public class Auth {

    @JsonProperty("basic")
    @ModuleConfigField(title = "Basic Authentication", description = "Username / password based authentication")
    private final @Nullable BasicAuth basicAuth;

    @JsonProperty("x509")
    @ModuleConfigField(title = "X509 Authentication", description = "Authentication based on certificate / private key")
    private final @Nullable X509Auth x509Auth;

    @JsonCreator
    public Auth(
            @JsonProperty("basic") final @Nullable BasicAuth basicAuth,
            @JsonProperty("x509") final @Nullable X509Auth x509Auth) {
        this.basicAuth = basicAuth;
        this.x509Auth = x509Auth;
    }

    public @Nullable BasicAuth getBasicAuth() {
        return basicAuth;
    }

    public @Nullable X509Auth getX509Auth() {
        return x509Auth;
    }
}
