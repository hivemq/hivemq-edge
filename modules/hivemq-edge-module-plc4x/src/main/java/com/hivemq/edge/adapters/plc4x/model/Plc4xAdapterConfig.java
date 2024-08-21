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
package com.hivemq.edge.adapters.plc4x.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import com.hivemq.adapter.sdk.api.config.UserProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Plc4xAdapterConfig implements ProtocolAdapterConfig {

    private static final @NotNull String ID_REGEX = "^([a-zA-Z_0-9-_])*$";
    private static final int PORT_MIN = 1;
    private static final int PORT_MAX = 65535;

    @JsonProperty(value = "id", required = true)
    @ModuleConfigField(title = "Identifier",
                       description = "Unique identifier for this protocol adapter",
                       format = ModuleConfigField.FieldType.IDENTIFIER,
                       required = true,
                       stringPattern = ID_REGEX,
                       stringMinLength = 1,
                       stringMaxLength = 1024)
    protected @NotNull String id;


    @JsonProperty("pollingIntervalMillis")
    @JsonAlias(value = "publishingInterval") //-- Ensure we cater for properties created with legacy configuration
    @ModuleConfigField(title = "Polling Interval [ms]",
                       description = "Time in millisecond that this endpoint will be polled",
                       numberMin = 1,
                       required = true,
                       defaultValue = "1000")
    private int pollingIntervalMillis = 1000; //1 second

    @JsonProperty("maxPollingErrorsBeforeRemoval")
    @ModuleConfigField(title = "Max. Polling Errors",
                       description = "Max. errors polling the endpoint before the polling daemon is stopped",
                       defaultValue = "10")
    private int maxPollingErrorsBeforeRemoval = 10;

    @JsonProperty("port")
    @ModuleConfigField(title = "Port",
            description = "The port number on the device you wish to connect to",
            required = true,
            numberMin = PORT_MIN,
            numberMax = PORT_MAX)
    private int port;

    @JsonProperty("host")
    @ModuleConfigField(title = "Host",
            description = "IP Address or hostname of the device you wish to connect to",
            required = true,
            format = ModuleConfigField.FieldType.HOSTNAME)
    private @NotNull String host;

    @JsonProperty("publishChangedDataOnly")
    @ModuleConfigField(title = "Only publish data items that have changed since last poll",
                       defaultValue = "true",
                       format = ModuleConfigField.FieldType.BOOLEAN)
    private boolean publishChangedDataOnly = true;

    @JsonProperty("subscriptions")
    @ModuleConfigField(title = "Subscriptions",
                       description = "Map your sensor data to MQTT Topics")
    private @NotNull List<? extends PollingContextImpl> subscriptions = new ArrayList<>();

    public Plc4xAdapterConfig() {
    }

    public @NotNull String getId() {
        return id;
    }

    public void setId(final @NotNull String id) {
        this.id = id;
    }

    public int getPort() {
        return port;
    }

    public @NotNull String getHost() {
        return host;
    }

    public boolean getPublishChangedDataOnly() {
        return publishChangedDataOnly;
    }

    public @NotNull List<? extends PollingContextImpl> getSubscriptions() {
        return subscriptions;
    }

    public int getPollingIntervalMillis() {
        return pollingIntervalMillis;
    }

    public int getMaxPollingErrorsBeforeRemoval() {
        return maxPollingErrorsBeforeRemoval;
    }

    @JsonPropertyOrder({"tagName", "tagAddress", "dataType", "destination", "qos"})
    public static class PollingContextImpl implements PollingContext {

        @JsonProperty(value = "tagName", required = true)
        @ModuleConfigField(title = "Tag Name",
                           description = "The name to assign to this address. The tag name must be unique for all subscriptions within this protocol adapter.",
                           required = true,
                           format = ModuleConfigField.FieldType.IDENTIFIER)
        private @NotNull String tagName;

        @JsonProperty("tagAddress")
        @ModuleConfigField(title = "Tag Address",
                           description = "The well formed address of the tag to read",
                           required = true)
        private @NotNull String tagAddress;

        @JsonProperty("dataType")
        @ModuleConfigField(title = "Data Type",
                           description = "The expected data type of the tag",
                           enumDisplayValues = {"Null",
                                                "Boolean",
                                                "Byte",
                                                "Word (unit 16)",
                                                "DWord (uint 32)",
                                                "LWord (uint 64)",
                                                "USint (uint 8)",
                                                "Uint (uint 16)",
                                                "UDint (uint 32)",
                                                "ULint (uint 64)",
                                                "Sint (int 8)",
                                                "Int (int 16)",
                                                "Dint (int 32)",
                                                "Lint (int 64)",
                                                "Real (float 32)",
                                                "LReal (double 64)",
                                                "Char (1 byte char)",
                                                "WChar (2 byte char)",
                                                "String",
                                                "WString",
                                                "Timing (Duration ms)",
                                                "Long Timing (Duration ns)",
                                                "Date (DateStamp)",
                                                "Long Date (DateStamp)",
                                                "Time Of Day (TimeStamp)",
                                                "Long Time Of Day (TimeStamp)",
                                                "Date Time (DateTimeStamp)",
                                                "Long Date Time (DateTimeStamp)",
                                                "Raw Byte Array"
                           },
                           required = true)
        private @NotNull Plc4xDataType.DATA_TYPE dataType;

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


        public @NotNull String getTagName() {
            return tagName;
        }

        public String getTagAddress() {
            return tagAddress;
        }

        public Plc4xDataType.DATA_TYPE getDataType() {
            return dataType;
        }

        @Override
        public @Nullable String getMqttTopic() {
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
}
