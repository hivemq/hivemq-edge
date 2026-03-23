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
package com.hivemq.edge.modules.adapters.adaptermetrics;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.STATELESS;
import static com.hivemq.protocols.ProtocolAdapterMetrics.PROTOCOL_ADAPTER_PREFIX;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingInput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationInput;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationOutput;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.modules.adapters.adaptermetrics.tag.AdapterMetricsTag;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdapterMetricsProtocolAdapter implements BatchPollingProtocolAdapter {

    private static final @NotNull Logger log = LoggerFactory.getLogger(AdapterMetricsProtocolAdapter.class);
    private static final @NotNull ObjectMapper MAPPER = new ObjectMapper();

    private final @NotNull String adapterId;
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull AdapterMetricsSpecificAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull List<AdapterMetricsTag> tags;
    private final @NotNull MetricRegistry metricRegistry;
    private final @NotNull DataPointFactory dataPointFactory;

    public AdapterMetricsProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<AdapterMetricsSpecificAdapterConfig> input,
            final @NotNull MetricRegistry metricRegistry) {
        this.adapterId = input.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.tags = input.getTags().stream().map(tag -> (AdapterMetricsTag) tag).toList();
        this.metricRegistry = metricRegistry;
        this.dataPointFactory = input.adapterFactories().dataPointFactory();
        this.protocolAdapterState.setConnectionStatus(STATELESS);
    }

    @Override
    public @NotNull String getId() {
        return adapterId;
    }

    @Override
    public void start(
            final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
        output.startedSuccessfully();
    }

    @Override
    public void stop(final @NotNull ProtocolAdapterStopInput input, final @NotNull ProtocolAdapterStopOutput output) {
        output.stoppedSuccessfully();
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    @Override
    public void poll(final @NotNull BatchPollingInput pollingInput, final @NotNull BatchPollingOutput pollingOutput) {
        for (final AdapterMetricsTag tag : tags) {
            final String metricKey = PROTOCOL_ADAPTER_PREFIX
                    + tag.getDefinition().getObservedAdapterType()
                    + "."
                    + tag.getDefinition().getObservedAdapterId()
                    + "."
                    + tag.getDefinition().getMetric().getSuffix();
            final Counter counter = metricRegistry.getCounters().get(metricKey);
            final long value = counter != null ? counter.getCount() : 0L;
            try {
                final ObjectNode inner = MAPPER.createObjectNode();
                inner.put("value", value);
                final ObjectNode context = inner.putObject("context");
                context.put("adapterType", tag.getDefinition().getObservedAdapterType());
                context.put("adapterId", tag.getDefinition().getObservedAdapterId());
                context.put("metric", tag.getDefinition().getMetric().name());
                final ObjectNode wrapper = MAPPER.createObjectNode();
                wrapper.set("value", inner);
                final DataPoint dataPoint =
                        dataPointFactory.createJsonDataPoint(tag.getName(), MAPPER.writeValueAsString(wrapper));
                pollingOutput.addDataPoint(dataPoint);
            } catch (final Exception e) {
                log.warn("[{}] Failed to create data point for tag '{}'", adapterId, tag.getName(), e);
                pollingOutput.fail(e, "Failed to serialize metric data point");
                return;
            }
        }
        pollingOutput.finish();
    }

    @Override
    public int getPollingIntervalMillis() {
        return adapterConfig.getPollingIntervalMillis();
    }

    @Override
    public int getMaxPollingErrorsBeforeRemoval() {
        return adapterConfig.getMaxPollingErrorsBeforeRemoval();
    }

    @Override
    public void createTagSchema(
            final @NotNull TagSchemaCreationInput input, final @NotNull TagSchemaCreationOutput output) {
        output.finish(MAPPER.createObjectNode());
    }
}
