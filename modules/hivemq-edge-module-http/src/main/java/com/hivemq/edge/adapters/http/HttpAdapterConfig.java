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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static com.hivemq.edge.adapters.http.HttpAdapterConfig.HttpContentType.JSON;
import static com.hivemq.edge.adapters.http.HttpAdapterConfig.HttpMethod.GET;
import static com.hivemq.edge.adapters.http.HttpAdapterConstants.DEFAULT_TIMEOUT_SECONDS;
import static com.hivemq.edge.adapters.http.HttpAdapterConstants.MAX_TIMEOUT_SECONDS;

@JsonPropertyOrder({
        "url",
        "destination",
        "qos",
        "httpRequestMethod",
        "httpConnectTimeout",
        "httpRequestBodyContentType",
        "httpRequestBody",
        "assertResponseIsJson",
        "httpPublishSuccessStatusCodeOnly",
        "httpHeaders",
        "id",
        "maxPollingErrorsBeforeRemoval",
        "allowUntrustedCertificates",
        "pollingIntervalMillis"})
public class HttpAdapterConfig implements ProtocolAdapterConfig {

    private static final @NotNull String ID_REGEX = "^([a-zA-Z_0-9-_])*$";

    public static final @NotNull String HTML_MIME_TYPE = "text/html";
    public static final @NotNull String PLAIN_MIME_TYPE = "text/plain";
    public static final @NotNull String JSON_MIME_TYPE = "application/json";
    public static final @NotNull String XML_MIME_TYPE = "application/xml";
    public static final @NotNull String YAML_MIME_TYPE = "application/yaml";

    @JsonProperty("url")
    @ModuleConfigField(title = "URL",
                       description = "The url of the http request you would like to make",
                       format = ModuleConfigField.FieldType.URI,
                       required = true)
    private final @NotNull String url;

    @JsonProperty(value = "destination", required = true)
    @ModuleConfigField(title = "Destination Topic",
                       description = "The topic to publish data on",
                       required = true,
                       format = ModuleConfigField.FieldType.MQTT_TOPIC)
    private final @NotNull String destination;

    @JsonProperty(value = "qos", required = true)
    @ModuleConfigField(title = "QoS",
                       description = "MQTT Quality of Service level",
                       required = true,
                       numberMin = 0,
                       numberMax = 2,
                       defaultValue = "0")
    private final int qos;

    @JsonProperty("httpRequestMethod")
    @ModuleConfigField(title = "Http Method",
                       description = "Http method associated with the request",
                       defaultValue = "GET")
    private final @NotNull HttpAdapterConfig.HttpMethod httpRequestMethod;

    @JsonProperty("httpConnectTimeout")
    @ModuleConfigField(title = "Http Connection Timeout",
                       description = "Timeout (in second) to wait for the HTTP Request to complete",
                       required = true,
                       defaultValue = DEFAULT_TIMEOUT_SECONDS + "")
    private final int httpConnectTimeoutSeconds;

    @JsonProperty("httpRequestBodyContentType")
    @ModuleConfigField(title = "Http Request Content Type",
                       description = "Content Type associated with the request",
                       defaultValue = "JSON")
    private final @NotNull HttpAdapterConfig.HttpContentType httpRequestBodyContentType;

    @JsonProperty("httpRequestBody")
    @ModuleConfigField(title = "Http Request Body", description = "The body to include in the HTTP request")
    private final @Nullable String httpRequestBody;

    @JsonProperty("assertResponseIsJson")
    @ModuleConfigField(title = "Assert JSON Response?",
                       description = "Always attempt to parse the body of the response as JSON data, regardless of the Content-Type on the response.",
                       defaultValue = "false",
                       format = ModuleConfigField.FieldType.BOOLEAN)
    private final boolean assertResponseIsJson;

    @JsonProperty("httpPublishSuccessStatusCodeOnly")
    @ModuleConfigField(title = "Only publish data when HTTP response code is successful ( 200 - 299 )",
                       defaultValue = "true",
                       format = ModuleConfigField.FieldType.BOOLEAN)
    private final boolean httpPublishSuccessStatusCodeOnly;

    @JsonProperty("httpHeaders")
    @ModuleConfigField(title = "HTTP Headers", description = "HTTP headers to be added to your requests")
    private final @NotNull List<HttpHeader> httpHeaders;

    @JsonProperty(value = "id", required = true)
    @ModuleConfigField(title = "Identifier",
                       description = "Unique identifier for this protocol adapter",
                       format = ModuleConfigField.FieldType.IDENTIFIER,
                       required = true,
                       stringPattern = ID_REGEX,
                       stringMinLength = 1,
                       stringMaxLength = 1024)
    private final @NotNull String id;

    @JsonProperty("maxPollingErrorsBeforeRemoval")
    @ModuleConfigField(title = "Max. Polling Errors",
                       description = "Max. errors polling the endpoint before the polling daemon is stopped",
                       numberMin = 3,
                       defaultValue = "10")
    private final int maxPollingErrorsBeforeRemoval;

