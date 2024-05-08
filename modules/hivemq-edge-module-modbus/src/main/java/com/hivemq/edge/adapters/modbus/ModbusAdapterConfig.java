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
package com.hivemq.edge.adapters.modbus;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hivemq.edge.modules.adapters.annotations.ModuleConfigField;
import com.hivemq.edge.modules.config.CustomConfig;
import com.hivemq.edge.modules.config.UserProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.hivemq.edge.HiveMQEdgeConstants.ID_REGEX;

public class ModbusAdapterConfig implements CustomConfig {

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
    private int pollingIntervalMillis = DEFAULT_POLLING_INTERVAL; //1 second

    @JsonProperty("maxPollingErrorsBeforeRemoval")
    @ModuleConfigField(title = "Max. Polling Errors",
                       description = "Max. errors polling the endpoint before the polling daemon is stopped",
                       defaultValue = "10")
    private int maxPollingErrorsBeforeRemoval = DEFAULT_MAX_POLLING_ERROR_BEFORE_REMOVAL;

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

    @JsonProperty("timeout")
    @ModuleConfigField(title = "Timeout",
                       description = "Time (in milliseconds) to await a connection before the client gives up",
                       numberMin = 1000,
                       numberMax = 15000,
                       defaultValue = "5000",
                       required = true)
    private @NotNull int timeout = 5000;

    @JsonProperty("publishChangedDataOnly")
    @ModuleConfigField(title = "Only publish data items that have changed since last poll",
                       defaultValue = "true",
                       format = ModuleConfigField.FieldType.BOOLEAN)
    private boolean publishChangedDataOnly = true;

    @JsonProperty("subscriptions")
    @ModuleConfigField(title = "Subscriptions", description = "Map your sensor data to MQTT Topics")
    private @NotNull List<AdapterSubscription> subscriptions = new ArrayList<>();

    public ModbusAdapterConfig() {
    }

    public @NotNull String getId() {
        return id;
    }

    public void setId(final @NotNull String id) {
        this.id = id;
    }

    public int getPollingIntervalMillis() {
        return pollingIntervalMillis;
    }

    public int getMaxPollingErrorsBeforeRemoval() {
        return maxPollingErrorsBeforeRemoval;
    }

    public ModbusAdapterConfig(final @NotNull String adapterId) {
        this.id = adapterId;
    }

    public boolean getPublishChangedDataOnly() {
        return publishChangedDataOnly;
    }

    public int getPort() {
        return port;
    }

    public @NotNull String getHost() {
        return host;
    }

    public @NotNull List<AdapterSubscription> getSubscriptions() {
        return subscriptions;
    }

    public int getTimeout() {
        return timeout;
    }

    public static class AdapterSubscription implements com.hivemq.edge.modules.config.AdapterSubscription {
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

        @JsonProperty("addressRange")
        @JsonAlias("holding-registers")
        @ModuleConfigField(title = "Holding Registers",
                           description = "Define the start and end index values for your memory addresses")
        private @NotNull AddressRange addressRange;

        @JsonCreator
        public AdapterSubscription(
                @JsonProperty("destination") @Nullable final String destination,
                @JsonProperty("qos") final int qos,
                @JsonProperty("addressRange") @NotNull final AddressRange addressRange,
                @JsonProperty("userProperties") @Nullable List<UserProperty> userProperties) {
            this.destination = destination;
            this.qos = qos;
            this.addressRange = addressRange;
            if (userProperties != null) {
                this.userProperties = userProperties;
            }
        }

        public @NotNull AddressRange getAddressRange() {
            return addressRange;
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

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof AdapterSubscription)) {
                return false;
            }
            AdapterSubscription that = (AdapterSubscription) o;
            return Objects.equals(addressRange, that.addressRange);
        }

        @Override
        public int hashCode() {
            return Objects.hash(addressRange);
        }
    }

    @JsonPropertyOrder({"startIdx", "endIdx"})
    public static class AddressRange {

        public AddressRange() {
        }

        @JsonProperty("startIdx")
        @ModuleConfigField(title = "Start Index",
                           description = "The Starting Index (Incl.) of the Address Range",
                           numberMin = 0,
                           numberMax = CustomConfig.PORT_MAX,
                           defaultValue = "0")
        public int startIdx;

        @JsonProperty("endIdx")
        @ModuleConfigField(title = "End Index",
                           description = "The Finishing Index (Excl.) of the Address Range",
                           numberMin = 1,
                           numberMax = CustomConfig.PORT_MAX,
                           defaultValue = "1")
        public int endIdx;

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("AddressRange{");
            sb.append("from=").append(startIdx);
            sb.append(", to=").append(endIdx);
            sb.append('}');
            return sb.toString();
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof AddressRange)) {
                return false;
            }
            AddressRange that = (AddressRange) o;
            return startIdx == that.startIdx && endIdx == that.endIdx;
        }

        @Override
        public int hashCode() {
            return Objects.hash(startIdx, endIdx);
        }
    }
}
