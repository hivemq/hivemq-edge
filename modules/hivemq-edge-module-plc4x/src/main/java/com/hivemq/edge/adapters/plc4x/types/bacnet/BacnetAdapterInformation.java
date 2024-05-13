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
package com.hivemq.edge.adapters.plc4x.types.bacnet;

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
public class BacnetAdapterInformation implements ProtocolAdapterInformation {

    public static final ProtocolAdapterInformation INSTANCE = new BacnetAdapterInformation();

    protected BacnetAdapterInformation() {
    }

    @Override
    public @NotNull String getProtocolName() {
        return "BACnet";
    }

    @Override
    public @NotNull String getProtocolId() {
        return "bacnet";
    }

    @Override
    public @NotNull String getDisplayName() {
        return "BACnet to MQTT Protocol Adapter";
    }

    @Override
    public @NotNull String getDescription() {
        return "Connects HiveMQ Edge to existing BACnet compliant devices, bringing data from the PLC into MQTT.";
    }

    @Override
    public @NotNull String getUrl() {
        return null;
    }

    @Override
    public String getVersion() {
        return "${edge-version}";
    }

    @Override
    public @NotNull String getLogoUrl() {
        return "/images/bacnet-icon.png" ;
    }

    @Override
    public @NotNull String getAuthor() {
        return "HiveMQ";
    }

    @Override
    public ProtocolAdapterCategory getCategory() {
        return ProtocolAdapterCategory.BUILDING_AUTOMATION;
    }

    @Override
    public List<ProtocolAdapterTag> getTags() {
        return List.of(ProtocolAdapterTag.TCP,
                ProtocolAdapterTag.AUTOMATION,
                ProtocolAdapterTag.IOT);
    }

    @Override
    public @NotNull EnumSet<ProtocolAdapterCapability> getCapabilities() {
        return EnumSet.of(ProtocolAdapterCapability.READ);
    }
}
