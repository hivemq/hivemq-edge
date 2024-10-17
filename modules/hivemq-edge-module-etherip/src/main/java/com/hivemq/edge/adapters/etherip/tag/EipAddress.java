package com.hivemq.edge.adapters.etherip.tag;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EipAddress {

    private final @NotNull String address;

    public EipAddress(final @NotNull String address) {
        this.address = address;
    }

    public @NotNull String getAddress() {
        return address;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final EipAddress that = (EipAddress) o;
        return address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }
}
