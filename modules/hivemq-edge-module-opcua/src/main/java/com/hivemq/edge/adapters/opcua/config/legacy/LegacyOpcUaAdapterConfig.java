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
package com.hivemq.edge.adapters.opcua.config.legacy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.Auth;
import com.hivemq.edge.adapters.opcua.config.SecPolicy;
import com.hivemq.edge.adapters.opcua.config.Security;
import com.hivemq.edge.adapters.opcua.config.Tls;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LegacyOpcUaAdapterConfig {

    @JsonProperty(value = "id", required = true)
    protected @NotNull String id;

    @JsonProperty("uri")
    private @NotNull String uri;

    @JsonProperty("overrideUri")
    private @NotNull Boolean overrideUri = false;

    @JsonProperty("subscriptions")
    private @NotNull List<Subscription> subscriptions = new ArrayList<>();

    @JsonProperty("auth")
    private @NotNull Auth auth = new Auth(null, null);

    @JsonProperty("tls")
    private @Nullable Tls tls = new Tls(false, null, null);

    @JsonProperty("security")
    private @NotNull Security security = new Security(SecPolicy.DEFAULT);

    public @NotNull String getId() {
        return id;
    }

    public @NotNull String getUri() {
        return uri;
    }

    public @NotNull List<Subscription> getSubscriptions() {
        return subscriptions;
    }

    public @NotNull Auth getAuth() {
        return auth;
    }

    public @Nullable Tls getTls() {
        return tls;
    }

    public @NotNull Security getSecurity() {
        return security;
    }

    public @NotNull Boolean getOverrideUri() {
        return overrideUri;
    }

    public static class Subscription {

        @JsonProperty("node")
        private @NotNull String node = "";

        @JsonProperty("mqtt-topic")
        private @NotNull String mqttTopic;

        @JsonProperty("publishing-interval")
        private int publishingInterval = 1000; //1 second

        @JsonProperty("server-queue-size")
        private int serverQueueSize = 1;

        @JsonProperty("qos")
        private int qos = 0;

        @JsonProperty("message-expiry-interval")
        private @Nullable Integer messageExpiryInterval;

        public @NotNull String getNode() {
            return node;
        }

        public @NotNull String getMqttTopic() {
            return mqttTopic;
        }

        public int getPublishingInterval() {
            return publishingInterval;
        }

        public int getServerQueueSize() {
            return serverQueueSize;
        }

        public int getQos() {
            return qos;
        }

        public @Nullable Integer getMessageExpiryInterval() {
            return messageExpiryInterval;
        }
    }

}
