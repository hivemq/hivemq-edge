package com.hivemq.edge.adapters.etherip.model.dataypes;

import com.hivemq.edge.adapters.etherip.model.EtherIpDataType;

import java.util.Objects;

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

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EtherIpBool that = (EtherIpBool) o;
        return Objects.equals(value, that.value) && Objects.equals(tagAddress, that.tagAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, tagAddress);
    }
}
