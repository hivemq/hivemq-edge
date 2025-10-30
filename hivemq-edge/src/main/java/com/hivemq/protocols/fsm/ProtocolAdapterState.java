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

    private final @NotNull BiFunction<ProtocolAdapterState, ProtocolAdapterInstance, ProtocolAdapterTransitionResponse>
            transitionFunction;

    ProtocolAdapterState(@NotNull final BiFunction<ProtocolAdapterState, ProtocolAdapterInstance, ProtocolAdapterTransitionResponse> transitionFunction) {
        this.transitionFunction = transitionFunction;
    }

    public static ProtocolAdapterTransitionResponse transitionFromStarted(
            final @NotNull ProtocolAdapterState toState,
            final @NotNull ProtocolAdapterInstance instance) {
        switch (toState) {
            case Starting:
                return new ProtocolAdapterTransitionResponse(ProtocolAdapterState.Starting);
            case Started:
                return new ProtocolAdapterTransitionResponse(ProtocolAdapterState.Started);
            case Stopping:
                return new ProtocolAdapterTransitionResponse(ProtocolAdapterState.Stopping);
            case Stopped:
                return new ProtocolAdapterTransitionResponse(ProtocolAdapterState.Stopped);
            default:
                return new ProtocolAdapterTransitionResponse(ProtocolAdapterState.Error);
        }
    }

    public static ProtocolAdapterTransitionResponse transitionFromStarting(
            final @NotNull ProtocolAdapterState toState,
            final @NotNull ProtocolAdapterInstance instance) {
        return switch (toState) {
            case Starting -> new ProtocolAdapterTransitionResponse(ProtocolAdapterState.Starting);
            case Started -> new ProtocolAdapterTransitionResponse(ProtocolAdapterState.Started);
            case Stopping -> new ProtocolAdapterTransitionResponse(ProtocolAdapterState.Stopping);
            case Stopped -> new ProtocolAdapterTransitionResponse(ProtocolAdapterState.Stopped);
            default -> new ProtocolAdapterTransitionResponse(ProtocolAdapterState.Error);
        };
    }

    public static @NotNull ProtocolAdapterTransitionResponse transitionFromStopped(
            final @NotNull ProtocolAdapterState toState,
            final @NotNull ProtocolAdapterInstance instance) {
        return switch (toState) {
            case Starting -> transitionFromStoppedToStarting(instance);
            case Started, Stopping -> transitionToError(ProtocolAdapterState.Stopped, toState);
            case Stopped -> transitionWithoutChanges(toState);
            case Error -> transitionFromStoppedToError(instance);
        };
    }

    public static ProtocolAdapterTransitionResponse transitionFromStoppedToError(final @NotNull ProtocolAdapterInstance instance) {
        try {
            // Do something to error.
//            instance.doSomething();
            return new ProtocolAdapterTransitionResponse(ProtocolAdapterState.Error);
        } catch (final Exception e) {
            return new ProtocolAdapterTransitionResponse(ProtocolAdapterState.Error,
                    ProtocolAdapterTransitionStatus.Failure,
                    "Failed transition from Stopped to Error.",
                    e);
        }
    }

    public static ProtocolAdapterTransitionResponse transitionFromStoppedToStarting(final @NotNull ProtocolAdapterInstance instance) {
        try {
            // Do something to start the protocol adapter.
            return new ProtocolAdapterTransitionResponse(ProtocolAdapterState.Starting);
        } catch (final Exception e) {
            return new ProtocolAdapterTransitionResponse(ProtocolAdapterState.Stopped,
                    ProtocolAdapterTransitionStatus.Failure,
                    "Failed transition from Stopped to Starting.",
                    e);
        }
    }

    public static @NotNull ProtocolAdapterTransitionResponse transitionWithoutChanges(final @NotNull ProtocolAdapterState toState) {
        return new ProtocolAdapterTransitionResponse(toState);
    }

    public static @NotNull ProtocolAdapterTransitionResponse transitionToError(
            final @NotNull ProtocolAdapterState fromState,
            final @NotNull ProtocolAdapterState toState) {
        return new ProtocolAdapterTransitionResponse(ProtocolAdapterState.Error,
                ProtocolAdapterTransitionStatus.Failure,
                "Unable to transition from " + fromState + " to " + toState + ".",
                null);
    }

    public static ProtocolAdapterTransitionResponse transitionFromStopping(
            final @NotNull ProtocolAdapterState toState,
            final @NotNull ProtocolAdapterInstance instance) {
        return switch (toState) {
            case Starting -> new ProtocolAdapterTransitionResponse(ProtocolAdapterState.Starting);
            case Started -> new ProtocolAdapterTransitionResponse(ProtocolAdapterState.Started);
            case Stopping -> new ProtocolAdapterTransitionResponse(ProtocolAdapterState.Stopping);
            case Stopped -> new ProtocolAdapterTransitionResponse(ProtocolAdapterState.Stopped);
            default -> new ProtocolAdapterTransitionResponse(ProtocolAdapterState.Error);
        };
    }

    public static ProtocolAdapterTransitionResponse transitionFromError(
            final @NotNull ProtocolAdapterState toState,
            final @NotNull ProtocolAdapterInstance instance) {
        return switch (toState) {
            case Starting -> new ProtocolAdapterTransitionResponse(ProtocolAdapterState.Starting);
            case Started -> new ProtocolAdapterTransitionResponse(ProtocolAdapterState.Started);
            case Stopping -> new ProtocolAdapterTransitionResponse(ProtocolAdapterState.Stopping);
            case Stopped -> new ProtocolAdapterTransitionResponse(ProtocolAdapterState.Stopped);
            default -> new ProtocolAdapterTransitionResponse(ProtocolAdapterState.Error);
        };
    }

    public @NotNull ProtocolAdapterTransitionResponse transition(
            final @NotNull ProtocolAdapterState toState,
            final @NotNull ProtocolAdapterInstance instance) {
        return transitionFunction.apply(toState, instance);
    }
}
