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
import org.jetbrains.annotations.NotNull;

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
    private final @NotNull EdgeCoreFactoryService edgeCoreFactoryService;

    public GeneralBootstrapServiceImpl(
            final @NotNull ShutdownHooks shutdownHooks,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull SystemInformation systemInformation,
            final @NotNull ConfigurationService configurationService,
            final @NotNull HivemqId hivemqId,
            final @NotNull EdgeCoreFactoryService edgeCoreFactoryService) {
        this.shutdownHooks = shutdownHooks;
        this.metricRegistry = metricRegistry;
        this.systemInformation = systemInformation;
        this.configurationService = configurationService;
        this.hivemqId = hivemqId;
        this.edgeCoreFactoryService = edgeCoreFactoryService;
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
    public @NotNull HivemqId getHivemqId() {
        return hivemqId;
    }

    @Override
    public @NotNull EdgeCoreFactoryService edgeCoreFactoryService() {
        return edgeCoreFactoryService;
    }


}
