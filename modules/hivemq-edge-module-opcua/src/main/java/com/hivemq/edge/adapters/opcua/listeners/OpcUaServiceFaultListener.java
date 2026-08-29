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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.eclipse.milo.opcua.sdk.client.ServiceFaultListener;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.structured.ServiceFault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpcUaServiceFaultListener implements ServiceFaultListener {

    private static final Logger log = LoggerFactory.getLogger(OpcUaServiceFaultListener.class);
    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull EventService eventService;
    private final @NotNull String adapterId;
    private final @Nullable Runnable reconnectionCallback;
    private final boolean reconnectOnServiceFault;

    /**
     * Whether the connection whose client raised the fault has been discarded.
     * <p>
     * A fault arrives on a client, and this listener cannot tell from the fault alone whether that client
     * still matters. The connection that owns the client can: it marks its subscription handler abandoned
     * before it disconnects anything, precisely so that work already in flight can stop. That mark is what
     * this asks for, and it is set early enough to cover the whole close.
     * <p>
     * Settable rather than constructor-injected because this listener outlives a single connection -- the
     * adapter builds one and hands it to each connection in turn -- so the connection to ask changes. Each
     * connection points it at itself as it attaches, and clears it as it detaches. Answers false while
     * unset, which is the behaviour this class had before the distinction existed: a caller that cannot say
     * whether its connection is being discarded gets the louder report, not the quieter one.
     */
    private final @NotNull AtomicReference<BooleanSupplier> discarded = new AtomicReference<>();

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
     * Names the connection whose discard state this listener should consult.
     * <p>
     * Called by a connection as it attaches this listener to its client. The most recent caller wins, which
     * is what a reconnect needs: the replacement connection attaches before the old one has finished closing,
     * and it is the replacement whose faults matter from then on.
     */
    public void watch(final @NotNull BooleanSupplier connectionDiscarded) {
        discarded.set(connectionDiscarded);
    }

    /**
     * Stops consulting {@code connectionDiscarded}, if it is still the one being consulted.
     * <p>
     * Conditional, and that is the whole point. A connection clears its watch as it detaches, but a reconnect
     * has the replacement attach <em>before</em> the old connection finishes closing -- so a blind clear here
     * would wipe the live connection's watch and leave this listener answering "not discarded" for a
     * connection that may since have been discarded. Clearing only what this connection itself installed
     * leaves a newer watch alone.
     */
    public void unwatch(final @NotNull BooleanSupplier connectionDiscarded) {
        discarded.compareAndSet(connectionDiscarded, null);
    }

    /** Whether the connection that raised this fault is being discarded; false when nobody has said. */
    private boolean isDiscarded() {
        final BooleanSupplier source = discarded.get();
        return source != null && source.getAsBoolean();
    }

    @Override
    public void onServiceFault(final ServiceFault serviceFault) {
        final StatusCode statusCode = serviceFault.getResponseHeader().getServiceResult();
        protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_SERVICE_FAULT_COUNT);

        // The same fault means two different things depending on whether the connection it arrived on still
        // matters. Discarding a connection deletes its subscription while this listener is still attached, so
        // a publish already in flight comes back Bad_NoSubscription -- the expected end of a subscription that
        // was deliberately removed, not a fault anyone can act on. Reporting it as a fault made every shutdown
        // look like a failure, to operators and to the tests that fail on any unexpected ERROR (EDG-942).
        //
        // "Discarded" rather than "the adapter is stopping", because those differ: a reconnect discards a
        // connection while the adapter stays up. Both cases want silence for the same reason -- a replacement
        // is coming, or nothing is -- and in neither is the old connection's failure something to act on.
        //
        // Above the critical/non-critical split rather than inside it. Which branch a fault takes depends on
        // `reconnectOnServiceFault`, an operator's autoReconnect setting -- so a check inside the critical
        // branch would let the identical fault stay noisy for anyone who turned auto-reconnect off. Being
        // discarded is a property of the connection, not of that setting.
        //
        // The reconnect the critical branch would have triggered is skipped with it, and that is right rather
        // than merely harmless: a connection that has already been discarded is not the one to recover from.
        if (isDiscarded()) {
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
