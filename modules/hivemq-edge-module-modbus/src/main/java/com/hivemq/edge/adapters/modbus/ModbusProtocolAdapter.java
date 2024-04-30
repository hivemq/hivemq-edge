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

import com.codahale.metrics.MetricRegistry;
import com.hivemq.edge.adapters.modbus.impl.ModbusClient;
import com.hivemq.edge.adapters.modbus.model.ModBusData;
import com.hivemq.edge.adapters.modbus.util.AdapterDataUtils;
import com.hivemq.edge.modules.adapters.PollingPerSubscriptionProtocolAdapter;
import com.hivemq.edge.modules.adapters.data.DataPoint;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSample;
import com.hivemq.edge.modules.adapters.factories.AdapterFactories;
import com.hivemq.edge.modules.adapters.model.NodeTree;
import com.hivemq.edge.modules.adapters.model.NodeType;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterDiscoveryInput;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterDiscoveryOutput;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterInput;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterPollingSampler;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterStartInput;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterStartOutput;
import com.hivemq.edge.modules.api.adapters.ModuleServices;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterState;
import com.hivemq.edge.modules.config.AdapterSubscription;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.hivemq.edge.modules.api.adapters.ProtocolAdapterState.ConnectionStatus.CONNECTED;

public class ModbusProtocolAdapter implements PollingPerSubscriptionProtocolAdapter {
    private static final Logger log = LoggerFactory.getLogger(ModbusProtocolAdapter.class);
    private final @NotNull Object lock = new Object();
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ModbusAdapterConfig adapterConfig;
    private final @NotNull MetricRegistry metricRegistry;
    private final @NotNull String version;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull ModuleServices moduleServices;
    private final @NotNull AdapterFactories adapterFactories;

    private volatile @Nullable IModbusClient modbusClient;
    private @Nullable Map<ModbusAdapterConfig.AdapterSubscription, ProtocolAdapterDataSample> lastSamples =
            new HashMap<>();

