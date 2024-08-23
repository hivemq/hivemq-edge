package com.hivemq.edge.adapters.opcua.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class Security {

    @JsonProperty("policy")
    @ModuleConfigField(title = "OPC UA security policy",
                       description = "Security policy to use for communication with the server.")
    private final @NotNull SecPolicy policy;

    public Security(@JsonProperty("policy") final @Nullable SecPolicy policy) {
        this.policy = Objects.requireNonNullElse(policy, SecPolicy.DEFAULT);
    }

    public @NotNull SecPolicy getPolicy() {
        return policy;
    }
}
