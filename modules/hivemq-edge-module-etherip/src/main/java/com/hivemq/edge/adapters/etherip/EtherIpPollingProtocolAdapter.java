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
import com.hivemq.edge.adapters.etherip.model.EtherIpAdapterConfig;
import etherip.EtherNetIP;
import etherip.types.CIPData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.util.List;


public class EtherIpPollingProtocolAdapter implements PollingProtocolAdapter<EtherIpAdapterConfig.PollingContextImpl> {

    private static final @NotNull org.slf4j.Logger LOG = LoggerFactory.getLogger(EtherIpPollingProtocolAdapter.class);

    protected static final String TAG_ADDRESS_TYPE_SEP = ":";

    private final @NotNull EtherIpAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    protected final @NotNull AdapterFactories adapterFactories;
    private volatile @Nullable EtherNetIP etherNetIP;

    public EtherIpPollingProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<EtherIpAdapterConfig> input) {
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.adapterFactories = input.adapterFactories();
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
        } catch (Exception e) {
            output.failStart(e, null);
        }
    }

    @Override
    public void stop(
            final @NotNull ProtocolAdapterStopInput protocolAdapterStopInput,
            final @NotNull ProtocolAdapterStopOutput protocolAdapterStopOutput) {
        try {
            if(etherNetIP != null) {
                etherNetIP.close();
                protocolAdapterStopOutput.stoppedSuccessfully();
                LOG.info("Stopped");
            } else {
                protocolAdapterStopOutput.stoppedSuccessfully();
                LOG.info("Stopped without an open connection");
            }
        } catch (Exception e) {
            protocolAdapterStopOutput.failStop(e, "Unable to stop Ethernet IP connection");
            LOG.error("Unable to stop", e);
        }
    }


    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    @Override
    public void poll(
            final @NotNull PollingInput<EtherIpAdapterConfig.PollingContextImpl> pollingInput, final @NotNull PollingOutput pollingOutput) {
        final String tagAddress = createTagAddressForSubscription(pollingInput.getPollingContext());
        System.out.println(tagAddress);
        try {
            CIPData evt = etherNetIP.readTag(pollingInput.getPollingContext().getTagAddress());
            if(evt.isNumeric()) {
                int count = evt.getElementCount();
                for(int i = 0; i < count; i++) {
                    try {
                        System.out.println(evt.getType());
                        pollingOutput.addDataPoint(tagAddress, evt.getNumber(i));
                    } catch (Exception e) {
                        LOG.error("Unable to process tag '{}' of type numeric.", tagAddress, e);
                    }
                }
            } else {
                try {
                    pollingOutput.addDataPoint(tagAddress, evt.getString());
                } catch (Exception e) {
                    LOG.error("Unable to process tag '{}' of type string .", tagAddress, e);
                }
            }
            pollingOutput.finish();
        } catch (Exception e) {
            LOG.warn("An exception occurred while reading tag '{}'.", tagAddress, e);
            pollingOutput.fail(e, "An exception occurred while reading tag '" + tagAddress + "'.");
        }
    }

    @Override
    public @NotNull List<EtherIpAdapterConfig.PollingContextImpl> getPollingContexts() {
        return adapterConfig.getSubscriptions();
    }

    @Override
    public int getPollingIntervalMillis() {
        return adapterConfig.getPollingIntervalMillis();
    }

    @Override
    public int getMaxPollingErrorsBeforeRemoval() {
        return adapterConfig.getMaxPollingErrorsBeforeRemoval();
    }

    /**
     * Use this hook method to modify the query generated used to read|subscribe to the devices,
     * for the most part this is simply the tagAddress field unchanged from the subscription
     * <p>
     * Default: tagAddress:expectedDataType eg. "0%20:BOOL"
     */
    protected @NotNull String createTagAddressForSubscription(@NotNull final EtherIpAdapterConfig.PollingContextImpl subscription) {
        return String.format("%s%s%s", subscription.getTagAddress(), TAG_ADDRESS_TYPE_SEP, subscription.getDataType());
    }

}
