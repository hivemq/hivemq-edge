package com.hivemq.extensions.core;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.edge.modules.ModuleLoader;
import com.hivemq.extension.sdk.api.annotations.NotNull;

public class CoreModuleServiceImpl implements CoreModuleService {

    private final @NotNull PersistencesService persistencesService;
    private final @NotNull SystemInformation systemInformation;
    private final @NotNull MetricRegistry metricRegistry;
    private final @NotNull ShutdownHooks shutdownHooks;
    private final @NotNull ModuleLoader moduleLoader;


    public CoreModuleServiceImpl(
            final @NotNull PersistencesService persistencesService,
            final @NotNull SystemInformation systemInformation,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull ShutdownHooks shutdownHooks,
            final @NotNull ModuleLoader moduleLoader) {
        this.persistencesService = persistencesService;
        this.systemInformation = systemInformation;
        this.metricRegistry = metricRegistry;
        this.shutdownHooks = shutdownHooks;
        this.moduleLoader = moduleLoader;
    }

    @Override
    public @NotNull PersistencesService persistenceService() {
        return persistencesService;
    }

    @Override
    public @NotNull SystemInformation systemInformation() {
        return systemInformation;
    }

    @Override
    public @NotNull MetricRegistry metricRegistry() {
        return metricRegistry;
    }

    @Override
    public @NotNull ShutdownHooks shutdownHooks() {
        return shutdownHooks;
    }

    @Override
    public @NotNull ModuleLoader moduleLoader() {
        return moduleLoader;
    }
}
