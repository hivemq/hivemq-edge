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

import com.hivemq.adapter.sdk.api.ProtocolAdapterConnectionDirection;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.discovery.NodeTree;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryInput;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryOutput;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
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
import com.hivemq.edge.adapters.opcua.browse.Browsing;
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
import com.hivemq.edge.adapters.opcua.method.MethodArguments;
import com.hivemq.edge.adapters.opcua.method.MethodCache;
import com.hivemq.edge.adapters.opcua.method.MethodCallResult;
import com.hivemq.edge.adapters.opcua.method.MethodReference;
import com.hivemq.edge.adapters.opcua.method.MethodResponse;
import com.hivemq.edge.adapters.opcua.method.MethodSchemas;
import com.hivemq.edge.adapters.opcua.method.MethodSignature;
import com.hivemq.edge.adapters.opcua.method.MethodVerifier;
import com.hivemq.edge.adapters.opcua.northbound.Decoding;
import com.hivemq.edge.adapters.opcua.southbound.JsonSchemaGenerator;
import com.hivemq.edge.adapters.opcua.southbound.JsonToOpcUaValueConverter;
import com.hivemq.edge.adapters.opcua.southbound.OpcUaPayload;
import com.hivemq.edge.adapters.opcua.southbound.VariableWrites;
import com.hivemq.edge.adapters.opcua.util.NodeIdText;
import com.hivemq.util.Topics;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
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

    /**
     * A method tag's methods, read once per tag and kept for the session. Cleared whenever a session starts,
     * because a new session is what can renumber the NodeIds it holds.
     * <p>
     * Filled by {@link #prewarmMethodTags} once the adapter is connected, and by the request path for anything
     * that pre-warm did not reach.
     */
    private final @NotNull MethodCache methodCache = new MethodCache();

    /**
     * The single thread that pre-warms method tags, and the handle on the run in progress.
     * <p>
     * Its own thread rather than the retry or health-check scheduler, because a pre-warm may run for minutes on
     * a device that serves metadata slowly, and both of those have work that must not wait that long — the
     * health check in particular is what notices a dead connection.
     */
    private volatile @Nullable ExecutorService methodPrewarmExecutor = null;

    private final @NotNull AtomicReference<Future<?>> methodPrewarmRun = new AtomicReference<>();

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

    // region Lifecycle lines (EDG-1130)

    /**
     * The prefix of every lifecycle line, so that one grep finds exactly these lines and nothing else.
     * <p>
     * A lifecycle line is written as a log line and fired as an event with the same text, and it has four parts:
     * the prefix, the identity, what just happened, and the state now. The state is the condition of value
     * reads, value writes and method requests, worked out from what the adapter knows at that moment, so the
     * last lifecycle line of an adapter always says what works now.
     */
    static final @NotNull String LIFECYCLE_PREFIX = "Lifecycle '";

    private static final @NotNull DateTimeFormatter LIFECYCLE_START_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    /**
     * Held from construction rather than read through {@link #moduleServices}, which {@link #stop} clears
     * before the last lifecycle line is written.
     */
    private final @NotNull EventService eventService;

    /**
     * When this adapter object began its start. Edge builds a new adapter object on every restart, so this
     * identifies the restart; a counter held here would start from zero again on exactly that event.
     */
    private volatile @Nullable String lifecycleStart;

    /** Client connections claimed within this start. */
    private final @NotNull AtomicInteger lifecycleConnection = new AtomicInteger();

    /** Sessions within the current connection: the first, then one more for every session Milo resumes. */
    private final @NotNull AtomicInteger lifecycleSession = new AtomicInteger();

    /**
     * Whether the current connection has published CONNECTED once. A session Milo activates before that is
     * part of the initial connect, not a resumed session.
     */
    private volatile boolean lifecycleConnectionReady;

    /** Whether the adapter holds a live connection and session, the condition for value reads and writes. */
    private volatile boolean lifecycleConnected;

    /** Whether the method read has finished for the current connection or session. */
    private volatile boolean lifecycleMethodsRead;

    /**
     * Whether a health check reported the connection lost while automatic reconnection is switched off, so that
     * a later passing health check reports the recovery.
     */
    private volatile boolean lifecycleLostWithoutReconnect;

    // endregion

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
        this.eventService = input.moduleServices().eventService();
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
        beginLifecycle();

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
            reportInvalidConfiguration(message);
            output.failStart(new IllegalStateException(message), message);
            return;
        }

        final ParsedConfig newlyParsedConfig;
        final var result = ParsedConfig.fromConfig(config);
        if (result instanceof Failure<ParsedConfig, String>(final String failure)) {
            log.error("Failed to parse configuration for OPC UA client: {}", failure);
            reportInvalidConfiguration(failure);
            output.failStart(new IllegalStateException(failure), "Failed to parse configuration for OPC UA client");
            return;
        } else if (result instanceof Success<ParsedConfig, String>(final ParsedConfig successfullyParsedConfig)) {
            newlyParsedConfig = successfullyParsedConfig;
            // Store for reconnection
            this.parsedConfig = successfullyParsedConfig;
            this.moduleServices = input.moduleServices();
            reportLifecycle(Event.SEVERITY.INFO, false, "starting");
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
                this::publishReadyFrom,
                this::onSessionReplaced);
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

        // Set stopped flag to prevent new scheduling. Together with the lifecycle line and under the ownership lock,
        // so a connect finishing concurrently either writes its line before this one or sees the adapter stopped.
        connectionOwnershipLock.lock();
        try {
            stopped = true;
            lifecycleConnected = false;
            reportLifecycle(Event.SEVERITY.INFO, false, "stopping");
        } finally {
            connectionOwnershipLock.unlock();
        }
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

            // Stop and clean up current connection. Released together with the lifecycle line, so a callback of the
            // old connection either writes its line before this one or no longer owns the adapter.
            final OpcUaClientConnection oldConn;
            connectionOwnershipLock.lock();
            try {
                oldConn = opcUaClientConnection.getAndSet(null);
                lifecycleConnected = false;
                reportLifecycle(Event.SEVERITY.WARN, true, "PROBLEM: connection lost. Replacing it by itself");
            } finally {
                connectionOwnershipLock.unlock();
            }
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
                    this::publishReadyFrom,
                    this::onSessionReplaced);

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
                        this::runHealthCheck, healthCheckIntervalMs, healthCheckIntervalMs, TimeUnit.MILLISECONDS);

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

    /** One health check: reconnect or report a lost connection, and reopen browse on a healthy one. */
    @VisibleForTesting
    void runHealthCheck() {
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
                log.warn("Health check failed for adapter '{}' - triggering automatic reconnection", adapterId);
                reconnect();
            } else {
                log.warn("Health check failed for adapter '{}' - automatic reconnection is disabled", adapterId);
                publishStatusFrom(conn, ProtocolAdapterState.ConnectionStatus.ERROR);
                // Once, on the transition: this check repeats every interval while the connection stays unhealthy.
                if (lifecycleConnected) {
                    lifecycleConnected = false;
                    lifecycleLostWithoutReconnect = true;
                    reportLifecycle(
                            Event.SEVERITY.WARN,
                            true,
                            "PROBLEM: connection lost. Automatic reconnection is switched off, so a restart is needed");
                }
            }
        } else {
            log.debug("Health check passed for adapter '{}'", adapterId);
            // The line above said a restart is needed. A connection that came back by itself must not leave that as
            // the last line. Only the line changes: the adapter status stays ERROR, as it always has, until the
            // adapter is restarted.
            if (lifecycleLostWithoutReconnect) {
                lifecycleLostWithoutReconnect = false;
                lifecycleConnected = true;
                reportLifecycle(
                        Event.SEVERITY.INFO,
                        true,
                        "connection recovered by itself. The adapter status still shows ERROR until the adapter "
                                + "is restarted");
            }
            // A healthy connection that never got its browse metadata is the one case that cannot fix itself: the
            // browse endpoint is refused before it runs, so the operator's natural retry never reaches the code that
            // would rebuild the tree. This is the periodic pass that does reach it.
            rehydrateBrowseMetadataIfMissing(conn);
        }
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
                    reportLifecycle(Event.SEVERITY.INFO, true, "browse is ready");
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

        // The method pre-warm. Interrupted but not waited for: a run may be blocked in a browse against a
        // device that is answering slowly, which is the whole reason it has its own thread, and nothing depends
        // on it finishing. shutdownNow interrupts it, and the run checks that flag before each tag and abandons
        // the rest. Waiting here would make a slow device able to delay every stop by the wait.
        final var prewarmExecutor = this.methodPrewarmExecutor;
        this.methodPrewarmExecutor = null;
        if (prewarmExecutor != null && !prewarmExecutor.isShutdown()) {
            prewarmExecutor.shutdownNow();
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
        // One thread, which is what keeps the pre-warm sequential: a run submitted while another is still
        // going waits rather than adding concurrent browses to a device that is already answering slowly.
        methodPrewarmExecutor = Executors.newSingleThreadExecutor();
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
    public @NotNull String nodeIdMatchKey(final @NotNull String nodeId) {
        final OpcUaClient client = clientForNodeIdResolution();
        return client == null ? nodeId : NodeIdText.matchKey(nodeId, client.getNamespaceTable());
    }

    /** The primary connection's client, else the browse client snapshot, else null. */
    private @Nullable OpcUaClient clientForNodeIdResolution() {
        final OpcUaClientConnection conn = opcUaClientConnection.get();
        if (conn != null) {
            final OpcUaClient client = conn.client().orElse(null);
            if (client != null) {
                return client;
            }
        }
        return browseClient.get();
    }

    @Override
    public @NotNull String resolveNodeId(final @NotNull String nodeId, final @Nullable String namespaceUri) {
        if (namespaceUri == null || namespaceUri.isEmpty()) {
            return nodeId;
        }
        final OpcUaClient client = clientForNodeIdResolution();
        if (client == null) {
            return nodeId;
        }
        try {
            // A node id that names its namespace by URI is already stable: it is kept as written, and the row's
            // namespace column does not override it.
            if (NodeIdText.expanded(nodeId).getNamespaceUri() != null) {
                return nodeId;
            }
            // The file's index is from the exporting session; the row's URI says which namespace it meant.
            return NodeIdText.resolve(nodeId, client.getNamespaceTable())
                    .reindex(client.getNamespaceTable(), namespaceUri)
                    .toParseableString();
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

        // Half of one misconfiguration: the tag's kind and the mapping's method-call setting disagree. The
        // payload carrying Edge's request parameters *is* that setting, as this adapter sees it.
        //
        // **Nothing but this adapter sees both facts.** The tag's kind is this protocol's own vocabulary and
        // the setting is Edge's, so Edge could only check the pair by being told what a METHOD tag is -- a
        // protocol concept the generic side must not learn. Three attempts to hand it one were made during this
        // feature and all three were reverted.
        //
        // The two halves are answered differently, and the asymmetry follows from what each tag is rather than
        // from convenience. **This half is a value tag that answers nobody**, by design and for every adapter,
        // so a log line and a failed write are the whole of what is available; inventing a reply channel for it
        // would be the same leak in the other direction. The other half -- a method tag with no parameters --
        // has a return path by construction and is answered at the METHOD dispatch below, where the client's
        // encoding context exists.
        //
        // Reported by QA as P19 on EDG-1116.
        final boolean isMethodTag = opcuaTag.getDefinition().getKind() == OpcuaTagKind.METHOD;
        final boolean carriesParameters = opcUAWritePayload.parameters() != null;

        if (!isMethodTag && carriesParameters) {
            // The mirror: a mapping carries method calls while its tag is not a method tag. The value cannot be
            // written -- it holds an instruction object the device never declared -- and writing it while
            // ignoring that object would be worse than refusing.
            log.error(
                    "A write to tag '{}' of kind {} carries method call parameters, which only a METHOD tag can "
                            + "accept. Either the tag's kind or the southbound mapping's 'this mapping carries "
                            + "method calls' setting is wrong; the message is dropped.",
                    tagName,
                    opcuaTag.getDefinition().getKind());
            output.fail("Tag '"
                    + tagName
                    + "' is a "
                    + opcuaTag.getDefinition().getKind()
                    + " tag, but the request carries method call parameters. Either make it a METHOD tag or "
                    + "switch 'this mapping carries method calls' off for the mapping that feeds it");
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

                            final NodeId nodeId = parseNodeOrFail(client, opcuaTag, tagName, output);
                            if (nodeId == null) {
                                return;
                            }

                            // A condition is moved by calling a method on it, not by assigning to it: the
                            // server owns the state machine. Everything else is an ordinary value write.
                            if (opcuaTag.getDefinition().getKind() == OpcuaTagKind.CONDITION) {
                                writeConditionUpdate(client, nodeId, opcUAWritePayload, tagName, output);
                                return;
                            }

                            // A method tag names an object, so there is no Value attribute to assign to: the
                            // request names a method and this calls it.
                            if (opcuaTag.getDefinition().getKind() == OpcuaTagKind.METHOD) {
                                // The other half of the pair checked at the top of `write`, answered here
                                // because a response needs the client's encoding context and this is the first
                                // point that has one. Still before the object is browsed and before the device
                                // is touched.
                                //
                                // Previously this fell through into the method path, which found no method name
                                // and refused with an empty one -- naming no method, carrying no token, and
                                // saying nothing about the setting to change. The caller could not match the
                                // answer to its request and the operator was not told what to switch on.
                                if (!carriesParameters) {
                                    log.warn(
                                            "A request to method tag '{}' carries no method parameters, so the "
                                                    + "southbound mapping that feeds it does not have method calls "
                                                    + "enabled. Switch 'this mapping carries method calls' on for "
                                                    + "that mapping.",
                                            tagName);
                                    refuseWithResponse(
                                            client,
                                            opcuaTag,
                                            "",
                                            opcUAWritePayload,
                                            StatusCodes.Bad_MethodInvalid,
                                            "the southbound mapping for tag '"
                                                    + tagName
                                                    + "' does not have method calls enabled, so the request named "
                                                    + "no method and none can be called. Switch 'this mapping "
                                                    + "carries method calls' on for that mapping",
                                            output);
                                    return;
                                }
                                callMethod(client, nodeId, opcUAWritePayload, opcuaTag, output);
                                return;
                            }

                            // Converted by the converter every southbound route uses, so a variable accepts
                            // exactly what a method argument of the same DataType does. A refusal fails the write
                            // here: thrown out of this consumer it would leave the WritingOutput uncompleted, and
                            // the caller would learn nothing until the write timed out.
                            final Variant value;
                            try {
                                value = VariableWrites.valueFor(client, nodeId, opcUAWritePayload.value());
                            } catch (final JsonToOpcUaValueConverter.ConversionException e) {
                                final String reason =
                                        switch (e.fault()) {
                                            case UNSUPPORTED ->
                                                "its type cannot be encoded: " + e.expected() + ", because "
                                                        + e.reason();
                                            case SHAPE -> "it " + e.reason();
                                            case VALUE ->
                                                "it expects " + e.expected() + ", and the value supplied is not one: "
                                                        + e.reason();
                                        };
                                log.warn("Refused write to tag '{}': {}", tagName, reason);
                                output.fail("Refused write to tag '" + tagName + "': " + reason);
                                return;
                            } catch (final UaException | RuntimeException e) {
                                log.error("Cannot write to tag '{}': its DataType could not be read", tagName, e);
                                output.fail("Cannot write to tag '" + tagName + "': its DataType could not be read: "
                                        + e.getMessage());
                                return;
                            }

                            @SuppressWarnings("unused")
                            final var unused = client.writeValuesAsync(
                                            List.of(nodeId), List.of(new DataValue(value, StatusCode.GOOD, null)))
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
     * Parsing throws an unchecked exception, and this runs inside the consumer of an
     * {@code ifPresentOrElse} — so a throw leaves the {@link WritingOutput} uncompleted rather than failed,
     * which is the difference between a write that reports an error and one that never answers.
     *
     * @return the parsed node, or {@code null} when the write has already been failed.
     */
    private @Nullable NodeId parseNodeOrFail(
            final @NotNull OpcUaClient client,
            final @NotNull OpcuaTag opcuaTag,
            final @NotNull String tagName,
            final @NotNull WritingOutput output) {

        final String node = opcuaTag.getDefinition().getNode();
        try {
            return NodeIdText.resolve(node, client.getNamespaceTable());
        } catch (final Exception e) {
            log.error("Cannot write to tag '{}': {}", tagName, e.getMessage(), e);
            output.fail("Cannot write to tag '" + tagName + "': " + e.getMessage());
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

    /**
     * Calls the method a request names, on the object a method tag names.
     * <p>
     * The object's methods are read once per tag and kept for the session (see {@link MethodCache}). Normally
     * {@link #prewarmMethodTags} has already read them, so this is a cache hit; a tag the pre-warm did not
     * reach -- because it failed, or because the run was abandoned when a new session began -- is read here
     * instead, and the failure is then reported to the request that provoked it.
     */
    private void callMethod(
            final @NotNull OpcUaClient client,
            final @NotNull NodeId objectNodeId,
            final @NotNull OpcUaPayload payload,
            final @NotNull OpcuaTag opcuaTag,
            final @NotNull WritingOutput output) {

        final String tagName = opcuaTag.getName();
        final String methodName = payload.methodName();
        if (methodName == null || methodName.isBlank()) {
            // Edge refuses a request it cannot name a method for, before the device is touched, so a request
            // reaching here without one means the parameter object never arrived. That is the version mismatch
            // the design accepts: method calls need a new Edge and a new bidirectional module together, and the
            // older of the two does not write the object this reads. A plain refusal naming the field is the
            // symptom, which is at least diagnosable.
            refuseWithResponse(
                    client,
                    opcuaTag,
                    "",
                    payload,
                    StatusCodes.Bad_MethodInvalid,
                    "A request to method tag '"
                            + tagName
                            + "' carries no method name. Method calls require the bidirectional module at the "
                            + "same version as Edge",
                    output);
            return;
        }

        // No check on the reply address here, deliberately. "Is this a publishable MQTT topic" is a question
        // about MQTT, and it is answered where the answer will be published: Edge validates the address while
        // assembling the request's parameters, before the message reaches the data policy, and quarantines a
        // request that names an unpublishable one. The adapter used to ask because it happened to be holding
        // the string, which put an MQTT question behind an OPC-UA status code.
        // Which session's node ids the answer below will be about, read before the lookup so the callback can
        // tell whether it still holds. See the generation check in the callback.
        final long generation = methodCache.generation();

        @SuppressWarnings("unused")
        final var unused = methodCache
                .get(tagName, () -> MethodVerifier.verify(client, objectNodeId, tagName))
                .whenComplete((result, throwable) -> {
                    // The session that answered this lookup has ended. Refused rather than dispatched, and
                    // never retried here.
                    //
                    // Clearing the cache on a session change fixes every *later* lookup and cannot reach this
                    // one: the future was handed out before the clear and completes as it always would, so
                    // without this check the call went to the device naming an object and a method that were
                    // identified in a session that no longer exists.
                    //
                    // What that costs, honestly: a stale node id almost always just fails, and the caller gets
                    // a status about a node rather than one about the session. Landing on a *different* node
                    // takes more than a stale id -- the index would have to be reassigned to another namespace
                    // that happens to hold the same identifier, OPC 10000-3 section 5.2.2 requiring the
                    // identifier itself to persist. Worth a check that costs an atomic read, not worth
                    // describing as the likely outcome: every OPC-UA tag in Edge is configured by a node id
                    // carrying an index and reused across every reconnect for the adapter's life, so indices
                    // moving would break far more than this.
                    //
                    // Not retried, even though the caller would prefer it: a Call this adapter has already
                    // sent may have taken effect, and a method is not idempotent. The caller gets a refusal
                    // they can act on, and their next request resolves against the live session.
                    if (methodCache.generation() != generation) {
                        log.info(
                                "OPC UA adapter '{}': refusing the call to '{}' on tag '{}' -- its methods were "
                                        + "read in a session that has since been replaced, and node ids do not "
                                        + "survive that. The next request reads them again.",
                                adapterId,
                                methodName,
                                tagName);
                        refuseWithResponse(
                                client,
                                opcuaTag,
                                methodName,
                                payload,
                                StatusCodes.Bad_SessionClosed,
                                "The session that read the methods of tag '" + tagName
                                        + "' has been replaced. The call was not sent; please repeat the request",
                                output);
                        return;
                    }
                    if (throwable != null) {
                        // Reading the object's methods failed outright. Bad_InternalError rather than a
                        // method-specific code: nothing was learned about the method, so nothing can be claimed
                        // about it.
                        log.error("Exception while reading the methods of tag '{}'", tagName, throwable);
                        refuseWithResponse(
                                client,
                                opcuaTag,
                                methodName,
                                payload,
                                StatusCodes.Bad_InternalError,
                                "Could not read the methods of tag '" + tagName + "': "
                                        + Objects.requireNonNullElse(throwable.getMessage(), "unknown error"),
                                output);
                        return;
                    }
                    // Guarded, because this runs on a callback whose exceptions reach nobody. A throw from here
                    // used to leave the request with no answer at all and hold its writer until the thirty-second
                    // timeout, queueing every later request for the tag behind it -- silence being the one
                    // outcome worse than a wrong status code. Each known cause is answered properly above and
                    // below; this is so an unforeseen one costs a refusal rather than a stall.
                    try {
                        switch (result) {
                            case MethodVerifier.Result.Verified verified ->
                                invoke(client, verified.methods(), methodName, payload, opcuaTag, generation, output);
                            // The tag names something that is not a callable object, so no method on it can
                            // exist.
                            case MethodVerifier.Result.Rejected rejected ->
                                refuseWithResponse(
                                        client,
                                        opcuaTag,
                                        methodName,
                                        payload,
                                        StatusCodes.Bad_NodeIdUnknown,
                                        rejected.reason(),
                                        output);
                            // The object may well be fine; Edge could not find out. Distinct from Rejected on
                            // purpose -- a caller retrying later may succeed, which is not true of a rejection.
                            case MethodVerifier.Result.Unverifiable unverifiable ->
                                refuseWithResponse(
                                        client,
                                        opcuaTag,
                                        methodName,
                                        payload,
                                        StatusCodes.Bad_InternalError,
                                        unverifiable.reason(),
                                        output);
                        }
                    } catch (final RuntimeException e) {
                        log.error("Unexpected failure answering a method request on tag '{}'", tagName, e);
                        refuseWithResponse(
                                client,
                                opcuaTag,
                                methodName,
                                payload,
                                StatusCodes.Bad_InternalError,
                                "Could not answer the request on tag '" + tagName + "': "
                                        + Objects.requireNonNullElse(e.getMessage(), "unknown error"),
                                output);
                    }
                });
    }

    /**
     * Sends the Call, once the method is known.
     * <p>
     * An unknown method name is answered here rather than by the server. The server's answer would be
     * {@code Bad_MethodInvalid} and nothing else; this one names what the object does offer, which is what an
     * operator who mistyped a method actually needs.
     *
     * @param generation which session the methods were read in, re-checked immediately before the Call is
     *                   submitted. See the check at that site.
     */
    private void invoke(
            final @NotNull OpcUaClient client,
            final @NotNull List<MethodSignature> methods,
            final @NotNull String methodName,
            final @NotNull OpcUaPayload payload,
            final @NotNull OpcuaTag opcuaTag,
            final long generation,
            final @NotNull WritingOutput output) {

        final String tagName = opcuaTag.getName();
        final MethodSignature signature;
        // `methodName` is either an unadorned browse name, which must be unique among the object's methods, or
        // a node id naming the method itself. Resolution is separate because a browse name is not unique on an
        // object, and picking by browse order called whichever method the server reported second.
        switch (MethodReference.resolve(methods, methodName, client.getNamespaceTable())) {
            case MethodReference.Resolution.Matched matched -> signature = matched.method();
            // The same status the server would have returned, with a message it could not have produced: naming
            // what the object does offer is what an operator who mistyped a method actually needs.
            case MethodReference.Resolution.NoSuchMethod noSuch -> {
                refuseWithResponse(
                        client,
                        opcuaTag,
                        methodName,
                        payload,
                        StatusCodes.Bad_MethodInvalid,
                        "Object of tag '" + tagName + "' " + noSuch.reason(),
                        output);
                return;
            }
            // Bad_MethodInvalid as well: the request did not identify a method, which is the same fault as
            // naming one that does not exist. The message carries the node ids that would identify them.
            case MethodReference.Resolution.Ambiguous ambiguous -> {
                refuseWithResponse(
                        client,
                        opcuaTag,
                        methodName,
                        payload,
                        StatusCodes.Bad_MethodInvalid,
                        "Object of tag '" + tagName + "' " + ambiguous.reason(),
                        output);
                return;
            }
            // The request is malformed rather than wrong about the device, which is why this one differs.
            case MethodReference.Resolution.Malformed malformed -> {
                refuseWithResponse(
                        client,
                        opcuaTag,
                        methodName,
                        payload,
                        StatusCodes.Bad_InvalidArgument,
                        "Request to tag '" + tagName + "': " + malformed.reason(),
                        output);
                return;
            }
        }

        final Variant[] arguments;
        try {
            arguments = MethodArguments.forCall(signature, payload.value(), JsonToOpcUaValueConverter.of(client));
        } catch (final MethodArguments.InvalidArgumentException e) {
            // The request is wrong about the method, which is a client error and not a device one. Sending it
            // anyway would get Bad_InvalidArgument back with no indication of which argument was at fault.
            //
            // The code comes from the exception rather than being fixed here: an omitted argument is
            // Bad_ArgumentsMissing and everything else is Bad_InvalidArgument, and only the throw site knows
            // which happened.
            refuseWithResponse(client, opcuaTag, methodName, payload, e.statusCode(), e.reason(), output);
            return;
        }

        // Last look before anything is sent, because the two steps above take time the session may not survive.
        // The caller checked the generation when the cached methods arrived; resolving the name and converting
        // the arguments then ran, and a session replaced during those steps would leave this call carrying node
        // ids -- a namespace index plus an identifier, in both the object id and the method id -- that were read
        // under a session that no longer exists.
        //
        // **This narrows the window rather than closing it.** A replacement between this check and the request
        // reaching the wire is still possible, and no check placed before an operation can exclude that; only a
        // submission path holding the session identity atomically could. What it removes is the part that was
        // demonstrable: the interval spanning resolution and argument conversion, which is where a reviewer
        // paused execution to produce a Call carrying old node ids.
        //
        // Not retried, for the same reason as the earlier check: a Call this adapter has already sent may have
        // taken effect, and a method is not idempotent. Refusing before submission is the one safe moment.
        //
        // Reported by Sam as V01 in review 05, as the interval his probe opened after the existing guard.
        if (methodCache.generation() != generation) {
            log.info(
                    "OPC UA adapter '{}': refusing the call to '{}' on tag '{}' -- the session that read its "
                            + "methods was replaced while the request was being prepared, and node ids do not "
                            + "survive that. The next request reads them again.",
                    adapterId,
                    methodName,
                    tagName);
            refuseWithResponse(
                    client,
                    opcuaTag,
                    methodName,
                    payload,
                    StatusCodes.Bad_SessionClosed,
                    "The session that read the methods of tag '" + tagName
                            + "' has been replaced. The call was not sent; please repeat the request",
                    output);
            return;
        }

        // The object id comes from the signature, not from the tag. A method found only on the object's type --
        // one carrying no modelling rule, so mirrored onto no instance -- is invoked by naming the type, which
        // OPC 10000-4's Call service permits and requires. For every method the object itself exposes this is
        // the tag's own node, so the common case is unchanged.
        final CallMethodRequest request = new CallMethodRequest(signature.objectId(), signature.nodeId(), arguments);

        @SuppressWarnings("unused")
        final var unused = client.callAsync(List.of(request)).whenComplete((response, throwable) -> {
            if (throwable != null) {
                // The call was sent and no answer came back -- a timeout, a closed session, a transport
                // failure. WARNING: this is NOT proof the method did not run, so the response says the call
                // failed without claiming the device did nothing. A caller retrying on it needs the method to
                // be idempotent.
                //
                // **The device's own status code, when the failure carries one.** This used to report
                // Bad_InternalError for every failure here, which section 7.1 of the reference defines as
                // "Edge learned nothing more" -- so a caller could not tell a device that never answered from
                // a fault in Edge, and the retry guidance in 7.3 became inapplicable precisely where it
                // matters. The code was not missing: it was in the failure and we discarded it.
                //
                // The fallback stays Bad_InternalError, for a failure that genuinely has no status -- a
                // client-side timeout before the request leaves, say. There the generic code is the honest one.
                final Long deviceStatus = Browsing.statusCodeOf(throwable);
                log.error("Exception while calling '{}' on tag '{}'", methodName, tagName, throwable);
                refuseWithResponse(
                        client,
                        opcuaTag,
                        methodName,
                        payload,
                        deviceStatus != null ? deviceStatus : StatusCodes.Bad_InternalError,
                        "Call to '"
                                + methodName
                                + "' on tag '"
                                + tagName
                                + "' did not complete: "
                                + Objects.requireNonNullElse(throwable.getMessage(), "unknown error"),
                        output);
                return;
            }
            final MethodCallResult result = MethodCallResult.of(response, signature);

            // The response goes out before the write completes. A caller waiting on the answer is waiting for
            // the message, not for the write, and publishing after completing the write would let a fast caller
            // see its request acknowledged while the answer is still in flight.
            publishMethodResponse(
                    client, opcuaTag, methodName, payload.correlationData(), publishableReplyTopic(payload), result);

            if (result.succeeded()) {
                log.debug("Called '{}' on tag '{}'", methodName, tagName);
                output.finish();
            } else {
                // Failed as a write too, not only as a response. The response carries the detail; failing the
                // write is what makes the failure visible in the adapter's own metrics and log rather than only
                // in a message the operator has to be subscribed to.
                log.error("Call to '{}' on tag '{}' failed: {}", methodName, tagName, result.statusName());
                output.fail("Call to '" + methodName + "' on tag '" + tagName + "' failed: " + result.statusName());
            }
        });
    }

    /**
     * Fails a request and publishes the failure as a response, for every outcome the device did not report itself.
     * <p>
     * The decisive property is that <strong>a caller sees the same shape whatever refused it</strong>. A failure
     * the device reports and one Edge spots before dispatch both arrive as a response with {@code value} null, an
     * {@code errorCode} naming the status, and the correlation token the request carried — so a caller matching
     * answers to requests needs to know nothing about where in Edge the refusal happened, and never waits for an
     * answer that is not coming. Without this, a pre-dispatch refusal was visible only in the adapter's log, which
     * a caller cannot read.
     * <p>
     * The write is failed as well, which is what puts the refusal in the adapter's own log and metrics rather than
     * only in a message an operator has to be subscribed to. {@code methodName} may be empty — a request that named
     * no method has none to echo — and the response then carries the field as an empty string rather than omitting
     * it, keeping every response the same shape.
     *
     * @param statusCode the OPC UA status this refusal reports, chosen to match what a server would have said for
     *                   the same fault. Edge answers these itself because its message can name what the object
     *                   actually offers, which the server's bare status code cannot.
     */
    private void refuseWithResponse(
            final @NotNull OpcUaClient client,
            final @NotNull OpcuaTag opcuaTag,
            final @NotNull String methodName,
            final @NotNull OpcUaPayload payload,
            final long statusCode,
            final @NotNull String reason,
            final @NotNull WritingOutput output) {

        log.error("Refused a method call on tag '{}': {}", opcuaTag.getName(), reason);

        final MethodCallResult refusal = new MethodCallResult(new StatusCode(statusCode), Map.of(), Map.of());
        publishMethodResponse(
                client, opcuaTag, methodName, payload.correlationData(), publishableReplyTopic(payload), refusal);

        output.fail(reason);
    }

    /**
     * The reply address a response may be sent to: the one the request named, or null when that cannot be published
     * to.
     * <p>
     * Null is how a response reaches the northbound mapping's own topic. The mapping holds the configured topic and
     * the adapter never sees it — a datapoint carrying no reply address falls back to it in
     * {@code NorthboundTagConsumer}, so omitting the field here <em>is</em> the instruction to use the default.
     * <p>
     * <b>A guard, not the validation.</b> An unpublishable address is rejected in Edge, where the request's
     * parameters are assembled before the data policy runs, and such a request is quarantined without the
     * device being touched. That is the right place for it: "is this a publishable MQTT topic" is a question
     * about MQTT, answered by the component that will publish, and answering it there means the caller learns of
     * the fault instead of waiting for an answer nobody could deliver.
     * <p>
     * This remains because the adapter must not depend on that for its own correctness. A payload reaching here
     * with an unpublishable address would mean the parameters were assembled by something other than Edge's
     * writer — so the address is dropped rather than passed on, and a missing address and a rejected one both
     * land on the mapping's own topic.
     * <p>
     * Package-private so the routing decision can be tested without a device and a broker: this is the part that
     * decides where an answer goes, and the rest of the path around it is driven by a live connection.
     */
    static @Nullable String publishableReplyTopic(final @NotNull OpcUaPayload payload) {
        final String requested = payload.responseTopic();
        if (requested == null || requested.isBlank() || !Topics.isValidTopicToPublish(requested)) {
            return null;
        }
        return requested;
    }

    /**
     * Publishes a call's outcome as a datapoint on the tag that received the request.
     * <p>
     * The tag is the routing key: a northbound mapping is configured per tag, so the response reaches the topic
     * that mapping names without anything here knowing what it is.
     * <p>
     * A failure to publish does not fail the call. The method already ran on the device — reporting the write as
     * failed because the answer could not be published would invite a retry of something that already happened,
     * and a method call is not generally safe to repeat.
     */
    private void publishMethodResponse(
            final @NotNull OpcUaClient client,
            final @NotNull OpcuaTag opcuaTag,
            final @NotNull String methodName,
            final @Nullable String correlationData,
            final @Nullable String responseTopic,
            final @NotNull MethodCallResult result) {

        final ModuleServices services = moduleServices;
        if (services == null) {
            // Stopped between the call being sent and its answer arriving. The device acted; there is nothing
            // left to publish through.
            log.warn(
                    "Adapter '{}' has no module services, so the answer from '{}' on tag '{}' is not published",
                    adapterId,
                    methodName,
                    opcuaTag.getName());
            return;
        }

        try {
            final var publisher = services.protocolAdapterTagStreamingService().dataPointsPublisher();
            final var builder = publisher.addDataPoint(opcuaTag);
            // The client's encoding context, so a method's output is converted by the same code as a variable's
            // value of the same type -- see MethodResponse#putVariant.
            // The encoding context is supplied as a supplier, not a value: fetching it can itself fail, and
            // evaluated here as an argument it took the whole answer down with it -- including a refusal,
            // which carries no output and so never needs a context at all. Same rule as the DataType
            // hierarchy: do not reach for a thing until a value's own type says it is needed.
            // A failure to fetch either reaches putVariant's own handler, which reports the output as undecodable
            // and publishes the answer -- or, for the hierarchy, the converter's fallback to the string form.
            final var undecodable = MethodResponse.write(
                    builder, methodName, correlationData, responseTopic, result, Decoding.of(client));
            builder.endDataPoint();
            publisher.publish();

            // An output published as a string rather than as the object its schema promises. Counted and
            // logged beside the answer that names it, because the cause is almost always a type description
            // this client has not fetched rather than anything about the method -- an operator seeing this
            // repeatedly is looking at a registry that is not being rebuilt, not at a broken method.
            if (!undecodable.isEmpty()) {
                protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_DATA_ERROR_COUNT);
                log.warn(
                        "Adapter '{}': the answer from '{}' on tag '{}' carries {} output(s) this client could not "
                                + "decode, published as null with the device's own body in "
                                + "statusInfo.undecodedOutputs: {}",
                        adapterId,
                        methodName,
                        opcuaTag.getName(),
                        undecodable.size(),
                        undecodable.keySet());
            }
        } catch (final Exception e) {
            log.error(
                    "Adapter '{}' could not publish the answer from '{}' on tag '{}'",
                    adapterId,
                    methodName,
                    opcuaTag.getName(),
                    e);
        }
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
        // METHOD joins VALUE on the device-derived path rather than this one. Its shape comes from the object's
        // methods and their declared Argument lists, which only a browse can supply -- the declared `type` says
        // nothing about it. Everything else here derives its shape from that declared type instead.
        if (tagKind != OpcuaTagKind.VALUE && tagKind != OpcuaTagKind.METHOD) {
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
                        // VALUE and METHOD never reach here -- both are answered from the device below.
                        case EVENT_SUBSCRIPTION, VALUE, METHOD -> ConditionSchemas.unwritableSchema();
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
                            // A method tag's shape comes from the object's methods, not from a value's type, so
                            // it browses rather than reading a DataType. Both schemas come from one browse: the
                            // request's arguments and the response's results are the same methods seen from
                            // opposite ends.
                            if (tagKind == OpcuaTagKind.METHOD) {
                                createMethodTagSchema(client, tag, tagName, output);
                                return;
                            }

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

    /**
     * Both schemas of a method tag, from one browse of its object.
     * <p>
     * The three verification outcomes answer differently on purpose. A rejection is a mistake in the tag and
     * says so. An unverifiable device is <em>not</em> answered with a schema describing no methods: that would
     * look exactly like an object that genuinely has none, and the operator would go looking at their
     * configuration for a problem that is in the connection.
     */
    private void createMethodTagSchema(
            final @NotNull OpcUaClient client,
            final @NotNull OpcuaTag tag,
            final @NotNull String tagName,
            final @NotNull TagSchemaCreationOutput output) {

        final NodeId objectNode;
        try {
            objectNode = NodeIdText.resolve(tag.getDefinition().getNode(), client.getNamespaceTable());
        } catch (final Exception e) {
            // tagNotFound rather than fail: the tag cannot be resolved against the device, which is the
            // operator's configuration being wrong rather than Edge or the device misbehaving. The runtime maps
            // fail() to 500, and a 500 tells an operator to look at Edge for a mistake that is in their tag.
            output.tagNotFound("Tag '" + tagName + "': " + e.getMessage());
            return;
        }

        @SuppressWarnings("unused")
        final var unused = methodCache
                .get(tagName, () -> MethodVerifier.verify(client, objectNode, tagName))
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Exception while reading the methods of tag '{}'", tagName, throwable);
                        output.fail(throwable, null);
                        return;
                    }
                    switch (result) {
                        case MethodVerifier.Result.Verified verified -> {
                            final var methods = verified.methods();
                            log.debug("Schema for METHOD tag='{}' derived from {} method(s)", tagName, methods.size());
                            // An object the device answered about and that exposes nothing callable. The schema
                            // that follows is empty and every request against the tag is then refused at
                            // validation, so without this the operator sees a configured tag, a connected
                            // adapter, and calls that vanish -- the count above is DEBUG, which nobody has on.
                            //
                            // WARN rather than ERROR: the tag and the device are both intelligible, and methods
                            // may be added to an object later without the tag changing.
                            if (methods.isEmpty()) {
                                log.warn(
                                        "Tag '{}' names object {}, which exposes no callable method{}. Its request "
                                                + "schema will admit no arguments and every call on this tag will be "
                                                + "refused. Enable DEBUG for {} to see what the browse returned.",
                                        tagName,
                                        objectNode,
                                        verified.complete()
                                                ? ""
                                                : " -- and part of its type hierarchy could not be read, so the "
                                                        + "device may have more",
                                        MethodVerifier.class.getPackageName());
                            }
                            // Each value schema declares its own parameter object -- the request's inside the
                            // southbound part, the response's inside the northbound one -- so the runtime lifts
                            // them to the document root by iterating, with no list of names of its own. The
                            // `context` slot stays empty: it is the protocol's namespace, not Edge's.
                            output.finish(new TagSchemaCreationOutput.DataPointSchema(
                                    MethodSchemas.responseSchema(methods),
                                    null,
                                    null,
                                    MethodSchemas.requestSchema(methods)));
                        }
                        // The tag is wrong about the device: the node is absent, or is not an object. The
                        // operator's configuration is at fault, so this is reported as a tag that cannot be
                        // resolved (4xx) rather than as a failure (500). The distinction is the whole point of
                        // Rejected existing beside Unverifiable -- a 500 sends an operator to look at Edge, or
                        // at us, for a mistake that is in their own tag definition.
                        case MethodVerifier.Result.Rejected rejected -> {
                            log.error("Cannot describe method tag '{}': {}", tagName, rejected.reason());
                            output.tagNotFound(rejected.reason());
                        }
                        // Left as a failure, deliberately. The tag may be perfectly correct and Edge could not
                        // find out -- a timeout, a refusal, a server that went away. Reporting that as a
                        // configuration error would send the operator to fix something that is not broken.
                        case MethodVerifier.Result.Unverifiable unverifiable -> {
                            log.error("Cannot describe method tag '{}': {}", tagName, unverifiable.reason());
                            output.fail(unverifiable.reason());
                        }
                    }
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

    // region Lifecycle lines (EDG-1130)

    /** Begins the lifecycle of one start: a new start time, and nothing connected or read yet. */
    @VisibleForTesting
    void beginLifecycle() {
        lifecycleStart = LIFECYCLE_START_FORMAT.format(Instant.now());
        lifecycleConnection.set(0);
        lifecycleSession.set(0);
        lifecycleConnectionReady = false;
        lifecycleConnected = false;
        lifecycleMethodsRead = false;
        lifecycleLostWithoutReconnect = false;
    }

    private void reportInvalidConfiguration(final @NotNull String reason) {
        reportLifecycle(
                Event.SEVERITY.WARN,
                false,
                "PROBLEM: the configuration is invalid: " + reason
                        + ". Fix the configuration, a restart alone does not help");
    }

    /**
     * Writes one lifecycle line as a log line and fires it as an event with the same text. Nothing is written
     * before the first start, because there is no lifecycle to identify.
     */
    private void reportLifecycle(
            final @NotNull Event.SEVERITY severity, final boolean withConnection, final @NotNull String happened) {
        final String start = lifecycleStart;
        if (start == null) {
            return;
        }
        final StringBuilder identity = new StringBuilder("start ").append(start);
        final int connection = lifecycleConnection.get();
        if (withConnection && connection > 0) {
            identity.append(", connection ").append(connection);
            final int session = lifecycleSession.get();
            if (session > 1) {
                identity.append(", session ").append(session);
            }
        }
        final String line =
                LIFECYCLE_PREFIX + adapterId + "'; " + identity + "; " + happened + "; state now - " + lifecycleState();
        if (severity == Event.SEVERITY.INFO) {
            log.info(line);
        } else {
            log.warn(line);
        }
        eventService
                .createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                .withSeverity(severity)
                .withMessage(line)
                .fire();
    }

    /** The condition of value reads, value writes and method requests, from what the adapter knows now. */
    private @NotNull String lifecycleState() {
        final boolean connected = lifecycleConnected && !stopped;
        final String values = connected ? "working" : "not working";
        final boolean hasMethodTags =
                tagList.stream().anyMatch(tag -> tag.getDefinition().getKind() == OpcuaTagKind.METHOD);
        final String methods;
        if (!hasMethodTags) {
            methods = "none";
        } else if (!connected) {
            methods = "not working";
        } else if (lifecycleMethodsRead) {
            methods = "working";
        } else {
            methods = "starting (working with delays)";
        }
        return "value reads: " + values + ", value writes: " + values + ", method requests: " + methods;
    }

    // endregion

    /** Claims the empty connection slot as one operation serialized with generation-owned status writes. */
    @VisibleForTesting
    boolean claimConnection(final @NotNull OpcUaClientConnection connection) {
        connectionOwnershipLock.lock();
        try {
            final boolean claimed = opcUaClientConnection.compareAndSet(null, connection);
            if (claimed) {
                // A method NodeId is the server's to assign and stable for a session, so the cache is keyed to
                // the session rather than given an expiry. One of the two seams that start a session: this one
                // covers a first connect and every reconnect the adapter drives by building a replacement
                // connection. The other is the session-active hook, for a reconnect Milo performs inside the
                // existing client -- see OpcUaClientConnection#onSessionReplaced. Both are needed; this one
                // alone kept entries across precisely the event that may renumber them.
                methodCache.invalidate();
                // Connections are numbered, not attempts: a retry of a connection that never reached CONNECTED
                // keeps its number, and its lines tell the attempts apart.
                if (lifecycleConnection.get() == 0 || lifecycleConnectionReady) {
                    lifecycleConnection.incrementAndGet();
                }
                lifecycleSession.set(1);
                lifecycleLostWithoutReconnect = false;
                lifecycleConnectionReady = false;
                lifecycleConnected = false;
                lifecycleMethodsRead = false;
            }
            return claimed;
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
                // The current connection reports DISCONNECTED only when its session is lost: a reconnect and a
                // stop release the connection before closing it, so their DISCONNECTED arrives here as stale.
                if (status == ProtocolAdapterState.ConnectionStatus.DISCONNECTED && lifecycleConnected) {
                    lifecycleConnected = false;
                    reportLifecycle(
                            Event.SEVERITY.WARN, true, "PROBLEM: session to the device lost. Waiting for it to resume");
                }
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
                lifecycleConnectionReady = true;
                lifecycleConnected = true;
                lifecycleMethodsRead = false;
                if (browseReady) {
                    reportLifecycle(Event.SEVERITY.INFO, true, "connected, browse is ready");
                } else {
                    reportLifecycle(
                            Event.SEVERITY.WARN,
                            true,
                            "PROBLEM: connected, but browse is not ready. The data-type tree could not be built. "
                                    + "Retrying by itself");
                }
                prewarmMethodTags(source);
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
     * What a new session means for the per-tag method cache: forget it, then read it again.
     *
     * <p><b>Why both halves, and why here.</b> A method NodeId is the server's to assign and stable only for the
     * session that reported it, so a new session must discard the cache. The two seams that start one are not
     * alike: claiming a connection covers a first connect and every reconnect the adapter drives by building a
     * replacement, and reaches {@link #prewarmMethodTags} through {@link #publishReadyFrom}; a session Milo
     * activates <em>inside</em> the existing client swaps no connection and reaches only this hook. Wiring this
     * hook to invalidation alone therefore cleared the cache and left nothing to refill it, so for the rest of
     * that session a slow device's first request paid for the browse again — the very case the pre-warm exists
     * for. Found by Miguel in round 2 of EDG-1087.
     *
     * <p>Invalidate first, so a request admitted during the pre-warm cannot be served from what the previous
     * session reported. A run already in flight notices the generation change and abandons itself; the
     * single-thread executor then serialises the replacement behind it.
     */
    @VisibleForTesting
    void onSessionReplaced() {
        final OpcUaClientConnection current;
        // Under the ownership lock, so a method read finishing concurrently either writes its line before the
        // cache is cleared or sees the new generation and writes nothing.
        connectionOwnershipLock.lock();
        try {
            methodCache.invalidate();
            current = opcUaClientConnection.get();
            // Only a session after the connection's first CONNECTED is a resumed one. Milo can report an
            // activation while the initial connect is still running, and that connect writes its own line.
            if (current != null && !stopped && lifecycleConnectionReady) {
                lifecycleSession.incrementAndGet();
                lifecycleConnected = true;
                lifecycleMethodsRead = false;
                reportLifecycle(Event.SEVERITY.INFO, true, "session resumed");
            }
        } finally {
            connectionOwnershipLock.unlock();
        }
        if (current != null && !stopped) {
            prewarmMethodTags(current);
        }
    }

    /**
     * Reads every method tag's methods now, so that a caller's first request does not have to.
     * <p>
     * <b>Why this exists.</b> A method tag's methods used to be read on the first request against that tag. On a
     * device that serves metadata slowly that browse can take minutes, so the first request paid for it and
     * could time out while a later one succeeded — and waiting after connect did not help, because nothing was
     * reading anything until a request arrived. Reported by Miguel as part of M11 on EDG-1087: the symptom
     * reproduced on two unrelated devices, and patience was no remedy for this half of it.
     * <p>
     * Called from {@link #publishReadyFrom} for a new connection and from {@link #onSessionReplaced} for a
     * session Milo activates inside an existing one.
     * <p>
     * <b>Why not in the connect-time metadata warm-up.</b> That warm-up — which builds the server's DataType
     * hierarchy — runs inside a task the connect sequence waits on, bounded by the smaller of the connection
     * timeout and 30 seconds. Exceeding the bound abandons the work and carries on. A browse that is allowed to
     * take minutes placed there would hold the adapter back from reporting itself connected and then throw away
     * what it had done. So this runs from the readiness hook instead: after {@code CONNECTED} is published, on
     * its own thread, unbounded, blocking nothing.
     * <p>
     * <b>One tag at a time.</b> A device gets no more concurrent metadata load from the pre-warm than a single
     * caller would produce. The cost is wall-clock time, which nothing is waiting on, rather than pressure on a
     * device that is already answering slowly.
     * <p>
     * <b>Why a failure is only logged.</b> The cache keeps a {@code Rejected} verdict — "this node is not an
     * object", "this node is not there" — because that is durable for the session, and evicts everything else: a
     * browse that threw and an {@code Unverifiable} outcome both leave no entry. So a pre-warm that fails leaves
     * the cache exactly as it found it and the first request browses again, reporting the failure to the caller
     * that provoked it as it always did. There is nothing for this to escalate: the adapter is connected,
     * streaming, and no worse off than before the pre-warm existed.
     * <p>
     * <b>The generation check is what makes this safe.</b> A run may still be in flight when its session ends,
     * and the cache is cleared on every new session because a remembered NodeId may name a different node in the
     * next one. Checking ownership once at the start is not enough — the check is repeated before each tag, so a
     * run whose connection has been replaced stops instead of writing entries belonging to a dead session into
     * the cache of the live one.
     */
    private void prewarmMethodTags(final @NotNull OpcUaClientConnection source) {
        final List<OpcuaTag> methodTags = tagList.stream()
                .filter(tag -> tag.getDefinition().getKind() == OpcuaTagKind.METHOD)
                .toList();
        if (methodTags.isEmpty()) {
            return;
        }
        final ExecutorService executor = methodPrewarmExecutor;
        if (executor == null || executor.isShutdown()) {
            return;
        }
        log.info(
                "OPC UA adapter '{}': reading the methods of {} method tag(s) now, one at a time. A device that "
                        + "serves metadata slowly may take several minutes; nothing waits on this, and a request "
                        + "arriving meanwhile is served as before.",
                adapterId,
                methodTags.size());
        final long generation = methodCache.generation();
        methodPrewarmRun.set(executor.submit(() -> runMethodPrewarm(source, methodTags, generation)));
    }

    /**
     * The pre-warm run started by the most recent readiness publication, for a test that needs to wait for it.
     * <p>
     * Nothing in production waits on this: the point of the run is that nothing does.
     */
    @VisibleForTesting
    @Nullable
    Future<?> methodPrewarmRun() {
        return methodPrewarmRun.get();
    }

    /** Whether a tag's methods are already known, for a test checking what the pre-warm achieved. */
    @VisibleForTesting
    boolean methodsKnownFor(final @NotNull String tagName) {
        return methodCache.isKnown(tagName);
    }

    /** One pre-warm run: each tag in turn, abandoned as soon as the session it belongs to has ended. */
    private void runMethodPrewarm(
            final @NotNull OpcUaClientConnection source,
            final @NotNull List<OpcuaTag> methodTags,
            final long generation) {

        int read = 0;
        // Two lists, because a request answers them differently: a tag whose read failed is read again by its first
        // request, while a tag whose node id cannot be used here is refused without reaching the device.
        final List<String> notRead = new ArrayList<>();
        final List<String> unusable = new ArrayList<>();
        for (final OpcuaTag tag : methodTags) {
            // Three ways this run becomes obsolete, and all three are checked before every tag rather than once
            // at the start: the thread was interrupted by a stop, the adapter was stopped, or the cache was
            // invalidated because a new session began. The last is the one object identity cannot see -- Milo
            // activates a fresh session inside the existing client on a reconnect, so `source` is unchanged
            // across exactly the event that may renumber the node ids this run is storing.
            if (Thread.currentThread().isInterrupted()
                    || stopped
                    || opcUaClientConnection.get() != source
                    || methodCache.generation() != generation) {
                log.debug(
                        "OPC UA adapter '{}': abandoning the method pre-warm after {} of {} tag(s); the session "
                                + "it belongs to has ended",
                        adapterId,
                        read,
                        methodTags.size());
                return;
            }
            final OpcUaClient client = source.client().orElse(null);
            if (client == null) {
                return;
            }
            final String tagName = tag.getName();
            final NodeId objectNode;
            try {
                objectNode = NodeIdText.resolve(tag.getDefinition().getNode(), client.getNamespaceTable());
            } catch (final Exception e) {
                // The request path reports this to the caller as an unusable tag. Nothing to add here: a
                // pre-warm cannot refuse a request that has not arrived.
                log.debug("OPC UA adapter '{}': not pre-warming tag '{}': {}", adapterId, tagName, e.getMessage());
                unusable.add(tagName);
                continue;
            }
            try {
                // Blocking deliberately, which is what makes the run sequential. One tag's browse completes
                // before the next begins, so the device sees one outstanding request at a time.
                methodCache
                        .get(tagName, () -> MethodVerifier.verify(client, objectNode, tagName))
                        .join();
                // The generation may have changed while that browse was in flight, in which case this run
                // belongs to a session that has ended and stops here.
                //
                // It must *not* invalidate on the way out, which is what it used to do. The generation is both
                // the record of which session the cache holds and the signal an obsolete run reads, so bumping
                // it here spent the signal: onSessionReplaced invalidates and then queues a replacement run
                // with the new generation, this run resumed first and bumped it again, and the replacement
                // then saw a number that no longer matched its own and exited without reading a single tag.
                // One reconnect, cache left cold -- in exactly the case pre-warming exists for.
                //
                // Nothing needs cleaning up. That invalidate already cleared the map, and this run's entry
                // went into a fresh one only if the browse finished before the clear; MethodCache.get stores a
                // relay the invalidation removes, and the completing callback does not put it back.
                if (methodCache.generation() != generation) {
                    log.debug(
                            "OPC UA adapter '{}': a new session began while pre-warming tag '{}'; this run stops "
                                    + "and the run queued for the new session fills the cache",
                            adapterId,
                            tagName);
                    return;
                }
                read++;
            } catch (final RuntimeException e) {
                // Logged, not escalated: the cache evicted the failure, so the first request against this tag
                // browses again and tells its caller what went wrong.
                log.debug(
                        "OPC UA adapter '{}': could not pre-warm the methods of tag '{}'; the first request "
                                + "against it will read them again",
                        adapterId,
                        tagName,
                        e);
                notRead.add(tagName);
            }
        }
        if (opcUaClientConnection.get() == source && !stopped) {
            log.info(
                    "OPC UA adapter '{}': pre-warmed {} of {} method tag(s); a request against one of them no "
                            + "longer waits for a browse",
                    adapterId,
                    read,
                    methodTags.size());
            // Not for a session that ended during the last tag: the line for its loss has said what is current,
            // and the run queued for the next session writes this line for that one. The check and the line are
            // one step under the ownership lock, which a resumed session also takes to clear the cache.
            connectionOwnershipLock.lock();
            try {
                if (methodCache.generation() == generation && opcUaClientConnection.get() == source && !stopped) {
                    lifecycleMethodsRead = true;
                    final String readCount =
                            "read the methods of " + read + " of " + methodTags.size() + " method tags";
                    if (notRead.isEmpty() && unusable.isEmpty()) {
                        reportLifecycle(Event.SEVERITY.INFO, true, readCount);
                    } else {
                        final StringBuilder problem = new StringBuilder("PROBLEM: ").append(readCount);
                        if (!notRead.isEmpty()) {
                            problem.append(". Not read, method requests to them read the methods first: '")
                                    .append(String.join("', '", notRead))
                                    .append('\'');
                        }
                        if (!unusable.isEmpty()) {
                            problem.append(". Node id cannot be used, method requests to them are refused: '")
                                    .append(String.join("', '", unusable))
                                    .append('\'');
                        }
                        reportLifecycle(Event.SEVERITY.WARN, true, problem.toString());
                    }
                }
            } finally {
                connectionOwnershipLock.unlock();
            }
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
            client.getDataTypeTree(); // cached on the client by Milo; every later browse reuses it (EDG-488)
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

        lifecycleConnected = false;
        reportLifecycle(
                Event.SEVERITY.WARN,
                true,
                "PROBLEM: could not connect (attempt " + attemptCount + "), see the preceding event. Retrying in "
                        + backoffDelayMs + " ms by itself. A restart does not help");

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
                                this::publishReadyFrom,
                                this::onSessionReplaced);

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
