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

import com.hivemq.edge.modules.adapters.ProtocolAdapterConstants;
import com.hivemq.edge.modules.adapters.impl.AbstractProtocolAdapterInformation;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.edge.modules.config.CustomConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;

public class ModbusProtocolAdapterInformation
        extends AbstractProtocolAdapterInformation {

    public static final ProtocolAdapterInformation INSTANCE = new ModbusProtocolAdapterInformation();

    protected ModbusProtocolAdapterInformation() {
    }

    @Override
    public @NotNull String getProtocolName() {
        return "Modbus TCP";
    }

    @Override
    public @NotNull String getProtocolId() {
        return "modbus";
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
        return "https://github.com/hivemq/hivemq-edge/wiki/Protocol-adapters#modbus-tcp-adapter";
    }

    @Override
    public List<ProtocolAdapterConstants.TAG> getTags() {
        return List.of(ProtocolAdapterConstants.TAG.TCP);
    }
}
