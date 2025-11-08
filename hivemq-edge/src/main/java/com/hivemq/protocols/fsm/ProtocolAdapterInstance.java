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

package com.hivemq.protocols.fsm;

import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtocolAdapterInstance {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolAdapterInstance.class);
    protected final @NotNull ProtocolAdapter adapter;
    protected volatile @NotNull ProtocolAdapterState state;
    protected volatile @NotNull ProtocolAdapterConnectionState northboundConnectionState;
    protected volatile @NotNull ProtocolAdapterConnectionState southboundConnectionState;

    public ProtocolAdapterInstance(final @NotNull ProtocolAdapter adapter) {
        this.adapter = adapter;
        northboundConnectionState = ProtocolAdapterConnectionState.Closed;
        southboundConnectionState = ProtocolAdapterConnectionState.Closed;
        state = ProtocolAdapterState.Stopped;
    }

    public @NotNull ProtocolAdapterConnectionState getSouthboundConnectionState() {
        return southboundConnectionState;
    }

    public @NotNull ProtocolAdapterState getState() {
        return state;
    }

    public @NotNull ProtocolAdapterConnectionState getNorthboundConnectionState() {
        return northboundConnectionState;
    }

    public @NotNull String getAdapterId() {
        return adapter.getId();
    }

    public void start() {
        final ProtocolAdapterTransitionResponse response = transitionTo(ProtocolAdapterState.Starting);
        if (response.status().isSuccess()) {
            startNorthbound();
            startSouthbound();
        }
    }

    public void stop() {
        transitionTo(ProtocolAdapterState.Stopping);
    }

    protected void startNorthbound() {

    }

    protected void startSouthbound() {

    }

    public synchronized @NotNull ProtocolAdapterTransitionResponse transitionTo(final @NotNull ProtocolAdapterState newState) {
        final ProtocolAdapterState fromState = state;
        final ProtocolAdapterTransitionResponse response = fromState.transition(newState, this);
        state = response.toState();
        switch (response.status()) {
            case Success -> {
                LOGGER.debug("Protocol adapter '{}' transitioned from {} to {} successfully.",
                        fromState,
                        state,
                        getAdapterId());
            }
            case Failure -> {
                LOGGER.error("Protocol adapter '{}' failed to transition from {} to {}.",
                        fromState,
                        state,
                        getAdapterId());
            }
            case NotChanged -> {
                LOGGER.warn("Protocol adapter '{}' state {} is unchanged.", state, getAdapterId());
            }
        }
        return response;
    }
}
