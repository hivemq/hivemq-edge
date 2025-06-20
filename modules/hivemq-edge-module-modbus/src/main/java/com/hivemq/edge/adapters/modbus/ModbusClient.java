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

import com.digitalpetri.modbus.client.ModbusTcpClient;
import com.digitalpetri.modbus.pdu.ReadCoilsRequest;
import com.digitalpetri.modbus.pdu.ReadDiscreteInputsRequest;
import com.digitalpetri.modbus.pdu.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.pdu.ReadInputRegistersRequest;
import com.digitalpetri.modbus.tcp.Netty;
import com.digitalpetri.modbus.tcp.client.NettyTcpClientTransport;
import com.digitalpetri.modbus.tcp.client.NettyTimeoutScheduler;
import com.hivemq.edge.adapters.modbus.config.ModbusDataType;
import com.hivemq.edge.adapters.modbus.config.ModbusSpecificAdapterConfig;
import com.hivemq.edge.adapters.modbus.config.tag.ModbusTag;
import com.hivemq.edge.adapters.modbus.config.tag.ModbusTagDefinition;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


class ModbusClient {

    private static final int DEFAULT_MAX_INPUT_REGISTERS = 125;
    private static final int DEFAULT_MAX_DISCRETE_INPUTS = 2000;

    private final @NotNull ModbusTcpClient client;

    ModbusClient(final @NotNull ModbusSpecificAdapterConfig adapterConfig) {
        client = ModbusTcpClient.create(NettyTcpClientTransport.create(cfg -> {
            cfg.hostname = adapterConfig.getHost();
            cfg.port = adapterConfig.getPort();
            cfg.connectTimeout = Duration.ofMillis(adapterConfig.getTimeoutMillis());
        }), cfg -> cfg.timeoutScheduler = new NettyTimeoutScheduler(Netty.sharedWheelTimer()));
    }

    @NotNull CompletionStage<Void> connect() {
        return client.connectAsync().thenApply(unused -> null);
    }

    @NotNull CompletionStage<Void> disconnect() {
        //-- If the client is manually disconnected before connection established ensure we still call into the client
        //-- to shut it all down.
        return client.disconnectAsync().thenApply(unused -> null);
    }

    boolean isConnected() {
        return client.isConnected();
    }

    @NotNull CompletionStage<Object> readRegisters(
            final @NotNull ModbusTag modbusTag) {
        final ModbusTagDefinition def = modbusTag.getDefinition();
        final var dataType = def.getDataType();
        return (switch (def.readType) {
            case HOLDING_REGISTERS -> readHoldingRegisters(def.startIdx, dataType, def.unitId, def.flipRegisters);
            case INPUT_REGISTERS -> readInputRegisters(def.startIdx, dataType, def.unitId, def.flipRegisters);
            case COILS -> readCoils(def.startIdx, def.unitId);
            case DISCRETE_INPUTS -> readDiscreteInput(def.startIdx, def.unitId);
        });
    }

    /**
     * Coils are 1bit.
     */
    @NotNull CompletionStage<Object> readCoils(final int startIdx, final int unitId) {
        if (!client.isConnected()) {
            return CompletableFuture.completedFuture(null);
        }

        return client.readCoilsAsync(unitId, new ReadCoilsRequest(startIdx, Math.min(1, DEFAULT_MAX_DISCRETE_INPUTS)))
                .thenApply(response -> ModbusDataType.BOOL.convert(response.coils(), false));
    }

    /**
     * Discrete registers are 1bit.
     */
    @NotNull CompletionStage<Object> readDiscreteInput(final int startIdx, final int unitId) {
        if (!client.isConnected()) {
            return CompletableFuture.completedFuture(null);
        }

        return client.readDiscreteInputsAsync(unitId,
                        new ReadDiscreteInputsRequest(startIdx, Math.min(1, DEFAULT_MAX_DISCRETE_INPUTS)))
                .thenApply(response -> ModbusDataType.BOOL.convert(response.inputs(), false));
    }

    /**
     * Holding registers are 16bit.
     */
    @NotNull CompletionStage<Object> readHoldingRegisters(
            final int startIdx,
            final @NotNull ModbusDataType dataType,
            final int unitId,
            final boolean flipRegisters) {
        if (!client.isConnected()) {
            return CompletableFuture.completedFuture(null);
        }

        return client.readHoldingRegistersAsync(unitId,
                        new ReadHoldingRegistersRequest(startIdx,
                                Math.min(dataType.nrOfRegistersToRead, DEFAULT_MAX_INPUT_REGISTERS)))
                .thenApply(response -> dataType.convert(response.registers(), flipRegisters));
    }

    /**
     * Inout registers are 16bit.
     */
    @NotNull CompletionStage<Object> readInputRegisters(
            final int startIdx,
            final @NotNull ModbusDataType dataType,
            final int unitId,
            final boolean flipRegisters) {
        if (!client.isConnected()) {
            return CompletableFuture.completedFuture(null);
        }

        return client.readInputRegistersAsync(unitId,
                        new ReadInputRegistersRequest(startIdx,
                                Math.min(dataType.nrOfRegistersToRead, DEFAULT_MAX_INPUT_REGISTERS)))
                .thenApply(response -> dataType.convert(response.registers(), flipRegisters));
    }
}
