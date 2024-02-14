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

import com.hivemq.edge.modules.ModuleLoader;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class CommercialModuleLoaderDiscovery {

    private static final @NotNull Logger log = LoggerFactory.getLogger(CommercialModuleLoaderDiscovery.class);
    private final @NotNull ModuleLoader moduleLoader;
    private final @NotNull CoreModuleServiceImpl coreModuleService;

    public CommercialModuleLoaderDiscovery(
            final @NotNull ModuleLoader moduleLoader, final @NotNull CoreModuleServiceImpl coreModuleService) {
        this.moduleLoader = moduleLoader;
        this.coreModuleService = coreModuleService;
    }

    public void loadAllCoreModules()
            throws InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        moduleLoader.loadModules();
        final List<Class<? extends ModuleLoaderMain>> implementations =
                moduleLoader.findImplementations(ModuleLoaderMain.class);
        for (Class<? extends ModuleLoaderMain> implementation : implementations) {
            loadAndStartMainClass(implementation);
        }
    }

    private void loadAndStartMainClass(Class<? extends ModuleLoaderMain> extensionMainClass)
            throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        final ModuleLoaderMain instance = extensionMainClass.getDeclaredConstructor().newInstance();
        instance.start(coreModuleService);
    }
}

