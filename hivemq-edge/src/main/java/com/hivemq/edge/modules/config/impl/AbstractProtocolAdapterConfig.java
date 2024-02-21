/*
 * Copyright 2019-present HiveMQ GmbH
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
package com.hivemq.edge.modules.config.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.edge.HiveMQEdgeConstants;
import com.hivemq.edge.modules.adapters.annotations.ModuleConfigField;
import com.hivemq.edge.modules.config.CustomConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.List;

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

        @JsonProperty(value = "messageHandlingOptions", required = false)
        @ModuleConfigField(title = "Message Handling Options",
                           description = "This setting defines the format of the resulting MQTT message, either a message per changed tag or a message per subscription that may include multiple data points per sample",
                           required = false,
                           enumDisplayValues = {"MQTT Message Per Device Tag",
                                                "MQTT Message Per Subscription (Potentially Multiple Data Points Per Sample)"},
                           defaultValue = "MQTTMessagePerTag")
        private @Nullable MessageHandlingOptions messageHandlingOptions = MessageHandlingOptions.MQTTMessagePerTag;

        @JsonProperty(value = "includeTimestamp", required = false)
        @ModuleConfigField(title = "Include Sample Timestamp In Publish?",
                           description = "Include the unix timestamp of the sample time in the resulting MQTT message",
                           defaultValue = "true")
        private @Nullable Boolean includeTimestamp = Boolean.TRUE;

        @JsonProperty(value = "includeTagNames")
        @ModuleConfigField(title = "Include Tag Names In Publish?",
                           description = "Include the names of the tags in the resulting MQTT publish",
                           defaultValue = "false")
        private @Nullable Boolean includeTagNames = Boolean.FALSE;

        @JsonProperty(value = "userProperties")
        @ModuleConfigField(title = "User Properties",
                           description = "Arbitrary properties to associate with the subscription",
                           arrayMaxItems = 10)
        private @Nullable List<UserProperty> userProperties;

        public Subscription() {
        }

        @JsonCreator
        public Subscription(
                @JsonProperty("destination") @Nullable final String destination,
                @JsonProperty("qos") final int qos,
                @JsonProperty("userProperties") List<UserProperty> userProperties) {
            this.destination = destination;
            this.qos = qos;
            this.userProperties = userProperties;
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

        public List<UserProperty> getUserProperties() {
            return userProperties;
        }
    }

    public static class UserProperty {
        @JsonProperty("propertyName")
        @ModuleConfigField(title = "Property Name", description = "Username for basic authentication")
        private @Nullable String propertyName = null;

        @JsonProperty("propertyValue")
        @ModuleConfigField(title = "Property Value", description = "Password for basic authentication")
        private @Nullable String propertyValue = null;

        public UserProperty() {
        }

        public UserProperty(@Nullable final String propertyName, @Nullable final String propertyValue) {
            this.propertyName = propertyName;
            this.propertyValue = propertyValue;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public void setPropertyName(final String propertyName) {
            this.propertyName = propertyName;
        }

        public String getPropertyValue() {
            return propertyValue;
        }

        public void setPropertyValue(final String propertyValue) {
            this.propertyValue = propertyValue;
        }
    }

}
