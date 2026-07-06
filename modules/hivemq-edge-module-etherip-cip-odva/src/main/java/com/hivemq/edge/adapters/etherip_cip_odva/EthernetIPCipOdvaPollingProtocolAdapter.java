/*
 * Copyright 2023-present HiveMQ GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.hivemq.edge.adapters.etherip_cip_odva;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.DISCONNECTED;
import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Stopwatch;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.datapoint.DataPointBuilder;
import com.hivemq.adapter.sdk.api.datapoint.DataPointListBuilder;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingInput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.schema.SchemaBuilder;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationInput;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationOutput;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.writing.WritingInput;
import com.hivemq.adapter.sdk.api.writing.WritingOutput;
import com.hivemq.adapter.sdk.api.writing.WritingPayload;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.edge.adapters.etherip_cip_odva.composite.CompositeValues;
import com.hivemq.edge.adapters.etherip_cip_odva.composite.CompositeValuesFactory;
import com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType;
import com.hivemq.edge.adapters.etherip_cip_odva.config.CipNumericRange;
import com.hivemq.edge.adapters.etherip_cip_odva.config.CipReadWrite;
import com.hivemq.edge.adapters.etherip_cip_odva.config.CipWriteMode;
import com.hivemq.edge.adapters.etherip_cip_odva.config.EipSpecificAdapterConfig;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTag;
import com.hivemq.edge.adapters.etherip_cip_odva.decoder.CipTagDecoders;
import com.hivemq.edge.adapters.etherip_cip_odva.encoder.CipTagEncoders;
import com.hivemq.edge.adapters.etherip_cip_odva.exception.ExceptionProcessor;
import com.hivemq.edge.adapters.etherip_cip_odva.exception.OdvaDecodeException;
import com.hivemq.edge.adapters.etherip_cip_odva.exception.OdvaException;
import com.hivemq.edge.adapters.etherip_cip_odva.handler.CipTagDecodingAttributeProtocol;
import com.hivemq.edge.adapters.etherip_cip_odva.handler.CipTagEncodingAttributeProtocol;
import com.hivemq.edge.adapters.etherip_cip_odva.handler.CipTagValueProducer;
import com.hivemq.edge.adapters.etherip_cip_odva.handler.RawAttributeReadProtocol;
import com.hivemq.edge.adapters.etherip_cip_odva.hysteresis.Hysteresis;
import com.hivemq.edge.adapters.etherip_cip_odva.tag.TagGroup;
import com.hivemq.edge.adapters.etherip_cip_odva.tag.TagGroups;
import com.hivemq.edge.adapters.etherip_cip_odva.tag.TagSchemaMapper;
import com.hivemq.edge.adapters.etherip_cip_odva.util.ExceptionUtils;
import etherip.EtherNetIP;
import etherip.EthernetIPWithODVA;
import etherip.data.CipException;
import etherip.protocol.Encapsulation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.commons.lang3.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.LoggerFactory;

public class EthernetIPCipOdvaPollingProtocolAdapter implements BatchPollingProtocolAdapter, WritingProtocolAdapter {

    private static final @NotNull org.slf4j.Logger LOG =
            LoggerFactory.getLogger(EthernetIPCipOdvaPollingProtocolAdapter.class);

    private static final String[] DISCONNECT_REASONS = new String[] {
        "Connection reset by peer", "Broken pipe", "Timeout", Encapsulation.Command.UnRegisterSession.name()
    };

    private final @NotNull EipSpecificAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    protected final @NotNull AdapterFactories adapterFactories;
    private final @NotNull String adapterId;
    private final AtomicReference<EthernetIPWithODVA> etherNetIP = new AtomicReference<>();
    private final @NotNull AtomicReference<DataPointStore> lastSamples = new AtomicReference<>(new DataPointStore());
    private final @NotNull StatsTracker statsTracker;
    private final @NotNull CipTagDecoders cipTagsDecoders = new CipTagDecoders();
    private final @NotNull CipTagEncoders cipTagsEncoders = new CipTagEncoders();

    // Serializes southbound writes so a read-modify-write's read->write window is never interleaved with another
    // write. A single adapter-level lock is enough: the bundled client serializes all connection I/O anyway
    // (etherip.protocol.Connection.execute is synchronized), so per-attribute parallelism is not actually
    // available, and one lock keeps the concurrency model simple. Does not protect against the device being
    // changed by its own program logic during the window (CIP offers no compare-and-swap); last writer wins.
    private final @NotNull Object writeLock = new Object();
    private final @NotNull Hysteresis hysteresis = new Hysteresis();
    private final @NotNull List<CipTag> tags;
    private final @NotNull TagGroups tagGroups = new TagGroups();

    @VisibleForTesting
    @NotNull
    Supplier<Long> clock = System::currentTimeMillis;

    static {
        // Enable JUL to SLF4j bridge
        JULtoSLF4JEnabler.enable();
    }

    public EthernetIPCipOdvaPollingProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<EipSpecificAdapterConfig> input) {
        this.adapterId = input.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();

        this.tags = input.getTags().stream().map(t -> (CipTag) t).toList();

        this.statsTracker = new StatsTracker(adapterId);

        this.protocolAdapterState = input.getProtocolAdapterState();
        this.adapterFactories = input.adapterFactories();
    }

    @Override
    public @NotNull String getId() {
        return adapterId;
    }

    @Override
    public void start(
            final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {

        try {
            lastSamples.set(new DataPointStore());
            tagGroups.registerTagsIfEmpty(tags);
        } catch (OdvaException e) {
            output.failStart(e, "Errors found in defined tags! " + e.getMessage());
            return;
        }

        tryConnect(
                e -> LOG.error(
                        "Adapter '{}'. Failed to connect and will retry! {}",
                        adapterId,
                        ExceptionUtils.extractMessageWithCause(e))

                // FIXME: Add send message with error via event service?
                );

        // If validation is ok, start successfully even if connect fails
        output.startedSuccessfully();
    }

    private void tryConnect(@NotNull Consumer<Exception> onError) {
        try {
            connect();
            protocolAdapterState.setConnectionStatus(CONNECTED);
        } catch (final Exception e) {
            protocolAdapterState.setConnectionStatus(DISCONNECTED);

            onError.accept(ExceptionProcessor.substituteTimeOutExceptionWithOdvaException(e));
        }
    }

    @SuppressWarnings("java:S2095")
    private void connect() throws Exception {
        final EthernetIPWithODVA connectingEthernetIp =
                new EthernetIPWithODVA(adapterConfig.getHost(), adapterConfig.getSlot());
        connectingEthernetIp.connectTcp();
        this.etherNetIP.set(connectingEthernetIp);
    }

    @Override
    public void stop(
            final @NotNull ProtocolAdapterStopInput protocolAdapterStopInput,
            final @NotNull ProtocolAdapterStopOutput protocolAdapterStopOutput) {

        tryDisconnect(protocolAdapterStopOutput::stoppedSuccessfully, e -> {
            protocolAdapterStopOutput.failStop(e, "Problem stopping adapter");
            LOG.error("Adapter '{}'. Problem during stopping adapter", adapterId, e);
        });
    }

    private void tryDisconnect() {
        tryDisconnect(null, null);
    }

    private void tryDisconnect(@Nullable Runnable onSuccess, @Nullable Consumer<Exception> onError) {
        try {
            disconnect();
            if (onSuccess != null) {
                onSuccess.run();
            }
        } catch (final Exception e) {
            if (onError != null) {
                onError.accept(e);
            }
        }
    }

    private void disconnect() throws Exception {
        try {
            final EtherNetIP disconnectingEtherNetIp = etherNetIP.getAndSet(null);

            if (disconnectingEtherNetIp != null) {
                disconnectingEtherNetIp.close();
                LOG.info("Adapter '{}'. Disconnected successfully", adapterId);
            } else {
                LOG.info("Adapter '{}'. Already disconnected", adapterId);
            }
        } finally {
            protocolAdapterState.setConnectionStatus(DISCONNECTED);
        }
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    @Override
    public void poll(final @NotNull BatchPollingInput pollingInput, final @NotNull BatchPollingOutput pollingOutput) {

        if (isAdapterNotStarted()) {
            // A poll can still fire while the adapter is being torn down. That is not a sampling error — it is a
            // teardown-window race — so finish quietly instead of failing. Reporting it as a failure would trip
            // the framework's error counter and drive a northbound connection-error transition, which can race
            // the concurrent stop and log a spurious "failed to transition from Disconnected to Error".
            pollingOutput.finish();
            return;
        }

        EthernetIPWithODVA client = etherNetIP.get();
        if (isNotConnected(client)) {
            tryConnect(e -> pollingOutput.fail(e, "Failed to reconnect! Will retry!"));

            client = etherNetIP.get();
            if (isNotConnected(client)) {
                return;
            }
        }
        final EthernetIPWithODVA connectedClient = requireNonNull(client);

        final DataPointStore currentLastSamples = requireNonNull(lastSamples.get());
        final List<String> errors = new ArrayList<>();
        try {
            // Write-only tag groups have no readable attribute; never poll them.
            final List<TagGroup> readableGroups = tagGroups.getTagGroups().stream()
                    .filter(TagGroup::isReadable)
                    .toList();
            for (final TagGroup tagGroup : readableGroups) {
                tryPoll(connectedClient, pollingOutput, tagGroup, currentLastSamples, errors::add);
            }
        } catch (final Exception e) {
            // A tag-group read can rethrow a connection-level failure (see tryPoll); drop the connection and
            // reconnect on the next poll. Per-tag read/decode errors are collected in `errors` instead.
            LOG.warn(
                    "Adapter '{}'. Communication error. Will try reconnecting! {}",
                    adapterId,
                    ExceptionUtils.extractMessageWithCause(e));
            tryDisconnect();
        }

        reportOutcome(pollingOutput, errors);
    }

    /**
     * Reports the single outcome of a poll to the framework: {@code finish()} if every readable tag group was
     * read without error, otherwise {@code fail()} with the collected messages (which increments the framework's
     * error counter and, past the configured limit, stops the adapter).
     */
    private void reportOutcome(final @NotNull BatchPollingOutput pollingOutput, final @NotNull List<String> errors) {
        if (errors.isEmpty()) {
            pollingOutput.finish();
        } else {
            final String errorString = String.join(",", errors);
            LOG.warn("Adapter '{}'. {}", adapterId, errorString);
            pollingOutput.fail(errorString);
        }
    }

    private boolean isAdapterNotStarted() {
        return protocolAdapterState.getRuntimeStatus() != ProtocolAdapterState.RuntimeStatus.STARTED;
    }

    private boolean isNotConnected(final @Nullable EtherNetIP client) {
        return client == null || protocolAdapterState.getConnectionStatus() != CONNECTED;
    }

    private void tryPoll(
            final @NotNull EthernetIPWithODVA client,
            final @NotNull BatchPollingOutput pollingOutput,
            final @NotNull TagGroup tagGroup,
            final @NotNull DataPointStore currentLastSamples,
            final @NotNull Consumer<String> errorConsumer)
            throws Exception {
        try {
            Stopwatch stopwatch = statsTracker.start();
            pollAndDecode(client, tagGroup, currentLastSamples, pollingOutput);
            statsTracker.stop(tagGroup::toConciseString, stopwatch);
        } catch (final CipException e) {
            if (e.getStatusCode() == 0x04) {
                errorConsumer.accept("Error '{" + e.getStatusCode()
                        + "}'. Tag at logical address '"
                        + tagGroup.getTagAddress()
                        + "' does not exist!");
            } else {
                errorConsumer.accept("Error '" + e.getStatusCode()
                        + "' reading Tag at logical address '"
                        + tagGroup.getTagAddress()
                        + "'!");
            }
        } catch (OdvaDecodeException e) {
            errorConsumer.accept("Error decoding '" + e.getCipTag().toConciseString()
                    + "'. "
                    + ExceptionUtils.extractMessageWithCause(e));
        } catch (Exception e) {
            errorConsumer.accept(
                    "Error reading '" + tagGroup.toConciseString() + "'. " + ExceptionUtils.extractMessageWithCause(e));

            throwIfExceptionContainsReasonToDisconnect(e);
        }
    }

    private void throwIfExceptionContainsReasonToDisconnect(@NotNull Throwable e) throws Exception {
        // Handles ie: ReadPendingException, WritePendingException
        if (e instanceof RuntimeException runtimeException) {
            throw runtimeException;
        } else if (e instanceof Exception exception && Strings.CI.containsAny(e.getMessage(), DISCONNECT_REASONS)) {
            throw exception;
        } else if (e.getCause() != null) {
            throwIfExceptionContainsReasonToDisconnect(e.getCause());
        }
    }

    private void pollAndDecode(
            final @NotNull EthernetIPWithODVA client,
            final @NotNull TagGroup tagGroup,
            final @NotNull DataPointStore currentLastSamples,
            final @NotNull BatchPollingOutput pollingOutput)
            throws Exception {

        final CompositeValues compositeValues = CompositeValuesFactory.create(tagGroup);
        final Long nowMs = clock.get();
        final DataPointListBuilder dataPoints = pollingOutput.dataPointListPublisher();

        CipTagDecodingAttributeProtocol cipTagAttributeProtocol = new CipTagDecodingAttributeProtocol(
                cipTagsDecoders,
                tagGroup.getTags(),
                adapterConfig.getByteOrder().toNioByteOrder(),
                (tag, value) -> {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Adapter {}. Read tag {}='{}'", adapterId, tag.toConciseString(), value);
                    }

                    if (adapterConfig.getEipToMqttConfig().getPublishChangedDataOnly()) {
                        Object currentValue = currentLastSamples.get(tag);

                        if (hysteresis.isModified(value, currentValue, tag.getDefinition())
                                || currentLastSamples.isValueOlderThan(tag, nowMs)) {
                            currentLastSamples.put(tag, value, nowMs);
                            applyValue(dataPoints.addDataPoint(tag), value);
                            compositeValues.add(tag.getName(), value);
                        }
                    } else {
                        applyValue(dataPoints.addDataPoint(tag), value);
                        compositeValues.add(tag.getName(), value);
                    }
                });

        client.getAttributeSingle(tagGroup.getLogicalAddressPath(), cipTagAttributeProtocol);

        final CipTag composite = tagGroup.getComposite();
        if (!compositeValues.isEmpty() && composite != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(
                        "Adapter {}. Created composite tag {}='{}'",
                        adapterId,
                        composite.toConciseString(),
                        compositeValues.getValues());
            }
            final DataPointBuilder.ObjectBuilder<DataPointBuilder<DataPointListBuilder>> objectBuilder =
                    dataPoints.addDataPoint(composite).startObjectValue();
            compositeValues.getValues().forEach((name, value) -> putValue(objectBuilder, name, value));
            objectBuilder.endObject();
        }

        dataPoints.publish();
    }

    /**
     * Encodes a decoded CIP value into a datapoint using its concrete Java type (see the {@code decoder}
     * package for the produced types). Arrays ({@code numberOfElements > 1}) arrive as a {@link List} and are
     * written as a JSON array; anything unexpected falls back to its string form.
     */
    private static void applyValue(
            final @NotNull DataPointBuilder<DataPointListBuilder> builder, final @NotNull Object value) {
        switch (value) {
            case final Boolean v -> builder.value(v);
            case final Byte v -> builder.value(v);
            case final Short v -> builder.value(v);
            case final Integer v -> builder.value(v);
            case final Long v -> builder.value(v);
            case final Float v -> builder.value(v);
            case final Double v -> builder.value(v);
            case final String v -> builder.value(v);
            case final List<?> v -> {
                final DataPointBuilder.ArrayBuilder<DataPointBuilder<DataPointListBuilder>> array =
                        builder.startArrayValue();
                v.forEach(element -> addToArray(array, element));
                array.endArray();
            }
            default -> builder.value(value.toString());
        }
    }

    /** As {@link #applyValue} but writing a named field into a structured composite object. */
    private static void putValue(
            final @NotNull DataPointBuilder.ObjectBuilder<?> builder,
            final @NotNull String name,
            final @NotNull Object value) {
        switch (value) {
            case final Boolean v -> builder.put(name, v);
            case final Byte v -> builder.put(name, v);
            case final Short v -> builder.put(name, v);
            case final Integer v -> builder.put(name, v);
            case final Long v -> builder.put(name, v);
            case final Float v -> builder.put(name, v);
            case final Double v -> builder.put(name, v);
            case final String v -> builder.put(name, v);
            case final List<?> v -> {
                final DataPointBuilder.ArrayBuilder<? extends DataPointBuilder.ObjectBuilder<?>> array =
                        builder.startArray(name);
                v.forEach(element -> addToArray(array, element));
                array.endArray();
            }
            default -> builder.put(name, value.toString());
        }
    }

    /**
     * Adds a single decoded element to an array builder, by its concrete Java type. The CIP decoders produce
     * flat lists of scalars, so the nested-list case is defensive: it recurses rather than stringifying, so a
     * nested array is preserved as an array should the decoders ever produce one.
     */
    private static void addToArray(final @NotNull DataPointBuilder.ArrayBuilder<?> array, final @NotNull Object value) {
        switch (value) {
            case final Boolean v -> array.add(v);
            case final Byte v -> array.add(v);
            case final Short v -> array.add(v);
            case final Integer v -> array.add(v);
            case final Long v -> array.add(v);
            case final Float v -> array.add(v);
            case final Double v -> array.add(v);
            case final String v -> array.add(v);
            case final List<?> v -> {
                final DataPointBuilder.ArrayBuilder<? extends DataPointBuilder.ArrayBuilder<?>> nested =
                        array.startArray();
                v.forEach(element -> addToArray(nested, element));
                nested.endArray();
            }
            default -> array.add(value.toString());
        }
    }

    @Override
    public int getPollingIntervalMillis() {
        return adapterConfig.getEipToMqttConfig().getPollingIntervalMillis();
    }

    @Override
    public int getMaxPollingErrorsBeforeRemoval() {
        return adapterConfig.getEipToMqttConfig().getMaxPollingErrorsBeforeRemoval();
    }

    @Override
    public void createTagSchema(
            final @NotNull TagSchemaCreationInput input, final @NotNull TagSchemaCreationOutput output) {
        final String tagName = input.getTagName();
        final Optional<CipTag> maybeTag =
                tags.stream().filter(t -> t.getName().equals(tagName)).findFirst();
        if (maybeTag.isEmpty()) {
            output.fail("Unable to find tag definition for tag " + tagName + ", cannot create schema");
            return;
        }
        final CipTag tag = maybeTag.get();
        if (tag.isComposite()) {
            final Schema compositeSchema = buildCompositeSchema(tag, output);
            if (compositeSchema == null) {
                return;
            }
            output.finish(new TagSchemaCreationOutput.DataPointSchema(compositeSchema, null, null));
            return;
        }
        final Schema scalarSchema = TagSchemaMapper.buildScalarSchema(
                tag.getDefinition().getDataType(), tag.isReadable(), tag.isWritable());
        output.finish(new TagSchemaCreationOutput.DataPointSchema(scalarSchema, null, null));
    }

    private @Nullable Schema buildCompositeSchema(
            final @NotNull CipTag composite, final @NotNull TagSchemaCreationOutput output) {
        final String address = composite.getDefinition().getAddress();
        final CipReadWrite readWrite = composite.getDefinition().getReadWrite();
        // Siblings are grouped at runtime by (address, readWrite) — see TagGroups.GroupKey — so the schema
        // must group the same way, or it could advertise siblings the runtime composite never publishes.
        final List<CipTag> siblings = tags.stream()
                .filter(t -> !t.isComposite())
                .filter(t -> t.getDefinition().getAddress().equals(address))
                .filter(t -> t.getDefinition().getReadWrite() == readWrite)
                .toList();
        if (siblings.isEmpty()) {
            output.fail("Composite tag '" + composite.getName() + "' has no scalar siblings at address " + address);
            return null;
        }
        // The composite and its siblings share the group's direction, so their schema flags match the
        // composite's configured CipReadWrite.
        final boolean readable = composite.isReadable();
        final boolean writable = composite.isWritable();
        final SchemaBuilder builder = new SchemaBuilder();
        var objectBuilder = builder.startObject();
        for (final CipTag sibling : siblings) {
            final CipDataType siblingType = sibling.getDefinition().getDataType();
            objectBuilder = objectBuilder
                    .property(sibling.getName())
                    .schema(TagSchemaMapper.buildScalarSchema(siblingType, readable, writable))
                    .endProperty();
        }
        objectBuilder.endObject();
        return builder.readable(readable).writable(writable).build();
    }

    @Override
    public void write(@NotNull final WritingInput writingInput, @NotNull final WritingOutput writingOutput) {
        final String tagName = writingInput.getWritingContext().getTagName();
        final CipWritePayload cipWritePayload = (CipWritePayload) writingInput.getWritingPayload();
        final JsonNode value = cipWritePayload.getValue();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Adapter '{}'. Received write tag={}, payload={}", adapterId, tagName, value);
        }

        final CipTag tag = tags.stream()
                .filter(t -> t.getName().equals(tagName))
                .findFirst()
                .orElse(null);
        if (tag == null) {
            writingOutput.fail("Tag '" + tagName + "' not found for write.");
            return;
        }
        if (!tag.isWritable()) {
            writingOutput.fail("Tag '" + tagName + "' is READ_ONLY and cannot be written.");
            return;
        }

        final TagGroup tagGroup = tagGroups.getTagGroups().stream()
                .filter(g -> g.getTags().contains(tag) || Objects.equals(g.getComposite(), tag))
                .findFirst()
                .orElse(null);
        if (tagGroup == null) {
            writingOutput.fail("No tag group found for tag '" + tagName + "'.");
            return;
        }

        final EthernetIPWithODVA client = etherNetIP.get();
        if (isNotConnected(client)) {
            writingOutput.fail("Adapter '" + adapterId + "' is not connected; cannot write tag '" + tagName + "'.");
            return;
        }
        final EthernetIPWithODVA connectedClient = requireNonNull(client);

        // The supplied tags are encoded on top of the attribute buffer. For a composite the payload is a JSON
        // object keyed by sibling tag name; for a scalar it is the value itself.
        final List<CipTag> tagsToWrite = tag.isComposite() ? tagGroup.getTags() : List.of(tag);
        final CipTagValueProducer<Object> valueProducer = t -> {
            final JsonNode node = tag.isComposite() ? value.get(t.getName()) : value;
            if (node == null) {
                throw new IllegalArgumentException("No value supplied for tag '" + t.getName() + "'.");
            }
            return jsonToCipValue(t, node);
        };

        // Serialize the read->write window of a PARTIAL_WRITE so it is not interleaved with another write.
        // COMPLETE_WRITE does not read, but sharing the lock is harmless and keeps concurrent writes ordered.
        synchronized (writeLock) {
            try {
                final byte @Nullable [] prefill = tag.getWriteMode() == CipWriteMode.PARTIAL_WRITE
                        ? readAttributeBytes(connectedClient, tagGroup)
                        : null;

                final CipTagEncodingAttributeProtocol encodingProtocol = new CipTagEncodingAttributeProtocol(
                        cipTagsEncoders,
                        tagsToWrite,
                        adapterConfig.getByteOrder().toNioByteOrder(),
                        valueProducer,
                        prefill);
                connectedClient.setAttributeSingle(tagGroup.getLogicalAddressPath(), encodingProtocol);
                writingOutput.finish();
            } catch (final Exception e) {
                // A COMPLETE_WRITE whose configured tag(s) do not actually span the whole attribute produces a
                // request shorter than the device's attribute, which the device rejects (typically CIP status
                // 0x08 "Service not supported"). Surface that likely cause rather than the raw device error.
                if (tag.getWriteMode() == CipWriteMode.COMPLETE_WRITE) {
                    LOG.warn(
                            "Adapter '{}'. Failed to write tag '{}' using COMPLETE_WRITE: {}. The device rejected the "
                                    + "request; the configured tag(s) at {} may not span the whole attribute. Either make "
                                    + "them cover the full attribute, or use PARTIAL_WRITE.",
                            adapterId,
                            tagName,
                            ExceptionUtils.extractMessageWithCause(e),
                            tagGroup.getTagAddress());
                    writingOutput.fail(
                            e,
                            "Failed to write tag '"
                                    + tagName
                                    + "' using COMPLETE_WRITE: the configured tag(s) at "
                                    + tagGroup.getTagAddress()
                                    + " may not span the whole attribute. Either make them cover the full attribute, or use PARTIAL_WRITE.");
                } else {
                    LOG.warn(
                            "Adapter '{}'. Failed to write tag '{}': {}",
                            adapterId,
                            tagName,
                            ExceptionUtils.extractMessageWithCause(e));
                    writingOutput.fail(e, "Failed to write tag '" + tagName + "'.");
                }
            }
        }
    }

    /**
     * Read the current bytes of the attribute for a read-modify-write. A failed read is a hard stop: the
     * caller must not fall back to zeroing the unsupplied bytes.
     */
    private byte @NotNull [] readAttributeBytes(
            final @NotNull EthernetIPWithODVA client, final @NotNull TagGroup tagGroup) throws Exception {
        final RawAttributeReadProtocol readProtocol = new RawAttributeReadProtocol();
        client.getAttributeSingle(tagGroup.getLogicalAddressPath(), readProtocol);
        final byte[] bytes = readProtocol.getBytes();
        if (bytes == null) {
            throw new OdvaException(
                    "Read-modify-write failed: could not read current attribute at " + tagGroup.getTagAddress());
        }
        return bytes;
    }

    /**
     * Convert a JSON value into the Java type the encoder for this tag's {@link CipDataType} expects:
     * {@link Boolean} for BOOL, {@link Number} for the integer/real types, {@link String} for the string
     * types. When the tag reads more than one element the JSON value must be an array and is converted to a
     * {@link List} of the corresponding scalar type.
     */
    @VisibleForTesting
    static @NotNull Object jsonToCipValue(final @NotNull CipTag tag, final @NotNull JsonNode node) {
        final CipDataType dataType = tag.getDefinition().getDataType();
        if (tag.getDefinition().getNumberOfElements() > 1) {
            if (!node.isArray()) {
                throw new IllegalArgumentException(
                        "Tag '" + tag.getName() + "' expects an array of " + dataType + " values.");
            }
            final List<Object> values = new ArrayList<>(node.size());
            for (final JsonNode element : node) {
                values.add(jsonToScalar(tag, dataType, element));
            }
            return values;
        }
        return jsonToScalar(tag, dataType, node);
    }

    /**
     * Converts a single JSON value to the Java value the encoder expects, rejecting anything that does not
     * strictly match the tag's CIP type. Jackson's {@code asLong}/{@code asDouble}/{@code asText}/{@code asBoolean}
     * silently coerce mismatched kinds (e.g. a string to 0, a fractional number to a truncated integer), and the
     * encoders then narrow a wider Java value to the CIP width, wrapping out-of-range values. Requiring the exact
     * JSON node kind and enforcing the type's range here turns both of those silent corruptions into a rejected
     * write.
     */
    private static @NotNull Object jsonToScalar(
            final @NotNull CipTag tag, final @NotNull CipDataType dataType, final @NotNull JsonNode node) {
        return switch (dataType) {
            case BOOL -> {
                if (!node.isBoolean()) {
                    throw new IllegalArgumentException(typeError(tag, dataType, "a boolean", node));
                }
                yield node.booleanValue();
            }
            case SINT, USINT, INT, UINT, DINT, UDINT, LINT -> toRangedLong(tag, dataType, node);
            case REAL, LREAL -> toRangedDouble(tag, dataType, node);
            case STRING, SSTRING -> {
                if (!node.isTextual()) {
                    throw new IllegalArgumentException(typeError(tag, dataType, "a string", node));
                }
                yield node.textValue();
            }
            case COMPOSITE ->
                throw new IllegalArgumentException(
                        "Tag '" + tag.getName() + "' has data type COMPOSITE and cannot be written as a scalar value.");
        };
    }

    private static long toRangedLong(
            final @NotNull CipTag tag, final @NotNull CipDataType dataType, final @NotNull JsonNode node) {
        if (!node.isIntegralNumber()) {
            throw new IllegalArgumentException(typeError(tag, dataType, "an integer", node));
        }
        final long value = node.longValue();
        final CipNumericRange.IntegerRange range = CipNumericRange.integerRange(dataType);
        if (value < range.minimum() || value > range.maximum()) {
            throw new IllegalArgumentException(rangeError(tag, dataType, value, range.minimum(), range.maximum()));
        }
        return value;
    }

    private static double toRangedDouble(
            final @NotNull CipTag tag, final @NotNull CipDataType dataType, final @NotNull JsonNode node) {
        // A whole number such as 3 is a valid float, so accept any numeric node, not only floating-point ones.
        if (!node.isNumber()) {
            throw new IllegalArgumentException(typeError(tag, dataType, "a number", node));
        }
        final double value = node.doubleValue();
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    "Tag '" + tag.getName() + "' (" + dataType + ") does not accept a non-finite value.");
        }
        final CipNumericRange.FloatRange range = CipNumericRange.floatRange(dataType);
        if (value < range.minimum() || value > range.maximum()) {
            throw new IllegalArgumentException(rangeError(tag, dataType, value, range.minimum(), range.maximum()));
        }
        return value;
    }

    private static @NotNull String typeError(
            final @NotNull CipTag tag,
            final @NotNull CipDataType dataType,
            final @NotNull String expected,
            final @NotNull JsonNode node) {
        return "Tag '"
                + tag.getName()
                + "' ("
                + dataType
                + ") expects "
                + expected
                + " but got JSON "
                + node.getNodeType()
                + ".";
    }

    private static @NotNull String rangeError(
            final @NotNull CipTag tag,
            final @NotNull CipDataType dataType,
            final @NotNull Object value,
            final @NotNull Object minimum,
            final @NotNull Object maximum) {
        return "Tag '"
                + tag.getName()
                + "' ("
                + dataType
                + ") value "
                + value
                + " is out of range ["
                + minimum
                + ", "
                + maximum
                + "].";
    }

    @Override
    public @NotNull Class<? extends WritingPayload> getMqttPayloadClass() {
        return CipWritePayload.class;
    }
}
