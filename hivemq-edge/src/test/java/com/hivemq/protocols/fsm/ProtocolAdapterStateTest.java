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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ProtocolAdapterStateTest {
    private static final Map<ProtocolAdapterState, Set<ProtocolAdapterState>> PROTOCOL_ADAPTER_STATE_MAP_MAP = Map.of(
            ProtocolAdapterState.Starting,
            Set.of(ProtocolAdapterState.Started, ProtocolAdapterState.Stopping, ProtocolAdapterState.Error),
            ProtocolAdapterState.Started,
            Set.of(ProtocolAdapterState.Stopping, ProtocolAdapterState.Error),
            ProtocolAdapterState.Stopping,
            Set.of(ProtocolAdapterState.Stopped, ProtocolAdapterState.Error),
            ProtocolAdapterState.Stopped,
            Set.of(ProtocolAdapterState.Starting),
            ProtocolAdapterState.Error,
            Set.of(ProtocolAdapterState.Starting));
    @Mock
    private @NotNull ProtocolAdapterInstance protocolAdapterInstance;

    @Test
    public void whenEverythingWorks_thenTransitionShouldWork() {
        final List<ProtocolAdapterState> states = List.of(ProtocolAdapterState.values());
        states.forEach(fromState -> {
            final Set<ProtocolAdapterState> possibleToStates = PROTOCOL_ADAPTER_STATE_MAP_MAP.get(fromState);
            assertThat(possibleToStates).isNotNull();
            states.forEach(toState -> {
                final ProtocolAdapterTransitionResponse response =
                        fromState.transition(toState, protocolAdapterInstance);
                switch (response.status()) {
                    case Success -> {
                        assertThat(possibleToStates).contains(toState);
                        assertThat(response.message()).isEqualTo(StringTemplate.format(
                                "Transitioned from ${fromState} to ${toState}.",
                                Map.of("fromState", fromState, "toState", toState)));
                    }
                    case Failure -> {
                        assertThat(possibleToStates).doesNotContain(toState);
                        assertThat(response.message()).isEqualTo(StringTemplate.format(
                                "Unable to transition from ${fromState} to ${toState}.",
                                Map.of("fromState", fromState, "toState", toState)));
                    }
                    case NotChanged -> {
                        assertThat(toState).isEqualTo(fromState);
                        assertThat(response.message()).isEqualTo(StringTemplate.format("${state} is unchanged.",
                                Map.of("state", fromState)));
                    }
                }
            });
        });
    }
}
