package com.hivemq.edge.adapters.file.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class FileToMqttConfig {

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
                       numberMin = 1,
                       defaultValue = "10")
    private final int maxPollingErrorsBeforeRemoval;

    @JsonProperty("fileToMqttMappings")
    @JsonSerialize(using = FilePollingContextSerializer.class)
    @ModuleConfigField(title = "File to MQTT Mappings", description = "Map your file to MQTT Topics")
    private final @NotNull List<FilePollingContext> mappings;

    @JsonCreator
    public FileToMqttConfig(
            @JsonProperty(value = "pollingIntervalMillis") final @Nullable Integer pollingIntervalMillis,
            @JsonProperty(value = "maxPollingErrorsBeforeRemoval") final @Nullable Integer maxPollingErrorsBeforeRemoval,
            @JsonProperty(value = "fileToMqttMappings") final @Nullable List<FilePollingContext> mappings) {
        this.pollingIntervalMillis = Objects.requireNonNullElse(pollingIntervalMillis, 1000);
        this.maxPollingErrorsBeforeRemoval = Objects.requireNonNullElse(maxPollingErrorsBeforeRemoval, 10);
        this.mappings = Objects.requireNonNullElse(mappings, List.of());
    }

    public int getPollingIntervalMillis() {
        return pollingIntervalMillis;
    }

    public int getMaxPollingErrorsBeforeRemoval() {
        return maxPollingErrorsBeforeRemoval;
    }

    public @NotNull List<FilePollingContext> getMappings() {
        return mappings;
    }
}
