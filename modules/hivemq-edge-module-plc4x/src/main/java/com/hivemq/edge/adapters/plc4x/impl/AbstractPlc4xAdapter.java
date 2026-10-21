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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.plc4x.java.api.PlcDriverManager;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.apache.plc4x.java.api.types.PlcResponseCode;
import org.apache.plc4x.java.api.value.PlcValue;
import org.apache.plc4x.java.spi.messages.DefaultPlcReadResponse;
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

    protected final @NotNull T adapterConfig;
    protected final @NotNull List<Plc4xTag> tags;
    private final @NotNull Logger log;
    private final @NotNull Object lock;
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull String adapterId;
    private final @NotNull PublishChangedDataOnlyHandler lastSamples;

    private final @NotNull AtomicReference<CompletableFuture<Void>> attempt = new AtomicReference<>();
    // Guarded by lock: set by stop(), cleared by start(); a stopped adapter starts no connection attempt.
    private boolean stopped;
    // Guarded by lock: the connection of the latest attempt, whose connect may still be ending after a timeout.
    private @Nullable Plc4xConnection<T> latestAttempt;

    protected volatile @Nullable Plc4xConnection<T> connection;
    private volatile @Nullable EventService eventService;

    public AbstractPlc4xAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation, final ProtocolAdapterInput<T> input) {
        this.adapterId = input.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.tags = input.getTags().stream().map(tag -> (Plc4xTag) tag).toList();
        this.log = LoggerFactory.getLogger(getClass());
        this.lock = new Object();
        this.lastSamples = new PublishChangedDataOnlyHandler();
    }

    public static @NotNull String nullSafe(final @Nullable Object o) {
        return Objects.toString(o);
    }

    @Override
    public void poll(final @NotNull BatchPollingInput pollingInput, final @NotNull BatchPollingOutput pollingOutput) {
        // Read the attempt before the connection: an attempt sets the connection before it is cleared, so with no
        // attempt in flight the connection read below is the last attempt's final outcome.
        final CompletableFuture<Void> inFlight = attempt.get();
        if (inFlight != null) {
            completeAfter(inFlight, pollingOutput);
            return;
        }
        final Plc4xConnection<T> tempConnection = connection;
        if (tempConnection != null && tempConnection.isConnected()) {
            read(tempConnection, pollingOutput);
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
        completeAfter(started, pollingOutput);
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
            final @NotNull CompletableFuture<Void> connectionAttempt, final @NotNull BatchPollingOutput pollingOutput) {
        @SuppressWarnings("unused")
        final var unused = connectionAttempt.whenComplete((ignored, t) -> {
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
            read(established, pollingOutput);
        });
    }

    /** Reads the tags into this poll's output; every outcome, a synchronous failure included, completes it. */
    private void read(final @NotNull Plc4xConnection<T> usable, final @NotNull BatchPollingOutput pollingOutput) {
        try {
            readTags(usable, pollingOutput);
        } catch (final RuntimeException e) {
            pollingOutput.fail(e, "Polling failed for adapter '" + adapterId + "' while reading from the device");
        }
    }

    private void readTags(final @NotNull Plc4xConnection<T> usable, final @NotNull BatchPollingOutput pollingOutput) {
        final var dataPointsPublisher = pollingOutput.dataPointListPublisher();
        if (!tags.isEmpty()) {
            @SuppressWarnings("unused")
            final var unused = usable.read(tags).whenComplete((response, t) -> {
                if (t instanceof Plc4xException) {
                    // Handled, as when the adapter stops during a read: one line with the cause, no stack trace.
                    pollingOutput.fail("Polling failed for adapter '" + adapterId + "' while reading from the device: "
                            + t.getMessage());
                } else if (t != null) {
                    pollingOutput.fail(t, null);
                } else {
                    final var mqttConfig = adapterConfig.getPlc4xToMqttConfig();
                    final boolean publishChangedDataOnly = mqttConfig != null && mqttConfig.getPublishChangedDataOnly();
                    processReadResponse(tags, response, dataPointsPublisher, publishChangedDataOnly);
                    dataPointsPublisher.publish();
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
                        .whenComplete((ignored, t) -> {
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

    protected void processReadResponse(
            final @NotNull List<Plc4xTag> tags,
            final @NotNull PlcReadResponse readEvent,
            final @NotNull DataPointListBuilder dataPointsPublisher,
            final boolean publishChangedDataOnly) {
        // it is possible that the read response does not contain any values at all, leading to unexpected error states
        if (readEvent instanceof final DefaultPlcReadResponse event) {
            if (tags.stream().allMatch(tag -> event.getResponseCode(tag.getName()) == PlcResponseCode.OK)) {
                if (protocolAdapterState.getConnectionStatus() == ProtocolAdapterState.ConnectionStatus.ERROR) {
                    // Error was transient
                    protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
                }
                final Map<String, Plc4xTag> tagsByName =
                        tags.stream().collect(Collectors.toMap(Plc4xTag::getName, Function.identity()));
                processPlcFieldData(
                        Plc4xDataUtils.readDataFromReadResponse(event),
                        dataPointsPublisher,
                        tagsByName,
                        publishChangedDataOnly);
            } else {
                tags.stream()
                        .filter(tag -> event.getResponseCode(tag.getName()) != PlcResponseCode.OK)
                        .forEach(tag -> log.error(
                                "Unable to read tag {}. Error Code: {}",
                                tag.getName(),
                                event.getResponseCode(tag.getName())));
            }
        }
    }

    protected void processPlcFieldData(
            final @NotNull List<Pair<String, PlcValue>> l,
            final @NotNull DataPointListBuilder dataPointsPublisher,
            final @NotNull Map<String, Plc4xTag> tagsByName,
            final boolean publishChangedDataOnly) {
        for (final Pair<String, PlcValue> pair : l) {
            final Object value = convertTagValue(pair.getValue());
            if (value != null) {
                final var tag = tagsByName.get(pair.getLeft());
                if (tag == null) {
                    log.error("No tag found for name '{}', skipping", pair.getLeft());
                    continue;
                }
                if (!publishChangedDataOnly || lastSamples.replaceIfValueIsNew(tag.getName(), value)) {
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
            }
        }
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
}
