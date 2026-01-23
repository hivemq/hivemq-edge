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
    Idle(ProtocolAdapterState::transitionFromIdle),
    Precheck(ProtocolAdapterState::transitionFromPrecheck),
    Working(ProtocolAdapterState::transitionFromWorking),
    Stopping(ProtocolAdapterState::transitionFromStopping),
    Error(ProtocolAdapterState::transitionFromError),
    ;

    private final @NotNull Function<ProtocolAdapterState, ProtocolAdapterTransitionResponse> transitionFunction;

    ProtocolAdapterState(@NotNull final Function<ProtocolAdapterState, ProtocolAdapterTransitionResponse> transitionFunction) {
        this.transitionFunction = transitionFunction;
    }

    public static @NotNull ProtocolAdapterTransitionResponse transitionFromIdle(
            final @NotNull ProtocolAdapterState toState) {
        final ProtocolAdapterState fromState = ProtocolAdapterState.Idle;
        return switch (toState) {
            case Idle -> ProtocolAdapterTransitionResponse.notChanged(fromState);
            case Precheck -> ProtocolAdapterTransitionResponse.success(fromState, toState);
            default -> ProtocolAdapterTransitionResponse.failure(fromState, toState);
        };
    }

    public static @NotNull ProtocolAdapterTransitionResponse transitionFromPrecheck(
            final @NotNull ProtocolAdapterState toState) {
        final ProtocolAdapterState fromState = ProtocolAdapterState.Precheck;
        return switch (toState) {
            case Precheck -> ProtocolAdapterTransitionResponse.notChanged(fromState);
            case Working, Error -> ProtocolAdapterTransitionResponse.success(fromState, toState);
            default -> ProtocolAdapterTransitionResponse.failure(fromState, toState);
        };
    }

    public static @NotNull ProtocolAdapterTransitionResponse transitionFromWorking(
            final @NotNull ProtocolAdapterState toState) {
        final ProtocolAdapterState fromState = ProtocolAdapterState.Working;
        return switch (toState) {
            case Working -> ProtocolAdapterTransitionResponse.notChanged(fromState);
            case Stopping, Error -> ProtocolAdapterTransitionResponse.success(fromState, toState);
            default -> ProtocolAdapterTransitionResponse.failure(fromState, toState);
        };
    }

    public static @NotNull ProtocolAdapterTransitionResponse transitionFromStopping(
            final @NotNull ProtocolAdapterState toState) {
        final ProtocolAdapterState fromState = ProtocolAdapterState.Stopping;
        return switch (toState) {
            case Stopping -> ProtocolAdapterTransitionResponse.notChanged(fromState);
            case Idle -> ProtocolAdapterTransitionResponse.success(fromState, toState);
            default -> ProtocolAdapterTransitionResponse.failure(fromState, toState);
        };
    }

    public static @NotNull ProtocolAdapterTransitionResponse transitionFromError(
            final @NotNull ProtocolAdapterState toState) {
        final ProtocolAdapterState fromState = ProtocolAdapterState.Error;
        return switch (toState) {
            case Error -> ProtocolAdapterTransitionResponse.notChanged(fromState);
            case Idle -> ProtocolAdapterTransitionResponse.success(fromState, toState);
            default -> ProtocolAdapterTransitionResponse.failure(fromState, toState);
        };
    }

    public @NotNull ProtocolAdapterTransitionResponse transition(
            final @NotNull ProtocolAdapterState toState) {
        return transitionFunction.apply(toState);
    }

    public boolean isIdle() {
        return this == Idle;
    }

    public boolean isPrecheck() {
        return this == Precheck;
    }

    public boolean isWorking() {
        return this == Working;
    }

    public boolean isStopping() {
        return this == Stopping;
    }

    public boolean isError() {
        return this == Error;
    }
}
