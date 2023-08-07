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

import com.hivemq.HiveMQEdgeGateway;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.edge.modules.config.CustomConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.util.ManifestUtils;

import static com.hivemq.configuration.info.SystemInformationImpl.DEVELOPMENT_VERSION;

public class SimulationProtocolAdapterInformation implements ProtocolAdapterInformation {

    public static final ProtocolAdapterInformation INSTANCE = new SimulationProtocolAdapterInformation();
    private final @NotNull String hivemqVersion;

    private SimulationProtocolAdapterInformation() {
        final String versionFromManifest =
                ManifestUtils.getValueFromManifest(HiveMQEdgeGateway.class, "HiveMQ-Edge-Version");
        if (versionFromManifest == null || versionFromManifest.length() < 1) {
            hivemqVersion = DEVELOPMENT_VERSION;
        } else {
            hivemqVersion = versionFromManifest;
        }
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
    public @NotNull String getName() {
        return "Simulated Edge Device";
    }

    @Override
    public @NotNull String getDescription() {
        return "Without needing to configure real devices, simulate traffic from an edge device into HiveMQ Edge.";
    }

    @Override
    public @NotNull String getUrl() {
        return "https://github.com/hivemq/hivemq-edge/wiki/Protocol-adapters#simulation-adapter";
    }

    @Override
    public @NotNull String getVersion() {
        return hivemqVersion;
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
    public @NotNull Class<? extends CustomConfig> getConfigClass() {
        return SimulationAdapterConfig.class;
    }
}
