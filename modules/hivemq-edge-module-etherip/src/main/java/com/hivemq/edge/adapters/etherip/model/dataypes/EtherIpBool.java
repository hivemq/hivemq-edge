package com.hivemq.edge.adapters.etherip.model.dataypes;

import com.hivemq.edge.adapters.etherip.model.EtherIpDataType;

public class EtherIpBool implements EtherIpDataType {
    private final Boolean value;
    private final String tagAddress;

    public EtherIpBool(final String tagAddress, final Number value) {
        //Values of 0 are false, all other values are treated as true
        this.value = value.intValue() != 0;
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
