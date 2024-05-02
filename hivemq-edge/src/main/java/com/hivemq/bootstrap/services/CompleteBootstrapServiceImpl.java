package com.hivemq.bootstrap.services;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.bootstrap.ioc.Persistences;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.HiveMQCapabilityService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.core.HandlerService;
import com.hivemq.extensions.core.PersistencesService;
import com.hivemq.extensions.core.RestComponentsService;

public class CompleteBootstrapServiceImpl implements CompleteBootstrapService {

    private final @NotNull Persistences persistences;
    private final @NotNull RestComponentsService restComponentsService;
    private final @NotNull HandlerService handlerService;
    private final @NotNull PersistenceBootstrapService delegate;

    public CompleteBootstrapServiceImpl(
            final @NotNull PersistenceBootstrapService delegate,
            final @NotNull Persistences persistences,
            final @NotNull RestComponentsService restComponentsService,
            final @NotNull HandlerService handlerService) {
        this.delegate = delegate;
        this.persistences = persistences;
        this.restComponentsService = restComponentsService;
        this.handlerService = handlerService;
    }

    @Override
    public @NotNull MetricRegistry metricRegistry() {
        return delegate.metricRegistry();
    }

    @Override
    public @NotNull SystemInformation systemInformation() {
        return delegate.systemInformation();
    }

    @Override
    public @NotNull ShutdownHooks shutdownHooks() {
        return delegate.shutdownHooks();
    }

    @Override
    public @NotNull ConfigurationService configurationService() {
        return delegate.configurationService();
    }

    @Override
    public @NotNull HivemqId getHivemqId() {
        return delegate.getHivemqId();
    }

    @Override
    public @NotNull PersistencesService persistenceService() {
        return delegate.persistenceService();
    }

    @Override
    public @NotNull HiveMQCapabilityService capabilityService() {
        return delegate.capabilityService();
    }

    @Override
    public @NotNull Persistences persistences() {
        return persistences;
    }

    @Override
    public @NotNull RestComponentsService restComponentsService() {
        return restComponentsService;
    }

    @Override
    public @NotNull HandlerService handlerService() {
        return handlerService;
    }

    public static @NotNull CompleteBootstrapService decorate(
            final @NotNull PersistenceBootstrapService persistenceBootstrapService,
            final @NotNull Persistences persistences,
            final @NotNull RestComponentsService restComponentsService,
            final @NotNull HandlerService handlerService) {
        return new CompleteBootstrapServiceImpl(persistenceBootstrapService,
                persistences,
                restComponentsService,
                handlerService);
    }
}
