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

import java.util.Map;

public record ProtocolAdapterTransitionResponse(ProtocolAdapterState fromState, ProtocolAdapterState toState,
                                                ProtocolAdapterTransitionStatus status, String message,
                                                Throwable error) {

    public static final String FROM_STATE = "fromState";
    public static final String TO_STATE = "toState";
    public static final String STATE = "state";

    public static ProtocolAdapterTransitionResponse success(
            final @NotNull ProtocolAdapterState fromState,
            final @NotNull ProtocolAdapterState toState) {
        return new ProtocolAdapterTransitionResponse(fromState,
                toState,
                ProtocolAdapterTransitionStatus.Success,
                I18nProtocolAdapterMessage.FSM_TRANSITION_SUCCESS_TRANSITIONED_FROM_STATE_TO_STATE.get(Map.of(FROM_STATE,
                        fromState.name(),
                        TO_STATE,
                        toState.name())),
                null);
    }

    public static ProtocolAdapterTransitionResponse notChanged(final @NotNull ProtocolAdapterState state) {
        return new ProtocolAdapterTransitionResponse(state,
                state,
                ProtocolAdapterTransitionStatus.NotChanged,
                I18nProtocolAdapterMessage.FSM_TRANSITION_SUCCESS_STATE_IS_NOT_TRANSITIONED.get(Map.of(STATE,
                        state.name())),
                null);
    }

    public static ProtocolAdapterTransitionResponse failure(final @NotNull ProtocolAdapterState state) {
        return new ProtocolAdapterTransitionResponse(state,
                ProtocolAdapterState.Error,
                ProtocolAdapterTransitionStatus.Failure,
                I18nProtocolAdapterMessage.FSM_TRANSITION_FAILURE_TRANSITIONED_FROM_STATE_TO_ERROR.get(Map.of(STATE,
                        state.name())),
                null);
    }

    public static ProtocolAdapterTransitionResponse failure(
            final @NotNull ProtocolAdapterState fromState,
            final @NotNull ProtocolAdapterState toState) {
        return new ProtocolAdapterTransitionResponse(fromState,
                toState,
                ProtocolAdapterTransitionStatus.Failure,
                I18nProtocolAdapterMessage.FSM_TRANSITION_FAILURE_UNABLE_TO_TRANSITION_FROM_STATE_TO_STATE.get(Map.of(
                        FROM_STATE,
                        fromState.name(),
                        TO_STATE,
                        toState.name())),
                null);
    }
}
