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

import com.hivemq.common.i18n.StringTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ProtocolAdapterConnectionStateTest {
    private static final Map<ProtocolAdapterConnectionState, Set<ProtocolAdapterConnectionState>>
            PROTOCOL_ADAPTER_CONNECTION_STATE_MAP = Map.of(
            ProtocolAdapterConnectionState.Disconnected,
            Set.of(ProtocolAdapterConnectionState.Connecting),
            ProtocolAdapterConnectionState.Connecting,
            Set.of(ProtocolAdapterConnectionState.Connected,
                    ProtocolAdapterConnectionState.Error,
                    ProtocolAdapterConnectionState.Disconnecting),
            ProtocolAdapterConnectionState.Connected,
            Set.of(ProtocolAdapterConnectionState.Error, ProtocolAdapterConnectionState.Disconnecting),
            ProtocolAdapterConnectionState.Error,
            Set.of(ProtocolAdapterConnectionState.Disconnecting),
            ProtocolAdapterConnectionState.Disconnecting,
            Set.of(ProtocolAdapterConnectionState.Disconnected));

    @Test
    public void whenEverythingWorks_thenTransitionShouldWork() {
        final List<ProtocolAdapterConnectionState> states = List.of(ProtocolAdapterConnectionState.values());
        states.forEach(fromState -> {
            final Set<ProtocolAdapterConnectionState> possibleToStates =
                    PROTOCOL_ADAPTER_CONNECTION_STATE_MAP.get(fromState);
            assertThat(possibleToStates).isNotNull();
            states.forEach(toState -> {
                final ProtocolAdapterConnectionTransitionResponse response = fromState.transition(toState);
                switch (response.status()) {
                    case Success -> {
                        assertThat(possibleToStates).contains(toState);
                        assertThat(response.message()).isEqualTo(StringTemplate.format(
                                "Connection transitioned from ${fromState} to ${toState}.",
                                Map.of("fromState", fromState, "toState", toState)));
                    }
                    case Failure -> {
                        assertThat(possibleToStates).doesNotContain(toState);
                        assertThat(response.message()).isEqualTo(StringTemplate.format(
                                "Unable to transition connection from ${fromState} to ${toState}.",
                                Map.of("fromState", fromState, "toState", toState)));
                    }
                    case NotChanged -> {
                        assertThat(toState).isEqualTo(fromState);
                        assertThat(response.message()).isEqualTo(StringTemplate.format(
                                "Connection ${state} is unchanged.",
                                Map.of("state", fromState)));
                    }
                }
            });
        });
    }
}
