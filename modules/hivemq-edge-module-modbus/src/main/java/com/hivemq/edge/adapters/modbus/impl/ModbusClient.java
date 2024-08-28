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
import com.digitalpetri.modbus.requests.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.requests.ReadInputRegistersRequest;
import com.digitalpetri.modbus.responses.ModbusResponse;
import com.digitalpetri.modbus.responses.ReadCoilsResponse;
import com.digitalpetri.modbus.responses.ReadHoldingRegistersResponse;
import com.digitalpetri.modbus.responses.ReadInputRegistersResponse;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.exceptions.ProtocolAdapterException;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.edge.adapters.modbus.config.ModbusDataType;
import com.hivemq.edge.adapters.modbus.config.ModbusAdapterConfig;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Simon L Johnson
 */
public class ModbusClient {

    private final @NotNull ModbusAdapterConfig adapterConfig;
    private final @NotNull DataPointFactory dataPointFactory;

    private final Object lock = new Object();
    private ModbusTcpMaster modbusClient;
    private AtomicBoolean connected = new AtomicBoolean(false);

    public ModbusClient(
            final @NotNull ModbusAdapterConfig adapterConfig, final @NotNull DataPointFactory dataPointFactory) {
        this.adapterConfig = adapterConfig;
        this.dataPointFactory = dataPointFactory;
    }

    private ModbusTcpMaster getOrCreateClient() {
        if (modbusClient == null) {
            synchronized (lock) {
                if (modbusClient == null) {
                    ModbusTcpMasterConfig config =
                            new ModbusTcpMasterConfig.Builder(adapterConfig.getHost()).setPort(adapterConfig.getPort())
                                    .setInstanceId(adapterConfig.getId())
                                    .setTimeout(Duration.ofMillis(adapterConfig.getTimeoutMillis()))
                                    .build();
                    modbusClient = new ModbusTcpMaster(config);
                }
            }
        }
        return modbusClient;
    }

    public boolean isConnected() {
        return connected.get();
    }

    public CompletableFuture connect() {
        ModbusTcpMaster client = getOrCreateClient();
        if (!connected.get()) {
            return client.connect().thenRun(() -> connected.set(true));
        }
        return CompletableFuture.completedFuture(null);
    }

    public Boolean[] readCoils(int startIdx, int count) throws ProtocolAdapterException {
        try {
            ModbusTcpMaster client = getOrCreateClient();
            CompletableFuture<ModbusResponse> future =
                    client.sendRequest(new ReadCoilsRequest(startIdx, Math.min(count, 2000)), 0);
            Boolean[] val = new Boolean[count];
            future.thenAccept(response -> {
                try {
                    ReadCoilsResponse coilsResponse = (ReadCoilsResponse) response;
                    ByteBuf buf = coilsResponse.getCoilStatus();
                    int idx = 0;
                    while (buf.isReadable()) {
                        val[idx++] = buf.readBoolean();
                    }
                } finally {
                    ReferenceCountUtil.release(response);
                }
            }).get();
            return val;
        } catch (Exception e) {
            throw new ProtocolAdapterException(e);
        }
    }

    public @NotNull DataPoint readHoldingRegisters(int startIdx, int count, final @NotNull ModbusDataType dataType)
            throws ProtocolAdapterException {
        try {
            ModbusTcpMaster client = getOrCreateClient();
            CompletableFuture<ReadHoldingRegistersResponse> future =
                    client.sendRequest(new ReadHoldingRegistersRequest(startIdx, Math.min(count, 125)), 0);
            return future.thenApply(response -> {
                try {
                    final ByteBuf buf = response.getRegisters();
                    return dataPointFactory.create("registers-" + startIdx + "-" + (startIdx + count),
                            convert(buf, dataType, count));
                } finally {
                    ReferenceCountUtil.release(response);
                }
            }).get();

        } catch (Exception e) {
            throw new ProtocolAdapterException(e);
        }
    }

    public Short[] readInputRegisters(int startIdx, int count) throws ProtocolAdapterException {

        try {
            ModbusTcpMaster client = getOrCreateClient();
            CompletableFuture<ModbusResponse> future =
                    client.sendRequest(new ReadInputRegistersRequest(startIdx, Math.min(count, 125)), 0);
            Short[] val = new Short[count];
            future.thenAccept(response -> {
                try {
                    ReadInputRegistersResponse coilsResponse = (ReadInputRegistersResponse) response;
                    ByteBuf buf = coilsResponse.getRegisters();
                    //2 bytes per register BE
                    int idx = 0;
                    while (buf.isReadable()) {
                        val[idx++] = buf.readShort();
                    }
                } finally {
                    ReferenceCountUtil.release(response);
                }
            }).get();
            return val;
        } catch (Exception e) {
            throw new ProtocolAdapterException(e);
        }
    }

    public boolean disconnect() {
        //-- If the client is manually disconnected before connection established ensure we still call into the client
        //-- to shut it all down.
        if (modbusClient != null) {
            try {
                modbusClient.disconnect().get();
                return true;
            } catch (Exception e) {
                //error disconnecting
            }
        }
        return false;
    }

    private @NotNull Object convert(
            final @NotNull ByteBuf buffi, final @NotNull ModbusDataType dataType, final int count) {
        switch (dataType) {
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
