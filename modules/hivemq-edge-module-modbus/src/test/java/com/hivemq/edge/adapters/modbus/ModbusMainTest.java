package com.hivemq.edge.adapters.modbus;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.edge.adapters.modbus.config.ModbusAdapterConfig;
import com.hivemq.edge.adapters.modbus.config.ModbusDataType;
import com.hivemq.edge.adapters.modbus.impl.ModbusClient;
import com.hivemq.edge.modules.adapters.data.DataPointImpl;

public class ModbusMainTest {
    public static void main(String[] args) throws Exception {

        String host = args[0];
        int port = Integer.parseInt(args[1]);

        ModbusAdapterConfig modbusAdapterConfig = new ModbusAdapterConfig("1", port, host, 5000, null, null) ;
        ModbusClient modbusClient = new ModbusClient(modbusAdapterConfig, DataPointImpl::new);

        modbusClient.connect().get();

        DataPoint dataPoint = modbusClient.readHoldingRegisters(100, ModbusDataType.INT_32, 255, false).get();

        System.out.println(dataPoint.getTagName() + " " + dataPoint.getTagValue());
    }
}
