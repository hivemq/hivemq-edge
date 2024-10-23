package com.hivemq.protocols;

import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterTagService;
import org.jetbrains.annotations.NotNull;

public class ProtocolAdapterFactoryInputImpl implements ProtocolAdapterFactoryInput {

    private final boolean writingEnabled;
    private final @NotNull ProtocolAdapterTagService protocolAdapterTagService;
    private final @NotNull EventService eventService;

    public ProtocolAdapterFactoryInputImpl(
            final boolean writingEnabled,
            final @NotNull ProtocolAdapterTagService protocolAdapterTagService,
            final @NotNull EventService eventService) {
        this.writingEnabled = writingEnabled;
        this.protocolAdapterTagService = protocolAdapterTagService;
        this.eventService = eventService;
    }

    @Override
    public boolean isWritingEnabled() {
        return writingEnabled;
    }

    @Override
    public @NotNull ProtocolAdapterTagService protocolAdapterTagService() {
        return protocolAdapterTagService;
    }

    @Override
    public @NotNull EventService eventService() {
        return eventService;
    }
}
