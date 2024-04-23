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

import static com.hivemq.edge.modules.adapters.ProtocolAdapterConstants.PROTOCOL_ADAPTER_PREFIX;

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
    private final @NotNull Set<String> metricNames = new HashSet<>();
    private final Object mutex = new Object();
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
        publishSuccessCounter = metricRegistry.counter(createAdapterMetricsNamespace("read.publish." + SUCCESS_COUNT));
        publishFailedCounter = metricRegistry.counter(createAdapterMetricsNamespace("read.publish." + FAILED_COUNT) );
        connectionSuccessCounter = metricRegistry.counter(createAdapterMetricsNamespace("connection." + SUCCESS_COUNT));
        connectionFailedCounter = metricRegistry.counter(createAdapterMetricsNamespace("connection." + FAILED_COUNT));
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
        metricRegistry.counter(createAdapterMetricsNamespace(metricName)).inc();
    }

    /**
     * Will clear down all metrics in the registry created by this metrics helper.
     * NB: metrics created outside the context of this helper will not be touched.
     */
    public void clearAll(){
        Preconditions.checkNotNull(metricRegistry);
        synchronized (mutex){
            metricNames.forEach(metricRegistry::remove);
            metricNames.clear();
        }
    }

    /**
     * Create a deterministic prefix for use in the metrics registry.
     *
     * Example format of the namespace:
     * com.hivemq.edge.protocol-adapters.[test-type].[test-id].[suffix](.) with optional trailing period
     * @param suffix - the suffix to append to the namespace
     * @return a namespace string for use in the metrics registry
     */
    protected String createAdapterMetricsNamespace(@NotNull final String suffix){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(PROTOCOL_ADAPTER_PREFIX);
        stringBuilder.append(protocolAdapterType);
        stringBuilder.append(PERIOD);
        stringBuilder.append(protocolAdapterId);
        stringBuilder.append(PERIOD);
        stringBuilder.append(suffix);
        String metricName = stringBuilder.toString();
        synchronized (mutex){
            metricNames.add(metricName);
        }
        return metricName;
    }
}
