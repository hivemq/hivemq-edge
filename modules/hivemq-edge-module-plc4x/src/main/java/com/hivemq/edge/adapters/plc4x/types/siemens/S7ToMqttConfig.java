package com.hivemq.edge.adapters.plc4x.types.siemens;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.edge.adapters.plc4x.model.Plc4xPollingContext;
import com.hivemq.edge.adapters.plc4x.model.Plc4xToMqttConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class S7ToMqttConfig extends Plc4xToMqttConfig {

    @JsonProperty("s7ToMqttMappings")
    @JsonSerialize(using = S7PollingContextSerializer.class)
    @ModuleConfigField(title = "S7 to MQTT Mappings", description = "Map your sensor data to MQTT Topics")
    private final @NotNull List<Plc4xPollingContext> mappings;

    @JsonCreator
    public S7ToMqttConfig(
            @JsonProperty(value = "pollingIntervalMillis") final @Nullable Integer pollingIntervalMillis,
            @JsonProperty(value = "maxPollingErrorsBeforeRemoval") final @Nullable Integer maxPollingErrorsBeforeRemoval,
            @JsonProperty(value = "publishChangedDataOnly") final @Nullable Boolean publishChangedDataOnly,
            @JsonProperty(value = "s7ToMqttMappings") final @Nullable List<Plc4xPollingContext> mappings) {
        super(pollingIntervalMillis, maxPollingErrorsBeforeRemoval, publishChangedDataOnly);
        this.mappings = Objects.requireNonNullElse(mappings, List.of());
    }

    public @NotNull List<Plc4xPollingContext> getMappings() {
        return mappings;
    }
}
