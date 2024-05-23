package com.hivemq.edge.modules.config.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.edge.modules.adapters.annotations.ModuleConfigField;
import com.hivemq.edge.modules.adapters.config.AdapterSubscription;
import com.hivemq.edge.modules.adapters.config.MessageHandlingOptions;
import com.hivemq.edge.modules.adapters.config.UserProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AdapterSubscriptionImpl implements AdapterSubscription {

    @JsonProperty(value = "destination", required = true)
    @ModuleConfigField(title = "Destination Topic",
                       description = "The topic to publish data on",
                       required = true,
                       format = ModuleConfigField.FieldType.MQTT_TOPIC)
    protected @Nullable String destination;

    @JsonProperty(value = "qos", required = true)
    @ModuleConfigField(title = "QoS",
                       description = "MQTT Quality of Service level",
                       required = true,
                       numberMin = 0,
                       numberMax = 2,
                       defaultValue = "0")
    protected int qos = 0;

    @JsonProperty(value = "messageHandlingOptions")
    @ModuleConfigField(title = "Message Handling Options",
                       description = "This setting defines the format of the resulting MQTT message, either a message per changed tag or a message per subscription that may include multiple data points per sample",
                       enumDisplayValues = {
                               "MQTT Message Per Device Tag",
                               "MQTT Message Per Subscription (Potentially Multiple Data Points Per Sample)"},
                       defaultValue = "MQTTMessagePerTag")
    protected @NotNull MessageHandlingOptions messageHandlingOptions = MessageHandlingOptions.MQTTMessagePerTag;

    @JsonProperty(value = "includeTimestamp")
    @ModuleConfigField(title = "Include Sample Timestamp In Publish?",
                       description = "Include the unix timestamp of the sample time in the resulting MQTT message",
                       defaultValue = "true")
    protected @NotNull Boolean includeTimestamp = Boolean.TRUE;

    @JsonProperty(value = "includeTagNames")
    @ModuleConfigField(title = "Include Tag Names In Publish?",
                       description = "Include the names of the tags in the resulting MQTT publish",
                       defaultValue = "false")
    protected @NotNull Boolean includeTagNames = Boolean.FALSE;

    @JsonProperty(value = "userProperties")
    @ModuleConfigField(title = "User Properties",
                       description = "Arbitrary properties to associate with the subscription",
                       arrayMaxItems = 10)
    private @NotNull List<UserProperty> userProperties = new ArrayList<>();

    public AdapterSubscriptionImpl() {
    }

    @JsonCreator
    public AdapterSubscriptionImpl(
            @JsonProperty("destination") @Nullable final String destination,
            @JsonProperty("qos") final int qos,
            @JsonProperty("userProperties") @Nullable List<UserProperty> userProperties) {
        this.destination = destination;
        this.qos = qos;
        if (userProperties != null) {
            this.userProperties = userProperties;
        }
    }

    @Override
    public @Nullable String getDestination() {
        return destination;
    }

    @Override
    public int getQos() {
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
