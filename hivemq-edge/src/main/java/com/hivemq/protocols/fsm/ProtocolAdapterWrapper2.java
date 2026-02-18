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
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtocolAdapterWrapper2 {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolAdapterWrapper2.class);
    protected final @NotNull ProtocolAdapter adapter;
    protected volatile @NotNull ProtocolAdapterState state;
    protected volatile @NotNull ProtocolAdapterConnectionState northboundConnectionState;
    protected volatile @NotNull ProtocolAdapterConnectionState southboundConnectionState;

    public ProtocolAdapterWrapper2(final @NotNull ProtocolAdapter adapter) {
        this.adapter = adapter;
        northboundConnectionState = ProtocolAdapterConnectionState.Disconnected;
        southboundConnectionState = ProtocolAdapterConnectionState.Disconnected;
        state = ProtocolAdapterState.Idle;
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

    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapter.getProtocolAdapterInformation();
    }

    public boolean start() {
        LOGGER.info("Starting protocol adapter {}.", getAdapterId());
        ProtocolAdapterTransitionResponse response = transitionTo(ProtocolAdapterState.Precheck);
        if (response.status().isSuccess()) {
            boolean success = startNorthbound();
            success = success && startSouthbound();
            if (success) {
                response = transitionTo(ProtocolAdapterState.Working);
            } else {
                response = transitionTo(ProtocolAdapterState.Error);
            }
        }
        return response.status().isSuccess();
    }

    public boolean stop(final boolean destroy) {
        LOGGER.info("Stopping protocol adapter {}.", getAdapterId());
        ProtocolAdapterTransitionResponse response = transitionTo(ProtocolAdapterState.Stopping);
        if (response.status().isSuccess()) {
            final boolean southboundSuccess = stopSouthbound();
            final boolean northboundSuccess = stopNorthbound();
            if (northboundSuccess && southboundSuccess) {
                response = transitionTo(ProtocolAdapterState.Idle);
            } else {
                response = transitionTo(ProtocolAdapterState.Error);
            }
        }
        return response.status().isSuccess();
    }

    protected boolean startNorthbound() {
        LOGGER.info("Starting northbound for protocol adapter {}.", getAdapterId());
        ProtocolAdapterConnectionTransitionResponse response =
                transitionNorthboundConnectionTo(ProtocolAdapterConnectionState.Connecting);
        if (response.status().isSuccess()) {
            response = transitionNorthboundConnectionTo(ProtocolAdapterConnectionState.Connected);
        }
        return response.status().isSuccess();
    }

    protected boolean startSouthbound() {
        LOGGER.info("Starting southbound for protocol adapter {}.", getAdapterId());
        ProtocolAdapterConnectionTransitionResponse response =
                transitionSouthboundConnectionTo(ProtocolAdapterConnectionState.Connecting);
        if (response.status().isSuccess()) {
            response = transitionSouthboundConnectionTo(ProtocolAdapterConnectionState.Connected);
        }
        return response.status().isSuccess();
    }

    protected boolean stopNorthbound() {
        LOGGER.info("Stopping northbound for protocol adapter {}.", getAdapterId());
        ProtocolAdapterConnectionTransitionResponse response =
                transitionNorthboundConnectionTo(ProtocolAdapterConnectionState.Disconnecting);
        if (response.status().isSuccess()) {
            response = transitionNorthboundConnectionTo(ProtocolAdapterConnectionState.Disconnected);
        }
        return response.status().isSuccess();
    }

    protected boolean stopSouthbound() {
        LOGGER.info("Stopping southbound for protocol adapter {}.", getAdapterId());
        ProtocolAdapterConnectionTransitionResponse response =
                transitionSouthboundConnectionTo(ProtocolAdapterConnectionState.Disconnecting);
        if (response.status().isSuccess()) {
            response = transitionSouthboundConnectionTo(ProtocolAdapterConnectionState.Disconnected);
        }
        return response.status().isSuccess();
    }

    public synchronized @NotNull ProtocolAdapterTransitionResponse transitionTo(final @NotNull ProtocolAdapterState newState) {
        final ProtocolAdapterState fromState = state;
        final ProtocolAdapterTransitionResponse response = fromState.transition(newState);
        state = response.toState();
        switch (response.status()) {
            case Success -> {
                LOGGER.debug("Protocol adapter '{}' transitioned from {} to {} successfully.",
                        getAdapterId(),
                        fromState,
                        state);
            }
            case Failure -> {
                LOGGER.error("Protocol adapter '{}' failed to transition from {} to {}.",
                        getAdapterId(),
                        fromState,
                        state);
            }
            case NotChanged -> {
                LOGGER.warn("Protocol adapter '{}' state {} is unchanged.", getAdapterId(), state);
            }
        }
        return response;
    }

    public synchronized @NotNull ProtocolAdapterConnectionTransitionResponse transitionSouthboundConnectionTo(
            final @NotNull ProtocolAdapterConnectionState newState) {
        final ProtocolAdapterConnectionState fromState = southboundConnectionState;
        final ProtocolAdapterConnectionTransitionResponse response = fromState.transition(newState);
        southboundConnectionState = response.toState();
        switch (response.status()) {
            case Success -> {
                LOGGER.debug("Protocol adapter '{}' southbound connection transitioned from {} to {} successfully.",
                        getAdapterId(),
                        fromState,
                        southboundConnectionState);
            }
            case Failure -> {
                LOGGER.error("Protocol adapter '{}' southbound connection failed to transition from {} to {}.",
                        getAdapterId(),
                        fromState,
                        southboundConnectionState);
            }
            case NotChanged -> {
                LOGGER.warn("Protocol adapter '{}' southbound connection state {} is unchanged.",
                        getAdapterId(),
                        southboundConnectionState);
            }
        }
        return response;
    }

    public synchronized @NotNull ProtocolAdapterConnectionTransitionResponse transitionNorthboundConnectionTo(
            final @NotNull ProtocolAdapterConnectionState newState) {
        final ProtocolAdapterConnectionState fromState = northboundConnectionState;
        final ProtocolAdapterConnectionTransitionResponse response = fromState.transition(newState);
        northboundConnectionState = response.toState();
        switch (response.status()) {
            case Success -> {
                LOGGER.debug("Protocol adapter '{}' northbound connection transitioned from {} to {} successfully.",
                        getAdapterId(),
                        fromState,
                        northboundConnectionState);
            }
            case Failure -> {
                LOGGER.error("Protocol adapter '{}' northbound connection failed to transition from {} to {}.",
                        getAdapterId(),
                        fromState,
                        northboundConnectionState);
            }
            case NotChanged -> {
                LOGGER.warn("Protocol adapter '{}' northbound connection state {} is unchanged.",
                        getAdapterId(),
                        northboundConnectionState);
            }
        }
        return response;
    }
}
