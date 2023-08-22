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
import com.hivemq.edge.adapters.modbus.IModbusClient;
import com.hivemq.edge.adapters.modbus.ModbusAdapterConfig;
import com.hivemq.edge.modules.adapters.ProtocolAdapterException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Simon L Johnson
 */
public class ModbusClient implements IModbusClient {

    private final @NotNull ModbusAdapterConfig adapterConfig;
    private final Object lock = new Object();
    private ModbusTcpMaster modbusClient;
    private AtomicBoolean connected = new AtomicBoolean(false);

    public ModbusClient(final @NotNull ModbusAdapterConfig adapterConfig) {
        this.adapterConfig = adapterConfig;
    }

    private ModbusTcpMaster getOrCreateClient() {
        if (modbusClient == null) {
            synchronized (lock) {
                if (modbusClient == null) {
                    ModbusTcpMasterConfig config = new ModbusTcpMasterConfig.Builder(adapterConfig.getHost()).
                            setPort(adapterConfig.getPort()).
                            setInstanceId(adapterConfig.getId()).
                            build();
                    modbusClient = new ModbusTcpMaster(config);
                }
            }
        }
        return modbusClient;
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }

    @Override
    public void connect() {
        ModbusTcpMaster client = getOrCreateClient();
        synchronized (lock) {
            if (!connected.get()) {
                client.connect().handle((res, ex) -> {
                    connected.set(ex == null);
                    return res;
                });
            }
        }
    }

    @Override
    public Boolean[] readCoils(int startIdx, int count) throws ProtocolAdapterException {
        try {
            ModbusTcpMaster client = getOrCreateClient();
            CompletableFuture<ModbusResponse> future =
                    client.sendRequest(new ReadCoilsRequest(startIdx, count), 0);
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

    @Override
    public Short[] readHoldingRegisters(int startIdx, int count) throws ProtocolAdapterException {
        try {
            ModbusTcpMaster client = getOrCreateClient();
            CompletableFuture<ReadHoldingRegistersResponse> future =
                    client.sendRequest(new ReadHoldingRegistersRequest(startIdx, count), 0);
            Short[] val = new Short[count];
            future.thenAccept(response -> {
                try {
                    ByteBuf buf = response.getRegisters();
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

    @Override
    public Short[] readInputRegisters(int startIdx, int count) throws ProtocolAdapterException {

        try {
            ModbusTcpMaster client = getOrCreateClient();
            CompletableFuture<ModbusResponse> future =
                    client.sendRequest(new ReadInputRegistersRequest(startIdx, count), 0);
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

    @Override
    public boolean disconnect() {
        //-- If the client is manually disconnected before connection established ensure we still call into the client
        //-- to shut it all down.
        if (modbusClient != null) {
            modbusClient.disconnect();
            return true;
        }
        return false;
    }
}
