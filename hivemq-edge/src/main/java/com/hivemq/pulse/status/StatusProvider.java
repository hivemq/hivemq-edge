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
package com.hivemq.pulse.status;

import org.jetbrains.annotations.NotNull;

public interface StatusProvider {

    @NotNull
    Status getStatus();

    /**
     * Adds a listener that will be notified when the status changes.
     * Will be called once with current state when registered.
     *
     * @param listener the listener
     */
    void addStatusChangedListener(@NotNull StatusChangedListener listener);

    void removeStatusChangedListener(@NotNull StatusChangedListener listener);

    interface StatusChangedListener {
        void onStatusChanged(@NotNull Status status);
    }

    /**
     * Activates pulse with the given connection string.
     *
     * @param connectionString the pulse connection string
     * @return if the connection string is valid and the pulse could be activated
     */
    boolean activatePulse(final @NotNull String connectionString);

    /**
     * Deactivate pulse.
     */
    void deactivatePulse();
}
