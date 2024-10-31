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
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.polling.PollingInput;
import com.hivemq.adapter.sdk.api.polling.PollingOutput;
import com.hivemq.adapter.sdk.api.polling.PollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.etherip.config.EipAdapterConfig;
import com.hivemq.edge.adapters.etherip.config.EipToMqttMapping;
import com.hivemq.edge.adapters.etherip.config.tag.EipTagDefinition;
import com.hivemq.edge.adapters.etherip.model.EtherIpValue;
import com.hivemq.edge.adapters.etherip.model.EtherIpValueFactory;
import etherip.EtherNetIP;
import etherip.data.CipException;
import etherip.types.CIPData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class EipPollingProtocolAdapter implements PollingProtocolAdapter<EipToMqttMapping> {

    private static final @NotNull org.slf4j.Logger log = LoggerFactory.getLogger(EipPollingProtocolAdapter.class);

    private static final @NotNull String TAG_ADDRESS_TYPE_SEP = ":";

    private final @NotNull EipAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    protected final @NotNull AdapterFactories adapterFactories;
    private volatile @Nullable EtherNetIP etherNetIP;

    private final @NotNull Map<String, EtherIpValue> lastSeenValues;

    public EipPollingProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<EipAdapterConfig> input) {
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.adapterFactories = input.adapterFactories();
        this.lastSeenValues = new ConcurrentHashMap<>();
    }

    @Override
    public @NotNull String getId() {
        return adapterConfig.getId();
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
            protocolAdapterState.setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STARTED);
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

                protocolAdapterState.setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STOPPED);
                log.info("Stopped");
            } else {
                protocolAdapterStopOutput.stoppedSuccessfully();
                log.info("Stopped without an open connection");
            }
        } catch (Exception e) {
            protocolAdapterStopOutput.failStop(e, "Unable to stop Ethernet IP connection");
            log.error("Unable to stop", e);
        }
    }


    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    @Override
    public void poll(
            final @NotNull PollingInput<EipToMqttMapping> pollingInput, final @NotNull PollingOutput pollingOutput) {
        if (etherNetIP == null) {
            pollingOutput.fail("Polling failed because adapter wasn't started.");
            return;
        }

        adapterConfig.getTags().stream()
                .filter(tag -> tag.getTagName().equals(pollingInput.getPollingContext().getTagName()))
                .findFirst()
                .ifPresentOrElse(
                        def -> pollWithAddress(pollingInput, pollingOutput, def),
                        () -> pollingOutput.fail("Polling for protocol adapter failed because the used tag '" +
                                pollingInput.getPollingContext().getTagName() +
                                "' was not found. For the polling to work the tag must be created via REST API or the UI.")
                );
    }

    private void pollWithAddress(
            @NotNull PollingInput<EipToMqttMapping> pollingInput,
            @NotNull PollingOutput pollingOutput,
            Tag<EipTagDefinition> eipAddressTag) {
        final String tagAddress = createTagAddressForSubscription(pollingInput.getPollingContext(),
                eipAddressTag.getTagDefinition().getAddress());
        try {
            final CIPData evt = etherNetIP.readTag(eipAddressTag.getTagDefinition().getAddress());

            if (adapterConfig.getEipToMqttConfig().getPublishChangedDataOnly()) {
                handleResult(evt, tagAddress).forEach(it -> {
                    if (!lastSeenValues.containsKey(tagAddress) || !lastSeenValues.get(tagAddress).equals(it)) {
                        pollingOutput.addDataPoint(tagAddress, it.getValue());
                        lastSeenValues.put(tagAddress, it);
                    }
                });
            } else {
                handleResult(evt, tagAddress).forEach(it -> pollingOutput.addDataPoint(tagAddress, it.getValue()));
            }


            pollingOutput.finish();
        } catch (CipException e) {
            if (e.getStatusCode() == 0x04) {
                log.warn("Tag '{}' doesn't exist on device.", tagAddress, e);
                pollingOutput.fail(e, "Tag '" + tagAddress + "'  doesn't exist on device");
            } else {
                log.warn("Problem accessing tag '{}' on device.", tagAddress, e);
                pollingOutput.fail(e, "Problem accessing tag '" + tagAddress + "' on device.");
            }
        } catch (Exception e) {
            log.warn("An exception occurred while reading tag '{}'.", tagAddress, e);
            pollingOutput.fail(e, "An exception occurred while reading tag '" + tagAddress + "'.");
        }
    }

    private @NotNull List<EtherIpValue> handleResult(final @NotNull CIPData evt, final @NotNull String tagAddress) {
        return EtherIpValueFactory.fromTagAddressAndCipData(tagAddress, evt).map(List::of).orElseGet(() -> {
            log.warn("Unable to parse tag {}, type {} not supported", tagAddress, evt.getType());
            return List.of();
        });
    }

    @Override
    public @NotNull List<EipToMqttMapping> getPollingContexts() {
        return adapterConfig.getEipToMqttConfig().getMappings();
    }

    @Override
    public int getPollingIntervalMillis() {
        return adapterConfig.getEipToMqttConfig().getPollingIntervalMillis();
    }

    @Override
    public int getMaxPollingErrorsBeforeRemoval() {
        return adapterConfig.getEipToMqttConfig().getMaxPollingErrorsBeforeRemoval();
    }

    /**
     * Use this hook method to modify the query generated used to read|subscribe to the devices,
     * for the most part this is simply the tagAddress field unchanged from the eipToMqttMapping
     * <p>
     * Default: tagAddress:expectedDataType eg. "0%20:BOOL"
     */
    protected @NotNull String createTagAddressForSubscription(
            @NotNull final EipToMqttMapping eipToMqttMapping, final @NotNull String address) {
        return String.format("%s%s%s", address, TAG_ADDRESS_TYPE_SEP, eipToMqttMapping.getDataType());
    }

}
