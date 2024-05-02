package com.hivemq.bootstrap.services;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * This class provides means to receive information that is needed for the bootstrap of config and other essential
 * requirements to start Edge
 */
public class GeneralBootstrapServiceImpl implements GeneralBootstrapService {

    private final @NotNull ShutdownHooks shutdownHooks;
    private final @NotNull MetricRegistry metricRegistry;
    private final @NotNull SystemInformation systemInformation;
    private final @NotNull ConfigurationService configurationService;
    private final @NotNull HivemqId hivemqId;

    public GeneralBootstrapServiceImpl(
            final @NotNull ShutdownHooks shutdownHooks,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull SystemInformation systemInformation,
            final @NotNull ConfigurationService configurationService,
            final @NotNull HivemqId hivemqId) {
        this.shutdownHooks = shutdownHooks;
        this.metricRegistry = metricRegistry;
        this.systemInformation = systemInformation;
        this.configurationService = configurationService;
        this.hivemqId = hivemqId;
    }

    @Override
    public @NotNull MetricRegistry metricRegistry() {
        return metricRegistry;
    }

    @Override
    public @NotNull SystemInformation systemInformation() {
        return systemInformation;
    }

    @Override
    public @NotNull ShutdownHooks shutdownHooks() {
        return shutdownHooks;
    }

    @Override
    public @NotNull ConfigurationService configurationService() {
        return configurationService;
    }

    @Override
    public @NotNull String getHivemqId() {
        return hivemqId.get();
    }


}
