/*
 * Copyright 2023-present HiveMQ GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.hivemq.edge.adapters.file.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.AdapterConfigWithPollingContexts;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings({"unused", "FieldCanBeLocal", "FieldMayBeFinal"})
public class FileSpecificAdapterConfig implements ProtocolSpecificAdapterConfig, AdapterConfigWithPollingContexts {

    private static final @NotNull String ID_REGEX = "^([a-zA-Z_0-9-_])*$";

    @JsonProperty(value = "id", required = true, access = JsonProperty.Access.WRITE_ONLY)
    @ModuleConfigField(title = "Identifier",
                       description = "Unique identifier for this protocol adapter",
                       format = ModuleConfigField.FieldType.IDENTIFIER,
                       required = true,
                       stringPattern = ID_REGEX,
                       stringMinLength = 1,
                       stringMaxLength = 1024)
    private final @NotNull String id;

    @JsonProperty(value = "fileToMqtt", required = true)
    @ModuleConfigField(title = "File To MQTT Config",
                       description = "The configuration for a data stream from File to MQTT",
                       required = true)
    private final @Nullable FileToMqttConfig fileToMqttConfig;

    public FileSpecificAdapterConfig(
            @JsonProperty(value = "id",
                          required = true,
                          access = JsonProperty.Access.WRITE_ONLY) final @NotNull String id,
            @JsonProperty(value = "fileToMqtt") final @Nullable FileToMqttConfig fileToMqttConfig) {
        this.id = id;
        if (fileToMqttConfig == null) {
            this.fileToMqttConfig = new FileToMqttConfig(null, null, null);
        } else {
            this.fileToMqttConfig = fileToMqttConfig;
        }
    }

    public @NotNull FileToMqttConfig getFileToMqttConfig() {
        return fileToMqttConfig;
    }

    @Override
    public @NotNull List<? extends PollingContext> getPollingContexts() {
        if (fileToMqttConfig == null) {
            return List.of();
        }
        return fileToMqttConfig.getMappings();
    }
}
