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

import com.google.common.base.Stopwatch;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingInput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.writing.WritingInput;
import com.hivemq.adapter.sdk.api.writing.WritingOutput;
import com.hivemq.adapter.sdk.api.writing.WritingPayload;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.edge.adapters.etherip_cip_odva.composite.CompositeValues;
import com.hivemq.edge.adapters.etherip_cip_odva.composite.CompositeValuesFactory;
import com.hivemq.edge.adapters.etherip_cip_odva.config.EipSpecificAdapterConfig;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTag;
import com.hivemq.edge.adapters.etherip_cip_odva.decoder.CipTagDecoders;
import com.hivemq.edge.adapters.etherip_cip_odva.exception.ExceptionProcessor;
import com.hivemq.edge.adapters.etherip_cip_odva.exception.OdvaDecodeException;
import com.hivemq.edge.adapters.etherip_cip_odva.exception.OdvaException;
import com.hivemq.edge.adapters.etherip_cip_odva.handler.CipTagDecodingAttributeProtocol;
import com.hivemq.edge.adapters.etherip_cip_odva.hysteresis.Hysteresis;
import com.hivemq.edge.adapters.etherip_cip_odva.tag.TagGroup;
import com.hivemq.edge.adapters.etherip_cip_odva.tag.TagGroups;
import com.hivemq.edge.adapters.etherip_cip_odva.util.ExceptionUtils;
import etherip.EtherNetIP;
import etherip.EthernetIPWithODVA;
import etherip.data.CipException;
import etherip.protocol.Encapsulation;
import java.util.ArrayList;
import java.util.List;
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
            pollingOutput.fail(getNotStartedMessage());
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

        List<String> errors = new ArrayList<>();
        DataPointStore currentLastSamples = lastSamples.get();
        try {
            // FIXME: Introduce Multi Request: MPR for single tags, Separate requests for BatchedTags?
            // Have to be careful - as maximum PLC response and request size are limiting factors and need
            // to be taken into account

            // FIXME: Add support for larger response bodies using read_fragmented

            // FIXME: Can easily add support for "read tag" as well, with same Decoders/Encoders

            // Create Requests, set handlers
            for (TagGroup tagGroup : tagGroups.getTagGroups()) {
                if (isAdapterNotStarted()) {
                    errors.add(getNotStartedMessage());
                    return;
                }

                tryPoll(client, pollingOutput, tagGroup, currentLastSamples, errors::add);
            }

        } catch (Exception e) {
            LOG.warn(
                    "Adapter '{}'. Communication error. Will try reconnecting! {}",
                    adapterId,
                    ExceptionUtils.extractMessageWithCause(e));

            tryDisconnect();
        } finally {
            if (errors.isEmpty()) {
                pollingOutput.finish();
            } else {
                String errorString = String.join(",", errors);

                LOG.warn("Adapter '{}'. {}", adapterId, errorString);
                pollingOutput.fail(errorString);
            }
        }
    }

    private boolean isAdapterNotStarted() {
        return protocolAdapterState.getRuntimeStatus() != ProtocolAdapterState.RuntimeStatus.STARTED;
    }

    private String getNotStartedMessage() {
        return String.format(
                "%s is stopped during polling (state=%s)", adapterId, protocolAdapterState.getRuntimeStatus());
    }

    private boolean isNotConnected(EtherNetIP client) {
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
                            pollingOutput.addDataPoint(tag.getName(), value);
                            compositeValues.add(tag.getName(), value);
                        }
                    } else {
                        pollingOutput.addDataPoint(tag.getName(), value);
                        compositeValues.add(tag.getName(), value);
                    }
                });

        client.getAttributeSingle(tagGroup.getLogicalAddressPath(), cipTagAttributeProtocol);

        if (!compositeValues.isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(
                        "Adapter {}. Created composite tag {}='{}'",
                        adapterId,
                        tagGroup.getComposite() != null
                                ? tagGroup.getComposite().toConciseString()
                                : "<NULL>",
                        compositeValues.getValues());
            }
            pollingOutput.addDataPoint(compositeValues.getCompositeTagName(), compositeValues.getValues());
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
    public void write(@NotNull final WritingInput writingInput, @NotNull final WritingOutput writingOutput) {
        CipWritePayload cipWritePayload = (CipWritePayload) writingInput.getWritingPayload();

        LOG.info(
                "{}. Received write tag={}, payload={}",
                adapterId,
                writingInput.getWritingContext().getTagName(),
                cipWritePayload.getValue());

        // write to happen here

        writingOutput.finish();
    }

    @Override
    public @NotNull Class<? extends WritingPayload> getMqttPayloadClass() {
        return CipWritePayload.class;
    }
}
