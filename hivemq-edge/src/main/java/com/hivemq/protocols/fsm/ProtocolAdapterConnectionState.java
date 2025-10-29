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
    Closed(context -> 0),
    Closing(context -> 0),
    Connected(context -> 0),
    Connecting(context -> 0),
    Disconnected(context -> 0),
    Disconnecting(context -> 0),
    Error(context -> 0),
    ErrorClosing(context -> 0),
    ;

    private final @NotNull Function<Object, Integer> transitionFunction;

    ProtocolAdapterConnectionState(@NotNull final Function<Object, Integer> transitionFunction) {
        this.transitionFunction = transitionFunction;
    }

    public @NotNull Integer transition(
            final @NotNull ProtocolAdapterConnectionState targetState,
            final @NotNull Object context) {
        return transitionFunction.apply(context);
    }
}
