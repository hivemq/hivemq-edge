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
package com.hivemq.edge.adapters.etherip_cip_odva.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EipSpecificAdapterConfig implements ProtocolSpecificAdapterConfig {

    private static final int PORT_MIN = 1;
    private static final int PORT_MAX = 65535;

    private static final @NotNull String ID_REGEX = "^([a-zA-Z_0-9-_])*$";

    @JsonProperty(value = "id", required = true, access = JsonProperty.Access.WRITE_ONLY)
    @ModuleConfigField(
            title = "Identifier",
            description = "Unique identifier for this protocol adapter",
            format = ModuleConfigField.FieldType.IDENTIFIER,
            required = true,
            stringPattern = ID_REGEX,
            stringMinLength = 1,
            stringMaxLength = 1024)
    private @Nullable String id;

    @JsonProperty(value = "host", required = true)
    @ModuleConfigField(
            title = "Host",
            description = "IP Address or hostname of the device you wish to connect to",
            required = true,
            format = ModuleConfigField.FieldType.HOSTNAME)
    private final @NotNull String host;

    @JsonProperty(value = "port", required = true)
    @ModuleConfigField(
            title = "Port",
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

    public enum ByteOrder {
        LITTLE_ENDIAN,
        BIG_ENDIAN;

        public java.nio.ByteOrder toNioByteOrder() {
            return this == ByteOrder.LITTLE_ENDIAN ? java.nio.ByteOrder.LITTLE_ENDIAN : java.nio.ByteOrder.BIG_ENDIAN;
        }
    }

    @JsonProperty("byteOrder")
    @ModuleConfigField(
            title = "Byte Order",
            description =
                    "Use Big Endian (most significant byte first) or Little Endian (least significant byte first) notation for numbers",
            enumDisplayValues = {"Little Endian", "Big Endian"},
            required = true)
    private ByteOrder byteOrder;

    @JsonProperty(value = "eipToMqtt", required = true)
    @ModuleConfigField(
            title = "Ethernet IP To MQTT Config",
            description = "The configuration for a data stream from Ethernet IP to MQTT",
            required = true)
    private final @NotNull EipToMqttConfig eipToMqttConfig;

    @JsonCreator
    public EipSpecificAdapterConfig(
            @JsonProperty(value = "port", required = true) final int port,
            @JsonProperty(value = "host", required = true) final @NotNull String host,
            @JsonProperty(value = "backplane") final @Nullable Integer backplane,
            @JsonProperty(value = "slot") final @Nullable Integer slot,
            @JsonProperty(value = "byteOrder") final @NotNull EipSpecificAdapterConfig.ByteOrder byteOrder,
            @JsonProperty(value = "eipToMqtt") final @Nullable EipToMqttConfig eipToMqttConfig) {
        this.host = host;
        this.port = port;
        this.backplane = Objects.requireNonNullElse(backplane, 1);
        this.slot = Objects.requireNonNullElse(slot, 0);
        this.byteOrder = byteOrder;
        this.eipToMqttConfig =
                Objects.requireNonNullElseGet(eipToMqttConfig, () -> new EipToMqttConfig(null, null, null));
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

    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    public @NotNull EipToMqttConfig getEipToMqttConfig() {
        return eipToMqttConfig;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof final EipSpecificAdapterConfig that)) {
            return false;
        }
        return port == that.port
                && backplane == that.backplane
                && slot == that.slot
                && com.google.common.base.Objects.equal(id, that.id)
                && com.google.common.base.Objects.equal(host, that.host)
                && byteOrder == that.byteOrder
                && com.google.common.base.Objects.equal(eipToMqttConfig, that.eipToMqttConfig);
    }

    @Override
    public int hashCode() {
        return com.google.common.base.Objects.hashCode(id, host, port, backplane, slot, byteOrder, eipToMqttConfig);
    }
}