    public ModbusProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ModbusAdapterConfig adapterConfig,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull ProtocolAdapterInput<ModbusAdapterConfig> input) {
        this.adapterInformation = adapterInformation;
        this.adapterConfig = adapterConfig;
        this.metricRegistry = metricRegistry;
        this.version = input.getVersion();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.moduleServices = input.moduleServices();
        this.adapterFactories = input.adapterFactories();
    }

    @Override
    public @NotNull CompletableFuture<ProtocolAdapterStartOutput> start(
            @NotNull final ProtocolAdapterStartInput input, @NotNull final ProtocolAdapterStartOutput output) {
        CompletableFuture<IModbusClient> startFuture = CompletableFuture.supplyAsync(this::initConnection);
        return startFuture.thenApply(connection -> output);
    }

    @Override
    public @NotNull CompletableFuture<Void> stop() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public @NotNull CompletableFuture<? extends ProtocolAdapterDataSample> poll(@NotNull final AdapterSubscription adapterSubscription) {

        //-- If a previously linked job has terminally disconnected the client
        //-- we need to ensure any orphaned jobs tidy themselves up properly
        try {
            if (modbusClient != null) {
                if (!modbusClient.isConnected()) {
                    modbusClient.connect()
                            .thenRun(() -> protocolAdapterState.setConnectionStatus(CONNECTED))
                            .get();
                }
                return CompletableFuture.supplyAsync(() -> readRegisters(adapterSubscription))
                        .thenApply(this::captureDataSample);
            } else {
                return CompletableFuture.failedFuture(new IllegalStateException("client not initialised"));
            }
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public @NotNull List<? extends AdapterSubscription> getSubscriptions() {
        return adapterConfig.getSubscriptions().stream().filter(sub -> sub.getAddressRange() != null).collect(Collectors.toList());
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
    public @NotNull CompletableFuture<Void> discoverValues(
            @NotNull ProtocolAdapterDiscoveryInput input, @NotNull ProtocolAdapterDiscoveryOutput output) {
        //-- Do the discovery of registers and coils, only for root level
        final NodeTree nodeTree = output.getNodeTree();
        if (input.getRootNode() == null) {
            nodeTree.addNode("holding-registers",
                    "Holding Registers",
                    "Holding Registers",
                    null,
                    NodeType.FOLDER,
                    false);
            nodeTree.addNode("coils", "Coils", "Coils", null, NodeType.FOLDER, false);
        }
        addAddresses(nodeTree, "holding-registers", 1, 256, 16);
        addAddresses(nodeTree, "coils", 1, 256, 16);

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    @Override
    public @Nullable String getErrorMessage() {
        //TODO
        return null;
    }

    protected @Nullable ProtocolAdapterDataSample captureDataSample(@NotNull final ProtocolAdapterDataSample data) {
        boolean publishData = true;
        if (log.isTraceEnabled()) {
            log.trace("Captured ModBus data with {} data points.", data.getDataPoints().size());
        }
        if (adapterConfig.getPublishChangedDataOnly()) {
            ModbusAdapterConfig.AdapterSubscription subscription =
                    (ModbusAdapterConfig.AdapterSubscription) data.getSubscription();
            ModBusData previousSample = (ModBusData) lastSamples.put(subscription, data);
            if (previousSample != null) {
                List<DataPoint> previousSampleDataPoints = previousSample.getDataPoints();
                List<DataPoint> currentSamplePoints = data.getDataPoints();
                List<DataPoint> delta =
                        AdapterDataUtils.mergeChangedSamples(previousSampleDataPoints, currentSamplePoints);
                if (log.isTraceEnabled()) {
                    log.trace("Calculating change data old {} samples, new {} sample, delta {}",
                            previousSampleDataPoints.size(),
                            currentSamplePoints.size(),
                            delta.size());
                }
                if (!delta.isEmpty()) {
                    data.setDataPoints(delta);
                } else {
                    publishData = false;
                }
            }
        }
        if (publishData) {
            if (log.isTraceEnabled()) {
                log.trace("Publishing data with {} samples", data.getDataPoints().size());
            }
            return data;
        }
        // TODO
        return null;
    }


    // TODO
    protected void onSamplerClosed(final @NotNull ProtocolAdapterPollingSampler sampler) {
        try {
            if (log.isTraceEnabled()) {
                log.trace("Sampler was closed by framework, disconnect Modbus device.");
            }
            if (modbusClient != null) {
                modbusClient.disconnect();
            }
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("Error encountered closing connection to Modbus device.", e);
            }
        }
    }

    protected @NotNull ModBusData readRegisters(@NotNull final AdapterSubscription sub) {
        try {
            ModbusAdapterConfig.AdapterSubscription subscription = (ModbusAdapterConfig.AdapterSubscription) sub;
            ModbusAdapterConfig.AddressRange addressRange = subscription.getAddressRange();
            Short[] registers = modbusClient.readHoldingRegisters(addressRange.startIdx,
                    addressRange.endIdx - addressRange.startIdx);
            ModBusData data = new ModBusData(subscription, ModBusData.TYPE.HOLDING_REGISTERS,
                    adapterFactories.dataPointFactory());
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
                    "",
                    parent,
                    NodeType.FOLDER,
                    false);
            parentNode = "grouping-" + startIdx;
        }
        for (int i = startIdx; i <= count; i++) {
            tree.addNode("address-location-" + i, String.valueOf(i), "", parentNode, NodeType.VALUE, true);
            if (i % groupIdx == 0 && i < count) {
                tree.addNode("grouping-" + i,
                        "Addresses " + (i + 1) + "-" + (i + groupIdx),
                        "",
                        parent,
                        NodeType.FOLDER,
                        false);
                parentNode = "grouping-" + i;
            }
        }
    }
}
