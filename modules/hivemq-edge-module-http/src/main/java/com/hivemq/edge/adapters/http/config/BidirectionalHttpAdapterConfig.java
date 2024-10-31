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
package com.hivemq.edge.adapters.http.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import com.hivemq.edge.adapters.http.config.http2mqtt.HttpToMqttConfig;
import com.hivemq.edge.adapters.http.config.mqtt2http.MqttToHttpConfig;
import com.hivemq.edge.adapters.http.tag.HttpTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static com.hivemq.edge.adapters.http.HttpAdapterConstants.*;

public class BidirectionalHttpAdapterConfig extends HttpAdapterConfig {

    @JsonProperty(value = "mqttToHttp")
    @ModuleConfigField(title = "MQTT to HTTP Config",
                       description = "The configuration for a data stream from MQTT to HTTP")
    private final @NotNull MqttToHttpConfig mqttToHttpConfig;

    @JsonCreator
    public BidirectionalHttpAdapterConfig(
            @JsonProperty(value = "id", required = true) final @NotNull String id,
            @JsonProperty(value = "httpConnectTimeoutSeconds") final @Nullable Integer httpConnectTimeoutSeconds,
            @JsonProperty(value = "httpToMqtt") final @Nullable HttpToMqttConfig httpToMqttConfig,
            @JsonProperty(value = "mqttToHttp") final @Nullable MqttToHttpConfig mqttToHttpConfig,
            @JsonProperty(value = "allowUntrustedCertificates") final @Nullable Boolean allowUntrustedCertificates,
            @JsonProperty(value = "tags") final @Nullable List<HttpTag> tags) {
        super(id, httpConnectTimeoutSeconds, httpToMqttConfig, allowUntrustedCertificates, tags);
        this.mqttToHttpConfig = Objects.requireNonNullElseGet(mqttToHttpConfig, () -> new MqttToHttpConfig(List.of()));
    }

    public @NotNull MqttToHttpConfig getMqttToHttpConfig() {
        return mqttToHttpConfig;
    }

}
