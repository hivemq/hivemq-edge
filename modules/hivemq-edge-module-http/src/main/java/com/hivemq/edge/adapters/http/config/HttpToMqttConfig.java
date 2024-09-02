/*
 * Copyright 2023-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    @ModuleConfigField(title = "Polling Interval [ms]",
                       description = "Time in millisecond that this endpoint will be polled",
                       numberMin = 1,
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
    @JsonSerialize(using = HttpToMqttMappingSerializer.class)
    @ModuleConfigField(title = "HTTP to MQTT Mappings", description = "Map your sensor data to MQTT Topics")
    private final @NotNull List<HttpToMqttMapping> mappings;

    @JsonCreator
    public HttpToMqttConfig(
            @JsonProperty(value = "pollingIntervalMillis") final @Nullable Integer pollingIntervalMillis,
            @JsonProperty(value = "maxPollingErrorsBeforeRemoval") final @Nullable Integer maxPollingErrorsBeforeRemoval,
            @JsonProperty(value = "allowUntrustedCertificates") final @Nullable Boolean allowUntrustedCertificates,
            @JsonProperty(value = "assertResponseIsJson") final @Nullable Boolean assertResponseIsJson,
            @JsonProperty(value = "httpPublishSuccessStatusCodeOnly") final @Nullable Boolean httpPublishSuccessStatusCodeOnly,
            @JsonProperty(value = "httpToMqttMappings") final @Nullable List<HttpToMqttMapping> mappings) {
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

    public @NotNull List<HttpToMqttMapping> getMappings() {
        return mappings;
    }
}
