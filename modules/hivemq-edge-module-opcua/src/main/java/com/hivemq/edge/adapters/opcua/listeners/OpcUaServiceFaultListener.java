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

import static com.hivemq.edge.adapters.opcua.Constants.PROTOCOL_ID_OPCUA;

import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.edge.adapters.opcua.Constants;
import java.util.function.BooleanSupplier;
import org.eclipse.milo.opcua.sdk.client.ServiceFaultListener;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.structured.ServiceFault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reports service faults raised by an OPC UA server, and recovers from the ones worth recovering from.
 * <p>
 * A service fault is how an OPC UA server rejects a request: the response carries a status code instead of a
 * result. Milo hands every one of them to the listeners registered on the client that made the request. This
 * listener counts them all, and then splits them. A handful of status codes mean the session or subscription
 * no longer exists, so nothing will work again until the client reconnects — those are logged at ERROR, raise
 * an adapter event, and trigger a reconnect. Everything else is logged at WARN with an event and no action.
 * <p>
 * <b>One instance is built per connection, not per adapter.</b> The adapter builds a template carrying its
 * settings; each connection derives its own copy with {@link #forConnection} and registers that copy on its
 * own client. The copy is bound at construction to the connection it serves, which is what lets it tell a
 * genuine fault from the noise a closing connection makes — see {@link #discarded}.
 */
public class OpcUaServiceFaultListener implements ServiceFaultListener {

    private static final Logger log = LoggerFactory.getLogger(OpcUaServiceFaultListener.class);
    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull EventService eventService;
    private final @NotNull String adapterId;
    private final @Nullable Runnable reconnectionCallback;
    private final boolean reconnectOnServiceFault;

    /**
     * Asks the connection this listener serves whether it has been discarded — that is, whether it is being
     * closed, either because the adapter is stopping or because a reconnect is replacing it.
     * <p>
     * Faults raised by a connection on its way out are expected rather than actionable. Closing a connection
     * deletes its subscription while its client is still live, so a publish already in flight comes back
     * {@code Bad_NoSubscription}: the ordinary end of a subscription that was deliberately removed. Before
     * this existed, every shutdown produced errors that looked like failures to operators and failed
     * whichever integration test happened to be finishing (EDG-942).
     * <p>
     * <b>Why a supplier rather than a reference to the connection:</b> the answer changes over the lifetime
     * of the connection, so it has to be asked at the moment a fault arrives rather than read once. The
     * connection answers from the mark its teardown sets on its subscription handler before disconnecting
     * anything, so the answer is already true for the whole of a close rather than only at the end of it.
     * <p>
     * Final, because a listener serves exactly one connection for its whole life. On a template that no
     * connection derived from, this is permanently false: with nobody to vouch for a fault being expected,
     * the louder report is the safe answer.
     */
    private final @NotNull BooleanSupplier discarded;

    public OpcUaServiceFaultListener(
            @NotNull final ProtocolAdapterMetricsService protocolAdapterMetricsService,
            @NotNull final EventService eventService,
            @NotNull final String adapterId,
            @Nullable final Runnable reconnectionCallback,
            final boolean reconnectOnServiceFault) {
        this(
                protocolAdapterMetricsService,
                eventService,
                adapterId,
                reconnectionCallback,
                reconnectOnServiceFault,
                () -> false);
    }

    private OpcUaServiceFaultListener(
            @NotNull final ProtocolAdapterMetricsService protocolAdapterMetricsService,
            @NotNull final EventService eventService,
            @NotNull final String adapterId,
            @Nullable final Runnable reconnectionCallback,
            final boolean reconnectOnServiceFault,
            @NotNull final BooleanSupplier discarded) {
        this.protocolAdapterMetricsService = protocolAdapterMetricsService;
        this.eventService = eventService;
        this.adapterId = adapterId;
        this.reconnectionCallback = reconnectionCallback;
        this.reconnectOnServiceFault = reconnectOnServiceFault;
        this.discarded = discarded;
    }

    /**
     * Derives a copy of this listener that serves one connection, to be registered on that connection's
     * client. The copy keeps this listener's settings and adds the one thing it cannot infer: a way to ask
     * that connection whether it is being closed.
     * <p>
     * Callers are connections. An adapter builds one listener and never registers it anywhere; every
     * connection derives its own and registers that.
     * <p>
     * <b>Why a copy per connection, when one listener would do:</b> a fault says nothing about where it came
     * from. Milo's callback takes only the fault, and the fault names neither the client nor the session, so
     * a listener cannot look up the connection that raised it. What it can know is the connection it was
     * built for — Milo calls the listener registered on the client that faulted, so the instance receiving
     * the call already identifies the connection, provided that instance serves only one.
     * <p>
     * <b>Why one shared listener would be wrong:</b> an adapter can have two connections alive at once, since
     * a reconnect starts a replacement while the connection it replaces is still finishing or closing. A
     * shared listener would need a mutable pointer to whichever connection to ask, and that pointer would be
     * written by whichever connection set it last rather than by whichever is current. A slow start
     * completing late could aim a live connection's reporting at a discarded one and silence real faults.
     * Binding at construction removes the possibility instead of guarding against it.
     */
    public @NotNull OpcUaServiceFaultListener forConnection(final @NotNull BooleanSupplier connectionDiscarded) {
        return new OpcUaServiceFaultListener(
                protocolAdapterMetricsService,
                eventService,
                adapterId,
                reconnectionCallback,
                reconnectOnServiceFault,
                connectionDiscarded);
    }

    @Override
    public void onServiceFault(final ServiceFault serviceFault) {
        final StatusCode statusCode = serviceFault.getResponseHeader().getServiceResult();
        protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_SERVICE_FAULT_COUNT);

        // Faults from a connection that is closing are expected, so they are recorded quietly and nothing is
        // recovered. Closing deletes the subscription while the client is still live, so a publish already in
        // flight comes back Bad_NoSubscription -- the ordinary end of a subscription that was deliberately
        // removed, and not something an operator can act on.
        //
        // Placed above the critical/non-critical split below, not inside it. Which branch a fault takes
        // depends on `reconnectOnServiceFault`, which comes from the operator's autoReconnect setting; a
        // check inside one branch would leave the identical shutdown fault noisy for anyone who turned
        // auto-reconnect off. Whether a connection is closing has nothing to do with that setting.
        //
        // Returning here also skips the reconnect the critical branch would trigger, which is what should
        // happen: a connection already being discarded is not one to recover.
        if (discarded.getAsBoolean()) {
            log.info(
                    "OPC UA service fault for adapter '{}' on a connection being closed: {}. Expected while"
                            + " the connection is discarded.",
                    adapterId,
                    statusCode);
            return;
        }

        // Check if this is a critical fault requiring immediate reconnection
        if (reconnectOnServiceFault && isCriticalFault(statusCode)) {
            log.error("Critical OPC UA service fault detected for adapter '{}': {}", adapterId, statusCode);

            eventService
                    .createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                    .withSeverity(Event.SEVERITY.ERROR)
                    .withPayload(statusCode)
                    .withMessage("Critical Service Fault detected: " + statusCode + ". Triggering reconnection.")
                    .fire();

            // Trigger reconnection if callback is available
            if (reconnectionCallback != null) {
                log.info("Triggering reconnection for adapter '{}' due to critical service fault", adapterId);
                try {
                    reconnectionCallback.run();
                } catch (final Exception e) {
                    log.error("Failed to trigger reconnection for adapter '{}'", adapterId, e);
                }
            } else {
                log.warn("Cannot trigger reconnection for adapter '{}' - no callback available", adapterId);
            }
        } else {
            // Non-critical fault or feature disabled, just log
            log.warn("OPC UA service fault detected for adapter '{}': {}", adapterId, statusCode);

            eventService
                    .createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                    .withSeverity(Event.SEVERITY.WARN)
                    .withPayload(statusCode)
                    .withMessage("Service Fault detected: " + statusCode)
                    .fire();
        }
    }

    /**
     * Determines if a status code represents a critical fault that requires reconnection.
     * Critical faults are those that indicate the session or subscription is no longer valid
     * and cannot recover without reconnection.
     *
     * @param statusCode the OPC UA status code from the service fault
     * @return true if this is a critical fault requiring reconnection
     */
    private boolean isCriticalFault(final @NotNull StatusCode statusCode) {
        final long code = statusCode.getValue();
        return code == StatusCodes.Bad_SessionIdInvalid
                || code == StatusCodes.Bad_NoSubscription
                || code == StatusCodes.Bad_SessionClosed
                || code == StatusCodes.Bad_SecureChannelClosed
                || code == StatusCodes.Bad_SubscriptionIdInvalid
                || code == StatusCodes.Bad_IdentityTokenInvalid;
    }
}
