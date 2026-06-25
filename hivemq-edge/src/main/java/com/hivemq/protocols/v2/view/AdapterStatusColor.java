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
package com.hivemq.protocols.v2.view;

import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState;
import org.jetbrains.annotations.NotNull;

/**
 * The externally visible color of an adapter — a <b>pure function of the adapter machine state</b>
 * computed by {@link #of(ProtocolAdapterWrapperState)}. The ten machine states collapse onto six colors that the
 * REST surface and any UI render directly; no adapter owns this value.
 */
public enum AdapterStatusColor {

    /** {@code STOPPED}: the adapter is intentionally not running. */
    GREY_STOPPED,

    /** {@code WAITING_FOR_STARTED} / {@code WAITING_FOR_CONNECTED} / {@code WAITING_FOR_VERIFICATION}: coming up. */
    YELLOW_CONNECTING,

    /** {@code CONNECTED}: the adapter is up and its active aspects are operating. */
    GREEN_CONNECTED,

    /** {@code WAITING_FOR_CONNECTION_RETRY}: the connection dropped and a backoff retry is pending. */
    AMBER_RETRYING,

    /**
     * {@code WAITING_FOR_DISCONNECTED} / {@code WAITING_FOR_DISCONNECTED_BEFORE_RECONNECT} /
     * {@code WAITING_FOR_STOPPED}: winding down toward a stop or a reconnect.
     */
    YELLOW_STOPPING,

    /** {@code ERROR}: the adapter is in its terminal error state and needs manual recovery. */
    RED_ERROR;

    /**
     * Fold an adapter machine state into its externally visible color.
     *
     * @param state the adapter machine state.
     * @return the color.
     */
    public static @NotNull AdapterStatusColor of(final @NotNull ProtocolAdapterWrapperState state) {
        return switch (state) {
            case STOPPED -> GREY_STOPPED;
            case WAITING_FOR_STARTED, WAITING_FOR_CONNECTED, WAITING_FOR_VERIFICATION -> YELLOW_CONNECTING;
            case CONNECTED -> GREEN_CONNECTED;
            case WAITING_FOR_CONNECTION_RETRY -> AMBER_RETRYING;
            case WAITING_FOR_DISCONNECTED, WAITING_FOR_DISCONNECTED_BEFORE_RECONNECT, WAITING_FOR_STOPPED ->
                YELLOW_STOPPING;
            case ERROR -> RED_ERROR;
        };
    }
}
