package com.hivemq.edge.adapters.etherip.model.dataypes;

import com.hivemq.edge.adapters.etherip.model.EtherIpDataType;

public class EtherIpString implements EtherIpDataType {
    private final String value;
    private final String tagAddress;

    public EtherIpString(final String tagAddress, final String value) {
        this.value = value;
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
