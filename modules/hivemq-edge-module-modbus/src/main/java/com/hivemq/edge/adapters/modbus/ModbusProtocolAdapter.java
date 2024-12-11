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
import com.hivemq.edge.adapters.modbus.config.ModbusAdu;
import com.hivemq.edge.adapters.modbus.config.ModbusDataType;
import com.hivemq.edge.adapters.modbus.config.ModbusSpecificAdapterConfig;
import com.hivemq.edge.adapters.modbus.config.tag.ModbusTag;
import com.hivemq.edge.adapters.modbus.config.tag.ModbusTagDefinition;
import com.hivemq.edge.adapters.modbus.impl.ModbusClient;
import com.hivemq.edge.adapters.modbus.model.ModBusData;
import com.hivemq.edge.adapters.modbus.util.AdapterDataUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.DISCONNECTED;

public class ModbusProtocolAdapter implements PollingProtocolAdapter {
    private static final Logger log = LoggerFactory.getLogger(ModbusProtocolAdapter.class);
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ModbusSpecificAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterState protocolAdapterState;

    private final @NotNull ModbusClient modbusClient;
    private final @NotNull Map<PollingContext, List<DataPoint>> lastSamples = new ConcurrentHashMap<>();
    private final @NotNull List<Tag> tags;
    private final @NotNull String adapterId;

