package com.hivemq.edge.modules.adapters.metrics;

import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;

public interface InternalProtocolAdapterMetricsService extends ProtocolAdapterMetricsService {

    /**
     * Removes all metrics created via this service for the adapter.
     */
    void clearAll();
}
