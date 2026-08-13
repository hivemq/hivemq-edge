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
package com.hivemq.edge.adapters.opcua;

import static com.hivemq.edge.adapters.opcua.Constants.PROTOCOL_ID_OPCUA;

import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.edge.adapters.opcua.client.OpcUaClientConfigurator;
import com.hivemq.edge.adapters.opcua.client.OpcUaEndpointFilter;
import com.hivemq.edge.adapters.opcua.client.ParsedConfig;
import com.hivemq.edge.adapters.opcua.config.MsgSecurityMode;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.SecPolicy;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.listeners.OpcUaServiceFaultListener;
import com.hivemq.edge.adapters.opcua.listeners.OpcUaSessionActivityListener;
import com.hivemq.edge.adapters.opcua.listeners.OpcUaSubscriptionLifecycleHandler;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.ServiceFaultListener;
import org.eclipse.milo.opcua.sdk.client.SessionActivityListener;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpcUaClientConnection {
    private static final @NotNull Logger log = LoggerFactory.getLogger(OpcUaClientConnection.class);

    private final @NotNull OpcUaSpecificAdapterConfig config;
    private final @NotNull List<OpcuaTag> tags;
    private final @NotNull ProtocolAdapterTagStreamingService tagStreamingService;
    private final @NotNull EventService eventService;
    private final @NotNull String adapterId;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;

    private final @NotNull AtomicReference<ConnectionContext> context = new AtomicReference<>();
    private final @NotNull OpcUaServiceFaultListener serviceFaultListener;

    /**
     * Whether the adapter still speaks through this connection, asked of the adapter rather than answered here.
     * <p>
     * {@link ProtocolAdapterState} is one slot per adapter, and every attempt the adapter makes — first start,
     * retry, reconnect — is a fresh {@code OpcUaClientConnection} writing into that same slot. A connection is
     * therefore never entitled to describe the adapter on its own authority; it is entitled to do so only while
     * it is the connection the adapter holds.
     * <p>
     * Without that distinction a teardown could report the wrong thing about a healthy replacement.
     * {@link OpcUaProtocolAdapter#destroy()} hands the slot over and closes the old connection on the common
     * pool, so the close outlives the call: the framework can install and connect a replacement while it runs,
     * and the old connection's {@link #closeContext} then wrote {@code DISCONNECTED} over the replacement's
     * {@code CONNECTED}. The next health check reads that status, judges the healthy replacement unhealthy and
     * reconnects it — a monitoring gap and a rebuild, both caused by a connection that no longer existed.
     * Setting a status also notifies the framework's connection-status listener, so the damage is not confined
     * to the health check.
     * <p>
     * A predicate over the connection rather than a plain {@code BooleanSupplier} so the adapter can answer it
     * with a bare identity comparison against its own slot, without either side having to be constructed first.
     * Required rather than defaulted: a call site that forgets it would silently reintroduce exactly this.
     */
    private final @NotNull Predicate<OpcUaClientConnection> stillOwned;

    /**
     * The subscription handler, reachable from the moment it is constructed rather than from the moment the
     * connection is fully established.
     * <p>
     * {@link ConnectionContext} also holds it, and that is the reference everything else uses. This one exists
     * for the window the context cannot cover: {@code context} is installed at the very end of {@link #start},
     * after {@code subscribe()} has verified every tag, and verification is where a start spends its time —
     * up to three blocking round trips per condition tag, each with a ten-second ceiling. For all of that
     * window the handler existed but nothing outside {@code start()} could name it, so
     * {@link OpcUaSubscriptionLifecycleHandler#abandon()} could not be called on the one path where the wait
     * is longest. {@link #stop} blocked on the monitor {@code start()} holds, and {@link #destroy} got through
     * but found a null context and returned having done nothing.
     * <p>
     * An additional path to the same object, not a replacement for the context's field.
     */
    private final @NotNull AtomicReference<OpcUaSubscriptionLifecycleHandler> subscriptionHandler =
            new AtomicReference<>();

    /**
     * Whether {@link #stop} or {@link #destroy} has been called on this connection.
     * <p>
     * One-way, and per connection object rather than per adapter: every attempt — first start, retry and
     * reconnect alike — builds a fresh {@code OpcUaClientConnection}, so a closed one is never started again
     * and the flag never has to be cleared.
     * <p>
     * Read by {@link #start} immediately before it installs its context, which is the point at which an
     * attempt stops being private to itself and becomes the adapter's live connection. Without it a teardown
     * that runs during verification completes against a null context and returns, and the attempt then
     * publishes a live client and session into an object the adapter has already discarded.
     */
    private final @NotNull AtomicBoolean closed = new AtomicBoolean();

    OpcUaClientConnection(
            final @NotNull String adapterId,
            final @NotNull List<OpcuaTag> tags,
            final @NotNull ProtocolAdapterState protocolAdapterState,
            final @NotNull ProtocolAdapterTagStreamingService tagStreamingService,
            final @NotNull EventService eventService,
            final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService,
            final @NotNull OpcUaSpecificAdapterConfig config,
            final @NotNull OpcUaServiceFaultListener serviceFaultListener,
            final @NotNull Predicate<OpcUaClientConnection> stillOwned) {
        this.config = config;
        this.tagStreamingService = tagStreamingService;
        this.eventService = eventService;
        this.protocolAdapterMetricsService = protocolAdapterMetricsService;
        this.adapterId = adapterId;
        this.protocolAdapterState = protocolAdapterState;
        this.tags = tags;
        this.serviceFaultListener = serviceFaultListener;
        this.stillOwned = stillOwned;
    }

    /**
     * Publishes a connection status, unless the adapter has already moved on to another connection.
     * <p>
     * Every status this class reports goes through here, not only the ones a teardown writes. The question
     * "may this object describe the adapter" has the same answer whichever status is being reported, and a
     * superseded attempt announcing {@code ERROR} because it could not connect is as wrong as one announcing
     * {@code DISCONNECTED} because it has been closed — in both cases the adapter is being described by an
     * object it no longer holds.
     * <p>
     * Silently dropping the status is the right outcome rather than a lossy one: the adapter's own lifecycle
     * paths publish what is true of the adapter at the moment they run — see {@link OpcUaProtocolAdapter#stop}
     * and {@link OpcUaProtocolAdapter#destroy}, which say {@code DISCONNECTED} themselves rather than leaving
     * it to a close that may complete long afterwards.
     */
    private void publishStatus(final ProtocolAdapterState.@NotNull ConnectionStatus status) {
        if (!stillOwned.test(this)) {
            log.debug(
                    "OPC UA adapter '{}': not reporting {} from a connection the adapter has already replaced",
                    adapterId,
                    status);
            return;
        }
        protocolAdapterState.setConnectionStatus(status);
    }

    synchronized boolean start(final ParsedConfig parsedConfig) {
        log.debug("Subscribing to OPC UA client");
        // A connection that was torn down before its attempt ever ran. Cheap, and it keeps the expensive part
        // -- creating a client and connecting it -- from happening on behalf of an adapter that is gone.
        if (closed.get()) {
            log.debug("Not connecting OPC UA adapter '{}': the connection was closed before it started", adapterId);
            return false;
        }
        final OpcUaClient client;
        // Given the same ownership question this connection asks of itself. The listener is the other writer
        // of the adapter's status, and a superseded connection's session is still a live session: it can go
        // inactive -- or reactivate -- long after the adapter has moved on, and either report would describe
        // the replacement rather than the object it actually happened to.
        final var activityListener = new OpcUaSessionActivityListener(
                protocolAdapterMetricsService,
                eventService,
                adapterId,
                protocolAdapterState,
                () -> stillOwned.test(this));

        // Determine preferred MessageSecurityMode with intelligent defaults
        final MessageSecurityMode preferredMode;
        final MsgSecurityMode configuredMode = config.getSecurity().messageSecurityMode();
        if (configuredMode != null && configuredMode != MsgSecurityMode.IGNORED) {
            // Explicitly configured mode
            preferredMode = configuredMode.getMiloMode();
            if (log.isDebugEnabled()) {
                log.debug("Using configured message security mode: {}", preferredMode);
            }
        } else {
            // Intelligent default based on security policy
            final SecPolicy policy = config.getSecurity().policy();
            if (policy == SecPolicy.NONE) {
                preferredMode = MessageSecurityMode.None;
            } else {
                // For all secure policies, prefer SignAndEncrypt (most secure)
                preferredMode = MessageSecurityMode.SignAndEncrypt;
                if (log.isDebugEnabled()) {
                    log.debug(
                            "No message security mode configured, defaulting to SignAndEncrypt for policy {}", policy);
                }
            }
        }
        final var endpointFilter = new OpcUaEndpointFilter(
                adapterId, config.getSecurity().policy().getSecurityPolicy().getUri(), preferredMode, config);
        try {
            client = OpcUaClient.create(
                    config.getUri(),
                    endpointFilter,
                    ignore -> {},
                    new OpcUaClientConfigurator(adapterId, parsedConfig, config));
            client.addFaultListener(serviceFaultListener);
            client.addSessionActivityListener(activityListener);

            // Add timeout to connection attempt to prevent hanging forever
            // Wrap synchronous connect() call with CompletableFuture timeout
            final long connectionTimeoutSecondsMs =
                    config.getConnectionOptions().connectionTimeoutMs();
            try {
                CompletableFuture.runAsync(() -> {
                            try {
                                client.connect();
                            } catch (final UaException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .get(connectionTimeoutSecondsMs, TimeUnit.MILLISECONDS);
                log.debug("OPC UA client connected successfully for adapter '{}'", adapterId);
            } catch (final TimeoutException e) {
                log.error(
                        "Connection timeout after {} milliseconds for OPC UA adapter '{}'",
                        connectionTimeoutSecondsMs,
                        adapterId);
                eventService
                        .createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                        .withMessage("Connection timeout after " + connectionTimeoutSecondsMs
                                + " milliseconds for adapter '" + adapterId + "'")
                        .withSeverity(Event.SEVERITY.ERROR)
                        .fire();
                publishStatus(ProtocolAdapterState.ConnectionStatus.ERROR);
                quietlyCloseClient(client, false, serviceFaultListener, null);
                return false;
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Connection interrupted for OPC UA adapter '{}'", adapterId, e);
                eventService
                        .createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                        .withMessage("Connection interrupted for adapter '" + adapterId + "'")
                        .withSeverity(Event.SEVERITY.ERROR)
                        .fire();
                publishStatus(ProtocolAdapterState.ConnectionStatus.ERROR);
                quietlyCloseClient(client, false, serviceFaultListener, null);
                return false;
            } catch (final ExecutionException e) {
                final Throwable cause = e.getCause();
                log.error("Connection failed for OPC UA adapter '{}'", adapterId, cause);
                final String causeMessage = cause != null ? cause.getMessage() : e.getMessage();
                eventService
                        .createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                        .withMessage("Connection failed for adapter '" + adapterId + "': " + causeMessage)
                        .withSeverity(Event.SEVERITY.ERROR)
                        .fire();
                publishStatus(ProtocolAdapterState.ConnectionStatus.ERROR);
                quietlyCloseClient(client, false, serviceFaultListener, null);
                return false;
            }
        } catch (final UaException e) {
            log.error("Unable to create OPC UA client for adapter '{}'", adapterId, e);
            eventService
                    .createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                    .withMessage("Unable to create OPC UA client for adapter '" + adapterId + "'")
                    .withSeverity(Event.SEVERITY.ERROR)
                    .fire();
            publishStatus(ProtocolAdapterState.ConnectionStatus.ERROR);
            return false;
        }

        final var subscriptionLifecycleHandler = new OpcUaSubscriptionLifecycleHandler(
                protocolAdapterMetricsService, tagStreamingService, eventService, adapterId, tags, client, config);

        // Reachable before the work it has to be able to interrupt, not after. subscribe() below verifies
        // every tag against the server, and abandon() is what stops that loop between tags -- so publishing
        // the handler only with the context, once subscribe() has already returned, meant the flag could not
        // be set during the one window in which it earns its keep.
        subscriptionHandler.set(subscriptionLifecycleHandler);
        // Ordered against the close paths, which set `closed` before they look for a handler to abandon. A
        // teardown that read a null handler a moment ago has already published the flag this re-reads, so
        // between the two of them the abandonment cannot be dropped by either interleaving.
        if (closed.get()) {
            subscriptionLifecycleHandler.abandon();
        }

        // A reconnect that transfers the subscription successfully recreates nothing, so the refresh that
        // rides on re-establishing monitored items never happens. This is the only signal for that case.
        activityListener.setOnReconnect(subscriptionLifecycleHandler::onSessionReactivated);

        final var subscriptionOptional = subscriptionLifecycleHandler.subscribe(client);

        if (subscriptionOptional.isEmpty()) {
            log.error("Failed to create or transfer OPC UA subscription. Closing client connection.");
            publishStatus(ProtocolAdapterState.ConnectionStatus.ERROR);
            eventService
                    .createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                    .withMessage("Failed to create or transfer OPC UA subscription. Closing client connection.")
                    .withSeverity(Event.SEVERITY.ERROR)
                    .fire();
            quietlyCloseClient(client, false, serviceFaultListener, activityListener);
            return false;
        }

        final var subscription = subscriptionOptional.get();
        log.trace("Creating Subscription for OPC UA client");

        // The last chance to notice that this attempt has been overtaken, and the only one that matters:
        // installing the context is what turns a private attempt into the adapter's live connection, and
        // until it happens a teardown finds nothing to close. A stop() or destroy() that ran while this
        // attempt was connecting and verifying has already completed against a null context and returned, so
        // publishing now would hand the adapter a session and subscription it has no reference to and can
        // never close -- one that goes on publishing events after the adapter that owned it is gone.
        //
        // Closed here rather than left for the caller, for the same reason establishInitial() deletes a
        // subscription it cannot establish: `false` is the last anyone sees of this client.
        if (closed.get()) {
            log.info(
                    "OPC UA adapter '{}': the connection was closed while it was being established; "
                            + "discarding the client rather than publishing it",
                    adapterId);
            quietlyCloseClient(client, false, serviceFaultListener, activityListener);
            // Said here rather than left to the activity listener, which cannot say it: quietlyCloseClient
            // removes that listener before disconnecting, precisely so a teardown does not announce itself as
            // a fault. The status is CONNECTED at this point -- onSessionActive reports it the moment Milo
            // activates the session, long before there is a subscription -- so without this the adapter would
            // go on reporting a connection that has just been thrown away. The teardown that set `closed`
            // could not correct it either: it ran against a null context and set nothing.
            publishStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
            return false;
        }

        context.set(new ConnectionContext(
                subscription.getClient(), serviceFaultListener, activityListener, subscriptionLifecycleHandler));
        publishStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);

        log.info("Client created and connected successfully");
        return true;
    }

    void stop() {
        log.info("Stopping OPC UA client");
        // Before the monitor, not behind it. A start() in progress holds this instance's lock for the whole
        // of its verification, so a synchronized stop() could not reach the flag that shortens the very wait
        // it was queueing behind: it blocked for up to three ten-second round trips per condition tag,
        // reported to the operator as a hang. Abandoning first turns that into one outstanding call.
        markClosed();
        closeContext(true);
    }

    void destroy() {
        log.info("Destroying OPC UA client");
        markClosed();
        closeContext(false);
    }

    /**
     * Records that this connection is finished with and tells the handler to stop verifying tags.
     * <p>
     * Deliberately unsynchronized and idempotent, so it can run while {@link #start} holds the monitor — that
     * is the whole point of it. The ordering is what makes it safe: {@code closed} is published before the
     * handler is read, and {@code start()} publishes the handler before it re-reads {@code closed}, so
     * whichever of the two goes second sees the other's write and the abandonment cannot fall between them.
     */
    private void markClosed() {
        closed.set(true);
        final OpcUaSubscriptionLifecycleHandler handler = subscriptionHandler.get();
        if (handler != null) {
            // Told before the client is closed, not after: a start or a recovery may be part way through
            // verifying tags, and this is what lets it stop rather than spend a round trip per remaining tag
            // on a client that is about to be disconnected.
            handler.abandon();
        }
    }

    /**
     * Closes whatever this connection actually established, if it got that far.
     * <p>
     * Synchronized, and that is intentional even though {@link #markClosed} is not: a teardown that overlaps
     * a start has to wait for the start to finish before it can decide there is nothing to close, or it would
     * race the {@code context.set(...)} at the end of {@link #start}. Because {@code closed} is already set by
     * the time this waits, the start it is waiting for will abandon its verification and then refuse to
     * install its context, so the wait is bounded by one outstanding server call rather than by every
     * remaining tag.
     *
     * @param keepSubscription whether to leave the subscription on the server — true for a stop, which may be
     *                         followed by a reconnect that transfers it, false for a destroy.
     */
    private synchronized void closeContext(final boolean keepSubscription) {
        final ConnectionContext ctx = context.getAndSet(null);
        if (ctx != null) {
            quietlyCloseClient(ctx.client(), keepSubscription, ctx.faultListener(), ctx.activityListener());
            publishStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
        }
    }

    /**
     * Checks if the connection is healthy by verifying both:
     * 1. Session is active (client connection exists and session is present)
     * 2. Keep-alive messages are being received (subscription is healthy)
     *
     * @return true if connection is healthy, false otherwise
     */
    public boolean isHealthy() {
        final ConnectionContext ctx = context.get();
        if (ctx == null) {
            log.debug("Connection health check failed: no connection context");
            return false;
        }

        try {
            // Check 1: Session is active
            if (protocolAdapterState.getConnectionStatus() == ProtocolAdapterState.ConnectionStatus.DISCONNECTED) {
                log.debug("Connection health check failed: session inactive for adapter '{}'", adapterId);
                return false;
            }

            // Check 2: Keep-alive is healthy
            if (!ctx.subscriptionHandler().isKeepAliveHealthy()) {
                log.debug("Connection health check failed: keep-alive timeout for adapter '{}'", adapterId);
                return false;
            }

            return true;
        } catch (final Exception e) {
            log.debug("Connection health check failed with exception for adapter '{}'", adapterId, e);
            return false;
        }
    }

    @NotNull
    Optional<OpcUaClient> client() {
        final ConnectionContext ctx = context.get();
        if (ctx != null) {
            return Optional.of(ctx.client());
        }
        return Optional.empty();
    }

    /**
     * Asks the server to re-report every condition it currently retains, on demand.
     * <p>
     * The same call the adapter makes automatically on connect and reconnect; this is the seam a refresh
     * tag's southbound write uses. The subscription is the connection's to know, which is why the request
     * goes through here rather than being assembled by the caller.
     *
     * @return the status of the call, or empty when there is no live subscription to refresh.
     */
    @NotNull
    Optional<CompletableFuture<StatusCode>> requestConditionRefresh() {
        final ConnectionContext ctx = context.get();
        if (ctx == null) {
            return Optional.empty();
        }
        return ctx.subscriptionHandler().requestConditionRefreshNow();
    }

    /**
     * Whether a teardown has reached this connection's subscription handler.
     * <p>
     * Visible for the tests that pin the ordering this fix is about: that {@link #stop} and {@link #destroy}
     * can set the flag while {@link #start} holds the monitor, which is the window in which it is worth
     * anything and the one in which it previously could not be set at all.
     */
    @VisibleForTesting
    boolean handlerWasAbandoned() {
        final OpcUaSubscriptionLifecycleHandler handler = subscriptionHandler.get();
        return handler != null && handler.isAbandoned();
    }

    private static void quietlyDeleteSubscription(
            final @NotNull OpcUaClient client, final @NotNull OpcUaSubscription subscription) {
        try {
            subscription.delete();
        } catch (final Exception e) {
            log.warn("Failed to delete subscription {}: {}", subscription, e.getMessage());
        }
        try {
            client.removeSubscription(subscription);
        } catch (final Exception e) {
            log.warn("Failed to remove subscription {}: {}", subscription, e.getMessage());
        }
    }

    private static void quietlyCloseClient(
            final @NotNull OpcUaClient client,
            final boolean keepSubscription,
            final @Nullable ServiceFaultListener faultListener,
            final @Nullable SessionActivityListener activityListener) {

        client.getSubscriptions().forEach(subscription -> {
            subscription.setSubscriptionListener(null);
            if (!keepSubscription) {
                quietlyDeleteSubscription(client, subscription);
            }
        });
        if (faultListener != null) {
            try {
                client.removeFaultListener(faultListener);
            } catch (final Throwable e) {
                log.error("Failed to remove fault listener {}: {}", faultListener, e.getMessage());
            }
        }
        if (activityListener != null) {
            try {
                client.removeSessionActivityListener(activityListener);
            } catch (final Throwable e) {
                log.error("Failed to remove session activity listener {}: {}", activityListener, e.getMessage());
            }
        }

        try {
            client.disconnect();
        } catch (final UaException e) {
            log.error("Failed to disconnect: {}", e.getMessage());
        }
    }

    private record ConnectionContext(
            @NotNull OpcUaClient client,
            @NotNull ServiceFaultListener faultListener,
            @NotNull SessionActivityListener activityListener,
            @NotNull OpcUaSubscriptionLifecycleHandler subscriptionHandler) {}
}
