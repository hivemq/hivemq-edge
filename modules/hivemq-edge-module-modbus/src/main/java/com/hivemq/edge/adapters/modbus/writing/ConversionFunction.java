package com.hivemq.edge.adapters.modbus.writing;

public interface ConversionFunction {


    public byte[] convert(Object value);


}
