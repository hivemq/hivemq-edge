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
package com.hivemq.edge.impl;

import com.hivemq.api.model.components.Extension;
import com.hivemq.api.model.components.Module;
import com.hivemq.edge.HiveMQEdgeRemoteService;
import com.hivemq.edge.ModulesAndExtensionsService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Simon L Johnson
 */
public class ModulesAndExtensionsServiceImpl implements ModulesAndExtensionsService {

    private @NotNull final HiveMQExtensions hiveMQExtensions;
    private @NotNull final HiveMQEdgeRemoteService hiveMQEdgeRemoteConfigurationService;

    @Inject
    public ModulesAndExtensionsServiceImpl(final @NotNull HiveMQExtensions hiveMQExtensions,
                                           final @NotNull HiveMQEdgeRemoteService hiveMQEdgeRemoteConfigurationService) {
        this.hiveMQExtensions = hiveMQExtensions;
        this.hiveMQEdgeRemoteConfigurationService = hiveMQEdgeRemoteConfigurationService;
    }

    public List<Extension> getExtensions(){

        //-- Add discovered extensions
        HashSet<Extension> extensions = new HashSet<>();
        List<Extension> availableExtensions = hiveMQEdgeRemoteConfigurationService.getConfiguration().getExtensions();
        extensions.addAll(availableExtensions);

        //-- Add installed extensions
        Map<String, HiveMQExtension> extensionsMap = hiveMQExtensions.getEnabledHiveMQExtensions();
        extensions.addAll(extensionsMap.values().stream().map(e -> convertExtension(e)).collect(Collectors.toList()));
        return extensions.stream().collect(Collectors.toList());
    }

    public List<Module> getModules(){
        HashSet<Module> modules = new HashSet<>();
        List<Module> availableModules = hiveMQEdgeRemoteConfigurationService.getConfiguration().getModules();
        modules.addAll(availableModules);
        return modules.stream().collect(Collectors.toList());
    }

    private static Extension convertExtension(HiveMQExtension extension){
        return new Extension(extension.getId(),
                extension.getVersion(),
                extension.getName(),
                null,
                extension.getAuthor(),
                extension.getPriority(),
                true,
                null);
    }
}
