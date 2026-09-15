/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua.condition;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.datapoint.DataPointListBuilder;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.datapoint.DataPointListBuilderImpl;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jetbrains.annotations.NotNull;

/**
 * A tag streaming service that keeps what the adapter publishes, so a test can assert on the payload.
 * <p>
 * Uses the production {@link DataPointListBuilderImpl} rather than a mock: the point of an integration test is
 * to see the value the adapter actually produces, and a mocked builder would only record that methods were
 * called.
 */
public class CapturingTagStreamingService implements ProtocolAdapterTagStreamingService {

    private final @NotNull List<DataPoint> dataPoints = new CopyOnWriteArrayList<>();
    private final @NotNull List<List<DataPoint>> publishedBatches = new CopyOnWriteArrayList<>();

    @Override
    public @NotNull DataPointListBuilder dataPointsPublisher() {
        return new DataPointListBuilderImpl("test-adapter-id", builder -> {}, this::capture);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void feed(final @NotNull String tag, final @NotNull List<DataPoint> points) {
        capture(points);
    }

    /** Retains one immutable snapshot for every builder {@code publish()} or legacy {@code feed()} call. */
    private void capture(final @NotNull List<DataPoint> points) {
        final List<DataPoint> snapshot = List.copyOf(points);
        publishedBatches.add(snapshot);
        dataPoints.addAll(snapshot);
    }

    /**
     * Every publication batch, in call order and with its data-point order intact.
     * <p>
     * Kept alongside {@link #published()} because flattening alone cannot distinguish one callback carrying
     * several transitions from several callbacks carrying one each — a boundary the OPC UA message contract
     * depends on downstream.
     */
    public @NotNull List<List<DataPoint>> publishedBatches() {
        return List.copyOf(publishedBatches);
    }

    /**
     * The values of everything published so far, in order.
     */
    public @NotNull List<JsonNode> published() {
        return dataPoints.stream()
                .map(DataPoint::getTagValue)
                .filter(JsonNode.class::isInstance)
                .map(JsonNode.class::cast)
                .toList();
    }
}
