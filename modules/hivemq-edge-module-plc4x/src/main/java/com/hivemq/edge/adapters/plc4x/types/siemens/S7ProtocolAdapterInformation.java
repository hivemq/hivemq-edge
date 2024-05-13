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
package com.hivemq.edge.adapters.plc4x.types.siemens;

import com.hivemq.edge.modules.adapters.ProtocolAdapterCapability;
import com.hivemq.edge.modules.adapters.ProtocolAdapterCategory;
import com.hivemq.edge.modules.adapters.ProtocolAdapterInformation;
import com.hivemq.edge.modules.adapters.ProtocolAdapterTag;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;

/**
 * @author HiveMQ Adapter Generator
 */
public class S7ProtocolAdapterInformation implements ProtocolAdapterInformation {

    public static final ProtocolAdapterInformation INSTANCE = new S7ProtocolAdapterInformation();

    protected S7ProtocolAdapterInformation() {
    }

    @Override
    public @NotNull String getProtocolName() {
        return "S7";
    }

    @Override
    public @NotNull String getProtocolId() {
        return "s7";
    }

    @Override
    public @NotNull String getDisplayName() {
        return "S7 to MQTT Protocol Adapter";
    }

    @Override
    public @NotNull String getDescription() {
        return "Connects HiveMQ Edge to S7-300, S7-400, S7-1200, S7-1500 & LOGO devices, reading data from the PLC into MQTT.";
    }

    @Override
    public @NotNull String getUrl() {
        return "https://docs.hivemq.com/hivemq-edge/protocol-adapters.html#s7-adapter";
    }

    @Override
    public @NotNull String getVersion() {
        return "${edge-version}";
    }

    @Override
    public @NotNull String getLogoUrl() {
        return "/images/s8-icon.png";

    }

    @Override
    public @NotNull String getAuthor() {
        return "HiveMQ";
    }

    @Override
    public ProtocolAdapterCategory getCategory() {
        return ProtocolAdapterCategory.INDUSTRIAL;
    }

    @Override
    public @NotNull String getUrl() {
        return "https://docs.hivemq.com/hivemq-edge/protocol-adapters.html#s7-adapter";
    }

    @Override
    public List<ProtocolAdapterTag> getTags() {
        return List.of(ProtocolAdapterTag.TCP,
                ProtocolAdapterTag.AUTOMATION,
                ProtocolAdapterTag.FACTORY);
    }

    @Override
    public @NotNull EnumSet<ProtocolAdapterCapability> getCapabilities() {
        return EnumSet.of(ProtocolAdapterCapability.READ);
    }

}
