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
package com.hivemq.edge.adapters.etherip;

import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingInput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.adapters.etherip.config.EipSpecificAdapterConfig;
import com.hivemq.edge.adapters.etherip.config.tag.EipTag;
import com.hivemq.edge.adapters.etherip.model.EtherIpValueFactory;
import etherip.EtherNetIP;
import etherip.data.CipException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

public class EipPollingProtocolAdapter implements BatchPollingProtocolAdapter {

    private static final @NotNull org.slf4j.Logger log = LoggerFactory.getLogger(EipPollingProtocolAdapter.class);

    /// The port and connect timeout `etherip` always uses; it offers no way to change either.
    private static final int ETHERIP_PORT = 44818;

    private static final int ETHERIP_CONNECT_TIMEOUT_MILLIS = 2000;

    private final @NotNull EipSpecificAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull String adapterId;
    private final @NotNull PublishChangedDataOnlyHandler lastSamples = new PublishChangedDataOnlyHandler();

    /// Guards `etherNetIP` and `started`, so that a session opened by a poll while `stop()` runs is closed
    /// instead of left open. The lock is never held while talking to the device.
    private final @NotNull Object sessionLock = new Object();

    private @Nullable EtherNetIP etherNetIP;
    private boolean started;

    private final @NotNull Map<String, EipTag> tags;

