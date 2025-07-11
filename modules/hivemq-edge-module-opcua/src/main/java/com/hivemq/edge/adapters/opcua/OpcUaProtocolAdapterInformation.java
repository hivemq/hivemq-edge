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

import com.hivemq.adapter.sdk.api.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.ProtocolAdapterCategory;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.ProtocolAdapterTag;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.opcua.config.BidirectionalOpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;

public class OpcUaProtocolAdapterInformation implements ProtocolAdapterInformation {

    public static final @NotNull ProtocolAdapterInformation INSTANCE = new OpcUaProtocolAdapterInformation();

    private static final @NotNull Logger log = LoggerFactory.getLogger(OpcUaProtocolAdapterInformation.class);


    private OpcUaProtocolAdapterInformation() {
    }

    @Override
    public @NotNull String getProtocolName() {
        return "OPC UA";
    }

    @Override
    public @NotNull String getProtocolId() {
        return Constants.PROTOCOL_ID_OPCUA;
    }

    @Override
    public @NotNull List<String> getLegacyProtocolIds() {
        return List.of("opc-ua-client");
    }

    @Override
    public @NotNull String getDisplayName() {
        return "OPC UA Protocol Adapter";
    }

    @Override
    public @NotNull String getDescription() {
        return "Supports Northbound and Southbound communicates from and to OPC UA.";
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
        return "HiveMQ";
    }

    @Override
    public @Nullable ProtocolAdapterCategory getCategory() {
        return ProtocolAdapterCategory.INDUSTRIAL;
    }

    @Override
    public @NotNull EnumSet<ProtocolAdapterCapability> getCapabilities() {
        return EnumSet.of(ProtocolAdapterCapability.READ,
                ProtocolAdapterCapability.WRITE,
                ProtocolAdapterCapability.DISCOVER,
                ProtocolAdapterCapability.COMBINE); // all of them
    }

    @Override
    public @Nullable List<ProtocolAdapterTag> getTags() {
        return null;
    }

    @Override
    public @Nullable String getUiSchema() {
        try (final InputStream is = this.getClass()
                .getClassLoader()
                .getResourceAsStream("opcua-adapter-ui-schema.json")) {
            if (is == null) {
                log.warn("The UISchema for the OPC UA Adapter could not be loaded from resources: Not found.");
                return null;
            }
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (final Exception e) {
            log.warn("The UISchema for the OPC UA Adapter could not be loaded from resources:", e);
            return null;
        }
    }


    @Override
    public @NotNull Class<? extends Tag> tagConfigurationClass() {
        return OpcuaTag.class;
    }

    @Override
    public @NotNull Class<? extends ProtocolSpecificAdapterConfig> configurationClassNorthbound() {
        return OpcUaSpecificAdapterConfig.class;
    }

    @Override
    public @NotNull Class<? extends ProtocolSpecificAdapterConfig> configurationClassNorthAndSouthbound() {
        return BidirectionalOpcUaSpecificAdapterConfig.class;
    }

    @Override
    public int getCurrentConfigVersion() {
        return Constants.CURRENT_CONFIG_VERSION;
    }
}
