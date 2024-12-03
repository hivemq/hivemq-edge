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
package com.hivemq.extensions.core;

import com.google.common.collect.ImmutableList;
import com.hivemq.bootstrap.services.AfterHiveMQStartBootstrapService;
import com.hivemq.bootstrap.services.CompleteBootstrapService;
import com.hivemq.bootstrap.services.GeneralBootstrapService;
import com.hivemq.bootstrap.services.PersistenceBootstrapService;
import com.hivemq.edge.modules.ModuleLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommercialModuleLoaderDiscovery {

    private static final @NotNull Logger log = LoggerFactory.getLogger(CommercialModuleLoaderDiscovery.class);
    private final @Nullable ModuleLoaderMain instance;

    public CommercialModuleLoaderDiscovery(final @NotNull ModuleLoader moduleLoader) {
        final ImmutableList.Builder<ModuleLoaderMain> builder = ImmutableList.builder();
        moduleLoader.findImplementations(ModuleLoaderMain.class).forEach(impl -> {
            try {
                builder.add(impl.getDeclaredConstructor().newInstance());
            } catch (final Exception e) {
                log.error("Error when instancing '{}':", impl, e);
            }
        });
        final ImmutableList<ModuleLoaderMain> moduleLoaderMains = builder.build();
        if (moduleLoaderMains.isEmpty()) {
            log.info("No commercial module loader main was discovered. Commercial modules will not be loaded.");
            instance = null;
        } else if (moduleLoaderMains.size() == 1) {
            this.instance = moduleLoaderMains.get(0);
        } else {
            log.warn("More than one module loader main was discovered. Only the first one will be used.");
            this.instance = moduleLoaderMains.get(0);
        }
    }

    public void generalBootstrap(final @NotNull GeneralBootstrapService generalBootstrapService) {
        try {
            if (instance != null) {
                instance.generalBootstrap(generalBootstrapService);
            }
        } catch (final Exception e) {
            log.error("Error when bootstrapping general information", e);
        }
    }

    public void persistenceBootstrap(final @NotNull PersistenceBootstrapService persistenceBootstrapService) {
        try {
            if (instance != null) {
                instance.persistenceBootstrap(persistenceBootstrapService);
            }
        } catch (final Exception e) {
            log.error("Error when bootstrapping persistences ", e);
        }
    }

    public void completeBootstrap(final @NotNull CompleteBootstrapService completeBootstrapService) {
        try {
            if (instance != null) {

                instance.afterPersistenceBootstrap(completeBootstrapService);
            }
        } catch (final Exception e) {
            log.error("Error when completing bootstrap ", e);
        }
    }


    public void afterHiveMQStart(final @NotNull AfterHiveMQStartBootstrapService afterHiveMQStartBootstrapService) {
        try {
            if (instance != null) {
                instance.afterHiveMQStart(afterHiveMQStartBootstrapService);
            }
        } catch (final Exception e) {
            log.error("Error when completing bootstrap ", e);
        }
    }


}


