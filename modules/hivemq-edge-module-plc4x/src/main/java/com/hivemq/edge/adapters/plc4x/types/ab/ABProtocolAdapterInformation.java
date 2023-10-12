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
package com.hivemq.edge.adapters.plc4x.types.ab;

import com.hivemq.edge.modules.adapters.ProtocolAdapterConstants;
import com.hivemq.edge.modules.adapters.impl.AbstractProtocolAdapterInformation;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterCapability;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.edge.modules.config.CustomConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;

/**
 * @author HiveMQ Adapter Generator
 */
public class ABProtocolAdapterInformation
    extends AbstractProtocolAdapterInformation {

    public static final ProtocolAdapterInformation INSTANCE = new ABProtocolAdapterInformation();

    protected ABProtocolAdapterInformation() {
    }

    @Override
    public @NotNull String getProtocolName() {
        return "Allen Bradley Ethernet";
    }

    @Override
    public @NotNull String getProtocolId() {
        return "ab-eth";
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Allen Bradley Ethernet to MQTT Protocol Adapter";
    }

    @Override
    public @NotNull String getDescription() {
        return "Connects HiveMQ Edge to existing Allen Bradley Ethernet compliant devices, bringing data from the PLC into MQTT.";
    }

    @Override
    public String getVersion() {
        return super.getVersion() + " (BETA)";
    }

    @Override
    public ProtocolAdapterConstants.CATEGORY getCategory() {
        return ProtocolAdapterConstants.CATEGORY.INDUSTRIAL;
    }

    @Override
    public List<ProtocolAdapterConstants.TAG> getTags() {
        return List.of(ProtocolAdapterConstants.TAG.TCP,
                ProtocolAdapterConstants.TAG.AUTOMATION,
                ProtocolAdapterConstants.TAG.FACTORY);
    }

    @Override
    public byte getCapabilities() {
        return ProtocolAdapterCapability.READ;
    }
}
