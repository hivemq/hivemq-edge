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

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public enum ProtocolAdapterConnectionState {
    Disconnected(ProtocolAdapterConnectionState::transitionFromDisconnected),
    Connecting(ProtocolAdapterConnectionState::transitionFromConnecting),
    Connected(ProtocolAdapterConnectionState::transitionFromConnected),
    Error(ProtocolAdapterConnectionState::transitionFromError),
    Disconnecting(ProtocolAdapterConnectionState::transitionFromDisconnecting),
    ;

    private final @NotNull Function<ProtocolAdapterConnectionState, ProtocolAdapterConnectionTransitionResponse>
            transitionFunction;

    ProtocolAdapterConnectionState(@NotNull final Function<ProtocolAdapterConnectionState, ProtocolAdapterConnectionTransitionResponse> transitionFunction) {
        this.transitionFunction = transitionFunction;
    }

    public static @NotNull ProtocolAdapterConnectionTransitionResponse transitionFromDisconnected(
            final @NotNull ProtocolAdapterConnectionState toState) {
        final ProtocolAdapterConnectionState fromState = ProtocolAdapterConnectionState.Disconnected;
        return switch (toState) {
            case Disconnected -> ProtocolAdapterConnectionTransitionResponse.notChanged(fromState);
            case Connecting -> ProtocolAdapterConnectionTransitionResponse.success(fromState, toState);
            default -> ProtocolAdapterConnectionTransitionResponse.failure(fromState, toState);
        };
    }

    public static @NotNull ProtocolAdapterConnectionTransitionResponse transitionFromConnecting(
            final @NotNull ProtocolAdapterConnectionState toState) {
        final ProtocolAdapterConnectionState fromState = ProtocolAdapterConnectionState.Connecting;
        return switch (toState) {
            case Connecting -> ProtocolAdapterConnectionTransitionResponse.notChanged(fromState);
            case Connected, Error, Disconnecting ->
                    ProtocolAdapterConnectionTransitionResponse.success(fromState, toState);
            default -> ProtocolAdapterConnectionTransitionResponse.failure(fromState, toState);
        };
    }

    public static @NotNull ProtocolAdapterConnectionTransitionResponse transitionFromConnected(
            final @NotNull ProtocolAdapterConnectionState toState) {
        final ProtocolAdapterConnectionState fromState = ProtocolAdapterConnectionState.Connected;
        return switch (toState) {
            case Connected -> ProtocolAdapterConnectionTransitionResponse.notChanged(fromState);
            case Error, Disconnecting -> ProtocolAdapterConnectionTransitionResponse.success(fromState, toState);
            default -> ProtocolAdapterConnectionTransitionResponse.failure(fromState, toState);
        };
    }

    public static @NotNull ProtocolAdapterConnectionTransitionResponse transitionFromError(
            final @NotNull ProtocolAdapterConnectionState toState) {
        final ProtocolAdapterConnectionState fromState = ProtocolAdapterConnectionState.Error;
        return switch (toState) {
            case Error -> ProtocolAdapterConnectionTransitionResponse.notChanged(fromState);
            case Disconnecting -> ProtocolAdapterConnectionTransitionResponse.success(fromState, toState);
            default -> ProtocolAdapterConnectionTransitionResponse.failure(fromState, toState);
        };
    }

    public static @NotNull ProtocolAdapterConnectionTransitionResponse transitionFromDisconnecting(
            final @NotNull ProtocolAdapterConnectionState toState) {
        final ProtocolAdapterConnectionState fromState = ProtocolAdapterConnectionState.Disconnecting;
        return switch (toState) {
            case Disconnecting -> ProtocolAdapterConnectionTransitionResponse.notChanged(fromState);
            case Disconnected -> ProtocolAdapterConnectionTransitionResponse.success(fromState, toState);
            default -> ProtocolAdapterConnectionTransitionResponse.failure(fromState, toState);
        };
    }

    public @NotNull ProtocolAdapterConnectionTransitionResponse transition(
            final @NotNull ProtocolAdapterConnectionState toState) {
        return transitionFunction.apply(toState);
    }

    public boolean isDisconnected() {
        return this == Disconnected;
    }

    public boolean isConnecting() {
        return this == Connecting;
    }

    public boolean isConnected() {
        return this == Connected;
    }

    public boolean isError() {
        return this == Error;
    }

    public boolean isDisconnecting() {
        return this == Disconnecting;
    }
}
