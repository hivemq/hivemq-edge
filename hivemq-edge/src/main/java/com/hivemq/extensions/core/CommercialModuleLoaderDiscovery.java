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
import com.hivemq.bootstrap.services.CompleteBootstrapService;
import com.hivemq.bootstrap.services.GeneralBootstrapService;
import com.hivemq.bootstrap.services.PersistenceBootstrapService;
import com.hivemq.edge.modules.ModuleLoader;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommercialModuleLoaderDiscovery {

    private static final @NotNull Logger log = LoggerFactory.getLogger(CommercialModuleLoaderDiscovery.class);
    private final @NotNull ImmutableList<ModuleLoaderMain> instances;

    public CommercialModuleLoaderDiscovery(
            final @NotNull ModuleLoader moduleLoader) {
        moduleLoader.loadModules();
        final ImmutableList.Builder<ModuleLoaderMain> builder = ImmutableList.builder();
        moduleLoader.findImplementations(ModuleLoaderMain.class).forEach(impl -> {
            try {
                builder.add(impl.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                log.error("Error when instancing '{}':", impl, e);
            }
        });
        this.instances = builder.build();
    }

    public void generalBootstrap(final @NotNull GeneralBootstrapService generalBootstrapService) {
        try {
            instances.forEach(instance -> instance.generalBootstrap(generalBootstrapService));
        } catch (Exception e) {
            log.error("Error when bootstrapping general information", e);
        }
    }

    public void persistenceBootstrap(final @NotNull PersistenceBootstrapService persistenceBootstrapService) {
        try {
            for (ModuleLoaderMain instance : instances) {
                instance.persistenceBootstrap(persistenceBootstrapService);
            }
        } catch (Exception e) {
            log.error("Error when bootstrapping persistences ", e);
        }
    }

    public void completeBootstrap(final @NotNull CompleteBootstrapService completeBootstrapService) {
        try {
            for (ModuleLoaderMain instance : instances) {
                instance.afterPersistenceBootstrap(completeBootstrapService);
            }
        } catch (Exception e) {
            log.error("Error when completing bootstrap ", e);
        }
    }


    public void afterHiveMQStart(final @NotNull CompleteBootstrapService completeBootstrapService) {
        try {
            for (ModuleLoaderMain instance : instances) {
                instance.afterPersistenceBootstrap(completeBootstrapService);
            }
        } catch (Exception e) {
            log.error("Error when completing bootstrap ", e);
        }
    }



}


