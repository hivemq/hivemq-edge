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
package com.hivemq.edge.adapters.plc4x.config.legacy;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import com.hivemq.edge.adapters.plc4x.config.Plc4xDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LegacyPlc4xAdapterConfig {

    @JsonProperty(value = "id", required = true)
    protected @NotNull String id;

    @JsonProperty("pollingIntervalMillis")
    @JsonAlias(value = "publishingInterval") //-- Ensure we cater for properties created with legacy configuration
    private int pollingIntervalMillis = 1000; //1 second

    @JsonProperty("maxPollingErrorsBeforeRemoval")
    private int maxPollingErrorsBeforeRemoval = 10;

    @JsonProperty("port")
    private int port;

    @JsonProperty("host")
    private @NotNull String host;

    @JsonProperty("publishChangedDataOnly")
    private boolean publishChangedDataOnly = true;

    @JsonProperty("subscriptions")
    private @NotNull List<? extends PollingContextImpl> subscriptions = new ArrayList<>();

    public LegacyPlc4xAdapterConfig() {
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

    public static class PollingContextImpl {

        @JsonProperty(value = "tagName", required = true)
        private @NotNull String tagName;

        @JsonProperty("tagAddress")
        private @NotNull String tagAddress;

        @JsonProperty("dataType")
        private @NotNull Plc4xDataType.DATA_TYPE dataType;

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

        public @NotNull String getTagAddress() {
            return tagAddress;
        }

        public @NotNull Plc4xDataType.DATA_TYPE getDataType() {
            return dataType;
        }

        public @Nullable String getMqttTopic() {
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

        public @NotNull List<MqttUserProperty> getUserProperties() {
            return userProperties;
        }
    }
}
