/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.protocols;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterPublishBuilder;
import com.hivemq.adapter.sdk.api.ProtocolPublishResult;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.data.JsonPayloadCreator;
import com.hivemq.adapter.sdk.api.data.ProtocolAdapterDataSample;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Payload;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterPublishService;
import com.hivemq.edge.modules.adapters.metrics.ProtocolAdapterMetricsServiceImpl;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingSampler;
import com.hivemq.edge.modules.api.events.model.EventImpl;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractSubscriptionSampler implements ProtocolAdapterPollingSampler {

    private static final Logger log = LoggerFactory.getLogger(AbstractSubscriptionSampler.class);

    private final long initialDelay;
    private final long period;
    private final int maxErrorsBeforeRemoval;

    private final @NotNull ObjectMapper objectMapper;
    private final @NotNull ProtocolAdapterPublishService adapterPublishService;
    private final @NotNull EventService eventService;
    private final @NotNull TimeUnit unit = TimeUnit.MILLISECONDS;
    private final @NotNull String adapterId;
    private final @NotNull UUID uuid;
    private final @NotNull Date created;
    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull AtomicInteger publishCount = new AtomicInteger(0);

    private volatile @Nullable ScheduledFuture<?> future;

    protected final @NotNull AtomicBoolean closed = new AtomicBoolean(false);
    protected final @NotNull ProtocolAdapterWrapper<? extends ProtocolAdapter> protocolAdapter;
    private final @NotNull JsonPayloadDefaultCreator jsonPayloadDefaultCreator;

    public AbstractSubscriptionSampler(
            final @NotNull ProtocolAdapterWrapper<? extends ProtocolAdapter> protocolAdapter,
            final long protocolPollingIntervalMillis,
            final int maxPollingErrorsBeforeRemoval,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull ObjectMapper objectMapper,
            final @NotNull ProtocolAdapterPublishService adapterPublishService,
            final @NotNull EventService eventService,
            final @NotNull JsonPayloadDefaultCreator jsonPayloadDefaultCreator) {
        this.protocolAdapter = protocolAdapter;
        this.adapterId = protocolAdapter.getId();
        this.initialDelay = Math.max(protocolPollingIntervalMillis, 100);
        this.period = Math.max(protocolPollingIntervalMillis, 10);
        this.objectMapper = objectMapper;
        this.jsonPayloadDefaultCreator = jsonPayloadDefaultCreator;
        this.adapterPublishService = adapterPublishService;
        this.eventService = eventService;
        this.maxErrorsBeforeRemoval = maxPollingErrorsBeforeRemoval;
        this.uuid = UUID.randomUUID();
        this.created = new Date();
        this.protocolAdapterMetricsService =
                new ProtocolAdapterMetricsServiceImpl(protocolAdapter.getProtocolAdapterInformation().getProtocolId(),
                        protocolAdapter.getId(),
                        metricRegistry);
    }

    @Override
    public abstract @NotNull CompletableFuture<?> execute();

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
            protocolAdapter.stop(new ProtocolAdapterStopInputImpl(), new ProtocolAdapterStopOutputImpl());
        }
    }


    protected @NotNull CompletableFuture<?> captureDataSample(
            final @NotNull ProtocolAdapterDataSample sample, final @NotNull PollingContext pollingContext) {
        Preconditions.checkNotNull(sample);
        Preconditions.checkNotNull(pollingContext);
        Preconditions.checkNotNull(pollingContext.getDestinationMqttTopic());

        Preconditions.checkArgument(pollingContext.getQos() <= 2 && pollingContext.getQos() >= 0,
                "QoS needs to be a valid QoS value (0,1,2)");
        try {
            final ImmutableList.Builder<CompletableFuture<?>> publishFutures = ImmutableList.builder();

            final List<byte[]> jsonPayloadsAsBytes;
            final JsonPayloadCreator jsonPayloadCreatorOverride = pollingContext.getJsonPayloadCreator();
            if (jsonPayloadCreatorOverride != null) {
                jsonPayloadsAsBytes = jsonPayloadCreatorOverride.convertToJson(sample, objectMapper);
            } else {
                jsonPayloadsAsBytes = jsonPayloadDefaultCreator.convertToJson(sample, objectMapper);
            }

            for (byte[] json : jsonPayloadsAsBytes) {
                final ProtocolAdapterPublishBuilder publishBuilder = adapterPublishService.createPublish()
                        .withTopic(pollingContext.getDestinationMqttTopic())
                        .withQoS(pollingContext.getQos())
                        .withPayload(json)
                        .withAdapter(protocolAdapter);
                final CompletableFuture<ProtocolPublishResult> publishFuture = publishBuilder.send();
                publishFuture.thenAccept(publishReturnCode -> {
                    protocolAdapterMetricsService.incrementReadPublishSuccess();
                    if (publishCount.incrementAndGet() == 1) {
                        eventService.createAdapterEvent(adapterId,
                                        protocolAdapter.getAdapterInformation().getProtocolId())
                                .withSeverity(EventImpl.SEVERITY.INFO)
                                .withTimestamp(System.currentTimeMillis())
                                .withMessage(String.format("Adapter '%s' took first sample to be published to '%s'",
                                        adapterId,
                                        pollingContext.getDestinationMqttTopic()))
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
    public @NotNull String getProtocolId() {
        return protocolAdapter.getProtocolAdapterInformation().getProtocolId();
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
