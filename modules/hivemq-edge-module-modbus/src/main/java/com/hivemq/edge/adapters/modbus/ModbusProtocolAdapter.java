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
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.discovery.NodeTree;
import com.hivemq.adapter.sdk.api.discovery.NodeType;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryInput;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryOutput;
import com.hivemq.adapter.sdk.api.exceptions.TagDefinitionParseException;
import com.hivemq.adapter.sdk.api.exceptions.TagNotFoundException;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.polling.PollingInput;
import com.hivemq.adapter.sdk.api.polling.PollingOutput;
import com.hivemq.adapter.sdk.api.polling.PollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterTagService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.modbus.config.ModbusAdapterConfig;
import com.hivemq.edge.adapters.modbus.config.ModbusAdu;
import com.hivemq.edge.adapters.modbus.config.ModbusDataType;
import com.hivemq.edge.adapters.modbus.config.ModbusToMqttMapping;
import com.hivemq.edge.adapters.modbus.config.tag.ModbusTagDefinition;
import com.hivemq.edge.adapters.modbus.impl.ModbusClient;
import com.hivemq.edge.adapters.modbus.model.ModBusData;
import com.hivemq.edge.adapters.modbus.util.AdapterDataUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.DISCONNECTED;

public class ModbusProtocolAdapter implements PollingProtocolAdapter<ModbusToMqttMapping> {
    private static final Logger log = LoggerFactory.getLogger(ModbusProtocolAdapter.class);
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ModbusAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterState protocolAdapterState;

    private final @NotNull ModbusClient modbusClient;
    private final @NotNull Map<ModbusToMqttMapping, List<DataPoint>> lastSamples = new HashMap<>();
    private final @NotNull ProtocolAdapterTagService protocolAdapterTagService;

    public ModbusProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ModbusAdapterConfig adapterConfig,
            final @NotNull ProtocolAdapterInput<ModbusAdapterConfig> input) {
        this.adapterInformation = adapterInformation;
        this.adapterConfig = adapterConfig;
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.protocolAdapterTagService = input.moduleServices().protocolAdapterTagService();
        this.modbusClient = new ModbusClient(adapterConfig, input.adapterFactories().dataPointFactory());
    }

    @Override
    public void start(
            @NotNull final ProtocolAdapterStartInput input,
            @NotNull final ProtocolAdapterStartOutput output) {
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
    public void stop(@NotNull final ProtocolAdapterStopInput input, @NotNull final ProtocolAdapterStopOutput output) {
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
            final @NotNull PollingInput<ModbusToMqttMapping> pollingInput, final @NotNull PollingOutput pollingOutput) {
        adapterConfig.getTags().stream()
                .filter(tag -> tag.getTagName().equals(pollingInput.getPollingContext().getTagName()))
                .findFirst()
                .ifPresentOrElse(
                        def -> pollModbus(pollingInput, pollingOutput, def),
                        () -> pollingOutput.fail("Polling for protocol adapter failed because the used tag '" +
                                pollingInput.getPollingContext().getTagName() +
                                "' was not found. For the polling to work the tag must be created via REST API or the UI.")
                );
    }

    private void pollModbus(
            @NotNull PollingInput<ModbusToMqttMapping> pollingInput,
            @NotNull PollingOutput pollingOutput,
            Tag<ModbusTagDefinition> modbusTag) {
        readRegisters(pollingInput.getPollingContext(),
                modbusClient, modbusTag).whenComplete((modbusdata, throwable) -> {
            if (throwable != null) {
                pollingOutput.fail(throwable, null);
            } else {
                this.captureDataSample(modbusdata, pollingOutput);
            }
        });
    }

    @Override
    public @NotNull List<ModbusToMqttMapping> getPollingContexts() {
        return new ArrayList<>(adapterConfig.getModbusToMQTTConfig().getMappings());
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
        return adapterConfig.getId();
    }


    @Override
    public void discoverValues(
            @NotNull final ProtocolAdapterDiscoveryInput input, @NotNull final ProtocolAdapterDiscoveryOutput output) {
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

    protected void captureDataSample(final @NotNull ModBusData modBusData, @NotNull final PollingOutput pollingOutput) {
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

    private void calculateDelta(@NotNull final ModBusData modBusData, @NotNull final PollingOutput pollingOutput) {
        final ModbusToMqttMapping subscription = (ModbusToMqttMapping) modBusData.getPollingContext();

        final List<DataPoint> previousSampleDataPoints = lastSamples.put(subscription, modBusData.getDataPoints());
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
            final @NotNull ModbusToMqttMapping modbusToMqttMapping,
            final @NotNull ModbusClient modbusClient,
            final @NotNull Tag<ModbusTagDefinition> modbusTag) {
        final ModbusTagDefinition modbusTagDefinition = modbusTag.getTagDefinition();

        return doRead(modbusTagDefinition.startIdx,
                modbusTagDefinition.unitId,
                modbusTagDefinition.flipRegisters,
                modbusTag.getTagDefinition().getDataType(),
                modbusTagDefinition.readType,
                modbusClient).thenApply(dataPoint -> {
            final ModBusData data = new ModBusData(modbusToMqttMapping);
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
            @NotNull final NodeTree tree,
            @NotNull final String parent,
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
