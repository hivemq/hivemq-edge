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
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

public class EipPollingProtocolAdapter implements BatchPollingProtocolAdapter {

    private static final @NotNull org.slf4j.Logger log = LoggerFactory.getLogger(EipPollingProtocolAdapter.class);

    private final @NotNull EipSpecificAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull String adapterId;
    private volatile @Nullable EtherNetIP etherNetIP;
    private final @NotNull PublishChangedDataOnlyHandler lastSamples = new PublishChangedDataOnlyHandler();

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
        // any setup which should be done before the adapter starts polling comes here.
        try {
            final EtherNetIP etherNetIP = new EtherNetIP(adapterConfig.getHost(), adapterConfig.getSlot());
            etherNetIP.connectTcp();
            this.etherNetIP = etherNetIP;
            output.startedSuccessfully();
            protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        } catch (final Exception e) {
            output.failStart(e, null);
        }
    }

    @Override
    public void stop(
            final @NotNull ProtocolAdapterStopInput protocolAdapterStopInput,
            final @NotNull ProtocolAdapterStopOutput protocolAdapterStopOutput) {
        try {
            final EtherNetIP etherNetIPTemp = etherNetIP;
            etherNetIP = null;
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
        }
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    @Override
    public void poll(final @NotNull BatchPollingInput pollingInput, final @NotNull BatchPollingOutput pollingOutput) {
        final var client = etherNetIP;
        if (client == null) {
            pollingOutput.fail("Polling failed because adapter wasn't started.");
            return;
        }

        final var dataPointsPublisher = pollingOutput.dataPointsPublisher();
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
