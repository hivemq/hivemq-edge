package com.hivemq.edge.adapters.etherip.model.dataypes;

import com.hivemq.edge.adapters.etherip.model.EtherIpDataType;

public class EtherIpInt implements EtherIpDataType {
    private final Integer value;
    private final String tagAddress;

    public EtherIpInt(final String tagAddress, final Number value) {
        this.value = value.intValue();
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
