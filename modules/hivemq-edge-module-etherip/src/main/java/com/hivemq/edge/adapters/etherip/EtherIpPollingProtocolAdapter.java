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
import com.hivemq.adapter.sdk.api.data.DataPoint;
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

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;


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
            if (etherNetIP != null) {
                etherNetIP.close();
                etherNetIP = null;
                protocolAdapterStopOutput.stoppedSuccessfully();

                protocolAdapterState.setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STOPPED);
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
            final @NotNull PollingInput<EtherIpAdapterConfig.PollingContextImpl> pollingInput,
            final @NotNull PollingOutput pollingOutput) {

        if (etherNetIP == null) {
            return;
        }

        final String tagAddress = createTagAddressForSubscription(pollingInput.getPollingContext());
        try {
            final CIPData evt = etherNetIP.readTag(pollingInput.getPollingContext().getTagAddress());

            final List<Object> dataPoints = handleResult(evt, tagAddress);

            // FIXME check for
            // adapterConfig.getPublishChangedDataOnly()
            dataPoints.stream().filter(Objects::nonNull).forEach(it -> pollingOutput.addDataPoint(tagAddress, it));

            pollingOutput.finish();
        } catch (Exception e) {
            LOG.warn("An exception occurred while reading tag '{}'.", tagAddress, e);
            pollingOutput.fail(e, "An exception occurred while reading tag '" + tagAddress + "'.");
        }
    }

    private List<Object> handleResult(final CIPData evt, final String tagAddress) {
        if (evt.isNumeric()) {
            return handleNumeric(evt, tagAddress);
        } else {
            return handleString(evt, tagAddress);
        }
    }

    private List<Object> handleString(
            @NotNull final CIPData evt, @NotNull final String tagAddress) {
        try {
            return List.of(evt.getString());
        } catch (final Exception exception) {
            LOG.error("Unable to process tag '{}' of type string.", tagAddress, exception);
        }
        return null;
    }

    private List<Object> handleNumeric(final CIPData evt, final String tagAddress) {
        Object result = null;
        if (evt.getElementCount() > 1) {
            LOG.warn("Only one element per result is supported: {}", tagAddress);
        } else {
            final CIPData.Type dataType = evt.getType();

            try {
                final Number number = evt.getNumber(0);
                LOG.debug("Got value {} for type {} for tag address {}", number, dataType, tagAddress);
                switch (dataType) {
                    case BOOL:
                    case SINT:
                    case INT:
                        result = number.intValue();
                        break;
                    case DINT:
                        result = number.longValue();
                        break;
                    case REAL:
                        result = number.doubleValue();
                        break;
                    case BITS:
                        LOG.error("Data type BITS is not supported, yet: {} ", tagAddress);
                        break;
                    case STRUCT:
                        LOG.error("Data type STRUCT is not supported, yet: {} ", tagAddress);
                        break;
                    case STRUCT_STRING:
                        LOG.trace("This should not be reached, since the type is a number type: {}", tagAddress);
                        break;
                }
            } catch (final Exception exception) {
                LOG.error("Unable to process tag '{}' of type numeric.", tagAddress, exception);
            }
        }
        return List.of(result);
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
