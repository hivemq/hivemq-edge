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
package com.hivemq.configuration.reader;

import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.InternalConfigEntity;
import com.hivemq.configuration.service.ModuleConfigurationService;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleConfigurator implements Configurator<Map<String, Object>>{

    private final @NotNull ModuleConfigurationService configurationService;

    private volatile Map<String, Object> configEntity;
    private volatile boolean initialized = false;

    public ModuleConfigurator(final @NotNull ModuleConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public boolean needsRestartWithConfig(final HiveMQConfigEntity config) {
        if(initialized && hasChanged(this.configEntity, config.getModuleConfigs())) {
            return true;
        }
        return false;
    }

    @Override
    public ConfigResult applyConfig(final @NotNull HiveMQConfigEntity config) {
        this.configEntity = config.getModuleConfigs();
        this.initialized = true;
        //Follow the pattern of other configurations, and hand off a clone of the map to the config layer
        final Map<String, Object> configMap = new HashMap<>();
        for (final String key : configEntity.keySet()) {
            Object value = configEntity.get(key);
            if (value instanceof List) {
                //if its a <structural element> ie. a list, create a shallow copy to additions and removals are distinct
                value = new ArrayList((List) value);
            } else if (value instanceof Map) {
                value = new HashMap<>((Map) value);
            }
            configMap.put(key, value);
        }
        configurationService.setAllConfigs(configMap);
        return ConfigResult.SUCCESS;
    }

}
