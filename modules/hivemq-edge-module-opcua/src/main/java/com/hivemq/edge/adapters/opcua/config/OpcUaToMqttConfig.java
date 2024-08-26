package com.hivemq.edge.adapters.opcua.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class OpcUaToMqttConfig {

    @JsonProperty("opcuaToMqttMappings")
    private final @NotNull List<OpcUaToMqttMapping> subscriptions;

    @JsonCreator
    public OpcUaToMqttConfig(@JsonProperty("opcuaToMqttMappings") final @Nullable List<OpcUaToMqttMapping> subscriptions) {
        this.subscriptions = Objects.requireNonNullElse(subscriptions, List.of());
    }

    public @NotNull List<OpcUaToMqttMapping> getMappings() {
        return subscriptions;
    }
}
