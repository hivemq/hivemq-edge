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
package com.hivemq.edge.pulse.integration.api.management;

import com.hivemq.edge.pulse.integration.api.management.PulseAgentStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Management surface for the Pulse Agent integration. Exposes the current activation status, lets callers observe
 * status transitions, and allows them to activate or deactivate the agent.
 */
public interface PulseManagement {

    @NotNull
    PulseAgentStatus getStatus();

    /**
     * Adds a listener that will be notified when the status changes. Will be called once with the current state
     * when registered.
     *
     * @param listener the listener
     */
    void addStatusChangedListener(@NotNull StatusChangedListener listener);

    void removeStatusChangedListener(@NotNull StatusChangedListener listener);

    /**
     * Activates pulse with the given connection string.
     *
     * @param connectionString the pulse connection string
     * @return if the connection string is valid and the pulse could be activated
     */
    boolean activatePulse(@NotNull String connectionString);

    /**
     * Deactivates pulse.
     */
    void deactivatePulse();

    interface StatusChangedListener {
        void onStatusChanged(@NotNull PulseAgentStatus status);
    }
}
