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
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSample;
import com.hivemq.edge.modules.adapters.impl.AbstractPollingPerSubscriptionAdapter;
import com.hivemq.edge.modules.adapters.model.NodeTree;
import com.hivemq.edge.modules.adapters.model.NodeType;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterDiscoveryInput;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterDiscoveryOutput;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterPollingSampler;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterStartOutput;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.edge.modules.config.impl.AbstractProtocolAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.handler.publish.PublishReturnCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ModbusProtocolAdapter extends AbstractPollingPerSubscriptionAdapter<ModbusAdapterConfig, ModBusData> {
    private static final Logger log = LoggerFactory.getLogger(ModbusProtocolAdapter.class);
    private final @NotNull Object lock = new Object();
    private volatile @Nullable IModbusClient modbusClient;
    private @Nullable Map<ModBusData.TYPE, ModBusData> lastSamples = new HashMap<>();

    public ModbusProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ModbusAdapterConfig adapterConfig,
            final @NotNull MetricRegistry metricRegistry) {
        super(adapterInformation, adapterConfig, metricRegistry);
    }

    @Override
    protected CompletableFuture<ProtocolAdapterStartOutput> startInternal( final @NotNull ProtocolAdapterStartOutput output) {
        CompletableFuture<IModbusClient> startFuture = CompletableFuture.supplyAsync(() -> initConnection());
        startFuture.thenAccept(this::subscribeAllInternal);
        return startFuture.thenApply(connection -> output);
    }

    private IModbusClient initConnection() {
        if (modbusClient == null) {
            synchronized (lock) {
                if (modbusClient == null) {
                    log.info("Creating new Instance Of ModbusClient with {}", adapterConfig);
                    modbusClient = new ModbusClient(adapterConfig);
                }
            }
        }
        return modbusClient;
    }

    @Override
    protected CompletableFuture<Void> stopInternal() {
        return CompletableFuture.completedFuture(null);
    }

    protected void subscribeAllInternal(@NotNull final IModbusClient client) throws RuntimeException {
        if (adapterConfig.getSubscriptions() != null) {
            for (ModbusAdapterConfig.Subscription subscription : adapterConfig.getSubscriptions()) {
                subscribeInternal(client, subscription);
            }
        }
    }

    protected void subscribeInternal(@NotNull final IModbusClient client, final @NotNull ModbusAdapterConfig.Subscription subscription) {
        if (subscription != null) {
            ModbusAdapterConfig.AddressRange registerAddressRange = subscription.getAddressRange();
            if (registerAddressRange != null) {
                startPolling(new SubscriptionSampler(this.adapterConfig, subscription));
            }
        }
    }

    @Override
    public CompletableFuture<Void> discoverValues(@NotNull ProtocolAdapterDiscoveryInput input, @NotNull ProtocolAdapterDiscoveryOutput output) {
        //-- Do the discovery of registers and coils, only for root level
        final NodeTree nodeTree = output.getNodeTree();
        if (input.getRootNode() == null) {
            nodeTree.addNode("holding-registers", "Holding Registers", "Holding Registers", null, NodeType.FOLDER, false);
            nodeTree.addNode("coils", "Coils", "Coils", null, NodeType.FOLDER, false);
        }
        addAddresses(nodeTree, "holding-registers", 1, 256, 16);
        addAddresses(nodeTree, "coils", 1, 256, 16);

        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletableFuture<?> captureDataSample(@NotNull final ModBusData data) {
        boolean publishData = true;
        if (adapterConfig.getPublishChangedDataOnly()) {
            ModBusData previousSample = lastSamples.put(data.getType(), data);
            if (previousSample != null) {
                List<ProtocolAdapterDataSample.DataPoint> dataPoints = previousSample.getDataPoints();
                publishData = !dataPoints.equals(data.getDataPoints());
            }
        }
        if (publishData) {
            return super.captureDataSample(data);
        }
        return CompletableFuture.completedFuture(null);
    }


    @Override
    protected void onSamplerClosed(final ProtocolAdapterPollingSampler sampler) {
        try {
            if(log.isInfoEnabled()){
                log.info("Sampler was closed by framework, disconnect modbus device");
            }
            if(modbusClient != null){
                modbusClient.disconnect();
            }
        } catch(Exception e){
            if(log.isWarnEnabled()){
                log.warn("Error encountered closing connection to modbus device", e);
            }
        }
    }

    @Override
    protected CompletableFuture<ModBusData> onSamplerInvoked(
            final ModbusAdapterConfig config,
            final AbstractProtocolAdapterConfig.Subscription subscription) {

        //-- If a previously linked job has terminally disconnected the client
        //-- we need to ensure any orphaned jobs tidy themselves up properly
        try {
            if(modbusClient != null){
                if (!modbusClient.isConnected()) {
                    modbusClient.connect().thenRun(() ->
                                setConnectionStatus(ConnectionStatus.CONNECTED)).get();
                }
                return CompletableFuture.supplyAsync(() -> readRegisters(subscription));
            } else {
                return CompletableFuture.failedFuture(new IllegalStateException("client not initialised"));
            }
        } catch(Exception e){
            return CompletableFuture.failedFuture(e);
        }
    }

    protected ModBusData readRegisters(@NotNull final AbstractProtocolAdapterConfig.Subscription sub) {
        try {
            ModbusAdapterConfig.Subscription subscription = (ModbusAdapterConfig.Subscription) sub;
            ModbusAdapterConfig.AddressRange addressRange = subscription.getAddressRange();
            Short[] registers = modbusClient.readHoldingRegisters(addressRange.startIdx,
                    addressRange.endIdx - addressRange.startIdx);
            ModBusData data = new ModBusData(subscription, ModBusData.TYPE.HOLDING_REGISTERS);
            //add data point per register
            for (int i = 0; i < registers.length; i++){
                data.addDataPoint("register-"+(addressRange.startIdx + i), registers[i]);
            }
            return data;
        } catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    private static void addAddresses(NodeTree tree, String parent, int startIdx, int count, int groupIdx) {

        String parentNode = parent;
        if (groupIdx < count) {
            tree.addNode("grouping-" + startIdx,
                    "Addresses " + startIdx + "-" + (startIdx + groupIdx - 1), "", parent, NodeType.FOLDER, false);
            parentNode = "grouping-" + startIdx;
        }
        for (int i = startIdx; i <= count; i++) {
            tree.addNode("address-location-" + i, String.valueOf(i), "", parentNode, NodeType.VALUE, true);
            if (i % groupIdx == 0 && i < count) {
                tree.addNode("grouping-" + i, "Addresses " + (i + 1) + "-" + (i + groupIdx), "", parent, NodeType.FOLDER, false);
                parentNode = "grouping-" + i;
            }
        }
    }
}
