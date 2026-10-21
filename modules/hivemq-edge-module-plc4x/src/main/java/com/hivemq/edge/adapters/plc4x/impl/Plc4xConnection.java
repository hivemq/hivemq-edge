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
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.apache.plc4x.java.spi.connection.ChannelFactory;
import org.apache.plc4x.java.spi.connection.NettyChannelFactory;
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
    private static final long REINTERRUPT_MILLIS = 50;
    private static final @NotNull ScheduledExecutorService INTERRUPTER =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                final Thread thread = new Thread(runnable, "plc4x-connect-interrupter");
                thread.setDaemon(true);
                return thread;
            });
    private static final @NotNull ExecutorService CONNECT_EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        final Thread thread = new Thread(runnable, "plc4x-connect-" + CONNECT_THREADS.incrementAndGet());
        thread.setDaemon(true);
        thread.setContextClassLoader(Plc4xConnection.class.getClassLoader());
        return thread;
    });

    private final Object lock = new Object();
    private final @NotNull Duration connectTimeout;
    private final @NotNull ExecutorService executor;
    protected volatile @Nullable PlcConnection plcConnection;
    // Guarded by lock: the attempt being connected, the one that connected, and whether disconnect() has run.
    private @Nullable Attempt pending;
    private @Nullable Attempt established;
    // Reads the device has not answered yet: added and drained under lock, removed without it, from the driver's
    // completing thread, which must never wait for lock.
    private final @NotNull Set<CompletableFuture<PlcReadResponse>> reads = ConcurrentHashMap.newKeySet();
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
        this(plcDriverManager, config, connectionQueryStringProvider, connectTimeout, CONNECT_EXECUTOR);
    }

    Plc4xConnection(
            final @NotNull PlcDriverManager plcDriverManager,
            final @NotNull T config,
            final @NotNull Plc4xConnectionQueryStringProvider<T> connectionQueryStringProvider,
            final @NotNull Duration connectTimeout,
            final @NotNull ExecutorService executor)
            throws Plc4xException {
        this.connectTimeout = connectTimeout;
        this.executor = executor;
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
     * waiting on a common-pool task can end up running that task itself, and then no timeout applies. On timeout or
     * {@link #disconnect()} the connect is interrupted, and interrupted again until it ends (S7 and ADS map the
     * interrupt to a {@link PlcConnectionException}; Netty can swallow one). The connection object is closed once,
     * on the connect thread, after {@code connect()} has ended: closed earlier, the drivers leave behind what they
     * create later in the connect. No lock is held while waiting, so {@code disconnect()} never waits for a connect.
     */
    void startConnection(
            final @NotNull EventService eventService, final @NotNull String adapterId, final @NotNull String protocolId)
            throws Plc4xException {
        final Attempt attempt;
        final Future<?> connectTask;
        synchronized (lock) {
            if (closed) {
                throw new Plc4xException("The connection was closed before it was started");
            }
            if (plcConnection != null) {
                return;
            }
            if (pending != null) {
                throw new Plc4xException("A connection attempt is already in flight");
            }
            final String connectionString = createConnectionString(config);
            if (log.isTraceEnabled()) {
                log.trace("Connecting via PLC4X to {}.", connectionString);
            }
            final PlcConnection candidate;
            try {
                candidate = withModuleClassLoader(
                        () -> plcDriverManager.getDriverForUrl(connectionString).getConnection(connectionString));
            } catch (final PlcConnectionException e) {
                throw new Plc4xException("Error encountered connecting to external device: " + e.getMessage(), e);
            }
            attempt = new Attempt(candidate);
            try {
                connectTask = executor.submit(() -> {
                    if (!attempt.claim()) {
                        // Cancelled before it ran; the canceller has ended the attempt.
                        return null;
                    }
                    attempt.enter();
                    boolean connected = false;
                    try {
                        candidate.connect();
                        connected = true;
                        return null;
                    } finally {
                        attempt.exit();
                        final boolean keep;
                        synchronized (lock) {
                            // Decided under the lock that abandon() and disconnect() take: whoever removes a kept
                            // attempt from pending closes it.
                            keep = connected && !closed && pending == attempt;
                            attempt.kept = keep;
                        }
                        // The connect has ended: another attempt may start while this one closes, which for ADS
                        // takes up to 2 s.
                        attempt.terminated.complete(null);
                        if (!keep) {
                            // The driver has created all it will: closing now releases it.
                            attempt.close();
                        }
                    }
                });
            } catch (final RejectedExecutionException e) {
                attempt.close();
                throw new Plc4xException("No thread is available to connect to external device", e);
            }
            attempt.task = connectTask;
            connectTerminated = attempt.terminated;
            pending = attempt;
        }
        try {
            connectTask.get(connectTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            abandon(attempt);
            eventService
                    .createAdapterEvent(adapterId, protocolId)
                    .withSeverity(Event.SEVERITY.WARN)
                    .withMessage("Timed out connecting to the device after " + connectTimeout.toMillis()
                            + " ms; the connection attempt was closed and will be retried.")
                    .fire();
            throw new Plc4xException(
                    "Timed out connecting to external device after " + connectTimeout.toMillis() + " ms");
        } catch (final ExecutionException e) {
            abandon(attempt);
            final Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new Plc4xException("Error encountered connecting to external device: " + cause.getMessage(), cause);
        } catch (final CancellationException e) {
            throw new Plc4xException("The connection was closed while connecting");
        } catch (final InterruptedException e) {
            abandon(attempt);
            Thread.currentThread().interrupt();
            throw new Plc4xException("Interrupted while connecting to external device", e);
        }
        synchronized (lock) {
            if (owns(attempt)) {
                pending = null;
                established = attempt;
                plcConnection = attempt.connection;
                return;
            }
        }
        // disconnect() ran after the connect succeeded; the connect has ended, so close it here.
        attempt.close();
        throw new Plc4xException("The connection was closed while connecting");
    }

    /** Completes once the connect started by this object has ended; already complete when none was started. */
    @NotNull
    CompletableFuture<Void> connectTerminated() {
        return connectTerminated;
    }

    /** Visible for testing: the reads the device has not answered yet. */
    int readsInFlight() {
        return reads.size();
    }

    /** Visible for testing: the driver connection whose connect is running, if any. */
    @Nullable
    PlcConnection runningConnect() {
        synchronized (lock) {
            final Attempt connecting = pending;
            return connecting != null && connecting.isRunning() ? connecting.connection : null;
        }
    }

    private boolean owns(final @NotNull Attempt attempt) {
        synchronized (lock) {
            return !closed && pending == attempt;
        }
    }

    private void abandon(final @NotNull Attempt attempt) {
        final boolean kept;
        synchronized (lock) {
            if (pending == attempt) {
                pending = null;
            }
            kept = attempt.kept;
        }
        if (attempt.cancel() || kept) {
            attempt.close();
        }
    }

    private static void closeQuietly(final @NotNull PlcConnection connection) {
        try {
            connection.close();
        } catch (final Exception e) {
            // Closing a connection that never connected can fail inside the driver; nothing is left to release.
            log.debug("Closing an unused PLC4X connection failed: {}", e.getMessage());
        }
    }

    /**
     * Closes every channel the driver's channel factories created, and their event loops. PLC4X 0.13.1 keeps them in
     * {@code NettyChannelFactory.eventLoops} and drops its own reference to a channel whose connect wait was
     * interrupted, though the TCP connect goes on; nothing else reaches that channel. Best effort: a driver without
     * these fields is left to its own close().
     */
    static void closeDriverChannels(final @NotNull PlcConnection connection) {
        for (Class<?> type = connection.getClass(); type != null; type = type.getSuperclass()) {
            for (final Field field : type.getDeclaredFields()) {
                if (!ChannelFactory.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    if (field.get(connection) instanceof final NettyChannelFactory factory) {
                        driverChannels(factory).forEach((channel, group) -> {
                            @SuppressWarnings("unused")
                            final var closing = channel.close();
                            @SuppressWarnings("unused")
                            final var stopping = group.shutdownGracefully();
                        });
                    }
                } catch (final ReflectiveOperationException | RuntimeException e) {
                    log.debug("Could not close the channels of a PLC4X connection: {}", e.getMessage());
                }
            }
        }
    }

    /** Visible for testing: the channels a driver's channel factory keeps, with their event loops. */
    @SuppressWarnings("unchecked")
    static @NotNull Map<Channel, EventLoopGroup> driverChannels(final @NotNull NettyChannelFactory factory)
            throws ReflectiveOperationException {
        final Field eventLoops = NettyChannelFactory.class.getDeclaredField("eventLoops");
        eventLoops.setAccessible(true);
        return Map.copyOf((Map<Channel, EventLoopGroup>) eventLoops.get(factory));
    }

    /**
     * One connect: the driver's connection, the task connecting it, the thread running it, and when that task has
     * ended. The connection is closed at most once.
     */
    private static final class Attempt {

        final @NotNull PlcConnection connection;
        final @NotNull CompletableFuture<Void> terminated = new CompletableFuture<>();
        private final @NotNull AtomicBoolean claimed = new AtomicBoolean();
        volatile @Nullable Future<?> task;
        // Guarded by this: the thread inside connect(), and whether the connection was closed.
        private @Nullable Thread runner;
        private boolean closed;
        // Guarded by the connection's lock: the connect succeeded while the attempt was still pending.
        boolean kept;

        Attempt(final @NotNull PlcConnection connection) {
            this.connection = connection;
        }

        /** Taken by the connect task before it connects; false when a canceller took it first. */
        boolean claim() {
            return claimed.compareAndSet(false, true);
        }

        synchronized void enter() {
            runner = Thread.currentThread();
        }

        /** No interrupt reaches the thread after this, and one pending is cleared. */
        synchronized void exit() {
            runner = null;
            //noinspection ResultOfMethodCallIgnored
            Thread.interrupted();
        }

        synchronized boolean isRunning() {
            return runner != null;
        }

        /**
         * Stops the connect. Returns true when it never ran, so the attempt has ended and the caller closes the
         * connection; otherwise the connect is interrupted until it ends, and its own thread closes the connection.
         */
        boolean cancel() {
            final Future<?> connectTask = task;
            if (connectTask != null) {
                connectTask.cancel(false);
            }
            if (claim()) {
                terminated.complete(null);
                return true;
            }
            interruptRunner();
            if (!terminated.isDone()) {
                // One interrupt is not always enough: Netty's HashedWheelTimer.start(), which the S7 driver reaches
                // while setting up its protocol layer inside connect(), catches and drops it.
                final ScheduledFuture<?> again = INTERRUPTER.scheduleWithFixedDelay(
                        this::interruptRunner, REINTERRUPT_MILLIS, REINTERRUPT_MILLIS, TimeUnit.MILLISECONDS);
                @SuppressWarnings("unused")
                final var unused = terminated.whenComplete((ignored, t) -> again.cancel(false));
            }
            return false;
        }

        private synchronized void interruptRunner() {
            if (runner != null) {
                runner.interrupt();
            }
        }

        /** Closes the connection unless it was already, swallowing the driver's failure. */
        void close() {
            if (markClosed()) {
                closeQuietly(connection);
                closeDriverChannels(connection);
            }
        }

        /** Closes the established connection unless it was already, reporting the driver's failure. */
        void closeEstablished() throws Exception {
            if (markClosed()) {
                try {
                    connection.close();
                } finally {
                    closeDriverChannels(connection);
                }
            }
        }

        private synchronized boolean markClosed() {
            if (closed) {
                return false;
            }
            closed = true;
            return true;
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
        final Attempt connected;
        final Attempt connecting;
        final boolean kept;
        final List<CompletableFuture<PlcReadResponse>> unanswered;
        synchronized (lock) {
            closed = true;
            unanswered = List.copyOf(reads);
            reads.clear();
            connected = established;
            connecting = pending;
            kept = connecting != null && connecting.kept;
            established = null;
            plcConnection = null;
            pending = null;
        }
        if (connecting != null && (connecting.cancel() || kept)) {
            connecting.close();
        }
        // The drivers drop the reads of a connection they close: fail them, so no poll waits on one.
        unanswered.forEach(read -> read.completeExceptionally(new Plc4xException("The connection was closed")));
        if (connected != null) {
            connected.closeEstablished();
        }
    }

    public boolean isConnected() {
        final PlcConnection connection = plcConnection;
        return connection != null && connection.isConnected();
    }

    /** Reads the tags; the read fails when {@link #disconnect()} runs before the device answered. */
    @SuppressWarnings("CollectionUndefinedEquality") // CompletableFuture keeps Object's identity equality
    public @NotNull CompletableFuture<? extends PlcReadResponse> read(final @NotNull List<Plc4xTag> tags) {
        final PlcConnection conn = plcConnection;
        if (conn == null || !conn.isConnected()) {
            throw new IllegalStateException("The PLC connection is not established");
        }
        if (!conn.getMetadata().isReadSupported()) {
            return CompletableFuture.failedFuture(new Plc4xException("connection type read-blocking"));
        }
        if (log.isTraceEnabled()) {
            log.trace("Sending direct-read request to connection for {}.", tags);
        }
        final PlcReadRequest.Builder builder = conn.readRequestBuilder();
        tags.forEach(tag -> builder.addTagAddress(tag.getName(), getTagAddressForSubscription(tag)));
        final PlcReadRequest readRequest = builder.build();
        final CompletableFuture<PlcReadResponse> result = new CompletableFuture<>();
        // Ok - seems the reads are not thread safe
        synchronized (lock) {
            if (plcConnection != conn) {
                return CompletableFuture.failedFuture(new Plc4xException("The connection was closed"));
            }
            reads.add(result);
            final CompletableFuture<? extends PlcReadResponse> sent;
            try {
                sent = readRequest.execute();
            } catch (final RuntimeException e) {
                reads.remove(result);
                throw e;
            }
            @SuppressWarnings("unused")
            final var unused = sent.whenComplete((response, t) -> {
                reads.remove(result);
                if (t != null) {
                    result.completeExceptionally(t);
                } else {
                    result.complete(response);
                }
            });
        }
        return result;
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
