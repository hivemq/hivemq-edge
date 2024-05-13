package com.hivemq.protocols;

import com.hivemq.edge.modules.adapters.ProtocolAdapter;
import com.hivemq.edge.modules.adapters.impl.ModuleServicesImpl;
import com.hivemq.edge.modules.adapters.impl.ModuleServicesPerModuleImpl;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterStartInput;
import com.hivemq.edge.modules.adapters.services.ModuleServices;
import com.hivemq.edge.modules.events.EventService;
import com.hivemq.extension.sdk.api.annotations.NotNull;

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
