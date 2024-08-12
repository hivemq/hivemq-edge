package com.hivemq.edge.adapters.etherip.model.dataypes;

import com.hivemq.edge.adapters.etherip.model.EtherIpValue;

import java.util.Objects;

public class EtherIpLong implements EtherIpValue {
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EtherIpLong that = (EtherIpLong) o;
        return Objects.equals(value, that.value) && Objects.equals(tagAddress, that.tagAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, tagAddress);
    }
}
