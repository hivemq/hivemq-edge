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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hivemq.edge.modules.adapters.annotations.ModuleConfigField;
import com.hivemq.edge.modules.config.impl.AbstractPollingProtocolAdapterConfig;
import com.hivemq.edge.modules.config.impl.AbstractProtocolAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Plc4xAdapterConfig extends AbstractPollingProtocolAdapterConfig {

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
    private @NotNull List<? extends Subscription> subscriptions = new ArrayList<>();

    public Plc4xAdapterConfig() {
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

    public @NotNull List<? extends Subscription> getSubscriptions() {
        return subscriptions;
    }

    @JsonPropertyOrder({"tagName", "tagAddress", "dataType", "destination", "qos"})
    public static class Subscription extends AbstractProtocolAdapterConfig.Subscription {

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

        public @NotNull String getTagName() {
            return tagName;
        }

        public String getTagAddress() {
            return tagAddress;
        }

        public Plc4xDataType.DATA_TYPE getDataType() {
            return dataType;
        }
    }
}
