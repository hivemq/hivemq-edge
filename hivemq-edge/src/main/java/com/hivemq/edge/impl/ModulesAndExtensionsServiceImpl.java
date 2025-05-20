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
import org.jetbrains.annotations.NotNull;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;

import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Simon L Johnson
 */
public class ModulesAndExtensionsServiceImpl implements ModulesAndExtensionsService {

    private final @NotNull HiveMQExtensions hiveMQExtensions;
    private final @NotNull HiveMQEdgeRemoteService hiveMQEdgeRemoteConfigurationService;

    @Inject
    public ModulesAndExtensionsServiceImpl(final @NotNull HiveMQExtensions hiveMQExtensions,
                                           final @NotNull HiveMQEdgeRemoteService hiveMQEdgeRemoteConfigurationService) {
        this.hiveMQExtensions = hiveMQExtensions;
        this.hiveMQEdgeRemoteConfigurationService = hiveMQEdgeRemoteConfigurationService;
    }

    public @NotNull List<Extension> getExtensions(){

        //-- Add discovered extensions
        List<Extension> availableExtensions = hiveMQEdgeRemoteConfigurationService.getConfiguration().getExtensions();
        HashSet<Extension> extensions = new HashSet<>(availableExtensions);

        //-- Add installed extensions
        Map<String, HiveMQExtension> extensionsMap = hiveMQExtensions.getEnabledHiveMQExtensions();
        extensions.addAll(extensionsMap.values().stream().map(ModulesAndExtensionsServiceImpl::convertExtension).collect(Collectors.toList()));
        return new ArrayList<>(extensions);
    }

    public @NotNull List<Module> getModules(){
        List<Module> availableModules = hiveMQEdgeRemoteConfigurationService.getConfiguration().getModules();
        HashSet<Module> modules = new HashSet<>(availableModules);
        return new ArrayList<>(modules);
    }

    private static Extension convertExtension(HiveMQExtension extension){
        return new Extension(extension.getId(),
                extension.getVersion(),
                extension.getName(),
                null, Objects.requireNonNullElse(extension.getAuthor(), "unknown"),
                extension.getPriority(),
                true,
                null);
    }
}
