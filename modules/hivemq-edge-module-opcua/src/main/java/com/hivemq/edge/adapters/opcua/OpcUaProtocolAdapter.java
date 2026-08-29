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

import com.hivemq.adapter.sdk.api.ProtocolAdapterConnectionDirection;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.discovery.NodeTree;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryInput;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationInput;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationOutput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.writing.WritingContext;
import com.hivemq.adapter.sdk.api.writing.WritingInput;
import com.hivemq.adapter.sdk.api.writing.WritingOutput;
import com.hivemq.adapter.sdk.api.writing.WritingPayload;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.edge.adapters.browse.BrowseException;
import com.hivemq.edge.adapters.browse.BrowsedNode;
import com.hivemq.edge.adapters.browse.BulkTagBrowser;
import com.hivemq.edge.adapters.opcua.browse.OpcUaNodeBrowser;
import com.hivemq.edge.adapters.opcua.client.Failure;
import com.hivemq.edge.adapters.opcua.client.ParsedConfig;
import com.hivemq.edge.adapters.opcua.client.Success;
import com.hivemq.edge.adapters.opcua.condition.ConditionSchemas;
import com.hivemq.edge.adapters.opcua.condition.ConditionUpdate;
import com.hivemq.edge.adapters.opcua.condition.ConditionUpdateWriter;
import com.hivemq.edge.adapters.opcua.condition.RefreshCommand;
import com.hivemq.edge.adapters.opcua.config.ConnectionOptions;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.tag.EventFieldSet;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagKind;
import com.hivemq.edge.adapters.opcua.listeners.OpcUaServiceFaultListener;
import com.hivemq.edge.adapters.opcua.southbound.JsonSchemaGenerator;
import com.hivemq.edge.adapters.opcua.southbound.JsonToOpcUAConverter;
import com.hivemq.edge.adapters.opcua.southbound.OpcUaPayload;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpcUaProtocolAdapter implements WritingProtocolAdapter, BulkTagBrowser {
    private static final @NotNull Logger log = LoggerFactory.getLogger(OpcUaProtocolAdapter.class);

    /** OPC UA's standard Objects folder — the conventional entry point when a browse names no root. */
    private static final @NotNull String OBJECTS_FOLDER_NODE_ID = "i=85";

    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull String adapterId;
    private final @NotNull Map<String, OpcuaTag> tagNameToTag;
    private final @NotNull List<OpcuaTag> tagList;
    private final @NotNull AtomicReference<OpcUaClientConnection> opcUaClientConnection;

    /** Serializes connection-generation changes with every generation-owned status publication. */
    private final @NotNull ReentrantLock connectionOwnershipLock = new ReentrantLock();

    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull OpcUaSpecificAdapterConfig config;
    private final @NotNull AtomicReference<ScheduledFuture<?>> retryFuture = new AtomicReference<>();
    private final @NotNull AtomicReference<ScheduledFuture<?>> healthCheckFuture = new AtomicReference<>();

    private final @NotNull OpcUaServiceFaultListener opcUaServiceFaultListener;
    // Retry attempt tracking for exponential backoff
    private final @NotNull AtomicLong reconnectAttempts = new AtomicLong(0);
    private final @NotNull AtomicLong lastReconnectTimestamp = new AtomicLong(0);
    private final @NotNull AtomicInteger consecutiveRetryAttempts = new AtomicInteger(0);

    // Lock to prevent concurrent reconnections
    private final @NotNull ReentrantLock reconnectLock = new ReentrantLock();
    private volatile @Nullable ScheduledExecutorService retryScheduler = null;
    private volatile @Nullable ScheduledExecutorService healthCheckScheduler = null;
    // Stored for reconnection - set during start()
    private volatile @Nullable ParsedConfig parsedConfig;
    private volatile @Nullable ModuleServices moduleServices;

    // Last-known-good client reference for browse operations during adapter restarts.
    // Set when a connection succeeds, cleared only on explicit stop (not during reconnect).
    private final @NotNull AtomicReference<OpcUaClient> browseClient = new AtomicReference<>();

    // Serialise all browse operations against this adapter to one at a time. Every REST browse call shares a
    // single Milo OpcUaClient against one physical device; resource-constrained PLCs (e.g. S7-1500) return
    // non-deterministic node counts when browseAsync/browseNext requests overlap. Owning the permit here makes
    // the serialisation effective across concurrent calls, not just within a single browse (EDG-576).
    static final int MAX_CONCURRENT_BROWSES = 1;
    private final @NotNull Semaphore browseConcurrency = new Semaphore(MAX_CONCURRENT_BROWSES);

    // EDG-577: browse-readiness remains an OPC-UA-local detail used by the REST browse endpoint, but it is now
    // ordered with public connection readiness. Milo activates the session before the namespace table and
    // data-type tree are hydrated; a browse in that window is non-deterministic. Every attempt resets this to
    // WARMING_UP, and the connection publishes CONNECTED only after the warm-up flips it to BROWSE_READY.
    private enum ReadinessStatus {
        WARMING_UP,
        BROWSE_READY
    }

    private final @NotNull AtomicReference<ReadinessStatus> browseReadiness =
            new AtomicReference<>(ReadinessStatus.WARMING_UP);

    // Flag to prevent scheduling after stop
    private volatile boolean stopped = false;

    /**
     * Whether this adapter owns its runtime resources — the lifecycle, as distinct from any one connection.
     * <p>
     * {@code opcUaClientConnection} used to stand in for this, and it cannot: that reference is the
     * <em>current connection attempt</em>, and an attempt that fails clears it while the adapter is still
     * very much started, with its retry and health-check schedulers running and a retry queued. A duplicate
     * {@code start()} arriving in that window — which is the ordinary "hardware is not online yet" window,
     * not an exotic one — passed both guards, installed a second connection and called
     * {@link #startSchedulers()}, replacing the first pair. CI observed exactly that ordering.
     * <p>
     * Claimed once the start is committed and released only by {@link #stop} or {@link #destroy}, so it does
     * not move when a connection comes and goes. Distinct from {@link #stopped}, which answers a different
     * question — "should background work keep running" — and is false on a freshly constructed adapter that
     * has never been started, precisely the state in which a first start must be allowed.
     */
    private final @NotNull AtomicBoolean started = new AtomicBoolean();

    public OpcUaProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<OpcUaSpecificAdapterConfig> input) {
        this.adapterId = input.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.tagList = input.getTags().stream().map(tag -> (OpcuaTag) tag).toList();
        this.tagNameToTag = tagList.stream().collect(Collectors.toMap(OpcuaTag::getName, Function.identity()));
        this.protocolAdapterMetricsService = input.getProtocolAdapterMetricsHelper();
        this.config = input.getConfig();
        this.opcUaClientConnection = new AtomicReference<>();
        this.opcUaServiceFaultListener = new OpcUaServiceFaultListener(
                protocolAdapterMetricsService,
                input.moduleServices().eventService(),
                adapterId,
                this::reconnect,
                config.getConnectionOptions().autoReconnect());
    }

    /**
     * Calculates backoff delay based on the number of consecutive retry attempts.
     * Parses the comma-separated retryIntervalMs string and returns the appropriate delay.
     * If attemptCount exceeds the number of configured delays, returns the last configured delay.
     *
     * @param retryIntervalMs comma-separated string of backoff delays in milliseconds
     * @param attemptCount    the number of consecutive retry attempts (1-indexed)
     * @return the backoff delay in milliseconds
     * @throws NumberFormatException when the format is incorrect
     */
    @SuppressWarnings("StringSplitter")
    public static long calculateBackoffDelayMs(final @NotNull String retryIntervalMs, final int attemptCount) {
        final String[] delayStrings = retryIntervalMs.split(",");
        final long[] backoffDelays = new long[delayStrings.length];

        for (int i = 0; i < delayStrings.length; i++) {
            // NumberFormatException is thrown.
            backoffDelays[i] = Long.parseLong(delayStrings[i].trim());
        }

        // Array is 0-indexed, attemptCount is 1-indexed, so we need attemptCount - 1
        final int index = Math.min(Math.max(0, attemptCount - 1), backoffDelays.length - 1);
        final double backoffDelay = backoffDelays[index]
                * (1 + ThreadLocalRandom.current().nextDouble(ConnectionOptions.DEFAULT_RETRY_JITTER));
        return (long) backoffDelay;
    }

    @Override
    public @NotNull String getId() {
        return adapterId;
    }

    public long getReconnectAttempts() {
        return reconnectAttempts.get();
    }

    @Override
    public synchronized void start(
            final @NotNull ProtocolAdapterConnectionDirection direction,
            final @NotNull ProtocolAdapterStartInput input,
            final @NotNull ProtocolAdapterStartOutput output) {
        // OPC UA uses a single client for both northbound and southbound.
        // The full connection setup happens during Northbound connect;
        // Southbound connect is a no-op since the shared client is already available.
        if (direction == ProtocolAdapterConnectionDirection.Southbound) {
            output.startedSuccessfully();
            return;
        }

        log.info("Starting OPC UA protocol adapter {}", adapterId);

        // Refused before anything is built, rather than after, and asked of the lifecycle rather than of the
        // connection. Everything below this point either allocates a runtime resource or hands one to a
        // background thread, so a caller's mistake is better answered before two executors exist for it.
        //
        // `started` rather than `opcUaClientConnection != null`, which is what this used to read. That
        // reference is the current connection *attempt*: an attempt that fails clears it, and the adapter
        // goes on being started with its schedulers running and a retry queued. A second start() in that
        // window -- the ordinary "hardware is not online yet" window -- passed this guard and the
        // compareAndSet below, then replaced both schedulers through startSchedulers(). The connection
        // reference is still the authority for *connections*, which a reconnect claims from another thread
        // without holding this lock; it was never the authority for the lifecycle.
        if (started.get()) {
            log.error("Cannot start OPC UA protocol adapter '{}' - adapter is already started", adapterId);
            output.failStart(
                    new IllegalStateException("Adapter already started"),
                    "Cannot start already started adapter. Please stop the adapter first.");
            return;
        }

        // Reset stopped flag
        stopped = false;

        // Every synchronous check the configuration can fail happens before any runtime resource exists.
        // The schedulers used to start first, and neither failure path below shut them down again -- so a
        // rejected start left two executor threads running until some later lifecycle call happened to
        // collect them, and repeated invalid starts accumulated them.
        //
        // At most one refresh tag. A second would place a second item on the Server object, and since the
        // refresh bracket is copied to every notifier item in the subscription, both would publish the same
        // event -- a duplicate that looks like two refreshes.
        //
        // Failing the whole adapter rather than dropping the extra tag, and the rule is firmer than "this is
        // probably a mistake". Two things can be wrong with a tag and they deserve opposite treatments. A
        // configuration that is not *valid* is a defect in what the user wrote, decidable by reading the
        // configuration alone: no device is involved and connecting could not produce a different answer.
        // That stops the adapter. A tag in a valid configuration that cannot be *verified against the device*
        // is a reasonable statement about the world that turns out to be false -- it was sensible to assume
        // this node is a condition of that type, and the server says otherwise. That is handled locally: drop
        // the tag, name it, keep going.
        //
        // Two REFRESH tags fall on the first side. No browse, no session, no round trip could decide it
        // differently, which is why this check sits here -- before ParsedConfig.fromConfig and before any
        // connection object exists. It is part of validating the configuration, not part of subscribing.
        // NotifierResolver.resolve is the second kind by construction, since the answer depends on what the
        // server exposes, and it is right to be per-tag; the two are different rules rather than an
        // inconsistency.
        final List<String> refreshTagNames = tagList.stream()
                .filter(tag -> tag.getDefinition().getKind() == OpcuaTagKind.REFRESH)
                .map(OpcuaTag::getName)
                .toList();
        if (refreshTagNames.size() > 1) {
            final String message = "An OPC UA adapter may have at most one REFRESH tag, but '" + adapterId
                    + "' has " + refreshTagNames.size() + ": " + String.join(", ", refreshTagNames)
                    + ". They would each publish the same refresh events.";
            log.error(message);
            output.failStart(new IllegalStateException(message), message);
            return;
        }

        final ParsedConfig newlyParsedConfig;
        final var result = ParsedConfig.fromConfig(config);
        if (result instanceof Failure<ParsedConfig, String>(final String failure)) {
            log.error("Failed to parse configuration for OPC UA client: {}", failure);
            output.failStart(new IllegalStateException(failure), "Failed to parse configuration for OPC UA client");
            return;
        } else if (result instanceof Success<ParsedConfig, String>(final ParsedConfig successfullyParsedConfig)) {
            newlyParsedConfig = successfullyParsedConfig;
            // Store for reconnection
            this.parsedConfig = successfullyParsedConfig;
            this.moduleServices = input.moduleServices();
        } else {
            output.failStart(
                    new IllegalStateException(
                            "Unexpected result type: " + result.getClass().getName()),
                    "Failed to parse configuration for OPC UA client");
            return;
        }

        final OpcUaClientConnection conn = new OpcUaClientConnection(
                adapterId,
                tagList,
                protocolAdapterState,
                input.moduleServices().protocolAdapterTagStreamingService(),
                input.moduleServices().eventService(),
                protocolAdapterMetricsService,
                config,
                opcUaServiceFaultListener,
                this::publishStatusFrom,
                this::warmUpBrowseMetadata,
                this::publishReadyFrom);
        if (claimConnection(conn)) {
            // The lifecycle is claimed here, with the slot owned and the configuration already validated, so
            // a start refused for a bad configuration leaves nothing behind to block a corrected retry.
            // Released only by stop() or destroy(), never by a connection attempt ending.
            started.set(true);
            // Only now, with the connection slot owned. The configuration was validated above, so this is no
            // longer about whether the resources are wanted -- it is about whether this call is the one
            // entitled to create them. They used to be created before the swap and were therefore replaced
            // wholesale by a duplicate start: the original pair went on running the retry and health-check
            // work of the connection that was still live, with nothing left holding a reference to them, so
            // no later stop() or destroy() could ever collect them.
            startSchedulers();

            publishConnectingFrom(conn);
            // Attempt initial connection asynchronously
            attemptConnection(conn, newlyParsedConfig, input);

            // Adapter starts successfully even if connection isn't established yet
            // Hardware may come online later and automatic retry will connect
            log.info("Successfully started OPC UA protocol adapter {}", adapterId);
            output.startedSuccessfully();
        } else {
            log.error("Cannot start OPC UA protocol adapter '{}' - adapter is already started", adapterId);
            output.failStart(
                    new IllegalStateException("Adapter already started"),
                    "Cannot start already started adapter. Please stop the adapter first.");
        }
    }

    @Override
    public synchronized void stop(
            final @NotNull ProtocolAdapterConnectionDirection direction,
            final @NotNull ProtocolAdapterStopInput input,
            final @NotNull ProtocolAdapterStopOutput output) {
        // OPC UA uses a single client for both northbound and southbound.
        // The full teardown happens during Northbound disconnect;
        // Southbound disconnect is a no-op since the shared client will be torn down later.
        if (direction == ProtocolAdapterConnectionDirection.Southbound) {
            output.stoppedSuccessfully();
            return;
        }

        log.info("Stopping OPC UA protocol adapter {}", adapterId);

        // Set stopped flag to prevent new scheduling
        stopped = true;
        // And release the lifecycle, so a later start() is a first start rather than a duplicate.
        started.set(false);

        // Acquire reconnect lock to ensure we don't stop while reconnecting
        reconnectLock.lock();
        try {
            // Cancel any pending retries and health checks
            cancelRetry();
            cancelHealthCheck();

            // Shutdown schedulers immediately to prevent new tasks
            shutdownSchedulers();

            // Clear stored configuration to prevent reconnection after stop
            this.parsedConfig = null;
            this.moduleServices = null;
            // Clear browse client on explicit stop
            this.browseClient.set(null);

            final OpcUaClientConnection conn = releaseCurrentConnection();
            if (conn != null) {
                conn.stop();
            } else {
                log.info("Tried stopping stopped OPC UA protocol adapter {}", adapterId);
            }
            // The wrapper owns the final lifecycle status. In particular, this stop may be failed-start
            // cleanup: the wrapper deliberately preserves ERROR while draining its connection FSM back to
            // Disconnected. Publishing here would erase that actionable configuration failure. An ordinary
            // wrapper stop completes the shared status after the adapter teardown has finished.
        } finally {
            reconnectLock.unlock();
        }
        output.stoppedSuccessfully();
    }

    /**
     * Triggers reconnection by stopping the current connection and creating a new one.
     * Used for runtime reconnection when health check detects issues.
     * Requires that start() has been called previously to initialize parsedConfig and moduleServices.
     * Uses tryLock to prevent concurrent reconnections without blocking the health check scheduler.
     */
    private void reconnect() {
        // Try to acquire lock - return immediately if another reconnection is in progress
        if (!reconnectLock.tryLock()) {
            log.debug("Reconnection already in progress for adapter '{}' - skipping", adapterId);
            return;
        }

        try {
            // Check if adapter has been stopped
            if (stopped) {
                log.debug("Skipping reconnection for adapter '{}' - adapter has been stopped", adapterId);
                return;
            }

            final long currentTime = System.currentTimeMillis();
            final long lastReconnectTime = lastReconnectTimestamp.get();
            if (reconnectAttempts.get() > 0) {
                long backoffDelayMs;
                try {
                    backoffDelayMs = calculateBackoffDelayMs(
                            config.getConnectionOptions().retryIntervalMs(), (int) reconnectAttempts.get());
                } catch (final Exception e) {
                    backoffDelayMs = calculateBackoffDelayMs(
                            ConnectionOptions.DEFAULT_RETRY_INTERVALS, (int) reconnectAttempts.get());
                }
                if (currentTime - lastReconnectTime < backoffDelayMs) {
                    log.debug(
                            "Reconnection for adapter '{}' attempted too soon after last reconnect - skipping",
                            adapterId);
                    return;
                }
            }
            reconnectAttempts.incrementAndGet();
            lastReconnectTimestamp.set(currentTime);

            log.info("Reconnecting OPC UA adapter '{}'", adapterId);

            // Verify we have the necessary configuration
            if (parsedConfig == null || moduleServices == null) {
                log.error("Cannot reconnect OPC UA adapter '{}' - adapter has not been started yet", adapterId);
                return;
            }

            // Reset retry counter for fresh reconnection attempt with exponential backoff
            consecutiveRetryAttempts.set(0);

            // Cancel any pending retries and health checks
            cancelRetry();
            cancelHealthCheck();

            // Stop and clean up current connection
            final OpcUaClientConnection oldConn = releaseCurrentConnection();
            if (oldConn != null) {
                oldConn.stop();
                log.debug("Stopped old connection for OPC UA adapter '{}'", adapterId);
            }

            // Create new connection
            final OpcUaClientConnection newConn = new OpcUaClientConnection(
                    adapterId,
                    tagList,
                    protocolAdapterState,
                    moduleServices.protocolAdapterTagStreamingService(),
                    moduleServices.eventService(),
                    protocolAdapterMetricsService,
                    config,
                    opcUaServiceFaultListener,
                    this::publishStatusFrom,
                    this::warmUpBrowseMetadata,
                    this::publishReadyFrom);

            // Set as current connection and attempt connection with retry logic
            if (claimConnection(newConn)) {
                publishConnectingFrom(newConn);
                // Create a minimal ProtocolAdapterStartInput for attemptConnection
                final ModuleServices ms = Objects.requireNonNull(moduleServices);
                final ProtocolAdapterStartInput input = new ProtocolAdapterStartInput() {
                    @Override
                    public @NotNull ModuleServices moduleServices() {
                        return ms;
                    }
                };
                attemptConnection(newConn, parsedConfig, input);
            } else {
                log.warn(
                        "OPC UA adapter '{}' reconnect failed - another connection was created concurrently",
                        adapterId);
            }
        } finally {
            reconnectLock.unlock();
        }
    }

    /**
     * Schedules periodic health check that monitors connection health and triggers reconnection if needed.
     */
    private void scheduleHealthCheck() {
        // Check if adapter has been stopped
        if (stopped) {
            log.debug("Skipping health check scheduling for adapter '{}' - adapter has been stopped", adapterId);
            return;
        }

        final long healthCheckIntervalMs = config.getConnectionOptions().healthCheckIntervalMs();
        final ScheduledFuture<?> future = Objects.requireNonNull(healthCheckScheduler)
                .scheduleAtFixedRate(
                        () -> {
                            // Check if adapter was stopped before health check executes
                            if (stopped) {
                                log.debug("Health check skipped for adapter '{}' - adapter was stopped", adapterId);
                                return;
                            }

                            final OpcUaClientConnection conn = opcUaClientConnection.get();
                            if (conn == null) {
                                log.debug("Health check skipped - no active connection for adapter '{}'", adapterId);
                                return;
                            }

                            if (!conn.isHealthy()) {
                                if (config.getConnectionOptions().autoReconnect()) {
                                    log.warn(
                                            "Health check failed for adapter '{}' - triggering automatic reconnection",
                                            adapterId);
                                    reconnect();
                                } else {
                                    log.warn(
                                            "Health check failed for adapter '{}' - automatic reconnection is disabled",
                                            adapterId);
                                    publishStatusFrom(conn, ProtocolAdapterState.ConnectionStatus.ERROR);
                                }
                            } else {
                                log.debug("Health check passed for adapter '{}'", adapterId);
                                // A healthy connection that never got its browse metadata is the one case
                                // that cannot fix itself: the browse endpoint is refused before it runs, so
                                // the operator's natural retry never reaches the code that would rebuild the
                                // tree. This is the periodic pass that does reach it.
                                rehydrateBrowseMetadataIfMissing(conn);
                            }
                        },
                        healthCheckIntervalMs,
                        healthCheckIntervalMs,
                        TimeUnit.MILLISECONDS);

        // Store future so it can be cancelled if needed
        final ScheduledFuture<?> oldFuture = healthCheckFuture.getAndSet(future);
        if (oldFuture != null && !oldFuture.isDone()) {
            oldFuture.cancel(false);
        }

        log.debug(
                "Scheduled connection health check every {} milliseconds for adapter '{}'",
                healthCheckIntervalMs,
                adapterId);
    }

    /**
     * Reopens browse on a healthy connection whose metadata build failed at connect time.
     * <p>
     * Browse readiness is otherwise decided once and never revisited, which made a transient metadata
     * failure permanent for the life of the connection while the REST layer answered {@code Retry-After: 2}
     * — advertising as momentary a condition that only a reconnect could clear. The promotion goes through
     * the same ownership lock as every other readiness write, so a build that finishes just after the
     * adapter has moved to a new connection cannot reopen browse on the strength of the old one.
     */
    @VisibleForTesting
    void rehydrateBrowseMetadataIfMissing(final @NotNull OpcUaClientConnection conn) {
        if (browseReadiness.get() == ReadinessStatus.BROWSE_READY || stopped) {
            return;
        }
        conn.retryBrowseMetadata(() -> {
            connectionOwnershipLock.lock();
            try {
                if (opcUaClientConnection.get() == conn && !stopped) {
                    browseReadiness.set(ReadinessStatus.BROWSE_READY);
                } else {
                    log.debug(
                            "OPC UA adapter '{}': not reopening browse from a connection the adapter no longer owns",
                            adapterId);
                }
            } finally {
                connectionOwnershipLock.unlock();
            }
        });
    }

    /**
     * Cancels any pending health check.
     */
    private void cancelHealthCheck() {
        final ScheduledFuture<?> future = healthCheckFuture.getAndSet(null);
        if (future != null && !future.isDone()) {
            future.cancel(false);
            log.debug("Cancelled health check for adapter '{}'", adapterId);
        }
    }

    /**
     * Shuts down both retry and health check schedulers.
     * Uses immediate shutdown to cancel all pending tasks.
     */
    private synchronized void shutdownSchedulers() {
        // Shutdown retry scheduler - use shutdownNow() to cancel pending tasks immediately
        final var retryScheduler = this.retryScheduler;
        final var healthCheckScheduler = this.healthCheckScheduler;
        this.retryScheduler = null;
        this.healthCheckScheduler = null;
        if (retryScheduler != null && !retryScheduler.isShutdown()) {
            retryScheduler.shutdownNow();
            try {
                retryScheduler.awaitTermination(5, TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Shutdown health check scheduler - use shutdownNow() to cancel pending tasks immediately
        if (healthCheckScheduler != null && !healthCheckScheduler.isShutdown()) {
            healthCheckScheduler.shutdownNow();
            try {
                healthCheckScheduler.awaitTermination(5, TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Initiates both retry and health check schedulers.
     * <p>
     * Shuts down whatever was there first. The caller is expected to have established that nothing is, and
     * with the guards in {@link #start} nothing should be — but overwriting a live executor field loses the
     * only reference to threads that are still running scheduled work against this adapter, and no later
     * lifecycle call can collect what it cannot name. Closing them here costs nothing when the fields are
     * null, which is every ordinary call.
     */
    private synchronized void startSchedulers() {
        if (retryScheduler != null || healthCheckScheduler != null) {
            log.warn(
                    "Adapter '{}': schedulers already existed when starting; shutting them down rather than "
                            + "losing the reference to them",
                    adapterId);
            shutdownSchedulers();
        }
        retryScheduler = Executors.newSingleThreadScheduledExecutor();
        healthCheckScheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void destroy() {
        log.info("Destroying OPC UA protocol adapter {}", adapterId);

        // Released here as well as in stop(), because destroy() is reachable without one -- the framework may
        // discard an adapter it never stopped. Leaving it claimed would make the object permanently
        // unstartable, which matters because the same instance is reused across a configuration change.
        started.set(false);
        // For the same reason, and this one had teeth. destroy() used to clear only `started`, so a start
        // still connecting went on believing background work was wanted: its completion callback tests
        // `stopped`, found it false, and called scheduleHealthCheck() -- against the scheduler field that
        // shutdownSchedulers() had just set to null, three lines below. Cleared again by start(), which is
        // what makes this safe on an instance the framework reuses after a configuration change.
        stopped = true;

        // Cancel any pending retries and health checks
        cancelRetry();
        cancelHealthCheck();

        // Shutdown schedulers (if not already shutdown in stop())
        shutdownSchedulers();

        // The client it names is being disconnected below, so keeping it would only offer browse callers a
        // dead session. stop() has always cleared it; destroy() not doing so was an omission rather than a
        // distinction, and it is the more final of the two.
        this.browseClient.set(null);

        // Removing the generation and publishing the lifecycle-owned terminal status are one operation. A
        // concurrent restart cannot install its replacement between them and then be described as disconnected.
        final OpcUaClientConnection conn =
                releaseCurrentConnectionAndPublish(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
        if (conn != null) {
            @SuppressWarnings("unused")
            final var unused = CompletableFuture.runAsync(() -> {
                        conn.destroy();
                        log.info("Destroyed OPC UA protocol adapter {}", adapterId);
                    })
                    .whenComplete((ignored, error) -> {
                        if (error != null) {
                            log.warn("Problem closing the connection for adapter {}", adapterId, error);
                        }
                    });
        } else {
            log.info("Tried destroying stopped OPC UA protocol adapter {}", adapterId);
        }
    }

    @Override
    public void discoverValues(
            final @NotNull ProtocolAdapterDiscoveryInput input, final @NotNull ProtocolAdapterDiscoveryOutput output) {
        if (stopped) {
            log.debug("Discovery operation skipped for adapter '{}' - adapter has been stopped", adapterId);
            output.fail("Discovery failed: Adapter has been stopped");
            return;
        }
        // No root means "start from the top", which for OPC UA is the standard Objects folder (i=85). Failing
        // instead turned the ordinary "browse this server" call into a 500 with no indication of what was
        // missing; every caller that wanted the whole address space had to know to pass i=85 itself.
        final String rootNode = input.getRootNode() == null ? OBJECTS_FOLDER_NODE_ID : input.getRootNode();
        final OpcUaClientConnection conn = opcUaClientConnection.get();
        if (conn == null) {
            output.fail("Discovery failed: ClientConnection not connected or not initialized");
            return;
        }
        conn.client()
                .ifPresentOrElse(
                        client -> {
                            @SuppressWarnings("unused")
                            final var unused = OpcUaNodeDiscovery.discoverValues(client, rootNode, input.getDepth())
                                    .whenComplete((collectedNodes, throwable) -> {
                                        if (throwable == null) {
                                            final NodeTree nodeTree = output.getNodeTree();
                                            collectedNodes.forEach(node -> nodeTree.addNode(
                                                    node.id(),
                                                    node.name(),
                                                    node.value(),
                                                    node.description(),
                                                    node.parentId(),
                                                    node.nodeType(),
                                                    node.selectable()));
                                            output.finish();
                                        } else {
                                            log.error("Unable to discover the OPC UA server", throwable);
                                            output.fail(throwable, "Unable to discover values");
                                        }
                                    });
                        },
                        () -> output.fail("Discovery failed: Client not connected or not initialized"));
    }

    @Override
    public @NotNull Stream<BrowsedNode> browse(final @Nullable String rootId, final int maxDepth)
            throws BrowseException {
        if (stopped) {
            throw new BrowseException("Browse failed: Adapter has been stopped");
        }
        // Try primary connection first, fall back to last-known-good browse client snapshot.
        // This allows browse to work during adapter restarts triggered by tag imports.
        OpcUaClient client = null;
        final OpcUaClientConnection conn = opcUaClientConnection.get();
        if (conn != null) {
            client = conn.client().orElse(null);
        }
        if (client == null) {
            client = browseClient.get();
        }
        if (client == null) {
            throw new BrowseException("Browse failed: Client not connected");
        }
        return new OpcUaNodeBrowser(client, adapterId, 0, browseConcurrency).browse(rootId, maxDepth);
    }

    @Override
    public boolean isBrowseReady() {
        // Connected AND the address-space metadata has been hydrated by the post-connect warm-up (EDG-577).
        return !stopped && browseReadiness.get() == ReadinessStatus.BROWSE_READY;
    }

    @Override
    public @NotNull String resolveNodeId(final @NotNull String nodeId, final @Nullable String namespaceUri) {
        if (namespaceUri == null || namespaceUri.isEmpty()) {
            return nodeId;
        }
        // Try primary connection, fall back to browse client snapshot
        OpcUaClient client = null;
        final OpcUaClientConnection conn = opcUaClientConnection.get();
        if (conn != null) {
            client = conn.client().orElse(null);
        }
        if (client == null) {
            client = browseClient.get();
        }
        if (client == null) {
            return nodeId;
        }
        final NodeId parsed = NodeId.parseOrNull(nodeId);
        if (parsed == null) {
            return nodeId;
        }
        try {
            return parsed.reindex(client.getNamespaceTable(), namespaceUri).toParseableString();
        } catch (final Exception e) {
            log.debug("Could not resolve namespace URI '{}' for nodeId '{}': {}", namespaceUri, nodeId, e.getMessage());
            return nodeId;
        }
    }

    @Override
    public void write(final @NotNull WritingInput input, final @NotNull WritingOutput output) {
        if (stopped) {
            log.debug("Write operation skipped for adapter '{}' - adapter has been stopped", adapterId);
            output.fail("Write failed: Adapter has been stopped");
            return;
        }
        final WritingContext writeContext = input.getWritingContext();
        final OpcUaPayload opcUAWritePayload = (OpcUaPayload) input.getWritingPayload();
        final String tagName = writeContext.getTagName();
        final OpcuaTag opcuaTag = tagNameToTag.get(tagName);
        if (opcuaTag == null) {
            log.error("Attempted to write to non-existent tag '{}'", tagName);
            output.fail("Tag '" + tagName + "' not found.");
            return;
        }

        // Second line of defence. The tag's write schema already describes a shape that accepts nothing, but
        // a schema is a description and this is the refusal: an event subscription names a notifier, and
        // writing a value to a notifier is meaningless rather than merely unsupported.
        if (opcuaTag.getDefinition().getKind() == OpcuaTagKind.EVENT_SUBSCRIPTION) {
            log.error("Attempted to write to event subscription tag '{}', which is northbound only", tagName);
            output.fail("Tag '" + tagName + "' is an event subscription and cannot be written: it is a query "
                    + "against a notifier, not a node. To acknowledge an alarm, write to a CONDITION tag.");
            return;
        }

        final OpcUaClientConnection conn = opcUaClientConnection.get();
        if (conn == null) {
            output.fail("Discovery failed: ClientConnection not connected or not initialized");
            return;
        }

        conn.client()
                .ifPresentOrElse(
                        client -> {
                            final JsonToOpcUAConverter converter = new JsonToOpcUAConverter(client);
                            if (log.isDebugEnabled()) {
                                log.debug(
                                        "Write invoked with payload '{}' for tag '{}'",
                                        opcUAWritePayload,
                                        opcuaTag.getName());
                            }

                            // A refresh tag is written to ask for the current alarm picture. Its node plays
                            // no part: the call is made on the well-known ConditionType, and the only
                            // argument is the subscription id, which is ours rather than the caller's.
                            //
                            // Dispatched before anything parses that node, which is the whole of the fix
                            // here. Subscription verification deliberately does not parse a refresh tag's
                            // node either -- it passes null and says why -- so a tag with a placeholder
                            // there starts, subscribes and publishes control events perfectly well, and
                            // then threw on the one command it exists to accept. Worse than a failed write:
                            // the throw happened inside this consumer, where nothing maps it to a failure,
                            // so the WritingOutput was never completed at all.
                            if (opcuaTag.getDefinition().getKind() == OpcuaTagKind.REFRESH) {
                                requestRefresh(opcUAWritePayload, tagName, output);
                                return;
                            }

                            final NodeId nodeId = parseNodeOrFail(opcuaTag, tagName, output);
                            if (nodeId == null) {
                                return;
                            }

                            // A condition is moved by calling a method on it, not by assigning to it: the
                            // server owns the state machine. Everything else is an ordinary value write.
                            if (opcuaTag.getDefinition().getKind() == OpcuaTagKind.CONDITION) {
                                writeConditionUpdate(client, nodeId, opcUAWritePayload, tagName, output);
                                return;
                            }

                            final Object opcuaObject = converter.convertToOpcUAValue(opcUAWritePayload.value(), nodeId);

                            @SuppressWarnings("unused")
                            final var unused = client.writeValuesAsync(
                                            List.of(nodeId),
                                            List.of(new DataValue(Variant.of(opcuaObject), StatusCode.GOOD, null)))
                                    .whenComplete((statusCodes, throwable) -> {
                                        final var badStatus = statusCodes.stream()
                                                .filter(StatusCode::isBad)
                                                .findFirst();
                                        if (badStatus.isPresent()) {
                                            log.error("Failed to write tag '{}': {}", tagName, badStatus.get());
                                            output.fail("Failed to write tag '" + tagName + "': " + badStatus.get());
                                        } else if (throwable == null) {
                                            log.debug("Successfully wrote tag '{}'", opcuaTag.getName());
                                            output.finish();
                                        } else {
                                            log.error("Exception while writing tag '{}'", tagName, throwable);
                                            output.fail(throwable, null);
                                        }
                                    });
                        },
                        () -> output.fail("Discovery failed: Client not connected or not initialized"));
    }

    /**
     * Parses the node a write is aimed at, answering the write rather than throwing when it cannot.
     * <p>
     * {@code NodeId.parse} throws an unchecked exception, and this runs inside the consumer of an
     * {@code ifPresentOrElse} — so a throw leaves the {@link WritingOutput} uncompleted rather than failed,
     * which is the difference between a write that reports an error and one that never answers.
     *
     * @return the parsed node, or {@code null} when the write has already been failed.
     */
    private @Nullable NodeId parseNodeOrFail(
            final @NotNull OpcuaTag opcuaTag, final @NotNull String tagName, final @NotNull WritingOutput output) {

        final String node = opcuaTag.getDefinition().getNode();
        try {
            return NodeId.parse(node);
        } catch (final Exception e) {
            log.error("Cannot write to tag '{}': '{}' is not a node id", tagName, node, e);
            output.fail("Cannot write to tag '" + tagName + "': '" + node + "' is not a node id");
            return null;
        }
    }

    /**
     * Handles a southbound write to a refresh tag: validate the command, then ask for a refresh.
     * <p>
     * Nothing from the payload reaches the server — the command carries no argument, and the subscription id
     * is the connection's. The payload exists to name the action, and validating it is what stops a write
     * meant for a different tag from silently triggering a refresh.
     */
    private void requestRefresh(
            final @NotNull OpcUaPayload payload, final @NotNull String tagName, final @NotNull WritingOutput output) {

        try {
            RefreshCommand.validate(payload.value());
        } catch (final IllegalArgumentException e) {
            log.error("Rejected refresh command for tag '{}': {}", tagName, e.getMessage());
            output.fail("Rejected refresh command for tag '" + tagName + "': " + e.getMessage());
            return;
        }

        final OpcUaClientConnection conn = opcUaClientConnection.get();
        if (conn == null) {
            output.fail("Cannot refresh: the OPC UA connection is not established");
            return;
        }

        conn.requestConditionRefresh()
                .ifPresentOrElse(
                        pending -> {
                            @SuppressWarnings("unused")
                            final var unused = pending.whenComplete((statusCode, throwable) -> {
                                if (throwable != null) {
                                    log.error("Exception while refreshing conditions for tag '{}'", tagName, throwable);
                                    output.fail(throwable, null);
                                } else if (statusCode.isBad()) {
                                    log.error(
                                            "Server refused a condition refresh for tag '{}': {}", tagName, statusCode);
                                    output.fail("The server refused the refresh: " + statusCode);
                                } else {
                                    log.debug("Requested a condition refresh via tag '{}'", tagName);
                                    output.finish();
                                }
                            });
                        },
                        () -> output.fail("Cannot refresh: no subscription is established yet"));
    }

    /**
     * Requests a state transition on a condition, as asked for by a southbound message.
     * <p>
     * A malformed command fails the write rather than being interpreted generously: the {@code eventId}
     * identifies one specific transition, so guessing at a command risks acknowledging something other than
     * what was intended.
     */
    private void writeConditionUpdate(
            final @NotNull OpcUaClient client,
            final @NotNull NodeId conditionNodeId,
            final @NotNull OpcUaPayload payload,
            final @NotNull String tagName,
            final @NotNull WritingOutput output) {

        final ConditionUpdate update;
        try {
            update = ConditionUpdate.fromJson(payload.value());
        } catch (final IllegalArgumentException e) {
            log.error("Rejected condition update for tag '{}': {}", tagName, e.getMessage());
            output.fail("Rejected condition update for tag '" + tagName + "': " + e.getMessage());
            return;
        }

        @SuppressWarnings("unused")
        final var unused = ConditionUpdateWriter.requestTransition(client, conditionNodeId, update)
                .whenComplete((statusCode, throwable) -> {
                    if (throwable != null) {
                        log.error("Exception while updating condition tag '{}'", tagName, throwable);
                        output.fail(throwable, null);
                    } else if (statusCode.isBad()) {
                        log.error("Failed to {} condition tag '{}': {}", update.method(), tagName, statusCode);
                        output.fail("Failed to " + update.method() + " condition tag '" + tagName + "': " + statusCode);
                    } else {
                        log.debug("Successfully requested {} on condition tag '{}'", update.method(), tagName);
                        output.finish();
                    }
                });
    }

    @Override
    public void createTagSchema(
            final @NotNull TagSchemaCreationInput input, final @NotNull TagSchemaCreationOutput output) {
        if (stopped) {
            log.debug("Create tag schema operation skipped for adapter '{}' - adapter is not started", adapterId);
            output.adapterNotStarted();
            return;
        }
        final String tagName = input.getTagName();
        final OpcuaTag tag = tagNameToTag.get(tagName);
        if (tag == null) {
            log.error("Cannot create schema for non-existent tag '{}'", tagName);
            output.tagNotFound("Tag '" + tagName + "' not found.");
            return;
        }

        // An event tag's shape follows from its declared type, not from the device, so both schemas are known
        // before a connection exists and this answers without one. It is also where read and write genuinely
        // differ — a transition report northbound, and southbound either a command to move the state machine
        // or nothing at all.
        final OpcuaTagKind tagKind = tag.getDefinition().getKind();
        if (tagKind != OpcuaTagKind.VALUE) {
            // getPublishedFields(), the same accessor the select clause and the decoder use, so the schema
            // cannot promise a field the server was never asked for. They differ only for a REFRESH tag.
            final EventFieldSet publishedFields = tag.getDefinition().getPublishedFields();
            log.debug(
                    "Schema for {} tag='{}' derived from declared type '{}'",
                    tagKind,
                    tagName,
                    publishedFields.browseName());
            // Northbound is the same for all three: type names the published shape, whether the tag observes
            // one condition, queries a notifier, or carries the refresh bracket. Southbound is where they
            // differ -- a transition command, a refresh request, or nothing writable at all.
            final Schema writeSchema =
                    switch (tagKind) {
                        case CONDITION -> ConditionSchemas.writeSchema();
                        case REFRESH -> ConditionSchemas.refreshCommandSchema();
                        case EVENT_SUBSCRIPTION, VALUE -> ConditionSchemas.unwritableSchema();
                    };
            output.finish(new TagSchemaCreationOutput.DataPointSchema(
                    ConditionSchemas.readSchema(publishedFields), null, null, writeSchema));
            return;
        }

        final OpcUaClientConnection conn = opcUaClientConnection.get();
        if (conn == null) {
            output.fail("Discovery failed: ClientConnection not connected or not initialized");
            return;
        }
        conn.client()
                .ifPresentOrElse(
                        client -> {
                            final var generator = new JsonSchemaGenerator(client);
                            @SuppressWarnings("unused")
                            final var unused = generator.collectTypeInfo(tag).whenComplete((fieldInfo, throwable) -> {
                                if (throwable == null) {
                                    log.debug("Schema inferred for tag='{}'", tagName);
                                    output.finish(JsonSchemaGenerator.buildSchema(fieldInfo));
                                } else {
                                    log.error("Exception while creating tag schema for '{}'", tagName, throwable);
                                    output.fail(throwable, null);
                                }
                            });
                        },
                        () -> {
                            log.error("Discovery failed: Client not connected or not initialized");
                            output.fail("Discovery failed: Client not connected or not initialized");
                        });
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    @Override
    public @NotNull Class<? extends WritingPayload> getMqttPayloadClass() {
        return OpcUaPayload.class;
    }

    @VisibleForTesting
    public @NotNull ProtocolAdapterState getProtocolAdapterState() {
        return protocolAdapterState;
    }

    @VisibleForTesting
    int consecutiveRetryAttempts() {
        return consecutiveRetryAttempts.get();
    }

    /** Claims the empty connection slot as one operation serialized with generation-owned status writes. */
    @VisibleForTesting
    boolean claimConnection(final @NotNull OpcUaClientConnection connection) {
        connectionOwnershipLock.lock();
        try {
            return opcUaClientConnection.compareAndSet(null, connection);
        } finally {
            connectionOwnershipLock.unlock();
        }
    }

    /** Releases whichever generation is current without letting one of its status writes pass concurrently. */
    @VisibleForTesting
    @Nullable
    OpcUaClientConnection releaseCurrentConnection() {
        connectionOwnershipLock.lock();
        try {
            return opcUaClientConnection.getAndSet(null);
        } finally {
            connectionOwnershipLock.unlock();
        }
    }

    /** Releases the current generation and publishes an adapter-lifecycle status under the same lock. */
    private @Nullable OpcUaClientConnection releaseCurrentConnectionAndPublish(
            final ProtocolAdapterState.@NotNull ConnectionStatus status) {
        connectionOwnershipLock.lock();
        try {
            final OpcUaClientConnection connection = opcUaClientConnection.getAndSet(null);
            protocolAdapterState.setConnectionStatus(status);
            return connection;
        } finally {
            connectionOwnershipLock.unlock();
        }
    }

    /**
     * Publishes a status only if its source is still current, with the check and write indivisible relative to
     * every connection-generation change.
     */
    @VisibleForTesting
    void publishStatusFrom(
            final @NotNull OpcUaClientConnection source, final ProtocolAdapterState.@NotNull ConnectionStatus status) {
        connectionOwnershipLock.lock();
        try {
            if (opcUaClientConnection.get() == source) {
                protocolAdapterState.setConnectionStatus(status);
            } else {
                log.debug(
                        "OPC UA adapter '{}': not reporting {} from a connection the adapter has already replaced",
                        adapterId,
                        status);
            }
        } finally {
            connectionOwnershipLock.unlock();
        }
    }

    /** Resets attempt-local readiness and publishes CONNECTING as one generation-owned transition. */
    private void publishConnectingFrom(final @NotNull OpcUaClientConnection source) {
        connectionOwnershipLock.lock();
        try {
            if (opcUaClientConnection.get() == source && !stopped) {
                browseReadiness.set(ReadinessStatus.WARMING_UP);
                protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTING);
            } else {
                log.debug(
                        "OPC UA adapter '{}': not publishing CONNECTING from a connection the adapter no longer owns",
                        adapterId);
            }
        } finally {
            connectionOwnershipLock.unlock();
        }
    }

    /**
     * Installs OPC UA readiness and public CONNECTED as one generation-owned transition.
     * <p>
     * The two are published together but are not the same fact, and the connection is asked which it earned.
     * A connection whose data-type tree could not be built is still fully connected — its tags are verified
     * and subscribed, and events and values flow — but a browse served from an unhydrated address space is
     * non-deterministic, so browse stays not-ready and the endpoint keeps answering 503 until something
     * rebuilds the tree. Reporting browse-ready on the strength of an attempt that did not manage it would be
     * the same class of untruth as the optimistic {@code CONNECTED} this readiness model exists to remove.
     */
    @VisibleForTesting
    void publishReadyFrom(final @NotNull OpcUaClientConnection source) {
        connectionOwnershipLock.lock();
        try {
            if (opcUaClientConnection.get() == source && !stopped) {
                final boolean browseReady = source.hasBrowseMetadata();
                browseReadiness.set(browseReady ? ReadinessStatus.BROWSE_READY : ReadinessStatus.WARMING_UP);
                protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
                if (browseReady) {
                    log.info(
                            "OPC UA adapter '{}' is connected and browse-ready "
                                    + "(namespace table and data-type tree hydrated)",
                            adapterId);
                } else {
                    log.info(
                            "OPC UA adapter '{}' is connected; browse is not ready because the data-type tree "
                                    + "could not be built during this connect. Tags, events and values are "
                                    + "unaffected, and later health checks retry the build with backoff. See "
                                    + "the preceding warning for the cause.",
                            adapterId);
                }
            } else {
                log.debug(
                        "OPC UA adapter '{}': not publishing readiness from a connection the adapter no longer owns",
                        adapterId);
            }
        } finally {
            connectionOwnershipLock.unlock();
        }
    }

    /**
     * Removes and marks a failed attempt only while it is still current. A failed ownership transition has no
     * status or retry side effects.
     */
    private boolean markCurrentConnectionFailed(final @NotNull OpcUaClientConnection connection) {
        connectionOwnershipLock.lock();
        try {
            if (stopped || !opcUaClientConnection.compareAndSet(connection, null)) {
                return false;
            }
            lastReconnectTimestamp.set(System.currentTimeMillis());
            protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.ERROR);
            return true;
        } finally {
            connectionOwnershipLock.unlock();
        }
    }

    /** Completes the current successful attempt without allowing its generation to change halfway through. */
    private boolean completeCurrentConnection(final @NotNull OpcUaClientConnection connection) {
        connectionOwnershipLock.lock();
        try {
            if (stopped || opcUaClientConnection.get() != connection) {
                return false;
            }
            lastReconnectTimestamp.set(System.currentTimeMillis());
            reconnectAttempts.set(0);
            connection.client().ifPresent(browseClient::set);
            cancelRetry();
            scheduleHealthCheck();
            return true;
        } finally {
            connectionOwnershipLock.unlock();
        }
    }

    /**
     * Finishes a failed attempt after ownership has been atomically revoked. Visible so the failed-CAS
     * regression can prove a superseded attempt performs neither the ERROR write nor retry scheduling.
     */
    @VisibleForTesting
    boolean completeFailedAttempt(
            final @NotNull OpcUaClientConnection connection,
            final @NotNull ProtocolAdapterStartInput input,
            final @Nullable Throwable throwable) {
        if (!markCurrentConnectionFailed(connection)) {
            log.debug(
                    "Ignoring failed connection attempt for OPC UA adapter '{}' because its generation is no longer current",
                    adapterId);
            return false;
        }

        if (throwable != null) {
            log.warn(
                    "OPC UA adapter '{}' connection failed, scheduling retry with exponential backoff",
                    adapterId,
                    throwable);
        } else {
            log.warn(
                    "OPC UA adapter '{}' connection returned false, scheduling retry with exponential backoff",
                    adapterId);
        }

        cancelHealthCheck();
        scheduleRetry(input);
        return true;
    }

    /**
     * Attempts to establish connection to OPC UA server.
     * On failure, schedules automatic retry after configured retry interval.
     */
    private void attemptConnection(
            final @NotNull OpcUaClientConnection conn,
            final @NotNull ParsedConfig parsedConfig,
            final @NotNull ProtocolAdapterStartInput input) {

        @SuppressWarnings("unused")
        final var unused = CompletableFuture.supplyAsync(() -> conn.start(parsedConfig))
                .whenComplete((success, throwable) -> {
                    // Two questions, not one. `stopped` asks whether the adapter still wants background work
                    // at all; the identity check asks whether *this* attempt is still the one that speaks for
                    // it. An attempt includes a bounded metadata build plus up to three round trips per
                    // condition tag,
                    // and a reconnect or a retry can install a newer connection while it runs, at which point
                    // everything below is about the wrong object: it would overwrite the live browse client
                    // with a superseded one and schedule health checks on the new lifecycle's executor.
                    //
                    // Ignoring the result is not enough on its own. A superseded attempt that *succeeded*
                    // holds a live session and subscription that nothing else can reach, so dropping the
                    // result on the floor leaves it publishing for the lifetime of the process. It has to be
                    // destroyed here, because this is the last reference to it. Done inline rather than
                    // posted: this callback already runs off the caller's thread, and doing it here means the
                    // future does not complete until the orphan is actually closed.
                    if (Boolean.TRUE.equals(success) && throwable == null) {
                        if (completeCurrentConnection(conn)) {
                            // The connection has already hydrated browse metadata and installed its context
                            // before publishing CONNECTED, so every public operation is usable at this point.
                            log.info("OPC UA adapter '{}' connected successfully", adapterId);
                        } else {
                            log.debug(
                                    "Connection attempt for adapter '{}' completed after it was superseded or "
                                            + "stopped, discarding the successful result",
                                    adapterId);
                            // This completion owns the last reference to the live session and subscription.
                            conn.destroy();
                        }
                    } else {
                        completeFailedAttempt(conn, input, throwable);
                    }
                });
    }

    /**
     * EDG-577: one-shot warm-up during a successful connection, so that a browse served immediately after
     * {@code CONNECTED} is deterministic rather than racing Milo's lazy hydration.
     * <p>
     * The connection runs this before tag verification, subscription creation, automatic refresh, context
     * installation and {@code CONNECTED}. A failure does not fail the attempt — see {@code
     * OpcUaClientConnection#prepareClientForUse} for why not — it leaves the adapter connected and not
     * browse-ready.
     * <p>
     * Only the second call does any work. {@code getNamespaceTable()} is a field read in Milo 1.1.6: the
     * table is populated by the client during session activation, so there is nothing here to force and
     * nothing that can fail. It is kept because it is the pairing that makes the intent legible, and because
     * a future Milo could make it lazy like the tree.
     */
    private void warmUpBrowseMetadata(final @NotNull OpcUaClient client) {
        try {
            client.getNamespaceTable();
            // The recursive browse of the DataType hierarchy, and the whole cost and failure surface of this
            // method. Lazy in Milo, and its slot is cleared on failure rather than poisoned, so whoever needs
            // it next -- a browse, a southbound write, a schema request -- rebuilds it.
            client.getDataTypeTree(); // feeds the EDG-488 cache when that lands
        } catch (final @NotNull Exception e) {
            throw new IllegalStateException("OPC UA browse metadata warm-up failed", e);
        }
    }

    /**
     * Schedules a retry attempt using exponential backoff strategy.
     * First retry is after 1 second, subsequent retries use exponential backoff (base 2) up to 5 minutes max.
     */
    private void scheduleRetry(final @NotNull ProtocolAdapterStartInput input) {

        // Check if adapter has been stopped
        if (stopped) {
            log.debug("Skipping retry scheduling for adapter '{}' - adapter has been stopped", adapterId);
            return;
        }

        // Increment retry attempt counter and calculate backoff delay
        final int attemptCount = consecutiveRetryAttempts.incrementAndGet();
        long backoffDelayMs;
        try {
            backoffDelayMs =
                    calculateBackoffDelayMs(config.getConnectionOptions().retryIntervalMs(), attemptCount);
        } catch (final Exception e) {
            log.warn(
                    "Failed to calculate backoff delay for adapter '{}' from retryIntervalMs {}",
                    adapterId,
                    config.getConnectionOptions().retryIntervalMs(),
                    e);
            backoffDelayMs = calculateBackoffDelayMs(ConnectionOptions.DEFAULT_RETRY_INTERVALS, attemptCount);
        }

        log.info(
                "Scheduling retry attempt #{} for OPC UA adapter '{}' with backoff delay of {} ms",
                attemptCount,
                adapterId,
                backoffDelayMs);

        // Capture scheduler reference to avoid races with shutdown setting it to null
        final ScheduledExecutorService scheduler = retryScheduler;
        if (scheduler == null) {
            log.debug("Skipping retry scheduling for adapter '{}' - retry scheduler is not available", adapterId);
            return;
        }

        final ScheduledFuture<?> future;
        try {
            future = scheduler.schedule(
                    () -> {
                        // Check if adapter was stopped before retry executes
                        if (stopped || this.parsedConfig == null || this.moduleServices == null) {
                            log.debug("OPC UA adapter '{}' retry cancelled - adapter was stopped", adapterId);
                            return;
                        }

                        log.info("Executing retry attempt #{} for OPC UA adapter '{}'", attemptCount, adapterId);

                        // Create new connection object for retry
                        final OpcUaClientConnection newConn = new OpcUaClientConnection(
                                adapterId,
                                tagList,
                                protocolAdapterState,
                                this.moduleServices.protocolAdapterTagStreamingService(),
                                this.moduleServices.eventService(),
                                protocolAdapterMetricsService,
                                config,
                                opcUaServiceFaultListener,
                                this::publishStatusFrom,
                                this::warmUpBrowseMetadata,
                                this::publishReadyFrom);

                        // Set as current connection and attempt
                        if (claimConnection(newConn)) {
                            publishConnectingFrom(newConn);
                            attemptConnection(newConn, this.parsedConfig, input);
                        } else {
                            log.debug("OPC UA adapter '{}' retry skipped - connection already exists", adapterId);
                        }
                    },
                    backoffDelayMs,
                    TimeUnit.MILLISECONDS);
        } catch (final RejectedExecutionException e) {
            if (scheduler.isShutdown()) {
                log.debug("OPC UA adapter '{}' retry scheduling rejected, executor is shutting down.", adapterId);
                return;
            } else {
                throw e;
            }
        }

        // Store future so it can be cancelled if needed
        final ScheduledFuture<?> oldFuture = retryFuture.getAndSet(future);
        if (oldFuture != null && !oldFuture.isDone()) {
            oldFuture.cancel(false);
        }
    }

    /**
     * Cancels any pending retry attempts.
     */
    private void cancelRetry() {
        final ScheduledFuture<?> future = retryFuture.getAndSet(null);
        if (future != null && !future.isDone()) {
            future.cancel(false);
            log.debug("Cancelled pending retry for OPC UA adapter '{}'", adapterId);
        }
    }
}
