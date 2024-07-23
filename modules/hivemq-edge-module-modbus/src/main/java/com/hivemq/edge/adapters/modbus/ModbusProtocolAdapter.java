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

import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.discovery.NodeTree;
import com.hivemq.adapter.sdk.api.discovery.NodeType;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryInput;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryOutput;
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
import com.hivemq.edge.adapters.modbus.impl.ModbusClient;
import com.hivemq.edge.adapters.modbus.model.ModBusData;
import com.hivemq.edge.adapters.modbus.util.AdapterDataUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;

public class ModbusProtocolAdapter implements PollingProtocolAdapter<ModbusAdapterConfig.PollingContextImpl> {
    private static final Logger log = LoggerFactory.getLogger(ModbusProtocolAdapter.class);
    private final @NotNull Object lock = new Object();
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ModbusAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull AdapterFactories adapterFactories;

    private volatile @Nullable IModbusClient modbusClient;
    private final @NotNull Map<ModbusAdapterConfig.PollingContextImpl, List<DataPoint>> lastSamples = new HashMap<>();

    public ModbusProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ModbusAdapterConfig adapterConfig,
            final @NotNull ProtocolAdapterInput<ModbusAdapterConfig> input) {
        this.adapterInformation = adapterInformation;
        this.adapterConfig = adapterConfig;
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.adapterFactories = input.adapterFactories();
    }

    @Override
    public void start(
            @NotNull final ProtocolAdapterStartInput input, @NotNull final ProtocolAdapterStartOutput output) {
        try {
            initConnection();
            output.startedSuccessfully();
        } catch (Exception e) {
            output.failStart(e, "Exception during setup of Modbus client.");
        }

    }

    @Override
    public void stop(@NotNull final ProtocolAdapterStopInput input, @NotNull final ProtocolAdapterStopOutput output) {
        try {
            if (modbusClient != null) {
                modbusClient.disconnect();
            }
        } catch (Exception e) {
                output.failStop(e, "Error encountered closing connection to Modbus device.");
                return;
        }
        output.stoppedSuccessfully();
    }

    @Override
    public void poll(
            final @NotNull PollingInput<ModbusAdapterConfig.PollingContextImpl> pollingInput, final @NotNull PollingOutput pollingOutput) {

        //-- If a previously linked job has terminally disconnected the client
        //-- we need to ensure any orphaned jobs tidy themselves up properly
        try {
            if (modbusClient != null) {
                if (!modbusClient.isConnected()) {
                    modbusClient.connect().thenRun(() -> protocolAdapterState.setConnectionStatus(CONNECTED)).get();
                }
                CompletableFuture.supplyAsync(() -> readRegisters(pollingInput.getPollingContext()))
                        .whenComplete((modbusdata, throwable) -> {
                            if (throwable != null) {
                                pollingOutput.fail(throwable, null);
                            } else {
                                this.captureDataSample(modbusdata, pollingOutput);
                            }
                        });
            } else {
                pollingOutput.fail(new IllegalStateException("client not initialised"),"The client is not initialised.");
            }
        } catch (Exception e) {
            pollingOutput.fail(e, null);
        }
    }

    @Override
    public @NotNull List<ModbusAdapterConfig.PollingContextImpl> getPollingContexts() {
        return new ArrayList<>(adapterConfig.getSubscriptions());
    }

    @Override
    public int getPollingIntervalMillis() {
        return adapterConfig.getPollingIntervalMillis();
    }

    @Override
    public int getMaxPollingErrorsBeforeRemoval() {
        return adapterConfig.getMaxPollingErrorsBeforeRemoval();
    }


    private @NotNull IModbusClient initConnection() {
        if (modbusClient == null) {
            synchronized (lock) {
                if (modbusClient == null) {
                    if (log.isTraceEnabled()) {
                        log.trace("Creating new instance of Modbus Client with {}.", adapterConfig);
                    }
                    modbusClient = new ModbusClient(adapterConfig);
                }
            }
        }
        return modbusClient;
    }

    @Override
    public @NotNull String getId() {
        return adapterConfig.getId();
    }


    @Override
    public void discoverValues(
            @NotNull ProtocolAdapterDiscoveryInput input, @NotNull ProtocolAdapterDiscoveryOutput output) {
        //-- Do the discovery of registers and coils, only for root level
        final NodeTree nodeTree = output.getNodeTree();
        if (input.getRootNode() == null) {
            nodeTree.addNode("holding-registers",
                    "Holding Registers",
                    "Holding Registers", "Holding Registers",
                    null,
                    NodeType.FOLDER,
                    false);
            nodeTree.addNode("coils", "Coils", "Coils", "coils", null, NodeType.FOLDER, false);
        }
        addAddresses(nodeTree, "holding-registers", 1, 256, 16);
        addAddresses(nodeTree, "coils", 1, 256, 16);
        output.finish();
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    protected void captureDataSample(final @NotNull ModBusData modBusData, @NotNull final PollingOutput pollingOutput) {
        if (log.isTraceEnabled()) {
            log.trace("Captured ModBus data with {} data points.", modBusData.getDataPoints().size());
        }
        if (!adapterConfig.getPublishChangedDataOnly()) {
            modBusData.getDataPoints().forEach(pollingOutput::addDataPoint);
            if (log.isTraceEnabled()) {
                log.trace("Publishing data with {} samples", modBusData.getDataPoints().size());
            }
        } else {
            calculateDelta(modBusData, pollingOutput);
        }
        pollingOutput.finish();
    }

    private void calculateDelta(@NotNull ModBusData modBusData, @NotNull PollingOutput pollingOutput) {
        ModbusAdapterConfig.PollingContextImpl subscription =
                (ModbusAdapterConfig.PollingContextImpl) modBusData.getPollingContext();

        List<DataPoint> previousSampleDataPoints = lastSamples.put(subscription, modBusData.getDataPoints());
        List<DataPoint> currentSamplePoints = modBusData.getDataPoints();
        List<DataPoint> delta = AdapterDataUtils.mergeChangedSamples(previousSampleDataPoints, currentSamplePoints);
        if (log.isTraceEnabled()) {
            log.trace("Calculating change data old {} samples, new {} sample, delta {}",
                    previousSampleDataPoints != null ? previousSampleDataPoints.size() : 0,
                    currentSamplePoints.size(),
                    delta.size());
        }
        delta.forEach(pollingOutput::addDataPoint);
        if (log.isTraceEnabled() && !delta.isEmpty()) {
            log.trace("Publishing data with {} samples", delta.size());
        }
    }

    protected @NotNull ModBusData readRegisters(@NotNull final PollingContext sub) {
        try {
            ModbusAdapterConfig.PollingContextImpl subscription = (ModbusAdapterConfig.PollingContextImpl) sub;
            ModbusAdapterConfig.AddressRange addressRange = subscription.getAddressRange();
            Short[] registers = modbusClient.readHoldingRegisters(addressRange.startIdx,
                    addressRange.endIdx - addressRange.startIdx);
            ModBusData data = new ModBusData(subscription, adapterFactories.dataPointFactory());
            //add data point per register
            for (int i = 0; i < registers.length; i++) {
                data.addDataPoint("register-" + (addressRange.startIdx + i), registers[i]);
            }
            return data;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void addAddresses(
            @NotNull NodeTree tree, @NotNull String parent, int startIdx, int count, int groupIdx) {

        String parentNode = parent;
        if (groupIdx < count) {
            tree.addNode("grouping-" + startIdx,
                    "Addresses " + startIdx + "-" + (startIdx + groupIdx - 1),
                    "", "",
                    parent,
                    NodeType.FOLDER,
                    false);
            parentNode = "grouping-" + startIdx;
        }
        for (int i = startIdx; i <= count; i++) {
            tree.addNode("address-location-" + i,
                    String.valueOf(i),
                    String.valueOf(i),
                    "",
                    parentNode,
                    NodeType.VALUE,
                    true);
            if (i % groupIdx == 0 && i < count) {
                tree.addNode("grouping-" + i,
                        "Addresses " + (i + 1) + "-" + (i + groupIdx),
                        "", "",
                        parent,
                        NodeType.FOLDER,
                        false);
                parentNode = "grouping-" + i;
            }
        }
    }
}
