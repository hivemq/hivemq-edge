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

import com.hivemq.configuration.service.ProtocolAdapterConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProtocolAdapterConfigurator {

    private final @NotNull ProtocolAdapterConfigurationService configurationService;

    public ProtocolAdapterConfigurator(final @NotNull ProtocolAdapterConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void setConfigs(final @NotNull Map<String, Object> protocolAdapterConfig) {
        //Follow the pattern of other configurations, and hand off a clone of the map to the config layer
        Map<String, Object> configMap = new HashMap<>();
        for(final String key : protocolAdapterConfig.keySet()){
            Object value = protocolAdapterConfig.get(key);
            if(value instanceof List){
                //if its a <structural element> ie. a list, create a shallow copy to additions and removals are distinct
                value = new ArrayList((List) value);
            }
            configMap.put(key, value);
        }
        configurationService.setAllConfigs(configMap);
    }

    public void syncConfigs(final @NotNull Map<String, Object> protocolAdapterConfig){
        if (protocolAdapterConfig == null) {
            return;
        }
        protocolAdapterConfig.clear();
        protocolAdapterConfig.putAll(configurationService.getAllConfigs());
    }
}
