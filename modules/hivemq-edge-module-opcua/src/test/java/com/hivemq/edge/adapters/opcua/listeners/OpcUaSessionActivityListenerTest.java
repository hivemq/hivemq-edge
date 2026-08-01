/*
 * Copyright 2023-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.edge.adapters.opcua.listeners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.adapters.opcua.FakeEventService;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.milo.opcua.sdk.client.UaSession;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The reconnect callback decides whether a condition refresh happens after a session comes back.
 * <p>
 * It exists for the case nothing else covers: a reconnect whose subscription transfers successfully recreates
 * no monitored items, so the refresh that rides on re-establishing them never runs.
 */
class OpcUaSessionActivityListenerTest {

    private @NotNull OpcUaSessionActivityListener listener;
    private @NotNull AtomicInteger reconnects;

    @BeforeEach
    void setUp() {
        final ProtocolAdapterState state = new ProtocolAdapterStateImpl(mock(), "test-adapter-id", "opcua");
        listener = new OpcUaSessionActivityListener(mock(), new FakeEventService(), "test-adapter-id", state);
        reconnects = new AtomicInteger();
        listener.setOnReconnect(reconnects::incrementAndGet);
    }

    @Test
    void theFirstActivationIsNotAReconnect() {
        // The initial connect creates the subscription, which refreshes on its own. Reporting it here too
        // would ask the server for the same burst twice.
        listener.onSessionActive(mock(UaSession.class));

        assertThat(reconnects).hasValue(0);
    }

    @Test
    void everyLaterActivationIsAReconnect() {
        listener.onSessionActive(mock(UaSession.class));
        listener.onSessionActive(mock(UaSession.class));
        listener.onSessionActive(mock(UaSession.class));

        assertThat(reconnects)
                .as("each reconnect needs its own refresh, not just the first")
                .hasValue(2);
    }

    @Test
    void aSessionGoingInactiveAndBackIsAReconnect() {
        listener.onSessionActive(mock(UaSession.class));
        listener.onSessionInactive(mock(UaSession.class));
        listener.onSessionActive(mock(UaSession.class));

        assertThat(reconnects).hasValue(1);
    }

    @Test
    void withNoCallbackNothingHappens() {
        // The callback is set after the listener is constructed, so an activation in between must not fail.
        final ProtocolAdapterState state = new ProtocolAdapterStateImpl(mock(), "test-adapter-id", "opcua");
        final OpcUaSessionActivityListener unwired =
                new OpcUaSessionActivityListener(mock(), new FakeEventService(), "test-adapter-id", state);

        unwired.onSessionActive(mock(UaSession.class));
        unwired.onSessionActive(mock(UaSession.class));
    }
}
