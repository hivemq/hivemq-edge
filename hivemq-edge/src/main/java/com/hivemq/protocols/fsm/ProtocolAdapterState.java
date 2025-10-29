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

import java.util.function.BiFunction;

public enum ProtocolAdapterState {
    Starting(ProtocolAdapterState::transitionFromStarting),
    Started(ProtocolAdapterState::transitionFromStarted),
    Stopping(ProtocolAdapterState::transitionFromStopping),
    Stopped(ProtocolAdapterState::transitionFromStopped),
    Error(ProtocolAdapterState::transitionFromError),
    ;

    private final @NotNull BiFunction<ProtocolAdapterState, ProtocolAdapterWrapper, ProtocolAdapterTransitionResult>
            transitionFunction;

    ProtocolAdapterState(@NotNull final BiFunction<ProtocolAdapterState, ProtocolAdapterWrapper, ProtocolAdapterTransitionResult> transitionFunction) {
        this.transitionFunction = transitionFunction;
    }

    public static ProtocolAdapterTransitionResult transitionFromStarted(
            final @NotNull ProtocolAdapterState targetState,
            final @NotNull ProtocolAdapterWrapper wrapper) {
        switch (targetState) {
            case Starting:
                return new ProtocolAdapterTransitionResult(ProtocolAdapterState.Starting);
            case Started:
                return new ProtocolAdapterTransitionResult(ProtocolAdapterState.Started);
            case Stopping:
                return new ProtocolAdapterTransitionResult(ProtocolAdapterState.Stopping);
            case Stopped:
                return new ProtocolAdapterTransitionResult(ProtocolAdapterState.Stopped);
            default:
                return new ProtocolAdapterTransitionResult(ProtocolAdapterState.Error);
        }
    }

    public static ProtocolAdapterTransitionResult transitionFromStarting(
            final @NotNull ProtocolAdapterState targetState,
            final @NotNull ProtocolAdapterWrapper wrapper) {
        switch (targetState) {
            case Starting:
                return new ProtocolAdapterTransitionResult(ProtocolAdapterState.Starting);
            case Started:
                return new ProtocolAdapterTransitionResult(ProtocolAdapterState.Started);
            case Stopping:
                return new ProtocolAdapterTransitionResult(ProtocolAdapterState.Stopping);
            case Stopped:
                return new ProtocolAdapterTransitionResult(ProtocolAdapterState.Stopped);
            default:
                return new ProtocolAdapterTransitionResult(ProtocolAdapterState.Error);
        }
    }

    public static ProtocolAdapterTransitionResult transitionFromStopped(
            final @NotNull ProtocolAdapterState targetState,
            final @NotNull ProtocolAdapterWrapper wrapper) {
        switch (targetState) {
            case Starting:
                return new ProtocolAdapterTransitionResult(ProtocolAdapterState.Starting);
            case Started:
                return new ProtocolAdapterTransitionResult(ProtocolAdapterState.Started);
            case Stopping:
                return new ProtocolAdapterTransitionResult(ProtocolAdapterState.Stopping);
            case Stopped:
                return new ProtocolAdapterTransitionResult(ProtocolAdapterState.Stopped);
            default:
                return new ProtocolAdapterTransitionResult(ProtocolAdapterState.Error);
        }
    }

    public static ProtocolAdapterTransitionResult transitionFromStopping(
            final @NotNull ProtocolAdapterState targetState,
            final @NotNull ProtocolAdapterWrapper wrapper) {
        switch (targetState) {
            case Starting:
                return new ProtocolAdapterTransitionResult(ProtocolAdapterState.Starting);
            case Started:
                return new ProtocolAdapterTransitionResult(ProtocolAdapterState.Started);
            case Stopping:
                return new ProtocolAdapterTransitionResult(ProtocolAdapterState.Stopping);
            case Stopped:
                return new ProtocolAdapterTransitionResult(ProtocolAdapterState.Stopped);
            default:
                return new ProtocolAdapterTransitionResult(ProtocolAdapterState.Error);
        }
    }

    public static ProtocolAdapterTransitionResult transitionFromError(
            final @NotNull ProtocolAdapterState targetState,
            final @NotNull ProtocolAdapterWrapper wrapper) {
        switch (targetState) {
            case Starting:
                return new ProtocolAdapterTransitionResult(ProtocolAdapterState.Starting);
            case Started:
                return new ProtocolAdapterTransitionResult(ProtocolAdapterState.Started);
            case Stopping:
                return new ProtocolAdapterTransitionResult(ProtocolAdapterState.Stopping);
            case Stopped:
                return new ProtocolAdapterTransitionResult(ProtocolAdapterState.Stopped);
            default:
                return new ProtocolAdapterTransitionResult(ProtocolAdapterState.Error);
        }
    }

    public @NotNull ProtocolAdapterTransitionResult transition(
            final @NotNull ProtocolAdapterState targetState,
            final @NotNull ProtocolAdapterWrapper wrapper) {
        return transitionFunction.apply(targetState, wrapper);
    }
}
