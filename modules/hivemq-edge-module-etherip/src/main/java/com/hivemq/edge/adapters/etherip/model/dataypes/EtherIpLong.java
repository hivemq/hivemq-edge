package com.hivemq.edge.adapters.etherip.model.dataypes;

import com.hivemq.edge.adapters.etherip.model.EtherIpDataType;

public class EtherIpLong implements EtherIpDataType {
    private final Long value;
    private final String tagAddress;

    public EtherIpLong(final String tagAddress, final Number value) {
        this.value = value.longValue();
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
