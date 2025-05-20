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
package com.hivemq.bootstrap.services;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.bootstrap.ioc.Persistences;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.HiveMQCapabilityService;
import com.hivemq.edge.ModulesAndExtensionsService;
import org.jetbrains.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.publish.PublishService;
import com.hivemq.extensions.core.HandlerService;
import com.hivemq.extensions.core.PersistencesService;
import com.hivemq.extensions.core.RestComponentsService;
import com.hivemq.protocols.ProtocolAdapterManager;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class AfterHiveMQStartBootstrapServiceImpl implements AfterHiveMQStartBootstrapService {

    private final @NotNull CompleteBootstrapService delegate;
    private final @NotNull ProtocolAdapterManager protocolAdapterManager;
    private final @NotNull ModulesAndExtensionsService modulesAndExtensionsService;

    @Inject
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
    public @NotNull EventService eventService() {
        return delegate.eventService();
    }

    @Override
    public @NotNull PublishService publishService() {
        return delegate.publishService();
    }

    @Override
    public @NotNull ProtocolAdapterManager protocolAdapterManager() {
        return protocolAdapterManager;
    }

    @Override
    public @NotNull ModulesAndExtensionsService modulesAndExtensionsService() {
        return modulesAndExtensionsService;
    }

    @Override
    public @NotNull EdgeCoreFactoryService edgeCoreFactoryService() {
        return delegate.edgeCoreFactoryService();
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
