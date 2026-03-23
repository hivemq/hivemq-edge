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
package com.hivemq.edge.modules.adapters.telemetry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EdgeTelemetryAdapter implements ProtocolAdapter {

    private static final @NotNull Logger log = LoggerFactory.getLogger(EdgeTelemetryAdapter.class);
    private static final @NotNull ObjectMapper MAPPER = new ObjectMapper();
    private static final int PUBLISH_INTERVAL_SECONDS = 30;

    private final @NotNull String adapterId;
    private final @NotNull List<EdgeTelemetryTag> tags;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull DataPointFactory dataPointFactory;

    private @Nullable ScheduledExecutorService scheduler;
    private final @NotNull List<ScheduledFuture<?>> scheduledFutures = new ArrayList<>();

    public EdgeTelemetryAdapter(final @NotNull ProtocolAdapterInput<EdgeTelemetryAdapterConfig> input) {
        this.adapterId = input.getAdapterId();
        this.tags = input.getTags().stream().map(t -> (EdgeTelemetryTag) t).toList();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.dataPointFactory = input.adapterFactories().dataPointFactory();
    }

    @Override
    public @NotNull String getId() {
        return adapterId;
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return EdgeTelemetryAdapterInformation.INSTANCE;
    }

    @Override
    public void start(
            final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
        final EdgeTelemetryService service = EdgeTelemetryService.getInstance();
        if (service == null) {
            output.failStart(
                    new IllegalStateException("EdgeTelemetryService not yet initialized"),
                    "EdgeTelemetryService not yet initialized");
            return;
        }

        final ProtocolAdapterTagStreamingService streamingService =
                input.moduleServices().protocolAdapterTagStreamingService();

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "edge-telemetry-" + adapterId));

        for (final EdgeTelemetryTag tag : tags) {
            final String tagName = tag.getName();
            final String topicFilter = tag.getDefinition().getTopicFilter();
            final String tagKey = adapterId + "/" + tagName;

            service.subscribe(tagKey, topicFilter);

            final ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                    () -> publishCount(service, streamingService, tagKey, tagName, topicFilter),
                    PUBLISH_INTERVAL_SECONDS,
                    PUBLISH_INTERVAL_SECONDS,
                    TimeUnit.SECONDS);
            scheduledFutures.add(future);

            log.info("[{}] Edge telemetry tag '{}' watching topic filter '{}'", adapterId, tagName, topicFilter);
        }

        protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        output.startedSuccessfully();
    }

    @Override
    public void stop(final @NotNull ProtocolAdapterStopInput input, final @NotNull ProtocolAdapterStopOutput output) {
        scheduledFutures.forEach(f -> f.cancel(false));
        scheduledFutures.clear();
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }

        final EdgeTelemetryService service = EdgeTelemetryService.getInstance();
        if (service != null) {
            for (final EdgeTelemetryTag tag : tags) {
                service.unsubscribe(adapterId + "/" + tag.getName());
            }
        }

        protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
        output.stoppedSuccessfully();
    }

    private void publishCount(
            final @NotNull EdgeTelemetryService service,
            final @NotNull ProtocolAdapterTagStreamingService streamingService,
            final @NotNull String tagKey,
            final @NotNull String tagName,
            final @NotNull String topicFilter) {
        try {
            final long count = service.getAndResetCount(tagKey);
            final ObjectNode inner = MAPPER.createObjectNode();
            inner.put("value", count);
            final ObjectNode context = inner.putObject("context");
            context.put("topicFilter", topicFilter);
            context.put("windowSeconds", PUBLISH_INTERVAL_SECONDS);

            final ObjectNode wrapper = MAPPER.createObjectNode();
            wrapper.set("value", inner);
            final DataPoint dataPoint =
                    dataPointFactory.createJsonDataPoint(tagName, MAPPER.writeValueAsString(wrapper));
            streamingService.feed(tagName, List.of(dataPoint));
        } catch (final Exception e) {
            log.warn("[{}] Failed to publish count for tag '{}'", adapterId, tagName, e);
        }
    }
}
