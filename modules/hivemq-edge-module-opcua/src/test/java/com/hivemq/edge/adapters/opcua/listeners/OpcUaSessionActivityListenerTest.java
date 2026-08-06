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
        final OpcUaSessionActivityListener unwired = unwiredListener();

        unwired.onSessionActive(mock(UaSession.class));
        unwired.onSessionActive(mock(UaSession.class));
    }

    @Test
    void aReconnectArrivingBeforeTheCallbackIsWiredIsNotLost() {
        // EDG-835: the listener is registered as soon as the client exists, but the handler it delegates to
        // is built only after the subscription is established -- and for condition tags that step makes
        // blocking round trips per tag, so the window is real. A reconnect landing in it used to be dropped
        // twice over: no callback to run, and the activation still consumed seenFirstActivation, so it was
        // miscounted as the initial connect.
        final OpcUaSessionActivityListener unwired = unwiredListener();
        final AtomicInteger late = new AtomicInteger();

        unwired.onSessionActive(mock(UaSession.class)); // the initial connect
        unwired.onSessionActive(mock(UaSession.class)); // a reconnect, with nothing wired yet

        unwired.setOnReconnect(late::incrementAndGet);

        assertThat(late)
                .as("the missed reconnect must be honoured once there is something to call")
                .hasValue(1);
    }

    @Test
    void aMissedReconnectIsReplayedOnlyOnce() {
        final OpcUaSessionActivityListener unwired = unwiredListener();
        final AtomicInteger late = new AtomicInteger();

        unwired.onSessionActive(mock(UaSession.class));
        unwired.onSessionActive(mock(UaSession.class));
        unwired.onSessionActive(mock(UaSession.class)); // two reconnects missed, not one

        unwired.setOnReconnect(late::incrementAndGet);

        // One refresh answers any number of missed reconnects: it re-reports the whole current picture, so
        // asking twice would only duplicate the burst.
        assertThat(late).hasValue(1);
    }

    @Test
    void nothingIsReplayedWhenNoReconnectWasMissed() {
        final OpcUaSessionActivityListener unwired = unwiredListener();
        final AtomicInteger late = new AtomicInteger();

        unwired.onSessionActive(mock(UaSession.class)); // the initial connect alone

        unwired.setOnReconnect(late::incrementAndGet);

        assertThat(late)
                .as("the initial connect refreshes on its own; replaying here would ask twice")
                .hasValue(0);
    }

    private static @NotNull OpcUaSessionActivityListener unwiredListener() {
        final ProtocolAdapterState state = new ProtocolAdapterStateImpl(mock(), "test-adapter-id", "opcua");
        return new OpcUaSessionActivityListener(mock(), new FakeEventService(), "test-adapter-id", state);
    }
}
