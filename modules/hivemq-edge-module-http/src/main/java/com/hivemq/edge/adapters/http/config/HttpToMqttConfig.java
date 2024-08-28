package com.hivemq.edge.adapters.http.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class HttpToMqttConfig {

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
                       numberMin = 3,
                       defaultValue = "10")
    private final int maxPollingErrorsBeforeRemoval;

    @JsonProperty("allowUntrustedCertificates")
    @ModuleConfigField(title = "Allow Untrusted Certificates",
                       description = "Allow the adapter to read from untrusted SSL sources (for example expired certificates).",
                       defaultValue = "false",
                       format = ModuleConfigField.FieldType.BOOLEAN)
    private final boolean allowUntrustedCertificates;

    @JsonProperty("assertResponseIsJson")
    @ModuleConfigField(title = "Assert JSON Response?",
                       description = "Always attempt to parse the body of the response as JSON data, regardless of the Content-Type on the response.",
                       defaultValue = "false",
                       format = ModuleConfigField.FieldType.BOOLEAN)
    private final boolean assertResponseIsJson;

    @JsonProperty("httpPublishSuccessStatusCodeOnly")
    @ModuleConfigField(title = "Publish Only On Success Codes",
                       description = "Only publish data when HTTP response code is successful ( 200 - 299 )",
                       defaultValue = "true",
                       format = ModuleConfigField.FieldType.BOOLEAN)
    private final boolean httpPublishSuccessStatusCodeOnly;

    @JsonProperty("httpToMqttMappings")
    @JsonSerialize(using = HttpPollingContextSerializer.class)
    @ModuleConfigField(title = "HTTP to MQTT Mappings", description = "Map your sensor data to MQTT Topics")
    private final @NotNull List<HttpPollingContext> mappings;

    @JsonCreator
    public HttpToMqttConfig(
            @JsonProperty(value = "pollingIntervalMillis") final @Nullable Integer pollingIntervalMillis,
            @JsonProperty(value = "maxPollingErrorsBeforeRemoval") final @Nullable Integer maxPollingErrorsBeforeRemoval,
            @JsonProperty(value = "allowUntrustedCertificates") final @Nullable Boolean allowUntrustedCertificates,
            @JsonProperty(value = "assertResponseIsJson") final @Nullable Boolean assertResponseIsJson,
            @JsonProperty(value = "httpPublishSuccessStatusCodeOnly") final @Nullable Boolean httpPublishSuccessStatusCodeOnly,
            @JsonProperty(value = "httpToMqttMappings") final @Nullable List<HttpPollingContext> mappings) {
        this.mappings = Objects.requireNonNullElse(mappings, List.of());
        this.pollingIntervalMillis = Objects.requireNonNullElse(pollingIntervalMillis, 1000);
        this.maxPollingErrorsBeforeRemoval = Objects.requireNonNullElse(maxPollingErrorsBeforeRemoval, 10);
        this.allowUntrustedCertificates = Objects.requireNonNullElse(allowUntrustedCertificates, false);
        this.assertResponseIsJson = Objects.requireNonNullElse(assertResponseIsJson, false);
        this.httpPublishSuccessStatusCodeOnly = Objects.requireNonNullElse(httpPublishSuccessStatusCodeOnly, true);
    }

    public int getPollingIntervalMillis() {
        return pollingIntervalMillis;
    }

    public int getMaxPollingErrorsBeforeRemoval() {
        return maxPollingErrorsBeforeRemoval;
    }

    public boolean isAllowUntrustedCertificates() {
        return allowUntrustedCertificates;
    }

    public boolean isAssertResponseIsJson() {
        return assertResponseIsJson;
    }

    public boolean isHttpPublishSuccessStatusCodeOnly() {
        return httpPublishSuccessStatusCodeOnly;
    }

    public @NotNull List<HttpPollingContext> getMappings() {
        return mappings;
    }
}
