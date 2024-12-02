package com.hivemq.edge.adapters.http.tag;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import com.hivemq.adapter.sdk.api.tag.TagDefinition;
import com.hivemq.edge.adapters.http.config.HttpSpecificAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static com.hivemq.edge.adapters.http.HttpAdapterConstants.DEFAULT_TIMEOUT_SECONDS;
import static com.hivemq.edge.adapters.http.HttpAdapterConstants.MAX_TIMEOUT_SECONDS;
import static com.hivemq.edge.adapters.http.HttpAdapterConstants.MIN_TIMEOUT_SECONDS;
import static com.hivemq.edge.adapters.http.config.HttpSpecificAdapterConfig.HttpContentType.JSON;
import static com.hivemq.edge.adapters.http.config.HttpSpecificAdapterConfig.HttpMethod.GET;
import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;

public class HttpTagDefinition implements TagDefinition {

    @JsonProperty(value = "httpRequestMethod")
    @ModuleConfigField(title = "Http Method",
                       description = "Http method associated with the request",
                       defaultValue = "GET")
    private final @NotNull HttpSpecificAdapterConfig.HttpMethod httpRequestMethod;

    @JsonProperty(value = "httpRequestTimeoutSeconds")
    @ModuleConfigField(title = "Http Request Timeout",
                       description = "Timeout (in seconds) to wait for the HTTP Request to complete",
                       defaultValue = DEFAULT_TIMEOUT_SECONDS + "",
                       numberMin = MIN_TIMEOUT_SECONDS,
                       numberMax = MAX_TIMEOUT_SECONDS)
    private final int httpRequestTimeoutSeconds;

    @JsonProperty(value = "httpRequestBodyContentType")
    @ModuleConfigField(title = "Http Request Content Type",
                       description = "Content Type associated with the request",
                       defaultValue = "JSON")
    private final @NotNull HttpSpecificAdapterConfig.HttpContentType httpRequestBodyContentType;

    @JsonProperty(value = "httpRequestBody")
    @ModuleConfigField(title = "Http Request Body", description = "The body to include in the HTTP request")
    private final @Nullable String httpRequestBody;


    @JsonProperty(value = "httpHeaders")
    @ModuleConfigField(title = "HTTP Headers", description = "HTTP headers to be added to your requests")
    private final @NotNull List<HttpSpecificAdapterConfig.HttpHeader> httpHeaders;


    @JsonProperty(value = "url", required = true)
    @ModuleConfigField(title = "URL",
                       description = "The url of the HTTP request you would like to make",
                       format = ModuleConfigField.FieldType.URI,
                       required = true)
    private final @NotNull String url;

    @JsonCreator
    public HttpTagDefinition(@JsonProperty(value = "url", required = true) final @NotNull String url,
                             @JsonProperty(value = "httpRequestMethod") final @Nullable HttpSpecificAdapterConfig.HttpMethod httpRequestMethod,
                             @JsonProperty(value = "httpRequestTimeoutSeconds") final @Nullable Integer httpRequestTimeoutSeconds,
                             @JsonProperty(value = "httpRequestBodyContentType") final @Nullable HttpSpecificAdapterConfig.HttpContentType httpRequestBodyContentType,
                             @JsonProperty(value = "httpRequestBody") final @Nullable String httpRequestBody,
                             @JsonProperty(value = "httpHeaders") final @Nullable List<HttpSpecificAdapterConfig.HttpHeader> httpHeaders) {
        this.url = url;
        this.httpRequestMethod = Objects.requireNonNullElse(httpRequestMethod, GET);
        this.httpRequestBodyContentType = Objects.requireNonNullElse(httpRequestBodyContentType, JSON);
        this.httpRequestBody = httpRequestBody;
        if (httpRequestTimeoutSeconds != null) {
            //-- Ensure we apply a reasonable timeout, so we don't hang threads
            this.httpRequestTimeoutSeconds = Math.min(httpRequestTimeoutSeconds, MAX_TIMEOUT_SECONDS);
        } else {
            this.httpRequestTimeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
        }
        this.httpHeaders = Objects.requireNonNullElseGet(httpHeaders, List::of);
    }

    public @NotNull String getUrl() {
        return url;
    }



    public @NotNull HttpSpecificAdapterConfig.HttpMethod getHttpRequestMethod() {
        return httpRequestMethod;
    }

    public int getHttpRequestTimeoutSeconds() {
        return httpRequestTimeoutSeconds;
    }

    public @NotNull HttpSpecificAdapterConfig.HttpContentType getHttpRequestBodyContentType() {
        return httpRequestBodyContentType;
    }

    public @Nullable String getHttpRequestBody() {
        return httpRequestBody;
    }

    public @NotNull List<HttpSpecificAdapterConfig.HttpHeader> getHttpHeaders() {
        return httpHeaders;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final HttpTagDefinition that = (HttpTagDefinition) o;
        return httpRequestTimeoutSeconds == that.httpRequestTimeoutSeconds &&
                httpRequestMethod == that.httpRequestMethod &&
                httpRequestBodyContentType == that.httpRequestBodyContentType &&
                Objects.equals(httpRequestBody, that.httpRequestBody) &&
                Objects.equals(httpHeaders, that.httpHeaders) &&
                Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(httpRequestMethod,
                httpRequestTimeoutSeconds,
                httpRequestBodyContentType,
                httpRequestBody,
                httpHeaders,
                url);
    }

    @Override
    public String toString() {
        return "HttpTagDefinition{" +
                "httpRequestMethod=" +
                httpRequestMethod +
                ", httpRequestTimeoutSeconds=" +
                httpRequestTimeoutSeconds +
                ", httpRequestBodyContentType=" +
                httpRequestBodyContentType +
                ", httpRequestBody='" +
                httpRequestBody +
                '\'' +
                ", httpHeaders=" +
                httpHeaders +
                ", url='" +
                url +
                '\'' +
                '}';
    }
}
