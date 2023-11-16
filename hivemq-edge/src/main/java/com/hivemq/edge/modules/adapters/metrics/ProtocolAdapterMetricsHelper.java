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
import com.hivemq.metrics.HiveMQMetrics;

/**
 * Ensures the adapters use consistent namespaces for the metrics so they can be derived
 * in a deterministic fashion.
 *
 * @author Simon L Johnson
 */
public class ProtocolAdapterMetricsHelper {

    private @NotNull String protocolAdapterType;
    private @NotNull String protocolAdapterId;
    private @NotNull MetricRegistry metricRegistry;
    static final String SUCCESS_COUNT = "success.count";
    static final String FAILED_COUNT = "failed.count";
    static final String PERIOD = ".";

    protected Counter publishSuccessCounter;
    protected Counter publishFailedCounter;
    protected Counter connectionSuccessCounter;
    protected Counter connectionFailedCounter;

    public ProtocolAdapterMetricsHelper(final @NotNull String protocolAdapterType,
                                        final @NotNull String protocolAdapterId,
                                        final @NotNull MetricRegistry metricRegistry) {
        Preconditions.checkNotNull(protocolAdapterType);
        Preconditions.checkNotNull(protocolAdapterId);
        Preconditions.checkNotNull(metricRegistry);
        this.protocolAdapterType = protocolAdapterType;
        this.protocolAdapterId = protocolAdapterId;
        this.metricRegistry = metricRegistry;
        initRegistry();
    }

    protected void initRegistry(){
        publishSuccessCounter = metricRegistry.counter(createAdapterMetricsNamespace("read.publish", true) + SUCCESS_COUNT);
        publishFailedCounter = metricRegistry.counter(createAdapterMetricsNamespace("read.publish", true) + FAILED_COUNT);
        connectionSuccessCounter = metricRegistry.counter(createAdapterMetricsNamespace("connection", true) + SUCCESS_COUNT);
        connectionFailedCounter = metricRegistry.counter(createAdapterMetricsNamespace("connection", true) + FAILED_COUNT);
    }

    /**
     * Use to indicate a read from the adapter has been successfully PUBLISHed
     */
    public void incrementReadPublishSuccess(){
        publishSuccessCounter.inc();
    }

    /**
     * Use to indicate a read from the adapter has failed
     */
    public void incrementReadPublishFailure(){
        publishFailedCounter.inc();
    }


    /**
     * Use to indicate a connection attempt to the device has failed
     */
    public void incrementConnectionFailure(){
        connectionFailedCounter.inc();
    }

    /**
     * Use to indicate a connection attempt to the device has succeeded
     */
    public void incrementConnectionSuccess(){
        connectionSuccessCounter.inc();
    }

    /**
     * Increment an arbitrary counter in the adapter instance namespace
     * @param metricName - the metric name to be incremented (inside) the adapter namespace
     */
    public void increment(final @NotNull String metricName){
        Preconditions.checkNotNull(metricName);
        metricRegistry.counter(createAdapterMetricsNamespace(metricName, false)).inc();
    }

    /**
     * Create a deterministic prefix for use in the metrics registry.
     *
     * Example format of the namespace:
     * com.hivemq.edge.protocol-adapters.[test-type].[test-id].[suffix](.) with optional trailing period
     * @param suffix - the suffix to append to the namespace
     * @param trailingPeriod - should the namespace by suffixed with a trailing period
     * @return a namespace string for use in the metrics registry
     */
    protected String createAdapterMetricsNamespace(@NotNull final String suffix, final boolean trailingPeriod){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(HiveMQMetrics.PROTOCOL_ADAPTER_PREFIX);
        stringBuilder.append(protocolAdapterType);
        stringBuilder.append(PERIOD);
        stringBuilder.append(protocolAdapterId);
        stringBuilder.append(PERIOD);
        stringBuilder.append(suffix);
        if(trailingPeriod){
            stringBuilder.append(PERIOD);
        }
        return stringBuilder.toString();
    }
}
