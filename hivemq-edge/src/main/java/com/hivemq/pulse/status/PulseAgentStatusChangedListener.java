/*
 *  Copyright 2019-present HiveMQ GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hivemq.pulse.status;

import com.hivemq.api.model.capabilities.Capability;
import com.hivemq.edge.HiveMQCapabilityService;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PulseAgentStatusChangedListener implements StatusProvider.StatusChangedListener {
    private final static @NotNull Capability CAPABILITY = new Capability("pulse-asset-management",
            "HiveMQ Pulse Agent Asset Management",
            "This enables HiveMQ Edge to be a HiveMQ Pulse Agent.");
    private final @NotNull HiveMQCapabilityService capabilityService;
    private @NotNull Status status;

    public PulseAgentStatusChangedListener(final @NotNull HiveMQCapabilityService capabilityService) {
        this.capabilityService = capabilityService;
        this.status = new Status(Status.ActivationStatus.DEACTIVATED, Status.ConnectionStatus.DISCONNECTED, List.of());
    }

    public @NotNull Status getStatus() {
        return status;
    }

    @Override
    public void onStatusChanged(@NotNull final Status status) {
        this.status = status;
        if (this.status.activationStatus() == Status.ActivationStatus.ACTIVATED) {
            capabilityService.addCapability(CAPABILITY);
        } else {
            capabilityService.removeCapability(CAPABILITY);
        }
    }
}
