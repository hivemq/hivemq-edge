package com.hivemq.edge.adapters.opcua.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;

public class X509Auth {

    @JsonProperty("enabled")
    @ModuleConfigField(title = "Enable X509", description = "Enables X509 auth")
    private final boolean enabled;

    @JsonCreator
    public X509Auth(@JsonProperty(value = "enabled", required = true) final boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
