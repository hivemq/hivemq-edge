/*
 * Copyright 2019-present HiveMQ GmbH
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
package com.hivemq.edge.modules.adapters.simulation;

import com.hivemq.adapter.sdk.api.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.ProtocolAdapterCategory;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.ProtocolAdapterTag;
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.modules.adapters.simulation.config.SimulationAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;

public class SimulationProtocolAdapterInformation implements ProtocolAdapterInformation {

    private static final Logger log = LoggerFactory.getLogger(SimulationProtocolAdapterInformation.class);

    public static final ProtocolAdapterInformation INSTANCE = new SimulationProtocolAdapterInformation();

    private SimulationProtocolAdapterInformation() {
    }

    @Override
    public @NotNull String getProtocolName() {
        return "Simulation";
    }

    @Override
    public @NotNull String getProtocolId() {
        return "simulation";
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Simulated Edge Device";
    }

    @Override
    public @NotNull String getDescription() {
        return "Without needing to configure real devices, simulate traffic from an edge device into HiveMQ Edge.";
    }

    @Override
    public @NotNull String getUrl() {
        return "https://docs.hivemq.com/hivemq-edge/protocol-adapters.html#simulation-adapter";
    }

    @Override
    public @NotNull String getVersion() {
        return "${edge-version}";
    }

    @Override
    public @NotNull String getLogoUrl() {
        return "/images/hivemq-icon.png";
    }

    @Override
    public @NotNull String getAuthor() {
        return "HiveMQ";
    }

    @Override
    public ProtocolAdapterCategory getCategory() {
        return ProtocolAdapterCategory.SIMULATION;
    }

    @Override
    public @Nullable List<ProtocolAdapterTag> getTags() {
        return List.of();
    }

    @Override
    public @NotNull EnumSet<ProtocolAdapterCapability> getCapabilities() {
        return EnumSet.of(ProtocolAdapterCapability.READ);
    }

    @Override
    public @Nullable String getUiSchema() {
        try (final InputStream is = this.getClass()
                .getClassLoader()
                .getResourceAsStream("simulation-adapter-ui-schema.json")) {
            if (is == null) {
                log.warn("The UISchema for the Simulation Adapter could not be loaded from resources: Not found.");
                return null;
            }
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("The UISchema for the Simulation Adapter could not be loaded from resources:", e);
            return null;
        }
    }


    @Override
    public @NotNull Class<? extends Tag> tagConfigurationClass() {
        return null; //TODO what would be correct here?
    }

    @Override
    public @NotNull Class<? extends ProtocolAdapterConfig> configurationClassReading() {
        return SimulationAdapterConfig.class;
    }

    @Override
    public @NotNull Class<? extends ProtocolAdapterConfig> configurationClassWriting() {
        return SimulationAdapterConfig.class;
    }
}
