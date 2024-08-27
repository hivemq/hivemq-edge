package com.hivemq.edge.adapters.etherip.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.config.UserProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.hivemq.adapter.sdk.api.config.MessageHandlingOptions.MQTTMessagePerTag;
import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;

@JsonPropertyOrder({"tagName", "tagAddress", "dataType", "mqttTopic", "mqttQos"})
public class EipPollingContext implements PollingContext {

    @JsonProperty(value = "tagName", required = true)
    @ModuleConfigField(title = "Tag Name",
                       description = "The name to assign to this address. The tag name must be unique for all subscriptions within this protocol adapter.",
                       required = true,
                       format = ModuleConfigField.FieldType.IDENTIFIER)
    private final @NotNull String tagName;

    @JsonProperty("tagAddress")
    @ModuleConfigField(title = "Tag Address",
                       description = "The well formed address of the tag to read",
                       required = true)
    private final @NotNull String tagAddress;

    @JsonProperty(value = "mqttTopic", required = true)
    @ModuleConfigField(title = "Destination Mqtt Topic",
                       description = "The topic to publish data on",
                       required = true,
                       format = ModuleConfigField.FieldType.MQTT_TOPIC)
    private final @NotNull String mqttTopic;

    @JsonProperty(value = "mqttQos", required = true)
    @ModuleConfigField(title = "QoS",
                       description = "MQTT Quality of Service level",
                       required = true,
                       numberMin = 0,
                       numberMax = 2,
                       defaultValue = "0")
    private final int qos;

    @JsonProperty(value = "messageHandlingOptions")
    @ModuleConfigField(title = "Message Handling Options",
                       description = "This setting defines the format of the resulting MQTT message, either a message per changed tag or a message per subscription that may include multiple data points per sample",
                       enumDisplayValues = {
                               "MQTT Message Per Device Tag",
                               "MQTT Message Per Subscription (Potentially Multiple Data Points Per Sample)"},
                       defaultValue = "MQTTMessagePerTag")
    private final @NotNull MessageHandlingOptions messageHandlingOptions;

    @JsonProperty(value = "includeTimestamp")
    @ModuleConfigField(title = "Include Sample Timestamp In Publish?",
                       description = "Include the unix timestamp of the sample time in the resulting MQTT message",
                       defaultValue = "true")
    private final @NotNull Boolean includeTimestamp;

    @JsonProperty(value = "includeTagNames")
    @ModuleConfigField(title = "Include Tag Names In Publish?",
                       description = "Include the names of the tags in the resulting MQTT publish",
                       defaultValue = "false")
    private final @NotNull Boolean includeTagNames;


    @JsonProperty("dataType")
    @ModuleConfigField(title = "Data Type", description = "The expected data type of the tag", enumDisplayValues = {
            "Bool",
            "DInt",
            "Int",
            "LInt",
            "LReal",
            "LTime",
            "Real",
            "SInt",
            "String",
            "Time",
            "UDInt",
            "UInt",
            "ULInt",
            "USInt"}, required = true)
    private final @NotNull EipDataType dataType;

    @JsonProperty(value = "userProperties")
    @ModuleConfigField(title = "User Properties",
                       description = "Arbitrary properties to associate with the subscription",
                       arrayMaxItems = 10)
    private final @NotNull List<UserProperty> userProperties;

    @JsonCreator
    public EipPollingContext(
            @JsonProperty(value = "mqttTopic", required = true) final @NotNull String mqttTopic,
            @JsonProperty(value = "mqttQos", required = true) final @Nullable Integer qos,
            @JsonProperty("messageHandlingOptions") final @Nullable MessageHandlingOptions messageHandlingOptions,
            @JsonProperty("includeTimestamp") final @Nullable Boolean includeTimestamp,
            @JsonProperty("includeTagNames") final @Nullable Boolean includeTagNames,
            @JsonProperty(value = "tagName", required = true) final @NotNull String tagName,
            @JsonProperty(value = "tagAddress", required = true) final @NotNull String tagAddress,
            @JsonProperty(value = "dataType", required = true) final @NotNull EipDataType dataType,
            @JsonProperty("userProperties") final @Nullable List<UserProperty> userProperties) {
        this.mqttTopic = mqttTopic;
        this.qos = requireNonNullElse(qos, 0);
        this.messageHandlingOptions = requireNonNullElse(messageHandlingOptions, MQTTMessagePerTag);
        this.includeTimestamp = requireNonNullElse(includeTimestamp, true);
        this.includeTagNames = requireNonNullElse(includeTagNames, false);
        this.tagName = tagName;
        this.tagAddress = tagAddress;
        this.dataType = dataType;
        this.userProperties = requireNonNullElseGet(userProperties, List::of);
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public @NotNull String getTagAddress() {
        return tagAddress;
    }

    public @NotNull EipDataType getDataType() {
        return dataType;
    }

    @Override
    public @NotNull String getMqttTopic() {
        return mqttTopic;
    }

    @Override
    public int getMqttQos() {
        return qos;
    }

    @Override
    public @NotNull MessageHandlingOptions getMessageHandlingOptions() {
        return messageHandlingOptions;
    }

    @Override
    public @NotNull Boolean getIncludeTimestamp() {
        return includeTimestamp;
    }

    @Override
    public @NotNull Boolean getIncludeTagNames() {
        return includeTagNames;
    }

    @Override
    public @NotNull List<UserProperty> getUserProperties() {
        return userProperties;
    }
}
