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
package com.hivemq.edge.adapters.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hivemq.edge.modules.adapters.annotations.ModuleConfigField;
import com.hivemq.edge.modules.config.impl.AbstractPollingProtocolAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.http.core.HttpConstants;

import java.util.ArrayList;
import java.util.List;

@JsonPropertyOrder({
        "url",
        "destination",
        "qos",
        "httpRequestMethod",
        "httpConnectTimeout",
        "httpRequestBodyContentType",
        "httpRequestBody",
        "httpPublishSuccessStatusCodeOnly",
        "httpHeaders"})
public class HttpAdapterConfig extends AbstractPollingProtocolAdapterConfig {

    public enum HttpMethod {
        GET,
        POST,
        PUT
    }

    public enum HttpContentType {
        JSON(HttpConstants.JSON_MIME_TYPE),
        PLAIN(HttpConstants.PLAIN_MIME_TYPE),
        HTML(HttpConstants.HTML_MIME_TYPE),
        XML("application/xml"),
        YAML("application/yaml");

        HttpContentType(String contentType) {
            this.contentType = contentType;
        }

        final String contentType;

        public String getContentType() {
            return contentType;
        }
    }

    @JsonProperty("url")
    @ModuleConfigField(title = "URL", description = "The url of the http request you would like to make",
//                       stringPattern = HttpConstants.HTTP_URL_REGEX,
                       format = ModuleConfigField.FieldType.URI, required = true)
    private @NotNull String url;

    @JsonProperty(value = "destination", required = true)
    @ModuleConfigField(title = "Destination Topic",
                       description = "The topic to publish data on",
                       required = true,
                       format = ModuleConfigField.FieldType.MQTT_TOPIC)
    private @Nullable String destination;

    @JsonProperty(value = "qos", required = true)
    @ModuleConfigField(title = "QoS",
                       description = "MQTT Quality of Service level",
                       required = true,
                       numberMin = 0,
                       numberMax = 2,
                       defaultValue = "0")
    private int qos = 0;

    @JsonProperty("httpRequestMethod")
    @ModuleConfigField(title = "Http Method",
                       description = "Http method associated with the request",
                       defaultValue = "GET")
    private @NotNull HttpAdapterConfig.HttpMethod httpRequestMethod = HttpAdapterConfig.HttpMethod.GET;

    @JsonProperty("httpRequestBodyContentType")
    @ModuleConfigField(title = "Http Request Content Type",
                       description = "Content Type associated with the request",
                       defaultValue = "JSON")
    private @NotNull HttpAdapterConfig.HttpContentType httpRequestBodyContentType = HttpContentType.JSON;

    @JsonProperty("httpRequestBody")
    @ModuleConfigField(title = "Http Request Body", description = "The body to include in the HTTP request")
    private @NotNull String httpRequestBody;

    @JsonProperty("httpConnectTimeout")
    @ModuleConfigField(title = "Http Connection Timeout",
                       description = "Timeout (in second) to wait for the HTTP Request to complete",
                       required = true,
                       defaultValue = HttpAdapterConstants.DEFAULT_TIMEOUT_SECONDS + "")
    private @NotNull Integer httpConnectTimeout = HttpAdapterConstants.DEFAULT_TIMEOUT_SECONDS;

    @JsonProperty("httpHeaders")
    @ModuleConfigField(title = "HTTP Headers", description = "HTTP headers to be added to your requests")
    private @NotNull List<HttpHeader> httpHeaders = new ArrayList<>();

    @JsonProperty("httpPublishSuccessStatusCodeOnly")
    @ModuleConfigField(title = "Only publish data when HTTP response code is successful ( 200 - 299 )",
                       defaultValue = "true",
                       format = ModuleConfigField.FieldType.BOOLEAN)
    private boolean httpPublishSuccessStatusCodeOnly = true;

    @JsonProperty("allowUntrustedCertificates")
    @ModuleConfigField(title = "Allow the adapter to read from untrusted SSL sources (for example expired certificates).",
                       defaultValue = "false",
                       format = ModuleConfigField.FieldType.BOOLEAN)
    private boolean allowUntrustedCertificates = false;

    public HttpAdapterConfig() {
    }

    public HttpAdapterConfig(final @NotNull String adapterId) {
        this.id = adapterId;
    }

    public boolean isHttpPublishSuccessStatusCodeOnly() {
        return httpPublishSuccessStatusCodeOnly;
    }

    public @NotNull HttpMethod getHttpRequestMethod() {
        return httpRequestMethod;
    }

    public @NotNull List<HttpHeader> getHttpHeaders() {
        return httpHeaders;
    }

    public HttpContentType getHttpRequestBodyContentType() {
        return httpRequestBodyContentType;
    }

    public String getHttpRequestBody() {
        return httpRequestBody;
    }

    public Integer getHttpConnectTimeout() {
        return httpConnectTimeout;
    }

    public @NotNull String getUrl() {
        return url;
    }

    public String getDestination() {
        return destination;
    }

    public int getQos() {
        return qos;
    }

    public boolean isAllowUntrustedCertificates() {
        return allowUntrustedCertificates;
    }

    public static class HttpHeader {

        @JsonProperty("name")
        @ModuleConfigField(title = "Http Header Name", description = "The name of the HTTP header")
        private String name;

        @JsonProperty("value")
        @ModuleConfigField(title = "Http Header Value", description = "The value of the HTTP header")
        private String value;

        public HttpHeader() {
        }

        public HttpHeader(@NotNull final String name, @NotNull final String value) {
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
}
