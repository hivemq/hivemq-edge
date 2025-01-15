package com.hivemq.protocols;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProtocolAdaptersContainer {
    private final @NotNull Map<String, ProtocolAdapterWrapper> protocolAdapters = new ConcurrentHashMap<>();

    public ProtocolAdapterWrapper getAdapterById(@NotNull final String id) {
        return protocolAdapters.get(id);
    }

    public ProtocolAdapterWrapper removeAdapterById(@NotNull final String id) {
        return protocolAdapters.remove(id);
    }
}
