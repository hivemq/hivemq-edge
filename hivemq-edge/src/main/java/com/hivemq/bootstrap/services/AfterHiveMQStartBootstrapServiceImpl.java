package com.hivemq.bootstrap.services;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.bootstrap.ioc.Persistences;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.HiveMQCapabilityService;
import com.hivemq.edge.ModulesAndExtensionsService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.core.HandlerService;
import com.hivemq.extensions.core.PersistencesService;
import com.hivemq.extensions.core.RestComponentsService;
import com.hivemq.protocols.ProtocolAdapterManager;

public class AfterHiveMQStartBootstrapServiceImpl implements AfterHiveMQStartBootstrapService {

    private final @NotNull CompleteBootstrapService delegate;
    private final @NotNull ProtocolAdapterManager protocolAdapterManager;
    private final @NotNull ModulesAndExtensionsService modulesAndExtensionsService;

    public AfterHiveMQStartBootstrapServiceImpl(
            final @NotNull CompleteBootstrapService delegate,
            final @NotNull ProtocolAdapterManager protocolAdapterManager,
            final @NotNull ModulesAndExtensionsService modulesAndExtensionsService) {
        this.delegate = delegate;
        this.protocolAdapterManager = protocolAdapterManager;
        this.modulesAndExtensionsService = modulesAndExtensionsService;
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
        return delegate.persistences();
    }

    @Override
    public @NotNull RestComponentsService restComponentsService() {
        return delegate.restComponentsService();
    }

    @Override
    public @NotNull HandlerService handlerService() {
        return delegate.handlerService();
    }

    @Override
    public @NotNull ProtocolAdapterManager protocolAdapterManager() {
        return protocolAdapterManager;
    }

    @Override
    public @NotNull ModulesAndExtensionsService modulesAndExtensionsService() {
        return modulesAndExtensionsService;
    }

    public static @NotNull AfterHiveMQStartBootstrapService decorate(
            final @NotNull CompleteBootstrapService completeBootstrapService,
            final @NotNull ProtocolAdapterManager protocolAdapterManager,
            final @NotNull ModulesAndExtensionsService modulesAndExtensionsService) {
        return new AfterHiveMQStartBootstrapServiceImpl(completeBootstrapService,
                protocolAdapterManager,
                modulesAndExtensionsService);
    }


}
