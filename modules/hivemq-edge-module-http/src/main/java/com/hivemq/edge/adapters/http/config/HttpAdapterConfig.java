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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.hivemq.edge.adapters.http.config.HttpAdapterConfig.HttpContentType.JSON;
import static com.hivemq.edge.adapters.http.config.HttpAdapterConfig.HttpMethod.GET;
import static com.hivemq.edge.adapters.http.HttpAdapterConstants.DEFAULT_TIMEOUT_SECONDS;
import static com.hivemq.edge.adapters.http.HttpAdapterConstants.MAX_TIMEOUT_SECONDS;

@JsonPropertyOrder({"id", "url", "httpConnectTimeout", "httpToMqtt"})
public class HttpAdapterConfig implements ProtocolAdapterConfig {

    private static final @NotNull String ID_REGEX = "^([a-zA-Z_0-9-_])*$";

    public static final @NotNull String HTML_MIME_TYPE = "text/html";
    public static final @NotNull String PLAIN_MIME_TYPE = "text/plain";
    public static final @NotNull String JSON_MIME_TYPE = "application/json";
    public static final @NotNull String XML_MIME_TYPE = "application/xml";
    public static final @NotNull String YAML_MIME_TYPE = "application/yaml";

    @JsonProperty(value = "id")
    @ModuleConfigField(title = "Identifier",
                       description = "Unique identifier for this protocol adapter",
                       format = ModuleConfigField.FieldType.IDENTIFIER,
                       required = true,
                       stringPattern = ID_REGEX,
                       stringMinLength = 1,
                       stringMaxLength = 1024)
    private final @NotNull String id;

    @JsonProperty("url")
    @ModuleConfigField(title = "URL",
                       description = "The url of the HTTP request you would like to make",
                       format = ModuleConfigField.FieldType.URI,
                       required = true)
    private final @NotNull String url;

    @JsonProperty("httpConnectTimeout")
    @ModuleConfigField(title = "HTTP Connection Timeout",
                       description = "Timeout (in seconds) to allow the underlying HTTP connection to be established",
                       defaultValue = DEFAULT_TIMEOUT_SECONDS + "")
    private final int httpConnectTimeoutSeconds;

    @JsonProperty("httpToMqtt")
    @ModuleConfigField(title = "HTTP To MQTT Config",
                       description = "The configuration for a data stream from HTTP to MQTT",
                       required = true)
    private final @NotNull HttpToMqttConfig httpToMqttConfig;

    @JsonCreator
    public HttpAdapterConfig(
            @JsonProperty(value = "id", required = true) final @NotNull String id,
            @JsonProperty(value = "url", required = true) final @NotNull String url,
            @JsonProperty(value = "httpConnectTimeout") final @Nullable Integer httpConnectTimeoutSeconds,
            @JsonProperty(value = "httpToMqtt", required = true) final @NotNull HttpToMqttConfig httpToMqttConfig) {
        this.id = id;
        this.url = url;
        if (httpConnectTimeoutSeconds != null) {
            //-- Ensure we apply a reasonable timeout, so we don't hang threads
            this.httpConnectTimeoutSeconds = Math.max(httpConnectTimeoutSeconds, MAX_TIMEOUT_SECONDS);
        } else {
            this.httpConnectTimeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
        }
        this.httpToMqttConfig = httpToMqttConfig;
    }

    @Override
    public @NotNull String getId() {
        return id;
    }

    public @NotNull String getUrl() {
        return url;
    }

    public int getHttpConnectTimeoutSeconds() {
        return httpConnectTimeoutSeconds;
    }

    public @NotNull HttpToMqttConfig getHttpToMqttConfig() {
        return httpToMqttConfig;
    }

    public static class HttpHeader {

        @JsonProperty("name")
        @ModuleConfigField(title = "Name", description = "The name of the HTTP header")
        private final @NotNull String name;

        @JsonProperty("value")
        @ModuleConfigField(title = "Value", description = "The value of the HTTP header")
        private final @NotNull String value;

        @JsonCreator
        public HttpHeader(
                @JsonProperty("name") final @NotNull String name, @JsonProperty("value") final @NotNull String value) {
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

        HttpContentType(final @NotNull String contentType) {
            this.contentType = contentType;
        }

        final @NotNull String contentType;

        public @NotNull String getContentType() {
            return contentType;
        }
    }
}
