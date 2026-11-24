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
package com.hivemq.edge.adapters.modbus;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.DISCONNECTED;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.ERROR;
import static com.hivemq.edge.adapters.modbus.config.ModbusToMqttConfig.DEFAULT_MAX_POLL_ERRORS_BEFORE_REMOVAL;
import static com.hivemq.edge.adapters.modbus.config.ModbusToMqttConfig.DEFAULT_POLL_INTERVAL_MILLIS;

import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.datapoint.DataPointListBuilder;
import com.hivemq.adapter.sdk.api.discovery.NodeTree;
import com.hivemq.adapter.sdk.api.discovery.NodeType;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryInput;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryOutput;
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
import com.hivemq.edge.adapters.modbus.config.ModbusSpecificAdapterConfig;
import com.hivemq.edge.adapters.modbus.config.tag.ModbusTag;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModbusProtocolAdapter implements BatchPollingProtocolAdapter {
    private static final Logger log = LoggerFactory.getLogger(ModbusProtocolAdapter.class);

    private static final @NotNull String NODE_ID_COILS = "coils";
    private static final @NotNull String NODE_ID_HOLDING_REGISTERS = "holding-registers";
    private static final int ADDRESS_START_IDX = 1;
    private static final int ADDRESS_GROUP_IDX = 16;
    private static final int ADDRESS_GROUP_MAX = 256;
    // The sampler times a poll out 10 s after poll() returns (AbstractSubscriptionSampler.POLL_OUTPUT_TIMEOUT_MILLIS
    // in Edge, which a test holds this to): a publish within this time of poll() is handed on.
    static final long SAMPLER_TIMEOUT_MILLIS = 10_000;

    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ModbusSpecificAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull ModbusClient client;
    // One change cache per run, which also tells a poll whether its run is still on: replaced by stop() before it
    // disconnects and by start(), both on the lifecycle thread; read by poll() on the polling thread and by the poll's
    // reconnect and read callbacks on the client's threads. A poll keeps the cache of the run it started in, so a poll
    // still completing after a stop cannot write into the next run's cache, and sees its run ended once the field
    // holds another cache.
    private volatile @NotNull PublishChangedDataOnlyHandler changeCache = new PublishChangedDataOnlyHandler();
    private final @NotNull List<ModbusTag> tags;
    // Tags a poll could not read while it read others, logged once each until read again; written by the poll's read
    // callback on the client's threads.
    private final @NotNull Set<String> unreadableTags = ConcurrentHashMap.newKeySet();
    private final @NotNull String adapterId;
    private final @NotNull AtomicBoolean stopRequested;
    private final @NotNull AtomicBoolean startRequested;
    private final long samplerTimeoutNanos;
    // A poll that waited on the reconnect reads only this soon after poll(): its read is answered or fails before the
    // sampler times the poll out, so a value the device clears on read is not read into a poll nobody takes.
    private final long lateReadDeadlineNanos;

    public ModbusProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<ModbusSpecificAdapterConfig> input) {
        this(adapterInformation, input, SAMPLER_TIMEOUT_MILLIS);
    }

    /**
     * {@code samplerTimeoutMillis} is how long after {@code poll} is called the sampler still takes its output; a
     * publish later than that is not recorded in the change cache.
     */
    ModbusProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<ModbusSpecificAdapterConfig> input,
            final long samplerTimeoutMillis) {
        this.samplerTimeoutNanos = TimeUnit.MILLISECONDS.toNanos(samplerTimeoutMillis);
        this.lateReadDeadlineNanos = samplerTimeoutNanos - ModbusClient.REQUEST_TIMEOUT.toNanos();
        this.adapterId = input.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.tags = input.getTags().stream().map(t -> (ModbusTag) t).toList();
        this.client = new ModbusClient(input.getConfig());
        this.stopRequested = new AtomicBoolean(false);
        this.startRequested = new AtomicBoolean(false);
    }

    private static void addAddresses(final @NotNull NodeTree tree, final @NotNull String parent) {
        tree.addNode(
                "grouping-" + ADDRESS_START_IDX,
                "Addresses " + ADDRESS_START_IDX + "-" + (ADDRESS_START_IDX + ADDRESS_GROUP_IDX - 1),
                "",
                "",
                parent,
                NodeType.FOLDER,
                false);
        String parentNode = "grouping-" + ADDRESS_START_IDX;
        for (int i = ADDRESS_START_IDX; i <= ADDRESS_GROUP_MAX; i++) {
            tree.addNode(
                    "address-location-" + i,
                    String.valueOf(i),
                    String.valueOf(i),
                    "",
                    parentNode,
                    NodeType.VALUE,
                    true);
            if (i % ADDRESS_GROUP_IDX == 0 && i < ADDRESS_GROUP_MAX) {
                tree.addNode(
                        "grouping-" + i,
                        "Addresses " + (i + 1) + "-" + (i + ADDRESS_GROUP_IDX),
                        "",
                        "",
                        parent,
                        NodeType.FOLDER,
                        false);
                parentNode = "grouping-" + i;
            }
        }
    }

    @Override
    public void start(
            final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
        if (!stopRequested.get() && startRequested.compareAndSet(false, true)) {
            changeCache = new PublishChangedDataOnlyHandler();
            // A new run warns again about a tag that still cannot be read.
            unreadableTags.clear();
            // The client connects persistently: when this first attempt fails, it keeps reconnecting with backoff
            // until stop() disconnects it, so an unreachable device does not fail the start.
            client.connect().whenComplete((_, throwable) -> {
                if (throwable == null) {
                    protocolAdapterState.setConnectionStatus(CONNECTED);
                    log.info("Successfully started Modbus protocol adapter {}", adapterId);
                } else {
                    // The connect stage is a dependent one, so the failure arrives wrapped; the cause (refused, timed
                    // out, unknown host) becomes the adapter's last error message.
                    final Throwable cause =
                            throwable instanceof final CompletionException wrapped && wrapped.getCause() != null
                                    ? wrapped.getCause()
                                    : throwable;
                    protocolAdapterState.setErrorConnectionStatus(cause, null);
                    log.warn(
                            "Started Modbus protocol adapter {} without a connection, device not reachable yet: {}",
                            adapterId,
                            cause.getMessage());
                }
                output.startedSuccessfully();
            });
        }
    }

    @Override
    public void stop(final @NotNull ProtocolAdapterStopInput input, final @NotNull ProtocolAdapterStopOutput output) {
        if (startRequested.get() && stopRequested.compareAndSet(false, true)) {
            log.info("Stopping Modbus protocol adapter {}", adapterId);
            // Replaced before the disconnect, whose failures of a waiting reconnect or a read in flight reach polls of
            // this run: those polls see their run ended and report nothing.
            changeCache = new PublishChangedDataOnlyHandler();
            try {
                client.disconnect()
                        .whenComplete((_, throwable) -> {
                            try {
                                if (throwable == null) {
                                    protocolAdapterState.setConnectionStatus(DISCONNECTED);
                                    output.stoppedSuccessfully();
                                    log.info("Successfully stopped Modbus protocol adapter {}", adapterId);
                                } else {
                                    protocolAdapterState.setConnectionStatus(ERROR);
                                    output.failStop(
                                            throwable, "Error encountered closing connection to Modbus server.");
                                    log.error("Unable to stop the connection to the Modbus server", throwable);
                                }
                            } finally {
                                startRequested.set(false);
                                stopRequested.set(false);
                            }
                        })
                        .toCompletableFuture()
                        .get();
            } catch (final InterruptedException | ExecutionException e) {
                log.error("Unable to stop the connection to the Modbus server", e);
                output.failStop(e, "Error encountered closing connection to Modbus server.");
            }
        } else {
            output.stoppedSuccessfully();
        }
    }

    private boolean isPublishAllChanges() {
        final var toMqttConfig = adapterConfig.getModbusToMQTTConfig();
        return toMqttConfig == null || !toMqttConfig.getPublishChangedDataOnly();
    }

    @Override
    public void poll(final @NotNull BatchPollingInput pollingInput, final @NotNull BatchPollingOutput pollingOutput) {
        final long pollStartNanos = System.nanoTime();
        final PublishChangedDataOnlyHandler runChangeCache = changeCache;
        if (startRequested.get() && !stopRequested.get()) {
            if (client.isConnected()) {
                readTags(pollingOutput, pollStartNanos, runChangeCache);
                return;
            }
            final CompletableFuture<Void> reconnect = client.reconnectAfterDrop();
            if (reconnect != null) {
                // An established connection dropped and the client reconnects within seconds: the poll waits for that
                // attempt, so a brief drop costs no polling error beyond the poll it broke. The sampler's timeout
                // bounds the wait.
                @SuppressWarnings("unused")
                final var unused = reconnect.whenComplete((_, throwable) -> {
                    if (hasEnded(runChangeCache)) {
                        retire(pollingOutput.dataPointListPublisher());
                        return;
                    }
                    if (throwable != null) {
                        pollingOutput.fail(notConnectedMessage(throwable));
                        return;
                    }
                    if (System.nanoTime() - pollStartNanos >= lateReadDeadlineNanos) {
                        // A read now could still be under way when the sampler takes the poll's output no longer,
                        // and a value the device clears on read would be lost; the next poll reads instead. This
                        // poll is not carried out: Edge leaves the polling-error count as it is, so the first
                        // reconnect costs no error, and a device whose every read fails still reaches its limit.
                        log.debug(
                                "Reconnected too late to read within this poll, the next poll reads [adapterId={},"
                                        + " host={}, port={}]",
                                adapterId,
                                adapterConfig.getHost(),
                                adapterConfig.getPort());
                        pollingOutput.fail(
                                new CancellationException("Reconnected too late to read within this poll"), null);
                        return;
                    }
                    // The read decides the poll, so polling errors keep counting until a read succeeds.
                    try {
                        readTags(pollingOutput, pollStartNanos, runChangeCache);
                    } catch (final RuntimeException e) {
                        // The poll's caller catches what a direct read throws; here no caller would.
                        pollingOutput.fail(asPollingError(e), "Unable to read tags from modbus");
                    }
                });
                return;
            }
            // The reconnect may have succeeded between the two reads above: the client's state machine sets the state
            // and takes the future under one lock, so a second read sees the connection.
            if (client.isConnected()) {
                readTags(pollingOutput, pollStartNanos, runChangeCache);
                return;
            }
        }
        // Fail at once so the polling backoff applies; an unanswered poll would fail only at the sampler's timeout.
        // The message carries the last connect failure, which tells a refusal from a timeout or an unknown host.
        pollingOutput.fail(notConnectedMessage(client.lastConnectFailure()));
    }

    private void readTags(
            final @NotNull BatchPollingOutput pollingOutput,
            final long pollStartNanos,
            final @NotNull PublishChangedDataOnlyHandler runChangeCache) {
        final var dataPointsPublisher = pollingOutput.dataPointListPublisher();

        final int limit = tags.size();
        final CompletableFuture<ResulTuple>[] readRegisterFutures = new CompletableFuture[limit];
        for (int i = 0; i < limit; i++) {
            final ModbusTag tag = tags.get(i);
            readRegisterFutures[i] = client.readRegisters(tag)
                    .thenApply(result -> new ResulTuple(tag, result))
                    .toCompletableFuture();
        }

        final boolean publishAllChanges = isPublishAllChanges();

        @SuppressWarnings("unused")
        final var unused = CompletableFuture.allOf(readRegisterFutures).whenComplete((_, _) -> {
            try {
                if (hasEnded(runChangeCache)) {
                    retire(dataPointsPublisher);
                    return;
                }
                // A tag that fails does not cost the others their values, which a device may clear on read: the poll
                // publishes every tag that was read and fails only when none was. Every read has completed here.
                Throwable firstFailure = null;
                int readCount = 0;
                for (final CompletableFuture<ResulTuple> readRegisterFuture : readRegisterFutures) {
                    if (!readRegisterFuture.isCompletedExceptionally()) {
                        readCount++;
                    } else if (firstFailure == null) {
                        firstFailure = readRegisterFuture.exceptionNow();
                    }
                }
                if (firstFailure != null && readCount == 0) {
                    protocolAdapterState.setConnectionStatus(ERROR);
                    pollingOutput.fail(asPollingError(firstFailure), "Unable to read tags from modbus");
                    return;
                }

                protocolAdapterState.setConnectionStatus(CONNECTED);

                for (int i = 0; i < limit; i++) {
                    final CompletableFuture<ResulTuple> readRegisterFuture = readRegisterFutures[i];
                    if (readRegisterFuture.isCompletedExceptionally()) {
                        reportUnreadable(tags.get(i), readRegisterFuture.exceptionNow());
                        continue;
                    }
                    final ResulTuple entry = readRegisterFuture.resultNow();
                    final var tag = entry.tag();
                    reportReadable(tag);
                    final var value = entry.value();
                    if (publishAllChanges || runChangeCache.isNew(tag.getName(), value)) {
                        final var dataPointBuilder = dataPointsPublisher.addDataPoint(tag);
                        switch (value) {
                            case final Long val -> dataPointBuilder.value(val);
                            case final Integer val -> dataPointBuilder.value(val);
                            case final Float val -> dataPointBuilder.value(val);
                            case final Double val -> dataPointBuilder.value(val);
                            case final Short val -> dataPointBuilder.value(val);
                            case final Boolean val -> dataPointBuilder.value(val);
                            case final String val -> dataPointBuilder.value(val);
                            default ->
                                log.error(
                                        "Unsupported data type {} for tag {}, skipping",
                                        value.getClass(),
                                        tag.getName());
                        }
                    }
                }
                // The change cache holds what the consumer was given. A publish that returns within the sampler's
                // timeout after poll() reached the consumer: it completes the poll's output at once, and the sampler
                // cannot time the output out earlier. Past that, or when the publish throws, whether the consumer got
                // the new values is unknown (a slow consumer, or a dropped poll), so their entries are forgotten and
                // the next poll publishes the current value: at worst a repeat, never a lost change.
                boolean delivered = false;
                try {
                    dataPointsPublisher.publish();
                    delivered = System.nanoTime() - pollStartNanos < samplerTimeoutNanos;
                } finally {
                    if (!publishAllChanges) {
                        for (final CompletableFuture<ResulTuple> readRegisterFuture : readRegisterFutures) {
                            if (readRegisterFuture.isCompletedExceptionally()) {
                                continue;
                            }
                            final ResulTuple entry = readRegisterFuture.resultNow();
                            if (delivered) {
                                runChangeCache.replaceIfValueIsNew(entry.tag().getName(), entry.value());
                            } else {
                                runChangeCache.forgetIfChanged(entry.tag().getName(), entry.value());
                            }
                        }
                    }
                }
            } catch (final Exception e) {
                pollingOutput.fail(asPollingError(e), "Unable to read tags from modbus");
            }
        });
    }

    /**
     * A read that failed is a polling error. A cancellation passed on as it is would tell Edge the poll was not
     * carried out, which leaves the error count as it is: a read the client cancelled is wrapped, so it counts.
     */
    private static @NotNull Throwable asPollingError(final @NotNull Throwable failure) {
        return failure instanceof CancellationException
                ? new IllegalStateException("The read was cancelled", failure)
                : failure;
    }

    /** Logs a tag that a poll could not read while it read others, once until the tag is read again. */
    private void reportUnreadable(final @NotNull ModbusTag tag, final @NotNull Throwable failure) {
        if (unreadableTags.add(tag.getName())) {
            log.warn(
                    "Modbus adapter {} could not read tag {}, the other tags are published: {}",
                    adapterId,
                    tag.getName(),
                    failure.getMessage());
        }
    }

    /** Logs a tag that is read again after {@link #reportUnreadable} logged it. */
    private void reportReadable(final @NotNull ModbusTag tag) {
        if (!unreadableTags.isEmpty() && unreadableTags.remove(tag.getName())) {
            log.info("Modbus adapter {} reads tag {} again", adapterId, tag.getName());
        }
    }

    /**
     * Whether the run a poll started in has ended: {@code stop()} or {@code start()} has replaced the run's change
     * cache since. The check is not atomic with the report that follows it: a failure found while the run was on
     * still reaches its sampler if the stop and the next start both complete in between. Holding a lock across the
     * report would make {@code stop()} wait on the sampler's reaction; disowning a completion after its polling
     * task was stopped belongs to Edge's polling task.
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

    private @NotNull String notConnectedMessage(final @Nullable Throwable connectFailure) {
        final String device = "The Modbus device is not connected [host=" + adapterConfig.getHost() + ", port="
                + adapterConfig.getPort();
        if (connectFailure == null) {
            return device + "]";
        }
        final String cause = connectFailure.getMessage() != null
                ? connectFailure.getMessage()
                : connectFailure.getClass().getSimpleName();
        return device + ", cause=" + cause + "]";
    }

    @Override
    public int getPollingIntervalMillis() {
        final var toMqttConfig = adapterConfig.getModbusToMQTTConfig();
        return toMqttConfig != null ? toMqttConfig.getPollingIntervalMillis() : DEFAULT_POLL_INTERVAL_MILLIS;
    }

    @Override
    public int getMaxPollingErrorsBeforeRemoval() {
        final var toMqttConfig = adapterConfig.getModbusToMQTTConfig();
        return toMqttConfig != null
                ? toMqttConfig.getMaxPollingErrorsBeforeRemoval()
                : DEFAULT_MAX_POLL_ERRORS_BEFORE_REMOVAL;
    }

    @Override
    public @NotNull String getId() {
        return adapterId;
    }

    @Override
    public void discoverValues(
            final @NotNull ProtocolAdapterDiscoveryInput input, final @NotNull ProtocolAdapterDiscoveryOutput output) {
        // -- Do the discovery of registers and coils, only for root level
        // -- Do the discovery of registers and coils, only for root level
        final NodeTree nodeTree = output.getNodeTree();
        if (input.getRootNode() == null) {
            nodeTree.addNode(
                    NODE_ID_HOLDING_REGISTERS,
                    "Holding Registers",
                    "Holding Registers",
                    "Holding Registers",
                    null,
                    NodeType.FOLDER,
                    false);
            nodeTree.addNode(NODE_ID_COILS, "Coils", "Coils", NODE_ID_COILS, null, NodeType.FOLDER, false);
        }
        addAddresses(nodeTree, NODE_ID_HOLDING_REGISTERS);
        addAddresses(nodeTree, NODE_ID_COILS);
        output.finish();
    }

    @Override
    public void createTagSchema(
            final @NotNull TagSchemaCreationInput input, final @NotNull TagSchemaCreationOutput output) {
        tags.stream()
                .filter(tag -> input.getTagName().equals(tag.getName()))
                .findFirst()
                .map(tag -> {
                    final var builder = new SchemaBuilder();
                    switch (tag.getDefinition().getDataType()) {
                        case BOOL ->
                            builder.scalar(ScalarType.BOOLEAN).writable().readable();
                        case INT_16 ->
                            builder.scalar(ScalarType.LONG)
                                    .minimum(-32_768L) // Modbus INT_16: signed 16-bit integer lower bound
                                    .maximum(32_767L) // Modbus INT_16: signed 16-bit integer upper bound
                                    .writable()
                                    .readable();
                        case UINT_16 ->
                            builder.scalar(ScalarType.ULONG)
                                    .minimum(0L) // Modbus UINT_16: unsigned 16-bit integer lower bound
                                    .maximum(65_535L) // Modbus UINT_16: unsigned 16-bit integer upper bound
                                    .writable()
                                    .readable();
                        case INT_32 ->
                            builder.scalar(ScalarType.LONG)
                                    .minimum(-2_147_483_648L) // Modbus INT_32: signed 32-bit integer lower bound
                                    .maximum(2_147_483_647L) // Modbus INT_32: signed 32-bit integer upper bound
                                    .writable()
                                    .readable();
                        case INT_64 ->
                            builder.scalar(ScalarType.LONG)
                                    .minimum(-9_223_372_036_854_775_808L) // Modbus INT_64: signed 64-bit integer lower
                                    // bound
                                    .maximum(9_223_372_036_854_775_807L) // Modbus INT_64: signed 64-bit integer upper
                                    // bound
                                    .writable()
                                    .readable();
                        case UINT_32 ->
                            builder.scalar(ScalarType.ULONG)
                                    .minimum(0L) // Modbus UINT_32: unsigned 32-bit integer lower bound
                                    .maximum(4_294_967_295L) // Modbus UINT_32: unsigned 32-bit integer upper bound
                                    .writable()
                                    .readable();
                        case FLOAT_32 ->
                            builder.scalar(ScalarType.DOUBLE)
                                    .minimum(-3.4028235e38d) // Modbus FLOAT_32: IEEE 754 single precision lower bound
                                    .maximum(3.4028235e38d) // Modbus FLOAT_32: IEEE 754 single precision upper bound
                                    .writable()
                                    .readable();
                        case FLOAT_64 ->
                            builder.scalar(ScalarType.DOUBLE)
                                    .minimum(-1.7976931348623157e308d) // Modbus FLOAT_64: IEEE 754 double precision
                                    // lower bound
                                    .maximum(
                                            1.7976931348623157e308d) // Modbus FLOAT_64: IEEE 754 double precision upper
                                    // bound
                                    .writable()
                                    .readable();
                        case UTF_8 ->
                            builder.scalar(ScalarType.STRING).writable().readable();
                    }
                    return builder.build();
                })
                .ifPresentOrElse(
                        schema -> output.finish(new TagSchemaCreationOutput.DataPointSchema(schema, null, null)),
                        () -> output.fail("Unable to find tag definition for tag " + input.getTagName()
                                + ", cannot create schema"));
        BatchPollingProtocolAdapter.super.createTagSchema(input, output);
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    private record ResulTuple(ModbusTag tag, Object value) {}
}
