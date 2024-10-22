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
package com.hivemq.edge.adapters.s7.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.edge.adapters.plc4x.config.Plc4xToMqttConfig;
import com.hivemq.edge.adapters.plc4x.config.Plc4xToMqttMapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class S7ToMqttConfig extends Plc4xToMqttConfig {

    @JsonProperty("s7ToMqttMappings")
    @ModuleConfigField(title = "S7 to MQTT Mappings", description = "Map your sensor data to MQTT Topics")
    private final @NotNull List<Plc4xToMqttMapping> mappings;

    @JsonCreator
    public S7ToMqttConfig(
            @JsonProperty(value = "pollingIntervalMillis") final @Nullable Integer pollingIntervalMillis,
            @JsonProperty(value = "maxPollingErrorsBeforeRemoval") final @Nullable Integer maxPollingErrorsBeforeRemoval,
            @JsonProperty(value = "publishChangedDataOnly") final @Nullable Boolean publishChangedDataOnly,
            @JsonProperty(value = "s7ToMqttMappings") final @Nullable List<Plc4xToMqttMapping> mappings) {
        super(pollingIntervalMillis, maxPollingErrorsBeforeRemoval, publishChangedDataOnly);
        this.mappings = Objects.requireNonNullElse(mappings, List.of());
    }

    public @NotNull List<Plc4xToMqttMapping> getMappings() {
        return mappings;
    }
}
