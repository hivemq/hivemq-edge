package com.hivemq.protocols;

import com.hivemq.edge.modules.adapters.impl.ModuleServicesImpl;
import com.hivemq.edge.modules.adapters.impl.ModuleServicesPerModuleImpl;
import com.hivemq.extension.sdk.api.adapters.ProtocolAdapter;
import com.hivemq.extension.sdk.api.adapters.model.ProtocolAdapterStartInput;
import com.hivemq.extension.sdk.api.adapters.services.ModuleServices;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.events.EventService;

class ProtocolAdapterStartInputImpl implements ProtocolAdapterStartInput {

    private final @NotNull ProtocolAdapter protocolAdapter;
    private final @NotNull ModuleServicesImpl moduleServices;
    private final @NotNull EventService eventService;

    ProtocolAdapterStartInputImpl(
            final @NotNull ModuleServicesImpl moduleServices,
            final @NotNull ProtocolAdapter protocolAdapter,
            final @NotNull EventService eventService) {
        this.moduleServices = moduleServices;
        this.protocolAdapter = protocolAdapter;
        this.eventService = eventService;
    }

    @Override
    public @NotNull ModuleServices moduleServices() {
        return new ModuleServicesPerModuleImpl(protocolAdapter, moduleServices, eventService);
    }
}
