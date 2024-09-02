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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static com.hivemq.adapter.sdk.api.config.MessageHandlingOptions.MQTTMessagePerTag;
import static com.hivemq.edge.adapters.http.HttpAdapterConstants.DEFAULT_TIMEOUT_SECONDS;
import static com.hivemq.edge.adapters.http.HttpAdapterConstants.MAX_TIMEOUT_SECONDS;
import static com.hivemq.edge.adapters.http.HttpAdapterConstants.MIN_TIMEOUT_SECONDS;
import static com.hivemq.edge.adapters.http.config.HttpAdapterConfig.HttpContentType.JSON;
import static com.hivemq.edge.adapters.http.config.HttpAdapterConfig.HttpMethod.GET;
import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;

public class HttpToMqttMapping implements PollingContext {

    @JsonProperty(value = "mqttTopic", required = true)
    @ModuleConfigField(title = "Destination MQTT Topic",
                       description = "The topic to publish data on",
                       required = true,
                       format = ModuleConfigField.FieldType.MQTT_TOPIC)
    private final @NotNull String mqttTopic;

    @JsonProperty(value = "mqttQos")
    @ModuleConfigField(title = "MQTT QoS",
                       description = "MQTT Quality of Service level",
                       numberMin = 0,
                       numberMax = 2,
                       defaultValue = "0")
    private final int mqttQos;

    @JsonProperty(value = "mqttUserProperties")
    @ModuleConfigField(title = "MQTT User Properties",
                       description = "Arbitrary properties to associate with the mapping",
                       arrayMaxItems = 10)
    private final @NotNull List<MqttUserProperty> userProperties;

    @JsonProperty(value = "includeTimestamp")
    @ModuleConfigField(title = "Include Sample Timestamp In Publish?",
                       description = "Include the unix timestamp of the sample time in the resulting MQTT message",
                       defaultValue = "true",
                       format = ModuleConfigField.FieldType.BOOLEAN)
    private final boolean includeTimestamp;

    @JsonProperty(value = "httpRequestMethod")
    @ModuleConfigField(title = "Http Method",
                       description = "Http method associated with the request",
                       defaultValue = "GET")
    private final @NotNull HttpAdapterConfig.HttpMethod httpRequestMethod;

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
    private final @NotNull HttpAdapterConfig.HttpContentType httpRequestBodyContentType;

    @JsonProperty(value = "httpRequestBody")
    @ModuleConfigField(title = "Http Request Body", description = "The body to include in the HTTP request")
    private final @Nullable String httpRequestBody;


    @JsonProperty(value = "httpHeaders")
    @ModuleConfigField(title = "HTTP Headers", description = "HTTP headers to be added to your requests")
    private final @NotNull List<HttpAdapterConfig.HttpHeader> httpHeaders;

    @JsonCreator
    public HttpToMqttMapping(
            @JsonProperty(value = "mqttTopic", required = true) final @NotNull String mqttTopic,
            @JsonProperty(value = "mqttQos") final @Nullable Integer mqttQos,
            @JsonProperty(value = "mqttUserProperties") final @Nullable List<MqttUserProperty> userProperties,
            @JsonProperty(value = "includeTimestamp") final @Nullable Boolean includeTimestamp,
            @JsonProperty(value = "httpRequestMethod") final @Nullable HttpAdapterConfig.HttpMethod httpRequestMethod,
            @JsonProperty(value = "httpRequestTimeoutSeconds") final @Nullable Integer httpRequestTimeoutSeconds,
            @JsonProperty(value = "httpRequestBodyContentType") final @Nullable HttpAdapterConfig.HttpContentType httpRequestBodyContentType,
            @JsonProperty(value = "httpRequestBody") final @Nullable String httpRequestBody,
            @JsonProperty(value = "httpHeaders") final @Nullable List<HttpAdapterConfig.HttpHeader> httpHeaders) {
        this.mqttTopic = mqttTopic;
        this.mqttQos = requireNonNullElse(mqttQos, 0);
        this.userProperties = requireNonNullElseGet(userProperties, List::of);
        this.includeTimestamp = requireNonNullElse(includeTimestamp, true);
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

    @Override
    public @NotNull String getMqttTopic() {
        return mqttTopic;
    }

    @Override
    public int getMqttQos() {
        return mqttQos;
    }

    @Override
    @JsonIgnore
    public @NotNull MessageHandlingOptions getMessageHandlingOptions() {
        return MQTTMessagePerTag;
    }

    @Override
    public @NotNull Boolean getIncludeTimestamp() {
        return includeTimestamp;
    }

    @Override
    @JsonIgnore
    public @NotNull Boolean getIncludeTagNames() {
        return false;
    }

    @Override
    public @NotNull List<MqttUserProperty> getUserProperties() {
        return userProperties;
    }

    public @NotNull HttpAdapterConfig.HttpMethod getHttpRequestMethod() {
        return httpRequestMethod;
    }

    public int getHttpRequestTimeoutSeconds() {
        return httpRequestTimeoutSeconds;
    }

    public @NotNull HttpAdapterConfig.HttpContentType getHttpRequestBodyContentType() {
        return httpRequestBodyContentType;
    }

    public @Nullable String getHttpRequestBody() {
        return httpRequestBody;
    }

    public @NotNull List<HttpAdapterConfig.HttpHeader> getHttpHeaders() {
        return httpHeaders;
    }
}
