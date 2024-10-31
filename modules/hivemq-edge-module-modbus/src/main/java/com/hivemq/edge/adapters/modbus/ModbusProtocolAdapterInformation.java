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
package com.hivemq.edge.adapters.modbus;

import com.hivemq.adapter.sdk.api.ProtocolAdapterCategory;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.ProtocolAdapterTag;
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.modbus.config.ModbusAdapterConfig;
import com.hivemq.edge.adapters.modbus.config.tag.ModbusTag;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ModbusProtocolAdapterInformation implements ProtocolAdapterInformation {

    public static final ProtocolAdapterInformation INSTANCE = new ModbusProtocolAdapterInformation();
    private static final @NotNull Logger log = LoggerFactory.getLogger(ModbusProtocolAdapterInformation.class);
    public static final @NotNull String PROTOCOL_ID = "modbus";

    protected ModbusProtocolAdapterInformation() {
    }

    @Override
    public @NotNull String getProtocolName() {
        return "Modbus TCP";
    }

    @Override
    public @NotNull String getProtocolId() {
        return PROTOCOL_ID;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Modbus to MQTT Protocol Adapter";
    }

    @Override
    public @NotNull String getDescription() {
        return "Connects HiveMQ Edge to existing Modbus devices, bringing data from coils & registers into MQTT.";
    }

    @Override
    public @NotNull String getUrl() {
        return "https://docs.hivemq.com/hivemq-edge/protocol-adapters.html#modbus-tcp-adapter";
    }

    @Override
    public @NotNull String getVersion() {
        return "${edge-version}";
    }

    @Override
    public @NotNull String getLogoUrl() {
        return "/images/modbus-icon.png";
    }

    @Override
    public @NotNull String getAuthor() {
        return "HiveMQ";
    }

    @Override
    public @Nullable ProtocolAdapterCategory getCategory() {
        return ProtocolAdapterCategory.INDUSTRIAL;
    }

    @Override
    public List<ProtocolAdapterTag> getTags() {
        return List.of(ProtocolAdapterTag.TCP);
    }

    @Override
    public @Nullable String getUiSchema() {
        try (final InputStream is = this.getClass()
                .getClassLoader()
                .getResourceAsStream("modbus-adapter-ui-schema.json")) {
            if (is == null) {
                log.warn("The UISchema for the Simulation Adapter could not be loaded from resources: Not found.");
                return null;
            }
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("The UISchema for the Simulation Adapter could not be loaded from resources:", e);
            return null;
        }
    }

    @Override
    public @NotNull Class<? extends Tag<?>> tagConfigurationClass() {
        return ModbusTag.class;
    }

    @Override
    public @NotNull Class<? extends ProtocolAdapterConfig> configurationClassReading() {
        return ModbusAdapterConfig.class;
    }

    @Override
    public @NotNull Class<? extends ProtocolAdapterConfig> configurationClassWriting() {
        return ModbusAdapterConfig.class;
    }
}
