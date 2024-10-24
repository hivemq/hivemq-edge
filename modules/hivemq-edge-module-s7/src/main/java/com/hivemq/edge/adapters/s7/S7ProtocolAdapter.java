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
import com.hivemq.edge.adapters.s7.config.S7ToMqttConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author HiveMQ Adapter Generator
 */
public class S7ProtocolAdapter implements PollingProtocolAdapter<S7ToMqttConfig> {

    private static final Logger log = LoggerFactory.getLogger(S7ProtocolAdapter.class);

    private final ProtocolAdapterInformation adapterInformation;
    private final S7AdapterConfig adapterConfig;
    private final ProtocolAdapterState protocolAdapterState;
    private final S7Client s7Client;

    private final Map<String, DataPoint> dataPoints;

    public S7ProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<S7AdapterConfig> input) {
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
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
    public @NotNull List<S7ToMqttConfig> getPollingContexts() {
        return adapterConfig.getS7ToMqttMappings();
    }

    @Override
    public int getPollingIntervalMillis() {
        return adapterConfig.getPollingIntervalMillis();
    }

    @Override
    public int getMaxPollingErrorsBeforeRemoval() {
        return adapterConfig.getMaxPollingErrorsBeforeRemoval();
    }

    @Override
    public @NotNull String getId() {
        return adapterConfig.getId();
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
    public void poll(
            @NotNull final PollingInput<S7ToMqttConfig> pollingInput,
            @NotNull final PollingOutput pollingOutput) {
        final S7ToMqttConfig s7ToMqtt = pollingInput.getPollingContext();
        //Every S7 address starts with a % but the iot-communications lib doesn't like it so we are stripping it.
        final String tagAddress = s7ToMqtt.getTagAddress().replace("%","");
        final DataPoint dataPoint;

        if(s7ToMqtt.getDataType() == S7DataType.BYTE) {
            dataPoint = s7Client.readByte(tagAddress);
        } else {
            dataPoint = s7Client.read(s7ToMqtt.getDataType(), List.of(tagAddress)).get(0);
        }

        if(adapterConfig.getPublishChangedDataOnly() && dataPoints.containsKey(tagAddress)) {
            final DataPoint existingDataPoint = dataPoints.get(tagAddress);
            if(existingDataPoint != null && !existingDataPoint.equals(dataPoint)) {
                dataPoints.put(tagAddress, dataPoint);
                pollingOutput.addDataPoint(dataPoint);
            } else {
                log.debug("Skipping sending for {} because publishChangedDataOnly=true", tagAddress);
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
