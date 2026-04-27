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
package com.hivemq.edge.adapters.snmp;

import com.hivemq.adapter.sdk.api.ProtocolAdapterCategory;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.ProtocolAdapterTag;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.snmp.config.SnmpSpecificAdapterConfig;
import com.hivemq.edge.adapters.snmp.config.tag.SnmpTag;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Provides metadata about the SNMP protocol adapter.
 */
public class SnmpProtocolAdapterInformation implements ProtocolAdapterInformation {

    public static final ProtocolAdapterInformation INSTANCE = new SnmpProtocolAdapterInformation();
    private static final @NotNull Logger log = LoggerFactory.getLogger(SnmpProtocolAdapterInformation.class);

    public static final @NotNull String PROTOCOL_ID = "snmp";
    private static final int CURRENT_CONFIG_VERSION = 1;

    protected SnmpProtocolAdapterInformation() {
    }

    @Override
    public @NotNull String getProtocolName() {
        return "SNMP";
    }

    @Override
    public @NotNull String getProtocolId() {
        return PROTOCOL_ID;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "SNMP Protocol Adapter";
    }

    @Override
    public @NotNull String getDescription() {
        return "Connects HiveMQ Edge to SNMP-enabled devices for monitoring network infrastructure, " +
                "UPS systems, environmental sensors, and other SNMP agents.";
    }

    @Override
    public @NotNull String getUrl() {
        return "https://docs.hivemq.com/hivemq-edge/protocol-adapters.html#snmp-adapter";
    }

    @Override
    public @NotNull String getVersion() {
        return "${edge-version}";
    }

    @Override
    public @NotNull String getLogoUrl() {
        return "/images/snmp-icon.png";
    }

    @Override
    public @NotNull String getAuthor() {
        return "HiveMQ";
    }

    @Override
    public @Nullable ProtocolAdapterCategory getCategory() {
        return ProtocolAdapterCategory.CONNECTIVITY;
    }

    @Override
    public List<ProtocolAdapterTag> getTags() {
        return List.of(ProtocolAdapterTag.UDP, ProtocolAdapterTag.TCP);
    }

    @Override
    public @Nullable String getUiSchema() {
        try (final InputStream is = this.getClass()
                .getClassLoader()
                .getResourceAsStream("snmp-adapter-ui-schema.json")) {
            if (is == null) {
                log.warn("The UISchema for the SNMP Adapter could not be loaded from resources: Not found.");
                return null;
            }
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (final Exception e) {
            log.warn("The UISchema for the SNMP Adapter could not be loaded from resources:", e);
            return null;
        }
    }

    @Override
    public int getCurrentConfigVersion() {
        return CURRENT_CONFIG_VERSION;
    }

    @Override
    public @NotNull Class<? extends Tag> tagConfigurationClass() {
        return SnmpTag.class;
    }

    @Override
    public @NotNull Class<? extends ProtocolSpecificAdapterConfig> configurationClassNorthbound() {
        return SnmpSpecificAdapterConfig.class;
    }

    @Override
    public @NotNull Class<? extends ProtocolSpecificAdapterConfig> configurationClassNorthAndSouthbound() {
        return SnmpSpecificAdapterConfig.class;
    }
}
