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
package com.hivemq.edge.modules.ioc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.HiveMQEdgeRemoteService;
import com.hivemq.edge.ModulesAndExtensionsService;
import com.hivemq.edge.impl.remote.HiveMQRemoteServiceImpl;
import com.hivemq.edge.impl.ModulesAndExtensionsServiceImpl;
import org.jetbrains.annotations.NotNull;
import com.hivemq.extensions.HiveMQExtensions;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;

import jakarta.inject.Singleton;

/**
 * @author Simon L Johnson
 */
@Module
public class RemoteServiceModule {

    @Provides
    @Singleton
    static @NotNull HiveMQEdgeRemoteService remoteConfigurationService(final @NotNull SystemInformation systemInformation,
                                                                        final @NotNull ConfigurationService configurationService,
                                                                        final @NotNull ObjectMapper objectMapper,
                                                                       final @NotNull ShutdownHooks shutdownHooks){
        return new HiveMQRemoteServiceImpl(systemInformation, configurationService, objectMapper, shutdownHooks);
    }

    @Provides
    @Singleton
    static @NotNull ModulesAndExtensionsService modulesAndExtensionsService(final @NotNull HiveMQExtensions hiveMQExtensions,
                                                                            final @NotNull HiveMQEdgeRemoteService remoteService){
        return new ModulesAndExtensionsServiceImpl(hiveMQExtensions, remoteService);
    }


    @Provides
    @IntoSet
    Boolean eagerSingletons(
            final @NotNull HiveMQEdgeRemoteService httpService) {
        // this is used to instantiate all the params, similar to guice's asEagerSingleton and returns nothing
        return Boolean.TRUE;
    }
}
