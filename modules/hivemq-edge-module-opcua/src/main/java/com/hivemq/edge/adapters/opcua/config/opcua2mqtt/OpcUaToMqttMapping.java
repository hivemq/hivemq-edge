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
package com.hivemq.edge.adapters.opcua.config.opcua2mqtt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNullElse;

public class OpcUaToMqttMapping {


    @JsonProperty(value = "tagName", required = true)
    @ModuleConfigField(title = "tagName",
                       description = "The name of the tag that holds the address data.",
                       format = ModuleConfigField.FieldType.URI,
                       required = true)
    private final @NotNull String tagName;

    @JsonProperty(value = "mqttTopic", required = true)
    @ModuleConfigField(title = "Destination MQTT topic",
                       description = "The MQTT topic to publish to",
                       format = ModuleConfigField.FieldType.MQTT_TOPIC,
                       required = true)
    private final @NotNull String mqttTopic;

    @JsonProperty("publishingInterval")
    @ModuleConfigField(title = "OPC UA publishing interval [ms]",
                       description = "OPC UA publishing interval in milliseconds for this subscription on the server",
                       numberMin = 1,
                       defaultValue = "1000")
    private final int publishingInterval;

    @JsonProperty("serverQueueSize")
    @ModuleConfigField(title = "OPC UA server queue size",
                       description = "OPC UA queue size for this subscription on the server",
                       numberMin = 1,
                       defaultValue = "1")
    private final int serverQueueSize;

    @JsonProperty("mqttQos")
    @ModuleConfigField(title = "MQTT QoS",
                       description = "MQTT quality of service level",
                       numberMin = 0,
                       numberMax = 2,
                       defaultValue = "0")
    private final int qos;

    @JsonProperty("messageExpiryInterval")
    @ModuleConfigField(title = "MQTT message expiry interval [s]",
                       description = "Time in seconds until an MQTT publish message expires",
                       numberMin = 1,
                       numberMax = 4294967295L)
    private final long messageExpiryInterval;

    @JsonCreator
    public OpcUaToMqttMapping(
            @JsonProperty(value = "tagName", required = true) final @NotNull String tagName,
            @JsonProperty(value = "mqttTopic", required = true) final @NotNull String mqttTopic,
            @JsonProperty("publishingInterval") final @Nullable Integer publishingInterval,
            @JsonProperty("serverQueueSize") final @Nullable Integer serverQueueSize,
            @JsonProperty("mqttQos") final @Nullable Integer qos,
            @JsonProperty("messageExpiryInterval") final @Nullable Long messageExpiryInterval) {
        this.tagName = tagName;
        this.mqttTopic = mqttTopic;
        this.publishingInterval = requireNonNullElse(publishingInterval, 1000);
        this.serverQueueSize = requireNonNullElse(serverQueueSize, 1);
        this.qos = requireNonNullElse(qos, 0);
        this.messageExpiryInterval = requireNonNullElse(messageExpiryInterval, 4294967295L);
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public @NotNull String getMqttTopic() {
        return mqttTopic;
    }

    public int getPublishingInterval() {
        return publishingInterval;
    }

    public int getServerQueueSize() {
        return serverQueueSize;
    }

    public int getMqttMaxQos() {
        return qos;
    }

    public long getMessageExpiryInterval() {
        return messageExpiryInterval;
    }
}
