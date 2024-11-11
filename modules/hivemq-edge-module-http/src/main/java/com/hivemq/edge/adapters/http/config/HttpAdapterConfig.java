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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import com.hivemq.edge.adapters.http.config.http2mqtt.HttpToMqttConfig;
import com.hivemq.edge.adapters.http.config.http2mqtt.HttpToMqttMapping;
import com.hivemq.edge.adapters.http.tag.HttpTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hivemq.edge.adapters.http.HttpAdapterConstants.DEFAULT_TIMEOUT_SECONDS;
import static com.hivemq.edge.adapters.http.HttpAdapterConstants.MAX_TIMEOUT_SECONDS;
import static com.hivemq.edge.adapters.http.HttpAdapterConstants.MIN_TIMEOUT_SECONDS;

public class HttpAdapterConfig implements ProtocolAdapterConfig {

    private static final @NotNull String ID_REGEX = "^([a-zA-Z_0-9-_])*$";

    public static final @NotNull String HTML_MIME_TYPE = "text/html";
    public static final @NotNull String PLAIN_MIME_TYPE = "text/plain";
    public static final @NotNull String JSON_MIME_TYPE = "application/json";
    public static final @NotNull String XML_MIME_TYPE = "application/xml";
    public static final @NotNull String YAML_MIME_TYPE = "application/yaml";

    @JsonProperty(value = "id", required = true)
    @ModuleConfigField(title = "Identifier",
                       description = "Unique identifier for this protocol adapter",
                       format = ModuleConfigField.FieldType.IDENTIFIER,
                       required = true,
                       stringPattern = ID_REGEX,
                       stringMinLength = 1,
                       stringMaxLength = 1024)
    private final @NotNull String id;

    @JsonProperty("httpConnectTimeoutSeconds")
    @ModuleConfigField(title = "HTTP Connection Timeout",
                       description = "Timeout (in seconds) to allow the underlying HTTP connection to be established",
                       defaultValue = DEFAULT_TIMEOUT_SECONDS + "",
                       numberMin = MIN_TIMEOUT_SECONDS,
                       numberMax = MAX_TIMEOUT_SECONDS)
    private final int httpConnectTimeoutSeconds;

    @JsonProperty("allowUntrustedCertificates")
    @ModuleConfigField(title = "Allow Untrusted Certificates",
                       description = "Allow the adapter to connect to untrusted SSL sources (for example expired certificates).",
                       defaultValue = "false",
                       format = ModuleConfigField.FieldType.BOOLEAN)
    private final boolean allowUntrustedCertificates;

    @JsonProperty(value = "httpToMqtt")
    @ModuleConfigField(title = "HTTP To MQTT Config",
                       description = "The configuration for a data stream from HTTP to MQTT")
    private final @NotNull HttpToMqttConfig httpToMqttConfig;

    @JsonCreator
    public HttpAdapterConfig(
            @JsonProperty(value = "id", required = true) final @NotNull String id,
            @JsonProperty(value = "httpConnectTimeoutSeconds") final @Nullable Integer httpConnectTimeoutSeconds,
            @JsonProperty(value = "httpToMqtt") final @Nullable HttpToMqttConfig httpToMqttConfig,
            @JsonProperty(value = "allowUntrustedCertificates") final @Nullable Boolean allowUntrustedCertificates) {
        this.id = id;
        if (httpConnectTimeoutSeconds != null) {
            //-- Ensure we apply a reasonable timeout, so we don't hang threads
            this.httpConnectTimeoutSeconds = Math.min(httpConnectTimeoutSeconds, MAX_TIMEOUT_SECONDS);
        } else {
            this.httpConnectTimeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
        }
        this.httpToMqttConfig = Objects.requireNonNullElse(httpToMqttConfig, HttpToMqttConfig.DEFAULT);
        this.allowUntrustedCertificates = Objects.requireNonNullElse(allowUntrustedCertificates, false);
    }

    @Override
    public @NotNull String getId() {
        return id;
    }

    @Override
    public @NotNull Set<String> calculateAllUsedTags() {
        return httpToMqttConfig.getMappings().stream().map(HttpToMqttMapping::getTagName).collect(Collectors.toSet());
    }

    public int getHttpConnectTimeoutSeconds() {
        return httpConnectTimeoutSeconds;
    }

    public @NotNull HttpToMqttConfig getHttpToMqttConfig() {
        return httpToMqttConfig;
    }

    public boolean isAllowUntrustedCertificates() {
        return allowUntrustedCertificates;
    }

    public static class HttpHeader {

        @JsonProperty(value = "name", required = true)
        @ModuleConfigField(title = "Name", description = "The name of the HTTP header", required = true)
        private final @NotNull String name;

        @JsonProperty(value = "value", required = true)
        @ModuleConfigField(title = "Value", description = "The value of the HTTP header", required = true)
        private final @NotNull String value;

        @JsonCreator
        public HttpHeader(
                @JsonProperty(value = "name", required = true) final @NotNull String name,
                @JsonProperty(value = "value", required = true) final @NotNull String value) {
            this.name = name;
            this.value = value;
        }

        public @NotNull String getName() {
            return name;
        }

        public @NotNull String getValue() {
            return value;
        }
    }

    public enum HttpMethod {
        GET,
        POST,
        PUT
    }

    public enum HttpContentType {
        JSON(JSON_MIME_TYPE),
        PLAIN(PLAIN_MIME_TYPE),
        HTML(HTML_MIME_TYPE),
        XML(XML_MIME_TYPE),
        YAML(YAML_MIME_TYPE);

        HttpContentType(final @NotNull String mimeType) {
            this.mimeType = mimeType;
        }

        final @NotNull String mimeType;

        public @NotNull String getMimeType() {
            return mimeType;
        }
    }
}
