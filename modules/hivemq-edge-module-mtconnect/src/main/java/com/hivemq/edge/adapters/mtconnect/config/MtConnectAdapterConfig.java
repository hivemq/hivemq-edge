/*
 * Copyright 2023-present HiveMQ GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.hivemq.edge.adapters.mtconnect.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

@SuppressWarnings({"unused", "FieldCanBeLocal", "FieldMayBeFinal"})
public class MtConnectAdapterConfig implements ProtocolSpecificAdapterConfig {
    private static final boolean DEFAULT_ALLOW_UNTRUSTED_CERTIFICATES = false;
    private static final int DEFAULT_POLLING_INTERVAL_MILLIS = 1000;
    private static final int DEFAULT_HTTP_CONNECT_TIMEOUT_SECONDS = 5;
    private static final int MIN_HTTP_CONNECT_TIMEOUT_SECONDS = 1;
    private static final int MAX_HTTP_CONNECT_TIMEOUT_SECONDS = 60;
    private static final int DEFAULT_MAX_POLLING_ERRORS_BEFORE_REMOVAL = 10;
    private static final @NotNull String ID_REGEX = "^([a-zA-Z_0-9-_])*$";

    @JsonProperty("allowUntrustedCertificates")
    @ModuleConfigField(title = "Allow Untrusted Certificates",
                       description = "Allow the adapter to connect to untrusted SSL sources (for example expired certificates).",
                       defaultValue = "false",
                       format = ModuleConfigField.FieldType.BOOLEAN)
    private final boolean allowUntrustedCertificates;

    @JsonProperty("httpConnectTimeoutSeconds")
    @ModuleConfigField(title = "HTTP Connection Timeout",
                       description = "Timeout (in seconds) to allow the underlying HTTP connection to be established",
                       defaultValue = DEFAULT_HTTP_CONNECT_TIMEOUT_SECONDS + "",
                       numberMin = MIN_HTTP_CONNECT_TIMEOUT_SECONDS,
                       numberMax = MAX_HTTP_CONNECT_TIMEOUT_SECONDS)
    private final int httpConnectTimeoutSeconds;

    @JsonProperty(value = "id", required = true, access = JsonProperty.Access.WRITE_ONLY)
    @ModuleConfigField(title = "Identifier",
                       description = "Unique identifier for this protocol adapter",
                       format = ModuleConfigField.FieldType.IDENTIFIER,
                       required = true,
                       stringPattern = ID_REGEX,
                       stringMinLength = 1,
                       stringMaxLength = 1024)
    private @Nullable String id;

    @JsonProperty("pollingIntervalMillis")
    @ModuleConfigField(title = "Polling Interval [ms]",
                       description = "Time in millisecond that this endpoint will be polled",
                       numberMin = 1,
                       required = true,
                       defaultValue = "1000")
    private int pollingIntervalMillis = 1000;

    @JsonProperty("maxPollingErrorsBeforeRemoval")
    @ModuleConfigField(title = "Max. Polling Errors",
                       description = "Max. errors polling the endpoint before the polling daemon is stopped",
                       numberMin = 3,
                       defaultValue = "10")
    private int maxPollingErrorsBeforeRemoval = 10;

    public MtConnectAdapterConfig(
            @JsonProperty(value = "httpConnectTimeoutSeconds") final @Nullable Integer httpConnectTimeoutSeconds,
            @JsonProperty(value = "maxPollingErrorsBeforeRemoval") final @Nullable Integer maxPollingErrorsBeforeRemoval,
            @JsonProperty(value = "pollingIntervalMillis") final @Nullable Integer pollingIntervalMillis,
            @JsonProperty(value = "allowUntrustedCertificates") final @Nullable Boolean allowUntrustedCertificates) {
        this.allowUntrustedCertificates =
                Objects.requireNonNullElse(allowUntrustedCertificates, DEFAULT_ALLOW_UNTRUSTED_CERTIFICATES);
        this.httpConnectTimeoutSeconds = Optional.ofNullable(httpConnectTimeoutSeconds)
                .map(s -> Math.min(s, MAX_HTTP_CONNECT_TIMEOUT_SECONDS))
                .map(s -> Math.max(s, MIN_HTTP_CONNECT_TIMEOUT_SECONDS))
                .orElse(DEFAULT_HTTP_CONNECT_TIMEOUT_SECONDS);
        this.maxPollingErrorsBeforeRemoval =
                Objects.requireNonNullElse(maxPollingErrorsBeforeRemoval, DEFAULT_MAX_POLLING_ERRORS_BEFORE_REMOVAL);
        this.pollingIntervalMillis = Objects.requireNonNullElse(pollingIntervalMillis, DEFAULT_POLLING_INTERVAL_MILLIS);
    }

    public boolean isAllowUntrustedCertificates() {
        return allowUntrustedCertificates;
    }

    public int getHttpConnectTimeoutSeconds() {
        return httpConnectTimeoutSeconds;
    }

    public int getPollingIntervalMillis() {
        return pollingIntervalMillis;
    }

    public int getMaxPollingErrorsBeforeRemoval() {
        return maxPollingErrorsBeforeRemoval;
    }
}
