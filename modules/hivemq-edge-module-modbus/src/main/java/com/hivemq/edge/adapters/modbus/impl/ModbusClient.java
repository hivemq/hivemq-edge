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
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * @author Simon L Johnson
 */
public class ModbusClient {

    public static final int DEFAULT_MAX_INPUT_REGISTERS = 125;
    public static final int DEFAULT_MAX_DISCRETE_INPUTS = 2000;
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
    public @NotNull CompletableFuture<DataPoint> readCoils(final int startIdx, final int unitId) {
        return modbusClient
            .<ReadCoilsResponse>sendRequest(new ReadCoilsRequest(startIdx, Math.min(1, DEFAULT_MAX_DISCRETE_INPUTS)), unitId)
            .thenApply(response -> {
                try {
                    final ByteBuf buf = response.getCoilStatus();
                    return dataPointFactory.create("registers-" + startIdx,
                            convert(buf, ModbusDataType.BOOL, 1, false));
                } finally {
                    ReferenceCountUtil.release(response);
                }
            });
    }

    /**
     * Discrete registers are 1bit.
     */
    public @NotNull CompletableFuture<DataPoint> readDiscreteInput(final int startIdx, final int unitId) {
        return modbusClient
                .<ReadDiscreteInputsResponse>sendRequest(new ReadDiscreteInputsRequest(startIdx, Math.min(1,
                        DEFAULT_MAX_DISCRETE_INPUTS)), unitId)
                .thenApply(response -> {
                    try {
                        final ByteBuf buf = response.getInputStatus();
                        return dataPointFactory.create("registers-" + startIdx,
                                convert(buf, ModbusDataType.BOOL, 1, false));
                    } finally {
                        ReferenceCountUtil.release(response);
                    }
                });
    }

    /**
     * Holding registers are 16bit.
     */
    public @NotNull CompletableFuture<DataPoint> readHoldingRegisters(final int startIdx, final @NotNull ModbusDataType dataType, final int unitId, final boolean flipRegisters) {

        return modbusClient
                .<ReadHoldingRegistersResponse>sendRequest(new ReadHoldingRegistersRequest(startIdx, Math.min(dataType.nrOfRegistersToRead,
                        DEFAULT_MAX_INPUT_REGISTERS)), unitId)
                .thenApply(response -> {
                    try {
                        final ByteBuf buf = response.getRegisters();
                        return dataPointFactory.create("registers-" + startIdx + "-" + (startIdx + dataType.nrOfRegistersToRead - 1),
                                convert(buf, dataType, dataType.nrOfRegistersToRead, flipRegisters));
                    } finally {
                        ReferenceCountUtil.release(response);
                    }
                });
    }

    /**
     * Inout registers are 16bit.
     */
    public @NotNull CompletableFuture<DataPoint> readInputRegisters(final int startIdx, final @NotNull ModbusDataType dataType, final int unitId, final boolean flipRegisters) {
        return modbusClient
                .<ReadInputRegistersResponse>sendRequest(new ReadInputRegistersRequest(startIdx, Math.min(dataType.nrOfRegistersToRead,
                        DEFAULT_MAX_INPUT_REGISTERS)), unitId)
                .thenApply(response -> {
                    try {
                        final ByteBuf buf = response.getRegisters();
                        return dataPointFactory.create("registers-" + startIdx + "-" + (startIdx + dataType.nrOfRegistersToRead - 1),
                                convert(buf, dataType, dataType.nrOfRegistersToRead, flipRegisters));
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
            final @NotNull ByteBuf buffi, final @NotNull ModbusDataType dataType, final int count, final boolean flipRegisters) {
        switch (dataType) {
            case BOOL:
                return buffi.readBoolean();
            case INT_16:
                return buffi.readShort();
            case UINT_16:
                return Short.toUnsignedInt(buffi.readShort());
            case INT_32:
                if(flipRegisters) {
                    byte b1 = buffi.readByte();
                    byte b2 = buffi.readByte();
                    byte b3 = buffi.readByte();
                    byte b4 = buffi.readByte();
                    return Unpooled.wrappedBuffer(new byte[] {b4 ,b3, b2, b1}).readInt();
                } else {
                    return buffi.readInt();
                }
            case UINT_32:
                if(flipRegisters) {
                    byte b1 = buffi.readByte();
                    byte b2 = buffi.readByte();
                    byte b3 = buffi.readByte();
                    byte b4 = buffi.readByte();
                    return Unpooled.wrappedBuffer(new byte[] {b3 ,b4, b1, b2}).readUnsignedInt();
                } else {
                    return buffi.readUnsignedInt();
                }
            case INT_64:
                if(flipRegisters) {
                    byte b1 = buffi.readByte();
                    byte b2 = buffi.readByte();
                    byte b3 = buffi.readByte();
                    byte b4 = buffi.readByte();
                    byte b5 = buffi.readByte();
                    byte b6 = buffi.readByte();
                    byte b7 = buffi.readByte();
                    byte b8 = buffi.readByte();
                    return Unpooled.wrappedBuffer(new byte[] {b7 ,b8, b5 ,b6, b3 ,b4, b1, b2}).readUnsignedInt();
                } else {
                    return buffi.readLong();
                }
            case FLOAT_32:
                if(flipRegisters) {
                    byte b1 = buffi.readByte();
                    byte b2 = buffi.readByte();
                    byte b3 = buffi.readByte();
                    byte b4 = buffi.readByte();
                    return Unpooled.wrappedBuffer(new byte[] {b3 ,b4, b1, b2}).readFloat();
                } else {
                    return buffi.readFloat();
                }
            case FLOAT_64:
                if(flipRegisters) {
                    byte b1 = buffi.readByte();
                    byte b2 = buffi.readByte();
                    byte b3 = buffi.readByte();
                    byte b4 = buffi.readByte();
                    byte b5 = buffi.readByte();
                    byte b6 = buffi.readByte();
                    byte b7 = buffi.readByte();
                    byte b8 = buffi.readByte();
                    return Unpooled.wrappedBuffer(new byte[] {b7 ,b8, b5 ,b6, b3 ,b4, b1, b2}).readUnsignedInt();
                } else {
                    return buffi.readDouble();
                }
            case UTF_8:
                final byte[] bytes = new byte[count * 2];
                buffi.readBytes(bytes);
                return new String(bytes, StandardCharsets.UTF_8);
        }
        throw new RuntimeException("Unknown dataType '" + dataType.name() + "'.");
    }
}