    public EipPollingProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<EipSpecificAdapterConfig> input) {
        this.adapterId = input.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.tags = input.getTags().stream()
                .map(tag -> (EipTag) tag)
                .collect(Collectors.toMap(tag -> tag.getDefinition().getAddress(), tag -> tag));
        this.protocolAdapterState = input.getProtocolAdapterState();
    }

    @Override
    public @NotNull String getId() {
        return adapterId;
    }

    @Override
    public void start(
            final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
        // -- The adapter starts even when the device is unreachable; every poll retries the connection.
        synchronized (sessionLock) {
            started = true;
        }
        try {
            openSession();
        } catch (final Exception e) {
            protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
            log.warn(
                    "Adapter '{}' could not connect to {}, retrying with every poll: {}",
                    adapterId,
                    adapterConfig.getHost(),
                    e.toString());
        }
        output.startedSuccessfully();
    }

    @Override
    public void stop(
            final @NotNull ProtocolAdapterStopInput protocolAdapterStopInput,
            final @NotNull ProtocolAdapterStopOutput protocolAdapterStopOutput) {
        final EtherNetIP etherNetIPTemp;
        synchronized (sessionLock) {
            started = false;
            etherNetIPTemp = etherNetIP;
            etherNetIP = null;
        }
        try {
            if (etherNetIPTemp != null) {
                etherNetIPTemp.close();
                protocolAdapterStopOutput.stoppedSuccessfully();
                log.info("Stopped");
            } else {
                protocolAdapterStopOutput.stoppedSuccessfully();
                log.info("Stopped without an open connection");
            }
        } catch (final Exception e) {
            protocolAdapterStopOutput.failStop(e, "Unable to stop Ethernet IP connection");
            log.error("Unable to stop", e);
        } finally {
            protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
        }
    }

    /// Returns the open session, opening one first if there is none.
    /// Returns `null` if the adapter is not started; a session opened while `stop()` ran is closed again.
    private @Nullable EtherNetIP openSession() throws Exception {
        synchronized (sessionLock) {
            if (!started) {
                return null;
            }
            if (etherNetIP != null) {
                return etherNetIP;
            }
        }
        checkDeviceAccepts();
        final EtherNetIP opened = new EtherNetIP(adapterConfig.getHost(), adapterConfig.getSlot());
        try {
            opened.connectTcp();
        } catch (final Exception e) {
            closeQuietly(opened);
            throw e;
        }
        final EtherNetIP current;
        synchronized (sessionLock) {
            if (started && etherNetIP == null) {
                etherNetIP = opened;
                protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
                log.info("Adapter '{}' connected to {}", adapterId, adapterConfig.getHost());
                return opened;
            }
            current = etherNetIP;
        }
        // -- stop() ran, or a concurrent poll opened a session first.
        closeQuietly(opened);
        return current;
    }

    /// Throws if the device does not accept a TCP connection within the time `etherip` waits for one.
    ///
    /// `etherip` leaves its socket open when its connect times out, and the socket stays open for good if the
    /// device answers later. A `java.net.Socket` closes on a timed-out connect, so this check fails first for a
    /// device that does not answer, and `etherip` connects only to a device that answered a moment ago.
    @SuppressWarnings("AddressSelection") // -- etherip connects to the same single address
    private void checkDeviceAccepts() throws IOException {
        try (Socket check = new Socket()) {
            check.connect(new InetSocketAddress(adapterConfig.getHost(), ETHERIP_PORT), ETHERIP_CONNECT_TIMEOUT_MILLIS);
        }
    }

    /// Drops the session after a failure that may have broken it, so that the next poll opens a new one.
    private void dropSession(final @NotNull EtherNetIP broken) {
        synchronized (sessionLock) {
            if (etherNetIP != broken) {
                return;
            }
            etherNetIP = null;
            if (started) {
                protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
            }
        }
        closeQuietly(broken);
    }

    private void closeQuietly(final @NotNull EtherNetIP session) {
        try {
            session.close();
        } catch (final Exception e) {
            log.debug("Adapter '{}' could not close a session cleanly", adapterId, e);
        }
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    @Override
    public void poll(final @NotNull BatchPollingInput pollingInput, final @NotNull BatchPollingOutput pollingOutput) {
        final EtherNetIP client;
        try {
            client = openSession();
        } catch (final Exception e) {
            log.warn("Adapter '{}' could not connect to {}: {}", adapterId, adapterConfig.getHost(), e.toString());
            pollingOutput.fail(e, "Unable to connect to EtherNet/IP device at " + adapterConfig.getHost() + ".");
            return;
        }
        if (client == null) {
            pollingOutput.fail("Polling failed because adapter wasn't started.");
            return;
        }

        final var dataPointsPublisher = pollingOutput.dataPointListPublisher();
        final var tagAddresses =
                tags.values().stream().map(v -> v.getDefinition().getAddress()).toArray(String[]::new);
        final boolean publishChangedDataOnly =
                adapterConfig.getEipToMqttConfig().getPublishChangedDataOnly();
        try {
            final var readCipData = client.readTags(tagAddresses);
            for (int i = 0; i < readCipData.length; i++) {
                final var cipData = readCipData[i];
                final var tagAddress = tagAddresses[i];
                EtherIpValueFactory.fromTagAddressAndCipData(tagAddress, cipData)
                        .ifPresent(etherIpValue -> {
                            final var tag = Objects.requireNonNull(tags.get(tagAddress));
                            final var value = etherIpValue.getValue();
                            if (!publishChangedDataOnly || lastSamples.replaceIfValueIsNew(tag.getName(), value)) {
                                final var builder = dataPointsPublisher.addDataPoint(tag);
                                switch (value) {
                                    case final Boolean val -> builder.value(val);
                                    case final Integer val -> builder.value(val);
                                    case final Long val -> builder.value(val);
                                    case final Double val -> builder.value(val);
                                    case final String val -> builder.value(val);
                                    default -> builder.value(value.toString());
                                }
                            }
                        });
            }
            dataPointsPublisher.publish();
        } catch (final CipException e) {
            if (e.getStatusCode() == 0x04) {
                log.warn("A Tag doesn't exist on device.", e);
                pollingOutput.fail(e, "Tag doesn't exist on device");
            } else {
                log.warn("Problem accessing tag on device.", e);
                pollingOutput.fail(e, "Problem accessing tag on device.");
            }
        } catch (final Exception e) {
            // -- The device answered a CipException; anything else may have broken the session.
            dropSession(client);
            log.warn("An exception occurred while reading tags '{}'.", Arrays.toString(tagAddresses), e);
            pollingOutput.fail(e, "An exception occurred while reading tags '" + Arrays.toString(tagAddresses) + "'.");
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
}
