package com.hivemq.protocols;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterPublishBuilder;
import com.hivemq.adapter.sdk.api.ProtocolPublishResult;
import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.data.ProtocolAdapterDataSample;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Payload;
import com.hivemq.adapter.sdk.api.exceptions.ProtocolAdapterException;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterPublishService;
import com.hivemq.edge.modules.adapters.data.AbstractProtocolAdapterJsonPayload;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterMultiPublishJsonPayload;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterPublisherJsonPayload;
import com.hivemq.edge.modules.adapters.data.TagSample;
import com.hivemq.edge.modules.adapters.metrics.ProtocolAdapterMetricsServiceImpl;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingSampler;
import com.hivemq.edge.modules.api.events.model.EventImpl;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public abstract class AbstractSubscriptionSampler implements ProtocolAdapterPollingSampler {

    private static final Logger log = LoggerFactory.getLogger(AbstractSubscriptionSampler.class);

    private final long initialDelay;
    private final long period;
    private final int maxErrorsBeforeRemoval;

    private final @NotNull ObjectMapper objectMapper;
    private final @NotNull ProtocolAdapterPublishService adapterPublishService;
    private final @NotNull EventService eventService;
    private final @NotNull TimeUnit unit;
    private final @NotNull String adapterId;
    private final @NotNull UUID uuid;
    private final @NotNull Date created;
    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull AtomicInteger publishCount = new AtomicInteger(0);

    private volatile @Nullable ScheduledFuture<?> future;

    protected final @NotNull AtomicBoolean closed = new AtomicBoolean(false);
    protected final @NotNull ProtocolAdapterWrapper<? extends ProtocolAdapter> protocolAdapter;

    public AbstractSubscriptionSampler(
            final @NotNull ProtocolAdapterWrapper<? extends ProtocolAdapter> protocolAdapter,
            final long protocolPollingIntervalMillis,
            final int maxPollingErrorsBeforeRemoval,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull ObjectMapper objectMapper,
            final @NotNull ProtocolAdapterPublishService adapterPublishService,
            final @NotNull EventService eventService) {
        this.protocolAdapter = protocolAdapter;
        this.adapterId = protocolAdapter.getId();
        this.initialDelay = Math.max(protocolPollingIntervalMillis, 100);
        this.period = Math.max(protocolPollingIntervalMillis, 10);
        this.objectMapper = objectMapper;
        this.adapterPublishService = adapterPublishService;
        this.eventService = eventService;
        this.unit = TimeUnit.MILLISECONDS;
        this.maxErrorsBeforeRemoval = maxPollingErrorsBeforeRemoval;
        this.uuid = UUID.randomUUID();
        this.created = new Date();
        this.protocolAdapterMetricsService =
                new ProtocolAdapterMetricsServiceImpl(protocolAdapter.getProtocolAdapterInformation().getProtocolId(),
                        protocolAdapter.getId(),
                        metricRegistry);
    }

    @Override
    public abstract @NotNull CompletableFuture<PollingOutputImpl.PollingResult> execute();

    @Override
    public void error(@NotNull final Throwable t, final boolean continuing) {
        onSamplerError(t, continuing);
    }

    /**
     * Hook Method is invoked by the sampling engine when the sampler throws an exception. It contains
     * details of whether the sampler will continue or be removed from the scheduler along with
     * the cause of the error.
     */
    protected void onSamplerError(
            final @NotNull Throwable exception, boolean continuing) {
        protocolAdapter.setErrorConnectionStatus(exception, null);
        if (!continuing) {
            protocolAdapter.stop();
        }
    }


    protected @NotNull CompletableFuture<?> captureDataSample(
            final @NotNull ProtocolAdapterDataSample sample, final @NotNull PollingContext pollingContext) {
        Preconditions.checkNotNull(sample);
        Preconditions.checkNotNull(pollingContext);
        Preconditions.checkNotNull(pollingContext.getMqttTopic());

        Preconditions.checkArgument(pollingContext.getQos() <= 2 && pollingContext.getQos() >= 0,
                "QoS needs to be a valid QoS value (0,1,2)");
        try {
            final ImmutableList.Builder<CompletableFuture<?>> publishFutures = ImmutableList.builder();
            List<AbstractProtocolAdapterJsonPayload> payloads = convertAdapterSampleToPublishes(sample, pollingContext);
            for (AbstractProtocolAdapterJsonPayload payload : payloads) {
                byte[] json = convertToJson(payload);
                final ProtocolAdapterPublishBuilder publishBuilder = adapterPublishService.publish()
                        .withTopic(pollingContext.getMqttTopic())
                        .withQoS(pollingContext.getQos())
                        .withPayload(json)
                        .withAdapter(protocolAdapter);
                final CompletableFuture<ProtocolPublishResult> publishFuture = publishBuilder.send();
                publishFuture.thenAccept(publishReturnCode -> {
                    protocolAdapterMetricsService.incrementReadPublishSuccess();
                    if (publishCount.incrementAndGet() == 1) {
                        eventService.adapterEvent(adapterId, protocolAdapter.getAdapterInformation().getProtocolId())
                                .withSeverity(EventImpl.SEVERITY.INFO)
                                .withTimestamp(System.currentTimeMillis())
                                .withMessage(String.format("Adapter '%s' took first sample to be published to '%s'",
                                        adapterId,
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
            return CompletableFuture.allOf(publishFutures.build().toArray(new CompletableFuture[0]));
        } catch (Exception e) {
            log.warn("Exception during polling of data for adapters '{}':", adapterId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public byte @NotNull [] convertToJson(final @NotNull AbstractProtocolAdapterJsonPayload data)
            throws ProtocolAdapterException {
        try {
            Preconditions.checkNotNull(data);
            return objectMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new ProtocolAdapterException("Error Wrapping Adapter Data", e);
        }
    }

    public @NotNull List<AbstractProtocolAdapterJsonPayload> convertAdapterSampleToPublishes(
            final @NotNull ProtocolAdapterDataSample data, final @NotNull PollingContext pollingContext) {
        Preconditions.checkNotNull(data);
        List<AbstractProtocolAdapterJsonPayload> list = new ArrayList<>();
        //-- Only include the timestamp if the settings say so
        Long timestamp = pollingContext.getIncludeTimestamp() ? data.getTimestamp() : null;
        if (data.getDataPoints().size() > 1 &&
                pollingContext.getMessageHandlingOptions() == MessageHandlingOptions.MQTTMessagePerSubscription) {
            //-- Put all derived samples into a single MQTT message
            AbstractProtocolAdapterJsonPayload payload =
                    createMultiPublishPayload(timestamp, data.getDataPoints(), pollingContext.getIncludeTagNames());
            decoratePayloadMessage(payload, pollingContext);
            list.add(payload);
        } else {
            //-- Put all derived samples into individual publish messages
            data.getDataPoints()
                    .stream()
                    .map(dp -> createPublishPayload(timestamp, dp, pollingContext.getIncludeTagNames()))
                    .map(pp -> decoratePayloadMessage(pp, pollingContext))
                    .forEach(list::add);
        }
        return list;
    }

    protected @NotNull ProtocolAdapterPublisherJsonPayload createPublishPayload(
            final @Nullable Long timestamp, @NotNull DataPoint dataPoint, boolean includeTagName) {
        return new ProtocolAdapterPublisherJsonPayload(timestamp, createTagSample(dataPoint, includeTagName));
    }

    protected @NotNull AbstractProtocolAdapterJsonPayload createMultiPublishPayload(
            final @Nullable Long timestamp, List<DataPoint> dataPoint, boolean includeTagName) {
        return new ProtocolAdapterMultiPublishJsonPayload(timestamp,
                dataPoint.stream().map(dp -> createTagSample(dp, includeTagName)).collect(Collectors.toList()));
    }

    protected static TagSample createTagSample(final @NotNull DataPoint dataPoint, boolean includeTagName) {
        return new TagSample(includeTagName ? dataPoint.getTagName() : null, dataPoint.getTagValue());
    }

    protected @NotNull AbstractProtocolAdapterJsonPayload decoratePayloadMessage(
            final @NotNull AbstractProtocolAdapterJsonPayload payload,
            final @NotNull PollingContext pollingContext) {
        if (!pollingContext.getUserProperties().isEmpty()) {
            payload.setUserProperties(pollingContext.getUserProperties());
        }
        return payload;
    }

    @Override
    public @NotNull ProtocolAdapter getAdapter() {
        return protocolAdapter;
    }

    @Override
    public long getInitialDelay() {
        return initialDelay;
    }

    @Override
    public long getPeriod() {
        return period;
    }

    @Override
    public @NotNull TimeUnit getUnit() {
        return unit;
    }

    @Override
    public int getMaxErrorsBeforeRemoval() {
        return maxErrorsBeforeRemoval;
    }

    @Override
    public void close() {
        closed.set(true);
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AbstractSubscriptionSampler that = (AbstractSubscriptionSampler) o;
        return uuid.equals(that.uuid);
    }

    @Override
    public @NotNull UUID getId() {
        return uuid;
    }

    @Override
    public @NotNull Date getCreated() {
        return created;
    }

    @Override
    public @NotNull String getAdapterId() {
        return adapterId;
    }

    @Override
    public @Nullable ScheduledFuture<?> getScheduledFuture() {
        return future;
    }

    @Override
    public void setScheduledFuture(final @NotNull ScheduledFuture<?> future) {
        this.future = future;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }


}
