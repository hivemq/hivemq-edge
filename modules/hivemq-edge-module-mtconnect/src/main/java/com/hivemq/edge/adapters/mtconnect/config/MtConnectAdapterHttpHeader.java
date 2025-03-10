package com.hivemq.edge.adapters.mtconnect.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.NotNull;

public class MtConnectAdapterHttpHeader {
    @JsonProperty(value = "name", required = true)
    @ModuleConfigField(title = "Name", description = "The name of the HTTP header", required = true)
    private final @NotNull String name;

    @JsonProperty(value = "value", required = true)
    @ModuleConfigField(title = "Value", description = "The value of the HTTP header", required = true)
    private final @NotNull String value;

    @JsonCreator
    public MtConnectAdapterHttpHeader(
            @JsonProperty(value = "name", required = true) final @NotNull String name,
            @JsonProperty(value = "value", required = true) final @NotNull String value) {
        this.name = name;
        this.value = value;
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull String getValue() {
        return value;
    }
}
