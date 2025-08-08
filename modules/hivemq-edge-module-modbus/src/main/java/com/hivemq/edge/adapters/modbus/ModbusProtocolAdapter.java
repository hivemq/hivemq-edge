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
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingInput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.adapters.etherip.PublishChangedDataOnlyHandler;
import com.hivemq.edge.adapters.modbus.config.ModbusSpecificAdapterConfig;
import com.hivemq.edge.adapters.modbus.config.tag.ModbusTag;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.DISCONNECTED;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.ERROR;
import static com.hivemq.edge.adapters.modbus.config.ModbusToMqttConfig.DEFAULT_MAX_POLL_ERRORS_BEFORE_REMOVAL;
import static com.hivemq.edge.adapters.modbus.config.ModbusToMqttConfig.DEFAULT_POLL_INTERVAL_MILLIS;

public class ModbusProtocolAdapter implements BatchPollingProtocolAdapter {
    private static final Logger log = LoggerFactory.getLogger(ModbusProtocolAdapter.class);

    private static final @NotNull String NODE_ID_COILS = "coils";
    private static final @NotNull String NODE_ID_HOLDING_REGISTERS = "holding-registers";
    private static final int ADDRESS_START_IDX = 1;
    private static final int ADDRESS_GROUP_IDX = 16;
    private static final int ADDRESS_GROUP_MAX = 256;

    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ModbusSpecificAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull ModbusClient client;
    private final @NotNull PublishChangedDataOnlyHandler publishChangedDataOnlyHandler =
            new PublishChangedDataOnlyHandler();
    private final @NotNull List<ModbusTag> tags;
    private final @NotNull String adapterId;
    private final @NotNull DataPointFactory dataPointFactory;
    private final @NotNull AtomicBoolean stopRequested;
    private final @NotNull AtomicBoolean startRequested;

