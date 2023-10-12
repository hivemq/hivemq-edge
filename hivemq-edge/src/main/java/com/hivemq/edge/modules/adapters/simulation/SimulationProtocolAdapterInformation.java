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

import com.hivemq.edge.modules.adapters.ProtocolAdapterConstants;
import com.hivemq.edge.modules.adapters.impl.AbstractProtocolAdapterInformation;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterCapability;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.edge.modules.config.CustomConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;

public class SimulationProtocolAdapterInformation
        extends AbstractProtocolAdapterInformation {

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
    public @NotNull String getLogoUrl() {
        return "/images/hivemq-icon.png";
    }

    @Override
    public ProtocolAdapterConstants.CATEGORY getCategory() {
        return ProtocolAdapterConstants.CATEGORY.SIMULATION;
    }

    @Override
    public byte getCapabilities() {
        return ProtocolAdapterCapability.READ;
    }

}
