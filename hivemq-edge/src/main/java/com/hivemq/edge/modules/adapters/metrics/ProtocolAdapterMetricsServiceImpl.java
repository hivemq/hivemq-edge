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
package com.hivemq.edge.modules.adapters.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

import static com.hivemq.protocols.ProtocolAdapterMetrics.PROTOCOL_ADAPTER_PREFIX;


/**
 * Ensures the adapters use consistent namespaces for the metrics so they can be derived
 * in a deterministic fashion.
 *
 * @author Simon L Johnson
 */
public class ProtocolAdapterMetricsServiceImpl implements InternalProtocolAdapterMetricsService {

    private static final @NotNull String SUCCESS_COUNT = "success.count";
    private static final @NotNull String FAILED_COUNT = "failed.count";

    private final @NotNull Set<String> metricNames = new HashSet<>();
    private final @NotNull MetricRegistry metricRegistry;

    private final @NotNull String protocolAdapterType;
    private final @NotNull String protocolAdapterId;
    private final @NotNull Counter publishReadSuccessCounter;
    private final @NotNull Counter publishReadFailedCounter;
    private final @NotNull Counter publishWriteSuccessCounter;
    private final @NotNull Counter publishWriteFailedCounter;
    private final @NotNull Counter connectionSuccessCounter;
    private final @NotNull Counter connectionFailedCounter;

    public ProtocolAdapterMetricsServiceImpl(
            final @NotNull String protocolAdapterType,
            final @NotNull String protocolAdapterId,
            final @NotNull MetricRegistry metricRegistry) {
        Preconditions.checkNotNull(protocolAdapterType);
        Preconditions.checkNotNull(protocolAdapterId);
        Preconditions.checkNotNull(metricRegistry);
        this.protocolAdapterType = protocolAdapterType;
        this.protocolAdapterId = protocolAdapterId;
        this.metricRegistry = metricRegistry;
        this.publishReadSuccessCounter =
                metricRegistry.counter(createAdapterMetricsNamespace("read.publish." + SUCCESS_COUNT));
        this.publishReadFailedCounter =
                metricRegistry.counter(createAdapterMetricsNamespace("read.publish." + FAILED_COUNT));
        this.publishWriteSuccessCounter =
                metricRegistry.counter(createAdapterMetricsNamespace("write.publish." + SUCCESS_COUNT));
        this.publishWriteFailedCounter =
                metricRegistry.counter(createAdapterMetricsNamespace("write.publish." + FAILED_COUNT));
        this.connectionSuccessCounter =
                metricRegistry.counter(createAdapterMetricsNamespace("connection." + SUCCESS_COUNT));
        this.connectionFailedCounter =
                metricRegistry.counter(createAdapterMetricsNamespace("connection." + FAILED_COUNT));
    }


    /**
     * Use to indicate a read from the adapter has been successfully PUBLISHed
     */
    @Override
    public void incrementReadPublishSuccess() {
        publishReadSuccessCounter.inc();
    }

    /**
     * Use to indicate a read from the adapter has failed
     */
    @Override
    public void incrementReadPublishFailure() {
        publishReadFailedCounter.inc();
    }

    @Override
    public void incrementWritePublishSuccess() {
        publishWriteSuccessCounter.inc();
    }

    @Override
    public void incrementWritePublishFailure() {
        publishWriteFailedCounter.inc();
    }

    /**
     * Use to indicate a connection attempt to the device has failed
     */
    @Override
    public void incrementConnectionFailure() {
        connectionFailedCounter.inc();
    }

    /**
     * Use to indicate a connection attempt to the device has succeeded
     */
    @Override
    public void incrementConnectionSuccess() {
        connectionSuccessCounter.inc();
    }

    /**
     * Increment an arbitrary counter in the adapter instance namespace
     *
     * @param metricName - the metric name to be incremented (inside) the adapter namespace
     */
    @Override
    public void increment(final @NotNull String metricName) {
        Preconditions.checkNotNull(metricName);
        metricRegistry.counter(createAdapterMetricsNamespace(metricName)).inc();
    }

    /**
     * Will clear down all metrics in the registry created by this metrics helper.
     * NB: metrics created outside the context of this helper will not be touched.
     */
    @Override
    public synchronized void clearAll() {
        Preconditions.checkNotNull(metricRegistry);
        metricNames.forEach(metricRegistry::remove);
        metricNames.clear();
    }

    /**
     * Create a deterministic prefix for use in the metrics registry.
     * <p>
     * Example format of the namespace:
     * com.hivemq.edge.protocol-adapters.[test-type].[test-id].[suffix](.) with optional trailing period
     *
     * @param suffix - the suffix to append to the namespace
     * @return a namespace string for use in the metrics registry
     */
    protected synchronized @NotNull String createAdapterMetricsNamespace(@NotNull final String suffix) {
        final String metricName =
                PROTOCOL_ADAPTER_PREFIX + protocolAdapterType + "." + protocolAdapterId + "." + suffix;
        metricNames.add(metricName);
        return metricName;
    }
}
