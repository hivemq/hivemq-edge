package com.hivemq.edge.adapters.modbus.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.config.UserProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static com.hivemq.adapter.sdk.api.config.MessageHandlingOptions.MQTTMessagePerSubscription;
import static java.util.Objects.*;

public class PollingContextImpl implements PollingContext {

    @JsonProperty(value = "destination", required = true)
    @ModuleConfigField(title = "Destination Topic",
                       description = "The topic to publish data on",
                       required = true,
                       format = ModuleConfigField.FieldType.MQTT_TOPIC)
    private final @NotNull String destination;

    @JsonProperty(value = "qos", required = true)
    @ModuleConfigField(title = "QoS",
                       description = "MQTT Quality of Service level",
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
    private final boolean includeTimestamp;

    @JsonProperty(value = "includeTagNames")
    @ModuleConfigField(title = "Include Tag Names In Publish?",
                       description = "Include the names of the tags in the resulting MQTT publish",
                       defaultValue = "false")
    private final boolean includeTagNames;

    @JsonProperty(value = "userProperties")
    @ModuleConfigField(title = "User Properties",
                       description = "Arbitrary properties to associate with the subscription",
                       arrayMaxItems = 10)
    private final @NotNull List<UserProperty> userProperties;

    @JsonProperty("addressRange")
    @JsonAlias("holding-registers")
    @ModuleConfigField(title = "Holding Registers",
                       description = "Define the start and end index values for your memory addresses")
    private final @NotNull AddressRange addressRange;

    @JsonCreator
    public PollingContextImpl(
            @JsonProperty(value = "destination", required = true) final @NotNull String destination,
            @JsonProperty(value = "qos") final @Nullable Integer qos,
            @JsonProperty(value = "messageHandlingOptions") final @Nullable MessageHandlingOptions messageHandlingOptions,
            @JsonProperty(value = "includeTimestamp") final @Nullable Boolean includeTimestamp,
            @JsonProperty(value = "includeTagNames") final @Nullable Boolean includeTagNames,
            @JsonProperty("userProperties") final @Nullable List<UserProperty> userProperties,
            @JsonProperty("addressRange") final @NotNull AddressRange addressRange) {
        this.destination = destination;
        this.qos = requireNonNullElse(qos, 0);
        this.messageHandlingOptions = requireNonNullElse(messageHandlingOptions, MQTTMessagePerSubscription);
        this.includeTimestamp = requireNonNullElse(includeTimestamp, true);
        this.includeTagNames = requireNonNullElse(includeTagNames, false);
        this.addressRange = addressRange;
        this.userProperties = requireNonNullElseGet(userProperties, List::of);
    }

    public @NotNull AddressRange getAddressRange() {
        return addressRange;
    }

    @Override
    public @Nullable String getDestinationMqttTopic() {
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
