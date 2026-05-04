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
package com.hivemq.pulse.management;

import com.hivemq.edge.HiveMQCapabilityService;
import com.hivemq.edge.pulse.integration.api.PulseAgentBootstrapOutput;
import com.hivemq.edge.pulse.integration.api.PulseManagement;
import com.hivemq.pulse.status.PulseAgentStatusChangedListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;

/**
 * Edge-side {@link PulseAgentBootstrapOutput} sink that captures the {@link PulseManagement} produced by the Pulse
 * Agent integration during {@code afterPersistenceBootstrap} and exposes it to other Edge components (e.g. the REST
 * API).
 */
@Singleton
public class PulseManagementHolder implements PulseAgentBootstrapOutput {

    private final @NotNull AtomicReference<PulseManagement> managementRef = new AtomicReference<>();
    private final @NotNull PulseAgentStatusChangedListener edgeListener;

    @Inject
    public PulseManagementHolder(final @NotNull HiveMQCapabilityService capabilityService) {
        this.edgeListener = new PulseAgentStatusChangedListener(capabilityService);
    }

    @Override
    public void success(final @NotNull PulseManagement pulseManagement) {
        pulseManagement.addStatusChangedListener(edgeListener);
        managementRef.set(pulseManagement);
    }

    public @NotNull Optional<PulseManagement> get() {
        return Optional.ofNullable(managementRef.get());
    }
}