    public ModbusProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<ModbusSpecificAdapterConfig> input) {
        this.adapterId = input.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.dataPointFactory = input.adapterFactories().dataPointFactory();
        this.tags = input.getTags().stream().map(t -> (ModbusTag) t).toList();
        this.client = new ModbusClient(input.getConfig());
        this.stopRequested = new AtomicBoolean(false);
        this.startRequested = new AtomicBoolean(false);
    }

    private static void addAddresses(final @NotNull NodeTree tree, final @NotNull String parent) {
        tree.addNode("grouping-" + ADDRESS_START_IDX,
                "Addresses " + ADDRESS_START_IDX + "-" + (ADDRESS_START_IDX + ADDRESS_GROUP_IDX - 1),
                "",
                "",
                parent,
                NodeType.FOLDER,
                false);
        String parentNode = "grouping-" + ADDRESS_START_IDX;
        for (int i = ADDRESS_START_IDX; i <= ADDRESS_GROUP_MAX; i++) {
            tree.addNode("address-location-" + i,
                    String.valueOf(i),
                    String.valueOf(i),
                    "",
                    parentNode,
                    NodeType.VALUE,
                    true);
            if (i % ADDRESS_GROUP_IDX == 0 && i < ADDRESS_GROUP_MAX) {
                tree.addNode("grouping-" + i,
                        "Addresses " + (i + 1) + "-" + (i + ADDRESS_GROUP_IDX),
                        "",
                        "",
                        parent,
                        NodeType.FOLDER,
                        false);
                parentNode = "grouping-" + i;
            }
        }
    }

    @Override
    public void start(
            final @NotNull ProtocolAdapterStartInput input,
            final @NotNull ProtocolAdapterStartOutput output) {
        if (!stopRequested.get() && startRequested.compareAndSet(false, true)) {
            client.connect().whenComplete((unused, throwable) -> {
                if (throwable == null) {
                    protocolAdapterState.setConnectionStatus(CONNECTED);
                    output.startedSuccessfully();
                    log.info("Successfully started Modbus protocol adapter {}", adapterId);
                } else {
                    try {
                        protocolAdapterState.setConnectionStatus(ERROR);
                        output.failStart(throwable, "Exception during setup of Modbus client.");
                    } finally {
                        startRequested.set(false);
                        log.error("Unable to connect to the Modbus device", throwable);
                    }
                }
            });
        }
    }

    @Override
    public void stop(final @NotNull ProtocolAdapterStopInput input, final @NotNull ProtocolAdapterStopOutput output) {
        if (startRequested.get() && stopRequested.compareAndSet(false, true)) {
            log.info("Stopping Modbus protocol adapter {}", adapterId);
            publishChangedDataOnlyHandler.clear();
            client.disconnect().whenComplete((unused, throwable) -> {
                try {
                    if (throwable == null) {
                        protocolAdapterState.setConnectionStatus(DISCONNECTED);
                        output.stoppedSuccessfully();
                        log.info("Successfully stopped Modbus protocol adapter {}", adapterId);
                    } else {
                        protocolAdapterState.setConnectionStatus(ERROR);
                        output.failStop(throwable, "Error encountered closing connection to Modbus server.");
                        log.error("Unable to stop the connection to the Modbus server", throwable);
                    }
                } finally {
                    startRequested.set(false);
                    stopRequested.set(false);
                }
            });
        }
    }

    private boolean isPublishAllChanges() {
        final var toMqttConfig = adapterConfig.getModbusToMQTTConfig();
        return toMqttConfig == null || !toMqttConfig.getPublishChangedDataOnly();
    }

    @Override
    public void poll(final @NotNull BatchPollingInput pollingInput, final @NotNull BatchPollingOutput pollingOutput) {
        if (startRequested.get() && !stopRequested.get() && client.isConnected()) {

            final int limit = tags.size();
            final CompletableFuture<ResulTuple>[] readRegisterFutures = new CompletableFuture[limit];
            for (int i = 0; i < limit; i++) {
                final ModbusTag tag = tags.get(i);
                readRegisterFutures[i] = client.readRegisters(tag)
                        .thenApply(result -> new ResulTuple(tag.getName(), result))
                        .toCompletableFuture();
            }

            final boolean publishAllChanges = isPublishAllChanges();

            CompletableFuture.allOf(readRegisterFutures).whenComplete((result, throwable) -> {
                try {
                    if (throwable != null) {
                        protocolAdapterState.setConnectionStatus(ERROR);
                        pollingOutput.fail(throwable, "Unable to read tags from modbus");
                        return;
                    }

                    protocolAdapterState.setConnectionStatus(CONNECTED);

                    for (final CompletableFuture<ResulTuple> readRegisterFuture : readRegisterFutures) {
                        final ResulTuple entry = readRegisterFuture.join();
                        final var tagName = entry.tagName();
                        final var value = entry.value();
                        final var dataPoints = List.of(dataPointFactory.create(tagName, value));
                        if (publishAllChanges ||
                                publishChangedDataOnlyHandler.replaceIfValueIsNew(tagName, dataPoints)) {
                            dataPoints.forEach(pollingOutput::addDataPoint);
                        }
                    }
                } finally {
                    pollingOutput.finish();
                }
            });
        }
    }

    @Override
    public int getPollingIntervalMillis() {
        final var toMqttConfig = adapterConfig.getModbusToMQTTConfig();
        return toMqttConfig != null ? toMqttConfig.getPollingIntervalMillis() : DEFAULT_POLL_INTERVAL_MILLIS;
    }

    @Override
    public int getMaxPollingErrorsBeforeRemoval() {
        final var toMqttConfig = adapterConfig.getModbusToMQTTConfig();
        return toMqttConfig != null ?
                toMqttConfig.getMaxPollingErrorsBeforeRemoval() :
                DEFAULT_MAX_POLL_ERRORS_BEFORE_REMOVAL;
    }

    @Override
    public @NotNull String getId() {
        return adapterId;
    }

    @Override
    public void discoverValues(
            final @NotNull ProtocolAdapterDiscoveryInput input,
            final @NotNull ProtocolAdapterDiscoveryOutput output) {
        //-- Do the discovery of registers and coils, only for root level
        //-- Do the discovery of registers and coils, only for root level
        final NodeTree nodeTree = output.getNodeTree();
        if (input.getRootNode() == null) {
            nodeTree.addNode(NODE_ID_HOLDING_REGISTERS,
                    "Holding Registers",
                    "Holding Registers",
                    "Holding Registers",
                    null,
                    NodeType.FOLDER,
                    false);
            nodeTree.addNode(NODE_ID_COILS, "Coils", "Coils", NODE_ID_COILS, null, NodeType.FOLDER, false);
        }
        addAddresses(nodeTree, NODE_ID_HOLDING_REGISTERS);
        addAddresses(nodeTree, NODE_ID_COILS);
        output.finish();
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    private record ResulTuple(String tagName, Object value) {
    }
}
