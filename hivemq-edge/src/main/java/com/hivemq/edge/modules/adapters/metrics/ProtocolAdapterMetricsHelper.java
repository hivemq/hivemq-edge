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
        publishSuccessCounter = metricRegistry.counter(createAdapterMetricsNamespace("read.publish") + SUCCESS_COUNT);
        publishFailedCounter = metricRegistry.counter(createAdapterMetricsNamespace("read.publish") + FAILED_COUNT);
        connectionSuccessCounter = metricRegistry.counter(createAdapterMetricsNamespace("connection") + SUCCESS_COUNT);
        connectionFailedCounter = metricRegistry.counter(createAdapterMetricsNamespace("connection") + FAILED_COUNT);
    }

    public void incrementReadPublishSuccess(){
        publishSuccessCounter.inc();
    }

    public void incrementReadPublishFailure(){
        publishSuccessCounter.inc();
    }

    public void incrementConnectionFailure(){
        connectionFailedCounter.inc();
    }

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

    protected String createAdapterMetricsNamespace(String suffix){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(HiveMQMetrics.PROTOCOL_ADAPTER_PREFIX);
        stringBuilder.append(protocolAdapterType);
        stringBuilder.append(PERIOD);
        stringBuilder.append(protocolAdapterId);
        stringBuilder.append(PERIOD);
        stringBuilder.append(suffix);
        stringBuilder.append(PERIOD);
        return stringBuilder.toString();
    }
}
