package com.hivemq.protocols;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.hivemq.edge.modules.adapters.ProtocolAdapterException;
import com.hivemq.edge.modules.adapters.data.AbstractProtocolAdapterJsonPayload;
import com.hivemq.edge.modules.adapters.data.DataPoint;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSample;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterMultiPublishJsonPayload;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterPublisherJsonPayload;
import com.hivemq.edge.modules.adapters.data.TagSample;
import com.hivemq.edge.modules.adapters.metrics.ProtocolAdapterMetricsHelper;
import com.hivemq.edge.modules.adapters.metrics.ProtocolAdapterMetricsHelperImpl;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterPollingSampler;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapter;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPublishBuilder;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPublishService;
import com.hivemq.edge.modules.config.AdapterSubscription;
import com.hivemq.edge.modules.config.CustomConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.PublishReturnCode;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public abstract class AbstractSubscriptionSampler implements ProtocolAdapterPollingSampler {

    private final long initialDelay;
    private final long period;
    private final @NotNull ObjectMapper objectMapper;
    private final @NotNull ProtocolAdapterPublishService adapterPublishService;
    private final @NotNull TimeUnit unit;
    private final int maxErrorsBeforeRemoval;
    protected @NotNull AtomicBoolean closed = new AtomicBoolean(false);
    private final @NotNull String adapterId;
    private final @NotNull UUID uuid;
    private final @NotNull Date created;
    private @Nullable ScheduledFuture<?> future;
    protected final @NotNull ProtocolAdapter protocolAdapter;
    private final @NotNull ProtocolAdapterMetricsHelper protocolAdapterMetricsHelper;
    private final @NotNull CustomConfig customConfig;

    public AbstractSubscriptionSampler(
            final @NotNull ProtocolAdapter protocolAdapter,
            final @NotNull CustomConfig customConfig,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull ObjectMapper objectMapper,
            final @NotNull ProtocolAdapterPublishService adapterPublishService) {
        this.protocolAdapter = protocolAdapter;
        this.customConfig = customConfig;
        this.adapterId = protocolAdapter.getId();
        this.initialDelay = Math.max(customConfig.getPollingIntervalMillis(), 100);
        this.period = Math.max(customConfig.getPollingIntervalMillis(), 10);
        this.objectMapper = objectMapper;
        this.adapterPublishService = adapterPublishService;
        this.unit = TimeUnit.MILLISECONDS;
        this.maxErrorsBeforeRemoval = customConfig.getMaxPollingErrorsBeforeRemoval();
        this.uuid = UUID.randomUUID();
        this.created = new Date();
        protocolAdapterMetricsHelper =
                new ProtocolAdapterMetricsHelperImpl(protocolAdapter.getProtocolAdapterInformation().getProtocolId(),
                        protocolAdapter.getId(),
                        metricRegistry);
    }

    @Override
    public abstract CompletableFuture<? extends ProtocolAdapterDataSample> execute();

    @Override
    public void error(@NotNull final Throwable t, final boolean continuing) {
        onSamplerError(this, t, continuing);
    }

    /**
     * Hook Method is invoked by the sampling engine when the sampler throws an exception. It contains
     * details of whether the sampler will continue or be removed from the scheduler along with
     * the cause of the error.
     */
    protected void onSamplerError(
            final @NotNull ProtocolAdapterPollingSampler sampler,
            final @NotNull Throwable exception,
            boolean continuing) {
        // TODO
        // protocolAdapter.setErrorConnectionStatus(exception, null);
        if (!continuing) {
            protocolAdapter.stop();
        }
    }


    protected @NotNull CompletableFuture<?> captureDataSample(final @NotNull ProtocolAdapterDataSample sample) {
        Preconditions.checkNotNull(sample);
        Preconditions.checkNotNull(sample.getTopic());
        Preconditions.checkArgument(sample.getQos() <= 2 && sample.getQos() >= 0,
                "QoS needs to be a valid Quality-Of-Service value (0,1,2)");
        try {
            final ImmutableList.Builder<CompletableFuture<?>> publishFutures = ImmutableList.builder();
            List<AbstractProtocolAdapterJsonPayload> payloads = convertAdapterSampleToPublishes(sample);
            for (AbstractProtocolAdapterJsonPayload payload : payloads) {
                byte[] json = convertToJson(payload);
                final ProtocolAdapterPublishBuilder publishBuilder = adapterPublishService.publish()
                        .withTopic(sample.getTopic())
                        .withQoS(sample.getQos())
                        .withPayload(json)
                        .withAdapter(protocolAdapter);
                final CompletableFuture<PublishReturnCode> publishFuture = publishBuilder.send();
                publishFuture.thenAccept(publishReturnCode -> {
                    protocolAdapterMetricsHelper.incrementReadPublishSuccess();
                    //TODO
                            /*
                            if(publishCount.incrementAndGet() == 1){
                                eventService.fireEvent(eventBuilder(EventImpl.SEVERITY.INFO).
                                        withMessage(String.format("Adapter took first sample to be published to '%s'", sample.getTopic())).
                                        withPayload(EventUtils.generateJsonPayload(json)).build());
                            }
                            */
                }).exceptionally(throwable -> {
                    protocolAdapterMetricsHelper.incrementReadPublishFailure();
                    //TODO log.warn("Error Publishing Adapter Payload", throwable);
                    return null;
                });
                publishFutures.add(publishFuture);
            }
            return CompletableFuture.allOf(publishFutures.build().toArray(new CompletableFuture[0]));
        } catch (Exception e) {

            e.printStackTrace();

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

    public @NotNull List<AbstractProtocolAdapterJsonPayload> convertAdapterSampleToPublishes(final @NotNull ProtocolAdapterDataSample data) {
        Preconditions.checkNotNull(data);
        List<AbstractProtocolAdapterJsonPayload> list = new ArrayList<>();
        //-- Only include the timestamp if the settings say so
        Long timestamp = data.getSubscription().getIncludeTimestamp() ? data.getTimestamp() : null;
        if (data.getDataPoints().size() > 1 &&
                data.getSubscription().getMessageHandlingOptions() ==
                        AdapterSubscription.MessageHandlingOptions.MQTTMessagePerSubscription) {
            //-- Put all derived samples into a single MQTT message
            AbstractProtocolAdapterJsonPayload payload = createMultiPublishPayload(timestamp,
                    data.getDataPoints(),
                    data.getSubscription().getIncludeTagNames());
            decoratePayloadMessage(data, payload);
            list.add(payload);
        } else {
            //-- Put all derived samples into individual publish messages
            data.getDataPoints()
                    .stream()
                    .map(dp -> createPublishPayload(timestamp, dp, data.getSubscription().getIncludeTagNames()))
                    .map(pp -> decoratePayloadMessage(data, pp))
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
            ProtocolAdapterDataSample sample, @NotNull AbstractProtocolAdapterJsonPayload payload) {
        if (sample.getUserProperties() != null && !sample.getUserProperties().isEmpty()) {
            payload.setUserProperties(sample.getUserProperties());
        }
        return payload;
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
    public void setScheduledFuture(final @NotNull ScheduledFuture future) {
        this.future = future;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }







}
