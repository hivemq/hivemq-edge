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
package com.hivemq.edge.adapters.http.v2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.NodeProperty;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The HTTP adapter's protocol {@link Node}: one HTTP(s) endpoint to poll, plus the per-tag request shaping carried
 * over verbatim from the v1 HTTP tag definition — the request method, per-request timeout, request body and its
 * content type, and any custom headers. A URL pins one endpoint, so a node is {@link NodeProperty#UNIQUE} (and
 * therefore {@link NodeProperty#TYPED}); it is not {@link NodeProperty#VALID}-checked until poll time.
 * <p>
 * The fields carry a Jackson creator and property annotations so the framework's own {@code ObjectMapper}
 * deserializes this node from its {@link #nodeString()} when an Edge runtime loads a configured HTTP adapter. The
 * per-request timeout is clamped to a sane ceiling and the method/content type default exactly as in v1. Node
 * correlation across the adapter boundary is by reference identity, so this class deliberately does not override
 * {@code equals}/{@code hashCode}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "url",
    "httpRequestMethod",
    "httpRequestTimeoutSeconds",
    "httpRequestBodyContentType",
    "httpRequestBody",
    "httpHeaders"
})
public final class HttpNode extends Node {

    static final int DEFAULT_TIMEOUT_SECONDS = 5;
    static final int MAX_TIMEOUT_SECONDS = 60;

    private static final @NotNull ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @JsonProperty("url")
    private final @NotNull String url;

    @JsonProperty("httpRequestMethod")
    private final @NotNull HttpMethod httpRequestMethod;

    @JsonProperty("httpRequestTimeoutSeconds")
    private final int httpRequestTimeoutSeconds;

    @JsonProperty("httpRequestBodyContentType")
    private final @NotNull HttpContentType httpRequestBodyContentType;

    @JsonProperty("httpRequestBody")
    private final @Nullable String httpRequestBody;

    @JsonProperty("httpHeaders")
    private final @NotNull List<HttpHeader> httpHeaders;

    /**
     * @param url                        the URL of the HTTP request.
     * @param httpRequestMethod          the request method; defaults to {@link HttpMethod#GET} when absent.
     * @param httpRequestTimeoutSeconds  the per-request timeout in seconds; clamped to a ceiling and defaulted when
     *                                   absent.
     * @param httpRequestBodyContentType the request body content type; defaults to {@link HttpContentType#JSON} when
     *                                   absent.
     * @param httpRequestBody            the request body, or {@code null} for none.
     * @param httpHeaders                the custom request headers; defaults to empty when absent.
     */
    @JsonCreator
    public HttpNode(
            @JsonProperty(value = "url", required = true) final @NotNull String url,
            @JsonProperty("httpRequestMethod") final @Nullable HttpMethod httpRequestMethod,
            @JsonProperty("httpRequestTimeoutSeconds") final @Nullable Integer httpRequestTimeoutSeconds,
            @JsonProperty("httpRequestBodyContentType") final @Nullable HttpContentType httpRequestBodyContentType,
            @JsonProperty("httpRequestBody") final @Nullable String httpRequestBody,
            @JsonProperty("httpHeaders") final @Nullable List<HttpHeader> httpHeaders) {
        this.url = url;
        this.httpRequestMethod = Objects.requireNonNullElse(httpRequestMethod, HttpMethod.GET);
        this.httpRequestBodyContentType = Objects.requireNonNullElse(httpRequestBodyContentType, HttpContentType.JSON);
        this.httpRequestBody = httpRequestBody;
        // Ensure a reasonable timeout so a hung endpoint cannot wedge a poll, exactly as the v1 adapter does.
        this.httpRequestTimeoutSeconds = httpRequestTimeoutSeconds != null
                ? Math.min(httpRequestTimeoutSeconds, MAX_TIMEOUT_SECONDS)
                : DEFAULT_TIMEOUT_SECONDS;
        this.httpHeaders = httpHeaders != null ? List.copyOf(httpHeaders) : List.of();
    }

    /**
     * @return the URL of the HTTP request.
     */
    public @NotNull String url() {
        return url;
    }

    /**
     * @return the request method.
     */
    public @NotNull HttpMethod httpRequestMethod() {
        return httpRequestMethod;
    }

    /**
     * @return the per-request timeout in seconds.
     */
    public int httpRequestTimeoutSeconds() {
        return httpRequestTimeoutSeconds;
    }

    /**
     * @return the request body content type.
     */
    public @NotNull HttpContentType httpRequestBodyContentType() {
        return httpRequestBodyContentType;
    }

    /**
     * @return the request body, or {@code null} for none.
     */
    public @Nullable String httpRequestBody() {
        return httpRequestBody;
    }

    /**
     * @return the custom request headers.
     */
    public @NotNull List<HttpHeader> httpHeaders() {
        return httpHeaders;
    }

    @Override
    public @NotNull String nodeId() {
        return url;
    }

    @Override
    public @NotNull String nodeString() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (final JsonProcessingException e) {
            throw new IllegalStateException(
                    "The http node could not be serialized to a node-string: " + e.getMessage());
        }
    }

    @Override
    public @NotNull EnumSet<NodeProperty> properties() {
        return EnumSet.of(NodeProperty.UNIQUE, NodeProperty.TYPED);
    }

    @Override
    public @NotNull String toString() {
        return httpRequestMethod + " " + url;
    }
}
