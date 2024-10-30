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
package com.hivemq.edge.adapters.etherip.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import com.hivemq.edge.adapters.etherip.config.tag.EipTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class EipAdapterConfig implements ProtocolAdapterConfig<EipTag> {

    private static final @NotNull String ID_REGEX = "^([a-zA-Z_0-9-_])*$";
    private static final int PORT_MIN = 1;
    private static final int PORT_MAX = 65535;

    @JsonProperty(value = "id", required = true)
    @ModuleConfigField(title = "Identifier",
                       description = "Unique identifier for this protocol adapter",
                       format = ModuleConfigField.FieldType.IDENTIFIER,
                       required = true,
                       stringPattern = ID_REGEX,
                       stringMinLength = 1,
                       stringMaxLength = 1024)
    private final @NotNull String id;

    @JsonProperty(value = "host", required = true)
    @ModuleConfigField(title = "Host",
                       description = "IP Address or hostname of the device you wish to connect to",
                       required = true,
                       format = ModuleConfigField.FieldType.HOSTNAME)
    private final @NotNull String host;


    @JsonProperty(value = "port", required = true)
    @ModuleConfigField(title = "Port",
                       description = "The port number on the device you wish to connect to",
                       required = true,
                       numberMin = PORT_MIN,
                       numberMax = PORT_MAX,
                       defaultValue = "44818")
    private final int port;

    @JsonProperty("backplane")
    @ModuleConfigField(title = "Backplane", description = "Backplane device value", defaultValue = "1")
    private final int backplane;

    @JsonProperty("slot")
    @ModuleConfigField(title = "Slot", description = "Slot device value", defaultValue = "0")
    private final int slot;

    @JsonProperty(value = "eipToMqtt", required = true)
    @ModuleConfigField(title = "Ethernet IP To MQTT Config",
                       description = "The configuration for a data stream from Ethernet IP to MQTT",
                       required = true)
    private final @NotNull EipToMqttConfig eipToMqttConfig;

    @JsonProperty(value = "tags", required = true)
    @ModuleConfigField(title = "Tags defined for this adapter",
                       description = "All tags used by this adapter",
                       required = true)
    private final @NotNull List<EipTag> tags;

    @JsonCreator
    public EipAdapterConfig(
            @JsonProperty(value = "id", required = true) final @NotNull String id,
            @JsonProperty(value = "port", required = true) final int port,
            @JsonProperty(value = "host", required = true) final @NotNull String host,
            @JsonProperty(value = "backplane") final @Nullable Integer backplane,
            @JsonProperty(value = "slot") final @Nullable Integer slot,
            @JsonProperty(value = "eipToMqtt", required = true) final @NotNull EipToMqttConfig eipToMqttConfig,
            @JsonProperty(value = "tags", required = true) final @NotNull List<EipTag> tags) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.backplane = Objects.requireNonNullElse(backplane, 1);
        this.slot = Objects.requireNonNullElse(slot, 0);
        this.eipToMqttConfig = eipToMqttConfig;
        this.tags = tags;
    }

    public @NotNull String getId() {
        return id;
    }

    @Override
    public @NotNull List<String> calculateAllUsedTags() {
        return eipToMqttConfig.getMappings().stream().map(EipToMqttMapping::getTagName).collect(Collectors.toList());
    }

    public @NotNull String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getBackplane() {
        return backplane;
    }

    public int getSlot() {
        return slot;
    }

    public @NotNull EipToMqttConfig getEipToMqttConfig() {
        return eipToMqttConfig;
    }

    @Override
    public List<EipTag> getTags() {
        return Collections.unmodifiableList(tags);
    }

    public void addTag(EipTag tag) {
        tags.add(tag);
    }

    public void removeTag(EipTag tag) {
        tags.remove(tag);
    }
}
