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

import com.hivemq.configuration.service.ModuleConfigurationService;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleConfigurator {

    private final @NotNull ModuleConfigurationService configurationService;

    public ModuleConfigurator(final @NotNull ModuleConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void setConfigs(final @NotNull Map<String, Object> config) {
        //Follow the pattern of other configurations, and hand off a clone of the map to the config layer
        Map<String, Object> configMap = new HashMap<>();
        for (final String key : config.keySet()) {
            Object value = config.get(key);
            if (value instanceof List) {
                //if its a <structural element> ie. a list, create a shallow copy to additions and removals are distinct
                value = new ArrayList((List) value);
            } else if (value instanceof Map) {
                value = new HashMap<>((Map) value);
            }
            configMap.put(key, value);
        }
        configurationService.setAllConfigs(configMap);
    }

    public void syncConfigs(final @NotNull Map<String, Object> commercialProtocolConfig) {
        if (commercialProtocolConfig == null) {
            return;
        }
        commercialProtocolConfig.clear();
        commercialProtocolConfig.putAll(configurationService.getAllConfigs());
    }
}
