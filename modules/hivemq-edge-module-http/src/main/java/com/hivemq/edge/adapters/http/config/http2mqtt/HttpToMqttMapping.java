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
package com.hivemq.edge.adapters.http.config.http2mqtt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.edge.adapters.http.config.HttpSpecificAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static com.hivemq.adapter.sdk.api.config.MessageHandlingOptions.MQTTMessagePerTag;
import static com.hivemq.edge.adapters.http.HttpAdapterConstants.DEFAULT_TIMEOUT_SECONDS;
import static com.hivemq.edge.adapters.http.HttpAdapterConstants.MAX_TIMEOUT_SECONDS;
import static com.hivemq.edge.adapters.http.HttpAdapterConstants.MIN_TIMEOUT_SECONDS;
import static com.hivemq.edge.adapters.http.config.HttpSpecificAdapterConfig.HttpContentType.JSON;
import static com.hivemq.edge.adapters.http.config.HttpSpecificAdapterConfig.HttpMethod.GET;
import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;

public class HttpToMqttMapping implements PollingContext {

    @JsonProperty(value = "tagName", required = true)
    @ModuleConfigField(title = "tagName", description = "The name of the tag that holds the address data.",
                       format = ModuleConfigField.FieldType.URI,
                       required = true)
    private final @NotNull String tagName;

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

    @JsonCreator
    public HttpToMqttMapping(
            @JsonProperty(value = "tagName", required = true) final @NotNull String tagName,
            @JsonProperty(value = "mqttTopic", required = true) final @NotNull String mqttTopic,
            @JsonProperty(value = "mqttQos") final @Nullable Integer mqttQos,
            @JsonProperty(value = "mqttUserProperties") final @Nullable List<MqttUserProperty> userProperties,
            @JsonProperty(value = "includeTimestamp") final @Nullable Boolean includeTimestamp) {
        this.tagName = tagName;
        this.mqttTopic = mqttTopic;
        this.mqttQos = requireNonNullElse(mqttQos, 0);
        this.userProperties = requireNonNullElseGet(userProperties, List::of);
        this.includeTimestamp = requireNonNullElse(includeTimestamp, true);
    }

    public @NotNull String getTagName() {
        return tagName;
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

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final HttpToMqttMapping that = (HttpToMqttMapping) o;
        return getMqttQos() == that.getMqttQos() &&
                getIncludeTimestamp() == that.getIncludeTimestamp() &&
                Objects.equals(getTagName(), that.getTagName()) &&
                Objects.equals(getMqttTopic(), that.getMqttTopic()) &&
                Objects.equals(getUserProperties(), that.getUserProperties());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTagName(), getMqttTopic(), getMqttQos(), getUserProperties(), getIncludeTimestamp());
    }
}
