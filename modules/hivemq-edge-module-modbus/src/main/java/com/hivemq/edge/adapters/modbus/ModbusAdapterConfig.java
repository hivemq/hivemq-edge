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
import com.hivemq.edge.modules.config.impl.AbstractPollingProtocolAdapterConfig;
import com.hivemq.edge.modules.config.impl.AdapterSubscriptionImpl;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ModbusAdapterConfig extends AbstractPollingProtocolAdapterConfig {

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
    @ModuleConfigField(title = "Subscriptions",
            description = "Map your sensor data to MQTT Topics")
    private @NotNull List<AdapterSubscription> subscriptions = new ArrayList<>();

    public ModbusAdapterConfig() {
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

    public static class AdapterSubscription extends AdapterSubscriptionImpl {
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
            super(destination, qos, userProperties);
            this.addressRange = addressRange;
        }

        public @NotNull AddressRange getAddressRange() {
            return addressRange;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (!(o instanceof AdapterSubscription)) return false;
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
            if (this == o) return true;
            if (!(o instanceof AddressRange)) return false;
            AddressRange that = (AddressRange) o;
            return startIdx == that.startIdx && endIdx == that.endIdx;
        }

        @Override
        public int hashCode() {
            return Objects.hash(startIdx, endIdx);
        }
    }
}