    public ModbusProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<ModbusSpecificAdapterConfig> input) {
        this.adapterId = input.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.tags = input.getTags();
        this.modbusClient =
                new ModbusClient(input.getAdapterId(), adapterConfig, input.adapterFactories().dataPointFactory());
    }

    @Override
    public void start(
            final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
        modbusClient.connect().whenComplete((unused, throwable) -> {
            if (throwable == null) {
                output.startedSuccessfully();
                protocolAdapterState.setConnectionStatus(CONNECTED);
            } else {
                output.failStart(throwable, "Exception during setup of Modbus client.");
            }
        });
    }

    @Override
    public void stop(final @NotNull ProtocolAdapterStopInput input, final @NotNull ProtocolAdapterStopOutput output) {
        lastSamples.clear();
        modbusClient.disconnect().whenComplete((unused, t) -> {
            if (t == null) {
                output.stoppedSuccessfully();
                protocolAdapterState.setConnectionStatus(DISCONNECTED);
            } else {
                output.failStop(t, "Error encountered closing connection to Modbus device.");
            }
        });
    }

    @Override
    public void poll(
            final @NotNull PollingInput pollingInput, final @NotNull PollingOutput pollingOutput) {
        tags.stream()
                .filter(tag -> tag.getName().equals(pollingInput.getPollingContext().getTagName()))
                .findFirst()
                .ifPresentOrElse(def -> pollModbus(pollingInput, pollingOutput, (ModbusTag) def),
                        () -> pollingOutput.fail("Polling for protocol adapter failed because the used tag '" +
                                pollingInput.getPollingContext().getTagName() +
                                "' was not found. For the polling to work the tag must be created via REST API or the UI."));
    }

    private void pollModbus(
            final @NotNull PollingInput pollingInput, final @NotNull PollingOutput pollingOutput, final @NotNull ModbusTag modbusTag) {
        readRegisters(pollingInput.getPollingContext(),
                modbusClient,
                modbusTag).whenComplete((modbusdata, throwable) -> {
            if (throwable != null) {
                pollingOutput.fail(throwable, null);
            } else {
                this.captureDataSample(modbusdata, pollingOutput);
            }
        });
    }

    @Override
    public int getPollingIntervalMillis() {
        return adapterConfig.getModbusToMQTTConfig().getPollingIntervalMillis();
    }

    @Override
    public int getMaxPollingErrorsBeforeRemoval() {
        return adapterConfig.getModbusToMQTTConfig().getMaxPollingErrorsBeforeRemoval();
    }

    @Override
    public @NotNull String getId() {
        return adapterId;
    }


    @Override
    public void discoverValues(
            final @NotNull ProtocolAdapterDiscoveryInput input, final @NotNull ProtocolAdapterDiscoveryOutput output) {
        //-- Do the discovery of registers and coils, only for root level
        final NodeTree nodeTree = output.getNodeTree();
        if (input.getRootNode() == null) {
            nodeTree.addNode("holding-registers",
                    "Holding Registers",
                    "Holding Registers",
                    "Holding Registers",
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

    protected void captureDataSample(final @NotNull ModBusData modBusData, final @NotNull PollingOutput pollingOutput) {
        if (log.isTraceEnabled()) {
            log.trace("Captured ModBus data with {} data points.", modBusData.getDataPoints().size());
        }
        if (!adapterConfig.getModbusToMQTTConfig().getPublishChangedDataOnly()) {
            modBusData.getDataPoints().forEach(pollingOutput::addDataPoint);
            if (log.isTraceEnabled()) {
                log.trace("Publishing data with {} samples", modBusData.getDataPoints().size());
            }
        } else {
            calculateDelta(modBusData, pollingOutput);
        }
        pollingOutput.finish();
    }

    private void calculateDelta(final @NotNull ModBusData modBusData, final @NotNull PollingOutput pollingOutput) {
        final PollingContext pollingContext = modBusData.getPollingContext();

        final List<DataPoint> previousSampleDataPoints = lastSamples.put(pollingContext, modBusData.getDataPoints());
        final List<DataPoint> currentSamplePoints = modBusData.getDataPoints();
        final List<DataPoint> delta =
                AdapterDataUtils.mergeChangedSamples(previousSampleDataPoints, currentSamplePoints);
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

    protected @NotNull CompletableFuture<ModBusData> readRegisters(
            final @NotNull PollingContext pollingContext,
            final @NotNull ModbusClient modbusClient,
            final @NotNull ModbusTag modbusTag) {
        final ModbusTagDefinition modbusTagDefinition = modbusTag.getDefinition();

        return doRead(modbusTagDefinition.startIdx,
                modbusTagDefinition.unitId,
                modbusTagDefinition.flipRegisters,
                modbusTag.getDefinition().getDataType(),
                modbusTagDefinition.readType,
                modbusClient).thenApply(dataPoint -> {
            final ModBusData data = new ModBusData(pollingContext);
            data.addDataPoint(dataPoint);
            return data;
        });
    }

    protected static @NotNull CompletableFuture<DataPoint> doRead(
            final int startIdx,
            final int unitId,
            final boolean flipRegisters,
            final @NotNull ModbusDataType dataType,
            final @NotNull ModbusAdu readType,
            final @NotNull ModbusClient modbusClient) {
        switch (readType) {
            case HOLDING_REGISTERS:
                return modbusClient.readHoldingRegisters(startIdx, dataType, unitId, flipRegisters);
            case INPUT_REGISTERS:
                return modbusClient.readInputRegisters(startIdx, dataType, unitId, flipRegisters);
            case COILS:
                return modbusClient.readCoils(startIdx, unitId);
            case DISCRETE_INPUTS:
                return modbusClient.readDiscreteInput(startIdx, unitId);
            default:
                return CompletableFuture.failedFuture(new Exception("Unknown read type " + readType));
        }
    }

    private static void addAddresses(
            final @NotNull NodeTree tree,
            final @NotNull String parent,
            final int startIdx,
            final int count,
            final int groupIdx) {

        String parentNode = parent;
        if (groupIdx < count) {
            tree.addNode("grouping-" + startIdx,
                    "Addresses " + startIdx + "-" + (startIdx + groupIdx - 1),
                    "",
                    "",
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
                        "",
                        "",
                        parent,
                        NodeType.FOLDER,
                        false);
                parentNode = "grouping-" + i;
            }
        }
    }
}
