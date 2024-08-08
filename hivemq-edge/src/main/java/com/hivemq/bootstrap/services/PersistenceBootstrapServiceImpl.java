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
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.HiveMQCapabilityService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.core.PersistencesService;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PersistenceBootstrapServiceImpl implements PersistenceBootstrapService {

    private final @NotNull GeneralBootstrapService delegate;
    private final @NotNull PersistencesService persistencesService;
    private final @NotNull HiveMQCapabilityService capabilityService;

    @Inject
    public PersistenceBootstrapServiceImpl(
            final @NotNull GeneralBootstrapService delegate,
            final @NotNull PersistencesService persistencesService,
            final @NotNull HiveMQCapabilityService capabilityService) {
        this.delegate = delegate;
        this.persistencesService = persistencesService;
        this.capabilityService = capabilityService;
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
    public @NotNull EdgeCoreFactoryService edgeCoreFactoryService() {
        return delegate.edgeCoreFactoryService();
    }

    @Override
    public @NotNull PersistencesService persistenceService() {
        return persistencesService;
    }

    @Override
    public @NotNull HiveMQCapabilityService capabilityService() {
        return capabilityService;
    }

    public static @NotNull PersistenceBootstrapService decorate(
            final @NotNull GeneralBootstrapService generalBootstrapService,
            final @NotNull PersistencesService persistencesService,
            final @NotNull HiveMQCapabilityService capabilityService) {
        return new PersistenceBootstrapServiceImpl(generalBootstrapService,
                persistencesService,
                capabilityService);
    }


}
