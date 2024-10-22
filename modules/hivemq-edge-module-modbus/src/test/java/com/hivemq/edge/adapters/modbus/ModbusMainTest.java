package com.hivemq.edge.adapters.modbus;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.edge.adapters.modbus.config.ModbusAdapterConfig;
import com.hivemq.edge.adapters.modbus.config.ModbusDataType;
import com.hivemq.edge.adapters.modbus.impl.ModbusClient;
import com.hivemq.edge.modules.adapters.data.DataPointImpl;

public class ModbusMainTest {
    public static void main(String[] args) throws Exception {

        ModbusAdapterConfig modbusAdapterConfig = new ModbusAdapterConfig("1", 502, "172.16.10.12", 5000, null) ;
        ModbusClient modbusClient = new ModbusClient(modbusAdapterConfig, DataPointImpl::new);

        modbusClient.connect().get();

        //41335
        DataPoint dataPoint = modbusClient.readInputRegisters(30021, ModbusDataType.INT_32, 12).get();

        System.out.println(dataPoint.getTagName() + " " + dataPoint.getTagValue());
    }
}
