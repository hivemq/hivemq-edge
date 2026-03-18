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
package com.hivemq.protocols.fsm;

import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.exceptions.ProtocolAdapterException;
import org.jetbrains.annotations.NotNull;

/**
 * Simplified protocol adapter interface for the FSM-based redesign.
 * <p>
 * Key differences from {@link com.hivemq.adapter.sdk.api.ProtocolAdapter}:
 * <ol>
 *   <li>{@link #connect} returns synchronously or throws - no CompletableFuture</li>
 *   <li>{@link #disconnect} returns synchronously or throws - no CompletableFuture</li>
 *   <li>Connection state is managed by the caller ({@link ProtocolAdapterWrapper2})</li>
 *   <li>Adapter should NOT manage its own state</li>
 * </ol>
 */
public interface ProtocolAdapter2 {

    /**
     * Get the adapter's unique identifier.
     */
    @NotNull
    String getId();

    /**
     * Get adapter information (protocol type, capabilities, etc.)
     */
    @NotNull
    ProtocolAdapterInformation getProtocolAdapterInformation();

    /**
     * Check if this adapter supports Southbound (MQTT to Device) communication.
     *
     * @return true if southbound is supported, false if this is a read-only adapter
     */
    default boolean supportsSouthbound() {
        return false;
    }

    /**
     * Validate configuration before connecting.
     * Called during the Precheck phase.
     *
     * @throws ProtocolAdapterException if configuration is invalid
     */
    void precheck() throws ProtocolAdapterException;

    /**
     * Establish connection to the device/service.
     * Called for BOTH northbound and southbound connections.
     * <p>
     * This method should:
     * <ul>
     *   <li>Establish the physical/logical connection</li>
     *   <li>Validate connectivity (handshake, auth, etc.)</li>
     *   <li>Return when connection is ready</li>
     *   <li>Throw on any failure</li>
     * </ul>
     *
     * @param direction the connection direction (northbound vs southbound)
     * @throws ProtocolAdapterException on connection failure
     */
    void connect(@NotNull ProtocolAdapterConnectionDirection direction) throws ProtocolAdapterException;

    /**
     * Disconnect from the device/service.
     * <p>
     * This method should:
     * <ul>
     *   <li>Gracefully close the connection</li>
     *   <li>Release resources</li>
     *   <li>Return when cleanup is complete</li>
     *   <li>NOT throw on failure (log errors instead)</li>
     * </ul>
     *
     * @param direction the connection direction (northbound vs southbound)
     */
    void disconnect(@NotNull ProtocolAdapterConnectionDirection direction);

    /**
     * Destroy the adapter instance.
     * Called after both connections are disconnected.
     * Release ALL resources including configuration.
     */
    void destroy();
}
