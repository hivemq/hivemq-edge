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

        @JsonProperty(value = "includeTagNames", required = false)
        @ModuleConfigField(title = "Include Tag Names In Publish?",
                           description = "Include the names of the tags in the resulting MQTT publish",
                           defaultValue = "false")
        private @Nullable Boolean includeTagNames = Boolean.FALSE;

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
