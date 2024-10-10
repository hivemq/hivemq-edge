package com.hivemq.edge.adapters.modbus.config.legacy;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.hivemq.adapter.sdk.api.config.MessageHandlingOptions.MQTTMessagePerSubscription;
import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;

public class LegacyModbusPollingContext {

    @JsonProperty(value = "destination", required = true)
    private final @NotNull String destination;

    @JsonProperty(value = "qos", required = true)
    private final int qos;

    @JsonProperty(value = "messageHandlingOptions")
    private final @NotNull MessageHandlingOptions messageHandlingOptions;

    @JsonProperty(value = "includeTimestamp")
    private final boolean includeTimestamp;

    @JsonProperty(value = "includeTagNames")
    private final boolean includeTagNames;

    @JsonProperty(value = "userProperties")
    private final @NotNull List<MqttUserProperty> userProperties;

    @JsonProperty("addressRange")
    @JsonAlias("holding-registers")
    private final @NotNull AddressRange addressRange;

    @JsonCreator
    public LegacyModbusPollingContext(
            @JsonProperty(value = "destination", required = true) final @NotNull String destination,
            @JsonProperty("qos") final @Nullable Integer qos,
            @JsonProperty("messageHandlingOptions") final @Nullable MessageHandlingOptions messageHandlingOptions,
            @JsonProperty("includeTimestamp") final @Nullable Boolean includeTimestamp,
            @JsonProperty("includeTagNames") final @Nullable Boolean includeTagNames,
            @JsonProperty("userProperties") final @Nullable List<MqttUserProperty> userProperties,
            @JsonProperty(value = "addressRange", required = true) final @NotNull AddressRange addressRange) {
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

    public @NotNull String getMqttTopic() {
        return destination;
    }

    public int getMqttQos() {
        return qos;
    }

    public @NotNull MessageHandlingOptions getMessageHandlingOptions() {
        return messageHandlingOptions;
    }

    public @NotNull Boolean getIncludeTimestamp() {
        return includeTimestamp;
    }

    public @NotNull Boolean getIncludeTagNames() {
        return includeTagNames;
    }

    public @NotNull List<MqttUserProperty> getLegacyProperties() {
        return userProperties;
    }
}
