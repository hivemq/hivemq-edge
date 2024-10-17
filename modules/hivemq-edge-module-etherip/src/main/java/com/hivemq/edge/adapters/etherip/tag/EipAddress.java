package com.hivemq.edge.adapters.etherip.tag;

import org.jetbrains.annotations.NotNull;

public class EipAddress {

    private final @NotNull String address;

    public EipAddress(final @NotNull String address) {
        this.address = address;
    }

    public @NotNull String getAddress() {
        return address;
    }
}
