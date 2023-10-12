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
package com.hivemq.edge.adapters.http;

import com.hivemq.edge.modules.adapters.ProtocolAdapterConstants;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterCapability;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.edge.modules.config.CustomConfig;
import com.hivemq.edge.modules.adapters.impl.AbstractProtocolAdapterInformation;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;

/**
 * @author HiveMQ Adapter Generator
 */
public class HttpProtocolAdapterInformation 
    extends AbstractProtocolAdapterInformation {

    public static final ProtocolAdapterInformation INSTANCE = new HttpProtocolAdapterInformation();

    protected HttpProtocolAdapterInformation() {
    }

    @Override
    public @NotNull String getProtocolName() {
        return "HTTP(s) over TCP";
    }

    @Override
    public @NotNull String getProtocolId() {
        return "http";
    }

    @Override
    public @NotNull String getDisplayName() {
        return "HTTP(s) to MQTT Protocol Adapter";
    }

    @Override
    public @NotNull String getDescription() {
        return "Connects HiveMQ Edge to arbitrary web endpoint URLs via HTTP(s), consuming structured JSON or plain data.";
    }

    @Override
    public String getVersion() {
        return super.getVersion() + " (BETA)";
    }

    @Override
    public ProtocolAdapterConstants.CATEGORY getCategory() {
        return ProtocolAdapterConstants.CATEGORY.CONNECTIVITY;
    }

    @Override
    public byte getCapabilities() {
        return ProtocolAdapterCapability.READ;
    }

    @Override
    public List<ProtocolAdapterConstants.TAG> getTags() {
        return List.of(ProtocolAdapterConstants.TAG.INTERNET,
                ProtocolAdapterConstants.TAG.TCP,
                ProtocolAdapterConstants.TAG.WEB);
    }
}
