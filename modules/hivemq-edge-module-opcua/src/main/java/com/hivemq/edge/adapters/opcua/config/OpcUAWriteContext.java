package com.hivemq.edge.adapters.opcua.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.WriteContext;
import com.hivemq.edge.adapters.opcua.writing.OpcUaValueType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("ALL")
public class OpcUAWriteContext implements WriteContext {

    @JsonProperty(value = "source", required = true)
    @ModuleConfigField(title = "Source Topic",
                       description = "The topic from which the data are received.",
                       required = true,
                       format = ModuleConfigField.FieldType.MQTT_TOPIC)
    protected @Nullable String source;

    @JsonProperty(value = "qos", required = true)
    @ModuleConfigField(title = "QoS",
                       description = "MQTT Quality of Service level",
                       required = true,
                       numberMin = 0,
                       numberMax = 2,
                       defaultValue = "0")
    protected int qos = 0;

    @JsonProperty("node")
    @ModuleConfigField(title = "Destination Node ID",
                       description = "identifier of the node on the OPC-UA server. Example: \"ns=3;s=85/0:Temperature\"",
                       required = true)
    private @NotNull String destination = "";

    @JsonProperty("type")
    @ModuleConfigField(title = "The type of the attribute",
                       description = "The data type of the attribute that should be written",
                       required = true)
    private @NotNull OpcUaValueType type;

    @Override
    public @Nullable String getSourceMqttTopic() {
        return source;
    }

    @Override
    public int getQos() {
        return qos;
    }

    public @NotNull String getDestination() {
        return destination;
    }

    public @NotNull OpcUaValueType getType() {
        return type;
    }
}
