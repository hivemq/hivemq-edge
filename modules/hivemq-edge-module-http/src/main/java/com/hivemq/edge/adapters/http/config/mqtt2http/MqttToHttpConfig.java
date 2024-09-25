package com.hivemq.edge.adapters.http.config.mqtt2http;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class MqttToHttpConfig {

    @JsonProperty("mqttToHttpMappings")
    @ModuleConfigField(title = "MQTT to HTTP Mappings", description = "Map your HTTP data to MQTT Topics")
    private final @NotNull List<MqttToHttpMapping> mappings;

    @JsonCreator
    public MqttToHttpConfig(
            @JsonProperty(value = "mqttToHttpMappings") final @Nullable List<MqttToHttpMapping> mappings) {
        this.mappings = Objects.requireNonNullElse(mappings, List.of());
    }

    public @NotNull List<MqttToHttpMapping> getMappings() {
        return mappings;
    }
}
