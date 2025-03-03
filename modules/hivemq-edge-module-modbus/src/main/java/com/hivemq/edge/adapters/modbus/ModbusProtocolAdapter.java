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
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingInput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.adapters.modbus.config.ModbusSpecificAdapterConfig;
import com.hivemq.edge.adapters.modbus.config.tag.ModbusTag;
import com.hivemq.edge.adapters.modbus.config.tag.ModbusTagDefinition;
import com.hivemq.edge.adapters.modbus.impl.ModbusClient;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.DISCONNECTED;

public class ModbusProtocolAdapter implements BatchPollingProtocolAdapter {
    private static final Logger log = LoggerFactory.getLogger(ModbusProtocolAdapter.class);
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ModbusSpecificAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterState protocolAdapterState;

    private final @NotNull ModbusClient modbusClient;
    private final @NotNull Map<String, Object> lastSamples = new ConcurrentHashMap<>();
    private final @NotNull List<ModbusTag> tags;
    private final @NotNull String adapterId;

    public ModbusProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<ModbusSpecificAdapterConfig> input) {
        this.adapterId = input.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.tags = input.getTags().stream().map(t -> (ModbusTag)t).toList();
        this.modbusClient =
                new ModbusClient(input.getAdapterId(), adapterConfig);
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

    public record ResulTuple(String tagName, Object value) {}

    @Override
    public void poll(
            final @NotNull BatchPollingInput pollingInput, final @NotNull BatchPollingOutput pollingOutput) {

        final var readRegisterFutures = tags.stream()
                .map(tag -> readRegisters(modbusClient, tag)
                        .thenApply(result -> new ResulTuple(tag.getName(), result)))
                .toList();

        CompletableFuture
                .allOf(readRegisterFutures.toArray(new CompletableFuture[]{}))
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Unable to read tags from modbus", throwable);
                        pollingOutput.fail(throwable, "Unable to read tags from modbus");
                    }
                    for (final CompletableFuture<ResulTuple> readRegisterFuture : readRegisterFutures) {
                        try {
                            final var entry = readRegisterFuture.get();
                            final String tagName = entry.tagName();
                            final Object dataPoint = lastSamples.get(tagName);
                            if (dataPoint != null) {
                                if (!dataPoint.equals(entry.value())) {
                                    //value changed, remember and forward
                                    lastSamples.put(tagName, entry.value());
                                    pollingOutput.addDataPoint(tagName, entry.value());
                                } else {
                                    //value didn't exist, remember and forward
                                    lastSamples.put(tagName, entry.value());
                                    pollingOutput.addDataPoint(tagName, entry.value());
                                }
                            } else {
                                //value didn't exist, remember and forward
                                lastSamples.put(tagName, entry.value());
                                pollingOutput.addDataPoint(tagName, entry.value());
                            }
                        } catch (final InterruptedException | ExecutionException e) {
                            log.error("Problem while accessing data in a completed future", e);
                            pollingOutput.fail(e,"Problem while accessing data in a completed future");
                            return;
                        }
                    }
                    pollingOutput.finish();
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

    protected @NotNull CompletableFuture<Object> readRegisters(
            final @NotNull ModbusClient modbusClient,
            final @NotNull ModbusTag modbusTag) {
        final ModbusTagDefinition modbusTagDefinition = modbusTag.getDefinition();

        final var startIdx = modbusTagDefinition.startIdx;
        final var unitId = modbusTagDefinition.unitId;
        final var flipRegisters= modbusTagDefinition.flipRegisters;
        final var dataType = modbusTagDefinition.getDataType();
        final var readType = modbusTagDefinition.readType;

        return switch (readType) {
            case HOLDING_REGISTERS -> modbusClient.readHoldingRegisters(startIdx, dataType, unitId, flipRegisters);
            case INPUT_REGISTERS -> modbusClient.readInputRegisters(startIdx, dataType, unitId, flipRegisters);
            case COILS -> modbusClient.readCoils(startIdx, unitId);
            case DISCRETE_INPUTS -> modbusClient.readDiscreteInput(startIdx, unitId);
        };
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
