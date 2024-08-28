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
package com.hivemq.edge.adapters.plc4x.types.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.edge.adapters.plc4x.model.Plc4xPollingContext;
import com.hivemq.edge.adapters.plc4x.model.Plc4xToMqttConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class ADSToMqttConfig extends Plc4xToMqttConfig {

    @JsonProperty("adsToMqttMappings")
    @JsonSerialize(using = ADSPollingContextSerializer.class)
    @ModuleConfigField(title = "ADS to MQTT Mappings", description = "Map your sensor data to MQTT Topics")
    private final @NotNull List<Plc4xPollingContext> mappings;

    @JsonCreator
    public ADSToMqttConfig(
            @JsonProperty(value = "pollingIntervalMillis") final @Nullable Integer pollingIntervalMillis,
            @JsonProperty(value = "maxPollingErrorsBeforeRemoval") final @Nullable Integer maxPollingErrorsBeforeRemoval,
            @JsonProperty(value = "publishChangedDataOnly") final @Nullable Boolean publishChangedDataOnly,
            @JsonProperty(value = "adsToMqttMappings") final @Nullable List<Plc4xPollingContext> mappings) {
        super(pollingIntervalMillis, maxPollingErrorsBeforeRemoval, publishChangedDataOnly);
        this.mappings = Objects.requireNonNullElse(mappings, List.of());
    }

    public @NotNull List<Plc4xPollingContext> getMappings() {
        return mappings;
    }
}
