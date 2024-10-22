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
import com.github.xingshuangs.iot.protocol.s7.service.S7PLC;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.config.PollingContext;
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
import com.hivemq.edge.adapters.plc4x.config.Plc4xDataType;
import com.hivemq.edge.adapters.plc4x.config.Plc4xToMqttMapping;
import com.hivemq.edge.adapters.plc4x.impl.AbstractPlc4xAdapter;
import com.hivemq.edge.adapters.plc4x.types.siemens.config.S7AdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.hivemq.edge.adapters.plc4x.config.Plc4xDataType.DATA_TYPE.*;

/**
 * @author HiveMQ Adapter Generator
 */
public class S7ProtocolAdapter implements PollingProtocolAdapter<S7AdapterConfig> {

    private static final Logger log = LoggerFactory.getLogger(S7ProtocolAdapter.class);

    private final ProtocolAdapterInformation adapterInformation;
    private final S7AdapterConfig adapterConfig;
    private final ProtocolAdapterState protocolAdapterState;
    private final PollingContext

    public S7ProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<S7AdapterConfig> input) {
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.pollingContext = adapterConfig.getFileToMqttConfig().getMappings();
    }

    @Override
    public void poll(
            @NotNull final PollingInput<S7AdapterConfig> pollingInput,
            @NotNull final PollingOutput pollingOutput) {
        S7PLC s7PLC = new S7PLC(EPlcType.S1200, "127.0.0.1");
        s7PLC.writeByte("DB2.1", (byte) 0x11);
        s7PLC.readByte("DB2.1");
        // close it manually, if you want to use it all the time, you do not need to close it
        s7PLC.close();
    }

    @Override
    public @NotNull List<S7AdapterConfig> getPollingContexts() {
        return List.of();
    }

    @Override
    public int getPollingIntervalMillis() {
        return adapterInformation;
    }

    @Override
    public int getMaxPollingErrorsBeforeRemoval() {
        return 0;
    }

    @Override
    public @NotNull String getId() {
        return "s7-new";
    }

    @Override
    public void start(
            @NotNull final ProtocolAdapterStartInput input,
            @NotNull final ProtocolAdapterStartOutput output) {
        log.error("REPLACE WITH AN ACTUAL IMPLEMENTATION!");
        output.startedSuccessfully();
    }

    @Override
    public void stop(@NotNull final ProtocolAdapterStopInput input, @NotNull final ProtocolAdapterStopOutput output) {
        log.error("REPLACE WITH AN ACTUAL IMPLEMENTATION!");
        output.stoppedSuccessfully();
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }
}
