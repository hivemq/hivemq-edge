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

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;
import static com.hivemq.edge.adapters.opcua.Constants.PROTOCOL_ID_OPCUA;

import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.adapters.opcua.Constants;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.milo.opcua.sdk.client.SessionActivityListener;
import org.eclipse.milo.opcua.sdk.client.UaSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpcUaSessionActivityListener implements SessionActivityListener {

    private static final Logger log = LoggerFactory.getLogger(OpcUaSessionActivityListener.class);

    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull EventService eventService;
    private final @NotNull String adapterId;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull AtomicBoolean seenFirstActivation = new AtomicBoolean(false);

    /**
     * Guards the reconnect handoff — {@link #onReconnect} and {@link #missedReconnect} together.
     * <p>
     * One lock rather than two atomics, because the two fields are not independent facts: they are two
     * halves of one state, "who is going to run the refresh". Reading one and writing the other has to be a
     * single transition or the handoff can fall between them. It did. With a volatile callback and a
     * separate {@code AtomicBoolean} this interleaving loses a reconnect outright:
     * <ol>
     *   <li>the session thread reads {@code onReconnect} and finds it null;</li>
     *   <li>the connection thread stores the callback;</li>
     *   <li>the connection thread tests {@code missedReconnect}, still false, and returns;</li>
     *   <li>the session thread sets {@code missedReconnect} to true.</li>
     * </ol>
     * The flag now says a reconnect is pending and the callback that would honour it is already installed,
     * so nobody is left to run it — and nothing notices until the <em>next</em> reconnect happens to
     * consume the stale flag. {@code volatile} makes each field's value visible; it does not make
     * check-then-act atomic, which is what this needs.
     */
    private final @NotNull Object reconnectLock = new Object();

    /** Set when a reconnect arrived before {@link #setOnReconnect} had anything to call. */
    private boolean missedReconnect;

    private @Nullable Runnable onReconnect;

    public OpcUaSessionActivityListener(
            @NotNull final ProtocolAdapterMetricsService protocolAdapterMetricsService,
            @NotNull final EventService eventService,
            @NotNull final String adapterId,
            @NotNull final ProtocolAdapterState protocolAdapterState) {
        this.protocolAdapterMetricsService = protocolAdapterMetricsService;
        this.eventService = eventService;
        this.adapterId = adapterId;
        this.protocolAdapterState = protocolAdapterState;
    }

    @Override
    public void onSessionInactive(final @NotNull UaSession session) {
        protocolAdapterMetricsService.increment(Constants.METRIC_SESSION_INACTIVE_COUNT);
        eventService
                .createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                .withSeverity(Event.SEVERITY.WARN)
                .withPayload(session.getSessionName() + '/' + session.getSessionId())
                .withMessage("Adapter '" + adapterId + "' session has been disconnected.")
                .fire();
        protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
        log.info("OPC UA client of protocol adapter '{}' disconnected: {}", adapterId, session);
    }

    /**
     * Called when a session becomes active again after this listener has already seen one.
     * <p>
     * Set by the connection once the subscription handler exists. Only reconnects are reported: the first
     * activation is the initial connect, where the subscription is created and already refreshes itself.
     * <p>
     * A reconnect can land <em>before</em> this is wired: the listener is registered as soon as the client
     * exists, while the handler it delegates to is built only after the subscription has been established —
     * and for condition tags that step makes blocking round trips per tag, so the window is not small. Such
     * a reconnect is remembered here and honoured now, rather than dropped for having arrived early. Without
     * that the refresh is lost twice over: no hook to run, and the activation still consumes
     * {@code seenFirstActivation}, so it is miscounted as the initial connect.
     * <p>
     * Installing the callback and claiming any pending reconnect happen as one transition under
     * {@link #reconnectLock} — see that field for the interleaving that made two separate atomics wrong.
     * The callback itself runs <em>after</em> the lock is released: it requests a condition refresh, which
     * is a server round trip, and holding a lock across it would block the session thread's next
     * activation for no benefit.
     */
    public void setOnReconnect(final @NotNull Runnable onReconnect) {
        final boolean owed;
        synchronized (reconnectLock) {
            this.onReconnect = onReconnect;
            owed = missedReconnect;
            missedReconnect = false;
        }
        if (owed) {
            log.debug(
                    "Adapter '{}': a reconnect arrived before the subscription handler was ready, requesting its condition refresh now",
                    adapterId);
            onReconnect.run();
        }
    }

    @Override
    public void onSessionActive(final @NotNull UaSession session) {
        protocolAdapterMetricsService.increment(Constants.METRIC_SESSION_ACTIVE_COUNT);
        protocolAdapterState.setConnectionStatus(CONNECTED);
        log.info("OPC UA client of protocol adapter '{}' connected: {}", adapterId, session);

        // A reconnect whose subscription transferred cleanly recreates nothing, so nothing else would ask the
        // server to re-report its retained conditions. Skipping the first activation avoids refreshing twice
        // on the initial connect, where creating the subscription already does it.
        if (!seenFirstActivation.compareAndSet(false, true)) {
            final Runnable reconnected;
            synchronized (reconnectLock) {
                reconnected = onReconnect;
                if (reconnected == null) {
                    // Nothing to call yet. Remembered rather than dropped: setOnReconnect runs it on
                    // arrival. Claiming the callback and recording the debt are one transition, so a
                    // registration racing this one cannot slip between them and leave nobody responsible.
                    missedReconnect = true;
                }
            }
            if (reconnected != null) {
                // Outside the lock, for the same reason as in setOnReconnect: this is a server round trip.
                reconnected.run();
            }
        }
    }
}
