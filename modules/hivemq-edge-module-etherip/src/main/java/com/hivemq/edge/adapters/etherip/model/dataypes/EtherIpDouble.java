package com.hivemq.edge.adapters.etherip.model.dataypes;

import com.hivemq.edge.adapters.etherip.model.EtherIpDataType;

public class EtherIpDouble implements EtherIpDataType {
    private final Double value;
    private final String tagAddress;

    public EtherIpDouble(final String tagAddress, final Number value) {
        this.value = value.doubleValue();
        this.tagAddress = tagAddress;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public String getTagAdress() {
        return tagAddress;
    }
}
