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
import java.util.function.BooleanSupplier;
import org.eclipse.milo.opcua.sdk.client.SessionActivityListener;
import org.eclipse.milo.opcua.sdk.client.UaSession;
import org.jetbrains.annotations.NotNull;
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
     * Whether the connection this listener belongs to is still the one the adapter speaks through.
     * <p>
     * A listener belongs to one client, but {@link ProtocolAdapterState} belongs to the adapter, and the two
     * do not have the same lifetime: a superseded connection's session stays live until its close completes,
     * and Milo goes on reporting that session's activity throughout. Reporting it into the adapter's status
     * describes whichever connection is current, which by then is a different one — a stale
     * {@code DISCONNECTED} makes a healthy replacement look failed, and a stale {@code CONNECTED} hides a
     * genuine failure of it.
     * <p>
     * The event and the metric are not gated. Those record that something happened, and it did happen; the
     * status is a single slot describing the adapter now, and only its owner may set it.
     */
    private final @NotNull BooleanSupplier stillOwned;

    /** Whether this session belongs to a connection whose context has finished initial setup. */
    private final @NotNull BooleanSupplier connectionReady;

    /**
     * Who is going to run the refresh a reconnect owes. See {@link ReconnectHandoff} for why the two facts
     * involved cannot be two independent atomics, and why both of its methods answer with what to run
     * instead of running it.
     */
    private final @NotNull ReconnectHandoff handoff;

    public OpcUaSessionActivityListener(
            @NotNull final ProtocolAdapterMetricsService protocolAdapterMetricsService,
            @NotNull final EventService eventService,
            @NotNull final String adapterId,
            @NotNull final ProtocolAdapterState protocolAdapterState,
            @NotNull final BooleanSupplier stillOwned) {
        this(
                protocolAdapterMetricsService,
                eventService,
                adapterId,
                protocolAdapterState,
                stillOwned,
                () -> true,
                new ReconnectHandoff());
    }

    public OpcUaSessionActivityListener(
            @NotNull final ProtocolAdapterMetricsService protocolAdapterMetricsService,
            @NotNull final EventService eventService,
            @NotNull final String adapterId,
            @NotNull final ProtocolAdapterState protocolAdapterState,
            @NotNull final BooleanSupplier stillOwned,
            @NotNull final BooleanSupplier connectionReady) {
        this(
                protocolAdapterMetricsService,
                eventService,
                adapterId,
                protocolAdapterState,
                stillOwned,
                connectionReady,
                new ReconnectHandoff());
    }

    /** Takes the handoff, so the test that pins mutual exclusion can supply one it can pause. */
    OpcUaSessionActivityListener(
            @NotNull final ProtocolAdapterMetricsService protocolAdapterMetricsService,
            @NotNull final EventService eventService,
            @NotNull final String adapterId,
            @NotNull final ProtocolAdapterState protocolAdapterState,
            @NotNull final BooleanSupplier stillOwned,
            @NotNull final BooleanSupplier connectionReady,
            @NotNull final ReconnectHandoff handoff) {
        this.protocolAdapterMetricsService = protocolAdapterMetricsService;
        this.eventService = eventService;
        this.adapterId = adapterId;
        this.protocolAdapterState = protocolAdapterState;
        this.stillOwned = stillOwned;
        this.connectionReady = connectionReady;
        this.handoff = handoff;
    }

    /**
     * Reports a status, unless the connection this listener belongs to has already been replaced.
     * <p>
     * See {@link #stillOwned}. The adapter's own lifecycle paths report what is true of the adapter when they
     * run, so a dropped status here is never the last word on anything.
     */
    private void publishStatus(final ProtocolAdapterState.@NotNull ConnectionStatus status) {
        if (!stillOwned.getAsBoolean()) {
            log.debug(
                    "OPC UA adapter '{}': not reporting {} from a session whose connection the adapter has already replaced",
                    adapterId,
                    status);
            return;
        }
        protocolAdapterState.setConnectionStatus(status);
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
        publishStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
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
     * Installing the callback and claiming any pending reconnect happen as one transition — see
     * {@link ReconnectHandoff} for the interleaving that made two separate atomics wrong. The callback runs
     * <em>after</em> that transition, which is why the handoff returns it rather than running it: a refresh
     * is a server round trip, and holding the lock across it would block the session thread's next
     * activation for no benefit.
     */
    public void setOnReconnect(final @NotNull Runnable onReconnect) {
        final Runnable owed = handoff.install(onReconnect);
        if (owed != null) {
            log.debug(
                    "Adapter '{}': a reconnect arrived before the subscription handler was ready, requesting its condition refresh now",
                    adapterId);
            owed.run();
        }
    }

    @Override
    public void onSessionActive(final @NotNull UaSession session) {
        protocolAdapterMetricsService.increment(Constants.METRIC_SESSION_ACTIVE_COUNT);
        log.info("OPC UA client of protocol adapter '{}' connected: {}", adapterId, session);

        // A reconnect whose subscription transferred cleanly recreates nothing, so nothing else would ask the
        // server to re-report its retained conditions. Skipping the first activation avoids refreshing twice
        // on the initial connect, where creating the subscription already does it. The initial activation
        // also cannot publish CONNECTED: Milo calls it before verification, monitored-item creation, browse
        // preparation and context installation have finished. The connection itself owns that first
        // transition. Later activations may restore CONNECTED only after that initial setup was completed.
        if (!seenFirstActivation.compareAndSet(false, true)) {
            if (connectionReady.getAsBoolean()) {
                publishStatus(CONNECTED);
            } else {
                log.debug(
                        "OPC UA adapter '{}': session reactivated before initial connection setup completed; "
                                + "leaving the public status unchanged",
                        adapterId);
            }
            // Null means there was nothing to call yet, and the handoff has remembered the debt rather than
            // dropping it: the next setOnReconnect runs it on arrival.
            final Runnable reconnected = handoff.reconnected();
            if (reconnected != null) {
                // Outside the handoff's lock, because this is a server round trip.
                reconnected.run();
            }
        }
    }
}
