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
import com.hivemq.persistence.connection.ConnectionPersistence;
import org.jetbrains.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.publish.PublishService;
import com.hivemq.extensions.core.HandlerService;
import com.hivemq.extensions.core.PersistencesService;
import com.hivemq.extensions.core.RestComponentsService;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class CompleteBootstrapServiceImpl implements CompleteBootstrapService {

    private final @NotNull Persistences persistences;
    private final @NotNull RestComponentsService restComponentsService;
    private final @NotNull HandlerService handlerService;
    private final @NotNull EventService eventService;
    private final @NotNull PublishService publishService;
    private final @NotNull PersistenceBootstrapService delegate;

    @Inject
    public CompleteBootstrapServiceImpl(
            final @NotNull PersistenceBootstrapService delegate,
            final @NotNull Persistences persistences,
            final @NotNull RestComponentsService restComponentsService,
            final @NotNull HandlerService handlerService,
            final @NotNull EventService eventService,
            final @NotNull PublishService publishService) {
        this.delegate = delegate;
        this.persistences = persistences;
        this.restComponentsService = restComponentsService;
        this.handlerService = handlerService;
        this.eventService = eventService;
        this.publishService = publishService;
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
    public @NotNull ConnectionPersistence connectionPersistence() {
        return delegate.connectionPersistence();
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

    @Override
    public @NotNull EventService eventService() {
        return eventService;
    }

    @Override
    public @NotNull PublishService publishService() {
        return publishService;
    }

    @Override
    public @NotNull EdgeCoreFactoryService edgeCoreFactoryService() {
        return delegate.edgeCoreFactoryService();
    }

}
