package com.hivemq.edge.adapters.etherip.model.dataypes;

import com.hivemq.edge.adapters.etherip.model.EtherIpValue;

import java.util.Objects;

public class EtherIpInt implements EtherIpValue {
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EtherIpInt that = (EtherIpInt) o;
        return Objects.equals(value, that.value) && Objects.equals(tagAddress, that.tagAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, tagAddress);
    }
}
