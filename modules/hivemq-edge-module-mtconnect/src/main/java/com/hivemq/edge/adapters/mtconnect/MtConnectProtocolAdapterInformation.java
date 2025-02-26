/*
 * Copyright 2023-present HiveMQ GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.hivemq.edge.adapters.mtconnect;


import com.hivemq.adapter.sdk.api.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.ProtocolAdapterCategory;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.ProtocolAdapterTag;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.mtconnect.config.MtConnectAdapterConfig;
import com.hivemq.edge.adapters.mtconnect.config.tag.MtConnectAdapterTag;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;

public class MtConnectProtocolAdapterInformation implements ProtocolAdapterInformation {

    public static final @NotNull ProtocolAdapterInformation INSTANCE = new MtConnectProtocolAdapterInformation();
    private static final @NotNull Logger LOG = LoggerFactory.getLogger(MtConnectProtocolAdapterInformation.class);
    private static final int CURRENT_CONFIG_VERSION = 1;
    private static final @NotNull String PROTOCOL_ID = "mtconnect";

    protected MtConnectProtocolAdapterInformation() {
    }

    @Override
    public @NotNull String getProtocolName() {
        return "MTConnect";
    }

    @Override
    public @NotNull String getProtocolId() {
        return PROTOCOL_ID;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "MTConnect Protocol Adapter";
    }

    @Override
    public @NotNull String getDescription() {
        return "Connects HiveMQ Edge to existing MTConnect devices.";
    }

    @Override
    public @NotNull String getUrl() {
        return "https://docs.hivemq.com/hivemq-edge/protocol-adapters.html#mtconnect-protocol-adapter";
    }

    @Override
    public @NotNull String getVersion() {
        return "${edge-version}";
    }

    @Override
    public @NotNull EnumSet<ProtocolAdapterCapability> getCapabilities() {
        return EnumSet.of(ProtocolAdapterCapability.READ);
    }

    @Override
    public @NotNull String getLogoUrl() {
        return "/images/mtconnect-icon.png";
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
        return List.of(ProtocolAdapterTag.INTERNET, ProtocolAdapterTag.TCP, ProtocolAdapterTag.WEB);
    }

    @Override
    public @Nullable String getUiSchema() {
        try (final InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("mtconnect-adapter-ui-schema.json")) {
            if (inputStream == null) {
                LOG.warn(
                        "The UISchema for the MTConnect Protocol Adapter could not be loaded from resources: Not found.");
                return null;
            }
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOG.warn("The UISchema for the MtConnect Protocol Adapter could not be loaded from resources:", e);
            return null;
        }
    }

    @Override
    public int getCurrentConfigVersion() {
        return CURRENT_CONFIG_VERSION;
    }

    @Override
    public @NotNull Class<? extends Tag> tagConfigurationClass() {
        return MtConnectAdapterTag.class;
    }

    @Override
    public @NotNull Class<? extends ProtocolSpecificAdapterConfig> configurationClassNorthbound() {
        return MtConnectAdapterConfig.class;
    }

    @Override
    public @NotNull Class<? extends ProtocolSpecificAdapterConfig> configurationClassNorthAndSouthbound() {
        return MtConnectAdapterConfig.class;
    }

}
