package com.hivemq.edge.adapters.opcua.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class OpcuaToMqttConfig {

    @JsonProperty("opcuaToMqttMappings")
    private final @NotNull List<OpcuaToMqttMapping> subscriptions;

    @JsonCreator
    public OpcuaToMqttConfig(@JsonProperty("opcuaToMqttMappings") final @Nullable List<OpcuaToMqttMapping> subscriptions) {
        this.subscriptions = Objects.requireNonNullElse(subscriptions, List.of());
    }

    public @NotNull List<OpcuaToMqttMapping> getMappings() {
        return subscriptions;
    }
}
