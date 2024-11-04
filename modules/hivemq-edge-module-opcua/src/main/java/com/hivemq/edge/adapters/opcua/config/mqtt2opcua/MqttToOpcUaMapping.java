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
package com.hivemq.edge.adapters.opcua.config.mqtt2opcua;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.writing.WritingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNullElse;

public class MqttToOpcUaMapping implements WritingContext {

    @JsonProperty(value = "tagName", required = true)
    @ModuleConfigField(title = "Tag name of the Destination",
                       description = "identifier of the node on the OPC UA server. Example: \"ns=3;s=85/0:Temperature\"",
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
                       description = "MQTT maximum quality of service level for the subscription",
                       numberMin = 0,
                       numberMax = 1,
                       defaultValue = "1")
    private final int qos;

    @JsonCreator
    public MqttToOpcUaMapping(
            @JsonProperty(value = "tagName", required = true) final @NotNull String tagName,
            @JsonProperty(value = "mqttTopicFilter", required = true) final @NotNull String mqttTopicFilter,
            @JsonProperty("mqttMaxQos") final @Nullable Integer qos) {
        this.tagName = tagName;
        this.mqttTopicFilter = mqttTopicFilter;
        this.qos = requireNonNullElse(qos, 1);
    }

    public @NotNull String getMqttTopicFilter() {
        return mqttTopicFilter;
    }

    @Override
    public int getMqttMaxQos() {
        return qos;
    }

    @Override
    public @NotNull String getTagName() {
        return tagName;
    }

}
