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

import com.hivemq.adapter.sdk.api.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.ProtocolAdapterCategory;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.ProtocolAdapterTag;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.plc4x.config.tag.Plc4xTag;
import com.hivemq.edge.adapters.plc4x.types.siemens.config.S7SpecificAdapterConfig;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;

/**
 * @author HiveMQ Adapter Generator
 */
public class S7ProtocolAdapterInformation implements ProtocolAdapterInformation {

    public static final ProtocolAdapterInformation INSTANCE = new S7ProtocolAdapterInformation();
    public static final String PROTOCOL_ID = "s7";

    private static final @NotNull Logger log = LoggerFactory.getLogger(S7ProtocolAdapterInformation.class);
    private static final int CURRENT_CONFIG_VERSION = 1;

    protected S7ProtocolAdapterInformation() {
    }

    @Override
    public @NotNull String getProtocolName() {
        return "S7";
    }

    @Override
    public @NotNull String getProtocolId() {
        return PROTOCOL_ID;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "S7 Protocol Adapter";
    }

    @Override
    public @NotNull String getDescription() {
        return "Connects HiveMQ Edge to S7-300, S7-400, S7-1200, S7-1500 & LOGO devices.";
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
        return "/images/s7-icon.png";

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
    public List<ProtocolAdapterTag> getTags() {
        return List.of(ProtocolAdapterTag.TCP,
                ProtocolAdapterTag.AUTOMATION,
                ProtocolAdapterTag.FACTORY);
    }

    @Override
    public @NotNull EnumSet<ProtocolAdapterCapability> getCapabilities() {
        return EnumSet.of(ProtocolAdapterCapability.READ);
    }

    @Override
    public @Nullable String getUiSchema() {
        try (final InputStream is = this.getClass()
                .getClassLoader()
                .getResourceAsStream("s7-adapter-ui-schema.json")) {
            if (is == null) {
                log.warn("The UISchema for the S7 Adapter could not be loaded from resources: Not found.");
                return null;
            }
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (final Exception e) {
            log.warn("The UISchema for the S7 Adapter could not be loaded from resources:", e);
            return null;
        }
    }

    @Override
    public @NotNull Class<? extends Tag> tagConfigurationClass() {
        return Plc4xTag.class;
    }

    @Override
    public @NotNull Class<? extends ProtocolSpecificAdapterConfig> configurationClassNorthbound() {
        return S7SpecificAdapterConfig.class;
    }

    @Override
    public @NotNull Class<? extends ProtocolSpecificAdapterConfig> configurationClassNorthAndSouthbound() {
        return S7SpecificAdapterConfig.class;
    }

    @Override
    public int getCurrentConfigVersion() {
        return CURRENT_CONFIG_VERSION;
    }
}
