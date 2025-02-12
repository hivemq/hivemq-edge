package com.hivemq.protocols.northbound;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.hivemq.adapter.sdk.api.ProtocolAdapterPublishBuilder;
import com.hivemq.adapter.sdk.api.ProtocolPublishResult;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.data.JsonPayloadCreator;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Payload;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterPublishServiceImpl;
import com.hivemq.edge.modules.api.events.model.EventImpl;
import com.hivemq.protocols.ProtocolAdapterWrapper;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class NorthboundTagConsumer implements TagConsumer{

    private static final Logger log = LoggerFactory.getLogger(NorthboundTagConsumer.class);

    private final @NotNull PollingContext pollingContext;
    private final @NotNull ProtocolAdapterWrapper protocolAdapter;
    private final @NotNull ObjectMapper objectMapper;
    private final @NotNull JsonPayloadCreator jsonPayloadCreator;
    private final @NotNull ProtocolAdapterPublishServiceImpl protocolAdapterPublishService;
    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull EventService eventService;

    // TODO does this really need to be atomic
    private final @NotNull AtomicInteger publishCount = new AtomicInteger(0);

    public NorthboundTagConsumer(
            final @NotNull PollingContext pollingContext,
            final @NotNull ProtocolAdapterWrapper protocolAdapter,
            final @NotNull ObjectMapper objectMapper,
            final @NotNull JsonPayloadCreator jsonPayloadCreator,
            final @NotNull ProtocolAdapterPublishServiceImpl protocolAdapterPublishService,
            final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService,
            final @NotNull EventService eventService) {
        this.pollingContext = pollingContext;
        this.protocolAdapter = protocolAdapter;
        this.objectMapper = objectMapper;
        this.jsonPayloadCreator = jsonPayloadCreator;
        this.protocolAdapterPublishService = protocolAdapterPublishService;
        this.protocolAdapterMetricsService = protocolAdapterMetricsService;
        this.eventService = eventService;
    }

    public void accept(final @NotNull List<DataPoint> dataPoints) {
        Preconditions.checkNotNull(dataPoints);
        Preconditions.checkNotNull(pollingContext);
        Preconditions.checkNotNull(pollingContext.getMqttTopic());

        Preconditions.checkArgument(pollingContext.getMqttQos() <= 2 && pollingContext.getMqttQos() >= 0,
                "QoS needs to be a valid QoS value (0,1,2)");
        try {
            final ImmutableList.Builder<CompletableFuture<?>> publishFutures = ImmutableList.builder();

            final List<byte[]> jsonPayloadsAsBytes;
            final JsonPayloadCreator jsonPayloadCreatorOverride = pollingContext.getJsonPayloadCreator();
            if (jsonPayloadCreatorOverride != null) {
                jsonPayloadsAsBytes =
                        jsonPayloadCreatorOverride.convertToJson(dataPoints, pollingContext, objectMapper);
            } else {
                jsonPayloadsAsBytes = jsonPayloadCreator.convertToJson(dataPoints, pollingContext, objectMapper);
            }

            for (final byte[] json : jsonPayloadsAsBytes) {
                final ProtocolAdapterPublishBuilder publishBuilder = protocolAdapterPublishService.createPublish()
                        .withTopic(pollingContext.getMqttTopic())
                        .withQoS(pollingContext.getMqttQos())
                        .withPayload(json)
                        .withAdapter(protocolAdapter.getAdapter());
                final CompletableFuture<ProtocolPublishResult> publishFuture = publishBuilder.send();
                publishFuture.thenAccept(publishReturnCode -> {
                    protocolAdapterMetricsService.incrementReadPublishSuccess();
                    if (publishCount.incrementAndGet() == 1) {
                        eventService.createAdapterEvent(protocolAdapter.getId(),
                                        protocolAdapter.getAdapterInformation().getProtocolId())
                                .withSeverity(EventImpl.SEVERITY.INFO)
                                .withTimestamp(System.currentTimeMillis())
                                .withMessage(String.format("Adapter '%s' took first sample to be published to '%s'",
                                        protocolAdapter.getId(),
                                        pollingContext.getMqttTopic()))
                                .withPayload(Payload.ContentType.JSON, new String(json, StandardCharsets.UTF_8))
                                .fire();
                    }
                }).exceptionally(throwable -> {
                    protocolAdapterMetricsService.incrementReadPublishFailure();
                    log.warn("Error publishing adapter payload", throwable);
                    return null;
                });
                publishFutures.add(publishFuture);
            }
        } catch (final Exception e) {
            log.warn("Exception during polling of data for adapters '{}':", protocolAdapter.getId(), e);
        }
    }

    @Override
    public @NotNull String getTagName() {
        return pollingContext.getTagName();
    }
}
