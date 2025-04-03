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

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.edge.adapters.modbus.config.ModbusSpecificAdapterConfig;
import com.hivemq.edge.adapters.modbus.config.ModbusDataType;
import com.hivemq.edge.adapters.modbus.impl.ModbusClient;
import com.hivemq.edge.modules.adapters.data.DataPointImpl;

import java.util.concurrent.CompletableFuture;

public class ModbusMainTest {
    public static void main(String[] args) throws Exception {

        String host = args[0];
        int port = Integer.parseInt(args[1]);

        ModbusSpecificAdapterConfig modbusAdapterConfig = new ModbusSpecificAdapterConfig(port, host, 5000, null) ;
        ModbusClient modbusClient = new ModbusClient("1", modbusAdapterConfig);

        modbusClient.connect().get();

        final CompletableFuture<Object> objectCompletableFuture =
                modbusClient.readHoldingRegisters(100, ModbusDataType.INT_32, 255, false);
        final Object result = objectCompletableFuture.get();

        System.out.println(result);
    }
}
