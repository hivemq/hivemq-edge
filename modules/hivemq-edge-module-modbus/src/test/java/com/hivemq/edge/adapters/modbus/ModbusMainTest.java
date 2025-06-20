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

import com.hivemq.edge.adapters.modbus.config.ModbusDataType;
import com.hivemq.edge.adapters.modbus.config.ModbusSpecificAdapterConfig;
import org.jetbrains.annotations.NotNull;

public class ModbusMainTest {
    public static void main(final @NotNull String @NotNull [] args) throws Exception {
        final String host = args[0];
        final int port = Integer.parseInt(args[1]);
        final ModbusSpecificAdapterConfig modbusAdapterConfig = new ModbusSpecificAdapterConfig(port, host, 5000, null);
        final ModbusClient modbusClient = new ModbusClient(modbusAdapterConfig);
        modbusClient.connect().toCompletableFuture().get();
        final Object result =
                modbusClient.readHoldingRegisters(100, ModbusDataType.INT_32, 255, false).toCompletableFuture().get();
        System.out.println(result);
        modbusClient.disconnect().toCompletableFuture().get();
    }
}
