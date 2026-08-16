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
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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

    /**
     * The longest a connection attempt will wait for browse metadata before going on without it.
     * <p>
     * A ceiling rather than the deadline itself: the wait is the smaller of this and {@code
     * connectionTimeoutMs}, so an operator who shortened the connect timeout to fail fast still gets a short
     * wait here, while one who raised it to 300 s for a slow WAN does not thereby agree to sit in {@code
     * CONNECTING} for five minutes on every connect. Expiry costs nothing but latency — see {@link
     * #prepareClientForUse}.
     */
    private static final long METADATA_PREPARATION_MAX_WAIT_MS = 30_000L;

    /** First backoff step after a rehydration failure, and the ceiling it doubles towards. */
    private static final long METADATA_RETRY_BASE_DELAY_MS = 60_000L;

    private static final long METADATA_RETRY_MAX_DELAY_MS = 30 * 60_000L;

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
     * Publishes through the adapter, which atomically validates this connection's generation and writes the
     * shared status. Keeping both operations on the owner side prevents a replacement from landing between a
     * connection's ownership check and its write.
     */
    private final @NotNull BiConsumer<OpcUaClientConnection, ProtocolAdapterState.ConnectionStatus> statusPublisher;

    /**
     * Connection-specific metadata preparation that must finish before any tag reaches the server.
     * <p>
     * The adapter uses this to hydrate Milo's namespace table and data-type tree. Keeping it on the
     * connection's start path makes the readiness transition ordered: preparation succeeds, tags are
     * verified and subscribed, the context is installed, and only then is {@code CONNECTED} published.
     */
    private final @NotNull Consumer<OpcUaClient> prepareForUse;

    /**
     * The metadata task currently holding startup, if any.
     * <p>
     * Milo's eager data-type-tree build is synchronous and recursively issues browse/read requests. Running
     * it in an interruptible task gives the complete build one configured deadline and lets teardown cancel
     * it before waiting for the {@link #start} monitor.
     */
    private final @NotNull AtomicReference<FutureTask<Void>> preparationTask = new AtomicReference<>();

    /**
     * Whether this connection's browse metadata was hydrated during its start.
     * <p>
     * Read by the adapter when it publishes readiness, so that {@code CONNECTED} and browse-readiness can be
     * two facts rather than one. They are not the same fact: the data-type tree is consumed only by the
     * browse endpoint and the southbound write/schema paths, all of which build their consumers on demand,
     * and none of which is on the northbound streaming path.
     */
    private final @NotNull AtomicBoolean browseMetadataPrepared = new AtomicBoolean();

    /**
     * The rehydration worker currently alive, or null. Its presence is the guard against a second one.
     * <p>
     * A thread reference rather than a flag because the question is whether a worker still exists, and only
     * the worker can answer it. See {@link #retryBrowseMetadata}.
     */
    private final @NotNull AtomicReference<Thread> metadataRetryWorker = new AtomicReference<>();

    /** Consecutive failed rehydration attempts, which is what the backoff is computed from. */
    private final @NotNull AtomicInteger metadataRetryFailures = new AtomicInteger();

    /** The earliest a further rehydration may start. Seeded to now, so the first one is not delayed. */
    private final @NotNull AtomicLong nextMetadataRetryNanos = new AtomicLong(System.nanoTime());

    /** Atomically marks adapter-owned readiness and CONNECTED after the usable context has been installed. */
    private final @NotNull Consumer<OpcUaClientConnection> publishReady;

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
        this(
                adapterId,
                tags,
                protocolAdapterState,
                tagStreamingService,
                eventService,
                protocolAdapterMetricsService,
                config,
                serviceFaultListener,
                stillOwned,
                ignored -> {},
                () -> {});
    }

    OpcUaClientConnection(
            final @NotNull String adapterId,
            final @NotNull List<OpcuaTag> tags,
            final @NotNull ProtocolAdapterState protocolAdapterState,
            final @NotNull ProtocolAdapterTagStreamingService tagStreamingService,
            final @NotNull EventService eventService,
            final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService,
            final @NotNull OpcUaSpecificAdapterConfig config,
            final @NotNull OpcUaServiceFaultListener serviceFaultListener,
            final @NotNull Predicate<OpcUaClientConnection> stillOwned,
            final @NotNull Consumer<OpcUaClient> prepareForUse,
            final @NotNull Runnable markReady) {
        this(
                adapterId,
                tags,
                protocolAdapterState,
                tagStreamingService,
                eventService,
                protocolAdapterMetricsService,
                config,
                serviceFaultListener,
                legacyStatusPublisher(protocolAdapterState, stillOwned),
                prepareForUse,
                legacyReadyPublisher(protocolAdapterState, stillOwned, markReady));
    }

    OpcUaClientConnection(
            final @NotNull String adapterId,
            final @NotNull List<OpcuaTag> tags,
            final @NotNull ProtocolAdapterState protocolAdapterState,
            final @NotNull ProtocolAdapterTagStreamingService tagStreamingService,
            final @NotNull EventService eventService,
            final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService,
            final @NotNull OpcUaSpecificAdapterConfig config,
            final @NotNull OpcUaServiceFaultListener serviceFaultListener,
            final @NotNull BiConsumer<OpcUaClientConnection, ProtocolAdapterState.ConnectionStatus> statusPublisher,
            final @NotNull Consumer<OpcUaClient> prepareForUse,
            final @NotNull Consumer<OpcUaClientConnection> publishReady) {
        this.config = config;
        this.tagStreamingService = tagStreamingService;
        this.eventService = eventService;
        this.protocolAdapterMetricsService = protocolAdapterMetricsService;
        this.adapterId = adapterId;
        this.protocolAdapterState = protocolAdapterState;
        this.tags = tags;
        this.serviceFaultListener = serviceFaultListener;
        this.statusPublisher = statusPublisher;
        this.prepareForUse = prepareForUse;
        this.publishReady = publishReady;
    }

    /** Compatibility seam for direct connection tests; production delegates the whole operation to the adapter. */
    private static @NotNull BiConsumer<OpcUaClientConnection, ProtocolAdapterState.ConnectionStatus>
            legacyStatusPublisher(
                    final @NotNull ProtocolAdapterState protocolAdapterState,
                    final @NotNull Predicate<OpcUaClientConnection> stillOwned) {
        return (source, status) -> {
            if (stillOwned.test(source)) {
                protocolAdapterState.setConnectionStatus(status);
            }
        };
    }

    /** Compatibility seam for tests written against the former separate readiness callback. */
    private static @NotNull Consumer<OpcUaClientConnection> legacyReadyPublisher(
            final @NotNull ProtocolAdapterState protocolAdapterState,
            final @NotNull Predicate<OpcUaClientConnection> stillOwned,
            final @NotNull Runnable markReady) {
        return source -> {
            if (stillOwned.test(source)) {
                markReady.run();
                protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
            }
        };
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
     * Silently dropping the status is the right outcome rather than a lossy one: lifecycle-owned paths publish
     * the state they are responsible for at a safely ordered point. The adapter does that synchronously in
     * {@link OpcUaProtocolAdapter#destroy}; the wrapper owns the terminal state of an ordinary stop so failed-start
     * cleanup can preserve {@code ERROR}.
     */
    private void publishStatus(final ProtocolAdapterState.@NotNull ConnectionStatus status) {
        statusPublisher.accept(this, status);
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
        // Given the same adapter-owned publisher this connection uses. The listener is the other source of
        // adapter status changes, and a superseded connection's session is still a live session: it can go
        // inactive -- or reactivate -- long after the adapter has moved on, and either report would describe
        // the replacement rather than the object it actually happened to.
        final var activityListener = new OpcUaSessionActivityListener(
                protocolAdapterMetricsService,
                eventService,
                adapterId,
                this::publishStatus,
                () -> context.get() != null);

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

        if (!prepareClientForUse(client, activityListener)) {
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
            // a fault. The initial onSessionActive deliberately does not report CONNECTED; the connection
            // owns that transition and publishes it only after installing the usable context below.
            publishStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
            return false;
        }

        context.set(new ConnectionContext(
                subscription.getClient(), serviceFaultListener, activityListener, subscriptionLifecycleHandler));
        publishReady.accept(this);

        log.info("Client created and connected successfully");
        return true;
    }

    /**
     * Hydrates browse metadata before any tag is verified or subscribed, and goes on without it if it cannot.
     * <p>
     * Ordering first, because that is what this being on the start path buys. Milo activates a session before
     * the data-type tree exists, so a browse in that window is non-deterministic; doing the build here means
     * that when it succeeds — the ordinary case — {@code CONNECTED} genuinely implies browse-ready, and no
     * monitored item is live before it. That is review-08 finding 1 and it is unchanged.
     * <p>
     * What is different is the consequence of failing, and the reason is that nothing at connect time needs
     * the result. Of the two calls, {@code getNamespaceTable()} is a field read that performs no I/O at all —
     * Milo populates the table during session activation — so it can neither be slow nor be refused. The
     * whole cost and the whole failure surface is {@code getDataTypeTree()}, a recursive browse whose only
     * consumers are the browse endpoint, the southbound converter and the southbound schema generator. Each
     * constructs itself at the point of use, and Milo's {@code NonBlockingLazy} clears its slot on failure
     * rather than caching it, so a build that fails or is interrupted here is simply rebuilt by whoever needs
     * it next. Northbound streaming and tag verification never touch it; verification needs only the
     * namespace table.
     * <p>
     * So a failure costs browse and southbound latency, not correctness — which is what it cost before this
     * PR, when the warm-up was fire-and-forget and a failure left the adapter connected and streaming with
     * the browse endpoint answering 503. Failing the whole attempt instead took that from every OPC UA
     * adapter, including value-only ones with no interest in any of it: a server that denies broad metadata
     * browse to the configured identity, or is merely slow, would move a working adapter into permanent
     * {@code ERROR}/backoff after an upgrade. The adapter is told what happened and reports browse as not
     * ready; it does not lose its data path over it.
     * <p>
     * The wait is bounded by the smaller of {@code connectionTimeoutMs} and {@link
     * #METADATA_PREPARATION_MAX_WAIT_MS}, and the task is published before its thread starts so {@link
     * #markClosed()} can interrupt a build without the start monitor. A genuine teardown still returns
     * {@code false}; a metadata failure does not.
     */
    private boolean prepareClientForUse(
            final @NotNull OpcUaClient client, final @NotNull SessionActivityListener activityListener) {
        final long timeoutMs = preparationTimeoutMs();
        try {
            awaitBoundedPreparation(client);
            browseMetadataPrepared.set(true);
            return true;
        } catch (final CancellationException e) {
            if (closed.get()) {
                log.debug("OPC UA metadata preparation was cancelled during teardown for adapter '{}'", adapterId);
                quietlyCloseClient(client, false, serviceFaultListener, activityListener);
                return false;
            }
            return continueWithoutBrowseMetadata("OPC UA metadata preparation was cancelled", e);
        } catch (final TimeoutException e) {
            return continueWithoutBrowseMetadata(
                    "OPC UA metadata preparation did not finish within " + timeoutMs + " milliseconds", e);
        } catch (final InterruptedException e) {
            // The one arm that abandons the attempt rather than continuing without the metadata, and the
            // difference is what the exception is about. Cancellation, timeout and execution failure are all
            // statements about the metadata build, which the connection does not need. An interrupt is a
            // statement about this thread -- so carrying on would run the rest of the connect sequence with
            // the flag re-set, and every blocking step after it (verification's futures, subscription
            // creation, a lock acquisition) would throw at once. The attempt fails either way; this way it
            // fails at the point that knows why.
            if (!closed.get()) {
                log.warn(
                        "OPC UA metadata preparation was interrupted for adapter '{}'; abandoning the attempt",
                        adapterId,
                        e);
            }
            // Cleanup first, interrupt flag restored after, and the order is load-bearing.
            // quietlyCloseClient ends in OpcUaClient.disconnect(), which is disconnectAsync().get() -- so with
            // the flag already set the get() throws InterruptedException at once, Milo wraps it as a
            // UaException, and this class logs "Failed to disconnect" over a teardown that never happened.
            // The session would then still be closing while the attempt returned and a retry was scheduled.
            // Catching InterruptedException cleared the flag for us, so the window to do the work is now.
            try {
                quietlyCloseClient(client, false, serviceFaultListener, activityListener);
            } finally {
                Thread.currentThread().interrupt();
            }
            return false;
        } catch (final ExecutionException e) {
            final Throwable cause = e.getCause() != null ? e.getCause() : e;
            return continueWithoutBrowseMetadata("Failed to prepare OPC UA browse metadata", cause);
        }
    }

    /**
     * Pushes the next rehydration out, further each time this one keeps failing.
     * <p>
     * Without a schedule the trigger is the health check, so a permanently restricted account would buy a
     * complete recursive browse of the data-type hierarchy every interval — as often as every ten seconds,
     * for the life of the connection, against a server that has already refused it. That is real traffic on
     * the device for an answer nobody expects to change soon.
     * <p>
     * The first failure is free: a build that timed out on a slow server is worth trying again promptly,
     * which also keeps the common transient case recovering within one interval. After that it doubles from
     * a minute to a half-hour cap. It does not stop, because the conditions that cause this — a permission
     * not yet granted, a server still starting up — are exactly the ones that come good later, and giving up
     * would restore the never-recovers behaviour this whole mechanism exists to remove.
     * <p>
     * Jitter because every adapter against one server would otherwise line up on the same schedule after a
     * shared outage and rebuild their trees in step.
     *
     * @return the delay applied, for logging.
     */
    private long scheduleNextRetry() {
        final int failures = metadataRetryFailures.incrementAndGet();
        long delayMs = 0L;
        if (failures > 1) {
            final int doublings = Math.min(failures - 2, 5);
            delayMs = Math.min(METADATA_RETRY_BASE_DELAY_MS << doublings, METADATA_RETRY_MAX_DELAY_MS);
            final long jitter = delayMs / 5;
            // Clamped after the jitter, not before. Applying it to an already-capped delay pushed the actual
            // maximum 20% past the ceiling this constant is named for -- a half-hour cap that could wait
            // thirty-six minutes, which is the sort of thing that is only ever discovered while chasing
            // something else.
            delayMs = Math.min(
                    delayMs + ThreadLocalRandom.current().nextLong(-jitter, jitter + 1), METADATA_RETRY_MAX_DELAY_MS);
        }
        nextMetadataRetryNanos.set(System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(delayMs));
        return delayMs;
    }

    /** The deadline for one metadata build. See {@link #METADATA_PREPARATION_MAX_WAIT_MS}. */
    private long preparationTimeoutMs() {
        return Math.min(config.getConnectionOptions().connectionTimeoutMs(), METADATA_PREPARATION_MAX_WAIT_MS);
    }

    /**
     * Runs one metadata build under a deadline, on a thread teardown can interrupt.
     * <p>
     * For the connect path only. The connecting thread has to get on with the rest of the attempt, so it
     * needs a bounded wait on a worker it can cancel — which is what the second thread and {@link
     * #preparationTask} are for, the latter also being how {@link #markClosed()} reaches an in-progress build.
     * <p>
     * Rehydration deliberately does not come through here. It is already off the caller's thread, so a
     * waiter would buy it nothing, and it needs the opposite guarantee: its worker's own liveness is the
     * guard against a second one, which a bounded wait cannot express. See {@link #retryBrowseMetadata}.
     */
    private void awaitBoundedPreparation(final @NotNull OpcUaClient client)
            throws InterruptedException, ExecutionException, TimeoutException {
        final FutureTask<Void> task = new FutureTask<>(() -> {
            prepareForUse.accept(client);
            return null;
        });
        preparationTask.set(task);

        // Ordered against markClosed() in the same two-sided shape as subscriptionHandler: if teardown read
        // a null task just before this publication, its `closed` write is what this side observes.
        if (closed.get()) {
            task.cancel(true);
        } else {
            Thread.ofVirtual().name("opcua-metadata-preparation-" + adapterId).start(task);
            if (closed.get()) {
                task.cancel(true);
            }
        }

        try {
            task.get(preparationTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (final TimeoutException | InterruptedException e) {
            // Interrupting the build is what makes the deadline real: without it the request loop carries on
            // against a server nobody is waiting for any more.
            task.cancel(true);
            throw e;
        } finally {
            preparationTask.compareAndSet(task, null);
        }
    }

    /**
     * Reports metadata the connection could not hydrate, and carries on connecting anyway.
     * <p>
     * WARN rather than ERROR, and no status change, because nothing has failed that the adapter cannot do:
     * the attempt goes on to verify and subscribe its tags, and events and values will flow.
     * <p>
     * Two things the wording has to get right, both learned by getting them wrong. It runs <em>before</em>
     * the subscription, the context and {@code CONNECTED}, so it cannot say the adapter is connected — the
     * attempt may still fail, or be stopped, and an event asserting a healthy connection next to an
     * {@code ERROR} status is worse than no event. It is conditional for that reason. And it must not
     * suggest retrying the browse: the REST layer refuses that before it reaches the browser, so the one
     * action an operator would naturally take is the one that cannot help. {@link #retryBrowseMetadata} is
     * what reopens browse, on the health-check interval and with backoff.
     *
     * @return always {@code true}: this is not a reason to abandon the attempt.
     */
    private boolean continueWithoutBrowseMetadata(final @NotNull String message, final @NotNull Throwable cause) {
        log.warn(
                "{} for adapter '{}'. The connection attempt continues and its tags are unaffected; if it "
                        + "succeeds, the browse endpoint will report not-ready until the metadata is built. The "
                        + "adapter retries the build on later health checks, backing off while it keeps failing, "
                        + "and reopens browse when one succeeds -- retrying the browse itself does not, because "
                        + "it is refused before it runs. A server that denies browsing the data-type hierarchy "
                        + "to the configured identity, or is slow to serve it, will keep reporting this.",
                message,
                adapterId,
                cause);
        eventService
                .createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                .withMessage(message + " for adapter '" + adapterId
                        + "'. The connection attempt continues; if it succeeds, browse stays not-ready until a "
                        + "later health check rebuilds the metadata.")
                .withSeverity(Event.SEVERITY.WARN)
                .fire();
        return true;
    }

    /** Whether this connection managed to hydrate its browse metadata during start. */
    boolean hasBrowseMetadata() {
        return browseMetadataPrepared.get();
    }

    /**
     * Tries once more to build the browse metadata on a connection that is otherwise healthy.
     * <p>
     * Without this the degraded state is permanent, and says otherwise. Browse readiness is decided once, at
     * {@code CONNECTED}, from {@link #hasBrowseMetadata()}; the REST layer refuses a browse with 503 and
     * {@code Retry-After} <em>before</em> it reaches the browser, so retrying a browse — the one action the
     * operator is invited to take — cannot be what rebuilds the tree. A southbound write does rebuild Milo's
     * tree, but nothing tells the adapter, so browse stays shut. The result was a connection reporting a
     * transient failure that only a reconnect could clear, on an adapter with no reason to reconnect.
     * <p>
     * Driven from the health check because that is already the periodic "is this connection still all right"
     * pass, and a metadata build the server refused at connect time is exactly the sort of thing that comes
     * good later — a permission granted, a slow server that has finished starting.
     * <p>
     * One worker, and its liveness <em>is</em> the guard: {@link #metadataRetryWorker} is set before it
     * starts and cleared by the worker itself, so "a retry is running" cannot be true of a thread that has
     * already gone or false of one that has not. An earlier version bounded the build on a second thread and
     * released the guard when that <em>wait</em> expired, which is not the same event. Milo's {@code
     * NonBlockingLazy} makes the difference concrete: a worker arriving while a southbound write is already
     * building the tree waits in {@code CompletableFuture.join()}, which ignores interruption — so cancelling
     * the wait left the worker alive, released the guard, and let the next interval start a second one
     * against the same server, with the first no longer reachable by teardown.
     * <p>
     * The deadline belongs to the build, so it is timed from when the build starts rather than checked when
     * the caller next happens to run. Tying it to the caller looked equivalent and is not: the health-check
     * interval is configurable up to five minutes while this deadline can be as short as two seconds, so a
     * stalled worker could hold the guard for a hundred and fifty times its allowance. The one-shot timer
     * only interrupts, and only the worker it was created for — clearing the guard stays with the worker,
     * because that is the event the guard is about.
     *
     * @param onHydrated run only if this attempt succeeds while the connection is still open.
     */
    void retryBrowseMetadata(final @NotNull Runnable onHydrated) {
        if (browseMetadataPrepared.get() || closed.get() || metadataRetryWorker.get() != null) {
            return;
        }
        // Subtraction rather than a bare comparison, because nanoTime() has no epoch and may be negative.
        if (System.nanoTime() - nextMetadataRetryNanos.get() < 0) {
            return;
        }
        final OpcUaClient client = client().orElse(null);
        if (client == null) {
            return;
        }

        final Thread worker = Thread.ofVirtual()
                .name("opcua-metadata-rehydration-" + adapterId)
                .unstarted(() -> runRehydration(client, onHydrated));
        // Created unstarted so the slot can be claimed before the body can run and clear it.
        if (!metadataRetryWorker.compareAndSet(null, worker)) {
            return;
        }
        // Re-read after claiming, because the checks at the top are a look and this is the commit. Between
        // the two, a worker that was running can succeed -- publishing the metadata and releasing the slot --
        // and this caller would then find an empty slot and start a build for something already done.
        if (browseMetadataPrepared.get() || closed.get()) {
            metadataRetryWorker.compareAndSet(worker, null);
            return;
        }
        worker.start();
        // The shared JVM delayer rather than a scheduler of this class's own: one task per attempt, no
        // lifecycle to own, and the same mechanism the refresh coordinator already uses for its retries.
        CompletableFuture.delayedExecutor(preparationTimeoutMs(), TimeUnit.MILLISECONDS)
                .execute(() -> interruptIfStillBuilding(worker));
    }

    /**
     * Interrupts one rehydration worker at its deadline, and no other.
     * <p>
     * The identity check is the whole of the condition, and deliberately so. It is what makes a late timer
     * harmless — by the time it fires, the attempt it was created for may have finished and a later one may
     * be running, and interrupting that one would cut short a build that has had no time at all — and it is
     * sufficient on its own, because exactly one of these is scheduled per worker.
     * <p>
     * An earlier version also required a shared "not yet interrupted" flag, reset by each new attempt. That
     * made one worker's timer able to consume the next worker's: an old timer passing its identity check,
     * then the old worker finishing and a new one resetting the flag, then the old timer setting it — after
     * which the new timer found the flag taken and the new worker was never interrupted at all.
     */
    private void interruptIfStillBuilding(final @NotNull Thread worker) {
        if (metadataRetryWorker.get() == worker) {
            log.debug(
                    "OPC UA adapter '{}': the browse-metadata build has passed its deadline; interrupting it.",
                    adapterId);
            worker.interrupt();
        }
    }

    /** One rehydration attempt, on its own thread. Owns the guard it was started under. */
    private void runRehydration(final @NotNull OpcUaClient client, final @NotNull Runnable onHydrated) {
        try {
            try {
                prepareForUse.accept(client);
            } catch (final RuntimeException e) {
                final long delayMs = scheduleNextRetry();
                log.debug(
                        "OPC UA adapter '{}': browse metadata is still unavailable; the next attempt is in about "
                                + "{} ms.",
                        adapterId,
                        delayMs,
                        e);
                return;
            }
            // Checked after the build, not only before it: a teardown that ran while this was in flight has
            // already discarded the connection, and promoting readiness for it would reopen browse against a
            // session nobody holds.
            if (closed.get()) {
                return;
            }
            browseMetadataPrepared.set(true);
            metadataRetryFailures.set(0);
            log.info(
                    "OPC UA adapter '{}': browse metadata is now available; the browse endpoint is ready again.",
                    adapterId);
            onHydrated.run();
        } finally {
            // After everything this worker does, not merely after the build. Releasing it before publishing
            // success left a window in which the metadata was not yet marked present and the slot was already
            // free, so a health check landing there would start a second worker -- and both would announce
            // the recovery and run the callback.
            metadataRetryWorker.compareAndSet(Thread.currentThread(), null);
        }
    }

    void stop() {
        log.info("Stopping OPC UA client");
        // Before the monitor, not behind it. A start() in progress holds this instance's lock for the whole
        // startup sequence, so a synchronized stop() could not reach either cancellation seam. Metadata
        // preparation is interrupted immediately; verification is reduced to the one timed phase already in
        // progress, whose ceiling is ten seconds.
        markClosed();
        closeContext(true);
    }

    void destroy() {
        log.info("Destroying OPC UA client");
        markClosed();
        closeContext(false);
    }

    /**
     * Records that this connection is finished with, cancels metadata preparation, and tells the handler to
     * stop verifying tags.
     * <p>
     * Deliberately unsynchronized and idempotent, so it can run while {@link #start} holds the monitor — that
     * is the whole point of it. The ordering is what makes it safe: {@code closed} is published before the
     * preparation task and handler are read, while {@code start()} publishes each one before it re-reads
     * {@code closed}. Whichever side goes second therefore sees the other's write, so neither cancellation
     * seam can be missed.
     */
    private void markClosed() {
        closed.set(true);
        final FutureTask<Void> preparation = preparationTask.get();
        if (preparation != null) {
            preparation.cancel(true);
        }
        // A rehydration is not reachable through preparationTask -- it owns its own thread precisely so its
        // liveness can be the guard -- so teardown has to name it separately or a build started by the health
        // check would run on past the connection it belongs to.
        final Thread rehydration = metadataRetryWorker.get();
        if (rehydration != null) {
            rehydration.interrupt();
        }
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
     * the time this waits, the start it is waiting for will cancel metadata preparation or abandon its
     * verification and then refuse to install its context. Metadata preparation is interrupted directly;
     * verification rechecks abandonment after each timed phase, so the remaining wait is bounded by the
     * phase already in progress (currently a ten-second ceiling) rather than by the rest of that tag or every
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
