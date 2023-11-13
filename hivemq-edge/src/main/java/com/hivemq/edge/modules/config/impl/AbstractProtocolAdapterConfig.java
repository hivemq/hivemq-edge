package com.hivemq.edge.modules.config.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.edge.HiveMQEdgeConstants;
import com.hivemq.edge.modules.adapters.annotations.ModuleConfigField;
import com.hivemq.edge.modules.config.CustomConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

/**
 * @author Simon L Johnson
 */
public class AbstractProtocolAdapterConfig implements CustomConfig {

    @JsonProperty(value = "id", required = true)
    @ModuleConfigField(title = "Identifier",
                       description = "Unique identifier for this protocol adapter",
                       format = ModuleConfigField.FieldType.IDENTIFIER,
                       required = true,
                       stringPattern = HiveMQEdgeConstants.ID_REGEX,
                       stringMinLength = 1,
                       stringMaxLength = 1024)
    protected @NotNull String id;

    public @NotNull String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public static class Subscription {

        public enum MessageHandlingOptions {
            MQTTMessagePerTag,
            MQTTMessagePerSubscription
        }

        @JsonProperty(value = "destination", required = true)
        @ModuleConfigField(title = "Destination Topic",
                           description = "The topic to publish data on",
                           required = true,
                           format = ModuleConfigField.FieldType.MQTT_TOPIC)
        private @Nullable String destination;

        @JsonProperty(value = "qos", required = true)
        @ModuleConfigField(title = "QoS",
                           description = "MQTT Quality of Service level",
                           required = true,
                           numberMin = 0,
                           numberMax = 2,
                           defaultValue = "0")
        private int qos = 0;

        @JsonProperty(value = "messageHandlingOptions", required = true)
        @ModuleConfigField(title = "Message Handling Options",
                           description = "The topic to publish data on",
                           required = true,
                           enumDisplayValues = {"MQTT Message Per Device Tag",
                                                "MQTT Message Per Subscription (Potentially Multiple Data Points Per Sample)"},
                           defaultValue = "MQTTMessagePerTag")
        private @Nullable MessageHandlingOptions messageHandlingOptions = MessageHandlingOptions.MQTTMessagePerTag;

        @JsonProperty(value = "includeTimestamp", required = true)
        @ModuleConfigField(title = "Include Sample Timestamp In Publish?",
                           description = "Include the unix timestamp of the sample time in the resulting MQTT message",
                           defaultValue = "true")
        private @Nullable Boolean includeTimestamp = Boolean.TRUE;

        @JsonProperty(value = "includeTagNames", required = true)
        @ModuleConfigField(title = "Include Tag Names In Publish?",
                           description = "Include the names of the tags in the resulting MQTT publish",
                           defaultValue = "true")
        private @Nullable Boolean includeTagNames = Boolean.TRUE;

        public Subscription() {
        }

        @JsonCreator
        public Subscription(
                @JsonProperty("destination") @Nullable final String destination,
                @JsonProperty("qos") final int qos) {
            this.destination = destination;
            this.qos = qos;
        }

        public String getDestination() {
            return destination;
        }

        public int getQos() {
            return qos;
        }

        public MessageHandlingOptions getMessageHandlingOptions() {
            return messageHandlingOptions;
        }

        public Boolean getIncludeTimestamp() {
            return includeTimestamp;
        }

        public Boolean getIncludeTagNames() {
            return includeTagNames;
        }
    }



}
