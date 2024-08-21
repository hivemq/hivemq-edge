package com.hivemq.edge.adapters.modbus.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * @since 4.9.0
 */
public class ModbusToMQTTConfig {

    @JsonProperty("pollingIntervalMillis")
    @JsonAlias(value = "publishingInterval") //-- Ensure we cater for properties created with legacy configuration
    @ModuleConfigField(title = "Polling Interval [ms]",
                       description = "Time in millisecond that this endpoint will be polled",
                       numberMin = 1,
                       required = true,
                       defaultValue = "1000")
    private final int pollingIntervalMillis;

    @JsonProperty("maxPollingErrorsBeforeRemoval")
    @ModuleConfigField(title = "Max. Polling Errors",
                       description = "Max. errors polling the endpoint before the polling daemon is stopped",
                       defaultValue = "10")
    private final int maxPollingErrorsBeforeRemoval;

    @JsonProperty("publishChangedDataOnly")
    @ModuleConfigField(title = "Only publish data items that have changed since last poll",
                       defaultValue = "true",
                       format = ModuleConfigField.FieldType.BOOLEAN)
    private final boolean publishChangedDataOnly;

    @JsonProperty("modbusToMqttMappings")
    @JsonSerialize(using = PollingContextSerializer.class)
    @ModuleConfigField(title = "Modbus to MQTT Mappings", description = "Map your sensor data to MQTT Topics")
    private final @NotNull List<PollingContextImpl> mappings;

    @JsonCreator
    public ModbusToMQTTConfig(
            @JsonProperty(value = "pollingIntervalMillis") final @Nullable Integer pollingIntervalMillis,
            @JsonProperty(value = "maxPollingErrorsBeforeRemoval") final @Nullable Integer maxPollingErrorsBeforeRemoval,
            @JsonProperty(value = "publishChangedDataOnly") final @Nullable Boolean publishChangedDataOnly,
            @JsonProperty(value = "modbusToMqttMappings") final @Nullable List<PollingContextImpl> mappings) {
        this.pollingIntervalMillis = Objects.requireNonNullElse(pollingIntervalMillis, 1000);
        this.maxPollingErrorsBeforeRemoval = Objects.requireNonNullElse(maxPollingErrorsBeforeRemoval, 10);
        this.publishChangedDataOnly = Objects.requireNonNullElse(publishChangedDataOnly, true);
        this.mappings = Objects.requireNonNullElse(mappings, List.of());
    }

    public int getPollingIntervalMillis() {
        return pollingIntervalMillis;
    }

    public int getMaxPollingErrorsBeforeRemoval() {
        return maxPollingErrorsBeforeRemoval;
    }

    public boolean getPublishChangedDataOnly() {
        return publishChangedDataOnly;
    }

    public @NotNull List<PollingContextImpl> getMappings() {
        return mappings;
    }
}
