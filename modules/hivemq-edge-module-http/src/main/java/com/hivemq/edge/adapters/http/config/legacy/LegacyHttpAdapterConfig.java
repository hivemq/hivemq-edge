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
package com.hivemq.edge.adapters.http.config.legacy;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.edge.adapters.http.config.HttpAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static com.hivemq.edge.adapters.http.HttpAdapterConstants.DEFAULT_TIMEOUT_SECONDS;
import static com.hivemq.edge.adapters.http.HttpAdapterConstants.MAX_TIMEOUT_SECONDS;

public class LegacyHttpAdapterConfig {

    @JsonProperty("url")
    private final @NotNull String url;

    @JsonProperty(value = "destination")
    private final @NotNull String destination;

    @JsonProperty(value = "qos")
    private final int qos;

    @JsonProperty("httpRequestMethod")
    private final @NotNull HttpAdapterConfig.HttpMethod httpRequestMethod;

    @JsonProperty("httpConnectTimeout")
    private final int httpConnectTimeoutSeconds;

    @JsonProperty("httpRequestBodyContentType")
    private final @NotNull HttpAdapterConfig.HttpContentType httpRequestBodyContentType;

    @JsonProperty("httpRequestBody")
    private final @Nullable String httpRequestBody;

    @JsonProperty("assertResponseIsJson")
    private final boolean assertResponseIsJson;

    @JsonProperty("httpPublishSuccessStatusCodeOnly")
    private final boolean httpPublishSuccessStatusCodeOnly;

    @JsonProperty("httpHeaders")
    private final @NotNull List<HttpAdapterConfig.HttpHeader> httpHeaders;

    @JsonProperty(value = "id")
    private final @NotNull String id;

    @JsonProperty("maxPollingErrorsBeforeRemoval")
    private final int maxPollingErrorsBeforeRemoval;

    @JsonProperty("allowUntrustedCertificates")
    private final boolean allowUntrustedCertificates;

    @JsonProperty("pollingIntervalMillis")
    @JsonAlias(value = "publishingInterval") //-- Ensure we cater for properties created with legacy configuration
    private final int pollingIntervalMillis;

    @JsonCreator
    public LegacyHttpAdapterConfig(
            @JsonProperty(value = "url", required = true) final @NotNull String url,
            @JsonProperty(value = "destination", required = true) final @NotNull String destination,
            @JsonProperty("qos") final @Nullable Integer qos,
            @JsonProperty("httpRequestMethod") final @Nullable HttpAdapterConfig.HttpMethod httpRequestMethod,
            @JsonProperty("httpConnectTimeout") final @Nullable Integer httpConnectTimeoutSeconds,
            @JsonProperty("httpRequestBodyContentType") final @Nullable HttpAdapterConfig.HttpContentType httpRequestBodyContentType,
            @JsonProperty("httpRequestBody") final @Nullable String httpRequestBody,
            @JsonProperty("assertResponseIsJson") final @Nullable Boolean assertResponseIsJson,
            @JsonProperty("httpPublishSuccessStatusCodeOnly") final @Nullable Boolean httpPublishSuccessStatusCodeOnly,
            @JsonProperty("httpHeaders") final @Nullable List<HttpAdapterConfig.HttpHeader> httpHeaders,
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
        this.httpRequestMethod = Objects.requireNonNullElse(httpRequestMethod, HttpAdapterConfig.HttpMethod.GET);
        this.httpRequestBodyContentType = Objects.requireNonNullElse(httpRequestBodyContentType, HttpAdapterConfig.HttpContentType.JSON);
        this.httpRequestBody = httpRequestBody;
        if (httpConnectTimeoutSeconds != null) {
            //-- Ensure we apply a reasonable timeout, so we don't hang threads
            this.httpConnectTimeoutSeconds = Math.min(httpConnectTimeoutSeconds, MAX_TIMEOUT_SECONDS);
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

    public @NotNull HttpAdapterConfig.HttpMethod getHttpRequestMethod() {
        return httpRequestMethod;
    }

    public @NotNull List<HttpAdapterConfig.HttpHeader> getHttpHeaders() {
        return httpHeaders;
    }

    public @NotNull HttpAdapterConfig.HttpContentType getHttpRequestBodyContentType() {
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

    public @NotNull String getId() {
        return id;
    }

    public int getPollingIntervalMillis() {
        return pollingIntervalMillis;
    }

    public int getMaxPollingErrorsBeforeRemoval() {
        return maxPollingErrorsBeforeRemoval;
    }


}
