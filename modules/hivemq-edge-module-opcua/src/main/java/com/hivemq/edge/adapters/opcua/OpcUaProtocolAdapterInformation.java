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
package com.hivemq.edge.adapters.opcua;

import com.hivemq.extension.sdk.api.adapters.ProtocolAdapterCategory;
import com.hivemq.extension.sdk.api.adapters.ProtocolAdapterInformation;
import com.hivemq.extension.sdk.api.adapters.ProtocolAdapterTag;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.List;

public class OpcUaProtocolAdapterInformation implements ProtocolAdapterInformation{

    public static final ProtocolAdapterInformation INSTANCE = new OpcUaProtocolAdapterInformation();

    private OpcUaProtocolAdapterInformation() {
    }

    @Override
    public @NotNull String getProtocolName() {
        return "OPC-UA Client";
    }

    @Override
    public @NotNull String getProtocolId() {
        return "opc-ua-client";
    }

    @Override
    public @NotNull String getDisplayName() {
        return "OPC-UA to MQTT Protocol Adapter";
    }

    @Override
    public @NotNull String getDescription() {
        return "Connects HiveMQ Edge to existing OPC-UA services as a client and enables a seamless exchange of data between MQTT and OPC-UA.";
    }

    @Override
    public @NotNull String getUrl() {
        return "https://docs.hivemq.com/hivemq-edge/protocol-adapters.html#opc-ua-adapter";
    }

    @Override
    public @NotNull String getVersion() {
        return "${edge-version}";
    }

    @Override
    public @NotNull String getLogoUrl() {
        return "/images/opc-ua-icon.jpg";
    }

    @Override
    public @NotNull String getAuthor() {
        return "HIVEMQ";
    }

    @Override
    public @Nullable ProtocolAdapterCategory getCategory() {
        return null;
    }

    @Override
    public @Nullable List<ProtocolAdapterTag> getTags() {
        return null;
    }
}
