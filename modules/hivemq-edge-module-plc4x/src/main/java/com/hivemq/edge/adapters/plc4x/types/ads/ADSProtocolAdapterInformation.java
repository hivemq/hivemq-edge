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
package com.hivemq.edge.adapters.plc4x.types.ads;

import com.hivemq.adapter.sdk.api.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.ProtocolAdapterCategory;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.ProtocolAdapterTag;
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.plc4x.config.Plc4xAdapterConfig;
import com.hivemq.edge.adapters.plc4x.config.tag.Plc4xTag;
import com.hivemq.edge.adapters.plc4x.types.ads.config.ADSAdapterConfig;
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
public class ADSProtocolAdapterInformation
    implements ProtocolAdapterInformation {

    public static final ProtocolAdapterInformation INSTANCE = new ADSProtocolAdapterInformation();
    private static final @NotNull Logger log = LoggerFactory.getLogger(ADSProtocolAdapterInformation.class);

    protected ADSProtocolAdapterInformation() {
    }

    @Override
    public @NotNull String getProtocolName() {
        return "ADS";
    }

    @Override
    public @NotNull String getProtocolId() {
        return "ads";
    }

    @Override
    public @NotNull String getDisplayName() {
        return "ADS to MQTT Protocol Adapter";
    }

    @Override
    public @NotNull String getDescription() {
        return "Connects HiveMQ Edge to devices supporting ADS, reading data from the PLC into MQTT.";
    }

    @Override
    public @NotNull String getUrl() {
        return "https://docs.hivemq.com/hivemq-edge/protocol-adapters.html#ads-adapter";
    }

    @Override
    public @NotNull String getVersion() {
        return "${edge-version}";
    }

    @Override
    public @NotNull String getLogoUrl() {
        return "/images/beckhoff-icon.gif";
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
                ProtocolAdapterTag.IIOT,
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
                .getResourceAsStream("ads-adapter-ui-schema.json")) {
            if (is == null) {
                log.warn("The UISchema for the ADS Adapter could not be loaded from resources: Not found.");
                return null;
            }
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("The UISchema for the ADS Adapter could not be loaded from resources:", e);
            return null;
        }
    }

    @Override
    public @NotNull Class<? extends Tag> tagConfigurationClass() {
        return Plc4xTag.class;
    }

    @Override
    public @NotNull Class<? extends ProtocolAdapterConfig> configurationClassReading() {
        return ADSAdapterConfig.class;
    }

    @Override
    public @NotNull Class<? extends ProtocolAdapterConfig> configurationClassWriting() {
        return ADSAdapterConfig.class;
    }
}
