package com.hivemq.protocols.northbound;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterPublishServiceImpl;
import com.hivemq.protocols.ProtocolAdapterWrapper;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NorthboundConsumerFactory {

    private final @NotNull ObjectMapper objectMapper;
    private final @NotNull JsonPayloadDefaultCreator jsonPayloadCreator;
    private final @NotNull ProtocolAdapterPublishServiceImpl protocolAdapterPublishService;
    private final @NotNull EventService eventService;

    @Inject
    public NorthboundConsumerFactory(
            final @NotNull ObjectMapper objectMapper,
            final @NotNull JsonPayloadDefaultCreator jsonPayloadCreator,
            final @NotNull ProtocolAdapterPublishServiceImpl protocolAdapterPublishService,
            final @NotNull EventService eventService) {
        this.objectMapper = objectMapper;
        this.jsonPayloadCreator = jsonPayloadCreator;
        this.protocolAdapterPublishService = protocolAdapterPublishService;
        this.eventService = eventService;
    }

    public @NotNull NorthboundTagConsumer build(
            final @NotNull ProtocolAdapterWrapper protocolAdapterWrapper,
            final @NotNull PollingContext pollingContext,
            final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService) {
        return new NorthboundTagConsumer(pollingContext,
                protocolAdapterWrapper,
                objectMapper,
                jsonPayloadCreator,
                protocolAdapterPublishService,
                protocolAdapterMetricsService,
                eventService);
    }
}
