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

import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.discovery.NodeTree;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryInput;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryOutput;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
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
import com.hivemq.edge.adapters.opcua.client.Failure;
import com.hivemq.edge.adapters.opcua.client.ParsedConfig;
import com.hivemq.edge.adapters.opcua.client.Success;
import com.hivemq.edge.adapters.opcua.config.ConnectionOptions;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.listeners.OpcUaServiceFaultListener;
import com.hivemq.edge.adapters.opcua.southbound.JsonSchemaGenerator;
import com.hivemq.edge.adapters.opcua.southbound.JsonToOpcUAConverter;
import com.hivemq.edge.adapters.opcua.southbound.OpcUaPayload;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OpcUaProtocolAdapter implements WritingProtocolAdapter {
    private static final @NotNull Logger log = LoggerFactory.getLogger(OpcUaProtocolAdapter.class);

    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull String adapterId;
    private final @NotNull Map<String, OpcuaTag> tagNameToTag;
    private final @NotNull List<OpcuaTag> tagList;
    private final @NotNull AtomicReference<OpcUaClientConnection> opcUaClientConnection;

    private final @NotNull DataPointFactory dataPointFactory;
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

    // Flag to prevent scheduling after stop
    private volatile boolean stopped = false;

    public OpcUaProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<OpcUaSpecificAdapterConfig> input) {
        this.adapterId = input.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.tagList = input.getTags().stream().map(tag -> (OpcuaTag) tag).toList();
        this.tagNameToTag = tagList.stream().collect(Collectors.toMap(OpcuaTag::getName, Function.identity()));
        this.dataPointFactory = input.adapterFactories().dataPointFactory();
        this.protocolAdapterMetricsService = input.getProtocolAdapterMetricsHelper();
        this.config = input.getConfig();
        this.opcUaClientConnection = new AtomicReference<>();
        this.opcUaServiceFaultListener = new OpcUaServiceFaultListener(protocolAdapterMetricsService,
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
    public static long calculateBackoffDelayMs(final @NotNull String retryIntervalMs, final int attemptCount) {
        final String[] delayStrings = retryIntervalMs.split(",");
        final long[] backoffDelays = new long[delayStrings.length];

        for (int i = 0; i < delayStrings.length; i++) {
            // NumberFormatException is thrown.
            backoffDelays[i] = Long.parseLong(delayStrings[i].trim());
        }

        // Array is 0-indexed, attemptCount is 1-indexed, so we need attemptCount - 1
        final int index = Math.min(Math.max(0, attemptCount - 1), backoffDelays.length - 1);
        final double backoffDelay =
                backoffDelays[index] * (1 + new Random().nextDouble(ConnectionOptions.DEFAULT_RETRY_JITTER));
        return Double.valueOf(backoffDelay).longValue();
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
            final @NotNull ProtocolAdapterStartInput input,
            final @NotNull ProtocolAdapterStartOutput output) {
        log.info("Starting OPC UA protocol adapter {}", adapterId);

        // Reset stopped flag
        stopped = false;

        startSchedulers();

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
            output.failStart(new IllegalStateException("Unexpected result type: " + result.getClass().getName()),
                    "Failed to parse configuration for OPC UA client");
            return;
        }

        final OpcUaClientConnection conn;
        if (opcUaClientConnection.compareAndSet(null,
                conn = new OpcUaClientConnection(adapterId,
                        tagList,
                        protocolAdapterState,
                        input.moduleServices().protocolAdapterTagStreamingService(),
                        dataPointFactory,
                        input.moduleServices().eventService(),
                        protocolAdapterMetricsService,
                        config,
                        opcUaServiceFaultListener))) {

            protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
            // Attempt initial connection asynchronously
            attemptConnection(conn, newlyParsedConfig, input);

            // Adapter starts successfully even if connection isn't established yet
            // Hardware may come online later and automatic retry will connect
            log.info("Successfully started OPC UA protocol adapter {}", adapterId);
            output.startedSuccessfully();
        } else {
            log.error("Cannot start OPC UA protocol adapter '{}' - adapter is already started", adapterId);
            output.failStart(new IllegalStateException("Adapter already started"),
                    "Cannot start already started adapter. Please stop the adapter first.");
        }
    }

    @Override
    public synchronized void stop(
            final @NotNull ProtocolAdapterStopInput input,
            final @NotNull ProtocolAdapterStopOutput output) {
        log.info("Stopping OPC UA protocol adapter {}", adapterId);

        // Set stopped flag to prevent new scheduling
        stopped = true;

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

            final OpcUaClientConnection conn = opcUaClientConnection.getAndSet(null);
            if (conn != null) {
                conn.stop();
            } else {
                log.info("Tried stopping stopped OPC UA protocol adapter {}", adapterId);
            }
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
                    backoffDelayMs = calculateBackoffDelayMs(config.getConnectionOptions().retryIntervalMs(),
                            (int) reconnectAttempts.get());
                } catch (final Exception e) {
                    backoffDelayMs = calculateBackoffDelayMs(ConnectionOptions.DEFAULT_RETRY_INTERVALS,
                            (int) reconnectAttempts.get());
                }
                if (currentTime - lastReconnectTime < backoffDelayMs) {
                    log.debug("Reconnection for adapter '{}' attempted too soon after last reconnect - skipping",
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
            final OpcUaClientConnection oldConn = opcUaClientConnection.getAndSet(null);
            if (oldConn != null) {
                oldConn.stop();
                log.debug("Stopped old connection for OPC UA adapter '{}'", adapterId);
            }

            // Create new connection
            final OpcUaClientConnection newConn = new OpcUaClientConnection(adapterId,
                    tagList,
                    protocolAdapterState,
                    moduleServices.protocolAdapterTagStreamingService(),
                    dataPointFactory,
                    moduleServices.eventService(),
                    protocolAdapterMetricsService,
                    config,
                    opcUaServiceFaultListener);

            // Set as current connection and attempt connection with retry logic
            protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
            if (opcUaClientConnection.compareAndSet(null, newConn)) {
                // Create a minimal ProtocolAdapterStartInput for attemptConnection
                final ProtocolAdapterStartInput input = new ProtocolAdapterStartInput() {
                    @Override
                    public @NotNull ModuleServices moduleServices() {
                        return moduleServices;
                    }
                };
                attemptConnection(newConn, parsedConfig, input);
            } else {
                log.warn("OPC UA adapter '{}' reconnect failed - another connection was created concurrently",
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
        final ScheduledFuture<?> future = healthCheckScheduler.scheduleAtFixedRate(() -> {
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
                    protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.ERROR);
                }
            } else {
                log.debug("Health check passed for adapter '{}'", adapterId);
            }
        }, healthCheckIntervalMs, healthCheckIntervalMs, TimeUnit.MILLISECONDS);

        // Store future so it can be cancelled if needed
        final ScheduledFuture<?> oldFuture = healthCheckFuture.getAndSet(future);
        if (oldFuture != null && !oldFuture.isDone()) {
            oldFuture.cancel(false);
        }

        log.debug("Scheduled connection health check every {} milliseconds for adapter '{}'",
                healthCheckIntervalMs,
                adapterId);
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
     */
    private synchronized void startSchedulers() {
        retryScheduler = Executors.newSingleThreadScheduledExecutor();
        healthCheckScheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void destroy() {
        log.info("Destroying OPC UA protocol adapter {}", adapterId);

        // Cancel any pending retries and health checks
        cancelRetry();
        cancelHealthCheck();

        // Shutdown schedulers (if not already shutdown in stop())
        shutdownSchedulers();

        final OpcUaClientConnection conn = opcUaClientConnection.getAndSet(null);
        if (conn != null) {
            CompletableFuture.runAsync(() -> {
                conn.destroy();
                log.info("Destroyed OPC UA protocol adapter {}", adapterId);
            });
        } else {
            log.info("Tried destroying stopped OPC UA protocol adapter {}", adapterId);
        }
    }

    @Override
    public void discoverValues(
            final @NotNull ProtocolAdapterDiscoveryInput input,
            final @NotNull ProtocolAdapterDiscoveryOutput output) {
        if (stopped) {
            log.debug("Discovery operation skipped for adapter '{}' - adapter has been stopped", adapterId);
            output.fail("Discovery failed: Adapter has been stopped");
            return;
        }
        if (input.getRootNode() == null) {
            log.error("Discovery failed: Root node is null");
            output.fail("Root node is null");
            return;
        }
        final OpcUaClientConnection conn = opcUaClientConnection.get();
        if (conn == null) {
            output.fail("Discovery failed: ClientConnection not connected or not initialized");
            return;
        }
        conn.client()
                .ifPresentOrElse(client -> OpcUaNodeDiscovery.discoverValues(client,
                        input.getRootNode(),
                        input.getDepth()).whenComplete((collectedNodes, throwable) -> {
                    if (throwable == null) {
                        final NodeTree nodeTree = output.getNodeTree();
                        collectedNodes.forEach(node -> nodeTree.addNode(node.id(),
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
                }), () -> output.fail("Discovery failed: Client not connected or not initialized"));
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

        final OpcUaClientConnection conn = opcUaClientConnection.get();
        if (conn == null) {
            output.fail("Discovery failed: ClientConnection not connected or not initialized");
            return;
        }

        conn.client().ifPresentOrElse(client -> {
            final JsonToOpcUAConverter converter = new JsonToOpcUAConverter(client);
            if (log.isDebugEnabled()) {
                log.debug("Write invoked with payload '{}' for tag '{}'", opcUAWritePayload, opcuaTag.getName());
            }

            final NodeId nodeId = NodeId.parse(opcuaTag.getDefinition().getNode());
            final Object opcuaObject = converter.convertToOpcUAValue(opcUAWritePayload.value(), nodeId);

            client.writeValuesAsync(List.of(nodeId),
                            List.of(new DataValue(Variant.of(opcuaObject), StatusCode.GOOD, null)))
                    .whenComplete((statusCodes, throwable) -> {
                        final var badStatus = statusCodes.stream().filter(StatusCode::isBad).findFirst();
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
        }, () -> output.fail("Discovery failed: Client not connected or not initialized"));
    }

    @Override
    public void createTagSchema(
            final @NotNull TagSchemaCreationInput input,
            final @NotNull TagSchemaCreationOutput output) {
        if (stopped) {
            log.debug("Create tag schema operation skipped for adapter '{}' - adapter has been stopped", adapterId);
            output.fail("Create tag schema failed: Adapter has been stopped");
            return;
        }
        final String tagName = input.getTagName();
        final OpcuaTag tag = tagNameToTag.get(tagName);
        if (tag == null) {
            log.error("Cannot create schema for non-existent tag '{}'", tagName);
            output.fail("Tag '" + tagName + "' not found.");
            return;
        }

        final OpcUaClientConnection conn = opcUaClientConnection.get();
        if (conn == null) {
            output.fail("Discovery failed: ClientConnection not connected or not initialized");
            return;
        }
        conn.client()
                .ifPresentOrElse(client -> new JsonSchemaGenerator(client).createMqttPayloadJsonSchema(tag)
                        .whenComplete((result, throwable) -> {
                            if (throwable == null) {
                                result.ifPresentOrElse(schema -> {
                                    log.debug("Schema inferred for tag='{}'", tagName);
                                    output.finish(schema);
                                }, () -> {
                                    log.error("No schema inferred for tag='{}'", tagName);
                                    output.fail("No schema inferred for tag='" + tagName + "'");
                                });
                            } else {
                                log.error("Exception while creating tag schema for '{}'", tagName, throwable);
                                output.fail(throwable, null);
                            }
                        }), () -> {
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

    /**
     * Attempts to establish connection to OPC UA server.
     * On failure, schedules automatic retry after configured retry interval.
     */
    private void attemptConnection(
            final @NotNull OpcUaClientConnection conn,
            final @NotNull ParsedConfig parsedConfig,
            final @NotNull ProtocolAdapterStartInput input) {

        CompletableFuture.supplyAsync(() -> conn.start(parsedConfig)).whenComplete((success, throwable) -> {
            lastReconnectTimestamp.set(System.currentTimeMillis());
            if (success && throwable == null) {
                reconnectAttempts.set(0);
                // Connection succeeded - cancel any pending retries and start health check
                cancelRetry();
                scheduleHealthCheck();
                log.info("OPC UA adapter '{}' connected successfully", adapterId);
            } else {
                // Connection failed - clean up and schedule retry with exponential backoff
                this.opcUaClientConnection.set(null);
                protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.ERROR);

                if (throwable != null) {
                    log.warn("OPC UA adapter '{}' connection failed, scheduling retry with exponential backoff",
                            adapterId,
                            throwable);
                } else {
                    log.warn("OPC UA adapter '{}' connection returned false, scheduling retry with exponential backoff",
                            adapterId);
                }

                cancelHealthCheck();
                // Schedule retry attempt with exponential backoff
                scheduleRetry(input);
            }
        });
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
            backoffDelayMs = calculateBackoffDelayMs(config.getConnectionOptions().retryIntervalMs(), attemptCount);
        } catch (final Exception e) {
            log.warn("Failed to calculate backoff delay for adapter '{}' from retryIntervalMs {}",
                    adapterId,
                    config.getConnectionOptions().retryIntervalMs(),
                    e);
            backoffDelayMs = calculateBackoffDelayMs(ConnectionOptions.DEFAULT_RETRY_INTERVALS, attemptCount);
        }

        log.info("Scheduling retry attempt #{} for OPC UA adapter '{}' with backoff delay of {} ms",
                attemptCount,
                adapterId,
                backoffDelayMs);

        final ScheduledFuture<?> future = retryScheduler.schedule(() -> {
            // Check if adapter was stopped before retry executes
            if (stopped || this.parsedConfig == null || this.moduleServices == null) {
                log.debug("OPC UA adapter '{}' retry cancelled - adapter was stopped", adapterId);
                return;
            }

            log.info("Executing retry attempt #{} for OPC UA adapter '{}'", attemptCount, adapterId);

            // Create new connection object for retry
            final OpcUaClientConnection newConn = new OpcUaClientConnection(adapterId,
                    tagList,
                    protocolAdapterState,
                    this.moduleServices.protocolAdapterTagStreamingService(),
                    dataPointFactory,
                    this.moduleServices.eventService(),
                    protocolAdapterMetricsService,
                    config,
                    opcUaServiceFaultListener);

            // Set as current connection and attempt
            protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
            if (opcUaClientConnection.compareAndSet(null, newConn)) {
                attemptConnection(newConn, this.parsedConfig, input);
            } else {
                log.debug("OPC UA adapter '{}' retry skipped - connection already exists", adapterId);
            }
        }, backoffDelayMs, TimeUnit.MILLISECONDS);

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
