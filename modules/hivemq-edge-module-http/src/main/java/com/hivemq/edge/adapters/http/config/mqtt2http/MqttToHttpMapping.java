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
package com.hivemq.edge.adapters.http.config.mqtt2http;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.writing.WritingContext;
import com.hivemq.edge.adapters.http.config.HttpSpecificAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static com.hivemq.edge.adapters.http.HttpAdapterConstants.DEFAULT_TIMEOUT_SECONDS;
import static com.hivemq.edge.adapters.http.HttpAdapterConstants.MAX_TIMEOUT_SECONDS;
import static com.hivemq.edge.adapters.http.HttpAdapterConstants.MIN_TIMEOUT_SECONDS;
import static com.hivemq.edge.adapters.http.config.HttpSpecificAdapterConfig.HttpMethod.POST;
import static java.util.Objects.requireNonNullElse;

public class MqttToHttpMapping implements WritingContext {

    @JsonProperty(value = "tagName", required = true)
    @ModuleConfigField(title = "tagName", description = "The name of the tag that holds the address data.",
                       format = ModuleConfigField.FieldType.URI,
                       required = true)
    private final @NotNull String tagName;


    @JsonProperty(value = "mqttTopicFilter", required = true)
    @ModuleConfigField(title = "Source MQTT topic filter",
                       description = "The MQTT topic filter to map from",
                       format = ModuleConfigField.FieldType.MQTT_TOPIC_FILTER,
                       required = true)
    private final @NotNull String mqttTopicFilter;

    @JsonProperty("mqttMaxQos")
    @ModuleConfigField(title = "MQTT Maximum QoS",
                       description = "MQTT quality of service level",
                       numberMin = 0,
                       numberMax = 1,
                       defaultValue = "1")
    private final int mqttMaxQos;

    @JsonProperty(value = "httpRequestMethod")
    @ModuleConfigField(title = "Http Method",
                       description = "Http method associated with the request",
                       defaultValue = "POST")
    private final @NotNull HttpSpecificAdapterConfig.HttpMethod httpRequestMethod;

    @JsonProperty(value = "httpRequestTimeoutSeconds")
    @ModuleConfigField(title = "Http Request Timeout",
                       description = "Timeout (in seconds) to wait for the HTTP Request to complete",
                       defaultValue = DEFAULT_TIMEOUT_SECONDS + "",
                       numberMin = MIN_TIMEOUT_SECONDS,
                       numberMax = MAX_TIMEOUT_SECONDS)
    private final int httpRequestTimeoutSeconds;

    @JsonProperty(value = "httpHeaders")
    @ModuleConfigField(title = "HTTP Headers", description = "HTTP headers to be added to your requests")
    private final @NotNull List<HttpSpecificAdapterConfig.HttpHeader> httpHeaders;

    @JsonCreator
    public MqttToHttpMapping(
            @JsonProperty(value = "tagName", required = true) final @NotNull String tagName,
            @JsonProperty(value = "mqttTopicFilter", required = true) final @NotNull String mqttTopicFilter,
            @JsonProperty(value = "mqttMaxQos") final @Nullable Integer mqttMaxQos,
            @JsonProperty(value = "httpRequestMethod") final @Nullable HttpSpecificAdapterConfig.HttpMethod httpRequestMethod,
            @JsonProperty(value = "httpRequestTimeoutSeconds") final @Nullable Integer httpRequestTimeoutSeconds,
            @JsonProperty(value = "httpHeaders") final @Nullable List<HttpSpecificAdapterConfig.HttpHeader> httpHeaders) {
        this.tagName = tagName;
        this.mqttTopicFilter = mqttTopicFilter;
        this.mqttMaxQos = requireNonNullElse(mqttMaxQos, 1);
        this.httpRequestMethod = Objects.requireNonNullElse(httpRequestMethod, POST);
        if (httpRequestTimeoutSeconds != null) {
            //-- Ensure we apply a reasonable timeout, so we don't hang threads
            this.httpRequestTimeoutSeconds = Math.min(httpRequestTimeoutSeconds, MAX_TIMEOUT_SECONDS);
        } else {
            this.httpRequestTimeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
        }
        this.httpHeaders = Objects.requireNonNullElseGet(httpHeaders, List::of);
    }

    @Override
    public @NotNull String getTagName() {
        return tagName;
    }

    public @NotNull String getTopicFilter() {
        return mqttTopicFilter;
    }

    public @NotNull HttpSpecificAdapterConfig.HttpMethod getHttpRequestMethod() {
        return httpRequestMethod;
    }

    public int getHttpRequestTimeoutSeconds() {
        return httpRequestTimeoutSeconds;
    }

    public @NotNull List<HttpSpecificAdapterConfig.HttpHeader> getHttpHeaders() {
        return httpHeaders;
    }
}
