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
package com.hivemq.edge.adapters.plc4x.impl;

import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.edge.adapters.plc4x.Plc4xException;
import com.hivemq.edge.adapters.plc4x.config.Plc4XSpecificAdapterConfig;
import com.hivemq.edge.adapters.plc4x.config.tag.Plc4xTag;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.PlcDriverManager;
import org.apache.plc4x.java.api.exceptions.PlcConnectionException;
import org.apache.plc4x.java.api.messages.PlcReadRequest;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.apache.plc4x.java.api.messages.PlcSubscriptionEvent;
import org.apache.plc4x.java.api.messages.PlcSubscriptionRequest;
import org.apache.plc4x.java.api.messages.PlcSubscriptionResponse;
import org.apache.plc4x.java.api.model.PlcSubscriptionHandle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Plc4xConnection<T extends Plc4XSpecificAdapterConfig<?>> {

    private static final @NotNull Logger log = LoggerFactory.getLogger(Plc4xConnection.class);
    private static final int MAX_UINT16 = 65535;
    protected final @NotNull PlcDriverManager plcDriverManager;
    protected final @NotNull T config;
    protected final @NotNull Plc4xConnectionQueryStringProvider<T> connectionQueryStringProvider;
    static final @NotNull Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final @NotNull AtomicInteger CONNECT_THREADS = new AtomicInteger();
    private static final @NotNull ExecutorService CONNECT_EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        final Thread thread = new Thread(runnable, "plc4x-connect-" + CONNECT_THREADS.incrementAndGet());
        thread.setDaemon(true);
        thread.setContextClassLoader(Plc4xConnection.class.getClassLoader());
        return thread;
    });

    private final Object lock = new Object();
    private final @NotNull Duration connectTimeout;
    protected volatile @Nullable PlcConnection plcConnection;
    // Guarded by lock: the connection being connected, its connect, and whether disconnect() has run.
    private @Nullable PlcConnection pendingConnection;
    private @Nullable Future<?> pendingConnect;
    private boolean closed;
    private volatile @NotNull CompletableFuture<Void> connectTerminated = CompletableFuture.completedFuture(null);

    public Plc4xConnection(
            final @NotNull PlcDriverManager plcDriverManager,
            final @NotNull T config,
            final @NotNull Plc4xConnectionQueryStringProvider<T> connectionQueryStringProvider)
            throws Plc4xException {
        this(plcDriverManager, config, connectionQueryStringProvider, DEFAULT_CONNECT_TIMEOUT);
    }

    protected Plc4xConnection(
            final @NotNull PlcDriverManager plcDriverManager,
            final @NotNull T config,
            final @NotNull Plc4xConnectionQueryStringProvider<T> connectionQueryStringProvider,
            final @NotNull Duration connectTimeout)
            throws Plc4xException {
        this.connectTimeout = connectTimeout;
        this.plcDriverManager = plcDriverManager;
        this.config = config;
        this.connectionQueryStringProvider = connectionQueryStringProvider;
        if (!validConfiguration(config)) {
            if (log.isTraceEnabled()) {
                log.trace("Configuration provided to PLC4X, connection was considered invalid by implementation");
            }
            throw new Plc4xException("invalid connection configuration, unable to initialize");
        }
    }

    protected @NotNull String createConnectionString(final @NotNull T config) {
        final String queryString = connectionQueryStringProvider.getConnectionQueryString(config);
        if (queryString != null && !queryString.trim().isEmpty()) {
            return String.format(
                    "%s://%s:%s?%s",
                    getProtocol().trim(), config.getHost().trim(), config.getPort(), queryString.trim());
        } else {
            return String.format(
                    "%s://%s:%s", getProtocol().trim(), config.getHost().trim(), config.getPort());
        }
    }

    /**
     * Connects to the device, giving up after the connect timeout.
     * <p>
     * The driver's connection object is created first, without I/O, so this class owns it before any connection is
     * attempted. The connect itself runs on a thread of {@link #connectExecutor()}, never on the caller's: a caller
     * waiting on a common-pool task can end up running that task itself, and then no timeout applies. On timeout,
     * failure or {@link #disconnect()}, the connection object is closed and the connect is interrupted, which stops
     * the driver's internal retry loop and ends the blocked connect (S7 and ADS map the interrupt to a
     * {@link PlcConnectionException}). No lock is held while waiting, so {@code disconnect()} never waits for a
     * connect.
     */
    void startConnection(
            final @NotNull EventService eventService, final @NotNull String adapterId, final @NotNull String protocolId)
            throws Plc4xException {
        final PlcConnection candidate;
        final Future<?> connectTask;
        synchronized (lock) {
            if (closed) {
                throw new Plc4xException("The connection was closed before it was started");
            }
            if (plcConnection != null) {
                return;
            }
            if (pendingConnection != null) {
                throw new Plc4xException("A connection attempt is already in flight");
            }
            final String connectionString = createConnectionString(config);
            if (log.isTraceEnabled()) {
                log.trace("Connecting via PLC4X to {}.", connectionString);
            }
            try {
                candidate = withModuleClassLoader(
                        () -> plcDriverManager.getDriverForUrl(connectionString).getConnection(connectionString));
            } catch (final PlcConnectionException e) {
                throw new Plc4xException("Error encountered connecting to external device: " + e.getMessage(), e);
            }
            final CompletableFuture<Void> terminated = new CompletableFuture<>();
            try {
                connectTask = connectExecutor().submit(() -> {
                    try {
                        candidate.connect();
                        return null;
                    } finally {
                        terminated.complete(null);
                    }
                });
            } catch (final RejectedExecutionException e) {
                closeQuietly(candidate);
                throw new Plc4xException("No thread is available to connect to external device", e);
            }
            connectTerminated = terminated;
            pendingConnection = candidate;
            pendingConnect = connectTask;
        }
        try {
            connectTask.get(connectTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            abandon(candidate, connectTask);
            eventService
                    .createAdapterEvent(adapterId, protocolId)
                    .withSeverity(Event.SEVERITY.WARN)
                    .withMessage("Timed out connecting to the device after " + connectTimeout.toMillis()
                            + " ms; the connection attempt was closed and will be retried.")
                    .fire();
            throw new Plc4xException(
                    "Timed out connecting to external device after " + connectTimeout.toMillis() + " ms");
        } catch (final ExecutionException e) {
            abandon(candidate, connectTask);
            final Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new Plc4xException("Error encountered connecting to external device: " + cause.getMessage(), cause);
        } catch (final CancellationException e) {
            throw new Plc4xException("The connection was closed while connecting");
        } catch (final InterruptedException e) {
            abandon(candidate, connectTask);
            Thread.currentThread().interrupt();
            throw new Plc4xException("Interrupted while connecting to external device", e);
        }
        synchronized (lock) {
            if (closed || pendingConnection != candidate) {
                // disconnect() ran while the connect was completing; it no longer owns this connection.
                closeQuietly(candidate);
                throw new Plc4xException("The connection was closed while connecting");
            }
            pendingConnection = null;
            pendingConnect = null;
            plcConnection = candidate;
        }
    }

    /** Completes once the connect started by this object has ended; already complete when none was started. */
    @NotNull
    CompletableFuture<Void> connectTerminated() {
        return connectTerminated;
    }

    private void abandon(final @NotNull PlcConnection candidate, final @NotNull Future<?> connectTask) {
        synchronized (lock) {
            if (pendingConnection == candidate) {
                pendingConnection = null;
                pendingConnect = null;
            }
        }
        connectTask.cancel(true);
        closeQuietly(candidate);
    }

    private static void closeQuietly(final @NotNull PlcConnection connection) {
        try {
            connection.close();
        } catch (final Exception e) {
            // Closing a connection that never connected can fail inside the driver; nothing is left to release.
            log.debug("Closing an unused PLC4X connection failed: {}", e.getMessage());
        }
    }

    private static <R> R withModuleClassLoader(final @NotNull DriverCall<R> call) throws PlcConnectionException {
        // As DefaultPlcDriverManager does: drivers load their transports through the context class loader.
        final Thread thread = Thread.currentThread();
        final ClassLoader original = thread.getContextClassLoader();
        thread.setContextClassLoader(Plc4xConnection.class.getClassLoader());
        try {
            return call.call();
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    @FunctionalInterface
    private interface DriverCall<R> {
        R call() throws PlcConnectionException;
    }

    /**
     * Daemon threads for connection attempts and their connects, with the module's class loader as context class
     * loader. Not the common pool: see {@link #startConnection}.
     */
    static @NotNull ExecutorService connectExecutor() {
        return CONNECT_EXECUTOR;
    }

    /** Fails when the connection is not established; reconnecting is the adapter's job, under its timeout. */
    protected void lazyConnectionCheck() {
        final PlcConnection connection = plcConnection;
        if (connection == null || !connection.isConnected()) {
            throw new IllegalStateException("The PLC connection is not established");
        }
    }

    /** Closes the connection, and any connect in flight, which is interrupted. Never waits for a connect. */
    public void disconnect() throws Exception {
        final PlcConnection established;
        final PlcConnection pending;
        final Future<?> pendingTask;
        synchronized (lock) {
            closed = true;
            established = plcConnection;
            pending = pendingConnection;
            pendingTask = pendingConnect;
            plcConnection = null;
            pendingConnection = null;
            pendingConnect = null;
        }
        if (pendingTask != null) {
            pendingTask.cancel(true);
        }
        if (pending != null) {
            closeQuietly(pending);
        }
        if (established != null) {
            established.close();
        }
    }

    public boolean isConnected() {
        return plcConnection != null && plcConnection.isConnected();
    }

    public @NotNull CompletableFuture<? extends PlcReadResponse> read(final @NotNull List<Plc4xTag> tags) {
        lazyConnectionCheck();
        final PlcConnection conn = Objects.requireNonNull(plcConnection);
        if (!conn.getMetadata().isReadSupported()) {
            return CompletableFuture.failedFuture(new Plc4xException("connection type read-blocking"));
        }
        if (log.isTraceEnabled()) {
            log.trace("Sending direct-read request to connection for {}.", tags);
        }
        final PlcReadRequest.Builder builder = conn.readRequestBuilder();
        tags.forEach(tag -> builder.addTagAddress(tag.getName(), getTagAddressForSubscription(tag)));
        final PlcReadRequest readRequest = builder.build();
        // Ok - seems the reads are not thread safe
        synchronized (lock) {
            return readRequest.execute();
        }
    }

    public @NotNull CompletableFuture<? extends PlcSubscriptionResponse> subscribe(
            final @NotNull Plc4xTag tag, final @NotNull Consumer<PlcSubscriptionEvent> consumer) {
        lazyConnectionCheck();
        final PlcConnection conn = Objects.requireNonNull(plcConnection);
        if (!conn.getMetadata().isSubscribeSupported()) {
            return CompletableFuture.failedFuture(new Plc4xException("connection type cannot subscribe"));
        }
        if (log.isTraceEnabled()) {
            log.trace("Sending subscribe request to connection for {}.", tag.getName());
        }
        final PlcSubscriptionRequest.Builder builder = conn.subscriptionRequestBuilder();

        // TODO we're only registering for state change, could also register events
        builder.addChangeOfStateTagAddress(tag.getName(), getTagAddressForSubscription(tag));
        PlcSubscriptionRequest subscriptionRequest = builder.build();
        CompletableFuture<PlcSubscriptionResponse> future =
                (CompletableFuture<PlcSubscriptionResponse>) subscriptionRequest.execute();
        @SuppressWarnings("unused")
        final var unused = future.whenComplete((plcSubscriptionResponse, throwable) -> {
            if (throwable != null) {
                log.warn("Connection subscription encountered an error;", throwable);
            } else {
                for (final String subscriptionName : plcSubscriptionResponse.getTagNames()) {
                    final PlcSubscriptionHandle subscriptionHandle =
                            plcSubscriptionResponse.getSubscriptionHandle(subscriptionName);
                    subscriptionHandle.register(consumer);
                }
            }
        });
        return future;
    }

    protected boolean validConfiguration(final @NotNull T config) {
        return config.getHost() != null && config.getPort() > 0 && config.getPort() < MAX_UINT16;
    }

    /**
     * Concrete implementations should provide the protocol with which they are connecting
     */
    protected abstract @NotNull String getProtocol();

    /**
     * Each adapter type will have its own address format. The implementation should provide the defaults
     */
    protected abstract @NotNull String getTagAddressForSubscription(final @NotNull Plc4xTag tag);
}
