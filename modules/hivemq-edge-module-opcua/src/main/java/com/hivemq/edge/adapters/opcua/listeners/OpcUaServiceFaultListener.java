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
import java.util.concurrent.atomic.AtomicBoolean;
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
 * settings; each connection takes its own copy with {@link #forConnection} and registers that on its own
 * client. The copy serves that connection alone, so when the connection begins closing it can call
 * {@link #connectionDiscarded()} and this listener knows the faults that follow are the ordinary noise of a
 * close rather than something to report or recover from.
 */
public class OpcUaServiceFaultListener implements ServiceFaultListener {

    private static final Logger log = LoggerFactory.getLogger(OpcUaServiceFaultListener.class);
    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull EventService eventService;
    private final @NotNull String adapterId;
    private final @Nullable Runnable reconnectionCallback;
    private final boolean reconnectOnServiceFault;

    /**
     * Whether the connection this listener serves has been discarded — set by {@link #connectionDiscarded()}
     * when that connection begins closing, either because the adapter is stopping or because a reconnect is
     * replacing it.
     * <p>
     * Faults raised by a connection on its way out are expected rather than actionable. Closing a connection
     * deletes its subscription while its client is still live, so a publish already in flight comes back
     * {@code Bad_NoSubscription}: the ordinary end of a subscription that was deliberately removed. Before
     * this existed, every shutdown produced errors that looked like failures to operators and failed
     * whichever integration test happened to be finishing (EDG-942).
     * <p>
     * One-way, because a connection is never reopened — a reconnect builds a new one, with a new listener.
     * False until told otherwise, so a listener nobody claimed reports everything: with no connection to
     * vouch for a fault being expected, the louder report is the safe answer.
     */
    private final @NotNull AtomicBoolean discarded = new AtomicBoolean();

    public OpcUaServiceFaultListener(
            @NotNull final ProtocolAdapterMetricsService protocolAdapterMetricsService,
            @NotNull final EventService eventService,
            @NotNull final String adapterId,
            @Nullable final Runnable reconnectionCallback,
            final boolean reconnectOnServiceFault) {
        this.protocolAdapterMetricsService = protocolAdapterMetricsService;
        this.eventService = eventService;
        this.adapterId = adapterId;
        this.reconnectionCallback = reconnectionCallback;
        this.reconnectOnServiceFault = reconnectOnServiceFault;
    }

    /**
     * Returns a listener with the same settings for one connection to register on its own client, leaving
     * this one untouched. Callers are connections; the adapter builds one listener as a template and
     * registers it nowhere.
     * <p>
     * <b>Why every connection needs its own rather than sharing the adapter's:</b> a fault says nothing about
     * where it came from. Milo's callback takes only the fault, and the fault names neither the client nor
     * the session, so a listener cannot look up the connection that raised it. What identifies the connection
     * is which listener Milo called — it calls the one registered on the client that faulted — and that only
     * identifies anything if each listener serves a single connection.
     * <p>
     * It matters because an adapter can have two connections alive at once: a reconnect starts a replacement
     * while the connection it replaces is still finishing or closing. One shared listener would have a single
     * discarded flag for both, so the closing connection would silence the live one's faults.
     */
    public @NotNull OpcUaServiceFaultListener forConnection() {
        return new OpcUaServiceFaultListener(
                protocolAdapterMetricsService, eventService, adapterId, reconnectionCallback, reconnectOnServiceFault);
    }

    /**
     * Tells this listener that the connection it serves is being discarded, so that faults from here on are
     * the expected noise of a close rather than something to report or recover from.
     * <p>
     * Called by that connection as it begins closing, before it disconnects anything — which is what makes
     * this cover the whole close rather than only its end. One-way and idempotent: a discarded connection is
     * never reopened.
     */
    public void connectionDiscarded() {
        discarded.set(true);
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
        if (discarded.get()) {
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
