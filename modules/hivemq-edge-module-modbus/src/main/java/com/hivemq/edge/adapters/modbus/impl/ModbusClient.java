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
package com.hivemq.edge.adapters.modbus.impl;

import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.master.ModbusTcpMasterConfig;
import com.digitalpetri.modbus.requests.ReadCoilsRequest;
import com.digitalpetri.modbus.requests.ReadDiscreteInputsRequest;
import com.digitalpetri.modbus.requests.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.requests.ReadInputRegistersRequest;
import com.digitalpetri.modbus.responses.ReadCoilsResponse;
import com.digitalpetri.modbus.responses.ReadDiscreteInputsResponse;
import com.digitalpetri.modbus.responses.ReadHoldingRegistersResponse;
import com.digitalpetri.modbus.responses.ReadInputRegistersResponse;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.edge.adapters.modbus.config.ModbusAdapterConfig;
import com.hivemq.edge.adapters.modbus.config.ModbusDataType;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * @author Simon L Johnson
 */
public class ModbusClient {

    private final @NotNull DataPointFactory dataPointFactory;

    private final ModbusTcpMaster modbusClient;

    public ModbusClient(
            final @NotNull ModbusAdapterConfig adapterConfig, final @NotNull DataPointFactory dataPointFactory) {
        this.dataPointFactory = dataPointFactory;
        final ModbusTcpMasterConfig config =
                new ModbusTcpMasterConfig.Builder(adapterConfig.getHost())
                        .setPort(adapterConfig.getPort())
                        .setInstanceId(adapterConfig.getId())
                        .setTimeout(Duration.ofMillis(adapterConfig.getTimeoutMillis()))
                        .build();
        modbusClient = new ModbusTcpMaster(config);
    }

    public boolean isConnected() {
        return modbusClient.isConnected();
    }

    public CompletableFuture<Void> connect() {
        return modbusClient.connect().thenApply(unused -> null);
    }

    /**
     * Coils are 1bit.
     */
    public @NotNull CompletableFuture<DataPoint> readCoils(final int startIdx, final int count, final int unitId) {
        return modbusClient
            .<ReadCoilsResponse>sendRequest(new ReadCoilsRequest(startIdx, Math.min(count, 2000)), unitId)
            .thenApply(response -> {
                try {
                    final ByteBuf buf = response.getCoilStatus();
                    return dataPointFactory.create("registers-" + startIdx + "-" + (startIdx + count),
                            convert(buf, ModbusDataType.BOOL, count));
                } finally {
                    ReferenceCountUtil.release(response);
                }
            });
    }

    /**
     * Discrete registers are 1bit.
     */
    public @NotNull CompletableFuture<DataPoint> readDiscreteInput(final int startIdx, final int count, final int unitId) {
        return modbusClient
                .<ReadDiscreteInputsResponse>sendRequest(new ReadDiscreteInputsRequest(startIdx, Math.min(count, 2000)), unitId)
                .thenApply(response -> {
                    try {
                        final ByteBuf buf = response.getInputStatus();
                        return dataPointFactory.create("registers-" + startIdx + "-" + (startIdx + count),
                                convert(buf, ModbusDataType.BOOL, count));
                    } finally {
                        ReferenceCountUtil.release(response);
                    }
                });
    }

    /**
     * Holding registers are 16bit.
     */
    public @NotNull CompletableFuture<DataPoint> readHoldingRegisters(final int startIdx, final int count, final @NotNull ModbusDataType dataType, final int unitId) {
        return modbusClient
                .<ReadHoldingRegistersResponse>sendRequest(new ReadHoldingRegistersRequest(startIdx, Math.min(count, 125)), unitId)
                .thenApply(response -> {
                    try {
                        final ByteBuf buf = response.getRegisters();
                        return dataPointFactory.create("registers-" + startIdx + "-" + (startIdx + count),
                                convert(buf, dataType, count));
                    } finally {
                        ReferenceCountUtil.release(response);
                    }
                });
    }

    /**
     * Inout registers are 16bit.
     */
    public @NotNull CompletableFuture<DataPoint> readInputRegisters(final int startIdx, final int count, final @NotNull ModbusDataType dataType, final int unitId) {
        return modbusClient
                .<ReadInputRegistersResponse>sendRequest(new ReadInputRegistersRequest(startIdx, Math.min(count, 125)), unitId)
                .thenApply(response -> {
                    try {
                        final ByteBuf buf = response.getRegisters();
                        return dataPointFactory.create("registers-" + startIdx + "-" + (startIdx + count),
                                convert(buf, dataType, count));
                    } finally {
                        ReferenceCountUtil.release(response);
                    }
                });
    }

    public CompletableFuture<Void> disconnect() {
        //-- If the client is manually disconnected before connection established ensure we still call into the client
        //-- to shut it all down.
        return modbusClient.disconnect().thenApply(t -> null);
    }

    private @NotNull Object convert(
            final @NotNull ByteBuf buffi, final @NotNull ModbusDataType dataType, final int count) {
        switch (dataType) {
            case BOOL:
                return buffi.readBoolean();
            case INT_16:
                return buffi.readShort();
            case UINT_16:
                return Short.toUnsignedInt(buffi.readShort());
            case INT_32:
                return buffi.readInt();
            case UINT_32:
                return Integer.toUnsignedLong(buffi.readInt());
            case INT_64:
                return buffi.readLong();
            case FLOAT_32:
                return buffi.readFloat();
            case UTF_8:
                final byte[] bytes = new byte[count * 2];
                buffi.readBytes(bytes);
                return new String(bytes, StandardCharsets.UTF_8);
        }
        throw new RuntimeException("Unknown dataType '" + dataType.name() + "'.");
    }
}
