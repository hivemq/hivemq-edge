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

public enum ProtocolAdapterState {
    Starting(ProtocolAdapterState::transitionFromStarting),
    Started(ProtocolAdapterState::transitionFromStarted),
    Stopping(ProtocolAdapterState::transitionFromStopping),
    Stopped(ProtocolAdapterState::transitionFromStopped),
    Error(ProtocolAdapterState::transitionFromError),
    ;

    private final @NotNull Function<ProtocolAdapterState, ProtocolAdapterTransitionResponse> transitionFunction;

    ProtocolAdapterState(@NotNull final Function<ProtocolAdapterState, ProtocolAdapterTransitionResponse> transitionFunction) {
        this.transitionFunction = transitionFunction;
    }

    public static @NotNull ProtocolAdapterTransitionResponse transitionFromStarting(
            final @NotNull ProtocolAdapterState toState) {
        final ProtocolAdapterState fromState = ProtocolAdapterState.Starting;
        return switch (toState) {
            case Starting -> ProtocolAdapterTransitionResponse.notChanged(fromState);
            case Started, Stopping, Error -> ProtocolAdapterTransitionResponse.success(fromState, toState);
            default -> ProtocolAdapterTransitionResponse.failure(fromState, toState);
        };
    }

    public static @NotNull ProtocolAdapterTransitionResponse transitionFromStarted(
            final @NotNull ProtocolAdapterState toState) {
        final ProtocolAdapterState fromState = ProtocolAdapterState.Started;
        return switch (toState) {
            case Started -> ProtocolAdapterTransitionResponse.notChanged(fromState);
            case Stopping, Error -> ProtocolAdapterTransitionResponse.success(fromState, toState);
            default -> ProtocolAdapterTransitionResponse.failure(fromState, toState);
        };
    }

    public static @NotNull ProtocolAdapterTransitionResponse transitionFromStopping(
            final @NotNull ProtocolAdapterState toState) {
        final ProtocolAdapterState fromState = ProtocolAdapterState.Stopping;
        return switch (toState) {
            case Stopping -> ProtocolAdapterTransitionResponse.notChanged(fromState);
            case Stopped, Error -> ProtocolAdapterTransitionResponse.success(fromState, toState);
            default -> ProtocolAdapterTransitionResponse.failure(fromState, toState);
        };
    }

    public static @NotNull ProtocolAdapterTransitionResponse transitionFromStopped(
            final @NotNull ProtocolAdapterState toState) {
        final ProtocolAdapterState fromState = ProtocolAdapterState.Stopped;
        return switch (toState) {
            case Starting -> ProtocolAdapterTransitionResponse.success(fromState, toState);
            case Stopped -> ProtocolAdapterTransitionResponse.notChanged(fromState);
            default -> ProtocolAdapterTransitionResponse.failure(fromState, toState);
        };
    }

    public static @NotNull ProtocolAdapterTransitionResponse transitionFromError(
            final @NotNull ProtocolAdapterState toState) {
        final ProtocolAdapterState fromState = ProtocolAdapterState.Error;
        return switch (toState) {
            case Starting -> ProtocolAdapterTransitionResponse.success(fromState, toState);
            case Error -> ProtocolAdapterTransitionResponse.notChanged(fromState);
            default -> ProtocolAdapterTransitionResponse.failure(fromState, toState);
        };
    }

    public @NotNull ProtocolAdapterTransitionResponse transition(
            final @NotNull ProtocolAdapterState toState) {
        return transitionFunction.apply(toState);
    }

    public boolean isStarting() {
        return this == Starting;
    }

    public boolean isStarted() {
        return this == Started;
    }

    public boolean isStopping() {
        return this == Stopping;
    }

    public boolean isStopped() {
        return this == Stopped;
    }

    public boolean isError() {
        return this == Error;
    }
}
