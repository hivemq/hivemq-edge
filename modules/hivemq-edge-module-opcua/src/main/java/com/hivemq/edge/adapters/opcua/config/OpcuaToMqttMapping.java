package com.hivemq.edge.adapters.opcua.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNullElse;

public class OpcuaToMqttMapping {

    @JsonProperty("node")
    @ModuleConfigField(title = "Source Node ID",
                       description = "identifier of the node on the OPC-UA server. Example: \"ns=3;s=85/0:Temperature\"",
                       required = true)
    private final @NotNull String node;

    @JsonProperty("mqtt-topic")
    @ModuleConfigField(title = "Destination MQTT topic",
                       description = "The MQTT topic to publish to",
                       format = ModuleConfigField.FieldType.MQTT_TOPIC,
                       required = true)
    private final @NotNull String mqttTopic;

    @JsonProperty("publishing-interval")
    @ModuleConfigField(title = "OPC UA publishing interval [ms]",
                       description = "OPC UA publishing interval in milliseconds for this subscription on the server",
                       numberMin = 1,
                       defaultValue = "1000")
    private final int publishingInterval;

    @JsonProperty("server-queue-size")
    @ModuleConfigField(title = "OPC UA server queue size",
                       description = "OPC UA queue size for this subscription on the server",
                       numberMin = 1,
                       defaultValue = "1")
    private final int serverQueueSize;

    @JsonProperty("qos")
    @ModuleConfigField(title = "MQTT QoS",
                       description = "MQTT quality of service level",
                       numberMin = 0,
                       numberMax = 2,
                       defaultValue = "0")
    private final int qos;

    @JsonProperty("message-expiry-interval")
    @ModuleConfigField(title = "MQTT message expiry interval [s]",
                       description = "Time in seconds until a MQTT message expires",
                       numberMin = 1,
                       numberMax = 4294967295L)
    private final long messageExpiryInterval;

    @JsonCreator
    public OpcuaToMqttMapping(
            @JsonProperty(value = "node", required = true) final @NotNull String node,
            @JsonProperty(value = "mqtt-topic", required = true) final @NotNull String mqttTopic,
            @JsonProperty("publishing-interval") final @Nullable Integer publishingInterval,
            @JsonProperty("server-queue-size") final @Nullable Integer serverQueueSize,
            @JsonProperty("qos") final @Nullable Integer qos,
            @JsonProperty("message-expiry-interval") final @Nullable Long messageExpiryInterval) {
        this.node = node;
        this.mqttTopic = mqttTopic;
        this.publishingInterval = requireNonNullElse(publishingInterval, 1000);
        this.serverQueueSize = requireNonNullElse(serverQueueSize, 1);
        this.qos = requireNonNullElse(qos, 0);
        this.messageExpiryInterval = requireNonNullElse(messageExpiryInterval, 4294967295L);
    }

    public @NotNull String getNode() {
        return node;
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

    public int getQos() {
        return qos;
    }

    public long getMessageExpiryInterval() {
        return messageExpiryInterval;
    }
}
