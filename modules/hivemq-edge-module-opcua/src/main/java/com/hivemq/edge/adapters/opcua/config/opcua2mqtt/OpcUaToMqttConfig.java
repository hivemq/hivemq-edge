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
package com.hivemq.edge.adapters.opcua.config.opcua2mqtt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class OpcUaToMqttConfig {

    @JsonProperty("opcuaToMqttMappings")
    @ModuleConfigField(title = "opcuaToMqttMappings",
                       description = "Map your sensor data to MQTT Topics")
    private final @NotNull List<OpcUaToMqttMapping> opcuaToMqttMappings;

    @JsonCreator
    public OpcUaToMqttConfig(@JsonProperty("opcuaToMqttMappings") final @Nullable List<OpcUaToMqttMapping> opcuaToMqttMappings) {
        this.opcuaToMqttMappings = Objects.requireNonNullElse(opcuaToMqttMappings, List.of());
    }

    public @NotNull List<OpcUaToMqttMapping> getOpcuaToMqttMappings() {
        return opcuaToMqttMappings;
    }
}
