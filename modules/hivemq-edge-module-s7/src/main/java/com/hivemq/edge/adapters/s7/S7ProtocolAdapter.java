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
package com.hivemq.edge.adapters.s7;

import com.github.xingshuangs.iot.protocol.s7.enums.EPlcType;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.polling.PollingInput;
import com.hivemq.adapter.sdk.api.polling.PollingOutput;
import com.hivemq.adapter.sdk.api.polling.PollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.adapters.s7.config.S7AdapterConfig;
import com.hivemq.edge.adapters.s7.config.S7DataType;
import com.hivemq.edge.adapters.s7.config.S7Tag;
import com.hivemq.edge.adapters.s7.config.S7TagDefinition;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author HiveMQ Adapter Generator
 */
public class S7ProtocolAdapter implements PollingProtocolAdapter {

    private static final Logger log = LoggerFactory.getLogger(S7ProtocolAdapter.class);

    private final ProtocolAdapterInformation adapterInformation;
    private final S7AdapterConfig adapterConfig;
    private final ProtocolAdapterState protocolAdapterState;
    private final S7Client s7Client;
    private final Map<String, S7Tag> tags;
    private final @NotNull String adapterId;

    private final Map<String, DataPoint> dataPoints;

    public S7ProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<S7AdapterConfig> input) {
        this.adapterId = input.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.tags = input.getTags().stream().map(t -> (S7Tag)t).collect(Collectors.toMap(S7Tag::getName, Function.identity()));
        this.protocolAdapterState = input.getProtocolAdapterState();
        final EPlcType eplcType = S7Client.getEplcType(adapterConfig.getControllerType());
        s7Client = new S7Client(
                eplcType,
                adapterConfig.getHost(),
                adapterConfig.getPort(),
                Objects.requireNonNullElse(adapterConfig.getRemoteRack(), eplcType.getRack()),
                Objects.requireNonNullElse(adapterConfig.getRemoteSlot(), eplcType.getSlot()),
                Objects.requireNonNullElse(adapterConfig.getPduLength(), eplcType.getPduLength()),
                input.adapterFactories().dataPointFactory());
        this.dataPoints = new ConcurrentHashMap<>();
    }

    @Override
    public int getPollingIntervalMillis() {
        return adapterConfig.getS7ToMqttConfig().getPollingIntervalMillis();
    }

    @Override
    public int getMaxPollingErrorsBeforeRemoval() {
        return adapterConfig.getS7ToMqttConfig().getMaxPollingErrorsBeforeRemoval();
    }

    @Override
    public @NotNull String getId() {
        return adapterId;
    }

    @Override
    public void start(
            @NotNull final ProtocolAdapterStartInput input,
            @NotNull final ProtocolAdapterStartOutput output) {
        log.info("Connecting to {}@{}:{}", adapterConfig.getControllerType(), adapterConfig.getHost(), adapterConfig.getPort());
        try {
            s7Client.connect();
            protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
            output.startedSuccessfully();
        } catch (final Exception e) {
            String msg = "Unable to connect to " + adapterConfig.getControllerType() + "@" + adapterConfig.getHost() + ":" + adapterConfig.getPort();
            protocolAdapterState.setErrorConnectionStatus(e, msg);
            output.failStart(e, msg);
        }
    }

    @Override
    public void stop(@NotNull final ProtocolAdapterStopInput input, @NotNull final ProtocolAdapterStopOutput output) {
        log.info("Closing connection to {}@{}:{}", adapterConfig.getControllerType(), adapterConfig.getHost(), adapterConfig.getPort());
        try {
            s7Client.disconnect();
            protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
            output.stoppedSuccessfully();
        } catch (final Exception e) {
            final String msg = "Unable to disconnect from " + adapterConfig.getControllerType() + "@" + adapterConfig.getHost() + ":" + adapterConfig.getPort();
            protocolAdapterState.setErrorConnectionStatus(e, msg);
            output.failStop(e, msg);
        }
    }

    @Override
    public void poll(@NotNull final PollingInput pollingInput, @NotNull final PollingOutput pollingOutput) {
        S7Tag tagToRead = tags.get(pollingInput.getPollingContext().getTagName());
        S7TagDefinition tagDefinition = tagToRead.getDefinition();
        //Every S7 address starts with a % but the iot-communications lib doesn't like it, so we are stripping it.
        final String tagAddress = tagDefinition.getAddress().replace("%","");
        final DataPoint dataPoint;
        if(tagDefinition.getDataType() == S7DataType.BYTE) {
            dataPoint = s7Client.readByte(tagAddress);
        } else {
            dataPoint = s7Client.read(tagDefinition.getDataType(), List.of(tagAddress)).get(0);
        }

        if(adapterConfig.getS7ToMqttConfig().getPublishChangedDataOnly()) {
            if(dataPoints.containsKey(tagAddress)) {
                final DataPoint existingDataPoint = dataPoints.get(tagAddress);
                if(existingDataPoint != null && existingDataPoint.equals(dataPoint)) {
                    if (log.isTraceEnabled()){
                        log.trace("Skipping sending for {} because publishChangedDataOnly=true", tagAddress);
                    }
                } else {
                    dataPoints.put(tagAddress, dataPoint);
                    pollingOutput.addDataPoint(dataPoint);
                }
            } else {
                dataPoints.put(tagAddress, dataPoint);
                pollingOutput.addDataPoint(dataPoint);
            }
        } else {
            pollingOutput.addDataPoint(dataPoint);
        }

        pollingOutput.finish();
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }
}
