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

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTING;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.DISCONNECTED;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.ERROR;

import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.datapoint.DataPointListBuilder;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingInput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.SchemaBuilder;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationInput;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationOutput;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.adapters.plc4x.Plc4xException;
import com.hivemq.edge.adapters.plc4x.PublishChangedDataOnlyHandler;
import com.hivemq.edge.adapters.plc4x.config.Plc4XSpecificAdapterConfig;
import com.hivemq.edge.adapters.plc4x.config.Plc4xDataType;
import com.hivemq.edge.adapters.plc4x.config.Plc4xToMqttMapping;
import com.hivemq.edge.adapters.plc4x.config.tag.Plc4xTag;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.plc4x.java.api.PlcDriverManager;
import org.apache.plc4x.java.api.exceptions.PlcInvalidTagException;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.apache.plc4x.java.api.types.PlcResponseCode;
import org.apache.plc4x.java.api.value.PlcValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract PLC4X implementation. Exposes core abstractions of the underlying framework so instances can be exposes
 * using the consistent
 * patterns.
 */
public abstract class AbstractPlc4xAdapter<T extends Plc4XSpecificAdapterConfig<?>, C extends Plc4xToMqttMapping>
        implements BatchPollingProtocolAdapter {

    protected static final @NotNull String TAG_ADDRESS_TYPE_SEP = ":";
    protected static final @NotNull PlcDriverManager driverManager = PlcDriverManager.getDefault();
    // The sampler times a poll out 10 s after poll() returns (AbstractSubscriptionSampler.POLL_OUTPUT_TIMEOUT_MILLIS
    // in Edge, which a test holds this to): a publish within this time of poll() is handed on.
    static final long SAMPLER_TIMEOUT_MILLIS = 10_000;
    // A read must be answered this long before the sampler's timeout, which leaves the answer time to be converted and
    // published. The drivers' own limits do not keep a read under the sampler's timeout: PLC4X 0.13.1's S7 driver
    // waits 10 s for every request (a constant, not configurable), the ADS driver 4 s by default (its timeout-request
    // parameter, which this adapter does not set), and a poll that waited on a connection attempt has less than the
    // whole timeout left. An estimate of the publishing time, not measured: the time from a read's answer to the
    // return of its publish, taken on a node at its highest polling load, would settle it.
    static final long READ_MARGIN_MILLIS = 500;
    private static final long READ_MARGIN_NANOS = TimeUnit.MILLISECONDS.toNanos(READ_MARGIN_MILLIS);

    protected final @NotNull T adapterConfig;
    protected final @NotNull List<Plc4xTag> tags;
    private final @NotNull Logger log;
    private final @NotNull Object lock;
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull String adapterId;
    private final long samplerTimeoutNanos;
    // The latest read started, and on which connection. Written by the thread that starts a read (the polling thread,
    // or the connect thread for a poll that waited on a connection attempt or on the previous read), read by the
    // thread starting the next one. The sampler starts a poll only after the previous one completed, so two reads do
    // not start at once.
    private volatile @Nullable InFlightRead lastRead;
    // The tags whose last read failed, so a tag's failure and its recovery are logged once each, not on every poll.
    // Added to and removed from by the read callbacks of polls, on the driver's threads.
    private final @NotNull Set<String> failingTags = ConcurrentHashMap.newKeySet();
    // One change cache per run, which also tells a poll whether its run is still on: replaced by stop() before it
    // disconnects and by start(), both on the lifecycle thread; read by poll() on the polling thread and by the poll's
    // connection-attempt and read callbacks on the connect and driver threads. A poll keeps the cache of the run it
    // started in, so a poll still completing after a stop cannot write into the next run's cache, and sees its run
    // ended once the field holds another cache.
    private volatile @NotNull PublishChangedDataOnlyHandler changeCache = new PublishChangedDataOnlyHandler();

    private final @NotNull AtomicReference<CompletableFuture<Void>> attempt = new AtomicReference<>();
    // Guarded by lock: set by stop(), cleared by start(); a stopped adapter starts no connection attempt.
    private boolean stopped;
    // Guarded by lock: the connection of the latest attempt, whose connect may still be ending after a timeout.
    private @Nullable Plc4xConnection<T> latestAttempt;

    protected volatile @Nullable Plc4xConnection<T> connection;
    private volatile @Nullable EventService eventService;

    public AbstractPlc4xAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation, final ProtocolAdapterInput<T> input) {
        this(adapterInformation, input, SAMPLER_TIMEOUT_MILLIS);
    }

    /**
     * {@code samplerTimeoutMillis} is how long after {@code poll} is called the sampler still takes its output; a
     * publish later than that is not recorded in the change cache.
     */
    AbstractPlc4xAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<T> input,
            final long samplerTimeoutMillis) {
        this.samplerTimeoutNanos = TimeUnit.MILLISECONDS.toNanos(samplerTimeoutMillis);
        this.adapterId = input.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.tags = input.getTags().stream().map(tag -> (Plc4xTag) tag).toList();
        this.log = LoggerFactory.getLogger(getClass());
        this.lock = new Object();
    }

    public static @NotNull String nullSafe(final @Nullable Object o) {
        return Objects.toString(o);
    }

    @Override
    public void poll(final @NotNull BatchPollingInput pollingInput, final @NotNull BatchPollingOutput pollingOutput) {
        final long pollStartNanos = System.nanoTime();
        final PublishChangedDataOnlyHandler runChangeCache = changeCache;
        // Read the attempt before the connection: an attempt sets the connection before it is cleared, so with no
        // attempt in flight the connection read below is the last attempt's final outcome.
        final CompletableFuture<Void> inFlight = attempt.get();
        if (inFlight != null) {
            completeAfter(inFlight, pollingOutput, pollStartNanos, runChangeCache);
            return;
        }
        final Plc4xConnection<T> tempConnection = connection;
        if (tempConnection != null && tempConnection.isConnected()) {
            read(tempConnection, pollingOutput, pollStartNanos, runChangeCache);
            return;
        }
        // No usable connection. Tear down whatever is left -- that is what stops the driver's internal retry loop and
        // releases what it has retained -- then start a fresh attempt and let its outcome decide this poll, as a
        // read would: a failed attempt is a polling error, paced by the polling task's backoff and counted toward
        // maxPollingErrorsBeforeRemoval, and a successful one is read at once.
        if (tempConnection != null && release(tempConnection)) {
            log.warn("The connection of adapter '{}' was lost, reconnecting.", adapterId);
            disconnectQuietly(tempConnection);
        }
        // With the connection released, or already replaced by another poll's attempt, connect() either starts an
        // attempt, joins the one in flight, or hands back the connection that is now in place.
        final CompletableFuture<Void> started;
        try {
            started = connect();
        } catch (final Plc4xException e) {
            pollingOutput.fail(e, "No connection attempt could be started for adapter '" + adapterId + "'");
            return;
        }
        completeAfter(started, pollingOutput, pollStartNanos, runChangeCache);
    }

    /**
     * Clears the connection field if it still holds {@code lost}. Taken under the same lock as {@link #connect()}, so
     * a poll that saw an old connection cannot clear one that another poll has established since.
     */
    private boolean release(final @NotNull Plc4xConnection<T> lost) {
        synchronized (lock) {
            if (connection != lost) {
                return false;
            }
            connection = null;
            // Set under the lock, before another attempt can start, so it never overwrites that attempt's status.
            protocolAdapterState.setConnectionStatus(ERROR);
            return true;
        }
    }

    private void completeAfter(
            final @NotNull CompletableFuture<Void> connectionAttempt,
            final @NotNull BatchPollingOutput pollingOutput,
            final long pollStartNanos,
            final @NotNull PublishChangedDataOnlyHandler runChangeCache) {
        @SuppressWarnings("unused")
        final var unused = connectionAttempt.whenComplete((_, t) -> {
            if (hasEnded(runChangeCache)) {
                retire(pollingOutput.dataPointListPublisher());
                return;
            }
            if (t != null) {
                // An unreachable device is expected and handled: one line with the driver's cause, no stack trace.
                final Throwable cause = t instanceof CompletionException && t.getCause() != null ? t.getCause() : t;
                pollingOutput.fail("Polling failed for adapter '" + adapterId
                        + "' because no connection could be established: " + cause.getMessage());
                return;
            }
            final Plc4xConnection<T> established = connection;
            if (established == null || !established.isConnected()) {
                pollingOutput.fail("Polling failed for adapter '" + adapterId + "' because the connection was closed");
                return;
            }
            read(established, pollingOutput, pollStartNanos, runChangeCache);
        });
    }

    /** Reads the tags into this poll's output; every outcome, a synchronous failure included, completes it. */
    private void read(
            final @NotNull Plc4xConnection<T> usable,
            final @NotNull BatchPollingOutput pollingOutput,
            final long pollStartNanos,
            final @NotNull PublishChangedDataOnlyHandler runChangeCache) {
        try {
            readTags(usable, pollingOutput, pollStartNanos, runChangeCache);
        } catch (final RuntimeException e) {
            // A stop between taking the connection and the read closes it, and the read throws; that poll's run has
            // ended, so it retires like any other poll of an ended run.
            if (hasEnded(runChangeCache)) {
                retire(pollingOutput.dataPointListPublisher());
                return;
            }
            pollingOutput.fail(e, "Polling failed for adapter '" + adapterId + "' while reading from the device");
        }
    }

    private void readTags(
            final @NotNull Plc4xConnection<T> usable,
            final @NotNull BatchPollingOutput pollingOutput,
            final long pollStartNanos,
            final @NotNull PublishChangedDataOnlyHandler runChangeCache) {
        final var dataPointsPublisher = pollingOutput.dataPointListPublisher();
        if (!tags.isEmpty()) {
            final InFlightRead previous = lastRead;
            if (previous != null
                    && previous.connection() == usable
                    && !previous.read().isDone()) {
                // The drivers send one request at a time, so a read sent now would queue behind the unanswered one.
                // Failing this poll instead would count one unanswered read twice: S7 holds a request for 10 s, past
                // the poll that gave up on it, into the next poll. This poll reads once the driver answers or gives
                // up, within what is left of its own time; off the driver's thread, which completes the previous read.
                @SuppressWarnings("unused")
                final var waited = previous.read()
                        .whenCompleteAsync(
                                (_, _) -> {
                                    if (hasEnded(runChangeCache)) {
                                        retire(dataPointsPublisher);
                                        return;
                                    }
                                    read(usable, pollingOutput, pollStartNanos, runChangeCache);
                                },
                                Plc4xConnection.connectExecutor());
                return;
            }
            final long readBudgetNanos = samplerTimeoutNanos - READ_MARGIN_NANOS - (System.nanoTime() - pollStartNanos);
            if (readBudgetNanos <= 0) {
                // A read now would be answered after the sampler takes the poll's output no longer, and a value the
                // device clears on read would be lost; the next poll reads instead. The connection is up, so this
                // poll ends without data rather than as a polling error: the SDK has no outcome that leaves the
                // error count as it is, and failing it would stop an adapter at its limit before the next poll
                // could read.
                log.debug(
                        "Too little of the poll's time was left to read, the next poll reads [adapterId={},"
                                + " samplerTimeoutMillis={}, readMarginMillis={}]",
                        adapterId,
                        TimeUnit.NANOSECONDS.toMillis(samplerTimeoutNanos),
                        READ_MARGIN_MILLIS);
                dataPointsPublisher.publish();
                return;
            }
            final CompletableFuture<? extends PlcReadResponse> sent = usable.read(tags);
            lastRead = new InFlightRead(usable, sent);
            // Timed out on a copy: the connection's own future stays pending until the driver answers or the
            // connection closes, which is what the in-flight check above looks at.
            @SuppressWarnings("unused")
            final var unused = sent.copy()
                    .orTimeout(readBudgetNanos, TimeUnit.NANOSECONDS)
                    .whenComplete((response, failure) -> {
                        if (hasEnded(runChangeCache)) {
                            retire(dataPointsPublisher);
                            return;
                        }
                        final Throwable t = failure instanceof CompletionException && failure.getCause() != null
                                ? failure.getCause()
                                : failure;
                        if (t instanceof Plc4xException) {
                            // Handled, as when the adapter stops during a read: one line with the cause, no stack
                            // trace.
                            pollingOutput.fail("Polling failed for adapter '" + adapterId
                                    + "' while reading from the device: " + t.getMessage());
                        } else if (t instanceof TimeoutException) {
                            pollingOutput.fail("Polling failed for adapter '" + adapterId
                                    + "' because the device did not answer the read in time [readTimeoutMillis="
                                    + TimeUnit.NANOSECONDS.toMillis(readBudgetNanos) + "]");
                        } else if (t != null) {
                            pollingOutput.fail(t, null);
                        } else {
                            try {
                                publishResponse(
                                        response, pollingOutput, dataPointsPublisher, pollStartNanos, runChangeCache);
                            } catch (final RuntimeException e) {
                                // Nothing above this callback would see the exception or complete the poll.
                                pollingOutput.fail(
                                        e,
                                        "Polling failed for adapter '" + adapterId
                                                + "' while publishing the values read");
                            }
                        }
                    });
        } else {
            // When no tags are present we keep the connection and just check it
            usable.lazyConnectionCheck();
            dataPointsPublisher.publish();
        }
    }

    @Override
    public @NotNull String getId() {
        return adapterId;
    }

    @Override
    public void start(
            final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
        this.eventService = input.moduleServices().eventService();
        changeCache = new PublishChangedDataOnlyHandler();
        // A new run warns again about a tag that still cannot be read.
        failingTags.clear();
        synchronized (lock) {
            stopped = false;
        }
        try {
            @SuppressWarnings("unused")
            final var unused = connect();
            output.startedSuccessfully();
        } catch (final Exception e) {
            output.failStart(e, null);
        }
    }

    /**
     * Returns the connection attempt in flight, or starts one when there is neither an attempt nor a connection.
     * <p>
     * The returned future completes when the attempt does: normally once the connection is established, exceptionally
     * when it failed, in which case no connection is left behind and the next call starts from scratch. With a
     * connection already in place it is complete at once. Safe to call repeatedly from the polling thread.
     *
     * @throws Plc4xException when no connection can be created from the configuration, so no attempt was started
     */
    private @NotNull CompletableFuture<Void> connect() throws Plc4xException {
        final EventService currentEventService = eventService;
        if (currentEventService == null) {
            return CompletableFuture.failedFuture(new Plc4xException("The adapter has not been started"));
        }
        synchronized (lock) {
            if (stopped) {
                return CompletableFuture.failedFuture(new Plc4xException("The adapter is stopped"));
            }
            final CompletableFuture<Void> inFlight = attempt.get();
            if (inFlight != null) {
                return inFlight;
            }
            if (connection != null) {
                return CompletableFuture.completedFuture(null);
            }
            final Plc4xConnection<T> previous = latestAttempt;
            if (previous != null && !previous.connectTerminated().isDone()) {
                // A timed-out connect is interrupted and closed, but must end before another starts, so that a
                // driver that ignores the interrupt holds at most one connect per adapter. The status says so before
                // start() returns: the adapter wrapper reports a start that left DISCONNECTED as CONNECTED.
                protocolAdapterState.setConnectionStatus(ERROR);
                return CompletableFuture.failedFuture(
                        new Plc4xException("The previous connection attempt is still ending"));
            }
            // we do not subscribe anymore as no current adapter type supports it anyway
            if (log.isTraceEnabled()) {
                log.trace("Creating new instance of Plc4x connector with {}.", adapterConfig);
            }
            final Plc4xConnection<T> tempConnection = createConnection();
            final CompletableFuture<Void> started = new CompletableFuture<>();
            attempt.set(started);
            latestAttempt = tempConnection;
            this.connection = tempConnection;
            // Published before the attempt runs and before start() returns, so the attempt's own outcome always comes
            // after it, and the adapter wrapper does not report a start that left DISCONNECTED as CONNECTED.
            protocolAdapterState.setConnectionStatus(CONNECTING);
            try {
                @SuppressWarnings("unused")
                final var unused = CompletableFuture.runAsync(
                                () -> {
                                    try {
                                        tempConnection.startConnection(
                                                currentEventService,
                                                adapterId,
                                                getProtocolAdapterInformation().getProtocolId());
                                        final boolean owned;
                                        synchronized (lock) {
                                            // Checked and published under the lock stop() takes, so a stop, or a stop
                                            // and a restart, cannot fall between the check and the status.
                                            owned = connection == tempConnection;
                                            if (owned) {
                                                protocolAdapterState.setConnectionStatus(CONNECTED);
                                            }
                                        }
                                        if (!owned) {
                                            // stop() cleared the connection while this attempt was connecting; whether
                                            // its disconnect ran before or after the connect, this connection is no
                                            // longer owned, so close it here rather than leave the driver's retry loop
                                            // running.
                                            disconnectQuietly(tempConnection);
                                            finish(started, new Plc4xException("The adapter stopped while connecting"));
                                            return;
                                        }
                                        finish(started, null);
                                    } catch (final Plc4xException e) {
                                        // Expected while the device is unreachable; the cause is in the message.
                                        log.warn(
                                                "Connection attempt for adapter '{}' failed: {}",
                                                adapterId,
                                                e.getMessage());
                                        abandon(started, tempConnection, e);
                                    }
                                },
                                Plc4xConnection.connectExecutor())
                        .whenComplete((_, t) -> {
                            if (t != null) {
                                log.error("Error starting PLC4X connection", t);
                                abandon(started, tempConnection, t);
                            }
                        });
            } catch (final RuntimeException e) {
                abandon(started, tempConnection, e);
            }
            return started;
        }
    }

    private void abandon(
            final @NotNull CompletableFuture<Void> started,
            final @NotNull Plc4xConnection<T> tempConnection,
            final @NotNull Throwable cause) {
        disconnectQuietly(tempConnection);
        synchronized (lock) {
            if (connection == tempConnection) {
                this.connection = null;
                protocolAdapterState.setConnectionStatus(ERROR);
            }
        }
        finish(started, cause);
    }

    /** Clears the attempt before completing it, so whatever runs on completion sees no attempt in flight. */
    private void finish(final @NotNull CompletableFuture<Void> started, final @Nullable Throwable cause) {
        attempt.compareAndSet(started, null);
        if (cause == null) {
            started.complete(null);
        } else {
            started.completeExceptionally(cause);
        }
    }

    /** Visible for testing: whether a connection attempt is currently in flight. */
    protected boolean isConnecting() {
        return attempt.get() != null;
    }

    private void disconnectQuietly(final @NotNull Plc4xConnection<T> toDisconnect) {
        try {
            toDisconnect.disconnect();
        } catch (final Exception e) {
            log.debug("Tried disconnecting after connection error and caught exception", e);
        }
    }

    @Override
    public void stop(final @NotNull ProtocolAdapterStopInput input, final @NotNull ProtocolAdapterStopOutput output) {
        final Plc4xConnection<T> tempConnection;
        // Replaced before the disconnect, whose failures of a read in flight and of a waiting connection attempt reach
        // polls of this run: those polls see their run ended and report nothing.
        changeCache = new PublishChangedDataOnlyHandler();
        synchronized (lock) {
            // From here no attempt starts, and one in flight closes its connection when it completes. A start after
            // this stop must not join that attempt: it would publish no status of its own.
            stopped = true;
            tempConnection = connection;
            connection = null;
            attempt.set(null);
        }
        if (tempConnection != null) {
            try {
                // -- Disconnect client; a connect still in flight is closed and interrupted, not waited for
                tempConnection.disconnect();
                protocolAdapterState.setConnectionStatus(DISCONNECTED);
            } catch (final Exception e) {
                protocolAdapterState.setErrorConnectionStatus(e, null);
                output.failStop(e, "Error disconnecting from PLC4X client");
                return;
            }
        }
        output.stoppedSuccessfully();
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    protected @NotNull Plc4xConnection<T> createConnection() throws Plc4xException {
        return new Plc4xConnection<>(
                driverManager,
                adapterConfig,
                plc4xAdapterConfig ->
                        Plc4xDataUtils.createQueryString(createQueryStringParams(plc4xAdapterConfig), true)) {
            @Override
            protected @NotNull String getProtocol() {
                return getProtocolHandler();
            }

            @Override
            protected @NotNull String createConnectionString(final @NotNull T config) {
                return super.createConnectionString(config);
            }

            @Override
            protected @NotNull String getTagAddressForSubscription(final @NotNull Plc4xTag tag) {
                return createTagAddressForSubscription(tag);
            }
        };
    }

    /**
     * The protocol Handler is the prefix of the JNDI Connection URI used to instantiate the connection from the factory
     *
     * @return the prefix to use, for example "opcua"
     */
    protected abstract @NotNull String getProtocolHandler();

    /**
     * Whether to use read or subscription types
     *
     * @return Decides on the mode of reading data from the underlying connection
     */
    protected abstract @NotNull ReadType getReadType();

    @Override
    public void createTagSchema(
            final @NotNull TagSchemaCreationInput input, final @NotNull TagSchemaCreationOutput output) {
        tags.stream()
                .filter(tag -> input.getTagName().equals(tag.getName()))
                .findFirst()
                .ifPresentOrElse(
                        tag -> {
                            final var dataType = tag.getDefinition().getDataType();
                            if (dataType == Plc4xDataType.DATA_TYPE.NULL) {
                                output.fail("Cannot create schema for NULL data type");
                                return;
                            }
                            final var builder = new SchemaBuilder();
                            applyDataTypeToBuilder(builder, dataType);
                            output.finish(new TagSchemaCreationOutput.DataPointSchema(
                                    builder.writable().readable().build(), null, null));
                        },
                        () -> output.fail("Unable to find tag definition for tag " + input.getTagName()
                                + ", cannot create schema"));
        BatchPollingProtocolAdapter.super.createTagSchema(input, output);
    }

    static void applyDataTypeToBuilder(
            final @NotNull SchemaBuilder builder, final @NotNull Plc4xDataType.DATA_TYPE dataType) {
        switch (dataType) {
            case BOOL -> builder.scalar(ScalarType.BOOLEAN);
            case SINT -> builder.scalar(ScalarType.LONG).minimum(-128L).maximum(127L);
            case BYTE, USINT -> builder.scalar(ScalarType.ULONG).minimum(0L).maximum(255L);
            case WORD, INT -> builder.scalar(ScalarType.LONG).minimum(-32_768L).maximum(32_767L);
            case UINT -> builder.scalar(ScalarType.ULONG).minimum(0L).maximum(65_535L);
            case DWORD, DINT ->
                builder.scalar(ScalarType.LONG).minimum(-2_147_483_648L).maximum(2_147_483_647L);
            case UDINT -> builder.scalar(ScalarType.ULONG).minimum(0L).maximum(4_294_967_295L);
            case LWORD, LINT ->
                builder.scalar(ScalarType.LONG).minimum(Long.MIN_VALUE).maximum(Long.MAX_VALUE);
            case ULINT -> builder.scalar(ScalarType.ULONG).minimum(0L);
            case REAL ->
                builder.scalar(ScalarType.DOUBLE).minimum(-3.4028235e38d).maximum(3.4028235e38d);
            case LREAL ->
                builder.scalar(ScalarType.DOUBLE)
                        .minimum(-1.7976931348623157e308d)
                        .maximum(1.7976931348623157e308d);
            case CHAR, WCHAR, STRING, WSTRING -> builder.scalar(ScalarType.STRING);
            case TIME, LTIME -> builder.scalar(ScalarType.DURATION);
            case DATE, LDATE -> builder.scalar(ScalarType.LOCAL_DATE);
            case TIME_OF_DAY, LTIME_OF_DAY -> builder.scalar(ScalarType.LOCAL_TIME);
            case DATE_AND_TIME, LDATE_AND_TIME, DATE_AND_LTIME -> builder.scalar(ScalarType.LOCAL_DATE_TIME);
            case RAW_BYTE_ARRAY -> builder.scalar(ScalarType.BINARY);
            case NULL ->
                throw new IllegalStateException(
                        "NULL must be handled by the caller before invoking applyDataTypeToBuilder");
        }
    }

    /**
     * Use this hook method to modify the query generated used to read|subscribe to the devices,
     * for the most part this is simply the tagAddress field unchanged from the subscription
     * <p>
     * Default: tagAddress:expectedDataType eg. "0%20:BOOL"
     */
    protected @NotNull String createTagAddressForSubscription(final @NotNull Plc4xTag tag) {
        return tag.getDefinition().getTagAddress()
                + TAG_ADDRESS_TYPE_SEP
                + tag.getDefinition().getDataType();
    }

    /**
     * Hook method used to populate the URL query parameters on the connection string from
     * the supplied configuration. For example;
     * ?remote-rack=0&remote-slot=3. Each value in the returned map will be encoded onto
     * the final connection string.
     */
    protected @NotNull Map<String, String> createQueryStringParams(final @NotNull T config) {
        return Collections.emptyMap();
    }

    /**
     * Hook method to format data from source tag
     */
    protected @Nullable Object convertTagValue(final @NotNull PlcValue plcValue) {
        return Plc4xDataUtils.convertObject(plcValue);
    }

    /**
     * Publishes the tags the device answered for and fails the poll only when it answered for none: one tag the
     * device cannot read does not hide the others' values, and a poll that read nothing counts as a polling error. In
     * change-only mode a value is recorded as published only when the publish returned before the sampler's timeout;
     * a later or failed publish may not have been taken, so the value is forgotten and published again next time.
     */
    private void publishResponse(
            final @NotNull PlcReadResponse response,
            final @NotNull BatchPollingOutput pollingOutput,
            final @NotNull DataPointListBuilder dataPointsPublisher,
            final long pollStartNanos,
            final @NotNull PublishChangedDataOnlyHandler runChangeCache) {
        final var mqttConfig = adapterConfig.getPlc4xToMqttConfig();
        final boolean publishChangedDataOnly = mqttConfig != null && mqttConfig.getPublishChangedDataOnly();
        final Object[] values = new Object[tags.size()];
        int tagsRead = 0;
        for (int i = 0; i < values.length; i++) {
            final String tagName = tags.get(i).getName();
            final PlcResponseCode responseCode = responseCodeOf(response, tagName);
            if (responseCode != PlcResponseCode.OK) {
                if (failingTags.add(tagName)) {
                    log.warn(
                            "Unable to read tag, the other tags are published [adapterId={}, tag={}, responseCode={}]",
                            adapterId,
                            tagName,
                            responseCode);
                }
                continue;
            }
            final PlcValue plcValue = response.getPlcValue(tagName);
            final Object value = plcValue != null ? convertedOrNull(plcValue, tagName) : null;
            if (value == null) {
                // Answered, but with no value that can be published: for the poll the tag was not read.
                if (failingTags.add(tagName)) {
                    log.warn(
                            "Unable to convert the value of tag, the other tags are published [adapterId={}, tag={}]",
                            adapterId,
                            tagName);
                }
                continue;
            }
            if (!failingTags.isEmpty() && failingTags.remove(tagName)) {
                log.info("Tag is read again [adapterId={}, tag={}]", adapterId, tagName);
            }
            tagsRead++;
            values[i] = value;
        }
        if (tagsRead == 0) {
            pollingOutput.fail("Polling failed for adapter '" + adapterId + "' because no tag could be read [tags="
                    + tags.size() + "]");
            return;
        }
        if (protocolAdapterState.getConnectionStatus() == ERROR) {
            // Error was transient
            protocolAdapterState.setConnectionStatus(CONNECTED);
        }
        for (int i = 0; i < values.length; i++) {
            final Object value = values[i];
            if (value != null
                    && (!publishChangedDataOnly
                            || runChangeCache.isNew(tags.get(i).getName(), value))) {
                addDataPoint(dataPointsPublisher, tags.get(i), value);
            }
        }
        boolean delivered = false;
        try {
            dataPointsPublisher.publish();
            delivered = System.nanoTime() - pollStartNanos < samplerTimeoutNanos;
        } finally {
            if (publishChangedDataOnly) {
                for (int i = 0; i < values.length; i++) {
                    final Object value = values[i];
                    if (value == null) {
                        continue;
                    }
                    if (delivered) {
                        runChangeCache.replaceIfValueIsNew(tags.get(i).getName(), value);
                    } else {
                        runChangeCache.forgetIfChanged(tags.get(i).getName(), value);
                    }
                }
            }
        }
    }

    /**
     * The tag's value as published, or {@code null} when it cannot be converted. PLC4X 0.13.1 throws for an array or
     * a struct value (an ADS symbol of that type), which would otherwise fail the poll and hide the other tags.
     */
    private @Nullable Object convertedOrNull(final @NotNull PlcValue plcValue, final @NotNull String tagName) {
        try {
            return convertTagValue(plcValue);
        } catch (final RuntimeException e) {
            log.debug("Unable to convert the value of tag [adapterId={}, tag={}]", adapterId, tagName, e);
            return null;
        }
    }

    /** The device's answer for the tag, or {@code null} when the response holds none for it. */
    private static @Nullable PlcResponseCode responseCodeOf(
            final @NotNull PlcReadResponse response, final @NotNull String tagName) {
        try {
            return response.getResponseCode(tagName);
        } catch (final PlcInvalidTagException _) {
            return null;
        }
    }

    private static void addDataPoint(
            final @NotNull DataPointListBuilder dataPointsPublisher,
            final @NotNull Plc4xTag tag,
            final @NotNull Object value) {
        final var builder = dataPointsPublisher.addDataPoint(tag);
        switch (value) {
            case final Boolean val -> builder.value(val);
            case final Short val -> builder.value(val);
            case final Integer val -> builder.value(val);
            case final Long val -> builder.value(val);
            case final Float val -> builder.value(val);
            case final Double val -> builder.value(val);
            case final BigInteger val -> builder.value(val);
            case final String val -> builder.value(val);
            case final byte[] val -> builder.value(val);
            default -> builder.value(value.toString());
        }
    }

    /**
     * Whether the run a poll started in has ended: {@code stop()} or {@code start()} has replaced the run's change
     * cache since. The check is not atomic with the report that follows it: a failure found while the run was on
     * still reaches its sampler if the stop and the next start both complete in between. Holding a lock across the
     * report would make {@code stop()} wait on the sampler's reaction; disowning a completion after its polling task
     * was stopped belongs to Edge's polling task.
     */
    private boolean hasEnded(final @NotNull PublishChangedDataOnlyHandler runChangeCache) {
        return changeCache != runChangeCache;
    }

    /**
     * Ends a poll of an ended run without data and without an error. A failure would reach the sampler of the ended
     * run, which reports to the adapter the next run shares: it sets the adapter's error status and, past the
     * polling-error limit, stops it. Nothing was read for the run, so nothing is published or recorded.
     */
    private static void retire(final @NotNull DataPointListBuilder emptyPublisher) {
        emptyPublisher.publish();
    }

    @Override
    public int getPollingIntervalMillis() {
        final var cfg = adapterConfig.getPlc4xToMqttConfig();
        return cfg != null ? cfg.getPollingIntervalMillis() : 1000;
    }

    @Override
    public int getMaxPollingErrorsBeforeRemoval() {
        final var cfg = adapterConfig.getPlc4xToMqttConfig();
        return cfg != null ? cfg.getMaxPollingErrorsBeforeRemoval() : 5;
    }

    public enum ReadType {
        Read,
        Subscribe
    }

    /** A read and its connection; the read completes when the driver answers or the connection closes. */
    private record InFlightRead(
            @NotNull Plc4xConnection<?> connection, @NotNull CompletableFuture<?> read) {}
}
