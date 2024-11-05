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
package com.hivemq.edge.adapters.etherip.config.legacy;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import com.hivemq.edge.adapters.etherip.config.EipDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LegacyEipAdapterConfig implements ProtocolAdapterConfig {

    @JsonProperty("port")
    private int port = 44818;

    @JsonProperty("backplane")
    private @NotNull Integer backplane;

    @JsonProperty("slot")
    private @NotNull Integer slot;

    @JsonProperty("subscriptions")
    private @NotNull List<LegacyEipAdapterConfig.PollingContextImpl> subscriptions = new ArrayList<>();

    @JsonProperty("pollingIntervalMillis")
    @JsonAlias(value = "publishingInterval") //-- Ensure we cater for properties created with legacy configuration
    private int pollingIntervalMillis = 1000; //1 second

    @JsonProperty("maxPollingErrorsBeforeRemoval")
    private int maxPollingErrorsBeforeRemoval = 10;

    @JsonProperty("host")
    private @NotNull String host;

    @JsonProperty("publishChangedDataOnly")
    private boolean publishChangedDataOnly = true;

    @JsonProperty(value = "id", required = true)
    protected @NotNull String id;

    public LegacyEipAdapterConfig() {
    }

    public @NotNull Integer getBackplane() {
        return backplane;
    }

    public @NotNull Integer getSlot() {
        return slot;
    }

    public @NotNull String getId() {
        return id;
    }

    @Override
    public @NotNull Set<String> calculateAllUsedTags() {
        // TODO
        return Set.of();
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

    public @NotNull List<LegacyEipAdapterConfig.PollingContextImpl> getSubscriptions() {
        return subscriptions;
    }

    public int getPollingIntervalMillis() {
        return pollingIntervalMillis;
    }

    public int getMaxPollingErrorsBeforeRemoval() {
        return maxPollingErrorsBeforeRemoval;
    }

    @JsonPropertyOrder({"tagName", "tagAddress", "dataType", "destination", "qos"})
    public static class PollingContextImpl {

        @JsonProperty(value = "tagName", required = true)
        private @NotNull String tagName;

        @JsonProperty("tagAddress")
        private @NotNull String tagAddress;

        @JsonProperty("dataType")
        private @NotNull EipDataType dataType;

        @JsonProperty(value = "destination", required = true)
        protected @Nullable String destination;

        @JsonProperty(value = "qos", required = true)
        protected int qos = 0;

        @JsonProperty(value = "messageHandlingOptions")
        protected @NotNull MessageHandlingOptions messageHandlingOptions = MessageHandlingOptions.MQTTMessagePerTag;

        @JsonProperty(value = "includeTimestamp")
        protected @NotNull Boolean includeTimestamp = Boolean.TRUE;

        @JsonProperty(value = "includeTagNames")
        protected @NotNull Boolean includeTagNames = Boolean.FALSE;

        @JsonProperty(value = "userProperties")
        private @NotNull List<MqttUserProperty> userProperties = new ArrayList<>();


        public @NotNull String getTagName() {
            return tagName;
        }

        public String getTagAddress() {
            return tagAddress;
        }

        public EipDataType getDataType() {
            return dataType;
        }

        public @Nullable String getDestinationMqttTopic() {
            return destination;
        }

        public int getQos() {
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

        public @NotNull List<MqttUserProperty> getUserProperties() {
            return userProperties;
        }
    }
}
