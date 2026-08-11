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

import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.adapters.opcua.FakeEventService;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.milo.opcua.sdk.client.UaSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The reconnect callback decides whether a condition refresh happens after a session comes back.
 * <p>
 * It exists for the case nothing else covers: a reconnect whose subscription transfers successfully recreates
 * no monitored items, so the refresh that rides on re-establishing them never runs.
 */
class OpcUaSessionActivityListenerTest {

    /** One shared session: the listener only logs it, and the race test creates half a million listeners. */
    private static final @NotNull UaSession SESSION = mock(UaSession.class);

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

    @Test
    void aReconnectRacingTheCallbackRegistrationIsNeverLost() throws Exception {
        // Review finding 3. The handoff used to be a volatile callback plus a separate AtomicBoolean, which
        // makes each field's value visible but does not make check-then-act atomic. That admits an
        // interleaving where nobody is left responsible:
        //
        //   1. the session thread reads onReconnect and finds it null
        //   2. the connection thread stores the callback
        //   3. the connection thread tests missedReconnect, still false, and returns
        //   4. the session thread sets missedReconnect = true
        //
        // The flag now says a refresh is owed and the callback that would honour it is already installed.
        // Nothing runs it, and nothing notices until a later reconnect happens to consume the stale flag --
        // so the retained alarm picture stays as it was before the disconnect.
        //
        // Driven as a race rather than by instrumenting the fields: there is no seam to pause the session
        // thread between steps 1 and 4, and adding one would test the seam rather than the code.
        //
        // The window is two instructions wide, so the shape of this matters. Both threads are parked on a
        // spin over one volatile flag rather than on a barrier or a lock: a barrier's own wake-up jitter is
        // microseconds, which is three orders of magnitude wider than the window, and the two threads then
        // reliably miss each other. Spinning puts them into their critical sections within tens of
        // nanoseconds. Both are pinned to the same pair of threads across all rounds so no round pays for
        // thread creation.
        //
        // Verified against the pre-fix implementation, which this fails on -- at round 66,777 of a 200,000
        // round run, so roughly one hit per 70,000. A quarter of a million rounds therefore expects three or
        // four, which is enough to catch a regression on most runs and certain to across a few, at about
        // fifteen seconds. The asymmetry is what makes that trade acceptable: too few rounds risks a false
        // negative and never a false positive, because correct code cannot fail this assertion however the
        // threads interleave.
        // No Mockito anywhere in this loop, and that is a correctness point rather than a style one. A mock
        // records every invocation it receives so it can be verified later, so a *shared* mock taking one and
        // a half million calls grows without bound and slows to a crawl -- and a *per-round* mock costs more
        // to construct than everything under test. Half a million rounds needs the collaborators to be plain
        // no-ops; the listener does nothing with them that this test is about.
        final ProtocolAdapterState sharedState = new NoOpAdapterState();
        final ProtocolAdapterMetricsService sharedMetrics = new NoOpMetrics();
        final FakeEventService sharedEvents = new FakeEventService();

        final int rounds = 250_000;
        final ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < rounds; round++) {
                final OpcUaSessionActivityListener racing =
                        new OpcUaSessionActivityListener(sharedMetrics, sharedEvents, "test-adapter-id", sharedState);
                final AtomicInteger refreshes = new AtomicInteger();
                // Consume the initial connect, so the activation below counts as a reconnect.
                racing.onSessionActive(SESSION);

                final AtomicBoolean go = new AtomicBoolean();
                final Future<?> reconnect = threads.submit(() -> {
                    while (!go.get()) {
                        Thread.onSpinWait();
                    }
                    racing.onSessionActive(SESSION);
                });
                final Future<?> register = threads.submit(() -> {
                    while (!go.get()) {
                        Thread.onSpinWait();
                    }
                    racing.setOnReconnect(refreshes::incrementAndGet);
                });
                go.set(true);
                reconnect.get(10, TimeUnit.SECONDS);
                register.get(10, TimeUnit.SECONDS);

                // Exactly one, whichever thread won. If the callback was installed first the reconnect runs
                // it directly; if the reconnect arrived first the registration owes it and runs it then.
                assertThat(refreshes.get())
                        .as("round %d: a reconnect must produce exactly one condition refresh", round)
                        .isEqualTo(1);
            }
        } finally {
            threads.shutdownNow();
        }
    }

    /** A state that records nothing, for the half-million-round race loop. See that test for why. */
    private static final class NoOpAdapterState implements ProtocolAdapterState {

        private volatile @NotNull ConnectionStatus connectionStatus = ConnectionStatus.DISCONNECTED;
        private volatile @NotNull RuntimeStatus runtimeStatus = RuntimeStatus.STARTED;

        @Override
        public boolean setConnectionStatus(final @NotNull ConnectionStatus connectionStatus) {
            this.connectionStatus = connectionStatus;
            return true;
        }

        @Override
        public @NotNull ConnectionStatus getConnectionStatus() {
            return connectionStatus;
        }

        @Override
        public void setErrorConnectionStatus(final @Nullable Throwable throwable, final @Nullable String message) {}

        @Override
        public void reportErrorMessage(
                final @Nullable Throwable throwable, final @Nullable String message, final boolean sendEvent) {}

        @Override
        public void setRuntimeStatus(final @NotNull RuntimeStatus runtimeStatus) {
            this.runtimeStatus = runtimeStatus;
        }

        @Override
        public @NotNull RuntimeStatus getRuntimeStatus() {
            return runtimeStatus;
        }

        @Override
        public @Nullable String getLastErrorMessage() {
            return null;
        }
    }

    /** Metrics that count nothing, for the same reason. */
    private static final class NoOpMetrics implements ProtocolAdapterMetricsService {

        @Override
        public void incrementReadPublishSuccess() {}

        @Override
        public void incrementReadPublishFailure() {}

        @Override
        public void incrementWritePublishSuccess() {}

        @Override
        public void incrementWritePublishFailure() {}

        @Override
        public void incrementConnectionFailure() {}

        @Override
        public void incrementConnectionSuccess() {}

        @Override
        public void increment(final @NotNull String metricName) {}
    }

    private static @NotNull OpcUaSessionActivityListener unwiredListener() {
        final ProtocolAdapterState state = new ProtocolAdapterStateImpl(mock(), "test-adapter-id", "opcua");
        return new OpcUaSessionActivityListener(mock(), new FakeEventService(), "test-adapter-id", state);
    }
}
