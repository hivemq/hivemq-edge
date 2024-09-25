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
package com.hivemq.edge.adapters.opcua.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.mqtt2opcua.MqttToOpcUaConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNullElse;

public class BidirectionalOpcUaAdapterConfig extends OpcUaAdapterConfig {

    @JsonProperty(value = "mqttToOpcua")
    @ModuleConfigField(title = "Mqtt to OpcUA Config",
                       description = "The configuration for a data stream from MQTT to OpcUa")
    private final @NotNull MqttToOpcUaConfig mqttToOpcUaConfig;

    @JsonCreator
    public BidirectionalOpcUaAdapterConfig(
            @JsonProperty(value = "id", required = true) final @NotNull String id,
            @JsonProperty(value = "uri", required = true) final @NotNull String uri,
            @JsonProperty("overrideUri") final @Nullable Boolean overrideUri,
            @JsonProperty("auth") final @Nullable Auth auth,
            @JsonProperty("tls") final @Nullable Tls tls,
            @JsonProperty(value = "opcuaToMqtt") final @Nullable OpcUaToMqttConfig opcuaToMqttConfig,
            @JsonProperty(value = "mqttToOpcua") final @Nullable MqttToOpcUaConfig mqttToOpcUaConfig,
            @JsonProperty("security") final @Nullable Security security) {
        super(id, uri, overrideUri, auth, tls, opcuaToMqttConfig, security);
        this.mqttToOpcUaConfig =
                Objects.requireNonNullElseGet(mqttToOpcUaConfig, () -> new MqttToOpcUaConfig(List.of()));
    }

    public @NotNull MqttToOpcUaConfig getMqttToOpcUaConfig() {
        return mqttToOpcUaConfig;
    }
}