    @JsonProperty("allowUntrustedCertificates")
    @ModuleConfigField(title = "Allow the adapter to read from untrusted SSL sources (for example expired certificates).",
                       defaultValue = "false",
                       format = ModuleConfigField.FieldType.BOOLEAN)
    private final boolean allowUntrustedCertificates;

    @JsonProperty("pollingIntervalMillis")
    @JsonAlias(value = "publishingInterval") //-- Ensure we cater for properties created with legacy configuration
    @ModuleConfigField(title = "Polling Interval [ms]",
                       description = "Time in millisecond that this endpoint will be polled",
                       numberMin = 1,
                       required = true,
                       defaultValue = "1000")
    private final int pollingIntervalMillis;

    @JsonCreator
    public HttpAdapterConfig(
            @JsonProperty(value = "url", required = true) final @NotNull String url,
            @JsonProperty(value = "destination", required = true) final @NotNull String destination,
            @JsonProperty("qos") final @Nullable Integer qos,
            @JsonProperty("httpRequestMethod") final @Nullable HttpMethod httpRequestMethod,
            @JsonProperty("httpConnectTimeout") final @Nullable Integer httpConnectTimeoutSeconds,
            @JsonProperty("httpRequestBodyContentType") final @Nullable HttpContentType httpRequestBodyContentType,
            @JsonProperty("httpRequestBody") final @Nullable String httpRequestBody,
            @JsonProperty("assertResponseIsJson") final @Nullable Boolean assertResponseIsJson,
            @JsonProperty("httpPublishSuccessStatusCodeOnly") final @Nullable Boolean httpPublishSuccessStatusCodeOnly,
            @JsonProperty("httpHeaders") final @Nullable List<HttpHeader> httpHeaders,
            @JsonProperty(value = "id", required = true) final @NotNull String id,
            @JsonProperty("maxPollingErrorsBeforeRemoval") final @Nullable Integer maxPollingErrorsBeforeRemoval,
            @JsonProperty("allowUntrustedCertificates") final @Nullable Boolean allowUntrustedCertificates,
            @JsonProperty("pollingIntervalMillis") @JsonAlias("publishingInterval") final @Nullable Integer pollingIntervalMillis) {
        this.id = id;
        if (pollingIntervalMillis != null) {
            this.pollingIntervalMillis = pollingIntervalMillis;
        } else {
            this.pollingIntervalMillis = 1000;
        }
        this.maxPollingErrorsBeforeRemoval = Objects.requireNonNullElse(maxPollingErrorsBeforeRemoval, 10);
        this.url = url;
        this.destination = destination;
        this.qos = Objects.requireNonNullElse(qos, 0);
        this.httpRequestMethod = Objects.requireNonNullElse(httpRequestMethod, GET);
        this.httpRequestBodyContentType = Objects.requireNonNullElse(httpRequestBodyContentType, JSON);
        this.httpRequestBody = httpRequestBody;
        if (httpConnectTimeoutSeconds != null) {
            //-- Ensure we apply a reasonable timeout, so we don't hang threads
            this.httpConnectTimeoutSeconds = Math.max(httpConnectTimeoutSeconds, MAX_TIMEOUT_SECONDS);
        } else {
            this.httpConnectTimeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
        }
        this.httpHeaders = Objects.requireNonNullElseGet(httpHeaders, List::of);
        this.httpPublishSuccessStatusCodeOnly = Objects.requireNonNullElse(httpPublishSuccessStatusCodeOnly, true);
        this.allowUntrustedCertificates = Objects.requireNonNullElse(allowUntrustedCertificates, false);
        this.assertResponseIsJson = Objects.requireNonNullElse(assertResponseIsJson, false);
    }

    public boolean isHttpPublishSuccessStatusCodeOnly() {
        return httpPublishSuccessStatusCodeOnly;
    }

    public boolean isAssertResponseIsJson() {
        return assertResponseIsJson;
    }

    public @NotNull HttpMethod getHttpRequestMethod() {
        return httpRequestMethod;
    }

    public @NotNull List<HttpHeader> getHttpHeaders() {
        return httpHeaders;
    }

    public @NotNull HttpContentType getHttpRequestBodyContentType() {
        return httpRequestBodyContentType;
    }

    public @Nullable String getHttpRequestBody() {
        return httpRequestBody;
    }

    public int getHttpConnectTimeoutSeconds() {
        return httpConnectTimeoutSeconds;
    }

    public @NotNull String getUrl() {
        return url;
    }

    public @NotNull String getDestination() {
        return destination;
    }

    public int getQos() {
        return qos;
    }

    public boolean isAllowUntrustedCertificates() {
        return allowUntrustedCertificates;
    }

    @Override
    public @NotNull String getId() {
        return id;
    }

    public int getPollingIntervalMillis() {
        return pollingIntervalMillis;
    }

    public int getMaxPollingErrorsBeforeRemoval() {
        return maxPollingErrorsBeforeRemoval;
    }

    public static class HttpHeader {

        @JsonProperty("name")
        @ModuleConfigField(title = "Http Header Name", description = "The name of the HTTP header")
        private final @NotNull String name;

        @JsonProperty("value")
        @ModuleConfigField(title = "Http Header Value", description = "The value of the HTTP header")
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
