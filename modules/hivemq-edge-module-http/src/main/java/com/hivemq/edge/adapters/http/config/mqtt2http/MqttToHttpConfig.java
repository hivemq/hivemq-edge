package com.hivemq.edge.adapters.http.config.mqtt2http;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class MqttToHttpConfig {

    private final @NotNull List<MqttToHttpMapping> mappings;

    public MqttToHttpConfig(final @Nullable List<MqttToHttpMapping> mappings) {
        this.mappings = Objects.requireNonNullElse(mappings, List.of());
    }

    public @NotNull List<MqttToHttpMapping> getMappings() {
        return mappings;
    }
}
