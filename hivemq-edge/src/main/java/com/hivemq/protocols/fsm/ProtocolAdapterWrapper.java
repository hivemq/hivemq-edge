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

public class ProtocolAdapterWrapper {
    protected @NotNull ProtocolAdapterState state;
    protected @NotNull ProtocolAdapterConnectionState connectionState;

    public ProtocolAdapterWrapper() {
        this.state = ProtocolAdapterState.Stopped;
        this.connectionState = ProtocolAdapterConnectionState.Closed;
    }

    public @NotNull ProtocolAdapterState getState() {
        return state;
    }

    public @NotNull ProtocolAdapterConnectionState getConnectionState() {
        return connectionState;
    }

    public @NotNull ProtocolAdapterTransitionStatus transitionTo(final @NotNull ProtocolAdapterState protocolAdapterState) {
        final ProtocolAdapterTransitionResult result = state.transition(protocolAdapterState, this);
        state = result.state();
        // Handle error (logging, throwing exception, etc.)
        switch (result.status()) {
            default -> {
                // TODO
            }
        }
        return result.status();
    }
}
